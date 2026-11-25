package net.bms.coinage

internal class PurseSelection {
    private val counts = IntArray(CoinTier.entries.size)
    operator fun get(tier: CoinTier) = counts[tier.ordinal]

    fun total(rates: ExchangeRates): Long = CoinTier.entries.sumOf { counts[it.ordinal] * rates.value(it) }

    fun limit(balance: Long, rates: ExchangeRates) {
        var remaining = balance.coerceAtLeast(0)
        for (tier in CoinTier.entries.reversed()) {
            val unit = rates.value(tier)
            val count = minOf(counts[tier.ordinal].toLong(), remaining / unit).toInt()
            counts[tier.ordinal] = count
            remaining -= count * unit
        }
    }

    fun adjust(tier: CoinTier, change: Int, balance: Long, rates: ExchangeRates) {
        limit(balance, rates)
        val old = counts[tier.ordinal]
        val extra = ((balance.coerceAtLeast(0) - total(rates)) / rates.value(tier)).coerceAtMost(100).toInt()
        counts[tier.ordinal] = (old.toLong() + change).coerceIn(0, minOf(100, old + extra).toLong()).toInt()
    }
}
