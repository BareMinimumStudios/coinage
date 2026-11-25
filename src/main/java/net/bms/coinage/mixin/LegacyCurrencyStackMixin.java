package net.bms.coinage.mixin;

import com.mojang.serialization.Codec;
import net.bms.coinage.LegacyCurrencyStacks;
import net.minecraft.world.item.ItemStack;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStack.class)
abstract class LegacyCurrencyStackMixin {
    @Shadow @Final @Mutable public static Codec<ItemStack> CODEC;

    @Inject(method = "<clinit>", at = @At(value = "FIELD",
        target = "Lnet/minecraft/world/item/ItemStack;CODEC:Lcom/mojang/serialization/Codec;",
        opcode = Opcodes.PUTSTATIC, shift = At.Shift.AFTER), require = 1)
    private static void coinage$readLegacyCurrency(CallbackInfo callback) {
        CODEC = LegacyCurrencyStacks.wrap(CODEC);
    }
}
