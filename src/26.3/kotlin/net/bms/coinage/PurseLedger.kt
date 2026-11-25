package net.bms.coinage

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import java.util.PriorityQueue
import java.util.UUID
import net.minecraft.core.HolderLookup
import net.minecraft.core.UUIDUtil
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.Identifier
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.saveddata.SavedData
import net.minecraft.world.level.saveddata.SavedDataType

internal class PurseLedger : SavedData() {
    private val balances = HashMap<UUID, Long>()
    private val legacyImports = HashSet<UUID>()

    fun hasLegacyImport(player: UUID): Boolean = player in legacyImports

    fun importLegacy(player: UUID, amount: Long): Boolean {
        require(amount >= 0)
        if (!legacyImports.add(player)) return false
        if (balance(player) == 0L && amount > 0) balances[player] = amount
        setDirty()
        return true
    }

    fun balance(player: UUID): Long = balances[player] ?: 0L

    fun setBalance(player: UUID, balance: Long) {
        require(balance >= 0)
        if (legacyImports.add(player)) setDirty()
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
        val best = PriorityQueue(limit, order.reversed())
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

    companion object {
        internal val codec: Codec<PurseLedger> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.LONG).fieldOf("balances")
                    .forGetter<PurseLedger> { it.balances.toMap() },
                Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.BOOL).optionalFieldOf("legacyImports", emptyMap())
                    .forGetter<PurseLedger> { ledger -> ledger.legacyImports.associateWith { true } }
            ).apply(instance) { values, imported ->
                PurseLedger().apply {
                    values.forEach { (id, value) -> if (value > 0) balances[id] = value }
                    imported.forEach { (id, value) -> if (value) legacyImports.add(id) }
                }
            }
        }

        private val type = SavedDataTypes.create(
            Identifier.fromNamespaceAndPath(MOD_ID, "purses"), ::PurseLedger, codec
        )

        fun get(server: MinecraftServer): PurseLedger = server.overworld().dataStorage.computeIfAbsent(type)
    }
}
