package net.bms.coinage

import me.fzzyhmstrs.fzzy_config.annotations.RootConfig
import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava
import me.fzzyhmstrs.fzzy_config.api.RegisterType
import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.util.Translatable
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt
import net.minecraft.resources.ResourceLocation

internal enum class PurseNotifications { ACTION_BAR, CHAT, OFF }

@RootConfig
@Translatable.Name("Client")
@Translatable.Desc("Your purse display and controls. These settings do not change the server economy.")
internal class PurseClientConfig : Config(ResourceLocation.fromNamespaceAndPath(MOD_ID, "client")) {
    @Translatable.Name("Show purse HUD")
    var showHud = true
    @Translatable.Name("Inventory purse button")
    var inventoryButton = true
    @Translatable.Name("Balance change notifications")
    var notifications = PurseNotifications.ACTION_BAR
    @Translatable.Name("HUD horizontal position (%)")
    var hudX = ValidatedInt(100, 100, 0)
    @Translatable.Name("HUD vertical position (%)")
    var hudY = ValidatedInt(0, 100, 0)

    companion object {
        fun register(): PurseClientConfig = ConfigApiJava.registerAndLoadConfig(::PurseClientConfig, RegisterType.CLIENT)
    }
}
