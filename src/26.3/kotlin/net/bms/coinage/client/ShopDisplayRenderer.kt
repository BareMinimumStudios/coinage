package net.bms.coinage.client

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.bms.coinage.ShopEntity
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState
import net.minecraft.client.renderer.feature.ModelFeatureRenderer
import net.minecraft.client.renderer.item.ItemStackRenderState
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.util.LightCoordsUtil
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.phys.Vec3

internal class ShopDisplayRenderer(context: BlockEntityRendererProvider.Context) : BlockEntityRenderer<ShopEntity, ShopDisplayRenderer.State> {
    private val models = context.itemModelResolver()

    class State : BlockEntityRenderState() {
        val item = ItemStackRenderState()
        var isBlock = false
        var rotation = 0f
    }

    override fun createRenderState() = State()

    override fun extractRenderState(shop: ShopEntity, state: State, partialTick: Float, camera: Vec3, breaking: ModelFeatureRenderer.CrumblingOverlay?) {
        super.extractRenderState(shop, state, partialTick, camera, breaking)
        val stack = shop.displayStack
        val level = shop.level
        models.updateForTopItem(state.item, stack, ItemDisplayContext.GROUND, level, null, 0)
        if (level != null && !stack.isEmpty) state.lightCoords = LightCoordsUtil.getLightCoords(level, shop.blockPos.above())
        state.isBlock = stack.item is BlockItem
        state.rotation = (((level?.gameTime ?: 0L) % 144L) + partialTick) * 2.5f
    }

    override fun submit(state: State, pose: PoseStack, nodes: SubmitNodeCollector, camera: CameraRenderState) {
        if (state.item.isEmpty) return
        val scale = if (state.isBlock) .95f else .85f
        pose.pushPose()
        pose.translate(.5, if (state.isBlock) .85 else .95, .5)
        pose.scale(scale, scale, scale)
        pose.rotateDegrees(Axis.YP, state.rotation)
        state.item.submit(pose, nodes, state.lightCoords, OverlayTexture.NO_OVERLAY, 0)
        pose.popPose()
    }
}
