package net.bms.coinage

import me.fzzyhmstrs.fzzy_config.config.ConfigSection
import me.fzzyhmstrs.fzzy_config.util.Translatable
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt

@Translatable.Name("Coin conversion")
@Translatable.Desc("Each step is worth 2–10,000 coins of the tier below it. Bronze is always lowest and platinum highest. The limit keeps complete stacks within the purse's integer range.")
class CurrencyConfig : ConfigSection() {
    @Translatable.Name("Bronze per iron")
    var bronzePerIron = ValidatedInt(100, 2..10_000)

    @Translatable.Name("Iron per silver")
    var ironPerSilver = ValidatedInt(100, 2..10_000)

    @Translatable.Name("Silver per gold")
    var silverPerGold = ValidatedInt(100, 2..10_000)

    @Translatable.Name("Gold per platinum")
    var goldPerPlatinum = ValidatedInt(100, 2..10_000)
}
