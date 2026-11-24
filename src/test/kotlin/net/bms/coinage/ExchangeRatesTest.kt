package net.bms.coinage

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ExchangeRatesTest {
    @Test
    fun defaultTiersUseOneHundredToOne() {
        val rates = ExchangeRates()
        assertEquals(listOf(1L, 100L, 10_000L, 1_000_000L, 100_000_000L), CoinTier.entries.map(rates::value))
    }

    @Test
    fun customRatesPreserveOrderAndStackArithmetic() {
        val rates = ExchangeRates(5, 20, 50, 10)
        assertEquals(listOf(1L, 5L, 100L, 5_000L, 50_000L), CoinTier.entries.map(rates::value))
        val largest = ExchangeRates(10_000, 10_000, 10_000, 10_000).value(CoinTier.PLATINUM)
        assertTrue(largest <= Long.MAX_VALUE / 99)
        assertThrows(IllegalArgumentException::class.java) { ExchangeRates(1, 100, 100, 100) }
    }

    @Test
    fun expressionsAcceptCoinNames() {
        assertEquals(203_000_000L, CoinAmount.parse("2 * platinum + 3 * gold", 0))
        assertEquals(20_100L, CoinAmount.parse("2 * silver + iron", 0))
    }
}
