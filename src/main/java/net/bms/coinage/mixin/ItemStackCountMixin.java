package net.bms.coinage.mixin;

import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(value = ItemStack.class, priority = 500)
public abstract class ItemStackCountMixin {
    @ModifyConstant(method = "*", constant = @Constant(intValue = 99), require = 0)
    private static int coinage$allowHundredCoinStacks(int original) {
        return 100;
    }
}
