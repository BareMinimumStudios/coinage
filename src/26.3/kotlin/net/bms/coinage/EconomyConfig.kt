package net.bms.coinage

import me.fzzyhmstrs.fzzy_config.annotations.RootConfig
import me.fzzyhmstrs.fzzy_config.api.ConfigApi
import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.util.Translatable
import net.minecraft.resources.Identifier

@RootConfig
@Translatable.Name("Economy")
class EconomyConfig : Config(Identifier.fromNamespaceAndPath(MOD_ID, "economy")) {
    var conversion = CurrencyConfig()

    @Translatable.Name("Player payments")
    @Translatable.Desc("Allow players to send coins directly from their purse.")
    var playerPayments = true

    @Translatable.Name("Purse loss on death")
    @Translatable.Desc("Percentage of the purse dropped as a money bag on death, from 0 to 100.")
    var deathLossPercent = 10
        set(value) { field = value.coerceIn(0, 100) }

    @Translatable.Name("Mob coin drops")
    @Translatable.Desc("Base bronze-coin value per entity ID. Only kills credited to a player drop coins.")
    var mobDrops: Map<String, Int> = linkedMapOf("minecraft:pillager" to 75, "minecraft:vindicator" to 75, "minecraft:evoker" to 75)

    @Translatable.Name("Mob drop variance")
    @Translatable.Desc("Random variation around the base drop, in percent.")
    var mobDropVariancePercent = 50
        set(value) { field = value.coerceIn(0, 100) }

    companion object {
        fun register(): EconomyConfig = ConfigApi.registerAndLoadConfig(configClass = ::EconomyConfig).also { CurrencyValues.configuration = it }
    }
}

const val MOD_ID = "coinage"
