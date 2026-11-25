package net.bms.coinage

import net.minecraft.server.level.ServerPlayer

object ShopClickGuard {
    @JvmStatic
    fun reject(player: ServerPlayer, containerId: Int, stateId: Int, slot: Int): Boolean {
        val menu = player.containerMenu as? ShopMenu ?: return false
        if (containerId != menu.containerId || slot !in 0..8 || stateId == menu.stateId) return false
        menu.rejectStaleClick(player)
        return true
    }
}
