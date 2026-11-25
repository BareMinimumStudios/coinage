package net.bms.coinage

import me.fzzyhmstrs.fzzy_config.annotations.RootConfig
import me.fzzyhmstrs.fzzy_config.annotations.Action
import me.fzzyhmstrs.fzzy_config.annotations.RequiresAction
import me.fzzyhmstrs.fzzy_config.api.ConfigApi
import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.util.Translatable
import net.minecraft.resources.Identifier

@RootConfig
@Translatable.Name("Economy")
class EconomyConfig : Config(Identifier.fromNamespaceAndPath(MOD_ID, "economy")) {
    var conversion = CurrencyConfig()
    var merchantTrades = MerchantTradeConfig()
    @RequiresAction(Action.RELOAD_DATA)
    var chestLoot = ChestLootConfig()

    @Translatable.Name("Pay merchants from purse")
    @Translatable.Desc("Withdraw real coins for merchant payment slots and refund unspent currency when trading closes.")
    var merchantPursePayments = true

    @Translatable.Name("Player payments")
    @Translatable.Desc("Allow players to send coins directly from their purse.")
    var playerPayments = true

    @Translatable.Name("Purse loss on death")
    @Translatable.Desc("Percentage of the purse dropped as a money bag on death, from 0 to 100.")
    var deathLossPercent = 10
        set(value) { field = value.coerceIn(0, 100) }

    @Translatable.Name("Mob coin drops")
    @Translatable.Desc("Base bronze-coin value by entity ID, #tag, @namespace, or €category. Only kills credited to a player drop coins.")
    var mobDrops: Map<String, Int> = linkedMapOf("#coinage:currency_drops" to 75)

    @Translatable.Name("Mob drop variance")
    @Translatable.Desc("Random variation around the base drop, in percent.")
    var mobDropVariancePercent = 50
        set(value) { field = value.coerceIn(0, 100) }

    @Translatable.Name("Scale mob currency with health")
    @Translatable.Desc("Multiply mob drops by maximum health divided by 20 and the health divisor.")
    var scaleMobDropsOnHealth = false

    @Translatable.Name("Mob health divisor")
    var mobHealthDivisor = 1.0
        set(value) {
            field = if (value.isFinite()) value.coerceIn(0.01, 1000.0) else 1.0
        }

    @Translatable.Name("Looting currency bonus")
    @Translatable.Desc("Extra currency percentage per Looting level. Set to zero to disable.")
    var lootingBonusPercent = 25
        set(value) {
            field = value.coerceIn(0, 1000)
        }

    companion object {
        fun register(): EconomyConfig = ConfigApi.registerAndLoadConfig(configClass = ::EconomyConfig).also { CurrencyValues.configuration = it }
    }
}

const val MOD_ID = "coinage"
