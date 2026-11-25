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
        it.getBoolean("coinage_shop") && it.getBoolean("coinage_manage")
    }
    fun nextOffer(stack: ItemStack): Int = data(stack).getInt("coinage_next_offer").coerceAtLeast(1)
    fun offerNumber(stack: ItemStack): Int = data(stack).getInt("coinage_offer_number")
    fun offerPrice(stack: ItemStack): Long = data(stack).getLong("coinage_offer_price")
}
