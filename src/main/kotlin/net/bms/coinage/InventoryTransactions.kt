package net.bms.coinage

import net.minecraft.world.Container
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack

internal object InventoryTransactions {
    fun snapshot(container: Container): List<ItemStack> =
        (0 until container.containerSize).map { container.getItem(it).copy() }

    fun restore(container: Container, items: List<ItemStack>) {
        items.forEachIndexed { index, stack -> container.setItem(index, stack) }
        container.setChanged()
    }

    fun insert(container: Container, incoming: ItemStack): Boolean {
        val limit = if (container is Inventory) 36 else container.containerSize
        for (emptySlots in listOf(false, true)) {
            for (slot in 0 until limit) {
                val stack = container.getItem(slot)
                if (stack.isEmpty != emptySlots || !container.canPlaceItem(slot, incoming)) continue
                if (!stack.isEmpty && !ItemStack.isSameItemSameComponents(stack, incoming)) continue
                val capacity = minOf(container.maxStackSize, incoming.maxStackSize) - stack.count
                val count = minOf(capacity, incoming.count)
                if (count <= 0) continue
                if (stack.isEmpty) container.setItem(slot, incoming.copyWithCount(count)) else stack.grow(count)
                incoming.shrink(count)
                container.setChanged()
                if (incoming.isEmpty) return true
            }
        }
        return false
    }

    fun removeMatching(container: Container, template: ItemStack): Boolean {
        val limit = if (container is Inventory) 36 else container.containerSize
        var remaining = template.count
        for (slot in 0 until limit) {
            val stack = container.getItem(slot)
            if (!ItemStack.isSameItemSameComponents(stack, template)) continue
            val take = minOf(stack.count, remaining)
            stack.shrink(take)
            remaining -= take
            if (remaining == 0) {
                container.setChanged()
                return true
            }
        }
        return false
    }
}
