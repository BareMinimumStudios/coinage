package net.bms.coinage.mixin;

import net.bms.coinage.ShopClickGuard;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ShopClickGuardMixin {
    @Shadow public ServerPlayer player;

    @Inject(method = "handleContainerClick", at = @At("HEAD"), cancellable = true)
    private void coinage$rejectStaleOffer(ServerboundContainerClickPacket packet, CallbackInfo callback) {
        // Vanilla schedules this handler again on the server thread; do not inspect menus on Netty.
        MinecraftServer server = player.level().getServer();
        if (server != null && server.isSameThread() && ShopClickGuard.reject(player,
                packet.containerId(), packet.stateId(), packet.slotNum())) callback.cancel();
    }
}
