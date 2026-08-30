package com.wandzz.item;

import com.wandzz.wand.WandData;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public final class ModComponents {

    public static final DataComponentType<WandData> WAND_DATA = register(
            "wand_data",
            builder -> builder.persistent(WandData.CODEC)
    );

    private static <T> DataComponentType<T> register(String path,
            java.util.function.UnaryOperator<DataComponentType.Builder<T>> builderOp) {
        return Registry.register(
                BuiltInRegistries.DATA_COMPONENT_TYPE,
                ResourceLocation.fromNamespaceAndPath("wandzz", path),
                builderOp.apply(DataComponentType.builder()).build()
        );
    }

    public static void bootstrap() {
        // klasa jest ladowana - statyczne pola powyzej rejestruja komponenty
    }

    private ModComponents() {
    }
}
