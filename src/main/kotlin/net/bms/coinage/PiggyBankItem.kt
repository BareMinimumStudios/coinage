package net.bms.coinage

import net.minecraft.stats.Stats
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Equipable
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block

class PiggyBankItem(block: Block, properties: Properties) : BlockItem(block, properties), Equipable {
    override fun getEquipmentSlot(): EquipmentSlot = EquipmentSlot.HEAD

    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        val held = player.getItemInHand(hand)
        val equipped = player.getItemBySlot(EquipmentSlot.HEAD)
        if (held.count != 1 || !player.canUseSlot(EquipmentSlot.HEAD) ||
            (!player.isCreative && EnchantmentHelper.has(equipped, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE))) {
            return InteractionResultHolder.fail(held)
        }
        player.setItemSlot(EquipmentSlot.HEAD, held)
        player.setItemInHand(hand, equipped)
        if (!level.isClientSide) player.awardStat(Stats.ITEM_USED.get(this))
        return InteractionResultHolder.sidedSuccess(equipped, level.isClientSide)
    }
}
