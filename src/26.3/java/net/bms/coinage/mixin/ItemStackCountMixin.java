package net.bms.coinage.mixin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(value = {ItemStack.class, ItemStackTemplate.class}, priority = 500)
public abstract class ItemStackCountMixin {
    @ModifyConstant(method = "*", constant = @Constant(intValue = 99), require = 0, remap = false)
    private static int coinage$allowHundredCoinStacks(int original) {
        return 100;
    }
}
