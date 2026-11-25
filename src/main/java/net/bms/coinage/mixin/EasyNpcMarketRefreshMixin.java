package net.bms.coinage.mixin;

import net.bms.coinage.EasyNpcMarket;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MerchantMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
abstract class EasyNpcMarketRefreshMixin {
    @Inject(method = "broadcastChanges", at = @At("HEAD"))
    private void coinage$refreshMarket(CallbackInfo callback) {
        if ((Object) this instanceof MerchantMenu menu) EasyNpcMarket.update(menu);
    }
}
