package com.wandzz.wand;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.wandzz.core.CoreType;

import java.util.ArrayList;
import java.util.List;

/**
 * Dane pojedynczej rozdzki: material (drewno) + lista zainstalowanych core'ow.
 * Przechowywane jako custom data component na ItemStacku (Minecraft 1.20.5+).
 */
public record WandData(WandMaterial material, List<CoreType> cores) {

    public static final Codec<WandData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.xmap(WandMaterial::valueOf, WandMaterial::name)
                    .fieldOf("material").forGetter(WandData::material),
            Codec.STRING.xmap(CoreType::valueOf, CoreType::name)
                    .listOf().fieldOf("cores").forGetter(WandData::cores)
    ).apply(instance, WandData::new));

    public static WandData empty(WandMaterial material) {
        return new WandData(material, List.of());
    }

    public int freeSlots() {
        return material.totalCoreSlots() - cores.size();
    }

    /** Zwraca nowe WandData z dodanym core'em, jesli jest wolny slot. */
    public WandData withCoreAdded(CoreType core) {
        if (freeSlots() <= 0) return this;
        List<CoreType> updated = new ArrayList<>(cores);
        updated.add(core);
        return new WandData(material, List.copyOf(updated));
    }

    /**
     * Usuniecie PIERWSZEGO wystapienia core'a. Zwraca `this`, jesli takiego nie ma,
     * dzieki temu wywolujacy rozroznia "nic do usuniecia" po tozsamosci obiektu.
     */
    public WandData withCoreRemoved(CoreType core) {
        if (!cores.contains(core)) return this;
        List<CoreType> updated = new ArrayList<>(cores);
        updated.remove(core);
        return new WandData(material, List.copyOf(updated));
    }

    public boolean hasCore(CoreType core) {
        return cores.contains(core);
    }

    /** Najwyzszy poziom sposrod zainstalowanych core'ow (0 jesli brak core'ow). */
    public int highestLevel() {
        return cores.stream().mapToInt(CoreType::level).max().orElse(0);
    }

    public boolean hasCoreOfLevel(int level) {
        return cores.stream().anyMatch(c -> c.level() >= level);
    }
}
