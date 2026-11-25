package net.bms.coinage.mixin;

import net.bms.coinage.EasyNpcMarket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ServerPlayer.class)
abstract class EasyNpcMarketSyncMixin {
    @ModifyVariable(method = "sendMerchantOffers", at = @At("HEAD"), argsOnly = true)
    private MerchantOffers coinage$sendMarketOffers(MerchantOffers offers) {
        return EasyNpcMarket.outgoing((ServerPlayer) (Object) this, offers);
    }
}
