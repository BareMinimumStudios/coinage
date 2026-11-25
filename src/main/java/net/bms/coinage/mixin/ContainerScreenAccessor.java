package net.bms.coinage.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface ContainerScreenAccessor {
    @Accessor("leftPos") int coinage$getLeft();
    @Accessor("topPos") int coinage$getTop();
    @Accessor("imageWidth") int coinage$getWidth();
}
