package net.bms.coinage

import java.util.Locale.ROOT
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.NonNullList
import net.minecraft.core.component.DataComponentGetter
import net.minecraft.core.component.DataComponentMap
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.stats.Stats
import net.minecraft.world.ContainerHelper
import net.minecraft.world.InteractionResult
import net.minecraft.world.MenuProvider
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.item.FallingBlockEntity
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.DyeColor
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.ItemContainerContents
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Mirror
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape

class PiggyBankBlock(name: String) : BaseEntityBlock(BlockBehaviour.Properties.of().strength(1.0f).noOcclusion().setId(
    ResourceKey.create(
        Registries.BLOCK,
        Identifier.fromNamespaceAndPath(MOD_ID, name)
    )
)) {
    init {
        registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING)
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState =
        defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, context.horizontalDirection.opposite)

    override fun rotate(state: BlockState, rotation: Rotation): BlockState =
        state.setValue(BlockStateProperties.HORIZONTAL_FACING, rotation.rotate(state.getValue(BlockStateProperties.HORIZONTAL_FACING)))

    override fun mirror(state: BlockState, mirror: Mirror): BlockState =
        state.rotate(mirror.getRotation(state.getValue(BlockStateProperties.HORIZONTAL_FACING)))

    override fun getShape(
        state: BlockState, level: BlockGetter, pos: BlockPos,
        context: CollisionContext
    ): VoxelShape = SHAPES.getValue(state.getValue(BlockStateProperties.HORIZONTAL_FACING))

    override fun getRenderShape(state: BlockState) = RenderShape.MODEL
    override fun newBlockEntity(pos: BlockPos, state: BlockState) = PiggyBankEntity(pos, state)

    override fun useWithoutItem(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hit: BlockHitResult
    ): InteractionResult {
        if (!level.isClientSide()) {
            val bank = level.getBlockEntity(pos) as? PiggyBankEntity ?: return InteractionResult.PASS
            player.openMenu(bank)
        }
        return InteractionResult.SUCCESS
    }

    override fun playerWillDestroy(level: Level, pos: BlockPos, state: BlockState, player: Player): BlockState {
        val bank = level.getBlockEntity(pos) as? PiggyBankEntity
        if (!level.isClientSide() && player.isCreative && bank != null && PiggyBankEffects.isHammer(player.mainHandItem)) {
            PiggyBankEffects.smash(level, pos, state, bank)
            return super.playerWillDestroy(level, pos, state, player)
        }
        if (!level.isClientSide() && player.isCreative && bank != null && !bank.inventory.isEmpty) {
            val stack = ItemStack(this)
            stack.applyComponents(bank.collectComponents())
            val drop = ItemEntity(level, pos.x + 0.5, pos.y + 0.5, pos.z + 0.5, stack)
            drop.setDefaultPickUpDelay()
            level.addFreshEntity(drop)
        }
        return super.playerWillDestroy(level, pos, state, player)
    }

    override fun playerDestroy(
        level: ServerLevel, player: ServerPlayer, pos: BlockPos, state: BlockState,
        blockEntity: BlockEntity?, tool: ItemStack
    ) {
        if (blockEntity is PiggyBankEntity && PiggyBankEffects.isHammer(tool)) {
            player.awardStat(Stats.BLOCK_MINED.get(this))
            player.causeFoodExhaustion(.005f)
            PiggyBankEffects.smash(level, pos, state, blockEntity)
        } else super.playerDestroy(level, player, pos, state, blockEntity, tool)
    }

    override fun fallOn(level: Level, state: BlockState, pos: BlockPos, entity: Entity, distance: Double) {
        if (!level.isClientSide() && entity is FallingBlockEntity && entity.blockState.`is`(PiggyBankEffects.heavyBlocks)) {
            (level.getBlockEntity(pos) as? PiggyBankEntity)?.let { PiggyBankEffects.smash(level, pos, state, it) }
            level.removeBlock(pos, false)
        }
        super.fallOn(level, state, pos, entity, distance)
    }

    companion object {
        private val BODY = Block.box(5.0, 0.0, 5.0, 11.0, 6.0, 11.0)
        private val SHAPES = mapOf(
            Direction.NORTH to Shapes.or(BODY, Block.box(7.0, 2.0, 4.0, 9.0, 4.0, 5.0)),
            Direction.SOUTH to Shapes.or(BODY, Block.box(7.0, 2.0, 11.0, 9.0, 4.0, 12.0)),
            Direction.WEST to Shapes.or(BODY, Block.box(4.0, 2.0, 7.0, 5.0, 4.0, 9.0)),
            Direction.EAST to Shapes.or(BODY, Block.box(11.0, 2.0, 7.0, 12.0, 4.0, 9.0))
        )
    }
}

class PiggyBankEntity(pos: BlockPos, state: BlockState) : BlockEntity(CoinageBlocks.piggyType, pos, state),
    MenuProvider {
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

    override fun getDisplayName(): Component =
        components().get(DataComponents.CUSTOM_NAME) ?: Component.translatable("container.coinage.piggy_bank")
    override fun createMenu(id: Int, playerInventory: Inventory, player: Player): AbstractContainerMenu =
        PiggyBankMenu(id, playerInventory, inventory)

    override fun applyImplicitComponents(input: DataComponentGetter) {
        super.applyImplicitComponents(input)
        val contents = input.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
        val stacks = NonNullList.withSize(27, ItemStack.EMPTY)
        contents.copyInto(stacks)
        for (slot in stacks.indices) inventory.setItem(slot, stacks[slot])
    }

    override fun collectImplicitComponents(builder: DataComponentMap.Builder) {
        super.collectImplicitComponents(builder)
        builder.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(
            (0 until inventory.containerSize).map { inventory.getItem(it) }
        ))
    }

    override fun removeComponentsFromTag(tag: ValueOutput) {
        super.removeComponentsFromTag(tag)
        tag.discard("Items")
    }

    override fun saveAdditional(tag: ValueOutput) {
        super.saveAdditional(tag)
        val stacks = NonNullList.withSize(27, ItemStack.EMPTY)
        for (slot in stacks.indices) stacks[slot] = inventory.getItem(slot)
        ContainerHelper.saveAllItems(tag, stacks)
    }

    override fun loadAdditional(tag: ValueInput) {
        super.loadAdditional(tag)
        val stacks = NonNullList.withSize(27, ItemStack.EMPTY)
        ContainerHelper.loadAllItems(tag, stacks)
        for (slot in stacks.indices) inventory.setItem(slot, stacks[slot])
    }
}

object CoinageBlocks {
    val piggyBanks = linkedMapOf<String, PiggyBankBlock>().apply {
        put("piggy_bank", PiggyBankBlock("piggy_bank"))
        DyeColor.entries.forEach {
            val name = it.name.lowercase(ROOT) + "_piggy_bank"
            put(name, PiggyBankBlock(name))
        }
    }
    val items = piggyBanks.mapValues { (name, block) -> PiggyBankItem(block, CoinageItems.properties(name).useBlockDescriptionPrefix().stacksTo(1)) }
    val shops = linkedMapOf(
        "shop" to ShopBlock(false, false, "shop"),
        "pawn_shop" to ShopBlock(true, false, "pawn_shop"),
        "inexhaustible_shop" to ShopBlock(false, true, "inexhaustible_shop"),
        "inexhaustible_pawn_shop" to ShopBlock(true, true, "inexhaustible_pawn_shop")
    )
    val shopItems = shops.mapValues { (name, block) -> BlockItem(block, CoinageItems.properties(name).useBlockDescriptionPrefix()) }
    lateinit var shopType: BlockEntityType<ShopEntity>
    lateinit var piggyType: BlockEntityType<PiggyBankEntity>
}
