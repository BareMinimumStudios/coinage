package net.bms.coinage.mixin;

import net.bms.coinage.CoinItem;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.item.ItemPropertyFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemProperties.class)
public abstract class CoinModelPropertyMixin {
    private static final ResourceLocation COINAGE_COUNT = ResourceLocation.fromNamespaceAndPath("coinage", "count");
    private static final ItemPropertyFunction COINAGE_STACK_SIZE = (stack, level, entity, seed) -> stack.getCount() / 100.0f;

    @Inject(method = "getProperty", at = @At("HEAD"), cancellable = true)
    private static void coinage$stackModel(ItemStack stack, ResourceLocation property,
            CallbackInfoReturnable<ItemPropertyFunction> callback) {
        if (stack.getItem() instanceof CoinItem && COINAGE_COUNT.equals(property)) {
            callback.setReturnValue(COINAGE_STACK_SIZE);
        }
    }
}
