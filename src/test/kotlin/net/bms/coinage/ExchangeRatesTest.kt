package net.bms.coinage

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ExchangeRatesTest {
    @Test
    fun defaultTiersUseOneHundredToOne() {
        val rates = ExchangeRates()
        assertEquals(listOf(1L, 100L, 10_000L, 1_000_000L), CoinTier.entries.map(rates::value))
    }

    @Test
    fun customRatesPreserveOrderAndStackArithmetic() {
        val rates = ExchangeRates(5, 20, 50)
        assertEquals(listOf(1L, 5L, 100L, 5_000L), CoinTier.entries.map(rates::value))
        val largest = ExchangeRates(10_000, 10_000, 10_000).value(CoinTier.PLATINUM)
        assertTrue(largest <= Long.MAX_VALUE / 100)
        assertThrows(IllegalArgumentException::class.java) { ExchangeRates(1, 100, 100) }
    }

    @Test
    fun expressionsAcceptCoinNames() {
        assertEquals(2_030_000L, CoinAmount.parse("2 * platinum + 3 * gold", 0))
        assertEquals(205L, CoinAmount.parse("2 * silver + 5 * bronze", 0))
    }
}
