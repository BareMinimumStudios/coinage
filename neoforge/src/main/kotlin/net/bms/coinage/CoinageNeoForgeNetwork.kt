package net.bms.coinage

import net.minecraft.server.level.ServerPlayer
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import net.neoforged.neoforge.event.server.ServerStoppedEvent
import net.neoforged.neoforge.event.tick.ServerTickEvent
import net.neoforged.neoforge.network.PacketDistributor
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent

internal object CoinageNeoForgeNetwork {
    fun register(bus: IEventBus) {
        ShopEditing.send = { player, reply ->
            if (player.connection.hasChannel(ShopEditPayloads.RESULT)) PacketDistributor.sendToPlayer(player, reply)
        }
        PurseSync.send = { player, snapshot ->
            if (player.connection.hasChannel(PursePayloads.SNAPSHOT)) PacketDistributor.sendToPlayer(player, snapshot)
        }
        bus.addListener { event: RegisterPayloadHandlersEvent ->
            val registrar = event.registrar(PursePayloads.PROTOCOL.toString()).optional()
            registrar.playToServer(ShopEditPayloads.EDIT, ShopEditPayloads.EDIT_CODEC) { packet, context ->
                context.enqueueWork { ShopEditing.edit(context.player() as ServerPlayer, packet) }
            }
            registrar.playToClient(ShopEditPayloads.RESULT, ShopEditPayloads.RESULT_CODEC) { packet, context ->
                context.enqueueWork { ShopEditPayloads.receive(packet) }
            }
            registrar.playToServer(PursePayloads.HELLO, PursePayloads.HELLO_CODEC) { packet, context ->
                context.enqueueWork { PurseSync.hello(context.player() as ServerPlayer, packet.protocol) }
            }
            registrar.playToServer(PursePayloads.OPEN, PursePayloads.OPEN_CODEC) { _, context ->
                context.enqueueWork { PurseSync.requestOpen(context.player() as ServerPlayer) }
            }
            registrar.playToClient(PursePayloads.SNAPSHOT, PursePayloads.SNAPSHOT_CODEC) { packet, context ->
                context.enqueueWork { ClientPurseState.accept(packet) }
            }
        }
        NeoForge.EVENT_BUS.addListener { event: ServerTickEvent.Post -> PurseSync.tick(event.server) }
        NeoForge.EVENT_BUS.addListener { event: PlayerEvent.PlayerLoggedOutEvent ->
            (event.entity as? ServerPlayer)?.let(PurseSync::leave)
        }
        NeoForge.EVENT_BUS.addListener { event: ServerStoppedEvent -> PurseSync.stop(event.server) }
    }
}
