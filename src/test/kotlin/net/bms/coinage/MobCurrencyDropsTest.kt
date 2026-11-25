package net.bms.coinage

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MobCurrencyDropsTest {
    @Test
    fun `health scaling is optional and Looting increases currency`() {
        assertEquals(200, MobCurrencyDrops.amount(200, 0, 0.5, 40.0, false, 1.0, 0, 25))
        assertEquals(400, MobCurrencyDrops.amount(200, 0, 0.5, 40.0, true, 1.0, 0, 25))
        assertEquals(350, MobCurrencyDrops.amount(200, 0, 0.5, 20.0, false, 1.0, 3, 25))
        assertEquals(200, MobCurrencyDrops.amount(200, 0, 0.5, 20.0, false, 1.0, 3, 0))
    }

    @Test
    fun `variance stays within the configured range`() {
        assertEquals(100, MobCurrencyDrops.amount(200, 50, 0.0, 20.0, false, 1.0, 0, 25))
        assertEquals(300, MobCurrencyDrops.amount(200, 50, 1.0, 20.0, false, 1.0, 0, 25))
    }

    @Test
    fun `extreme modded health cannot overflow into negative currency`() {
        assertEquals(Long.MAX_VALUE, MobCurrencyDrops.amount(Long.MAX_VALUE, 100, 1.0, 1e30, true, 1.0, 255, 1000))
        assertEquals(0, MobCurrencyDrops.amount(200, 0, 0.5, Double.NaN, true, 1.0, 0, 25))
        assertEquals(0, MobCurrencyDrops.amount(200, 0, 0.5, 20.0, true, 0.0, 0, 25))
    }
}
