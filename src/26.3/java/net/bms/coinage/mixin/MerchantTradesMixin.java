package net.bms.coinage.mixin;

import net.bms.coinage.TradeCatalog;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.trading.Merchant;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = {"net.minecraft.world.entity.npc.villager.Villager", "net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader"})
public abstract class MerchantTradesMixin {
    @Inject(method = "updateTrades", at = @At("HEAD"), cancellable = true)
    private void coinage$replaceTrades(CallbackInfo callback) {
        if (TradeCatalog.replace((LivingEntity) (Object) this, ((Merchant) this).getOffers())) callback.cancel();
    }

    @Inject(method = "updateTrades", at = @At("TAIL"))
    private void coinage$appendTrades(CallbackInfo callback) {
        TradeCatalog.afterNative((LivingEntity) (Object) this, ((Merchant) this).getOffers());
    }
}
