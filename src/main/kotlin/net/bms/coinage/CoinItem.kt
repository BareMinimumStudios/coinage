package net.bms.coinage

import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level

class CoinItem(val denomination: Long) : Item(Properties().stacksTo(99)) {
    override fun use(level: Level, player: net.minecraft.world.entity.player.Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = player.getItemInHand(hand)
        if (player is ServerPlayer) {
            val ledger = PurseLedger.get(player.server)
            val amount = denomination * stack.count
            val balance = ledger.balance(player.uuid)
            if (balance > Long.MAX_VALUE - amount) return InteractionResultHolder.fail(stack)
            ledger.setBalance(player.uuid, balance + amount)
            stack.shrink(stack.count)
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide)
    }
}

object CoinageItems {
    val bronze = CoinItem(1)
    val silver = CoinItem(100)
    val gold = CoinItem(10_000)
    val entries = linkedMapOf("bronze_coin" to bronze, "silver_coin" to silver, "gold_coin" to gold)
}
