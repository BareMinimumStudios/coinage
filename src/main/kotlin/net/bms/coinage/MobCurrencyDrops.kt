package net.bms.coinage

internal object MobCurrencyDrops {
    fun amount(
        base: Long,
        variancePercent: Int,
        random: Double,
        maximumHealth: Double,
        scaleOnHealth: Boolean,
        healthDivisor: Double,
        looting: Int,
        lootingBonusPercent: Int
    ): Long {
        if (base <= 0) return 0
        val variance = variancePercent.coerceIn(0, 100) / 100.0
        var multiplier = 1 - variance + random.coerceIn(0.0, 1.0) * variance * 2
        if (scaleOnHealth) {
            if (!maximumHealth.isFinite() || maximumHealth <= 0 || !healthDivisor.isFinite() || healthDivisor <= 0) return 0
            multiplier *= maximumHealth / (20 * healthDivisor)
        }
        multiplier *= 1 + looting.coerceIn(0, 255) * lootingBonusPercent.coerceIn(0, 1000) / 100.0
        return (base * multiplier).toLong().coerceAtLeast(0)
    }
}
