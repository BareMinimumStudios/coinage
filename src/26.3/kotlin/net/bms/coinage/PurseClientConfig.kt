package net.bms.coinage

import me.fzzyhmstrs.fzzy_config.annotations.RootConfig
import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava
import me.fzzyhmstrs.fzzy_config.api.RegisterType
import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.config.ConfigSection
import me.fzzyhmstrs.fzzy_config.util.Translatable
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt
import net.minecraft.resources.Identifier

internal enum class PurseNotifications { ACTION_BAR, CHAT, OFF }

@Translatable.Name("Purse button position")
@Translatable.Desc("Move the purse button in GUI pixels. Negative values move it left or up.")
internal class PurseButtonOffsets : ConfigSection() {
    @Translatable.Name("Inventory horizontal offset")
    var inventoryX = ValidatedInt(0, -128..128)
    @Translatable.Name("Inventory vertical offset")
    var inventoryY = ValidatedInt(0, -128..128)
    @Translatable.Name("Creative inventory horizontal offset")
    var creativeX = ValidatedInt(0, -128..128)
    @Translatable.Name("Creative inventory vertical offset")
    var creativeY = ValidatedInt(0, -128..128)
    @Translatable.Name("Merchant horizontal offset")
    var merchantX = ValidatedInt(0, -128..128)
    @Translatable.Name("Merchant vertical offset")
    var merchantY = ValidatedInt(0, -128..128)
    @Translatable.Name("Pawn shop horizontal offset")
    var pawnShopX = ValidatedInt(0, -128..128)
    @Translatable.Name("Pawn shop vertical offset")
    var pawnShopY = ValidatedInt(0, -128..128)
}

@RootConfig
@Translatable.Name("Client")
@Translatable.Desc("Your purse display and controls. These settings do not change the server economy.")
internal class PurseClientConfig : Config(Identifier.fromNamespaceAndPath(MOD_ID, "client")) {
    @Translatable.Name("Show purse HUD")
    var showHud = true
    @Translatable.Name("Inventory purse button")
    var inventoryButton = true
    var purseOffsets = PurseButtonOffsets()
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
