package net.bms.coinage

import net.bms.coinage.client.PurseAmountClient
import net.bms.coinage.client.PurseClient
import net.bms.coinage.client.ShopClient
import net.minecraft.client.Minecraft
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.common.Mod
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent
import net.neoforged.neoforge.client.event.ClientTickEvent
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent
import net.neoforged.neoforge.client.event.RenderGuiEvent
import net.neoforged.neoforge.client.event.ScreenEvent
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.client.network.ClientPacketDistributor

@Mod(value = MOD_ID, dist = [Dist.CLIENT])
class CoinageNeoForgeClient(modBus: IEventBus) {
    init {
        modBus.addListener { event: FMLClientSetupEvent -> event.enqueueWork { PurseClient.initialize(); PurseAmountClient.initialize(); ShopClient.initialize() } }
        modBus.addListener { event: RegisterKeyMappingsEvent -> event.registerCategory(PurseClient.key.category); event.register(PurseClient.key) }
        PurseAmountClient.available = { Minecraft.getInstance().connection?.hasChannel(PurseWithdrawPayloads.WITHDRAW) == true }
        PurseAmountClient.send = { ClientPacketDistributor.sendToServer(it) }
        ShopClient.available = { Minecraft.getInstance().connection?.hasChannel(ShopEditPayloads.EDIT) == true }
        ShopClient.send = { ClientPacketDistributor.sendToServer(it) }
        PurseClient.requestOpen = {
            if (Minecraft.getInstance().connection?.hasChannel(PursePayloads.OPEN) == true)
                ClientPacketDistributor.sendToServer(PursePayloads.Open())
        }
        NeoForge.EVENT_BUS.addListener { event: ClientPlayerNetworkEvent.LoggingIn ->
            PurseClient.reset()
            if (Minecraft.getInstance().connection?.hasChannel(PursePayloads.HELLO) == true)
                ClientPacketDistributor.sendToServer(PursePayloads.Hello())
        }
        NeoForge.EVENT_BUS.addListener { event: ClientPlayerNetworkEvent.LoggingOut -> PurseClient.reset() }
        NeoForge.EVENT_BUS.addListener { event: ClientTickEvent.Post -> PurseClient.tick(); PurseAmountClient.tick(); ShopClient.tick() }
        NeoForge.EVENT_BUS.addListener { event: RenderGuiEvent.Post -> PurseClient.render(event.guiGraphics) }
        NeoForge.EVENT_BUS.addListener { event: ScreenEvent.Init.Post ->
            PurseClient.inventoryButtons(event.screen).forEach(event::addListener)
            ShopClient.buttons(event.screen).forEach(event::addListener)
            PurseAmountClient.buttons(event.screen).forEach(event::addListener)
        }
    }
}
