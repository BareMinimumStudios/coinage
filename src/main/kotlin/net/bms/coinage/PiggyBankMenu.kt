package net.bms.coinage

import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.Container
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ClickType
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

internal class PiggyBankMenu(id: Int, playerInventory: Inventory, private val bank: Container) :
    AbstractContainerMenu(MenuType.GENERIC_9x4, id) {
    private val controls = SimpleContainer(9)

    init {
        bank.startOpen(playerInventory.player)
        for (row in 0..2) for (column in 0..8) {
            addSlot(object : Slot(bank, row * 9 + column, 8 + column * 18, 18 + row * 18) {
                override fun mayPlace(stack: ItemStack): Boolean = stack.item is CoinItem || stack.item is MoneyBagItem
            })
        }
        controls.setItem(4, ItemStack(Items.HOPPER).apply {
            set(DataComponents.CUSTOM_NAME, Component.translatable("gui.coinage.bank_quick_stack"))
        })
        for (column in 0..8) {
            addSlot(object : Slot(controls, column, 8 + column * 18, 72) {
                override fun mayPlace(stack: ItemStack) = false
                override fun mayPickup(player: Player) = false
            })
        }
        for (row in 0..2) for (column in 0..8) {
            addSlot(Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 103 + row * 18))
        }
        for (column in 0..8) addSlot(Slot(playerInventory, column, 8 + column * 18, 161))
    }

    override fun stillValid(player: Player): Boolean = bank.stillValid(player)

    override fun clicked(slot: Int, button: Int, type: ClickType, player: Player) {
        if (!stillValid(player)) return
        if (slot in 27..35) {
            if (slot == 31 && button == 0 && type == ClickType.PICKUP) {
                if (!BankTransactions.quickStack(bank, player.inventory)) {
                    player.displayClientMessage(Component.translatable("gui.coinage.bank_full"), true)
                }
                broadcastChanges()
            }
            return
        }
        super.clicked(slot, button, type, player)
    }

    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        if (!stillValid(player) || index !in slots.indices || index in 27..35) return ItemStack.EMPTY
        val slot = slots[index]
        if (!slot.hasItem()) return ItemStack.EMPTY
        val stack = slot.item
        val original = stack.copy()
        val moved = if (index < 27) moveItemStackTo(stack, 36, slots.size, true)
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
