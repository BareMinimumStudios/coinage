package net.bms.coinage

import me.fzzyhmstrs.fzzy_config.config.ConfigSection
import me.fzzyhmstrs.fzzy_config.util.Translatable

@Translatable.Name("Shop market")
class MarketConfig : ConfigSection() {
    @Translatable.Desc("Share supply and demand between shops and pawn shops.")
    var enabled = false

    @Translatable.Desc("Crunch expression returning a price percentage from pressure and spillover.")
    var pricePercent = "100 + pressure + spillover"

    @Translatable.Desc("Public shop aliases mapped to dimension, X, Y, and Z separated by spaces.")
    var remoteShops: Map<String, String> = emptyMap()
}
