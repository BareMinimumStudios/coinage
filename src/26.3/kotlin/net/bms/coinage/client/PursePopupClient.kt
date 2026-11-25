package net.bms.coinage.client

import net.bms.coinage.ClientPurseState
import net.bms.coinage.CurrencyTooltip
import net.minecraft.ChatFormatting
import net.bms.coinage.PurseWithdrawPayloads
import net.bms.coinage.mixin.ContainerScreenAccessor
import net.bms.coinage.mixin.CreativeScreenAccessor
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.client.gui.screens.inventory.MerchantScreen
import net.minecraft.network.chat.Component
import net.minecraft.world.item.CreativeModeTab

internal object PursePopupClient {
    var selected: (PurseWithdrawPayloads.Selected) -> Unit = {}
    var selectionAvailable: () -> Boolean = { false }
    var quick: (PurseWithdrawPayloads.Quick) -> Unit = {}
    var quickAvailable: () -> Boolean = { false }
    private var attached: Pair<AbstractContainerScreen<*>, Button>? = null
    private var tooltipRevision = -1L

    fun anchor(screen: AbstractContainerScreen<*>): Pair<Int, Int> {
        val access = screen as ContainerScreenAccessor
        val offset = if (screen is CreativeModeInventoryScreen) 38 else access.`coinage$getWidth`() - 16
        val settings = PurseClient.config.purseOffsets
        val (dx, dy) = when (screen) {
            is CreativeModeInventoryScreen -> settings.creativeX.get() to settings.creativeY.get()
            is MerchantScreen -> settings.merchantX.get() to settings.merchantY.get()
            else -> settings.inventoryX.get() to settings.inventoryY.get()
        }
        val x = (access.`coinage$getLeft`() + offset + dx).coerceIn(0, (screen.width - 11).coerceAtLeast(0))
        val y = (access.`coinage$getTop`() + 5 + dy).coerceIn(0, (screen.height - 13).coerceAtLeast(0))
        return x to y
    }

    fun open(screen: Screen? = Minecraft.getInstance().gui.screen()): Boolean {
        val client = Minecraft.getInstance()
        val player = client.player ?: return false
        if (!ClientPurseState.ready || !player.isAlive) return false
        if (screen is PursePopupScreen) { screen.onClose(); return true }
        val parent = if (screen == null) InventoryScreen(player).also { client.gui.setScreen(it) }
            else screen as? AbstractContainerScreen<*> ?: return false
        if (parent.menu !== client.player?.containerMenu) return false
        client.gui.setScreen(PursePopupScreen(parent))
        return true
    }

    fun buttons(screen: Screen): List<Button> {
        if (screen !is AbstractContainerScreen<*> || !PurseClient.config.inventoryButton) return emptyList()
        val (x, y) = anchor(screen)
        val button = PurseSpriteButton(x, y, 11, 13, Component.translatable("container.coinage.purse"), 62, 0) {
            if (PurseDraw.shiftDown() && quickAvailable()) {
                quick(PurseWithdrawPayloads.Quick(screen.menu.containerId, PurseAmountClient.nextRequest(), 0))
            } else open(screen)
        }
        button.visible = isButtonVisible(screen)
        // Creative inventory can replace InventoryScreen during its initialization.
        val current = Minecraft.getInstance().gui.screen()
        if (current === screen || (current as? PursePopupScreen)?.parent === screen) {
            attached = screen to button
            tooltipRevision = -1
        }
        return listOf(button)
    }

    private fun isButtonVisible(screen: AbstractContainerScreen<*>): Boolean =
        screen !is CreativeModeInventoryScreen || CreativeScreenAccessor.`coinage$getTab`().type == CreativeModeTab.Type.INVENTORY

    fun tick() {
        val (screen, button) = attached ?: return
        val current = Minecraft.getInstance().gui.screen()
        if (current !== screen && (current as? PursePopupScreen)?.parent !== screen) {
            attached = null
            return
        }
        val (x, y) = anchor(screen)
        button.x = x
        button.y = y
        button.visible = isButtonVisible(screen)
        button.active = ClientPurseState.ready
        if (tooltipRevision != ClientPurseState.revision) {
            tooltipRevision = ClientPurseState.revision
            button.setTooltip(Tooltip.create(tooltip()))
        }
    }

    fun tooltip(): Component {
        val tooltip = Component.empty().append(Component.translatable("container.coinage.purse").withStyle(ChatFormatting.GOLD))
        CurrencyTooltip.appendRows(ClientPurseState.balance, ClientPurseState.rates) { row ->
            tooltip.append(Component.literal("\n")).append(row)
        }
        return tooltip
    }
}
