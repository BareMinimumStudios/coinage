package net.bms.coinage

import java.math.BigInteger
import java.util.PriorityQueue
import java.util.UUID
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtOps
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.saveddata.SavedData

internal class PurseLedger : SavedData() {
    var shopMarket = ShopMarket()
        private set

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

    fun totalBalance(): BigInteger =
        balances.values.fold(BigInteger.ZERO) { total, balance -> total + BigInteger.valueOf(balance) }

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

    override fun save(tag: CompoundTag, registries: HolderLookup.Provider): CompoundTag {
        val purseBalances = CompoundTag()
        balances.forEach { (player, balance) -> purseBalances.putLong(player.toString(), balance) }
        tag.put("balances", purseBalances)
        ShopMarket.codec.encodeStart(NbtOps.INSTANCE, shopMarket).result().ifPresent { tag.put("shop_market", it) }
        val autoCollect = CompoundTag()
        autoCollectPlayers.forEach { autoCollect.putBoolean(it.toString(), true) }
        tag.put("auto_collect", autoCollect)
        return tag
    }

    companion object {
        // The ledger has its own format and does not use a vanilla data fixer.
        @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        private val factory = Factory(::PurseLedger, { tag, _ -> load(tag) }, null)

        fun get(server: MinecraftServer): PurseLedger =
            server.overworld().dataStorage.computeIfAbsent(factory, MOD_ID).also { it.server = server }

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
            val autoCollect = tag.getCompound("auto_collect")
            for (key in autoCollect.allKeys) {
                if (autoCollect.getBoolean(key)) {
                    runCatching { UUID.fromString(key) }.getOrNull()?.let(ledger.autoCollectPlayers::add)
                }
            }
            tag.get("shop_market")?.let { saved ->
                ShopMarket.codec.parse(NbtOps.INSTANCE, saved).result().ifPresent { ledger.shopMarket = it }
            }
            return ledger
        }
    }
}
