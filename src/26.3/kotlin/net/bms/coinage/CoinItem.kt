package net.bms.coinage

import net.minecraft.world.inventory.Slot

import net.minecraft.world.inventory.ClickAction

import net.minecraft.world.entity.SlotAccess

import java.util.Locale
import java.util.function.Consumer
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.component.TooltipDisplay
import net.minecraft.world.level.Level

class CoinItem(val tier: CoinTier) : Item(CoinageItems.properties(tier.name.lowercase(Locale.ROOT) + "_coin").stacksTo(100)) {
    val denomination: Long get() = CurrencyValues.current().value(tier)

    override fun overrideOtherStackedOnMe(
        self: ItemStack, other: ItemStack, slot: Slot,
        action: ClickAction, player: Player, cursor: SlotAccess
    ): Boolean = CurrencyInteractions.click(self, other, slot, action, player, cursor)

    override fun appendHoverText(stack: ItemStack, context: TooltipContext, display: TooltipDisplay, tooltip: Consumer<Component>, flag: TooltipFlag) {
        tooltip.accept(Component.translatable("tooltip.coinage.value", denomination))
    }
    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResult {
        val stack = player.getItemInHand(hand)
        if (player is ServerPlayer) {
            val ledger = PurseLedger.get(player.level().server)
            val amount = denomination * stack.count
            val balance = ledger.balance(player.uuid)
            if (balance > Long.MAX_VALUE - amount) return InteractionResult.FAIL
            ledger.setBalance(player.uuid, balance + amount)
            stack.shrink(stack.count)
        }
        return InteractionResult.SUCCESS
    }
}

object CoinageItems {
    fun properties(name: String): Item.Properties = Item.Properties().setId(
        ResourceKey.create(
            Registries.ITEM,
            Identifier.fromNamespaceAndPath(MOD_ID, name)
        )
    )

    val bronze = CoinItem(CoinTier.BRONZE)
    val iron = CoinItem(CoinTier.IRON)
    val silver = CoinItem(CoinTier.SILVER)
    val gold = CoinItem(CoinTier.GOLD)
    val platinum = CoinItem(CoinTier.PLATINUM)
    val descending = listOf(platinum, gold, silver, iron, bronze)
    val moneyBag = MoneyBagItem()
    val entries: Map<String, Item> = linkedMapOf(
        "money_bag" to moneyBag,
        "bronze_coin" to bronze,
        "iron_coin" to iron,
        "silver_coin" to silver,
        "gold_coin" to gold,
        "platinum_coin" to platinum
    )
}
