package net.bms.coinage

import com.mojang.brigadier.suggestion.SuggestionsBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CoinAmountArgumentTest {
    @Test
    fun completesOnlyTheDenominationAtTheEndOfAnExpression() {
        val command = "coinage withdraw "
        val input = command + "2 * SIL"
        val suggestion = CoinAmountArgument.suggestions(SuggestionsBuilder(input, command.length), ExchangeRates()).join()
            .list.single()
        assertEquals(command + "2 * silver", suggestion.apply(input))
    }

    @Test
    fun offersAllDenominationsAfterAnOperator() {
        val input = "silver + "
        val suggestions = CoinAmountArgument.suggestions(SuggestionsBuilder(input, 0), ExchangeRates()).join()
        assertEquals(setOf("silver + bronze", "silver + silver", "silver + gold", "silver + platinum"),
            suggestions.list.map { it.apply(input) }.toSet())
    }

    @Test
    fun leavesPlainNumericAmountsAlone() {
        val input = "250"
        assertEquals(emptyList<Any>(),
            CoinAmountArgument.suggestions(SuggestionsBuilder(input, 0), ExchangeRates()).join().list)
    }
}
