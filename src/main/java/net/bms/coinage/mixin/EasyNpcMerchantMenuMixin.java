package net.bms.coinage.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.bms.coinage.EasyNpcMarket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MerchantMenu.class)
abstract class EasyNpcMerchantMenuMixin {
    @Shadow @Final private Merchant trader;
    @Shadow @Final private MerchantContainer tradeContainer;

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/item/trading/Merchant;)V",
        at = @At("TAIL"))
    private void coinage$marketSession(int id, Inventory inventory, Merchant merchant, CallbackInfo callback) {
        EasyNpcMarket.open((MerchantMenu) (Object) this, merchant, tradeContainer);
    }

    @ModifyReturnValue(method = "getOffers", at = @At("RETURN"))
    private MerchantOffers coinage$marketOffers(MerchantOffers offers) {
        return EasyNpcMarket.offers(trader, offers);
    }
}
