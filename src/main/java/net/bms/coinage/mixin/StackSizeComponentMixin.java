package net.bms.coinage.mixin;

import net.minecraft.core.component.DataComponents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(value = DataComponents.class, priority = 500)
public abstract class StackSizeComponentMixin {
    // The codec is created in a compiler-generated callback whose name differs by mapping set.
    @ModifyConstant(method = "*", constant = @Constant(intValue = 99), require = 0)
    private static int coinage$allowHundredCoinStacks(int original) {
        return 100;
    }
}
