package net.bms.coinage

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback

object CoinageFabric : ModInitializer {
    override fun onInitialize() {
        CoinageItems.entries.forEach { (name, item) ->
            net.minecraft.core.Registry.register(
                net.minecraft.core.registries.BuiltInRegistries.ITEM,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MOD_ID, name), item
            )
        }
        val commands = EconomyCommands(EconomyConfig.register())
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ -> commands.register(dispatcher) }
    }
}
