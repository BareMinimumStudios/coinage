package net.bms.coinage

import net.minecraft.world.phys.shapes.VoxelShape

import net.minecraft.world.phys.shapes.CollisionContext

import net.minecraft.world.level.block.Block

import net.minecraft.world.level.BlockGetter

import com.mojang.serialization.MapCodec
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.Containers
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult

class ShopBlock(val pawn: Boolean, val unlimited: Boolean, name: String) : BaseEntityBlock(BlockBehaviour.Properties.of().strength(3f, 6f).setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, name)))) {
    override fun getShape(
        state: BlockState, level: BlockGetter, pos: BlockPos,
        context: CollisionContext
    ): VoxelShape = SHAPE

    override fun getRenderShape(state: BlockState) = RenderShape.MODEL
    override fun newBlockEntity(pos: BlockPos, state: BlockState) = ShopEntity(pos, state)

    override fun setPlacedBy(level: Level, pos: BlockPos, state: BlockState, placer: LivingEntity?, stack: ItemStack) {
        if (placer is Player) (level.getBlockEntity(pos) as? ShopEntity)?.claim(placer)
    }

    override fun useWithoutItem(state: BlockState, level: Level, pos: BlockPos, player: Player, hit: BlockHitResult): InteractionResult {
        if (!level.isClientSide()) {
            val shop = level.getBlockEntity(pos) as? ShopEntity ?: return InteractionResult.PASS
            shop.claim(player)
            player.openMenu(shop)
        }
        return InteractionResult.SUCCESS
    }
    companion object {
        private val SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 12.0, 16.0)
    }
}
