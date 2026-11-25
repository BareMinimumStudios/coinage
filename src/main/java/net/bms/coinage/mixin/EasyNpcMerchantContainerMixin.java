package net.bms.coinage.mixin;

import net.bms.coinage.EasyNpcMarket;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MerchantContainer.class)
abstract class EasyNpcMerchantContainerMixin {
    @Redirect(method = "updateSellItem", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/item/trading/Merchant;getOffers()Lnet/minecraft/world/item/trading/MerchantOffers;"))
    private MerchantOffers coinage$marketOffers(Merchant merchant) {
        return EasyNpcMarket.offers(merchant, merchant.getOffers());
    }
}
