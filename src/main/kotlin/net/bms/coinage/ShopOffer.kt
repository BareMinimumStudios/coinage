package net.bms.coinage

import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.world.item.ItemStack

internal data class ShopOffer(val number: Int, val price: Long, val stack: ItemStack) {
    companion object {
        private fun positivePrice(value: Long): DataResult<Long> =
            if (value > 0) DataResult.success(value) else DataResult.error { "Shop prices must be positive" }
        private val PRICE_CODEC = Codec.LONG.flatXmap(::positivePrice, ::positivePrice)
        val CODEC: Codec<ShopOffer> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.intRange(0, Int.MAX_VALUE - 1).fieldOf("slot").forGetter<ShopOffer> { it.number },
                PRICE_CODEC.fieldOf("price").forGetter<ShopOffer> { it.price },
                ItemStack.CODEC.fieldOf("item").forGetter<ShopOffer> { it.stack }
            ).apply(instance, ::ShopOffer)
        }
        val LIST_CODEC: Codec<List<ShopOffer>> = CODEC.listOf()
    }
}
