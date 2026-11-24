package net.bms.coinage

import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.RegisterCommandsEvent

@Mod(MOD_ID)
class CoinageNeoForge(modBus: net.neoforged.bus.api.IEventBus) {
    init {
        modBus.addListener { event: net.neoforged.neoforge.registries.RegisterEvent ->
            event.register(net.minecraft.core.registries.Registries.ITEM) { registry ->
                CoinageItems.entries.forEach { (name, item) ->
                    registry.register(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MOD_ID, name), item)
                }
            }
        }
        val commands = EconomyCommands(EconomyConfig.register())
        NeoForge.EVENT_BUS.addListener { event: RegisterCommandsEvent -> commands.register(event.dispatcher) }
    }
}
