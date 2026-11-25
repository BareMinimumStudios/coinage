package net.bms.coinage.mixin;

import net.bms.coinage.TradeCatalog;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.entity.npc.villager.AbstractVillager")
public abstract class MerchantCurrencyMixin {
    @Inject(method = "getOffers", at = @At("RETURN"))
    private void coinage$convertEmeralds(CallbackInfoReturnable<MerchantOffers> callback) {
        TradeCatalog.remap((LivingEntity) (Object) this, callback.getReturnValue());
    }
}
