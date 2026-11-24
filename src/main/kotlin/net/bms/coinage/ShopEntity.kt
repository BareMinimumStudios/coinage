package net.bms.coinage

import java.util.UUID
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.core.NonNullList
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.Container
import net.minecraft.world.ContainerHelper
import net.minecraft.world.MenuProvider
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState

class ShopEntity(pos: BlockPos, state: BlockState) : BlockEntity(CoinageBlocks.shopType, pos, state), MenuProvider {
    var owner: UUID? = null
        private set
    var storedCoins = 0L
        private set
    val prices = LongArray(9)
    val offers = NonNullList.withSize(9, ItemStack.EMPTY)
    val stock = object : SimpleContainer(27) {
        override fun setChanged() {
            super.setChanged()
            this@ShopEntity.setChanged()
        }
    }

    val shopBlock: ShopBlock get() = blockState.block as ShopBlock

    fun claim(player: Player) {
        if (owner == null) {
            owner = player.uuid
            setChanged()
        }
    }

    fun canManage(player: Player): Boolean = owner == player.uuid || player.hasPermissions(2)

    fun configure(player: Player, slot: Int, price: Long): Boolean {
        if (!canManage(player) || slot !in 0..8 || price < 0) return false
        offers[slot] = if (price == 0L) ItemStack.EMPTY else player.mainHandItem.copy()
        prices[slot] = price
        setChanged()
        return true
    }

    fun withdraw(player: ServerPlayer): Boolean {
        if (!canManage(player) || storedCoins == 0L) return false
        val ledger = PurseLedger.get(player.server)
        val balance = ledger.balance(player.uuid)
        if (balance > Long.MAX_VALUE - storedCoins) return false
        ledger.setBalance(player.uuid, balance + storedCoins)
        storedCoins = 0L
        setChanged()
        return true
    }

    fun fund(player: ServerPlayer, amount: Long): Boolean {
        if (!canManage(player) || amount <= 0 || storedCoins > Long.MAX_VALUE - amount) return false
        val ledger = PurseLedger.get(player.server)
        val balance = ledger.balance(player.uuid)
        if (balance < amount) return false
        ledger.setBalance(player.uuid, balance - amount)
        storedCoins += amount
        setChanged()
        return true
    }

    fun trade(player: ServerPlayer, offer: Int): Boolean {
        if (offer !in 0..8) return false
        val template = offers[offer]
        val price = prices[offer]
        if (template.isEmpty || price <= 0) return false
        val ledger = PurseLedger.get(player.server)
        val balance = ledger.balance(player.uuid)
        val pawn = shopBlock.pawn
        val unlimited = shopBlock.unlimited
        if (pawn) {
            if ((!unlimited && storedCoins < price) || balance > Long.MAX_VALUE - price) return false
            val playerSnapshot = InventoryTransactions.snapshot(player.inventory)
            val stockSnapshot = InventoryTransactions.snapshot(stock)
            if (!InventoryTransactions.removeMatching(
                    player.inventory,
                    template
                ) || (!unlimited && !InventoryTransactions.insert(stock, template.copy()))
            ) {
                InventoryTransactions.restore(player.inventory, playerSnapshot)
                InventoryTransactions.restore(stock, stockSnapshot)
                return false
            }
            ledger.setBalance(player.uuid, balance + price)
            if (!unlimited) storedCoins -= price
        } else {
            if (balance < price || storedCoins > Long.MAX_VALUE - price) return false
            val playerSnapshot = InventoryTransactions.snapshot(player.inventory)
            val stockSnapshot = InventoryTransactions.snapshot(stock)
            if ((!unlimited && !InventoryTransactions.removeMatching(stock, template)) || !InventoryTransactions.insert(
                    player.inventory,
                    template.copy()
                )
            ) {
                InventoryTransactions.restore(player.inventory, playerSnapshot)
                InventoryTransactions.restore(stock, stockSnapshot)
                return false
            }
            ledger.setBalance(player.uuid, balance - price)
            storedCoins += price
        }
        setChanged()
        player.containerMenu.broadcastChanges()
        return true
    }

    override fun getDisplayName(): Component =
        Component.translatable(if (shopBlock.pawn) "container.coinage.pawn_shop" else "container.coinage.shop")

    override fun createMenu(id: Int, inventory: Inventory, player: Player): AbstractContainerMenu =
        ShopMenu(id, inventory, this)

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        owner?.let { tag.putUUID("owner", it) }
        tag.putLong("coins", storedCoins)
        tag.putLongArray("prices", prices)
        ContainerHelper.saveAllItems(tag, offers, registries)
        val stockTag = CompoundTag()
        ContainerHelper.saveAllItems(stockTag, NonNullList.withSize(27, ItemStack.EMPTY).also { list ->
            for (slot in list.indices) list[slot] = stock.getItem(slot)
        }, registries)
        tag.put("stock", stockTag)
    }

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        owner = if (tag.hasUUID("owner")) tag.getUUID("owner") else null
        storedCoins = tag.getLong("coins").coerceAtLeast(0)
        val savedPrices = tag.getLongArray("prices")
        for (slot in prices.indices) prices[slot] = savedPrices.getOrElse(slot) { 0 }.coerceAtLeast(0)
        ContainerHelper.loadAllItems(tag, offers, registries)
        val list = NonNullList.withSize(27, ItemStack.EMPTY)
        ContainerHelper.loadAllItems(tag.getCompound("stock"), list, registries)
        for (slot in list.indices) stock.setItem(slot, list[slot])
    }
}
