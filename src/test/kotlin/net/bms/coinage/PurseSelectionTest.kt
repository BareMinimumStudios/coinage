package net.bms.coinage

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PurseSelectionTest {
    @Test
    fun `selectors support a full one hundred coin stack and clamp increments`() {
        val selection = PurseSelection()
        val rates = ExchangeRates()
        selection.adjust(CoinTier.BRONZE, 100, 1000, rates)
        selection.adjust(CoinTier.BRONZE, Int.MAX_VALUE, 1000, rates)
        assertEquals(100, selection[CoinTier.BRONZE])
        assertEquals(100L, selection.total(rates))
        selection.adjust(CoinTier.BRONZE, Int.MIN_VALUE, 1000, rates)
        assertEquals(0L, selection.total(rates))
    }

    @Test
    fun `all selectors share the available purse budget`() {
        val selection = PurseSelection()
        val rates = ExchangeRates()
        selection.adjust(CoinTier.IRON, 2, 205, rates)
        selection.adjust(CoinTier.BRONZE, 10, 205, rates)
        selection.adjust(CoinTier.SILVER, 1, 205, rates)
        assertEquals(2, selection[CoinTier.IRON])
        assertEquals(5, selection[CoinTier.BRONZE])
        assertEquals(0, selection[CoinTier.SILVER])
        assertEquals(205L, selection.total(rates))
    }

    @Test
    fun `selectors use configured conversion values and react to spent money`() {
        val selection = PurseSelection()
        val rates = ExchangeRates(10, 20, 30, 40)
        selection.adjust(CoinTier.SILVER, 1, 615, rates)
        selection.adjust(CoinTier.IRON, 100, 615, rates)
        selection.adjust(CoinTier.BRONZE, 100, 615, rates)
        assertEquals(615L, selection.total(rates))
        assertEquals(41, selection[CoinTier.IRON])
        assertEquals(5, selection[CoinTier.BRONZE])
        selection.limit(205, rates)
        assertEquals(205L, selection.total(rates))
        assertEquals(0, selection[CoinTier.IRON])
        selection.limit(-1, rates)
        assertEquals(0L, selection.total(rates))
    }

    @Test
    fun `maximum conversion rates and balances remain exact without overflow`() {
        val selection = PurseSelection()
        val rates = ExchangeRates(10000, 10000, 10000, 10000)
        for (tier in CoinTier.entries) selection.adjust(tier, 100, Long.MAX_VALUE, rates)
        assertEquals(1_000_100_010_001_000_100L, selection.total(rates))
        selection.limit(1, rates)
        assertEquals(1L, selection.total(rates))
        assertTrue(CoinTier.entries.all { selection[it] in 0..100 })
    }
}
