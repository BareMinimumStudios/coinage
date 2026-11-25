package net.bms.coinage.client

import net.bms.coinage.ShopEditPayloads
import net.bms.coinage.ShopEditorData
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import org.lwjgl.glfw.GLFW

internal class ShopEditScreen(private val parent: AbstractContainerScreen<*>) :
    Screen(Component.translatable("gui.coinage.edit_offers")) {
    private val shopMenu = parent.menu
    private val client get() = Minecraft.getInstance()
    private lateinit var number: EditBox
    private lateinit var price: EditBox
    private lateinit var save: Button
    private lateinit var delete: Button
    private lateinit var source: Button
    private var useExisting = false
    private var pending: Int? = null
    private var sentAt = 0L
    private var status = Component.empty()
    private var savedNumber = ShopEditorData.nextOffer(shopMenu.slots[53].item).toString()
    private var savedPrice = "silver"
    private val existing get() = shopMenu.slots.take(9).map { it.item }.filter { ShopEditorData.offerNumber(it) > 0 }
    private val selected get() = existing.firstOrNull { ShopEditorData.offerNumber(it) == number.value.toIntOrNull() }
    private val currentItem get(): ItemStack = if (useExisting) selected ?: ItemStack.EMPTY
        else shopMenu.carried.takeUnless { it.isEmpty } ?: client.player?.mainHandItem ?: ItemStack.EMPTY

    override fun init() {
        val left = (width - 288) / 2
        val top = (height - 228) / 2
        number = addRenderableWidget(EditBox(font, left + 12, top + 42, 86, 20,
            Component.translatable("gui.coinage.offer_number"))).apply {
            setMaxLength(10)
            setFilter { value -> value.all(Char::isDigit) }
            value = savedNumber
            setResponder {
                savedNumber = it
                selected?.let { offer ->
                    price.value = ShopEditorData.offerPrice(offer).toString()
                    useExisting = true
                }
                updateSource()
            }
        }
        price = addRenderableWidget(EditBox(font, left + 12, top + 80, 264, 20,
            Component.translatable("gui.coinage.offer_price"))).apply {
            setMaxLength(ShopEditPayloads.MAX_EXPRESSION)
            value = savedPrice
            setResponder { savedPrice = it }
        }
        addRenderableWidget(Button.builder(Component.literal("<")) { select(-1) }
            .bounds(left + 104, top + 42, 22, 20).build())
        addRenderableWidget(Button.builder(Component.literal(">")) { select(1) }
            .bounds(left + 130, top + 42, 22, 20).build())
        addRenderableWidget(Button.builder(Component.translatable("gui.coinage.new_offer")) {
            number.value = ShopEditorData.nextOffer(shopMenu.slots[53].item).toString()
            price.value = "silver"
            useExisting = false
            updateSource()
        }.bounds(left + 158, top + 42, 118, 20).build())
        source = addRenderableWidget(Button.builder(Component.empty()) {
            useExisting = !useExisting
            updateSource()
        }.bounds(left + 12, top + 146, 264, 20).build())
        save = addRenderableWidget(Button.builder(Component.translatable("gui.coinage.save_offer")) { submit(false) }
            .bounds(left + 12, top + 172, 84, 20).build())
        delete = addRenderableWidget(Button.builder(Component.translatable("gui.coinage.delete_offer").withStyle(ChatFormatting.RED)) { submit(true) }
            .bounds(left + 102, top + 172, 84, 20).build())
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel")) { onClose() }
            .bounds(left + 192, top + 172, 84, 20).build())
        updateSource()
        setInitialFocus(price)
    }

    private fun updateSource() {
        if (::source.isInitialized) source.message = Component.translatable(
            if (useExisting) "gui.coinage.existing_template" else "gui.coinage.cursor_template")
    }

    private fun select(direction: Int) {
        val offers = existing
        if (offers.isEmpty()) return
        val current = offers.indexOfFirst { ShopEditorData.offerNumber(it) == number.value.toIntOrNull() }
        val index = if (current < 0) 0 else Math.floorMod(current + direction, offers.size)
        number.value = ShopEditorData.offerNumber(offers[index]).toString()
    }

    private fun submit(removing: Boolean) {
        val offer = number.value.toIntOrNull()?.takeIf { it > 0 } ?: return
        if (pending != null || !ShopClient.available()) return
        val request = ShopClient.nextRequest()
        pending = request
        sentAt = System.nanoTime()
        status = Component.translatable("gui.coinage.saving_offer")
        ShopClient.send(ShopEditPayloads.Edit(shopMenu.containerId, request, offer,
            if (removing) "" else price.value, useExisting, removing))
    }

    fun receive(reply: ShopEditPayloads.Result) {
        if (reply.containerId != shopMenu.containerId || reply.requestId != pending) return
        pending = null
        if (reply.code == 0) onClose()
        else status = Component.translatable("gui.coinage.shop_edit_error_" + reply.code).withStyle(ChatFormatting.RED)
    }

    override fun tick() {
        if (client.player?.containerMenu !== shopMenu) {
            client.setScreen(null)
            return
        }
        if (pending != null && System.nanoTime() - sentAt > 5_000_000_000L) {
            pending = null
            status = Component.translatable("gui.coinage.shop_edit_timeout").withStyle(ChatFormatting.RED)
        }
        val ready = pending == null && ShopClient.available() && number.value.toIntOrNull()?.let { it > 0 } == true
        save.active = ready && price.value.isNotBlank()
        delete.active = ready
        source.active = pending == null
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) &&
            (number.isFocused || price.isFocused) && save.active) {
            submit(false)
            return true
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun onClose() {
        client.setScreen(if (client.player?.containerMenu === shopMenu) parent else null)
    }

    override fun isPauseScreen() = false

    // The inherited render calls this after our panel; draw its blur first instead.
    override fun renderBackground(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {}

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick)
        val left = (width - 288) / 2
        val top = (height - 228) / 2
        graphics.fill(0, 0, width, height, 0x99000000.toInt())
        graphics.fill(left, top, left + 288, top + 228, 0xF21A1A1A.toInt())
        graphics.fill(left, top, left + 288, top + 2, 0xFFF2F2F2.toInt())
        graphics.drawString(font, title, left + 12, top + 12, 0xFFF2F2F2.toInt())
        graphics.drawString(font, Component.translatable("gui.coinage.offer_number"), left + 12, top + 30, 0xFFF2F2F2.toInt())
        graphics.drawString(font, Component.translatable("gui.coinage.offer_price"), left + 12, top + 68, 0xFFF2F2F2.toInt())
        graphics.drawString(font, Component.translatable("gui.coinage.offer_template"), left + 12, top + 106, 0xFFF2F2F2.toInt())
        val item = currentItem
        if (!item.isEmpty) {
            graphics.renderItem(item, left + 12, top + 121)
            graphics.drawString(font, font.plainSubstrByWidth("${item.count} × ${item.hoverName.string}", 218), left + 34, top + 125, 0xFFF2F2F2.toInt())
        } else graphics.drawString(font, Component.translatable(if (useExisting)
            "gui.coinage.existing_offer_hint" else "gui.coinage.empty_template_hint"), left + 12, top + 125, 0xFFB8B8B8.toInt())
        font.split(status, 264).take(2).forEachIndexed { index, line ->
            graphics.drawString(font, line, left + 12, top + 201 + index * 10, 0xFFF2F2F2.toInt())
        }
        super.render(graphics, mouseX, mouseY, partialTick)
    }
}
