package net.bms.coinage

import com.mojang.serialization.MapCodec
import java.util.Locale.ROOT
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.core.NonNullList
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.world.ContainerHelper
import net.minecraft.world.Containers
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.InteractionResult
import net.minecraft.world.inventory.ChestMenu
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.DyeColor
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.Level
import net.minecraft.world.MenuProvider
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.SimpleContainer

class PiggyBankBlock : BaseEntityBlock(BlockBehaviour.Properties.of().strength(1.0f).noOcclusion()) {
    override fun codec(): MapCodec<PiggyBankBlock> = CODEC
    override fun getRenderShape(state: BlockState) = RenderShape.MODEL
    override fun newBlockEntity(pos: BlockPos, state: BlockState) = PiggyBankEntity(pos, state)

    override fun useWithoutItem(state: BlockState, level: Level, pos: BlockPos, player: Player, hit: BlockHitResult): InteractionResult {
        if (!level.isClientSide) {
            val bank = level.getBlockEntity(pos) as? PiggyBankEntity ?: return InteractionResult.PASS
            player.openMenu(bank)
        }
        return InteractionResult.sidedSuccess(level.isClientSide)
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, replacement: BlockState, moving: Boolean) {
        if (state.block != replacement.block) {
            (level.getBlockEntity(pos) as? PiggyBankEntity)?.let { Containers.dropContents(level, pos, it.inventory) }
        }
        super.onRemove(state, level, pos, replacement, moving)
    }

    companion object {
        val CODEC: MapCodec<PiggyBankBlock> = simpleCodec { PiggyBankBlock() }
    }
}

class PiggyBankEntity(pos: BlockPos, state: BlockState) : BlockEntity(CoinageBlocks.piggyType, pos, state), MenuProvider {
    val inventory = object : SimpleContainer(27) {
        override fun setChanged() {
            super.setChanged()
            this@PiggyBankEntity.setChanged()
        }
        override fun canPlaceItem(slot: Int, stack: ItemStack): Boolean =
            stack.item is CoinItem || stack.item is MoneyBagItem
        override fun stillValid(player: Player): Boolean =
            level?.getBlockEntity(worldPosition) === this@PiggyBankEntity &&
                player.distanceToSqr(worldPosition.x + 0.5, worldPosition.y + 0.5, worldPosition.z + 0.5) <= 64
    }

    override fun getDisplayName(): Component = Component.translatable("container.coinage.piggy_bank")
    override fun createMenu(id: Int, playerInventory: Inventory, player: Player) = ChestMenu.threeRows(id, playerInventory, inventory)

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        val stacks = NonNullList.withSize(27, ItemStack.EMPTY)
        for (slot in stacks.indices) stacks[slot] = inventory.getItem(slot)
        ContainerHelper.saveAllItems(tag, stacks, registries)
    }

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        val stacks = NonNullList.withSize(27, ItemStack.EMPTY)
        ContainerHelper.loadAllItems(tag, stacks, registries)
        for (slot in stacks.indices) inventory.setItem(slot, stacks[slot])
    }
}

object CoinageBlocks {
    val piggyBanks = linkedMapOf<String, PiggyBankBlock>().apply {
        put("piggy_bank", PiggyBankBlock())
        DyeColor.entries.forEach { put(it.name.lowercase(ROOT) + "_piggy_bank", PiggyBankBlock()) }
    }
    val items = piggyBanks.mapValues { (_, block) -> BlockItem(block, Item.Properties().stacksTo(1)) }
    lateinit var piggyType: BlockEntityType<PiggyBankEntity>
}
