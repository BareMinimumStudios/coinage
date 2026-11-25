package net.bms.coinage.client

import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava
import com.mojang.blaze3d.platform.InputConstants
import java.text.NumberFormat
import java.util.Locale
import net.bms.coinage.ClientPurseState
import net.bms.coinage.CoinageItems
import net.bms.coinage.CoinTier
import net.bms.coinage.PurseClientConfig
import net.bms.coinage.PurseNotifications
import net.minecraft.resources.Identifier
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack

internal object PurseClient {
    val key = KeyMapping("key.coinage.purse", InputConstants.Type.KEYBOARD, InputConstants.KEY_B, KeyMapping.Category(Identifier.fromNamespaceAndPath("coinage", "controls")))
    var requestOpen: () -> Unit = {}
    lateinit var config: PurseClientConfig
        private set
    private val numbers = NumberFormat.getIntegerInstance(Locale.ROOT)
    private var purseButton: Button? = null
    private var revision = -1L
    private var previousBalance: Long? = null
    private var entries = emptyList<Pair<ItemStack, String>>()

    fun initialize() { config = PurseClientConfig.register() }

    fun reset() {
        ClientPurseState.reset()
        previousBalance = null
        revision = -1
        entries = emptyList()
    }

    fun tick() {
        val client = Minecraft.getInstance()
        purseButton?.active = ClientPurseState.ready
        if (client.player == null) {
            if (ClientPurseState.ready) reset()
            while (key.consumeClick()) { }
            return
        }
        while (key.consumeClick()) {
            if (ClientPurseState.ready && (client.gui.screen() == null || client.gui.screen() is InventoryScreen)) requestOpen()
        }
        if (!ClientPurseState.ready || revision == ClientPurseState.revision) return
        val balance = ClientPurseState.balance
        val old = previousBalance
        if (old != null && old != balance && config.notifications != PurseNotifications.OFF) {
            val difference = balance - old
            val message = Component.translatable(if (difference > 0) "gui.coinage.gained" else "gui.coinage.lost",
                numbers.format(if (difference < 0) -difference else difference))
            if (config.notifications == PurseNotifications.ACTION_BAR) client.gui.hud.setOverlayMessage(message, false)
            else client.gui.hud.chat.addClientSystemMessage(message)
        }
        previousBalance = balance
        revision = ClientPurseState.revision
        var remaining = balance
        val display = mutableListOf<Pair<ItemStack, String>>()
        for (tier in CoinTier.entries.reversed()) {
            val denomination = ClientPurseState.rates.value(tier)
            val count = remaining / denomination
            remaining %= denomination
            if (count > 0 || (balance == 0L && tier == CoinTier.BRONZE)) {
                val item = CoinageItems.descending.first { it.tier == tier }
                display.add(ItemStack(item) to numbers.format(count))
            }
        }
        entries = display
    }

    fun inventoryButtons(screen: Screen): List<Button> {
        if (screen !is InventoryScreen || !config.inventoryButton) return emptyList()
        return listOf(
            Button.builder(Component.translatable("container.coinage.purse")) {
                if (ClientPurseState.ready) requestOpen()
            }.bounds(screen.width - 82, 6, 54, 20)
                .tooltip(Tooltip.create(Component.translatable("gui.coinage.open_purse"))).build().apply {
                    active = ClientPurseState.ready
                    purseButton = this
                },
            Button.builder(Component.literal("...")) {
                ConfigApiJava.openScreen("coinage")
            }.bounds(screen.width - 26, 6, 20, 20)
                .tooltip(Tooltip.create(Component.translatable("gui.coinage.configure"))).build()
        )
    }

    fun render(graphics: GuiGraphicsExtractor) {
        val client = Minecraft.getInstance()
        if (!ClientPurseState.ready || !config.showHud || client.gui.hud.isHidden || client.gui.screen() != null || entries.isEmpty()) return
        val font = client.font
        val panelWidth = entries.sumOf { 22 + font.width(it.second) } + 8
        val panelHeight = 24
        val x = 4 + ((graphics.guiWidth() - panelWidth - 8).coerceAtLeast(0) * config.hudX.get() / 100)
        val y = 4 + ((graphics.guiHeight() - panelHeight - 8).coerceAtLeast(0) * config.hudY.get() / 100)
        graphics.fill(x, y, x + panelWidth, y + panelHeight, 0xF21A1A1A.toInt())
        graphics.fill(x, y, x + panelWidth, y + 2, 0xFFF2F2F2.toInt())
        var cursor = x + 4
        for ((stack, count) in entries) {
            graphics.item(stack, cursor, y + 5)
            graphics.text(font, count, cursor + 18, y + 9, 0xFFF2F2F2.toInt(), true)
            cursor += 22 + font.width(count)
        }
    }
}
