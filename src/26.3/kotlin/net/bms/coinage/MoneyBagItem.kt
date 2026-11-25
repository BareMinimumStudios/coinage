package net.bms.coinage

import net.minecraft.world.inventory.Slot

import net.minecraft.world.inventory.ClickAction

import net.minecraft.world.entity.SlotAccess

import java.util.function.Consumer
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.item.component.TooltipDisplay
import net.minecraft.world.level.Level

class MoneyBagItem : Item(CoinageItems.properties("money_bag").stacksTo(1)) {
    override fun overrideOtherStackedOnMe(
        self: ItemStack, other: ItemStack, slot: Slot,
        action: ClickAction, player: Player, cursor: SlotAccess
    ): Boolean = CurrencyInteractions.click(self, other, slot, action, player, cursor)

    override fun appendHoverText(stack: ItemStack, context: TooltipContext, display: TooltipDisplay, tooltip: Consumer<Component>, flag: TooltipFlag) {
        val amount = value(stack)
        tooltip.accept(Component.translatable("tooltip.coinage.value", amount))
        if (amount > 0L) CurrencyTooltip.appendRows(amount, CurrencyValues.current()) { tooltip.accept(it) }
    }
    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResult {
        val stack = player.getItemInHand(hand)
        if (player is ServerPlayer) {
            val amount = value(stack)
            val ledger = PurseLedger.get(player.level().server)
            val balance = ledger.balance(player.uuid)
            if (amount <= 0 || balance > Long.MAX_VALUE - amount) return InteractionResult.FAIL
            ledger.setBalance(player.uuid, balance + amount)
            stack.shrink(1)
        }
        return InteractionResult.SUCCESS
    }

    companion object {
        fun value(stack: ItemStack): Long =
            stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getLongOr("coinage_value", 0).coerceAtLeast(0)

        fun create(amount: Long): ItemStack {
            require(amount > 0)
            return ItemStack(CoinageItems.moneyBag).apply {
                set(DataComponents.CUSTOM_DATA, CustomData.of(CompoundTag().apply { putLong("coinage_value", amount) }))
            }
        }
    }
}
