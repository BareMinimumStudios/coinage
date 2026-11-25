package net.bms.coinage

import net.minecraft.core.component.DataComponents
import net.minecraft.stats.Stats
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.item.equipment.Equippable
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block

class PiggyBankItem(block: Block, properties: Properties) : BlockItem(block, properties.component(
    DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.HEAD).build()
)) {
    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResult {
        val held = player.getItemInHand(hand)
        val equippable = held.get(DataComponents.EQUIPPABLE) ?: return InteractionResult.PASS
        val slot = equippable.slot()
        val equipped = player.getItemBySlot(slot)
        if (held.count != 1 || !player.canUseSlot(slot) || !equippable.canBeEquippedBy(player.typeHolder()) ||
            (!player.isCreative && EnchantmentHelper.has(equipped, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE))) {
            return InteractionResult.FAIL
        }
        player.setItemSlot(slot, held)
        player.setItemInHand(hand, equipped)
        if (!level.isClientSide()) player.awardStat(Stats.ITEM_USED.get(this))
        return InteractionResult.SUCCESS.heldItemTransformedTo(equipped)
    }
}
