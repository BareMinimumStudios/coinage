package net.bms.coinage

import com.google.gson.JsonObject
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.Registries
import net.minecraft.server.level.ServerLevel
import net.minecraft.tags.EnchantmentTags
import net.minecraft.tags.TagKey
import net.minecraft.world.entity.LivingEntity
import net.minecraft.core.HolderSet
import net.minecraft.world.item.MapItem
import net.minecraft.world.item.alchemy.PotionContents
import net.minecraft.world.level.saveddata.maps.MapItemSavedData
import net.minecraft.world.level.levelgen.structure.Structure
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.DyeItem
import net.minecraft.world.item.DyeColor
import net.minecraft.world.item.component.DyedItemColor
import net.minecraft.world.item.component.SuspiciousStewEffects
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.item.enchantment.ItemEnchantments
import net.minecraft.world.item.trading.MerchantOffer
import java.util.Optional

internal object TradeSpecializations {
    fun create(entity: LivingEntity, data: JsonObject, stacks: Map<String, ItemStack>, price: Long): MerchantOffer? {
        val level = entity.level() as? ServerLevel ?: return null
        val random = entity.random
        val money = TradePlatform.currency(price)
        var second = ItemStack.EMPTY
        val result: ItemStack
        when (val type = data.get("type").asString.substringAfter(':')) {
            "sell_potion_container" -> {
                val potions = TradePlatform.potions(level)
                if (potions.isEmpty()) return null
                result = stacks.getValue("container_item").copy().apply {
                    set(DataComponents.POTION_CONTENTS, PotionContents(potions[random.nextInt(potions.size)]))
                }
                second = stacks["buy_item"]?.copy() ?: ItemStack.EMPTY
            }
            "sell_map", "sell_map_tag" -> {
                val registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE)
                val structures: HolderSet<Structure> = if (type == "sell_map_tag") {
                    registry.getTag(TagKey.create(Registries.STRUCTURE, TradePlatform.id(data.get("tag").asString)))
                        .orElse(null) ?: return null
                } else {
                    val structure = registry.getHolder(TradePlatform.id(data.get("structure").asString)).orElse(null)
                        ?: return null
                    HolderSet.direct(structure)
                }
                if (structures.size() == 0) return null
                val radius = CurrencyValues.configuration?.merchantTrades?.mapSearchRadius?.get() ?: 100
                val destination = level.chunkSource.generator.findNearestMapStructure(level, structures,
                    entity.blockPosition(), radius, true) ?: return null
                result = MapItem.create(level, destination.first.x, destination.first.z, 2.toByte(), true, true)
                MapItem.renderBiomePreviewMap(level, result)
                val presentation = ExplorerMaps.presentation(destination.second)
                MapItemSavedData.addTargetDecoration(result, destination.first, "+", presentation.icon)
                result.set(DataComponents.ITEM_NAME, presentation.name)
                second = ItemStack(Items.MAP)
            }
            "sell_tag", "buy_tag" -> {
                val key = if (type == "sell_tag") "sell" else "buy"
                val definition = data.getAsJsonObject(key)
                val registry = level.registryAccess().registryOrThrow(Registries.ITEM)
                val entries = registry.getTag(TagKey.create(Registries.ITEM, TradePlatform.id(definition.get("tag").asString)))
                    .orElse(null)?.stream()?.toList() ?: return null
                if (entries.isEmpty()) return null
                val stack = ItemStack(entries[random.nextInt(entries.size)], definition.get("count")?.asInt ?: 1)
                require(stack.count in 1..stack.maxStackSize)
                return if (type == "buy_tag") TradeDefinition.offer(stack, second, money, data)
                    else TradeDefinition.offer(money, second, stack, data)
            }
            "sell_sus_stew" -> {
                val registry = level.registryAccess().registryOrThrow(Registries.MOB_EFFECT)
                val effect = registry.getHolder(TradePlatform.id(data.get("effect_id").asString)).orElseThrow()
                val duration = data.get("duration")?.asInt ?: 160
                require(duration > 0)
                result = ItemStack(Items.SUSPICIOUS_STEW).apply {
                    set(DataComponents.SUSPICIOUS_STEW_EFFECTS,
                        SuspiciousStewEffects(listOf(SuspiciousStewEffects.Entry(effect, duration))))
                }
            }
            "sell_dyed_armor" -> {
                val dyes = mutableListOf(DyeItem.byColor(DyeColor.byId(random.nextInt(16))))
                if (random.nextFloat() > 0.7f) dyes.add(DyeItem.byColor(DyeColor.byId(random.nextInt(16))))
                if (random.nextFloat() > 0.8f) dyes.add(DyeItem.byColor(DyeColor.byId(random.nextInt(16))))
                result = DyedItemColor.applyDyes(stacks.getValue("item").copy(), dyes)
            }
            "sell_single_enchantment", "sell_enchanted_item", "enchant_item" -> {
                val registry = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                val tag = if (data.get("allow_treasure")?.asBoolean == true || type == "sell_single_enchantment")
                    EnchantmentTags.TRADEABLE else EnchantmentTags.NON_TREASURE
                val allowed = registry.getTag(tag).orElse(null) ?: return null
                if (allowed.size() == 0) return null
                var total = price
                if (type == "sell_single_enchantment") {
                    val enchantment = allowed.get(random.nextInt(allowed.size()))
                    val strength = enchantment.value().minLevel + random.nextInt(enchantment.value().maxLevel - enchantment.value().minLevel + 1)
                    result = ItemStack(Items.ENCHANTED_BOOK)
                    val enchantments = ItemEnchantments.Mutable(ItemEnchantments.EMPTY)
                    enchantments.set(enchantment, strength)
                    EnchantmentHelper.setEnchantments(result, enchantments.toImmutable())
                    val factor = (10 / enchantment.value().weight).coerceAtLeast(1)
                    total = 100L * factor + (random.nextInt(50) + strength).toLong() * strength * strength * factor
                    if (enchantment.`is`(EnchantmentTags.DOUBLE_TRADE_PRICE)) total = Math.multiplyExact(total, 2)
                } else {
                    val input = stacks["item"]?.copy() ?: ItemStack(Items.BOOK)
                    result = EnchantmentHelper.enchantItem(random, input.copy(), data.get("level")?.asInt ?: 10,
                        level.registryAccess(), Optional.of(allowed))
                    for (entry in EnchantmentHelper.getEnchantmentsForCrafting(result).entrySet()) {
                        val strength = entry.intValue
                        val factor = (10 / entry.key.value().weight).coerceAtLeast(1)
                        total = Math.addExact(total, (random.nextInt(50) + strength).toLong() * strength * strength * factor)
                    }
                    if (type == "enchant_item") second = input
                }
                return TradeDefinition.offer(TradePlatform.currency(total), second, result, data)
            }
            else -> error("Trade factory '$type' is unsupported")
        }
        return TradeDefinition.offer(money, second, result, data)
    }
}
