package com.wandzz.client;

import com.wandzz.mana.AttunementComponent;

/**
 * Klientowy lustrzany stan many - jedyny source prawdy dla HUD-a.
 *
 * Mana zyje w Fabric Data Attachment na Playerze PO STRONIE SERWERA i nie jest
 * synchronizowana, wiec klient dostaje ja pakietem {@code ManaSyncPayload}.
 * Serwer wysyla wartosc ~2x/sekunde, a `advance()` wygladza wskaznik co klatke
 * (lerp), dzieki czemu pasek plynie zamiast skakac.
 */
public final class ManaClientState {

    /** Ujemny = "serwer jeszcze nic nie powiedzial" (wtedy HUD sie nie rysuje). */
    private static double target = -1.0;
    private static double shown = -1.0;
    private static double max = 1.0;
    private static int attuneTier;
    private static String attuneSpell = "";

    private ManaClientState() {
    }

    public static void update(double current, double maxMana, int tier, String spell) {
        attuneTier = Math.max(0, Math.min(AttunementComponent.MAX_TIER, tier));
        attuneSpell = spell == null ? "" : spell;
        max = Math.max(1.0, maxMana);
        target = Math.max(0.0, Math.min(current, max));
        // Pierwszy pakiet (albo powrot z innego wymiaru): skocz od razu, zeby
        // nie animowac paska od zera przy wejsciu na swiat.
        if (shown < 0.0 || shown > max) {
            shown = target;
        }
    }

    /** Poziom zgrania - klient liczy z niego obnizony prog gestu. */
    public static int attuneTier() {
        return attuneTier;
    }

    /** Id zaklecia, na ktore jest rozped (puste = brak). */
    public static String attuneSpell() {
        return attuneSpell;
    }

    public static void reset() {
        attuneTier = 0;
        attuneSpell = "";
        target = -1.0;
        shown = -1.0;
        max = 1.0;
    }

    public static boolean hasState() {
        return target >= 0.0;
    }

    /** Wywolac raz na klatke (z renderu HUD-a). Zwraca wartosc do narysowania. */
    public static double advance() {
        if (shown < 0.0) return 0.0;
        double diff = target - shown;
        if (Math.abs(diff) < 0.05) {
            shown = target;
        } else {
            shown += diff * 0.18;
        }
        return shown;
    }

    public static double mana() {
        return Math.max(0.0, shown);
    }

    public static double max() {
        return max;
    }

    /** True, gdy wskaznik dogania wartosc serwera, tj. mana wlasnie wraca. */
    public static boolean isRegenerating() {
        return target > shown + 0.05;
    }
}
