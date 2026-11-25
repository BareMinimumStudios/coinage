package net.bms.coinage.mixin;

import net.bms.coinage.MerchantBagPricing;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MerchantOffer.class)
public abstract class MerchantBagPricingMixin {
    @Inject(method = "getCostA", at = @At("HEAD"), cancellable = true)
    private void coinage$adjustBagValue(CallbackInfoReturnable<ItemStack> callback) {
        ItemStack adjusted = MerchantBagPricing.adjustedCost((MerchantOffer) (Object) this);
        if (adjusted != null) callback.setReturnValue(adjusted);
    }

    @Inject(method = "satisfiedBy", at = @At("HEAD"), cancellable = true)
    private void coinage$checkAdjustedBag(ItemStack first, ItemStack second, CallbackInfoReturnable<Boolean> callback) {
        Boolean matches = MerchantBagPricing.satisfiedBy((MerchantOffer) (Object) this, first, second);
        if (matches != null) callback.setReturnValue(matches);
    }
}
