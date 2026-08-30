package com.wandzz.wand;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.wandzz.core.CoreType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

/**
 * Zainstalowane rdzenie rozdzki, przechowywane jako custom data component na
 * ItemStacku (Minecraft 1.20.5+).
 *
 * SWIADOMIE tylko lista core'ow: gatunek drewna i flaga "magiczna" NIE sa
 * duplikowane w komponencie - ida z {@link WandItem}, ktorego jest stack.
 * Dzieki temu nie da sie rozjechac skladu ("wand_normal" z 6 gniazdami) przez
 * zmiane komponentu, a stary format zapisu ({@code {"material":..,"cores":[..]}})
 * nadal sie deserializuje - {@code RecordCodecBuilder} ignoruje dodatkowe klucze.
 *
 * Dodatkowy {@link #STREAM_CODEC} jest niezbedny, bo tooltipy i ekran stolika
 * czytaja sklada PO STRONIE KLIENTA - bez `networkSynchronized` komponent zostalby
 * na synchronizacje wyciety i klient widzialby pusta rozdzke.
 */
public record WandData(List<CoreType> cores) {

    public static final WandData EMPTY = new WandData(List.of());

    public static final Codec<WandData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.xmap(CoreType::valueOf, CoreType::name)
                    .listOf().fieldOf("cores").forGetter(WandData::cores)
    ).apply(instance, WandData::new));

    public static final StreamCodec<FriendlyByteBuf, WandData> STREAM_CODEC = StreamCodec.of(
            (buf, data) -> {
                buf.writeVarInt(data.cores().size());
                for (CoreType core : data.cores()) {
                    buf.writeUtf(core.name());
                }
            },
            buf -> {
                int count = buf.readVarInt();
                List<CoreType> cores = new ArrayList<>(Math.max(0, Math.min(count, 16)));
                for (int i = 0; i < count; i++) {
                    CoreType core = byName(buf.readUtf());
                    if (core != null) {
                        cores.add(core);
                    }
                }
                return new WandData(List.copyOf(cores));
            }
    );

    /** Bezpieczny odczyt nazwy enuma - nieznana wartosc nie wywala watku sieciowego. */
    private static CoreType byName(String name) {
        for (CoreType core : CoreType.values()) {
            if (core.name().equals(name)) {
                return core;
            }
        }
        return null;
    }

    /** Dodanie rdzenia, jesli jest wolne gniazdo. Zwraca `this`, gdy brak miejsca. */
    public WandData withCoreAdded(CoreType core, int capacity) {
        if (cores.size() >= capacity) return this;
        List<CoreType> updated = new ArrayList<>(cores);
        updated.add(core);
        return new WandData(List.copyOf(updated));
    }

    /**
     * Usuniecie PIERWSZEGO wystapienia rdzenia. Zwraca `this`, jesli takiego nie ma,
     * dzieki temu wywolujacy rozroznia "nic do usuniecia" po tozsamosci obiektu.
     */
    public WandData withCoreRemoved(CoreType core) {
        if (!cores.contains(core)) return this;
        List<CoreType> updated = new ArrayList<>(cores);
        updated.remove(core);
        return new WandData(List.copyOf(updated));
    }

    public boolean hasCore(CoreType core) {
        return cores.contains(core);
    }

    /** Najwyzszy poziom sposrod zainstalowanych rdzeni (0 jesli brak). */
    public int highestLevel() {
        return cores.stream().mapToInt(CoreType::level).max().orElse(0);
    }

    public boolean hasCoreOfLevel(int level) {
        return cores.stream().anyMatch(c -> c.level() >= level);
    }
}
