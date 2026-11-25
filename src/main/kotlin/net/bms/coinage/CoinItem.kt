package net.bms.coinage

import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.SlotAccess
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.ClickAction
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level

class CoinItem(val tier: CoinTier) : Item(Properties().stacksTo(100)) {
    val denomination: Long get() = CurrencyValues.current().value(tier)

    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltip: MutableList<Component>,
        flag: TooltipFlag
    ) {
        tooltip.add(Component.translatable("tooltip.coinage.value", denomination * stack.count))
    }

    override fun overrideOtherStackedOnMe(
        self: ItemStack, other: ItemStack, slot: Slot,
        action: ClickAction, player: Player, cursor: SlotAccess
    ): Boolean = CurrencyInteractions.click(self, other, slot, action, player, cursor)

    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
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
    val bronze = CoinItem(CoinTier.BRONZE)
    val silver = CoinItem(CoinTier.SILVER)
    val gold = CoinItem(CoinTier.GOLD)
    val platinum = CoinItem(CoinTier.PLATINUM)
    val descending = listOf(platinum, gold, silver, bronze)
    val moneyBag = MoneyBagItem()
    val entries: Map<String, Item> = linkedMapOf(
        "money_bag" to moneyBag,
        "bronze_coin" to bronze,
        "silver_coin" to silver,
        "gold_coin" to gold,
        "platinum_coin" to platinum
    )
}
