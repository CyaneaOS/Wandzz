package com.wandzz.core;

/**
 * 15 typow core'ow. Kazdy okresla przede wszystkim POZIOM i mozliwosci,
 * a niekoniecznie jedno konkretne zaklecie - np. Dragon Breath jako efekt
 * moze byc dostepny takze dla innych core'ow poziomu 3 (patrz
 * Spell#isProvidedBy w konkretnych implementacjach zaklec).
 *
 * W pelni opisane w dokumencie projektowym: FEATHER (lvl 1) i DRAGON_BREATH
 * (lvl 3). Pozostale 13 to sloty na przyszla rozbudowe - maja przypisany
 * placeholder-owy poziom/opis do dopracowania w dalszych iteracjach moda.
 */
public enum CoreType {

    // ---- W pelni zaimplementowane w dokumencie ----
    FEATHER(1, "core_feather", 1.0),
    DRAGON_BREATH(3, "core_dragon_breath", 0.9),

    // ---- Sloty na dalsza rozbudowe (15 core'ow total) ----
    EARTH(1, "core_earth", 1.0),
    WATER(1, "core_water", 1.0),
    FLAME(2, "core_flame", 1.0),
    FROST(2, "core_frost", 1.0),
    STORM(2, "core_storm", 1.0),
    VOID(3, "core_void", 0.9),
    PHOENIX(3, "core_phoenix", 0.9),
    SHADOW(2, "core_shadow", 1.0),
    LIGHT(2, "core_light", 1.0),
    NATURE(1, "core_nature", 1.1),
    IRON(1, "core_iron", 1.0),
    ENDER(3, "core_ender", 0.85),
    CHRONOS(4, "core_chronos", 0.75);

    private final int level;
    private final String translationKey;
    /** Mnoznik regeneracji many (1.0 = domyslnie, np. Feather moze przyspieszac regeneracje). */
    private final double manaRegenMultiplier;

    CoreType(int level, String translationKey, double manaRegenMultiplier) {
        this.level = level;
        this.translationKey = translationKey;
        this.manaRegenMultiplier = manaRegenMultiplier;
    }

    public int level() {
        return level;
    }

    public String translationKey() {
        return translationKey;
    }

    public double manaRegenMultiplier() {
        return manaRegenMultiplier;
    }
}
