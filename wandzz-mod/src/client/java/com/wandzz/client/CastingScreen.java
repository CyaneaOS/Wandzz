package com.wandzz.client;

import com.wandzz.Wandzz;
import com.wandzz.gesture.CastingData;
import com.wandzz.gesture.DollarOneRecognizer;
import com.wandzz.mana.AttunementComponent;
import com.wandzz.gesture.Point;
import com.wandzz.network.CastPayload;
import com.wandzz.spell.Spell;
import com.wandzz.spell.SpellRegistry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Optional;

/**
 * Ekran otwierany na czas trzymania PPM z rozdzka w rece. Zbiera pozycje
 * myszy do CastingData, rysuje slad na ekranie, a po puszczeniu PPM
 * uruchamia rozpoznawanie $1 i (jesli sukces) wysyla wynik do serwera.
 *
 * PPM wcisniety -> startCasting() -> zbieranie Point -> PPM puszczony
 * -> rozpoznawanie gestu -> CastPayload -> serwer
 *
 * Minecraft 1.21.11:
 *  - obsluga myszy przeszla na obiekty zdarzen: {@code mouseReleased(MouseButtonEvent)}
 *    zamiast {@code mouseReleased(double, double, int)},
 *  - {@code RenderSystem#enableBlend/disableBlend} zniknely wraz z rewrite'em
 *    renderowania (RenderTypes/GpuDevice), dlatego slad rysujemy bez blendu.
 */
public class CastingScreen extends Screen {

    private static final int TRAIL_COLOR = 0xFFAA33FF;

    private final CastingData castingData = new CastingData();

    public CastingScreen() {
        super(Component.literal("Wandzz Casting"));
    }

    @Override
    protected void init() {
        super.init();
        castingData.startCasting();
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        castingData.addPoint(mouseX, mouseY);
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            finishCasting();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public void onClose() {
        // ESC lub inne zamkniecie bez PPM = anulowanie, bez wysylania gestu
        castingData.stopCasting();
        super.onClose();
    }

    private void finishCasting() {
        List<Point> points = castingData.stopCasting();
        Minecraft client = Minecraft.getInstance();

        if (points.size() < 2) {
            // Puste przesuniecie kursora - nie wysylamy nic, ale mowimy dlaczego,
            // inaczej wyglada to jak "czar nie dziala".
            tell(client, "wandzz.gesture.too_short");
        } else {
            // Zgranie obniza prog: "reka przyzwyczaja sie do jednego ksztaltu".
            // Liczymy to tu, a nie w SpellRegistry.recognize(), bo ksiega zaklec
            // i podglad w oknie rysuja te same ksztalty i nie moga byc "latwiejsze".
            Optional<Spell> recognized = ManaClientState.attuneTier() > 0
                    ? relaxed(SpellRegistry.recognizer().bestMatch(points))
                    : SpellRegistry.recognize(points);
            if (recognized.isPresent()) {
                ClientPlayNetworking.send(new CastPayload(recognized.get().id()));
            } else {
                tell(client, "wandzz.gesture.unknown");
                SpellRegistry.recognizer().bestMatch(points).ifPresent(best ->
                        Wandzz.LOGGER.info("Wandzz: gest nierozpoznany, najblizej: {} ({})",
                                best.templateId(), String.format("%.0f%%", best.score() * 100)));
            }
        }
        client.setScreen(null);
    }

    /** To samo co SpellRegistry.recognize, tylko z progiem od stopnia zgrania. */
    private static Optional<Spell> relaxed(final Optional<DollarOneRecognizer.Result> best) {
        double threshold = DollarOneRecognizer.MIN_SCORE
                - AttunementComponent.tolerance(ManaClientState.attuneTier());
        return best.filter(result -> result.score() >= threshold)
                .flatMap(result -> SpellRegistry.get(result.templateId()));
    }

    private static void tell(Minecraft client, String key) {
        if (client.player != null) {
            client.player.displayClientMessage(Component.translatable(key), true);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        // przezroczyste tlo - nie przyciemniamy swiata gry podczas rysowania
        renderTrail(context);
        super.render(context, mouseX, mouseY, delta);
    }

    private void renderTrail(GuiGraphics context) {
        List<Point> pts = castingData.currentPoints();
        if (pts.size() < 2) return;

        for (int i = 1; i < pts.size(); i++) {
            drawLine(context, pts.get(i - 1), pts.get(i), TRAIL_COLOR);
        }
    }

    private void drawLine(GuiGraphics context, Point a, Point b, int color) {
        // prosta reprezentacja sladu - seria malych kwadratow miedzy punktami
        int steps = 4;
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            int x = (int) (a.x() + (b.x() - a.x()) * t);
            int y = (int) (a.y() + (b.y() - a.y()) * t);
            context.fill(x - 1, y - 1, x + 1, y + 1, color);
        }
    }
}
