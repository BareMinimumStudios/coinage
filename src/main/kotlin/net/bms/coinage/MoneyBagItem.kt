package net.bms.coinage

import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level

class MoneyBagItem : Item(Properties().stacksTo(1)) {
    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = player.getItemInHand(hand)
        if (player is ServerPlayer) {
            val amount = value(stack)
            val ledger = PurseLedger.get(player.server)
            val balance = ledger.balance(player.uuid)
            if (amount <= 0 || balance > Long.MAX_VALUE - amount) return InteractionResultHolder.fail(stack)
            ledger.setBalance(player.uuid, balance + amount)
            stack.shrink(1)
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide)
    }

    companion object {
        fun value(stack: ItemStack): Long =
            stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getLong("coinage_value").coerceAtLeast(0)

        fun create(amount: Long): ItemStack {
            require(amount > 0)
            return ItemStack(CoinageItems.moneyBag).apply {
                set(DataComponents.CUSTOM_DATA, CustomData.of(CompoundTag().apply { putLong("coinage_value", amount) }))
            }
        }
    }
}
