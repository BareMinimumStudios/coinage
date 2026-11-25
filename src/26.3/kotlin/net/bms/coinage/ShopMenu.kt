package net.bms.coinage

import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.ItemLore

internal class ShopMenu(id: Int, playerInventory: Inventory, internal val shop: ShopEntity) :
    AbstractContainerMenu(MenuType.GENERIC_9x6, id) {
    private val display = SimpleContainer(54)
    private val viewer = playerInventory.player
    private var page = 0
    private var shownPage = -1
    private var shownRevision = -1L
    private var shownOffers = emptyList<ShopOffer>()

    init {
        for (slot in 0 until 54) {
            val x = 8 + slot % 9 * 18
            val y = 18 + slot / 9 * 18
            if (slot in 9..35) {
                addSlot(object : Slot(shop.stock, slot - 9, x, y) {
                    override fun mayPlace(stack: ItemStack): Boolean = shop.canManage(playerInventory.player)
                    override fun mayPickup(player: Player): Boolean = shop.canManage(player)
                })
            } else {
                addSlot(object : Slot(display, slot, x, y) {
                    override fun mayPlace(stack: ItemStack) = false
                    override fun mayPickup(player: Player) = false
                })
            }
        }
        for (row in 0..2) for (column in 0..8) {
            addSlot(Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 139 + row * 18))
        }
        for (column in 0..8) addSlot(Slot(playerInventory, column, 8 + column * 18, 197))
        updateDisplay()
    }

    private fun updateDisplay() {
        page = page.coerceIn(0, shop.pageCount - 1)
        if (shownPage == page && shownRevision == shop.revision) return
        shownPage = page
        shownRevision = shop.revision
        shownOffers = shop.pageOffers(page)
        for (slot in 0..8) {
            val offer = shownOffers.getOrNull(slot)
            val item = offer?.stack?.copy() ?: ItemStack.EMPTY
            if (offer != null) item.set(DataComponents.CUSTOM_NAME, Component.translatable(
                "gui.coinage.numbered_offer", offer.number + 1, offer.stack.hoverName, offer.price))
            if (offer != null) ShopEditorData.markOffer(item, offer.number + 1, offer.price)
            display.setItem(slot, item)
        }
        display.setItem(46, if (page > 0) ItemStack(Items.ARROW).apply {
            set(DataComponents.CUSTOM_NAME, Component.translatable("gui.coinage.previous_shop_page"))
        } else ItemStack.EMPTY)
        display.setItem(48, ItemStack(Items.PAPER).apply {
            set(DataComponents.CUSTOM_NAME, Component.translatable("gui.coinage.shop_page", page + 1, shop.pageCount))
        })
        display.setItem(52, if (page + 1 < shop.pageCount) ItemStack(Items.ARROW).apply {
            set(DataComponents.CUSTOM_NAME, Component.translatable("gui.coinage.next_shop_page"))
        } else ItemStack.EMPTY)
        display.setItem(45, ItemStack(Items.HOPPER).apply {
            set(DataComponents.CUSTOM_NAME, Component.translatable(
                if (shop.shopBlock.pawn) "gui.coinage.automation_output" else "gui.coinage.automation_input",
                Component.translatable(if (shop.automationEnabled) "options.on" else "options.off")
            ))
        })
        display.setItem(49, if (shop.canManage(viewer)) ItemStack(Items.GOLD_INGOT).apply {
            set(DataComponents.CUSTOM_NAME, Component.translatable("gui.coinage.collect", shop.storedCoins))
        } else ItemStack.EMPTY)
        display.setItem(50, if (shop.shopBlock.pawn && shop.canManage(viewer)) ItemStack(CoinageItems.moneyBag).apply {
            set(DataComponents.CUSTOM_NAME, Component.translatable("gui.coinage.fund_pawn_shop"))
            set(DataComponents.LORE, ItemLore(listOf(
                Component.translatable("gui.coinage.fund_inventory"), Component.translatable("gui.coinage.fund_purse"))))
        } else ItemStack.EMPTY)
        display.setItem(53, ItemStack(Items.PAPER).apply {
            set(DataComponents.CUSTOM_NAME, Component.translatable("gui.coinage.shop_help"))
            ShopEditorData.markMenu(this, shop.canManage(viewer), shop.nextOfferNumber)
        })
    }

    fun rejectStaleClick(player: ServerPlayer) {
        updateDisplay()
        broadcastFullState()
        player.sendOverlayMessage(Component.translatable("gui.coinage.shop_changed"))
    }

    override fun broadcastChanges() {
        updateDisplay()
        super.broadcastChanges()
    }

    override fun stillValid(player: Player): Boolean =
        shop.level?.getBlockEntity(shop.blockPos) === shop &&
            player.distanceToSqr(shop.blockPos.x + .5, shop.blockPos.y + .5, shop.blockPos.z + .5) <= 64

    override fun clicked(slot: Int, button: Int, type: ContainerInput, player: Player) {
        if (!stillValid(player)) return
        if (slot in 0..8) {
            if (player is ServerPlayer && type == ContainerInput.PICKUP && button == 0) {
                val offer = shownOffers.getOrNull(slot) ?: return
                if (!shop.trade(player, offer)) player.sendOverlayMessage(Component.translatable("gui.coinage.trade_failed"))
                broadcastChanges()
            }
            return
        }
        if (slot in 36..53) {
            if (type == ContainerInput.PICKUP && button in 0..1) {
                if (slot == 46) page = if (button == 1) 0 else (page - 1).coerceAtLeast(0)
                if (slot == 52) page = if (button == 1) shop.pageCount - 1 else (page + 1).coerceAtMost(shop.pageCount - 1)
            }
            if (slot == 45 && type == ContainerInput.PICKUP && button == 0) shop.setAutomation(player, !shop.automationEnabled)
            if (slot == 49 && player is ServerPlayer && type == ContainerInput.PICKUP && button == 0) shop.withdraw(player)
            if (slot == 50 && player is ServerPlayer && type == ContainerInput.PICKUP && button in 0..1 &&
                shop.shopBlock.pawn && shop.canManage(player)) {
                val success = if (button == 0) shop.fundInventory(player)
                    else shop.fund(player, PurseLedger.get(player.level().server).balance(player.uuid))
                if (!success) player.sendOverlayMessage(Component.translatable("gui.coinage.fund_failed"))
            }
            broadcastChanges()
            return
        }
        super.clicked(slot, button, type, player)
    }

    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        if (index !in slots.indices || !shop.canManage(player)) return ItemStack.EMPTY
        if (index < 9 || index in 36..53) return ItemStack.EMPTY
        val slot = slots[index]
        if (!slot.hasItem()) return ItemStack.EMPTY
        val stack = slot.item
        val original = stack.copy()
        val moved = if (index in 9..35) moveItemStackTo(stack, 54, slots.size, true)
            else moveItemStackTo(stack, 9, 36, false)
        if (!moved) return ItemStack.EMPTY
        if (stack.isEmpty) slot.setByPlayer(ItemStack.EMPTY) else slot.setChanged()
        slot.onTake(player, stack)
        return original
    }
}
