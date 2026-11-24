package net.bms.coinage



enum class CoinTier {
    BRONZE, IRON, SILVER, GOLD, PLATINUM
}

data class ExchangeRates(
    val bronzePerIron: Int = 100,
    val ironPerSilver: Int = 100,
    val silverPerGold: Int = 100,
    val goldPerPlatinum: Int = 100
) {
    init {
        require(listOf(bronzePerIron, ironPerSilver, silverPerGold, goldPerPlatinum).all { it in 2..10_000 })
    }

    fun value(tier: CoinTier): Long {
        var value = 1L
        val ratios = intArrayOf(bronzePerIron, ironPerSilver, silverPerGold, goldPerPlatinum)
        for (index in 0 until tier.ordinal) value *= ratios[index]
        return value
    }
}

internal object CurrencyValues {
    var configuration: EconomyConfig? = null
    private var cached = ExchangeRates()

    fun current(): ExchangeRates {
        val config = configuration?.conversion ?: return cached
        if (cached.bronzePerIron != config.bronzePerIron.get() || cached.ironPerSilver != config.ironPerSilver.get() ||
            cached.silverPerGold != config.silverPerGold.get() || cached.goldPerPlatinum != config.goldPerPlatinum.get()) {
            cached = ExchangeRates(config.bronzePerIron.get(), config.ironPerSilver.get(), config.silverPerGold.get(), config.goldPerPlatinum.get())
        }
        return cached
    }
}
