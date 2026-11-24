package net.bms.numismatic_overhaul

import redempt.crunch.Crunch

internal object CoinAmount {
    private const val MAX_EXACT = 9_007_199_254_740_991L
    private val expression = Regex("[0-9+*() -]{1,96}")
    private val integers = Regex("[0-9]+")

    fun parse(input: String, minimum: Long): Long? {
        val value = input.trim()
        value.toLongOrNull()?.let { return it.takeIf { amount -> amount >= minimum } }
        if (!expression.matches(value)) return null

        // Bound every possible intermediate sum or product within the exact integer range of Double.
        val formula = value.replace(" ", "")
        var bound = 1L
        for (match in integers.findAll(formula)) {
            val operand = match.value.toLongOrNull() ?: return null
            if (operand >= MAX_EXACT || bound > MAX_EXACT / (operand + 1)) return null
            bound *= operand + 1
        }

        val result = try {
            Crunch.evaluateExpression(formula)
        } catch (_: RuntimeException) {
            return null
        }
        if (!result.isFinite() || result < minimum || result > MAX_EXACT || result % 1.0 != 0.0) return null
        return result.toLong()
    }
}
