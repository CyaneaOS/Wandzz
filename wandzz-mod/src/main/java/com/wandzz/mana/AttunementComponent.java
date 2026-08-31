package com.wandzz.mana;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * "Zgranie" (ang. attunement): ile z rzedu rzucilismy TEN SAM zaklecie.
 *
 * To jest jedyna progresja w modzie i jest celowo ulozona tak, zeby gracze
 * robili to, co mod lubi najbardziej: zamiast skakac po calej ksiezyc, trzymaja
 * jedno zaklecie i ucza sie jego gestu. Dwie nagrody, obie odczuwalne:
 *
 * <ol>
 *   <li><b>nizszy koszt</b> - koszt many spada 1.00 -&gt; 0.85 -&gt; 0.70 -&gt; 0.55
 *       (progi co 3 rzuty, patrz {@link #tier}),</li>
 *   <li><b>wybaczanie</b> - program rozpoznawania gestu na kliencie jest
 *       obnizany o {@code 0.02} za poziom ({@code 0.72} -&gt; {@code 0.66}),
 *       czyli "reka sie przyzwyczaja" - to jest ta czesc, ktora realnie
 *       zmienia gre, bo nierozpoznany gest byl najczestsza frustracja.</li>
 * </ol>
 *
 * Zasada jest jedna i brutalna: zmiana zaklecia kasuje poziom. Nie ma
 * rozpedu "na wszystko", jest rozped na jedno. Dlatego {@link #tier(String)}
 * zwraca 0 dla kazdego innego id, a {@link #afterCast(String)} zaczyna od
 * nowu - bez degradacji w czasie, bo to nie jest buff, tylko licznik.
 *
 * Przechowywane jako Fabric Data Attachment na graczu (patrz
 * {@link ManaAttachments}) - ten sam mechanizm co mana, ten sam
 * {@code persistent}, wiec przezywa relog.
 */
public record AttunementComponent(String spellId, int streak) {

    public static final Codec<AttunementComponent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("spell", "").forGetter(AttunementComponent::spellId),
            Codec.INT.optionalFieldOf("streak", 0).forGetter(AttunementComponent::streak)
    ).apply(instance, AttunementComponent::new));

    /** Poziomy: 0..3, kazdy po {@link #PER_LEVEL} rzutach pod rzad. */
    public static final int MAX_TIER = 3;
    public static final int PER_LEVEL = 3;

    /** Mnoznik kosztu many dla poziomu. */
    private static final double[] COST = {1.00, 0.85, 0.70, 0.55};
    /** O ile obnizamy prog rozpoznawania gestu za poziom. */
    private static final double TOLERANCE_PER_LEVEL = 0.02;

    public static AttunementComponent none() {
        return new AttunementComponent("", 0);
    }

    /** Poziom zgrania wybranym przez {@code spell} - inny czar = 0. */
    public int tier(final String spell) {
        return spell.equals(spellId) ? Math.min(MAX_TIER, streak / PER_LEVEL) : 0;
    }

    public int tier() {
        return Math.min(MAX_TIER, streak / PER_LEVEL);
    }

    /** Mnoznik kosztu many; poziom out-of-range jest obcinany, nie wyjatkiem. */
    public static double costMultiplier(final int tier) {
        return COST[clamp(tier)];
    }

    /** O ile nizej niz {@code DollarOneRecognizer.MIN_SCORE} schodzi prog. */
    public static double tolerance(final int tier) {
        return TOLERANCE_PER_LEVEL * clamp(tier);
    }

    /** Ten sam czar = licznik do gory; inny = zaczynamy od nowa (swiadomie). */
    public AttunementComponent afterCast(final String spell) {
        return spell.equals(spellId)
                ? new AttunementComponent(spell, streak + 1)
                : new AttunementComponent(spell, 1);
    }

    /** Rzymski numer do UI (tylko trzy poziomy, wiec bez generalnego solvera). */
    public static String roman(final int tier) {
        return switch (clamp(tier)) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            default -> "0";
        };
    }

    private static int clamp(final int tier) {
        return Math.max(0, Math.min(MAX_TIER, tier));
    }
}
