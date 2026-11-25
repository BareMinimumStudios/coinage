package net.bms.coinage

import me.fzzyhmstrs.fzzy_config.config.ConfigSection
import me.fzzyhmstrs.fzzy_config.util.Translatable
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt

@Translatable.Name("Shop market")
class MarketConfig : ConfigSection() {
    @Translatable.Desc("Share supply and demand between shops and pawn shops.")
    var enabled = false

    @Translatable.Name("Market reset interval (seconds)")
    @Translatable.Desc("Seconds of world time at 20 TPS between price resets. Changes apply when the next interval is scheduled.")
    var resetIntervalSeconds = ValidatedInt(7_200, 1..Int.MAX_VALUE)

    @Translatable.Desc("Crunch expression returning a price percentage from pressure and spillover.")
    var pricePercent = "100 + pressure + spillover"

    @Translatable.Desc("Public shop aliases mapped to dimension, X, Y, and Z separated by spaces.")
    var remoteShops: Map<String, String> = emptyMap()
}
