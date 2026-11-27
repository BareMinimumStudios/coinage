package net.bms.coinage

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import java.math.BigInteger
import redempt.crunch.Crunch

internal class ShopMarket {
    private val pressure = HashMap<String, Int>()
    private var totalSupplyPressure = 0L
    private var resetAt = 0L
    var revision = 0L
        private set

    fun refresh(now: Long, intervalSeconds: Int, dirty: () -> Unit) {
        if (resetAt > now) return
        pressure.clear()
        totalSupplyPressure = 0L
        resetAt = now + intervalSeconds * 20L
        revision++
        dirty()
    }

    fun record(item: String, units: Int) {
        val next = ((pressure[item] ?: 0) + units.coerceIn(-50, 50)).coerceIn(-50, 50)
        setPressure(item, next)
        revision++
    }

    private fun setPressure(item: String, value: Int) {
        val previous = pressure[item] ?: 0
        totalSupplyPressure += previous.coerceAtMost(0) - value.coerceAtMost(0)
        if (value == 0) pressure.remove(item) else pressure[item] = value
    }

    fun quote(item: String, base: Long, expression: String): Long {
        if (pressure.isEmpty() || base <= 0 || expression.length > 96) return base
        val itemPressure = pressure[item] ?: 0
        val otherSupplyPressure = totalSupplyPressure + itemPressure.coerceAtMost(0)
        val spillover = (otherSupplyPressure / 10).coerceAtMost(10)
        val formula = variables.replace(expression) {
            if (it.value == "pressure") "($itemPressure)" else "($spillover)"
        }
        if (!arithmetic.matches(formula)) return base
        val percent = try {
            Crunch.evaluateExpression(formula)
        } catch (_: RuntimeException) {
            return base
        }
        if (!percent.isFinite()) return base
        val multiplier = percent.coerceIn(50.0, 150.0).toInt()
        return BigInteger.valueOf(base).multiply(BigInteger.valueOf(multiplier.toLong()))
            .add(FIFTY).divide(HUNDRED).coerceIn(BigInteger.ONE, MAX_LONG).toLong()
    }

    companion object {
        private val variables = Regex("""\b(pressure|spillover)\b""")
        private val arithmetic = Regex("[0-9+*()/ .-]{1,160}")
        private val FIFTY = BigInteger.valueOf(50)
        private val HUNDRED = BigInteger.valueOf(100)
        private val MAX_LONG = BigInteger.valueOf(Long.MAX_VALUE)
        val codec: Codec<ShopMarket> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.LONG.fieldOf("reset_at").forGetter<ShopMarket> { it.resetAt },
                Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("pressure")
                    .forGetter<ShopMarket> { it.pressure.toMap() }
            ).apply(instance) { resetAt, pressure ->
                ShopMarket().apply {
                    this.resetAt = resetAt
                    pressure.forEach { (item, value) -> setPressure(item, value.coerceIn(-50, 50)) }
                }
            }
        }
    }
}
