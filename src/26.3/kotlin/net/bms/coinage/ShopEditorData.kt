package net.bms.coinage

import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData

internal object ShopEditorData {
    private fun data(stack: ItemStack) = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag()

    fun markMenu(stack: ItemStack, canManage: Boolean, next: Int) {
        val tag = CompoundTag()
        tag.putBoolean("coinage_shop", true)
        tag.putBoolean("coinage_manage", canManage)
        tag.putInt("coinage_next_offer", next)
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag))
    }

    fun markOffer(stack: ItemStack, number: Int, price: Long) {
        val tag = data(stack)
        tag.putInt("coinage_offer_number", number)
        tag.putLong("coinage_offer_price", price)
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag))
    }

    fun canManage(stack: ItemStack): Boolean = data(stack).let {
        it.getBooleanOr("coinage_shop", false) && it.getBooleanOr("coinage_manage", false)
    }
    fun nextOffer(stack: ItemStack): Int = data(stack).getIntOr("coinage_next_offer", 1).coerceAtLeast(1)
    fun offerNumber(stack: ItemStack): Int = data(stack).getIntOr("coinage_offer_number", 0)
    fun offerPrice(stack: ItemStack): Long = data(stack).getLongOr("coinage_offer_price", 0)
}
