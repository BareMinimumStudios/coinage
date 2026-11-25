package net.bms.coinage.client

import net.bms.coinage.ShopEditPayloads
import net.bms.coinage.ShopEditorData
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.component.CustomData
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.world.inventory.MenuType

internal object ShopClient {
    var send: (ShopEditPayloads.Edit) -> Unit = {}
    var available: () -> Boolean = { false }
    private var attached: Pair<Screen, Button>? = null
    private var requestId = 0
    private var controlData: CustomData? = null
    private var canManage = false

    fun initialize() {
        ShopEditPayloads.receive = { reply ->
            (Minecraft.getInstance().screen as? ShopEditScreen)?.receive(reply)
        }
    }

    fun nextRequest(): Int = ++requestId

    fun buttons(screen: Screen): List<Button> {
        if (screen !is AbstractContainerScreen<*> || screen.menu.type != MenuType.GENERIC_9x6 || screen.menu.slots.size != 90)
            return emptyList()
        return listOf(Button.builder(Component.translatable("gui.coinage.edit_offers")) {
            if (available() && ShopEditorData.canManage(screen.menu.slots[53].item))
                Minecraft.getInstance().setScreen(ShopEditScreen(screen))
        }.bounds(screen.width / 2 - 80, (screen.height - 222) / 2 + 90, 160, 16).build().apply {
            visible = false
            active = false
            attached = screen to this
            controlData = null
            canManage = false
        })
    }

    fun tick() {
        val (screen, button) = attached ?: return
        if (Minecraft.getInstance().screen !== screen) {
            attached = null
            return
        }
        val menu = (screen as AbstractContainerScreen<*>).menu
        val control = menu.slots[53].item
        val data = control.get(DataComponents.CUSTOM_DATA)
        if (data !== controlData) {
            controlData = data
            canManage = ShopEditorData.canManage(control)
        }
        button.visible = available() && canManage
        button.active = button.visible
    }
}
