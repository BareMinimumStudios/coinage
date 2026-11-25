package net.bms.coinage.client

import java.text.NumberFormat
import java.util.Locale
import net.bms.coinage.ClientPurseState
import net.bms.coinage.CoinTier
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
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.CreativeModeTab

internal object PursePopupClient {
    var selected: (PurseWithdrawPayloads.Selected) -> Unit = {}
    var selectionAvailable: () -> Boolean = { false }
    var quick: (PurseWithdrawPayloads.Quick) -> Unit = {}
    var quickAvailable: () -> Boolean = { false }
    private var attached: Pair<AbstractContainerScreen<*>, Button>? = null
    private var tooltipRevision = -1L
    private val numbers = NumberFormat.getIntegerInstance(Locale.ROOT)

    fun anchor(screen: AbstractContainerScreen<*>): Pair<Int, Int> {
        val access = screen as ContainerScreenAccessor
        val offset = if (screen is CreativeModeInventoryScreen) 38 else access.`coinage$getWidth`() - 16
        return access.`coinage$getLeft`() + offset to access.`coinage$getTop`() + 5
    }

    fun open(screen: Screen? = Minecraft.getInstance().screen) {
        val client = Minecraft.getInstance()
        val player = client.player ?: return
        if (!ClientPurseState.ready || !player.isAlive) return
        if (screen is PursePopupScreen) { screen.onClose(); return }
        val parent = if (screen == null) InventoryScreen(player).also { client.setScreen(it) }
            else screen as? AbstractContainerScreen<*> ?: return
        if (parent.menu !== client.player?.containerMenu) return
        client.setScreen(PursePopupScreen(parent))
    }

    fun buttons(screen: Screen): List<Button> {
        if (screen !is AbstractContainerScreen<*> || !PurseClient.config.inventoryButton) return emptyList()
        val (x, y) = anchor(screen)
        val button = PurseSpriteButton(x, y, 11, 13, Component.translatable("container.coinage.purse"), 62, 0) {
            if (PurseDraw.shiftDown() && quickAvailable()) {
                quick(PurseWithdrawPayloads.Quick(screen.menu.containerId, PurseAmountClient.nextRequest(), 0))
            } else open(screen)
        }
        attached = screen to button
        tooltipRevision = -1
        return listOf(button)
    }

    fun tick() {
        val (screen, button) = attached ?: return
        val current = Minecraft.getInstance().screen
        if (current !== screen && (current as? PursePopupScreen)?.parent !== screen) {
            attached = null
            return
        }
        val (x, y) = anchor(screen)
        button.x = x
        button.y = y
        button.visible = screen !is CreativeModeInventoryScreen || CreativeScreenAccessor.`coinage$getTab`().type == CreativeModeTab.Type.INVENTORY
        button.active = ClientPurseState.ready
        if (tooltipRevision != ClientPurseState.revision) {
            tooltipRevision = ClientPurseState.revision
            button.setTooltip(Tooltip.create(tooltip()))
        }
    }

    private val tooltipFont = ResourceLocation.fromNamespaceAndPath("coinage", "purse_coins")

    fun tooltip(): Component {
        val tooltip = Component.empty().append(Component.translatable("container.coinage.purse").withStyle(net.minecraft.ChatFormatting.GOLD))
        var remaining = ClientPurseState.balance
        for (tier in CoinTier.entries.reversed()) {
            val unit = ClientPurseState.rates.value(tier)
            val count = remaining / unit
            remaining %= unit
            if (count > 0 || (tier == CoinTier.BRONZE && ClientPurseState.balance == 0L)) {
                val icon = (0xe000 + tier.ordinal).toChar()
                val spacing = if (tier == CoinTier.PLATINUM) '\ue005' else '\ue004'
                tooltip.append(Component.literal("\n"))
                    .append(Component.literal("$icon$spacing").withStyle { it.withFont(tooltipFont).withColor(0xffffff) })
                    .append(Component.literal(numbers.format(count)).withStyle(net.minecraft.ChatFormatting.GRAY))
            }
        }
        return tooltip
    }
}
