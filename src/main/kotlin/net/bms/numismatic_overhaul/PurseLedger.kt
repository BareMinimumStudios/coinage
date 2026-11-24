package net.bms.numismatic_overhaul

import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.saveddata.SavedData
import java.util.UUID

internal class PurseLedger : SavedData() {
    private val balances = HashMap<UUID, Long>()

    fun balance(player: UUID): Long = balances[player] ?: 0L

    fun setBalance(player: UUID, balance: Long) {
        require(balance >= 0)
        if (balance == balance(player)) return
        if (balance == 0L) balances.remove(player) else balances[player] = balance
        setDirty()
    }

    fun transfer(from: UUID, to: UUID, amount: Long): Boolean {
        require(from != to && amount > 0)
        val sender = balance(from)
        val recipient = balance(to)
        if (sender < amount || recipient > Long.MAX_VALUE - amount) return false
        setBalance(from, sender - amount)
        setBalance(to, recipient + amount)
        return true
    }

    fun richest(limit: Int): List<Map.Entry<UUID, Long>> {
        val order = compareByDescending<Map.Entry<UUID, Long>> { it.value }.thenBy { it.key }
        val best = java.util.PriorityQueue(limit, order.reversed())
        for (entry in balances.entries) {
            if (best.size < limit) {
                best.add(entry)
            } else if (order.compare(entry, best.element()) < 0) {
                best.remove()
                best.add(entry)
            }
        }
        return best.sortedWith(order)
    }

    override fun save(tag: CompoundTag, registries: HolderLookup.Provider): CompoundTag {
        val purseBalances = CompoundTag()
        balances.forEach { (player, balance) -> purseBalances.putLong(player.toString(), balance) }
        tag.put("balances", purseBalances)
        return tag
    }

    companion object {
        // The ledger has its own format and does not use a vanilla data fixer.
        @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        private val factory = Factory(::PurseLedger, { tag, _ -> load(tag) }, null)

        fun get(server: MinecraftServer): PurseLedger =
            server.overworld().dataStorage.computeIfAbsent(factory, MOD_ID)

        internal fun load(tag: CompoundTag): PurseLedger {
            val ledger = PurseLedger()
            val balances = tag.getCompound("balances")
            for (key in balances.allKeys) {
                val player = try {
                    UUID.fromString(key)
                } catch (_: IllegalArgumentException) {
                    continue
                }
                val balance = balances.getLong(key)
                if (balance > 0) ledger.balances[player] = balance
            }
            return ledger
        }
    }
}
