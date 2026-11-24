package net.bms.coinage

import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.RegisterCommandsEvent

@Mod(MOD_ID)
class CoinageNeoForge {
    init {
        val commands = EconomyCommands(EconomyConfig.register())
        NeoForge.EVENT_BUS.addListener { event: RegisterCommandsEvent -> commands.register(event.dispatcher) }
    }
}
