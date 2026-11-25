package net.bms.coinage.client

import net.bms.coinage.CoinTier
import net.bms.coinage.MOD_ID
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

internal object PurseDraw {
    val texture = ResourceLocation.fromNamespaceAndPath(MOD_ID, "textures/gui/purse_widget.png")
    private val panel = ResourceLocation.fromNamespaceAndPath(MOD_ID, "textures/gui/purse_panel.png")
    private val coins = CoinTier.entries.associateWith {
        ResourceLocation.fromNamespaceAndPath(MOD_ID, "textures/item/${it.name.lowercase(java.util.Locale.ROOT)}_coin.png")
    }

    fun region(graphics: GuiGraphics, x: Int, y: Int, u: Int, v: Int, width: Int, height: Int,
        targetWidth: Int = width, targetHeight: Int = height) {
        graphics.blit(texture, x, y, targetWidth, targetHeight, u.toFloat(), v.toFloat(), width, height, 128, 64)
    }

    fun background(graphics: GuiGraphics, x: Int, y: Int) {
        graphics.blit(panel, x, y, 0f, 0f, 43, 84, 64, 128)
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
            graphics.blit(coins.getValue(tier), spriteX, spriteY, u, v, spriteWidth, spriteHeight, 16, 16)
        }
    }

    fun shiftDown() = net.minecraft.client.gui.screens.Screen.hasShiftDown()
    fun controlDown() = net.minecraft.client.gui.screens.Screen.hasControlDown()
}

internal class PurseSpriteButton(x: Int, y: Int, width: Int, height: Int, text: Component,
    private val u: Int, private val v: Int, press: () -> Unit) :
    Button(x, y, width, height, text, OnPress { press() }, DEFAULT_NARRATION) {
    override fun renderWidget(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        val state = if (active && isHoveredOrFocused) 1 else 0
        PurseDraw.region(graphics, x, y, u, v + state * height, width, height)
    }
}
