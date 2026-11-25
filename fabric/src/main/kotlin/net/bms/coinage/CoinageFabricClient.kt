package net.bms.coinage

import net.bms.coinage.client.PurseAmountClient
import net.bms.coinage.client.PursePopupClient
import net.bms.coinage.client.PurseClient
import net.bms.coinage.client.ShopClient
import net.bms.coinage.client.ShopDisplayRenderer
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents
import net.fabricmc.fabric.api.client.screen.v1.Screens

object CoinageFabricClient : ClientModInitializer {
    override fun onInitializeClient() {
        BlockEntityRendererRegistry.register(CoinageBlocks.shopType, ::ShopDisplayRenderer)
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
        ClientTickEvents.END_CLIENT_TICK.register { PurseClient.tick(); PurseAmountClient.tick(); ShopClient.tick() }
        HudRenderCallback.EVENT.register { graphics, _ -> PurseClient.render(graphics) }
        ScreenEvents.AFTER_INIT.register { _, screen, _, _ ->
            ScreenKeyboardEvents.allowKeyPress(screen).register { active, code, scanCode, _ ->
                !PurseClient.openFromContainer(active, code, scanCode)
            }
            val buttons = PurseClient.inventoryButtons(screen)
            val shopButtons = ShopClient.buttons(screen) + PurseAmountClient.buttons(screen)
            Screens.getButtons(screen).addAll(buttons)
            Screens.getButtons(screen).addAll(shopButtons)
        }
    }
}
