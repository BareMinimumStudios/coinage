package net.bms.coinage

import me.fzzyhmstrs.fzzy_config.config.ConfigSection
import me.fzzyhmstrs.fzzy_config.util.Translatable
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt

enum class TradeMode { REPLACE, APPEND, DISABLED }

@Translatable.Name("Merchant trades")
class MerchantTradeConfig : ConfigSection() {
    @Translatable.Desc("Replace listed profession trades, append to native trades, or leave trading unchanged.")
    var mode = TradeMode.REPLACE
    @Translatable.Desc("Profession IDs that Coinage should leave unchanged.")
    var excludedProfessions: List<String> = emptyList()
    @Translatable.Desc("Convert plain emerald costs and payouts in other professions while preserving item components.")
    var convertEmeralds = true
    var bronzePerEmerald = ValidatedInt(125, 1..1_000_000)
    var offersPerLevel = ValidatedInt(2, 1..32)
    var mapSearchRadius = ValidatedInt(100, 1..512)
}
