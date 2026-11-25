package net.bms.coinage.client

import net.bms.coinage.PurseWithdrawPayloads
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.item.component.CustomData

internal object PurseAmountClient {
    var send: (PurseWithdrawPayloads.Withdraw) -> Unit = {}
    var available: () -> Boolean = { false }
    private var attached: Pair<Screen, Button>? = null
    private var controlData: CustomData? = null
    private var marked = false
    private var requestId = 0

    fun initialize() {
        PurseWithdrawPayloads.receive = { reply ->
            (Minecraft.getInstance().gui.screen() as? PurseAmountScreen)?.receive(reply)
        }
    }

    fun nextRequest(): Int {
        requestId = if (requestId == Int.MAX_VALUE) 1 else requestId + 1
        return requestId
    }

    fun buttons(screen: Screen): List<Button> {
        if (screen !is AbstractContainerScreen<*> || screen.menu.slots.size != 63)
            return emptyList()
        return listOf(Button.builder(Component.translatable("gui.coinage.purse_amount")) {
            if (available() && marked) Minecraft.getInstance().gui.setScreen(PurseAmountScreen(screen))
        }.bounds(screen.width / 2 - 80, (screen.height - 168) / 2 + 18, 54, 16).build().apply {
            visible = false
            active = false
            attached = screen to this
            controlData = null
            marked = false
        })
    }

    fun tick() {
        val (screen, button) = attached ?: return
        if (Minecraft.getInstance().gui.screen() !== screen) {
            attached = null
            return
        }
        val data = (screen as AbstractContainerScreen<*>).menu.slots[26].item.get(DataComponents.CUSTOM_DATA)
        if (data !== controlData) {
            controlData = data
            marked = data?.copyTag()?.contains("coinage_purse") == true
        }
        button.visible = available() && marked
        button.active = button.visible
    }
}
