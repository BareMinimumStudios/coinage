package net.bms.coinage

import net.minecraft.world.Container

internal object CurrencyPayment {
    internal data class Debit(val slot: Int, val count: Int)
    internal class Plan(val balance: Long, private val debits: List<Debit>) {
        fun apply(inventory: Container) {
            for (debit in debits) inventory.removeItem(debit.slot, debit.count)
        }
    }

    fun plan(inventory: Container, balance: Long, price: Long): Plan? {
        require(balance >= 0 && price > 0)
        if (balance >= price) return Plan(balance - price, emptyList())
        var remaining = price - balance
        val debits = mutableListOf<Debit>()
        for (slot in 0 until inventory.containerSize) {
            val stack = inventory.getItem(slot)
            val value = PurseTransactions.value(stack) ?: return null
            if (value == 0L) continue
            val unit = value / stack.count
            val count = minOf(stack.count.toLong(), (remaining - 1) / unit + 1).toInt()
            val paid = unit * count
            debits.add(Debit(slot, count))
            if (paid >= remaining) return Plan(paid - remaining, debits)
            remaining -= paid
        }
        return null
    }
}
