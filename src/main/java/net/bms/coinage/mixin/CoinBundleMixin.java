package net.bms.coinage.mixin;

import net.bms.coinage.CoinItem;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BundleItem.class)
abstract class CoinBundleMixin {
    @Inject(method = "overrideStackedOnOther", at = @At("HEAD"), cancellable = true)
    private void coinage$keepCoinsOutOfSlotBundle(ItemStack bundle, Slot slot, ClickAction action, Player player,
                                                 CallbackInfoReturnable<Boolean> callback) {
        if (slot.getItem().getItem() instanceof CoinItem) callback.setReturnValue(false);
    }

    @Inject(method = "overrideOtherStackedOnMe", at = @At("HEAD"), cancellable = true)
    private void coinage$keepCoinsOutOfCarriedBundle(ItemStack bundle, ItemStack other, Slot slot, ClickAction action,
                                                    Player player, SlotAccess carried, CallbackInfoReturnable<Boolean> callback) {
        if (other.getItem() instanceof CoinItem) callback.setReturnValue(false);
    }
}
