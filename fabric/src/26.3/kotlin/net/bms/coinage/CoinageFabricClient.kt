package net.bms.coinage

import net.bms.coinage.client.PurseClient
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.minecraft.resources.Identifier
import net.minecraft.client.KeyMapping
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.screen.v1.Screens

object CoinageFabricClient : ClientModInitializer {
    override fun onInitializeClient() {
        PurseClient.initialize()
        KeyMapping.Category.register(PurseClient.key.category.id())
        KeyMappingHelper.registerKeyMapping(PurseClient.key)
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
        ClientTickEvents.END_CLIENT_TICK.register { PurseClient.tick() }
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "purse")) { graphics, _ -> PurseClient.render(graphics) }
        ScreenEvents.AFTER_INIT.register { _, screen, _, _ ->
            val buttons = PurseClient.inventoryButtons(screen)
            Screens.getWidgets(screen).addAll(buttons)
            ScreenEvents.afterTick(screen).register {
                buttons.firstOrNull()?.active = ClientPurseState.ready
            }
        }
    }
}
