package net.bms.coinage

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback

object CoinageFabric : ModInitializer {
    override fun onInitialize() {
        val commands = EconomyCommands(EconomyConfig.register())
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ -> commands.register(dispatcher) }
    }
}
