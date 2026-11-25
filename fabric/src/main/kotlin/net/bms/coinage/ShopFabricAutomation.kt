package net.bms.coinage

import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage
import net.minecraft.core.Direction

internal object ShopFabricAutomation {
    fun register() {
        ItemStorage.SIDED.registerForBlockEntity({ shop, side ->
            InventoryStorage.of(shop.stock, side ?: Direction.DOWN)
        }, CoinageBlocks.shopType)
    }
}
