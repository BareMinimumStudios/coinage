package net.bms.coinage

import java.util.UUID
import java.util.WeakHashMap
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer

internal object PurseSync {
    var send: (ServerPlayer, PursePayloads.Snapshot) -> Unit = { _, _ -> }
    private class Session {
        var balance = -1L
        var rates: ExchangeRates? = null
        var lastHello = 0L
        var lastOpen = 0L
    }
    private class State {
        val sessions = HashMap<UUID, Session>()
        val dirty = HashSet<UUID>()
        var rates: ExchangeRates? = null
    }
    private val servers = WeakHashMap<MinecraftServer, State>()

    fun hello(player: ServerPlayer, protocol: Int) {
        if (protocol != PursePayloads.PROTOCOL) return
        val state = servers.getOrPut(player.level().server) { State() }
        val session = state.sessions.getOrPut(player.uuid) { Session() }
        val now = System.nanoTime()
        if (session.lastHello != 0L && now - session.lastHello < 500_000_000L) return
        session.lastHello = now
        session.balance = -1
        state.dirty.add(player.uuid)
    }

    fun changed(server: MinecraftServer, player: UUID) {
        val state = servers[server] ?: return
        if (player in state.sessions) state.dirty.add(player)
    }

    fun tick(server: MinecraftServer) {
        val state = servers[server] ?: return
        val rates = CurrencyValues.current()
        if (rates != state.rates) {
            state.rates = rates
            state.dirty.addAll(state.sessions.keys)
        }
        if (state.dirty.isEmpty()) return
        val pending = state.dirty.toList()
        state.dirty.clear()
        val ledger = PurseLedger.get(server)
        for (id in pending) {
            val player = server.playerList.getPlayer(id)
            if (player == null) {
                state.sessions.remove(id)
                continue
            }
            val session = state.sessions[id] ?: continue
            val balance = ledger.balance(id)
            if (balance == session.balance && rates == session.rates) continue
            session.balance = balance
            session.rates = rates
            send(player, PursePayloads.Snapshot(balance, rates))
        }
    }

    fun requestOpen(player: ServerPlayer) {
        val session = servers[player.level().server]?.sessions?.get(player.uuid) ?: return
        if (!player.isAlive || player.containerMenu !== player.inventoryMenu) return
        val now = System.nanoTime()
        if (session.lastOpen != 0L && now - session.lastOpen < 250_000_000L) return
        session.lastOpen = now
        PurseMenu.open(player)
    }

    fun leave(player: ServerPlayer) {
        servers[player.level().server]?.let { state ->
            state.sessions.remove(player.uuid)
            state.dirty.remove(player.uuid)
        }
    }

    fun stop(server: MinecraftServer) { servers.remove(server) }
}
