package com.wandzz.item;

import com.wandzz.Wandzz;
import com.wandzz.wand.WandData;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import java.util.function.UnaryOperator;

/**
 * Custom data component (Item Components, MC 1.20.5+) przechowujace dane
 * rozdzki: material + zainstalowane core'y.
 */
public final class ModComponents {

    public static final DataComponentType<WandData> WAND_DATA = register(
            "wand_data",
            builder -> builder.persistent(WandData.CODEC)
    );

    private static <T> DataComponentType<T> register(String path,
            UnaryOperator<DataComponentType.Builder<T>> builderOp) {
        return Registry.register(
                BuiltInRegistries.DATA_COMPONENT_TYPE,
                Identifier.fromNamespaceAndPath(Wandzz.MOD_ID, path),
                builderOp.apply(DataComponentType.builder()).build()
        );
    }

    public static void bootstrap() {
        // klasa jest ladowana - statyczne pola powyzej rejestruja komponenty
    }

    private ModComponents() {
    }
}
