package com.wandzz.wand;

/**
 * Rodzaj drewna rozdzki. Kazde ma swoja liczbe DODATKOWYCH slotow na core'y
 * (poza jednym slotem bazowym, ktory posiada kazda rozdzka).
 *
 * Zgodnie z dokumentem projektowym:
 *   Normalne drewno                -> 0 dodatkowych core'ow
 *   Normalne drewno magiczne       -> 0 dodatkowych core'ow
 *   Customowe drewno               -> 1 dodatkowy core
 *   Magiczna wersja customowego    -> 2 dodatkowe core'y
 *   Rzadkie/lepsze drewno          -> 3 dodatkowe core'y
 *   Magiczna wersja rzadkiego      -> 5 dodatkowych core'ow
 *
 * Dzieki temu wybor rozdzki wplywa na budowanie WLASNEGO ZESTAWU
 * MOZLIWOSCI, a nie tylko na rosnacy damage.
 */
public enum WandMaterial {

    NORMAL(0, "wand_normal"),
    NORMAL_MAGIC(0, "wand_normal_magic"),
    CUSTOM(1, "wand_custom"),
    CUSTOM_MAGIC(2, "wand_custom_magic"),
    RARE(3, "wand_rare"),
    RARE_MAGIC(5, "wand_rare_magic");

    private final int extraCoreSlots;
    private final String translationKey;

    WandMaterial(int extraCoreSlots, String translationKey) {
        this.extraCoreSlots = extraCoreSlots;
        this.translationKey = translationKey;
    }

    /** Calkowita liczba slotow na core'y = 1 slot bazowy + dodatkowe z drewna. */
    public int totalCoreSlots() {
        return 1 + extraCoreSlots;
    }

    public String translationKey() {
        return translationKey;
    }
}
