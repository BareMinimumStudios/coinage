package net.bms.coinage

import java.lang.ref.WeakReference
import java.util.WeakHashMap
import net.minecraft.server.level.ServerPlayer

internal object PurseWithdrawal {
    private data class Request(val menu: WeakReference<PurseMenu>, val id: Int, val time: Long)
    private val requests = WeakHashMap<ServerPlayer, Request>()
    var send: (ServerPlayer, PurseWithdrawPayloads.Result) -> Unit = { _, _ -> }

    fun withdraw(player: ServerPlayer, packet: PurseWithdrawPayloads.Withdraw) {
        val menu = player.containerMenu as? PurseMenu
        val previous = requests[player]
        val now = System.nanoTime()
        val code = when {
            menu == null || menu.containerId != packet.containerId || !menu.stillValid(player) -> 3
            packet.requestId <= 0 -> 1
            previous?.menu?.get() === menu && packet.requestId <= previous.id -> 5
            previous != null && now - previous.time < 250_000_000 -> 4
            else -> {
                val amount = if (packet.expression.length <= PurseWithdrawPayloads.MAX_EXPRESSION)
                    CoinAmount.parse(packet.expression, 1) else null
                val result = if (amount == null) 1 else if (PurseTransactions.withdraw(player, amount, bag = packet.bag)) 0 else 2
                requests[player] = Request(WeakReference(menu), packet.requestId, now)
                result
            }
        }
        send(player, PurseWithdrawPayloads.Result(packet.containerId, packet.requestId, code))
    }
}
