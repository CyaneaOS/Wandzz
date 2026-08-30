package com.wandzz.wand;

/**
 * Drewno, z ktorego jest rozdzka - jedna stala na gatunek.
 *
 * Dawniej byl to enum {@code WandMaterial} z szescioma "tierami"
 * (NORMAL/CUSTOM/RARE x zwykla/magiczna). Teraz tier jest WYPROWADZONY z
 * gatunku drewna, wiec kazde drewno (rowniez kazde vanilla) daje wlasny patyk
 * i wlasna rozdzke, a material nadal decyduje o liczbie gniazd na rdzenie:
 *
 *   totalSlots(magic) = 1 gniazdo bazowe + extraCoreSlots + (magic ? bonus : 0)
 *
 * Dzieki temu dawna drabina pozostaje w mocy (rozdzka z debu = 1 gniazdo,
 * arkana magiczna = 6 gniazd), a srodek drabiny wypelnia sie drewnami z gry.
 */
public enum WandWood {

    // ---- drewno pospolite: 1 gniazdo (1 po magicznym wzmocnieniu) ----
    OAK("oak", 0, 0),
    SPRUCE("spruce", 0, 0),
    BIRCH("birch", 0, 0),
    JUNGLE("jungle", 0, 0),
    ACACIA("acacia", 0, 0),
    BAMBOO("bamboo", 0, 0),

    // ---- drewno srednie: 2 gniazda (3 z magicznym) ----
    DARK_OAK("dark_oak", 1, 1),
    MANGROVE("mangrove", 1, 1),
    CHERRY("cherry", 1, 1),
    PALE_OAK("pale_oak", 1, 1),

    // ---- drewno z Netheru: 3 gniazda (4 z magicznym) ----
    CRIMSON("crimson", 2, 1),
    WARPED("warped", 2, 1),

    // ---- drewno arkanskie (wlasne drzewo, patrz com.wandzz.world) ----
    ARCANE("arcane", 3, 2);

    /** Sciezka gatunku, np. {@code dark_oak} - used to build all item ids. */
    private final String woodId;
    private final int extraCoreSlots;
    /** Dodatek za wzmocnienie glowstone'em (wersja `_magic`). */
    private final int magicBonusSlots;

    WandWood(String woodId, int extraCoreSlots, int magicBonusSlots) {
        this.woodId = woodId;
        this.extraCoreSlots = extraCoreSlots;
        this.magicBonusSlots = magicBonusSlots;
    }

    public String woodId() {
        return woodId;
    }

    /** Identyfikator patyka tego gatunku, np. {@code wandzz:arcane_stick}. */
    public String stickId() {
        return woodId + "_stick";
    }

    /** Identyfikator rozdzki, np. {@code cherry_wand} / {@code cherry_wand_magic}. */
    public String wandId(boolean magic) {
        return magic ? woodId + "_wand_magic" : woodId + "_wand";
    }

    /**
     * Deski uzywane w recepturze tej rozdzki. Arkanskie sa nasze, reszta to
     * vanilla ({@code minecraft:jungle_planks}, {@code minecraft:crimson_planks}...).
     */
    public String planksId() {
        return this == ARCANE ? "wandzz:arcane_planks" : "minecraft:" + woodId + "_planks";
    }

    /** Klucz tlumaczenia nazwy gatunku, uzywany w tooltipach rozdziek. */
    public String woodTranslationKey() {
        return "wandzz.wood." + woodId;
    }

    public int extraCoreSlots() {
        return extraCoreSlots;
    }

    public int magicBonusSlots() {
        return magicBonusSlots;
    }

    /** Calkowita liczba gniazd na rdzenie: 1 bazowe + dodatki z drewna. */
    public int totalSlots(boolean magic) {
        return 1 + extraCoreSlots + (magic ? magicBonusSlots : 0);
    }
}
