package net.bms.coinage

import com.google.gson.JsonObject
import com.mojang.serialization.JsonOps
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.MinecraftServer
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.trading.MerchantOffer
import java.util.Optional

internal class TradeDefinition private constructor(
    val source: String,
    private val data: JsonObject,
    private val stacks: Map<String, ItemStack>
) {
    fun create(entity: LivingEntity): MerchantOffer? {
        val price = CoinAmount.parse(priceExpression(data), 1)
            ?: error("Invalid currency expression")
        val money = TradePlatform.currency(price)
        fun stack(name: String): ItemStack = stacks[name]?.copy() ?: error("Missing '$name'")
        val type = data.get("type").asString.substringAfter(':')
        val first: ItemStack
        val second: ItemStack
        val result: ItemStack
        when (type) {
            "sell_stack", "dimension_sell_stack" -> {
                if (type == "dimension_sell_stack" && entity.level().dimension().identifier().toString() !=
                    TradePlatform.id(data.get("dimension").asString).toString()) return null
                first = money
                second = ItemStack.EMPTY
                result = stack("sell")
            }
            "buy_stack", "buy_item" -> {
                first = stack("buy")
                second = ItemStack.EMPTY
                result = money
            }
            "process_item" -> {
                first = money
                second = stack("buy")
                result = stack("sell")
            }
            else -> return TradeSpecializations.create(entity, data, stacks, price)
        }
        return offer(first, second, result, data)
    }

    companion object {
        fun offer(first: ItemStack, second: ItemStack, result: ItemStack, data: JsonObject): MerchantOffer =
            MerchantOffer(TradePlatform.cost(first), if (second.isEmpty) Optional.empty() else Optional.of(TradePlatform.cost(second)),
                result, data.get("max_uses")?.asInt ?: 12, data.get("villager_experience")?.asInt ?: 5,
                data.get("price_multiplier")?.asFloat ?: 0.05f)

        fun parse(data: JsonObject, server: MinecraftServer, source: String): TradeDefinition {
            val type = TradePlatform.id(data.get("type").asString)
            require(type.namespace == MOD_ID) { "Unsupported trade namespace: $type" }
            require(type.path in TYPES) { "Unknown trade type: $type" }
            require((data.get("max_uses")?.asInt ?: 12) in 1..1_000_000) { "Invalid max_uses" }
            require((data.get("villager_experience")?.asInt ?: 5) >= 0) { "Negative experience" }
            val multiplier = data.get("price_multiplier")?.asFloat ?: 0.05f
            require(multiplier.isFinite() && multiplier >= 0) { "Invalid price_multiplier" }
            val ops = server.registryAccess().createSerializationContext(JsonOps.INSTANCE)
            val stacks = linkedMapOf<String, ItemStack>()
            for (key in listOf("buy", "sell", "item", "buy_item", "container_item")) {
                val entry = data.get(key) ?: continue
                val encoded = if (entry.isJsonPrimitive) JsonObject().apply { addProperty("id", entry.asString) }
                    else entry.asJsonObject.deepCopy().apply {
                        if (has("item")) add("id", remove("item"))
                    }
                if (!encoded.has("id")) continue
                val id = TradePlatform.id(encoded.get("id").asString)
                require(BuiltInRegistries.ITEM.containsKey(id)) { "Unknown item: $id" }
                encoded.addProperty("id", id.toString())
                val stack = ItemStack.CODEC.parse(ops, encoded).getOrThrow()
                require(!stack.isEmpty && stack.count <= stack.maxStackSize) { "Invalid stack: $key" }
                stacks[key] = stack
            }
            val requiredStacks = when (type.path) {
                "sell_stack", "dimension_sell_stack" -> listOf("sell")
                "buy_stack", "buy_item" -> listOf("buy")
                "process_item" -> listOf("buy", "sell")
                "sell_potion_container" -> listOf("container_item")
                "sell_dyed_armor" -> listOf("item")
                else -> emptyList()
            }
            for (key in requiredStacks) require(key in stacks) { "Missing '$key' stack" }
            require(CoinAmount.parse(priceExpression(data), 1) != null) { "Invalid currency expression" }
            return TradeDefinition(source, data.deepCopy(), stacks)
        }

        private fun priceExpression(data: JsonObject): String =
            (data.get("price") ?: data.get("base_price"))?.asString ?: "200"

        private val TYPES = setOf("sell_stack", "buy_stack", "buy_item", "sell_tag", "buy_tag",
            "dimension_sell_stack", "process_item", "sell_sus_stew", "sell_dyed_armor",
            "sell_potion_container", "sell_single_enchantment", "sell_enchanted_item", "enchant_item",
            "sell_map", "sell_map_tag")
    }
}
