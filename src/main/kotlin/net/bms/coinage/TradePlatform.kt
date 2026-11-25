package net.bms.coinage

import net.minecraft.core.Holder
import net.minecraft.core.registries.Registries
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.alchemy.Potion
import net.minecraft.core.component.DataComponentPredicate
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.npc.Villager
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.trading.ItemCost
import net.minecraft.world.item.trading.MerchantOffer
import net.minecraft.world.level.levelgen.structure.Structure
import java.util.Optional

internal object TradePlatform {
    fun id(value: String): ResourceLocation = ResourceLocation.parse(value)

    fun profession(entity: LivingEntity): String = if (entity is Villager)
        BuiltInRegistries.VILLAGER_PROFESSION.getKey(entity.villagerData.profession).toString()
        else "minecraft:wandering_trader"

    fun level(entity: LivingEntity): Int = if (entity is Villager) entity.villagerData.level else 0

    fun structurePath(structure: Holder<Structure>): String =
        structure.unwrapKey().orElseThrow().location().path

    fun cost(stack: ItemStack): ItemCost = ItemCost(stack.itemHolder, stack.count,
        DataComponentPredicate.allOf(stack.componentsPatch.split().added()))

    fun currency(amount: Long): ItemStack {
        require(amount > 0)
        for (coin in CoinageItems.descending) {
            val value = coin.denomination
            if (amount % value == 0L && amount / value in 1L..100L)
                return ItemStack(coin, (amount / value).toInt())
        }
        return MoneyBagItem.create(amount)
    }

    fun potions(level: ServerLevel): List<Holder<Potion>> = level.registryAccess()
        .registryOrThrow(Registries.POTION).holders().filter {
            it.value().effects.isNotEmpty() && level.potionBrewing().isBrewablePotion(it)
        }.map<Holder<Potion>> { it }.toList()

    fun remap(offer: MerchantOffer, bronzePerEmerald: Int): MerchantOffer {
        fun ordinaryEmerald(stack: ItemStack): Boolean = stack.`is`(Items.EMERALD) && stack.componentsPatch.isEmpty
        if (!offer.shouldRewardExp() || (!ordinaryEmerald(offer.baseCostA) && !ordinaryEmerald(offer.costB) &&
            !ordinaryEmerald(offer.result))) return offer
        // Component-bearing emeralds may be a different mod's specialized currency.
        fun converted(stack: ItemStack): ItemStack = if (ordinaryEmerald(stack))
            currency(stack.count.toLong() * bronzePerEmerald) else stack.copy()
        val first = converted(offer.baseCostA)
        val second = converted(offer.costB)
        val result = converted(offer.result)
        if (!offer.shouldRewardExp() || (ItemStack.matches(first, offer.baseCostA) &&
            ItemStack.matches(second, offer.costB) && ItemStack.matches(result, offer.result))) return offer
        return MerchantOffer(cost(first), if (second.isEmpty) Optional.empty() else Optional.of(cost(second)),
            result, offer.uses, offer.maxUses, offer.xp, offer.priceMultiplier, offer.demand).apply {
                specialPriceDiff = offer.specialPriceDiff
            }
    }
}
