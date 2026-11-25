package net.bms.coinage.client

import net.bms.coinage.CoinTier
import net.bms.coinage.MOD_ID
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier

internal object PurseDraw {
    val texture = Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/purse_widget.png")
    private val panel = Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/purse_panel.png")
    private val coins = CoinTier.entries.associateWith {
        Identifier.fromNamespaceAndPath(MOD_ID, "textures/item/${it.name.lowercase(java.util.Locale.ROOT)}_coin.png")
    }

    fun region(graphics: GuiGraphicsExtractor, x: Int, y: Int, u: Int, v: Int, width: Int, height: Int,
        targetWidth: Int = width, targetHeight: Int = height) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u.toFloat(), v.toFloat(), targetWidth, targetHeight, width, height, 128, 64)
    }

    fun background(graphics: GuiGraphicsExtractor, x: Int, y: Int) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, panel, x, y, 0f, 0f, 43, 84, 64, 128)
        for ((index, tier) in CoinTier.entries.reversed().withIndex()) {
            val row = y + 10 + index * 12
            val spriteWidth = when (tier) {
                CoinTier.BRONZE, CoinTier.SILVER -> 7
                CoinTier.GOLD -> 8
                CoinTier.PLATINUM -> 9
            }
            val spriteHeight = when (tier) {
                CoinTier.BRONZE -> 7
                CoinTier.SILVER, CoinTier.GOLD -> 8
                CoinTier.PLATINUM -> 9
            }
            val u = if (spriteWidth == 7) 5f else 4f
            val v = if (tier == CoinTier.BRONZE) 5f else 4f
            val spriteX = x + 33 + (10 - spriteWidth) / 2
            val spriteY = row + (11 - spriteHeight) / 2
            graphics.blit(RenderPipelines.GUI_TEXTURED, coins.getValue(tier), spriteX, spriteY, u, v, spriteWidth, spriteHeight, 16, 16)
        }
    }

    fun shiftDown() = net.minecraft.client.Minecraft.getInstance().hasShiftDown()
    fun controlDown() = net.minecraft.client.Minecraft.getInstance().hasControlDown()
}

internal class PurseSpriteButton(x: Int, y: Int, width: Int, height: Int, text: Component,
    private val u: Int, private val v: Int, press: () -> Unit) :
    Button(x, y, width, height, text, OnPress { press() }, DEFAULT_NARRATION) {
    override fun extractContents(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        val state = if (active && isHoveredOrFocused) 1 else 0
        PurseDraw.region(graphics, x, y, u, v + state * height, width, height)
    }
}
