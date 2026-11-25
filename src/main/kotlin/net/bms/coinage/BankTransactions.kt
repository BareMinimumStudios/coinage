package net.bms.coinage

import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack
import net.minecraft.world.entity.player.Inventory

internal object BankTransactions {
    fun quickStack(bank: Container, inventory: Inventory): Boolean {
        val bankBefore = InventoryTransactions.snapshot(bank)
        val inventoryBefore = InventoryTransactions.snapshot(inventory)
        for (slot in 0 until inventory.containerSize) {
            val stack = inventory.getItem(slot)
            if (stack.isEmpty || (stack.item !is CoinItem && stack.item !is MoneyBagItem)) continue
            if (!InventoryTransactions.insert(bank, stack.copy())) {
                InventoryTransactions.restore(bank, bankBefore)
                InventoryTransactions.restore(inventory, inventoryBefore)
                return false
            }
            inventory.setItem(slot, ItemStack.EMPTY)
        }
        bank.setChanged()
        inventory.setChanged()
        return true
    }
}
