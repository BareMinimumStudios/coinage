package net.bms.coinage.client

import net.bms.coinage.ClientPurseState
import net.bms.coinage.PurseWithdrawPayloads
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.client.input.KeyEvent

internal class PurseAmountScreen(private val parent: AbstractContainerScreen<*>) :
    Screen(Component.translatable("gui.coinage.purse_amount_title")) {
    private val purseMenu = parent.menu
    private val client get() = Minecraft.getInstance()
    private lateinit var amount: EditBox
    private lateinit var coins: Button
    private lateinit var bag: Button
    private var savedAmount = "iron"
    private var pending: Int? = null
    private var sentAt = 0L
    private var status = Component.empty()

    override fun init() {
        val left = (width - 288) / 2
        val top = (height - 176) / 2
        amount = addRenderableWidget(EditBox(font, left + 12, top + 62, 264, 20,
            Component.translatable("gui.coinage.purse_amount"))).apply {
            setMaxLength(PurseWithdrawPayloads.MAX_EXPRESSION)
            value = savedAmount
            setResponder { savedAmount = it }
        }
        coins = addRenderableWidget(Button.builder(Component.translatable("gui.coinage.withdraw_coins")) { submit(false) }
            .bounds(left + 12, top + 106, 84, 20).build())
        bag = addRenderableWidget(Button.builder(Component.translatable("gui.coinage.withdraw_bag")) { submit(true) }
            .bounds(left + 102, top + 106, 84, 20).build())
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel")) { onClose() }
            .bounds(left + 192, top + 106, 84, 20).build())
        setInitialFocus(amount)
    }

    private fun submit(asBag: Boolean) {
        if (pending != null || !PurseAmountClient.available() || amount.value.isBlank()) return
        val request = PurseAmountClient.nextRequest()
        pending = request
        sentAt = System.nanoTime()
        status = Component.translatable("gui.coinage.purse_withdrawing")
        PurseAmountClient.send(PurseWithdrawPayloads.Withdraw(purseMenu.containerId, request, amount.value, asBag))
    }

    fun receive(reply: PurseWithdrawPayloads.Result) {
        if (reply.containerId != purseMenu.containerId || reply.requestId != pending) return
        pending = null
        if (reply.code == 0) onClose()
        else status = Component.translatable("gui.coinage.purse_withdraw_error_" + reply.code).withStyle(ChatFormatting.RED)
    }

    override fun tick() {
        if (client.player?.containerMenu !== purseMenu) {
            client.gui.setScreen(null)
            return
        }
        if (pending != null && System.nanoTime() - sentAt > 5_000_000_000L) {
            pending = null
            status = Component.translatable("gui.coinage.purse_withdraw_timeout").withStyle(ChatFormatting.RED)
        }
        val ready = pending == null && PurseAmountClient.available() && amount.value.isNotBlank()
        coins.active = ready
        bag.active = ready
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        if (event.isConfirmation && amount.isFocused && coins.active) {
            submit(false)
            return true
        }
        return super.keyPressed(event)
    }

    override fun onClose() {
        client.gui.setScreen(if (client.player?.containerMenu === purseMenu) parent else null)
    }

    override fun isPauseScreen() = false

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        val left = (width - 288) / 2
        val top = (height - 176) / 2
        graphics.fill(0, 0, width, height, 0x99000000.toInt())
        graphics.fill(left, top, left + 288, top + 176, 0xF21A1A1A.toInt())
        graphics.fill(left, top, left + 288, top + 2, 0xFFF2F2F2.toInt())
        graphics.text(font, title, left + 12, top + 12, 0xFFF2F2F2.toInt())
        graphics.text(font, Component.translatable("gui.coinage.purse_balance", ClientPurseState.balance),
            left + 12, top + 30, 0xFFF2F2F2.toInt())
        graphics.text(font, Component.translatable("gui.coinage.purse_amount"), left + 12, top + 50, 0xFFF2F2F2.toInt())
        graphics.text(font, Component.translatable("gui.coinage.purse_expression_hint"), left + 12, top + 88, 0xFFB8B8B8.toInt())
        font.split(status, 264).take(3).forEachIndexed { index, line ->
            graphics.text(font, line, left + 12, top + 136 + index * 10, 0xFFF2F2F2.toInt())
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick)
    }
}
