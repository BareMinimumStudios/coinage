package net.bms.coinage

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking

internal object PurseFabricNetwork {
    fun register() {
        PayloadTypeRegistry.serverboundPlay().register(PursePayloads.HELLO, PursePayloads.HELLO_CODEC)
        PayloadTypeRegistry.serverboundPlay().register(PursePayloads.OPEN, PursePayloads.OPEN_CODEC)
        PayloadTypeRegistry.clientboundPlay().register(PursePayloads.SNAPSHOT, PursePayloads.SNAPSHOT_CODEC)
        PurseSync.send = { player, snapshot ->
            if (ServerPlayNetworking.canSend(player, PursePayloads.SNAPSHOT)) ServerPlayNetworking.send(player, snapshot)
        }
        ServerPlayNetworking.registerGlobalReceiver(PursePayloads.HELLO) { packet, context ->
            context.server().execute { PurseSync.hello(context.player(), packet.protocol) }
        }
        ServerPlayNetworking.registerGlobalReceiver(PursePayloads.OPEN) { _, context ->
            context.server().execute { PurseSync.requestOpen(context.player()) }
        }
        ServerTickEvents.END_SERVER_TICK.register(PurseSync::tick)
        ServerPlayConnectionEvents.DISCONNECT.register { handler, _ -> PurseSync.leave(handler.player) }
        ServerLifecycleEvents.SERVER_STOPPED.register(PurseSync::stop)
    }
}
