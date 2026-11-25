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
    private val coins = CoinTier.entries.associateWith {
        Identifier.fromNamespaceAndPath(MOD_ID, "textures/item/${it.name.lowercase(java.util.Locale.ROOT)}_coin.png")
    }

    fun region(graphics: GuiGraphicsExtractor, x: Int, y: Int, u: Int, v: Int, width: Int, height: Int,
        targetWidth: Int = width, targetHeight: Int = height) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u.toFloat(), v.toFloat(), targetWidth, targetHeight, width, height, 128, 64)
    }

    fun background(graphics: GuiGraphicsExtractor, x: Int, y: Int) {
        fun band(sourceY: Int, height: Int, targetY: Int) {
            region(graphics, x, targetY, 0, sourceY, 17, height)
            region(graphics, x + 17, targetY, 16, sourceY, 1, height, 6)
            region(graphics, x + 23, targetY, 17, sourceY, 20, height)
        }
        band(0, 10, y)
        repeat(CoinTier.entries.size) { band(10, 12, y + 10 + it * 12) }
        band(46, 13, y + 58)
        for ((index, tier) in CoinTier.entries.reversed().withIndex()) {
            val row = y + 10 + index * 12
            graphics.fill(x + 33, row + 1, x + 42, row + 11, 0xFFC6C6C6.toInt())
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
            val spriteX = x + 33 + (9 - spriteWidth) / 2
            val spriteY = row + (12 - spriteHeight) / 2
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
