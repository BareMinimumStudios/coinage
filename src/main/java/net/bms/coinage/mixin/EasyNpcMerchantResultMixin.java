package net.bms.coinage.mixin;

import net.bms.coinage.EasyNpcMarket;
import net.minecraft.world.inventory.MerchantResultSlot;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MerchantResultSlot.class)
abstract class EasyNpcMerchantResultMixin {
    @Redirect(method = "onTake", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/item/trading/Merchant;notifyTrade(Lnet/minecraft/world/item/trading/MerchantOffer;)V"))
    private void coinage$recordMarketTrade(Merchant merchant, MerchantOffer offer) {
        EasyNpcMarket.complete(merchant, offer);
    }
}
