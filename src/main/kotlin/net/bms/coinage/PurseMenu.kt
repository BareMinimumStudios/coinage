package net.bms.coinage

import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.component.CustomData
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.SimpleContainer
import net.minecraft.world.SimpleMenuProvider
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ClickType
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

internal class PurseMenu(id: Int, inventory: Inventory, private val owner: ServerPlayer) :
    AbstractContainerMenu(MenuType.GENERIC_9x3, id) {
    private val display = SimpleContainer(27)
    private var shownBalance = -1L
    private var shownRates: ExchangeRates? = null
    private val coins = CoinageItems.descending.reversed()

    init {
        for (slot in 0 until 27) {
            addSlot(object : Slot(display, slot, 8 + slot % 9 * 18, 18 + slot / 9 * 18) {
                override fun mayPlace(stack: ItemStack) = false
                override fun mayPickup(player: Player) = false
            })
        }
        for (row in 0..2) for (column in 0..8) {
            addSlot(Slot(inventory, column + row * 9 + 9, 8 + column * 18, 85 + row * 18))
        }
        for (column in 0..8) addSlot(Slot(inventory, column, 8 + column * 18, 143))
        updateDisplay()
    }

    private fun named(stack: ItemStack, key: String, vararg arguments: Any): ItemStack = stack.apply {
        set(DataComponents.CUSTOM_NAME, Component.translatable(key, *arguments))
    }

    private fun updateDisplay() {
        val balance = PurseLedger.get(owner.server).balance(owner.uuid)
        val rates = CurrencyValues.current()
        if (shownBalance == balance && shownRates == rates) return
        shownBalance = balance
        shownRates = rates
        display.setItem(4, named(ItemStack(CoinageItems.moneyBag), "gui.coinage.purse_balance", balance))
        for ((index, coin) in coins.withIndex()) {
            display.setItem(11 + index, named(ItemStack(coin), "gui.coinage.withdraw_coin", ItemStack(coin).hoverName, coin.denomination))
        }
        display.setItem(21, named(ItemStack(Items.HOPPER), "gui.coinage.deposit_all"))
        display.setItem(23, named(ItemStack(CoinageItems.moneyBag), "gui.coinage.bag_all"))
        display.setItem(26, named(ItemStack(Items.PAPER), "gui.coinage.purse_help").apply {
            val tag = CompoundTag()
            tag.putBoolean("coinage_purse", true)
            set(DataComponents.CUSTOM_DATA, CustomData.of(tag))
        })
    }

    override fun broadcastChanges() {
        updateDisplay()
        super.broadcastChanges()
    }

    override fun stillValid(player: Player): Boolean = player === owner && player.isAlive

    override fun clicked(slot: Int, button: Int, type: ClickType, player: Player) {
        if (!stillValid(player)) return
        if (slot in 0 until 27) {
            if (type != ClickType.PICKUP || button !in 0..1) return
            val success = when (slot) {
                in 11 until 11 + coins.size -> {
                    val coin = coins[slot - 11]
                    val count = if (button == 0) 1 else 100
                    PurseTransactions.withdraw(owner, coin.denomination * count, coin = coin)
                }
                21 -> PurseTransactions.deposit(owner) != null
                23 -> PurseTransactions.withdraw(owner, PurseLedger.get(owner.server).balance(owner.uuid), bag = true)
                else -> true
            }
            if (!success) owner.displayClientMessage(Component.translatable("gui.coinage.purse_failed"), true)
            broadcastChanges()
            return
        }
        super.clicked(slot, button, type, player)
    }

    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        if (!stillValid(player) || index !in 27 until slots.size) return ItemStack.EMPTY
        val slot = slots[index]
        val stack = slot.item.copy()
        if ((PurseTransactions.value(stack) ?: 0) <= 0) return ItemStack.EMPTY
        if (PurseTransactions.deposit(owner, slot.containerSlot) == null) return ItemStack.EMPTY
        return stack
    }

    companion object {
        var show: (ServerPlayer) -> Boolean = { false }
        fun open(player: ServerPlayer) {
            if (show(player)) return
            player.openMenu(SimpleMenuProvider(
                { id, inventory, _ -> PurseMenu(id, inventory, player) },
                Component.translatable("container.coinage.purse")
            ))
        }
    }
}
