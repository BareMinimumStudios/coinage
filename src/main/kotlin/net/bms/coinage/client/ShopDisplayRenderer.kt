package net.bms.coinage.client

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.bms.coinage.ShopEntity
import net.minecraft.client.renderer.LevelRenderer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemDisplayContext

internal class ShopDisplayRenderer(context: BlockEntityRendererProvider.Context) : BlockEntityRenderer<ShopEntity> {
    private val items = context.itemRenderer

    override fun render(shop: ShopEntity, partialTick: Float, pose: PoseStack, buffers: MultiBufferSource, light: Int, overlay: Int) {
        val level = shop.level ?: return
        val stack = shop.displayStack
        if (stack.isEmpty) return
        val block = stack.item is BlockItem
        val scale = if (block) .95f else .85f
        pose.pushPose()
        pose.translate(.5, if (block) .85 else .95, .5)
        pose.scale(scale, scale, scale)
        pose.mulPose(Axis.YP.rotationDegrees(((level.gameTime % 144L) + partialTick) * 2.5f))
        items.renderStatic(stack, ItemDisplayContext.GROUND, LevelRenderer.getLightColor(level, shop.blockPos.above()),
            OverlayTexture.NO_OVERLAY, pose, buffers, level, 0)
        pose.popPose()
    }
}
