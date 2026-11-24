package net.bms.coinage

import com.mojang.serialization.MapCodec
import net.minecraft.core.BlockPos
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

class ShopBlock(val pawn: Boolean, val unlimited: Boolean) : BaseEntityBlock(BlockBehaviour.Properties.of().strength(3f, 6f)) {
    override fun codec(): MapCodec<ShopBlock> = simpleCodec { ShopBlock(pawn, unlimited) }
    override fun getRenderShape(state: BlockState) = RenderShape.MODEL
    override fun newBlockEntity(pos: BlockPos, state: BlockState) = ShopEntity(pos, state)

    override fun setPlacedBy(level: Level, pos: BlockPos, state: BlockState, placer: LivingEntity?, stack: ItemStack) {
        if (placer is Player) (level.getBlockEntity(pos) as? ShopEntity)?.claim(placer)
    }

    override fun useWithoutItem(state: BlockState, level: Level, pos: BlockPos, player: Player, hit: BlockHitResult): InteractionResult {
        if (!level.isClientSide) {
            val shop = level.getBlockEntity(pos) as? ShopEntity ?: return InteractionResult.PASS
            shop.claim(player)
            player.openMenu(shop)
        }
        return InteractionResult.sidedSuccess(level.isClientSide)
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, replacement: BlockState, moving: Boolean) {
        if (state.block != replacement.block) {
            (level.getBlockEntity(pos) as? ShopEntity)?.let { shop ->
                Containers.dropContents(level, pos, shop.stock)
                if (!level.isClientSide && shop.storedCoins > 0) Containers.dropItemStack(level, pos.x + .5, pos.y + .5, pos.z + .5, MoneyBagItem.create(shop.storedCoins))
            }
        }
        super.onRemove(state, level, pos, replacement, moving)
    }
}
