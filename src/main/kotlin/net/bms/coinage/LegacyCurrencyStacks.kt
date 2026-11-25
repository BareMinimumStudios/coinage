package net.bms.coinage

import com.mojang.datafixers.util.Pair
import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import com.mojang.serialization.Dynamic
import com.mojang.serialization.DynamicOps
import net.minecraft.world.item.ItemStack

object LegacyCurrencyStacks {
    private const val LEGACY_PREFIX = "numismatic-overhaul:"
    private val coinIds = setOf("bronze_coin", "silver_coin", "gold_coin")

    @JvmStatic
    fun wrap(codec: Codec<ItemStack>): Codec<ItemStack> = object : Codec<ItemStack> {
        override fun <T> decode(ops: DynamicOps<T>, input: T): DataResult<Pair<ItemStack, T>> =
            codec.decode(ops, convert(Dynamic(ops, input)).value)

        override fun <T> encode(input: ItemStack, ops: DynamicOps<T>, prefix: T): DataResult<T> =
            codec.encode(input, ops, prefix)
    }

    internal fun <T> convert(input: Dynamic<T>): Dynamic<T> {
        val id = input.get("id").asString("")
        if (!id.startsWith(LEGACY_PREFIX)) return input
        val item = id.removePrefix(LEGACY_PREFIX)
        if (item in coinIds) return input.set("id", input.createString("coinage:$item"))
        if (item != "money_bag") return input

        val renamed = input.set("id", input.createString("coinage:money_bag"))
        val components = input.get("components").result().orElse(null) ?: return renamed
        val oldBag = components.get(LEGACY_PREFIX + "money_bag").result().orElse(null) ?: return renamed
        val bronze = oldBag.get("bronze").asLong(-1)
        val silver = oldBag.get("silver").asLong(-1)
        val gold = oldBag.get("gold").asLong(-1)
        if (bronze < 0 || silver < 0 || gold < 0) return input
        val amount = try {
            Math.addExact(bronze, Math.addExact(Math.multiplyExact(silver, 100), Math.multiplyExact(gold, 10_000)))
        } catch (_: ArithmeticException) {
            return input
        }
        val customData = components.get("minecraft:custom_data").result().orElse(input.emptyMap())
        val updated = components.remove(LEGACY_PREFIX + "money_bag")
            .set("minecraft:custom_data", customData.set("coinage_value", input.createLong(amount)))
        return renamed.set("components", updated)
    }
}
