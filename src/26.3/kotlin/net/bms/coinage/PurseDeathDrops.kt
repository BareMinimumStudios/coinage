package net.bms.coinage

import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.gamerules.GameRules

internal fun dropPurseOnDeath(player: ServerPlayer, accept: (ItemStack) -> Boolean): Boolean {
    val level = player.level()
    if (level.gameRules.get(GameRules.KEEP_INVENTORY) || player.isSpectator) return false
    val percent = CurrencyValues.configuration?.deathLossPercent?.coerceIn(0, 100) ?: return false
    val ledger = PurseLedger.get(level.server)
    val balance = ledger.balance(player.uuid)
    val loss = balance / 100 * percent + balance % 100 * percent / 100
    if (loss == 0L || !accept(MoneyBagItem.create(loss))) return false
    ledger.setBalance(player.uuid, balance - loss)
    return true
}
