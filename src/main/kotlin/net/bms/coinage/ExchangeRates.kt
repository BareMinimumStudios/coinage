package net.bms.coinage

enum class CoinTier {
    BRONZE, SILVER, GOLD, PLATINUM
}

data class ExchangeRates(
    val bronzePerSilver: Int = 100,
    val silverPerGold: Int = 100,
    val goldPerPlatinum: Int = 100
) {
    init {
        require(listOf(bronzePerSilver, silverPerGold, goldPerPlatinum).all { it in 2..10_000 })
    }

    fun value(tier: CoinTier): Long = when (tier) {
        CoinTier.BRONZE -> 1L
        CoinTier.SILVER -> bronzePerSilver.toLong()
        CoinTier.GOLD -> bronzePerSilver.toLong() * silverPerGold
        CoinTier.PLATINUM -> bronzePerSilver.toLong() * silverPerGold * goldPerPlatinum
    }
}

internal object CurrencyValues {
    var configuration: EconomyConfig? = null
    private var cached = ExchangeRates()

    fun current(): ExchangeRates {
        val config = configuration?.conversion ?: return cached
        if (cached.bronzePerSilver != config.bronzePerSilver.get() ||
            cached.silverPerGold != config.silverPerGold.get() || cached.goldPerPlatinum != config.goldPerPlatinum.get()
        ) {
            cached = ExchangeRates(
                config.bronzePerSilver.get(),
                config.silverPerGold.get(),
                config.goldPerPlatinum.get()
            )
        }
        return cached
    }
}
