package net.bms.coinage;

import com.mojang.serialization.Codec;
import java.util.function.Supplier;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class SavedDataTypes {
    private SavedDataTypes() {}

    // Minecraft accepts null for mod data fixers despite the package nullability annotation.
    public static <T extends SavedData> SavedDataType<T> create(
            Identifier id, Supplier<T> constructor, Codec<T> codec) {
        return new SavedDataType<>(id, constructor, codec, null);
    }
}
