package net.bms.coinage

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import java.util.PriorityQueue
import java.util.UUID
import net.minecraft.core.UUIDUtil
import net.minecraft.resources.Identifier
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.saveddata.SavedData

internal class PurseLedger : SavedData() {
    private var server: MinecraftServer? = null
    private val balances = HashMap<UUID, Long>()

    private val autoCollectPlayers = HashSet<UUID>()

    fun autoCollect(player: UUID): Boolean = player in autoCollectPlayers

    fun setAutoCollect(player: UUID, enabled: Boolean) {
        val changed = if (enabled) autoCollectPlayers.add(player) else autoCollectPlayers.remove(player)
        if (changed) setDirty()
    }

    fun balance(player: UUID): Long = balances[player] ?: 0L

    fun setBalance(player: UUID, balance: Long) {
        require(balance >= 0)
        if (balance == balance(player)) return
        if (balance == 0L) balances.remove(player) else balances[player] = balance
        setDirty()
        server?.let { PurseSync.changed(it, player) }
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
                UUIDUtil.STRING_CODEC.listOf().optionalFieldOf("auto_collect", emptyList())
                    .forGetter<PurseLedger> { it.autoCollectPlayers.sorted() }
            ).apply(instance) { values, autoCollect ->
                PurseLedger().apply {
                    autoCollectPlayers.addAll(autoCollect)
                    values.forEach { (id, value) -> if (value > 0) balances[id] = value }
                }
            }
        }

        private val type = SavedDataTypes.create(
            Identifier.fromNamespaceAndPath(MOD_ID, "purses"), ::PurseLedger, codec
        )

        fun get(server: MinecraftServer): PurseLedger = server.overworld().dataStorage.computeIfAbsent(type).also { it.server = server }
    }
}
