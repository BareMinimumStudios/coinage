package net.bms.coinage

import net.bms.coinage.client.PurseAmountClient
import net.bms.coinage.client.PursePopupClient
import net.bms.coinage.client.PurseClient
import net.bms.coinage.client.ShopClient
import net.bms.coinage.client.ShopDisplayRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.minecraft.resources.Identifier
import net.minecraft.client.KeyMapping
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents
import net.fabricmc.fabric.api.client.screen.v1.Screens

object CoinageFabricClient : ClientModInitializer {
    override fun onInitializeClient() {
        BlockEntityRenderers.register(CoinageBlocks.shopType, ::ShopDisplayRenderer)
        PurseClient.initialize(); PurseAmountClient.initialize()
        ShopClient.initialize()
        PursePopupClient.selectionAvailable = { ClientPlayNetworking.canSend(PurseWithdrawPayloads.SELECTED) }
        PursePopupClient.selected = { ClientPlayNetworking.send(it) }
        PursePopupClient.quickAvailable = { ClientPlayNetworking.canSend(PurseWithdrawPayloads.QUICK) }
        PursePopupClient.quick = { ClientPlayNetworking.send(it) }
        ClientPlayNetworking.registerGlobalReceiver(PursePayloads.SHOW) { _, context ->
            context.client().execute { PursePayloads.show() }
        }
        PurseAmountClient.available = { ClientPlayNetworking.canSend(PurseWithdrawPayloads.WITHDRAW) }
        PurseAmountClient.send = { ClientPlayNetworking.send(it) }
        ClientPlayNetworking.registerGlobalReceiver(PurseWithdrawPayloads.RESULT) { packet, context ->
            context.client().execute { PurseWithdrawPayloads.receive(packet) }
        }
        ShopClient.available = { ClientPlayNetworking.canSend(ShopEditPayloads.EDIT) }
        ShopClient.send = { ClientPlayNetworking.send(it) }
        ClientPlayNetworking.registerGlobalReceiver(ShopEditPayloads.RESULT) { packet, context ->
            context.client().execute { ShopEditPayloads.receive(packet) }
        }
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
        ClientTickEvents.END_CLIENT_TICK.register { PurseClient.tick(); PurseAmountClient.tick(); ShopClient.tick() }
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "purse")) { graphics, _ -> PurseClient.render(graphics) }
        ScreenEvents.AFTER_INIT.register { _, screen, _, _ ->
            ScreenKeyboardEvents.allowKeyPress(screen).register { active, event ->
                !PurseClient.openFromContainer(active, event)
            }
            val buttons = PurseClient.inventoryButtons(screen)
            val shopButtons = ShopClient.buttons(screen) + PurseAmountClient.buttons(screen)
            Screens.getWidgets(screen).addAll(buttons)
            Screens.getWidgets(screen).addAll(shopButtons)
        }
    }
}
