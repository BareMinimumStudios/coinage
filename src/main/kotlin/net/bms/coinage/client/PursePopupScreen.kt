package net.bms.coinage.client

import com.mojang.blaze3d.systems.RenderSystem
import org.lwjgl.opengl.GL11
import net.bms.coinage.ClientPurseState
import net.bms.coinage.CoinTier
import net.bms.coinage.PurseSelection
import net.bms.coinage.PurseWithdrawPayloads
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component

internal class PursePopupScreen(val parent: AbstractContainerScreen<*>) : Screen(Component.translatable("container.coinage.purse")) {
    private val client get() = Minecraft.getInstance()
    private val menu = parent.menu
    private val selection = PurseSelection()
    private val selectors = mutableListOf<Triple<CoinTier, Button, Button>>()
    private lateinit var purse: Button
    private var tooltipRevision = -1L
    private lateinit var extract: Button
    private lateinit var bag: Button
    private var pending: Int? = null
    private var sentAt = 0L
    private var error = Component.empty()
    private var left = 0
    private var top = 0

    override fun init() {
        if (parent.width != width || parent.height != height) parent.resize(client, width, height)
        val anchor = PursePopupClient.anchor(parent)
        left = (anchor.first - 36).coerceIn(0, (width - 43).coerceAtLeast(0))
        top = (anchor.second + 15).coerceIn(0, (height - 99).coerceAtLeast(0))
        purse = addRenderableWidget(PurseSpriteButton(anchor.first, anchor.second, 11, 13, title, 62, 0) {
            if (PurseDraw.shiftDown() && PursePopupClient.quickAvailable()) deposit() else onClose()
        })
        tooltipRevision = -1L
        selectors.clear()
        for ((index, tier) in CoinTier.entries.reversed().withIndex()) {
            fun adjust(change: Int) {
                selection.adjust(tier, change * if (PurseDraw.shiftDown()) 10 else 1,
                    ClientPurseState.balance, ClientPurseState.rates)
                error = Component.empty()
                updateButtons()
            }
            val row = top + 10 + index * 12
            val name = Component.translatable("item.coinage.${tier.name.lowercase(java.util.Locale.ROOT)}_coin")
            val up = addRenderableWidget(PurseSpriteButton(left + 24, row, 9, 5,
                Component.translatable("gui.coinage.purse_increase", name), 37, 24) { adjust(1) })
            val down = addRenderableWidget(PurseSpriteButton(left + 24, row + 6, 9, 5,
                Component.translatable("gui.coinage.purse_decrease", name), 46, 24) { adjust(-1) })
            up.setTooltip(Tooltip.create(Component.translatable("gui.coinage.purse_step", name)))
            down.setTooltip(Tooltip.create(Component.translatable("gui.coinage.purse_step", name)))
            selectors.add(Triple(tier, up, down))
        }
        extract = addRenderableWidget(PurseSpriteButton(left + 3, top + 70, 24, 8,
            Component.translatable("gui.coinage.withdraw_coins"), 37, 0) { submit(false) })
        extract.setTooltip(Tooltip.create(Component.translatable("gui.coinage.purse_extract_hint")))
        bag = addRenderableWidget(PurseSpriteButton(left + 29, top + 69, 11, 13,
            Component.translatable("gui.coinage.withdraw_bag"), 62, 0) { submit(true) })
        bag.setTooltip(Tooltip.create(Component.translatable("gui.coinage.purse_bag_hint")))
        addRenderableWidget(Button.builder(Component.literal("...")) {
            client.setScreen(PurseAmountScreen(parent))
        }.bounds(left + 3, top + 85, 24, 12)
            .tooltip(Tooltip.create(Component.translatable("gui.coinage.purse_amount_title"))).build())
        updateButtons()
    }

    private fun updateButtons() {
        if (tooltipRevision != ClientPurseState.revision) {
            tooltipRevision = ClientPurseState.revision
            purse.setTooltip(Tooltip.create(PursePopupClient.tooltip()))
        }
        selection.limit(ClientPurseState.balance, ClientPurseState.rates)
        val ready = pending == null && ClientPurseState.ready && PursePopupClient.selectionAvailable()
        val total = selection.total(ClientPurseState.rates)
        for ((tier, up, down) in selectors) {
            up.active = ready && selection[tier] < 100 && ClientPurseState.balance - total >= ClientPurseState.rates.value(tier)
            down.active = ready && selection[tier] > 0
        }
        extract.active = ready && (total > 0 || (PurseDraw.shiftDown() && PurseDraw.controlDown() && ClientPurseState.balance > 0))
        bag.active = ready && total > 0
    }

    private fun deposit() {
        if (pending != null || !PursePopupClient.quickAvailable()) return
        val request = PurseAmountClient.nextRequest()
        pending = request
        sentAt = System.nanoTime()
        error = Component.empty()
        PursePopupClient.quick(PurseWithdrawPayloads.Quick(menu.containerId, request, 0))
        updateButtons()
    }

    private fun submit(asBag: Boolean) {
        if (pending != null || !PursePopupClient.selectionAvailable()) return
        val all = !asBag && PurseDraw.shiftDown() && PurseDraw.controlDown()
        if (all && !PursePopupClient.quickAvailable()) return
        val total = selection.total(ClientPurseState.rates)
        if (!all && total <= 0) return
        val request = PurseAmountClient.nextRequest()
        pending = request
        sentAt = System.nanoTime()
        error = Component.empty()
        if (all) PursePopupClient.quick(PurseWithdrawPayloads.Quick(menu.containerId, request, 1))
        else PursePopupClient.selected(PurseWithdrawPayloads.Selected(menu.containerId, request, CoinTier.entries.map { selection[it] }, asBag))
        updateButtons()
    }

    fun receive(reply: PurseWithdrawPayloads.Result) {
        if (reply.containerId != menu.containerId || reply.requestId != pending) return
        pending = null
        if (reply.code != 0) error = Component.translatable("gui.coinage.purse_withdraw_error_" + reply.code).withStyle(ChatFormatting.RED)
        updateButtons()
    }

    override fun tick() {
        if (client.player?.containerMenu !== menu || client.player?.isAlive != true) {
            client.setScreen(null)
            return
        }
        parent.tick()
        if (pending != null && System.nanoTime() - sentAt > 5_000_000_000L) {
            pending = null
            error = Component.translatable("gui.coinage.purse_withdraw_timeout").withStyle(ChatFormatting.RED)
        }
        updateButtons()
    }

    override fun onClose() { client.setScreen(if (client.player?.containerMenu === menu) parent else null) }
    override fun isPauseScreen() = false
    override fun renderBackground(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {}

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (super.mouseClicked(mouseX, mouseY, button)) return true
        if (mouseX !in left.toDouble()..(left + 43).toDouble() || mouseY !in top.toDouble()..(top + 99).toDouble()) onClose()
        return true
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (PurseClient.key.matches(keyCode, scanCode)) { onClose(); return true }
        if (client.options.keyInventory.matches(keyCode, scanCode)) {
            onClose()
            parent.onClose()
            return true
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        parent.render(graphics, -1000, -1000, partialTick)
        graphics.flush()
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX)
        graphics.pose().pushPose()
        graphics.pose().translate(0f, 0f, 300f)
        PurseDraw.background(graphics, left, top)
        for ((index, tier) in CoinTier.entries.reversed().withIndex()) {
            graphics.drawString(font, selection[tier].toString(), left + 5, top + 12 + index * 12, 0xFFFFFFFF.toInt(), false)
        }
        if (error.string.isNotEmpty()) {
            val x = (left - 190).coerceAtLeast(4)
            val y = top
            val lines = font.split(error, 180)
            graphics.fill(x - 3, y - 3, x + 183, y + lines.size * 10 + 3, 0xF21A1A1A.toInt())
            lines.forEachIndexed { index, line -> graphics.drawString(font, line, x, y + index * 10, 0xFFFFFFFF.toInt()) }
        }
        super.render(graphics, mouseX, mouseY, partialTick)
        graphics.pose().popPose()
    }
}
