package net.bms.coinage.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.bms.coinage.MerchantBagPricing;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.world.entity.npc.villager.Villager")
public abstract class VillagerBagDiscountMixin {
    @ModifyExpressionValue(method = "updateSpecialPrices", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/trading/MerchantOffer;getBaseCostA()Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack coinage$bagHeroDiscount(ItemStack original) {
        return MerchantBagPricing.heroPriceBasis(original);
    }
}
