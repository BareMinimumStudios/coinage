package net.bms.coinage

import java.text.NumberFormat
import java.util.Locale
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.FontDescription
import net.minecraft.resources.Identifier

internal object CurrencyTooltip {
    private val font = FontDescription.Resource(Identifier.fromNamespaceAndPath(MOD_ID, "purse_coins"))

    fun appendRows(value: Long, rates: ExchangeRates, append: (Component) -> Unit) {
        var remaining = value
        val numbers = NumberFormat.getIntegerInstance(Locale.ROOT)
        for (tier in CoinTier.entries.asReversed()) {
            val unit = rates.value(tier)
            val count = remaining / unit
            remaining %= unit
            if (count == 0L && (value > 0 || tier != CoinTier.BRONZE)) continue

            val icon = (0xe000 + tier.ordinal).toChar()
            val spacing = if (tier == CoinTier.PLATINUM) '\ue005' else '\ue004'
            append(Component.empty()
                .append(Component.literal("$icon$spacing").withStyle { it.withFont(font).withColor(0xffffff) })
                .append(Component.literal(numbers.format(count)).withStyle(ChatFormatting.GRAY)))
        }
    }
}
