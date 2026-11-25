package net.bms.coinage

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking

internal object CoinageFabricNetwork {
    fun register() {
        PayloadTypeRegistry.serverboundPlay().register(PurseWithdrawPayloads.QUICK, PurseWithdrawPayloads.QUICK_CODEC)
        PayloadTypeRegistry.clientboundPlay().register(PursePayloads.SHOW, PursePayloads.SHOW_CODEC)
        ServerPlayNetworking.registerGlobalReceiver(PurseWithdrawPayloads.QUICK) { packet, context ->
            context.server().execute { PurseWithdrawal.quick(context.player(), packet) }
        }
        PurseMenu.show = { player ->
            val available = ServerPlayNetworking.canSend(player, PursePayloads.SHOW)
            if (available) ServerPlayNetworking.send(player, PursePayloads.Show())
            available
        }
        PayloadTypeRegistry.serverboundPlay().register(PurseWithdrawPayloads.WITHDRAW, PurseWithdrawPayloads.WITHDRAW_CODEC)
        PayloadTypeRegistry.clientboundPlay().register(PurseWithdrawPayloads.RESULT, PurseWithdrawPayloads.RESULT_CODEC)
        PurseWithdrawal.send = { player, reply ->
            if (ServerPlayNetworking.canSend(player, PurseWithdrawPayloads.RESULT)) ServerPlayNetworking.send(player, reply)
        }
        ServerPlayNetworking.registerGlobalReceiver(PurseWithdrawPayloads.WITHDRAW) { packet, context ->
            context.server().execute { PurseWithdrawal.withdraw(context.player(), packet) }
        }
        PayloadTypeRegistry.serverboundPlay().register(ShopEditPayloads.EDIT, ShopEditPayloads.EDIT_CODEC)
        PayloadTypeRegistry.clientboundPlay().register(ShopEditPayloads.RESULT, ShopEditPayloads.RESULT_CODEC)
        ShopEditing.send = { player, reply ->
            if (ServerPlayNetworking.canSend(player, ShopEditPayloads.RESULT)) ServerPlayNetworking.send(player, reply)
        }
        ServerPlayNetworking.registerGlobalReceiver(ShopEditPayloads.EDIT) { packet, context ->
            context.server().execute { ShopEditing.edit(context.player(), packet) }
        }
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
