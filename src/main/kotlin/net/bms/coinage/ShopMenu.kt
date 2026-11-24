package net.bms.coinage

import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ClickType
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

internal class ShopMenu(id: Int, playerInventory: Inventory, private val shop: ShopEntity) :
    AbstractContainerMenu(MenuType.GENERIC_9x6, id) {
    private val display = SimpleContainer(54)

    init {
        for (slot in 0 until 54) {
            val x = 8 + slot % 9 * 18
            val y = 18 + slot / 9 * 18
            if (slot in 9..35) {
                addSlot(object : Slot(shop.stock, slot - 9, x, y) {
                    override fun mayPlace(stack: ItemStack): Boolean = shop.canManage(playerInventory.player)
                    override fun mayPickup(player: Player): Boolean = shop.canManage(player)
                })
            } else {
                addSlot(object : Slot(display, slot, x, y) {
                    override fun mayPlace(stack: ItemStack) = false
                    override fun mayPickup(player: Player) = false
                })
            }
        }
        for (row in 0..2) for (column in 0..8) {
            addSlot(Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 139 + row * 18))
        }
        for (column in 0..8) addSlot(Slot(playerInventory, column, 8 + column * 18, 197))
        updateDisplay()
    }

    private fun updateDisplay() {
        for (slot in 0..8) {
            val item = shop.offers[slot].copy()
            if (!item.isEmpty) item.set(
                DataComponents.CUSTOM_NAME,
                Component.translatable("gui.coinage.offer", shop.offers[slot].hoverName, shop.prices[slot])
            )
            display.setItem(slot, item)
        }
        display.setItem(49, ItemStack(Items.GOLD_INGOT).apply {
            set(DataComponents.CUSTOM_NAME, Component.translatable("gui.coinage.collect", shop.storedCoins))
        })
        display.setItem(53, ItemStack(Items.PAPER).apply {
            set(DataComponents.CUSTOM_NAME, Component.translatable("gui.coinage.shop_help"))
        })
    }

    override fun broadcastChanges() {
        updateDisplay()
        super.broadcastChanges()
    }

    override fun stillValid(player: Player): Boolean =
        shop.level?.getBlockEntity(shop.blockPos) === shop &&
                player.distanceToSqr(shop.blockPos.x + .5, shop.blockPos.y + .5, shop.blockPos.z + .5) <= 64

    override fun clicked(slot: Int, button: Int, type: ClickType, player: Player) {
        if (!stillValid(player)) return
        if (slot in 0..8) {
            if (player is ServerPlayer && type == ClickType.PICKUP && button == 0) {
                if (!shop.trade(
                        player,
                        slot
                    )
                ) player.displayClientMessage(Component.translatable("gui.coinage.trade_failed"), true)
                broadcastChanges()
            }
            return
        }
        if (slot in 36..53) {
            if (slot == 49 && player is ServerPlayer && type == ClickType.PICKUP && button == 0) shop.withdraw(player)
            broadcastChanges()
            return
        }
        super.clicked(slot, button, type, player)
    }

    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        if (index !in slots.indices || !shop.canManage(player)) return ItemStack.EMPTY
        if (index < 9 || index in 36..53) return ItemStack.EMPTY
        val slot = slots[index]
        if (!slot.hasItem()) return ItemStack.EMPTY
        val stack = slot.item
        val original = stack.copy()
        val moved = if (index in 9..35) moveItemStackTo(stack, 54, slots.size, true)
        else moveItemStackTo(stack, 9, 36, false)
        if (!moved) return ItemStack.EMPTY
        if (stack.isEmpty) slot.setByPlayer(ItemStack.EMPTY) else slot.setChanged()
        slot.onTake(player, stack)
        return original
    }
}
