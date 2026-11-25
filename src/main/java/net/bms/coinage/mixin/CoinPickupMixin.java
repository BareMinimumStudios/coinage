package net.bms.coinage.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.bms.coinage.PurseTransactions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemEntity.class)
abstract class CoinPickupMixin {
    @WrapOperation(method = "playerTouch",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;add(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean coinage$collectCoin(Inventory inventory, ItemStack stack, Operation<Boolean> original, Player player) {
        return (player instanceof ServerPlayer serverPlayer && PurseTransactions.collectCoin(serverPlayer, stack))
            || original.call(inventory, stack);
    }
}
