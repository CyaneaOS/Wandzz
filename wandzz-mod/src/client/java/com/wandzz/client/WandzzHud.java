package com.wandzz.client;

import com.wandzz.Wandzz;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * HUD many: pionowy pasek po prawej stronie, tuz nad hotbarem (te same
 * "wspolrzedne" co pasek oddechu, tylko przesuniety w prawo o ~10 px, zeby nie
 * zachodzil na level doswiadczenia).
 *
 * Fabric 1.21.11: dawny {@code HudLayerRegistrationCallback} zostal zastapiony
 * przez rejestry elementow HUD-a - {@code HudElementRegistry.addLast(id,
 * (graphics, tickCounter) -> ...)}. {@code addLast} = rysowane jako ostatnie,
 * czyli nad vanilla paskami (zyczenie: pasek ma byc widoczny zawsze).
 */
public final class WandzzHud {

    private static final int BAR_WIDTH = 6;
    private static final int BAR_HEIGHT = 60;
    /** Odstep od gory hotbara (vanilla rysuje go na `h - 39`). */
    private static final int HOTBAR_GAP = 6;
    /** Prawa krawedz hotbara to `w/2 + 91`; level doswiadczenia stoi przy +91. */
    private static final int X_OFFSET_FROM_CENTER = 100;

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
        if (client.player == null || client.options.hideGui || !ManaClientState.hasState()) {
            return;
        }

        double shown = ManaClientState.advance();
        double max = ManaClientState.max();
        double fraction = max <= 0.0 ? 0.0 : clamp01(shown / max);

        int x = graphics.guiWidth() / 2 + X_OFFSET_FROM_CENTER;
        int y = graphics.guiHeight() - 39 - HOTBAR_GAP - BAR_HEIGHT;
        int filled = (int) Math.round(BAR_HEIGHT * fraction);

        // Ramka + dno
        graphics.fill(x - 2, y - 2, x + BAR_WIDTH + 2, y + BAR_HEIGHT + 2, COLOR_FRAME);
        graphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, COLOR_EMPTY);

        // Wypelnienie (od dolu, jak woda w kolbie)
        if (filled > 0) {
            int color = fraction < 0.25 ? COLOR_LOW : COLOR_FULL;
            graphics.fill(x, y + BAR_HEIGHT - filled, x + BAR_WIDTH, y + BAR_HEIGHT, color);
            if (ManaClientState.isRegenerating()) {
                // Jasna kreska na gornym skraju slupka = "mana wlasnie wraca".
                graphics.fill(x, y + BAR_HEIGHT - filled - 1, x + BAR_WIDTH, y + BAR_HEIGHT - filled,
                        COLOR_REGEN_LINE);
            }
        }

        // Kreski co 1/10 wysokosci - zeby moc ocenic wartosc "na oko" bez liczenia
        for (int mark = 1; mark < 10; mark++) {
            int line = y + BAR_HEIGHT * mark / 10;
            graphics.fill(x, line, x + (mark == 5 ? BAR_WIDTH : 2), line + 1, 0x60000000);
        }

        // Etykieta idzie przez translatable, wiec po polsku jest "Mana: 42/100",
        // a nie surowe "42/100" - ten sam sposob, ktorym vanilla tlumaczy
        // komunikaty w action barze.
        Component label = Component.translatable("wandzz.mana.bar",
                (int) Math.round(shown), (int) Math.round(max));
        graphics.drawCenteredString(client.font, label, x + BAR_WIDTH / 2, y - 11, COLOR_TEXT);
    }

    private static double clamp01(double value) {
        return value < 0.0 ? 0.0 : (value > 1.0 ? 1.0 : value);
    }
}
