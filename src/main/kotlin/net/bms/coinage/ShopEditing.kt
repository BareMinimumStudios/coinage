package net.bms.coinage

import java.util.WeakHashMap
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack

internal object ShopEditing {
    private val lastRequest = WeakHashMap<ServerPlayer, Long>()
    var send: (ServerPlayer, ShopEditPayloads.Result) -> Unit = { _, _ -> }

    fun edit(player: ServerPlayer, packet: ShopEditPayloads.Edit) {
        val now = System.nanoTime()
        val previous = lastRequest.put(player, now)
        val menu = player.containerMenu as? ShopMenu
        val code = when {
            previous != null && now - previous < 250_000_000 -> 5
            menu == null || menu.containerId != packet.containerId || !menu.stillValid(player) -> 4
            !player.isAlive || !menu.canManage(player) -> 1
            packet.offerNumber < 1 || packet.expression.length > ShopEditPayloads.MAX_EXPRESSION -> 3
            else -> {
                val price = if (packet.removing) 0L else CoinAmount.parse(packet.expression, 1)
                if (price == null) 3 else {
                    val template = if (packet.useExisting) menu.shop.offerStack(packet.offerNumber - 1)
                        else (if (!menu.carried.isEmpty) menu.carried else player.mainHandItem).copy()
                    if (price != 0L && (template == null || template.isEmpty)) 2
                    else if (menu.shop.configure(player, packet.offerNumber - 1, price,
                            template ?: ItemStack.EMPTY)) {
                        menu.broadcastChanges()
                        0
                    } else 2
                }
            }
        }
        send(player, ShopEditPayloads.Result(packet.containerId, packet.requestId, code))
    }
}
