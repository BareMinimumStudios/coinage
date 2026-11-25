package net.bms.coinage

import java.math.BigDecimal
import java.math.RoundingMode
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.trading.MerchantOffer

object MerchantBagPricing {
    const val PRICE_STEPS = 100
    private val HUNDRED = BigDecimal.valueOf(PRICE_STEPS.toLong())
    private val MAXIMUM = BigDecimal.valueOf(Long.MAX_VALUE)

    private fun canonical(stack: ItemStack): Boolean {
        if (stack.item !is MoneyBagItem || stack.count != 1) return false
        val value = MoneyBagItem.value(stack)
        return value > 0 &&
            ItemStack.isSameItemSameComponents(stack, MoneyBagItem.create(value))
    }

    @JvmStatic
    fun heroPriceBasis(stack: ItemStack): ItemStack =
        if (canonical(stack)) stack.copyWithCount(PRICE_STEPS) else stack

    @JvmStatic
    fun adjustedCost(offer: MerchantOffer): ItemStack? {
        val base = offer.baseCostA
        if (!canonical(base)) return null
        return MoneyBagItem.create(adjustedValue(MoneyBagItem.value(base), offer.demand,
            offer.priceMultiplier, offer.specialPriceDiff))
    }

    internal fun adjustedValue(base: Long, demand: Int, multiplier: Float, specialPrice: Int): Long {
        require(base > 0)
        if (!multiplier.isFinite() || multiplier < 0 || (specialPrice == 0 && (demand <= 0 || multiplier == 0f))) return base
        // Bags use percentage points instead of changing their unstackable item count.
        val surcharge = BigDecimal.valueOf(demand.coerceAtLeast(0).toLong())
            .multiply(BigDecimal(multiplier.toString())).multiply(HUNDRED).setScale(0, RoundingMode.FLOOR)
        val steps = HUNDRED.add(surcharge).add(BigDecimal.valueOf(specialPrice.toLong())).max(BigDecimal.ONE)
        return BigDecimal.valueOf(base).multiply(steps).divide(HUNDRED, 0, RoundingMode.FLOOR)
            .max(BigDecimal.ONE).min(MAXIMUM).longValueExact()
    }

    @JvmStatic
    fun satisfiedBy(offer: MerchantOffer, first: ItemStack, second: ItemStack): Boolean? {
        val required = adjustedCost(offer) ?: return null
        if (first.count < required.count || !ItemStack.isSameItemSameComponents(first, required)) return false
        val costB = offer.itemCostB
        return if (costB.isPresent) costB.get().test(second) && second.count >= costB.get().count()
            else second.isEmpty
    }
}
