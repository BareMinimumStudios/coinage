package net.bms.coinage

import net.bms.coinage.client.PurseClient
import net.bms.coinage.client.ShopClient
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.screen.v1.Screens

object CoinageFabricClient : ClientModInitializer {
    override fun onInitializeClient() {
        PurseClient.initialize()
        ShopClient.initialize()
        ShopClient.available = { ClientPlayNetworking.canSend(ShopEditPayloads.EDIT) }
        ShopClient.send = { ClientPlayNetworking.send(it) }
        ClientPlayNetworking.registerGlobalReceiver(ShopEditPayloads.RESULT) { packet, context ->
            context.client().execute { ShopEditPayloads.receive(packet) }
        }
        KeyBindingHelper.registerKeyBinding(PurseClient.key)
        PurseClient.requestOpen = {
            if (ClientPlayNetworking.canSend(PursePayloads.OPEN)) ClientPlayNetworking.send(PursePayloads.Open())
        }
        ClientPlayNetworking.registerGlobalReceiver(PursePayloads.SNAPSHOT) { packet, context ->
            context.client().execute { ClientPurseState.accept(packet) }
        }
        ClientPlayConnectionEvents.JOIN.register { _, _, _ ->
            PurseClient.reset()
            if (ClientPlayNetworking.canSend(PursePayloads.HELLO)) ClientPlayNetworking.send(PursePayloads.Hello())
        }
        ClientPlayConnectionEvents.DISCONNECT.register { _, _ -> PurseClient.reset() }
        ClientTickEvents.END_CLIENT_TICK.register { PurseClient.tick(); ShopClient.tick() }
        HudRenderCallback.EVENT.register { graphics, _ -> PurseClient.render(graphics) }
        ScreenEvents.AFTER_INIT.register { _, screen, _, _ ->
            val buttons = PurseClient.inventoryButtons(screen)
            val shopButtons = ShopClient.buttons(screen)
            Screens.getButtons(screen).addAll(buttons)
            Screens.getButtons(screen).addAll(shopButtons)
            ScreenEvents.afterTick(screen).register {
                buttons.firstOrNull()?.active = ClientPurseState.ready
            }
        }
    }
}
