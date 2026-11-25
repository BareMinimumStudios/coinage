package net.bms.coinage

import net.minecraft.core.Direction
import net.minecraft.world.SimpleContainer
import net.minecraft.world.WorldlyContainer
import net.minecraft.world.item.ItemStack

class ShopStock(private val shop: ShopEntity) : SimpleContainer(27), WorldlyContainer {
    override fun setChanged() {
        super.setChanged()
        shop.setChanged()
    }

    override fun getSlotsForFace(side: Direction): IntArray = if (shop.automationEnabled) SLOTS else NO_SLOTS
    override fun canPlaceItemThroughFace(slot: Int, stack: ItemStack, side: Direction?): Boolean =
        shop.automationEnabled && !shop.shopBlock.pawn
    override fun canTakeItemThroughFace(slot: Int, stack: ItemStack, side: Direction): Boolean =
        shop.automationEnabled && shop.shopBlock.pawn

    companion object {
        private val SLOTS = IntArray(27) { it }
        private val NO_SLOTS = intArrayOf()
    }
}
