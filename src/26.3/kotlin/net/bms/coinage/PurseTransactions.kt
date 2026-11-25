package net.bms.coinage

import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack

internal object PurseTransactions {
    fun value(stack: ItemStack): Long? {
        if (stack.isEmpty) return 0
        val unit = when (val item = stack.item) {
            is CoinItem -> item.denomination
            is MoneyBagItem -> MoneyBagItem.value(stack)
            else -> return 0
        }
        if (unit <= 0 || stack.count <= 0 || unit > Long.MAX_VALUE / stack.count) return null
        return unit * stack.count
    }

    fun deposit(player: ServerPlayer, onlySlot: Int? = null): Long? {
        val inventory = player.inventory
        val slots = if (onlySlot == null) (0 until inventory.containerSize).toList() else listOf(onlySlot)
        if (slots.any { it !in 0 until inventory.containerSize }) return null
        var amount = 0L
        val currencySlots = mutableListOf<Int>()
        for (slot in slots) {
            val value = value(inventory.getItem(slot)) ?: return null
            if (amount > Long.MAX_VALUE - value) return null
            amount += value
            if (value > 0) currencySlots.add(slot)
        }
        val ledger = PurseLedger.get(player.level().server)
        val balance = ledger.balance(player.uuid)
        if (balance > Long.MAX_VALUE - amount) return null
        for (slot in currencySlots) inventory.setItem(slot, ItemStack.EMPTY)
        ledger.setBalance(player.uuid, balance + amount)
        inventory.setChanged()
        player.containerMenu.broadcastChanges()
        return amount
    }

    fun withdrawAll(player: ServerPlayer): Boolean =
        withdraw(player, PurseLedger.get(player.level().server).balance(player.uuid))

    fun withdraw(player: ServerPlayer, amount: Long, bag: Boolean = false, coin: CoinItem? = null): Boolean {
        if (amount <= 0) return false
        val ledger = PurseLedger.get(player.level().server)
        val balance = ledger.balance(player.uuid)
        if (balance < amount) return false
        val stacks = mutableListOf<ItemStack>()
        if (coin != null) {
            if (amount % coin.denomination != 0L) return false
            val count = amount / coin.denomination
            if (count !in 1..100) return false
            stacks.add(ItemStack(coin, count.toInt()))
        } else if (bag) {
            stacks.add(MoneyBagItem.create(amount))
        } else {
            var remaining = amount
            for (coin in CoinageItems.descending) {
                var count = remaining / coin.denomination
                remaining %= coin.denomination
                if (count > 3600) return false
                while (count > 0) {
                    val size = minOf(count, 100).toInt()
                    stacks.add(ItemStack(coin, size))
                    count -= size
                }
            }
        }
        val snapshot = InventoryTransactions.snapshot(player.inventory)
        if (!stacks.all { InventoryTransactions.insert(player.inventory, it) }) {
            InventoryTransactions.restore(player.inventory, snapshot)
            player.containerMenu.broadcastChanges()
            return false
        }
        ledger.setBalance(player.uuid, balance - amount)
        player.containerMenu.broadcastChanges()
        return true
    }
}
