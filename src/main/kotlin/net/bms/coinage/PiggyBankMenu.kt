package net.bms.coinage

import net.minecraft.world.Container
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack

internal class PiggyBankMenu(id: Int, playerInventory: Inventory, private val bank: Container) :
    AbstractContainerMenu(MenuType.GENERIC_9x3, id) {
    init {
        bank.startOpen(playerInventory.player)
        for (row in 0..2) for (column in 0..8) {
            addSlot(object : Slot(bank, row * 9 + column, 8 + column * 18, 18 + row * 18) {
                override fun mayPlace(stack: ItemStack): Boolean = stack.item is CoinItem || stack.item is MoneyBagItem
            })
        }
        for (row in 0..2) for (column in 0..8) {
            addSlot(Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 85 + row * 18))
        }
        for (column in 0..8) addSlot(Slot(playerInventory, column, 8 + column * 18, 143))
    }

    override fun stillValid(player: Player): Boolean = bank.stillValid(player)

    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        if (index !in slots.indices) return ItemStack.EMPTY
        val slot = slots[index]
        if (!slot.hasItem()) return ItemStack.EMPTY
        val stack = slot.item
        val original = stack.copy()
        val moved = if (index < 27) moveItemStackTo(stack, 27, slots.size, true)
            else moveItemStackTo(stack, 0, 27, false)
        if (!moved) return ItemStack.EMPTY
        if (stack.isEmpty) slot.setByPlayer(ItemStack.EMPTY) else slot.setChanged()
        slot.onTake(player, stack)
        return original
    }

    override fun removed(player: Player) {
        super.removed(player)
        bank.stopOpen(player)
    }
}
