package net.bms.coinage

import java.lang.ref.WeakReference
import java.util.WeakHashMap
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.inventory.AbstractContainerMenu

internal object PurseWithdrawal {
    private data class Request(val menu: WeakReference<AbstractContainerMenu>, val id: Int, val time: Long)
    private val requests = WeakHashMap<ServerPlayer, Request>()
    var send: (ServerPlayer, PurseWithdrawPayloads.Result) -> Unit = { _, _ -> }

    private fun execute(player: ServerPlayer, containerId: Int, requestId: Int, transaction: () -> Int) {
        val menu = player.containerMenu
        val previous = requests[player]
        val now = System.nanoTime()
        val code = when {
            !player.isAlive || menu.containerId != containerId || !menu.stillValid(player) -> 3
            requestId <= 0 -> 1
            previous?.menu?.get() === menu && requestId <= previous.id -> 5
            previous != null && now - previous.time < 250_000_000 -> 4
            else -> {
                val result = transaction()
                requests[player] = Request(WeakReference(menu), requestId, now)
                result
            }
        }
        send(player, PurseWithdrawPayloads.Result(containerId, requestId, code))
    }

    fun withdraw(player: ServerPlayer, packet: PurseWithdrawPayloads.Withdraw) =
        execute(player, packet.containerId, packet.requestId) {
            val amount = if (packet.expression.length <= PurseWithdrawPayloads.MAX_EXPRESSION)
                CoinAmount.parse(packet.expression, 1) else null
            if (amount == null) 1 else if (PurseTransactions.withdraw(player, amount, bag = packet.bag)) 0 else 2
        }

    fun selected(player: ServerPlayer, packet: PurseWithdrawPayloads.Selected) =
        execute(player, packet.containerId, packet.requestId) {
            if (PurseSelection.valueOf(packet.counts, CurrencyValues.current()) == null) 1
            else if (PurseTransactions.withdrawSelected(player, packet.counts, packet.bag)) 0 else 2
        }

    fun quick(player: ServerPlayer, packet: PurseWithdrawPayloads.Quick) =
        execute(player, packet.containerId, packet.requestId) {
            when (packet.action) {
                0 -> if (PurseTransactions.deposit(player) != null) 0 else 2
                1 -> if (PurseTransactions.withdrawAll(player)) 0 else 2
                else -> 1
            }
        }
}
