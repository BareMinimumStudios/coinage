package net.bms.numismatic_overhaul

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback

object NumismaticFabric : ModInitializer {
    override fun onInitialize() {
        val commands = EconomyCommands(EconomyConfig.register())
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ -> commands.register(dispatcher) }
    }
}
