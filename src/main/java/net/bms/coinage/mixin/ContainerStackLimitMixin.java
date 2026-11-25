package net.bms.coinage.mixin;

import net.minecraft.world.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(value = Container.class, priority = 500)
public interface ContainerStackLimitMixin {
    @ModifyConstant(method = "getMaxStackSize()I", constant = @Constant(intValue = 99), require = 0)
    private int coinage$allowHundredCoinStacks(int original) {
        return Math.max(original, 100);
    }
}
