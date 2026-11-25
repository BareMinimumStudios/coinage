package net.bms.coinage

import net.minecraft.core.Direction
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper

internal object ShopNeoForgeAutomation {
    fun register(bus: IEventBus) {
        bus.addListener { event: RegisterCapabilitiesEvent ->
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CoinageBlocks.shopType) { shop, side ->
                SidedInvWrapper(shop.stock, side ?: Direction.DOWN)
            }
        }
    }
}
