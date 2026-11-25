package net.bms.coinage.mixin;

import net.bms.coinage.YigdPurseCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.b1n_ry.yigd.components.InventoryComponent", remap = false)
abstract class YigdInventoryMixin {
    @Inject(method = "onDeath", at = @At("RETURN"), require = 0)
    private void coinage$capturePurse(@Coerce Object context, CallbackInfo callback) {
        YigdPurseCompat.capture(this, context);
    }
}
