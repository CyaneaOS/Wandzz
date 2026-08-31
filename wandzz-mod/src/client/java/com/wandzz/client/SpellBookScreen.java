package com.wandzz.client;

import com.wandzz.core.CoreType;
import com.wandzz.gesture.Point;
import com.wandzz.spell.Spell;
import com.wandzz.spell.SpellRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Ksiega zaklec - podrecznik. Trzy rzeczy na wpis: nazwa, "ile to kosztuje i
 * jaki rdzen wymaga", oraz RYSUNEK gestu (ten sam, ktory gracz ma odwzorowac
 * myszka w CastingScreen).
 *
 * Lista jest budowana z {@link SpellRegistry#all()} w momencie otwarcia, wiec
 * nie ma tu zadnej recopisanej tabeli - nowe zaklecie pojawia sie w ksiedze
 * samo, tylko przez `SpellRegistry.register(...)`.
 *
 * Gest NIE jest brany z gotowego pliku PNG, tylko jest rysowany z tych samych
 * punktow, z ktorych korzysta recognizer (SpellRegistry#gestureOf). Diagram nie
 * moze sie wiec rozjechac z realnym wzorcem - jedno zrodlo danych.
 *
 * Ekran nie jest menu kontenera (patrz javadok WandCoreScreen - MenuType w
 * 1.21.11 jest prywatny), jest zwyklym Screenem otwieranym pakietem S2C.
 */
public class SpellBookScreen extends Screen {

    private static final int PAGE_WIDTH = 236;
    private static final int PAGE_HEIGHT = 184;
    private static final int PAD = 12;
    /** 3 wpisy x 4 linie (nazwa, opis, mana+rdzen, lista rdzeni) = 184 px panelu. */
    private static final int PER_PAGE = 3;
    private static final int ENTRY_PITCH = 44;
    private static final int FIRST_ENTRY_Y = 32;
    /** Bok kratki z gestem. */
    private static final int GLYPH_BOX = 34;

    private static final int PANEL_FILL = 0xF5160F24;
    private static final int PANEL_BORDER = 0xFF7A4FB8;
    private static final int TEXT_MAIN = 0xFFF0E4FF;
    private static final int TEXT_DIM = 0xFFA89BC0;
    private static final int TEXT_NAME = 0xFFFFF0B0;
    private static final int GLYPH_BG = 0xE00A0A14;
    private static final int GLYPH_LINE = 0xFF59E0FF;
    private static final int GLYPH_START = 0xFFFFF0B0;

    private int page;
    private List<Spell> spells = List.of();

    public SpellBookScreen() {
        super(Component.translatable("wandzz.book.title"));
    }

    @Override
    protected void init() {
        super.init();
        this.spells = List.copyOf(SpellRegistry.all());
        this.page = 0;

        int y = top() + PAGE_HEIGHT - 22;
        addRenderableWidget(Button.builder(Component.translatable("wandzz.book.prev"),
                button -> turn(-1)).bounds(left() + PAD, y, 60, 16).build());
        addRenderableWidget(Button.builder(Component.translatable("wandzz.book.next"),
                button -> turn(1)).bounds(left() + PAGE_WIDTH - PAD - 60, y, 60, 16).build());
    }

    private void turn(int delta) {
        this.page = clampPage(this.page + delta);
    }

    private int pages() {
        return Math.max(1, (spells.size() + PER_PAGE - 1) / PER_PAGE);
    }

    private int clampPage(int wanted) {
        return Math.min(Math.max(0, wanted), pages() - 1);
    }

    private int left() {
        return (this.width - PAGE_WIDTH) / 2;
    }

    private int top() {
        // Na niskich ekranach (GUI scale 3-4 w malym oknie) panel moze byc wyzszy
        // niz okno - wtedy przyklejamy go do gory, zamiast centrujac i ucinac.
        int free = this.height - 8;
        if (free <= PAGE_HEIGHT) {
            return 4;
        }
        return (free - PAGE_HEIGHT) / 2 + 4;
    }

    @Override
    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float tickDelta) {
        // Rejestr moze byc przebudowany (dev serwer), wiec rzucamy okiem co klatke.
        if (this.spells.isEmpty()) {
            this.spells = List.copyOf(SpellRegistry.all());
        }
        this.page = clampPage(this.page);

        // Ramka + tlo panelu (ten sam schemat co w oknie stolika).
        graphics.fill(left() - 1, top() - 1, left() + PAGE_WIDTH + 1, top() + PAGE_HEIGHT + 1, PANEL_BORDER);
        graphics.fill(left(), top(), left() + PAGE_WIDTH, top() + PAGE_HEIGHT, PANEL_FILL);

        graphics.drawCenteredString(this.font, this.title, left() + PAGE_WIDTH / 2, top() + 8, TEXT_NAME);
        graphics.drawCenteredString(this.font,
                Component.translatable("wandzz.book.page", this.page + 1, pages()),
                left() + PAGE_WIDTH / 2, top() + 18, TEXT_DIM);

        int first = this.page * PER_PAGE;
        for (int row = 0; row < PER_PAGE; row++) {
            int index = first + row;
            if (index >= this.spells.size()) {
                break;
            }
            renderEntry(graphics, this.spells.get(index), top() + FIRST_ENTRY_Y + row * ENTRY_PITCH);
        }

        super.render(graphics, mouseX, mouseY, tickDelta);
    }

    private void renderEntry(final GuiGraphics graphics, final Spell spell, final int y) {
        int x = left() + PAD;
        int textWidth = PAGE_WIDTH - 2 * PAD - GLYPH_BOX - 8;

        graphics.drawString(this.font, spellName(spell), x, y, TEXT_MAIN);
        graphics.drawString(this.font, Component.translatable(spellKey(spell) + ".desc"), x, y + 11, TEXT_DIM);

        Component meta = Component.translatable("wandzz.book.meta",
                (int) Math.ceil(spell.manaCost()), spell.requiredLevel());
        graphics.drawString(this.font, meta, x, y + 22, TEXT_DIM);

        graphics.drawString(this.font, fit(coresOf(spell), textWidth), x, y + 33, TEXT_DIM);

        drawGesture(graphics, SpellRegistry.gestureOf(spell.id()),
                left() + PAGE_WIDTH - PAD - GLYPH_BOX, y - 2);
    }

    /** Nazwa zaklecia = klucz `wandzz.spell.<sciezka>` (te same co w lang). */
    private Component spellName(final Spell spell) {
        return Component.translatable(spellKey(spell));
    }

    private static String spellKey(final Spell spell) {
        return "wandzz.spell." + spell.id().substring(spell.id().indexOf(':') + 1);
    }

    /** Wyliczenie, ktore rdzenie to zaklecie udostepniaja - te sama logika co na serwerze. */
    private String coresOf(final Spell spell) {
        List<String> names = new ArrayList<>();
        for (CoreType core : CoreType.values()) {
            if (spell.isProvidedBy(core)) {
                names.add(Component.translatable("item.wandzz." + core.translationKey()).getString());
            }
        }
        if (names.isEmpty()) {
            return Component.translatable("wandzz.book.no_cores").getString();
        }
        return Component.translatable("wandzz.book.cores", String.join(", ", names)).getString();
    }

    private static String fit(final String text, final int maxWidth) {
        Minecraft client = Minecraft.getInstance();
        if (client.font.width(text) <= maxWidth) {
            return text;
        }
        int cut = text.length();
        while (cut > 1 && client.font.width(text.substring(0, cut) + "...") > maxWidth) {
            cut--;
        }
        return text.substring(0, cut) + "...";
    }

    // ------------------------------------------------------------------
    // Diagram gestu
    // ------------------------------------------------------------------

    private void drawGesture(final GuiGraphics graphics, final @Nullable List<Point> points,
            final int boxX, final int boxY) {

        graphics.fill(boxX - 1, boxY - 1, boxX + GLYPH_BOX + 1, boxY + GLYPH_BOX + 1, PANEL_BORDER);
        graphics.fill(boxX, boxY, boxX + GLYPH_BOX, boxY + GLYPH_BOX, GLYPH_BG);
        if (points == null || points.size() < 2) {
            return;
        }

        // Mapowanie jak w recognizerze: skalowanie JEDNOLITE po dluzszym boku
        // bboxa, potem wysrodkowanie w kratce. Bez tego takze "V" byloby
        // rozciagane do pelni kratki, czyli diagram klamalby co grac ma
        // naprawde narysowac.
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (Point p : points) {
            minX = Math.min(minX, p.x());
            maxX = Math.max(maxX, p.x());
            minY = Math.min(minY, p.y());
            maxY = Math.max(maxY, p.y());
        }
        double span = Math.max(maxX - minX, maxY - minY);
        if (span < 1e-9) {
            return;
        }
        double scale = (GLYPH_BOX - 8) / span;
        double offsetX = boxX + 4 + ((GLYPH_BOX - 8) - (maxX - minX) * scale) / 2.0;
        double offsetY = boxY + 4 + ((GLYPH_BOX - 8) - (maxY - minY) * scale) / 2.0;

        int prevX = 0, prevY = 0;
        for (int i = 0; i < points.size(); i++) {
            Point p = points.get(i);
            int px = (int) Math.round(offsetX + (p.x() - minX) * scale);
            int py = (int) Math.round(offsetY + (p.y() - minY) * scale);
            if (i > 0) {
                drawLine(graphics, prevX, prevY, px, py, GLYPH_LINE);
            } else {
                graphics.fill(px - 1, py - 1, px + 1, py + 1, GLYPH_START);
            }
            prevX = px;
            prevY = py;
        }
    }

    /**
     * Kreska "po pikselach" (Bresenham). GuiGraphics nie ma w 1.21.11 zadnego
     * drawLine, a fill() 1x1 przy krate 30 px to maksymalnie kilkanascie
     * wywolan - tanie i dokladne.
     */
    // x0/y0 sa MUTOWANE w petli Bresenhama, wiec nie moga byc final (wlasnie to
    // wywalilo compileClientJava: "final parameter y0 may not be assigned").
    private static void drawLine(final GuiGraphics graphics, int x0, int y0, int x1, final int y1,
            final int color) {

        int dx = Math.abs(x1 - x0);
        int dy = -Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx + dy;

        while (true) {
            graphics.fill(x0, y0, x0 + 1, y0 + 1, color);
            if (x0 == x1 && y0 == y1) {
                break;
            }
            int e2 = 2 * err;
            if (e2 >= dy) {
                err += dy;
                x0 += sx;
            }
            if (e2 <= dx) {
                err += dx;
                y0 += sy;
            }
        }
    }

    /** Jak w stoliku: otwarta ksiega nie zatrzymuje swiata singleplayer. */
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
