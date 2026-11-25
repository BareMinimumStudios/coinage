package net.bms.coinage.mixin;

import net.bms.coinage.EasyNpcMarket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantResultSlot;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Slot.class)
abstract class EasyNpcMarketPickupMixin {
    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void coinage$guardMarketPrice(Player player, CallbackInfoReturnable<Boolean> callback) {
        if ((Object) this instanceof MerchantResultSlot && player instanceof ServerPlayer serverPlayer
            && EasyNpcMarket.stale(serverPlayer)) callback.setReturnValue(false);
    }
}
