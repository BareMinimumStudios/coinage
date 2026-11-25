package net.bms.coinage

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MerchantBagPricingTest {
    @Test
    fun `discounts and demand adjust the value rather than the bag count`() {
        assertEquals(205L, MerchantBagPricing.adjustedValue(205, 0, .05f, 0))
        assertEquals(143L, MerchantBagPricing.adjustedValue(205, 0, .05f, -30))
        assertEquals(246L, MerchantBagPricing.adjustedValue(205, 4, .05f, 0))
        assertEquals(184L, MerchantBagPricing.adjustedValue(205, 4, .05f, -30))
        assertEquals(205L, MerchantBagPricing.adjustedValue(205, -10, .05f, 0))
    }

    @Test
    fun `prices stay positive and long values remain exact`() {
        assertEquals(1L, MerchantBagPricing.adjustedValue(1, 0, .05f, Int.MIN_VALUE))
        assertEquals(92_233_720_368_547_758L, MerchantBagPricing.adjustedValue(Long.MAX_VALUE, 0, 0f, -99))
        assertEquals(Long.MAX_VALUE, MerchantBagPricing.adjustedValue(Long.MAX_VALUE, Int.MAX_VALUE, Float.MAX_VALUE, Int.MAX_VALUE))
        assertEquals(Long.MAX_VALUE, MerchantBagPricing.adjustedValue(Long.MAX_VALUE, 0, 0f, 0))
    }
}
