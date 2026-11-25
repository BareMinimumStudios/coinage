package net.bms.coinage.mixin;

import net.bms.coinage.MerchantPayments;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.Merchant;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MerchantMenu.class)
public abstract class MerchantPaymentsMixin {
    @Shadow @Final private Merchant trader;
    @Shadow @Final private MerchantContainer tradeContainer;

    @Inject(method = "tryMoveItems", at = @At("TAIL"))
    private void coinage$payFromPurse(int selected, CallbackInfo callback) {
        MerchantPayments.autofill(trader, tradeContainer, selected);
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void coinage$refundUnspentCoins(Player player, CallbackInfo callback) {
        MerchantPayments.refund(trader, tradeContainer);
    }
}
