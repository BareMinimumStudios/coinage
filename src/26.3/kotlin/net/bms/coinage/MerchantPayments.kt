package net.bms.coinage

import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.trading.Merchant

object MerchantPayments {
    @JvmStatic
    fun autofill(merchant: Merchant, inputs: Container, selected: Int) {
        if (CurrencyValues.configuration?.merchantPursePayments != true) return
        val player = merchant.tradingPlayer as? ServerPlayer ?: return
        val offers = EasyNpcMarket.offers(merchant, merchant.offers)
        if (selected !in offers.indices) return
        val offer = offers[selected]
        if (offer.isOutOfStock) return
        fill(player, inputs, 0, offer.costA)
        fill(player, inputs, 1, offer.costB)
    }

    private fun unitValue(stack: ItemStack): Long = when (val item = stack.item) {
        is CoinItem -> if (ItemStack.isSameItemSameComponents(stack, ItemStack(item))) item.denomination else 0
        is MoneyBagItem -> {
            val value = MoneyBagItem.value(stack)
            if (value > 0 && ItemStack.isSameItemSameComponents(stack, MoneyBagItem.create(value))) value else 0
        }
        else -> 0
    }

    private fun fill(player: ServerPlayer, inputs: Container, slot: Int, required: ItemStack) {
        if (required.isEmpty || required.count !in 1..required.maxStackSize) return
        val value = unitValue(required)
        if (value <= 0) return
        val present = inputs.getItem(slot)
        if (!present.isEmpty && !ItemStack.isSameItemSameComponents(present, required)) return
        val missing = required.count - present.count
        if (missing <= 0 || value > Long.MAX_VALUE / missing) return
        val amount = value * missing
        val ledger = PurseLedger.get(player.level().server)
        val balance = ledger.balance(player.uuid)
        if (balance < amount) return
        ledger.setBalance(player.uuid, balance - amount)
        inputs.setItem(slot, required.copy())
    }

    @JvmStatic
    fun refund(merchant: Merchant, inputs: Container) {
        if (CurrencyValues.configuration?.merchantPursePayments != true) return
        val player = merchant.tradingPlayer as? ServerPlayer ?: return
        if (!player.isAlive) return
        val ledger = PurseLedger.get(player.level().server)
        for (slot in 0..1) {
            val stack = inputs.getItem(slot)
            val value = unitValue(stack)
            if (value <= 0 || stack.count <= 0 || value > Long.MAX_VALUE / stack.count) continue
            val amount = value * stack.count
            val balance = ledger.balance(player.uuid)
            if (balance > Long.MAX_VALUE - amount) continue
            inputs.removeItemNoUpdate(slot)
            ledger.setBalance(player.uuid, balance + amount)
        }
    }
}
