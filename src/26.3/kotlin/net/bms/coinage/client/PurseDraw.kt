package net.bms.coinage.client

import net.bms.coinage.MOD_ID
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier

internal object PurseDraw {
    private val texture = Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/purse_widget.png")
    private val panel = Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/purse_panel.png")

    fun region(graphics: GuiGraphicsExtractor, x: Int, y: Int, u: Int, v: Int, width: Int, height: Int) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u.toFloat(), v.toFloat(), width, height, width, height, 128, 64)
    }

    fun background(graphics: GuiGraphicsExtractor, x: Int, y: Int) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, panel, x, y, 0f, 0f, 43, 71, 64, 128)
    }

    fun shiftDown() = Minecraft.getInstance().hasShiftDown()
    fun controlDown() = Minecraft.getInstance().hasControlDown()
}

internal class PurseSpriteButton(x: Int, y: Int, width: Int, height: Int, text: Component,
    private val u: Int, private val v: Int, press: () -> Unit) :
    Button(x, y, width, height, text, OnPress { press() }, DEFAULT_NARRATION) {
    override fun extractContents(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        val state = if (active && isHoveredOrFocused) 1 else 0
        PurseDraw.region(graphics, x, y, u, v + state * height, width, height)
    }
}
