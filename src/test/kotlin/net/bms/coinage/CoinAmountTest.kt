package net.bms.coinage

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class CoinAmountTest {
    @Test
    fun `arithmetic works without rounding integer inputs`() {
        assertEquals(220L, CoinAmount.parse("2 * (100 + 10)", 1))
        assertEquals(997L, CoinAmount.parse("1000 - 3", 1))
        assertEquals(Long.MAX_VALUE, CoinAmount.parse(Long.MAX_VALUE.toString(), 0))
    }

    @Test
    fun `invalid and out of range amounts are rejected`() {
        assertNull(CoinAmount.parse("2 * (", 1))
        assertNull(CoinAmount.parse("4 - 4", 1))
        assertNull(CoinAmount.parse("9007199254740992 + 1", 0))
        assertNull(CoinAmount.parse("random()", 0))
    }
}
