package net.bms.coinage

import java.util.UUID
import java.util.TreeMap
import net.minecraft.core.Direction
import net.minecraft.nbt.NbtOps
import net.minecraft.nbt.CompoundTag
import net.minecraft.core.HolderLookup
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.NonNullList
import net.minecraft.core.component.DataComponents
import net.minecraft.core.UUIDUtil
import net.minecraft.network.chat.Component
import net.minecraft.world.level.block.Block
import net.minecraft.server.level.ServerLevel
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.permissions.Permission
import net.minecraft.server.permissions.PermissionLevel
import net.minecraft.world.WorldlyContainer
import net.minecraft.world.Container
import net.minecraft.world.ContainerHelper
import net.minecraft.world.Containers
import net.minecraft.world.MenuProvider
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import org.slf4j.LoggerFactory
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput

class ShopEntity(pos: BlockPos, state: BlockState) : BlockEntity(CoinageBlocks.shopType, pos, state), MenuProvider, WorldlyContainer {
    var owner: UUID? = null
        private set
    var automationEnabled = false
        private set
    var storedCoins = 0L
        private set
    private val offers = TreeMap<Int, ShopOffer>()
    private var cachedOffers: List<ShopOffer>? = null
    private var cachedNextOfferNumber = 1
    internal var revision = 0L
        private set
    internal val pageCount: Int get() = maxOf(1, ((offers.size.toLong() + 8) / 9).toInt())
    val stock = ShopStock(this)

    private var displayItems: List<ItemStack> = emptyList()

    internal val displayStack: ItemStack
        get() {
            if (displayItems.isEmpty()) return ItemStack.EMPTY
            val seconds = (level?.gameTime ?: 0L) / 60L
            return displayItems[Math.floorMod(seconds, displayItems.size.toLong()).toInt()]
        }

    private fun updateDisplay() {
        val serverLevel = level as? ServerLevel ?: return
        serverLevel.sendBlockUpdated(blockPos, blockState, blockState, Block.UPDATE_CLIENTS)
    }

    override fun getUpdatePacket(): ClientboundBlockEntityDataPacket = ClientboundBlockEntityDataPacket.create(this)

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag = CompoundTag().apply {
        putBoolean("display_update", true)
        if (offers.isNotEmpty()) {
            val ops = registries.createSerializationContext(NbtOps.INSTANCE)
            val items = orderedOffers().map { it.stack.copyWithCount(1) }
            DISPLAY_ITEMS_CODEC.encodeStart(ops, items).result().ifPresent { put("display", it) }
        }
    }

    val shopBlock: ShopBlock get() = blockState.block as ShopBlock

    override fun setChanged() {
        revision++
        super.setChanged()
    }

    private fun orderedOffers(): List<ShopOffer> =
        cachedOffers ?: offers.values.toList().also { entries ->
            cachedOffers = entries
            var candidate = 0
            for (offer in entries) {
                if (offer.number != candidate) break
                if (candidate == Int.MAX_VALUE - 1) break
                candidate++
            }
            cachedNextOfferNumber = candidate + 1
        }

    internal val nextOfferNumber: Int get() {
        orderedOffers()
        return cachedNextOfferNumber
    }

    internal fun offerStack(number: Int): ItemStack? = offers[number]?.stack?.copy()

    internal fun pageOffers(page: Int): List<ShopOffer> {
        val entries = orderedOffers()
        val start = page.coerceIn(0, pageCount - 1) * 9
        return entries.subList(start, minOf(start + 9, entries.size)).toList()
    }

    private fun loadOffer(offer: ShopOffer) {
        if (!offer.stack.isEmpty && offer.stack.count <= offer.stack.maxStackSize) offers[offer.number] = offer
    }

    fun setAutomation(player: Player, enabled: Boolean): Boolean {
        if (!canManage(player)) return false
        if (automationEnabled != enabled) {
            automationEnabled = enabled
            setChanged()
        }
        return true
    }

    override fun getContainerSize(): Int = stock.containerSize
    override fun isEmpty(): Boolean = stock.isEmpty
    override fun getItem(slot: Int): ItemStack = stock.getItem(slot)
    override fun removeItem(slot: Int, amount: Int): ItemStack = stock.removeItem(slot, amount)
    override fun removeItemNoUpdate(slot: Int): ItemStack = stock.removeItemNoUpdate(slot)
    override fun setItem(slot: Int, stack: ItemStack) = stock.setItem(slot, stack)
    override fun clearContent() = stock.clearContent()
    override fun stillValid(player: Player): Boolean = canManage(player) &&
        level?.getBlockEntity(blockPos) === this && player.distanceToSqr(blockPos.x + .5, blockPos.y + .5, blockPos.z + .5) <= 64
    override fun getSlotsForFace(side: Direction): IntArray = stock.getSlotsForFace(side)
    override fun canPlaceItem(slot: Int, stack: ItemStack): Boolean = automationEnabled && !shopBlock.pawn
    override fun canTakeItem(target: Container, slot: Int, stack: ItemStack): Boolean = automationEnabled && shopBlock.pawn
    override fun canPlaceItemThroughFace(slot: Int, stack: ItemStack, side: Direction?): Boolean = canPlaceItem(slot, stack)
    override fun canTakeItemThroughFace(slot: Int, stack: ItemStack, side: Direction): Boolean = automationEnabled && shopBlock.pawn

    fun claim(player: Player) {
        if (owner == null) {
            owner = player.uuid
            setChanged()
        }
    }

    fun canManage(player: Player): Boolean = owner == player.uuid || (player as? ServerPlayer)?.permissions()?.hasPermission(Permission.HasCommandLevel(PermissionLevel.byId(2))) == true

    fun configure(player: Player, slot: Int, price: Long, template: ItemStack = player.mainHandItem): Boolean {
        if (!canManage(player) || slot < 0 || slot == Int.MAX_VALUE || price < 0) return false
        if (price == 0L) {
            if (offers.remove(slot) != null) {
                cachedOffers = null
                setChanged()
                updateDisplay()
            }
            return true
        }
        val stack = template
        if (stack.isEmpty || stack.count > stack.maxStackSize) return false
        val previous = offers[slot]
        if (previous != null && previous.price == price && ItemStack.matches(previous.stack, stack)) return true
        offers[slot] = ShopOffer(slot, price, stack.copy())
        cachedOffers = null
        setChanged()
        if (previous == null || !ItemStack.isSameItemSameComponents(previous.stack, stack)) updateDisplay()
        return true
    }

    fun withdraw(player: ServerPlayer): Boolean {
        if (!canManage(player) || storedCoins == 0L) return false
        val ledger = PurseLedger.get(player.level().server)
        val balance = ledger.balance(player.uuid)
        if (balance > Long.MAX_VALUE - storedCoins) return false
        ledger.setBalance(player.uuid, balance + storedCoins)
        storedCoins = 0L
        setChanged()
        return true
    }

    fun fundInventory(player: ServerPlayer): Boolean {
        if (!canManage(player) || !shopBlock.pawn) return false
        var amount = 0L
        val currencySlots = mutableListOf<Int>()
        for (slot in 0 until player.inventory.containerSize) {
            val value = PurseTransactions.value(player.inventory.getItem(slot)) ?: return false
            if (amount > Long.MAX_VALUE - value) return false
            amount += value
            if (value > 0) currencySlots.add(slot)
        }
        if (amount == 0L || storedCoins > Long.MAX_VALUE - amount) return false
        for (slot in currencySlots) player.inventory.setItem(slot, ItemStack.EMPTY)
        storedCoins += amount
        player.inventory.setChanged()
        setChanged()
        player.containerMenu.broadcastChanges()
        return true
    }

    fun fund(player: ServerPlayer, amount: Long): Boolean {
        if (!canManage(player) || amount <= 0 || storedCoins > Long.MAX_VALUE - amount) return false
        val ledger = PurseLedger.get(player.level().server)
        val balance = ledger.balance(player.uuid)
        if (balance < amount) return false
        ledger.setBalance(player.uuid, balance - amount)
        storedCoins += amount
        setChanged()
        return true
    }

    internal fun market(player: ServerPlayer): ShopMarket? {
        val config = CurrencyValues.configuration?.market ?: return null
        if (!config.enabled) return null
        val server = player.level().server
        val ledger = PurseLedger.get(server)
        return ledger.shopMarket.also { it.refresh(server.overworld().gameTime, config.resetIntervalSeconds.get(), ledger::setDirty) }
    }

    internal fun quote(offer: ShopOffer, market: ShopMarket?): Long {
        val expression = CurrencyValues.configuration?.market?.pricePercent ?: return offer.price
        val item = BuiltInRegistries.ITEM.getKey(offer.stack.item).toString()
        return market?.quote(item, offer.price, expression) ?: offer.price
    }

    fun trade(player: ServerPlayer, offer: Int): Boolean =
        offers[offer]?.let { trade(player, it) } ?: false

    internal fun trade(player: ServerPlayer, offer: ShopOffer, quotedPrice: Long? = null): Boolean {
        if (offers[offer.number] !== offer) return false
        val template = offer.stack
        val market = market(player)
        val price = quote(offer, market)
        if (quotedPrice != null && price != quotedPrice) return false
        if (template.isEmpty || price <= 0) return false
        val ledger = PurseLedger.get(player.level().server)
        val balance = ledger.balance(player.uuid)
        val pawn = shopBlock.pawn
        val unlimited = shopBlock.unlimited
        if (pawn) {
            if ((!unlimited && storedCoins < price) || balance > Long.MAX_VALUE - price) return false
            val playerSnapshot = InventoryTransactions.snapshot(player.inventory)
            val stockSnapshot = InventoryTransactions.snapshot(stock)
            if (!InventoryTransactions.removeMatching(player.inventory, template) || (!unlimited && !InventoryTransactions.insert(stock, template.copy()))) {
                InventoryTransactions.restore(player.inventory, playerSnapshot)
                InventoryTransactions.restore(stock, stockSnapshot)
                return false
            }
            ledger.setBalance(player.uuid, balance + price)
            if (!unlimited) storedCoins -= price
        } else {
            if (storedCoins > Long.MAX_VALUE - price) return false
            val payment = CurrencyPayment.plan(player.inventory, balance, price) ?: return false
            val playerSnapshot = InventoryTransactions.snapshot(player.inventory)
            val stockSnapshot = InventoryTransactions.snapshot(stock)
            payment.apply(player.inventory)
            if ((!unlimited && !InventoryTransactions.removeMatching(stock, template)) || !InventoryTransactions.insert(player.inventory, template.copy())) {
                InventoryTransactions.restore(player.inventory, playerSnapshot)
                InventoryTransactions.restore(stock, stockSnapshot)
                return false
            }
            ledger.setBalance(player.uuid, payment.balance)
            storedCoins += price
        }
        setChanged()
        if (market != null && owner != player.uuid) {
            market.record(BuiltInRegistries.ITEM.getKey(template.item).toString(), if (pawn) -template.count else template.count)
            ledger.setDirty()
        }
        player.containerMenu.broadcastChanges()
        return true
    }

    override fun preRemoveSideEffects(pos: BlockPos, state: BlockState) {
        level?.let { world ->
            Containers.dropContents(world, pos, stock)
            if (!world.isClientSide() && storedCoins > 0) Containers.dropItemStack(world, pos.x + .5, pos.y + .5, pos.z + .5, MoneyBagItem.create(storedCoins))
        }
    }

    override fun getDisplayName(): Component =
        components().get(DataComponents.CUSTOM_NAME) ?:
            Component.translatable(if (shopBlock.pawn) "container.coinage.pawn_shop" else "container.coinage.shop")
    override fun createMenu(id: Int, inventory: Inventory, player: Player): AbstractContainerMenu = ShopMenu(id, inventory, this)

    override fun saveAdditional(tag: ValueOutput) {
        super.saveAdditional(tag)
        owner?.let { tag.store("owner", UUIDUtil.CODEC, it) }
        tag.putLong("coins", storedCoins)
        tag.putBoolean("automation", automationEnabled)
        tag.store("offerList", ShopOffer.LIST_CODEC, orderedOffers())
        val stockTag = tag.child("stock")
        ContainerHelper.saveAllItems(stockTag, NonNullList.withSize(27, ItemStack.EMPTY).also { list ->
            for (slot in list.indices) list[slot] = stock.getItem(slot)
        })
    }

    override fun loadAdditional(tag: ValueInput) {
        super.loadAdditional(tag)
        if (tag.getBooleanOr("display_update", false)) {
            displayItems = tag.read("display", DISPLAY_ITEMS_CODEC).orElse(emptyList())
            return
        }
        owner = tag.read("owner", UUIDUtil.CODEC).orElse(null)
        storedCoins = tag.getLongOr("coins", 0).coerceAtLeast(0)
        automationEnabled = tag.getBooleanOr("automation", false)
        offers.clear()
        cachedOffers = null
        val savedOffers = tag.childrenList("offerList")
        if (savedOffers.isPresent) {
            for (entry in savedOffers.get()) {
                val number = entry.getIntOr("slot", -1)
                val price = entry.getLongOr("price", 0)
                val item = entry.read("item", ItemStack.CODEC).orElse(ItemStack.EMPTY)
                if (number >= 0 && number < Int.MAX_VALUE && price > 0) loadOffer(ShopOffer(number, price, item))
                else LOGGER.warn("Ignored an invalid shop offer at {}", blockPos)
            }
        }
        val list = NonNullList.withSize(27, ItemStack.EMPTY)
        ContainerHelper.loadAllItems(tag.childOrEmpty("stock"), list)
        for (slot in list.indices) stock.setItem(slot, list[slot])
    }

    companion object {
        private val DISPLAY_ITEMS_CODEC = ItemStack.CODEC.listOf()
        private val LOGGER = LoggerFactory.getLogger("Coinage/Shops")
    }
}
