package net.bms.coinage

import me.fzzyhmstrs.fzzy_config.annotations.RootConfig
import me.fzzyhmstrs.fzzy_config.api.ConfigApi
import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.util.Translatable
import net.minecraft.resources.ResourceLocation

@RootConfig
@Translatable.Name("Economy")
class EconomyConfig : Config(ResourceLocation.fromNamespaceAndPath(MOD_ID, "economy")) {
    @Translatable.Name("Player payments")
    @Translatable.Desc("Allow players to send coins directly from their purse.")
    var playerPayments = true

    companion object {
        fun register(): EconomyConfig = ConfigApi.registerAndLoadConfig(configClass = ::EconomyConfig)
    }
}

const val MOD_ID = "coinage"
