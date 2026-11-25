package net.bms.coinage

import net.minecraft.world.entity.SlotAccess
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.ClickAction
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack

internal object CurrencyInteractions {
    private fun value(stack: ItemStack): Long = when (val item = stack.item) {
        is CoinItem -> item.denomination * stack.count
        is MoneyBagItem -> MoneyBagItem.value(stack)
        else -> 0
    }

    fun click(
        self: ItemStack,
        other: ItemStack,
        slot: Slot,
        action: ClickAction,
        player: Player,
        cursor: SlotAccess
    ): Boolean {
        if (!slot.mayPickup(player)) return false
        val item = self.item
        val rates = CurrencyValues.current()
        if (action == ClickAction.SECONDARY && other.isEmpty) {
            val result = if (item is CoinItem && item.tier != CoinTier.BRONZE) {
                val lower = CoinageItems.descending.first { it.tier.ordinal == item.tier.ordinal - 1 }
                val count = item.denomination / lower.denomination
                if (count > 100) return false
                self.copyWithCount(self.count - 1) to ItemStack(lower, count.toInt())
            } else if (item is MoneyBagItem) {
                val amount = value(self)
                val coin = CoinageItems.descending.firstOrNull { rates.value(it.tier) <= amount } ?: return false
                val count = minOf(amount / coin.denomination, 100).toInt()
                val remainder = amount - count * coin.denomination
                (if (remainder == 0L) ItemStack.EMPTY else MoneyBagItem.create(remainder)) to ItemStack(coin, count)
            } else return false
            if (!result.first.isEmpty && !slot.mayPlace(result.first)) return false
            slot.setByPlayer(result.first)
            cursor.set(result.second)
            return true
        }
        if (action != ClickAction.PRIMARY || other.isEmpty) return false
        if (item is CoinItem && item.tier == CoinTier.PLATINUM &&
            ItemStack.isSameItemSameComponents(self, other) && self.count + other.count <= self.maxStackSize
        ) return false

        val ownValue = value(self)
        val otherValue = value(other)
        if (ownValue <= 0 || otherValue <= 0 || ownValue > Long.MAX_VALUE - otherValue) return false
        if (item is CoinItem && other.item === item && item.tier != CoinTier.PLATINUM) {
            val upper = CoinageItems.descending.first { it.tier.ordinal == item.tier.ordinal + 1 }
            val ratio = upper.denomination / item.denomination
            val total = self.count + other.count
            if (total >= ratio) {
                val compacted = ItemStack(upper, (total / ratio).toInt())
                if (!slot.mayPlace(compacted)) return false
                slot.setByPlayer(compacted)
                cursor.set(if (total % ratio == 0L) ItemStack.EMPTY else ItemStack(item, (total % ratio).toInt()))
                return true
            }
            return false
        }
        val bag = MoneyBagItem.create(ownValue + otherValue)
        if (!slot.mayPlace(bag)) return false
        slot.setByPlayer(bag)
        cursor.set(ItemStack.EMPTY)
        return true
    }
}
