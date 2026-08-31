package com.wandzz.client;

import com.wandzz.Wandzz;
import com.wandzz.mana.AttunementComponent;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;

/**
 * HUD many - pasek, ktory NIE ma stalej pozycji. W rundzie 4 bylo na sztywno
 * {@code x = szerokosc/2 + 100} i to byl blad: prawa krawedz hotbara to
 * {@code /2 + 91}, a jak gracz cos trzyma w drugiej rece, vanilla dokleja tam
 * slot offhandu (+29) - wiec pasek wchodzil na ekwipunek. Przy duzym GUI scale
 * (waske okno, ~320 px szerokosci po skalowaniu) etykieta wychodzila za ekran.
 *
 * Teraz geometria jest liczona ze stalych, ktorych uziva sama vanilla w
 * {@code Gui#renderItemHotbar} i {@code Gui#renderPlayerHealth}:
 *
 *   hotbar:       x = s/2 -+ 91, y = h - 22, wysokosc 22
 *   offhand:      +29 po tej stronie, po ktorej gracz ma druga reke
 *   serca/jedzenie: dolny rzed na y = h - 39, wyzsze o `max(10-(rzedy-2), 3)`
 *
 * Regula wyboru ukladu:
 *   1. jest miejsce po ktorejs stronie hotbara -> pionowy pasek przy tej stronie
 *      (preferowana prawa, tak jak w projekcie);
 *   2. nie ma (waske lub wysokie okno) -> poziomy pasek NAD rzadami serc, na
 *      szerokosc hotbara.
 * Do tego wysokosc slupka skaluje sie z ekraniem (20..60 px), grubosc z
 * szerokoscia (6 lub 8 px), a podzialka co 1/10 zmienia sie w co 1/5, gdy pasek
 * jest krotki - inaczej kreski zlylyby sie w jeden pas.
 *
 * Nie rysujemy wcale, gdy: wcisniete F1 (hideGui), tryb widza, albo klient nie
 * dostal jeszcze zadnego stanu many.
 */
public final class WandzzHud {

    private static final int MAX_BAR_HEIGHT = 60;
    private static final int MIN_BAR_HEIGHT = 20;
    /** Minimalnie potrzebne wolne miejsce obok hotbara, zeby stalo sie pionowo. */
    private static final int MIN_SIDE_SPACE = 12;
    private static final int MIN_BAR_WIDTH = 60;

    // -- stale konstanty vanilli (Gui#renderItemHotbar, Gui#renderPlayerHealth) --
    private static final int HOTBAR_HALF = 91;
    private static final int HOTBAR_HEIGHT = 22;
    private static final int OFFHAND_WIDTH = 29;
    private static final int HEALTH_BASELINE = 39;

    private static final int COLOR_FRAME = 0xF0120818;
    private static final int COLOR_EMPTY = 0xC00A0A14;
    private static final int COLOR_FULL = 0xFF59E0FF;
    private static final int COLOR_LOW = 0xFF2F6FB8;
    private static final int COLOR_REGEN_LINE = 0xFFFFFFFF;
    private static final int COLOR_TEXT = 0xFFE6CCFF;

    private WandzzHud() {
    }

    public static void register() {
        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(Wandzz.MOD_ID, "mana_bar"),
                WandzzHud::render);
    }

    private static void render(final GuiGraphics graphics, final DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (player == null || client.options.hideGui || player.isSpectator() || !ManaClientState.hasState()) {
            return;
        }

        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        if (width < MIN_BAR_WIDTH + 8 || height < MIN_BAR_HEIGHT + HOTBAR_HEIGHT + 16) {
            return;
        }

        double shown = ManaClientState.advance();
        double max = ManaClientState.max();
        double fraction = max <= 0.0 ? 0.0 : clamp01(shown / max);
        boolean regenerating = ManaClientState.isRegenerating();

        int center = width / 2;
        boolean hasOffhand = !player.getOffhandItem().isEmpty();
        HumanoidArm offhandArm = player.getMainArm().getOpposite();
        int rightEdge = center + HOTBAR_HALF + (hasOffhand && offhandArm == HumanoidArm.RIGHT ? OFFHAND_WIDTH : 0) + 2;
        int leftEdge = center - HOTBAR_HALF - (hasOffhand && offhandArm == HumanoidArm.LEFT ? OFFHAND_WIDTH : 0) - 2;
        int freeRight = width - rightEdge;
        int freeLeft = leftEdge;

        int thickness = width >= 470 ? 8 : 6;
        Component label = Component.translatable("wandzz.mana.bar",
                (int) Math.round(shown), (int) Math.round(max));
        // Zgranie doklejam do TEJ samej etykiety: oba warianty ukladu (slupek przy
        // hotbarze i pasek nad sercami) rysuja jedna etykieta, wiec nie ma drugiej
        // pozycji do pilnowania, a labelWidth jest liczony juz po doklejeniu.
        int attuneTier = ManaClientState.attuneTier();
        if (attuneTier > 0) {
            label = label.append(Component.translatable("wandzz.attune.hud",
                    AttunementComponent.roman(attuneTier)));
        }
        int labelWidth = client.font.width(label.getVisualOrderText());

        if (Math.max(freeRight, freeLeft) >= thickness + MIN_SIDE_SPACE) {
            renderVertical(graphics, client, label, labelWidth, fraction, regenerating,
                    width, height, center, rightEdge, leftEdge, freeRight >= freeLeft, thickness);
        } else {
            renderHorizontal(graphics, client, label, labelWidth, fraction, regenerating,
                    width, height, center, player, thickness);
        }
    }

    /** Wariant "po prawej/lewej od hotbara" - ten zamierzony w projekcie. */
    private static void renderVertical(final GuiGraphics graphics, final Minecraft client, final Component label,
            final int labelWidth, final double fraction, final boolean regenerating,
            final int width, final int height, final int center, final int rightEdge, final int leftEdge,
            final boolean preferRight, final int thickness) {

        int bottom = height - HOTBAR_HEIGHT - 2;
        int barHeight = Mth.clamp(bottom - 14, MIN_BAR_HEIGHT, MAX_BAR_HEIGHT);
        int top = bottom - barHeight;
        int x = preferRight
                ? Mth.clamp(rightEdge + 3, 2, width - thickness - 2)
                : Mth.clamp(leftEdge - 3 - thickness, 2, width - thickness - 2);

        drawBar(graphics, x, top, thickness, barHeight, true, fraction, regenerating);

        // Etykieta trafia nad slupek; jesli nie ma na nia miejsca (albo wychodzilaby
        // za ekran) - rezygnujemy, bo sam pasek jest czytelny.
        int labelY = top - 11;
        if (labelY >= 2 && labelWidth + 4 <= width) {
            int labelX = Mth.clamp(x + thickness / 2 - labelWidth / 2, 2, width - labelWidth - 2);
            graphics.drawString(client.font, label, labelX, labelY, COLOR_TEXT);
        }
    }

    /** Wariant awaryjny: waske okno - poziomy pasek nad rzadami serc. */
    private static void renderHorizontal(final GuiGraphics graphics, final Minecraft client, final Component label,
            final int labelWidth, final double fraction, final boolean regenerating,
            final int width, final int height, final int center, final Player player, final int thickness) {

        int rows = Math.max(1, Mth.ceil((player.getMaxHealth() + player.getAbsorptionAmount()) / 20.0f));
        int rowPitch = Math.max(10 - (rows - 2), 3);
        int healthTop = height - HEALTH_BASELINE - rowPitch * (rows - 1);

        int barWidth = Mth.clamp(Math.min(HOTBAR_HALF * 2, width - 12), MIN_BAR_WIDTH, 182);
        int x = Mth.clamp(center - barWidth / 2, 2, width - barWidth - 2);
        int y = Math.max(2, healthTop - 12);

        drawBar(graphics, x, y, barWidth, thickness, false, fraction, regenerating);

        int labelY = y - 11;
        if (labelY >= 2 && labelWidth + 4 <= width) {
            int labelX = Mth.clamp(x + barWidth / 2 - labelWidth / 2, 2, width - labelWidth - 2);
            graphics.drawString(client.font, label, labelX, labelY, COLOR_TEXT);
        }
    }

    /**
     * Rysowanie slupka/paska: ramka, dno, wypelnienie "od dna/od lewej", kreska
     * regeneracji i podzialka. Kierunek to jeden parametr, dzieki czemu oba
     * warianty UI nie duplikuja logiki wypelnienia.
     */
    private static void drawBar(final GuiGraphics graphics, final int x, final int y, final int width,
            final int height, final boolean vertical, final double fraction, final boolean regenerating) {

        graphics.fill(x - 2, y - 2, x + width + 2, y + height + 2, COLOR_FRAME);
        graphics.fill(x, y, x + width, y + height, COLOR_EMPTY);

        int color = fraction < 0.25 ? COLOR_LOW : COLOR_FULL;
        int filled = (int) Math.round((vertical ? height : width) * fraction);

        if (filled > 0) {
            if (vertical) {
                graphics.fill(x, y + height - filled, x + width, y + height, color);
                if (regenerating) {
                    graphics.fill(x, y + height - filled - 1, x + width, y + height - filled, COLOR_REGEN_LINE);
                }
            } else {
                graphics.fill(x, y, x + filled, y + height, color);
                if (regenerating) {
                    graphics.fill(x + filled, y, x + filled + 1, y + height, COLOR_REGEN_LINE);
                }
            }
        }

        // Podzialka: co 1/10, ale dla krotkiego paska co 1/5 - inaczej kreski
        // zlylyby sie w jeden pas przy wysokosci ~20 px.
        int steps = (vertical ? height : width) >= 40 ? 10 : 5;
        int span = vertical ? height : width;
        for (int mark = 1; mark < steps; mark++) {
            int at = span * mark / steps;
            if (vertical) {
                graphics.fill(x, y + at, x + (mark == steps / 2 ? width : 2), y + at + 1, 0x60000000);
            } else {
                graphics.fill(x + at, y, x + at + 1, y + (mark == steps / 2 ? height : 2), 0x60000000);
            }
        }
    }

    private static double clamp01(double value) {
        return value < 0.0 ? 0.0 : (value > 1.0 ? 1.0 : value);
    }
}
