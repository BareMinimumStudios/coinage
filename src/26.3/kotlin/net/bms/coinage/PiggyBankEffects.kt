package net.bms.coinage

import net.minecraft.core.BlockPos
import net.minecraft.core.component.DataComponents
import net.minecraft.core.particles.BlockParticleOption
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.tags.TagKey
import net.minecraft.world.Containers
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties

object PiggyBankEffects {
    val breakSound: SoundEvent = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(MOD_ID, "piggy_bank_break"))
    val heavyBlocks: TagKey<Block> = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, "very_heavy_blocks"))
    val hammers: TagKey<Item> = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, "bank_hammers"))

    fun isHammer(stack: ItemStack): Boolean = !stack.isEmpty &&
        (stack.`is`(hammers) || stack.get(DataComponents.CUSTOM_NAME)?.string.equals("Hammer", ignoreCase = true))

    fun smash(level: Level, pos: BlockPos, state: BlockState, bank: PiggyBankEntity) {
        if (level !is ServerLevel) return
        Containers.dropContents(level, pos.relative(state.getValue(BlockStateProperties.HORIZONTAL_FACING).opposite), bank.inventory)
        bank.inventory.clearContent()
        bank.setChanged()
        level.playSound(null, pos, breakSound, SoundSource.BLOCKS, 1f, 1f)
        level.sendParticles(BlockParticleOption(ParticleTypes.BLOCK, state),
            pos.x + .5, pos.y + .25, pos.z + .5, 18, .2, .15, .2, .02)
    }
}
