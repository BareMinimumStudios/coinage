package net.bms.coinage

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import java.util.Locale
import java.util.concurrent.CompletableFuture
import net.minecraft.commands.Commands.argument
import net.minecraft.network.chat.Component

internal object CoinAmountArgument {
    const val NAME = "coins"
    private val denominationNames = CoinTier.entries.associateWith { it.name.lowercase(Locale.ROOT) }
    private val trailingLetters = Regex("[a-z]*$")

    fun argument() = argument<String>(NAME, StringArgumentType.greedyString())
        .suggests { _, builder -> suggestions(builder, CurrencyValues.current()) }

    internal fun suggestions(builder: SuggestionsBuilder, rates: ExchangeRates): CompletableFuture<Suggestions> {
        val remaining = builder.remainingLowerCase
        val prefix = trailingLetters.find(remaining)?.value.orEmpty()
        val before = remaining.dropLast(prefix.length).trimEnd()
        if (before.isNotEmpty() && before.last() !in "+*-(") return builder.buildFuture()

        val token = builder.createOffset(builder.input.length - prefix.length)
        for ((tier, name) in denominationNames) {
            if (name.startsWith(prefix)) {
                token.suggest(name, Component.translatable("tooltip.coinage.value", rates.value(tier)))
            }
        }
        return token.buildFuture()
    }
}
