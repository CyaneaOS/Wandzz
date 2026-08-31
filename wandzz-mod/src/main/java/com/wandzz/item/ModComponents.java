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
 * rozdzki: zainstalowane rdzenie (gatunek drewna idzie z Itemem).
 */
public final class ModComponents {

    /**
     * Tooltip rozdzki i okno stolika czytaja sklada PO STRONIE KLIENTA, wiec
     * komponent musi miec kodek sieciowy. {@code DataComponentType.Builder#build()}
     * wprawdzie wyrabia go sam z {@code CODEC} (przez
     * {@code ByteBufCodecs.fromCodecWithRegistries}), ale to kodek "leniwy":
     * wymaga {@code RegistryFriendlyByteBuf} i koduje przez JSON w kazda strone.
     * Wlasny {@code STREAM_CODEC} jest jawny, tanszy (VarInt + UTF) i dziala na
     * zwyklym {@code FriendlyByteBuf} - stad jest tu wpisany wprost.
     */
    public static final DataComponentType<WandData> WAND_DATA = register(
            "wand_data",
            builder -> builder
                    .persistent(WandData.CODEC)
                    .networkSynchronized(WandData.STREAM_CODEC)
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
