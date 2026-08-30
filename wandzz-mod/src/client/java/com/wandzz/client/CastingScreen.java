package com.wandzz.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.wandzz.gesture.CastingData;
import com.wandzz.gesture.Point;
import com.wandzz.network.CastPayload;
import com.wandzz.spell.Spell;
import com.wandzz.spell.SpellRegistry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
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
 */
public class CastingScreen extends Screen {

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
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            finishCasting();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
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
        Optional<Spell> recognized = SpellRegistry.recognize(points);
        recognized.ifPresent(spell ->
                ClientPlayNetworking.send(new CastPayload(spell.id())));
        Minecraft client = Minecraft.getInstance();
        client.setScreen(null);
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

        RenderSystem.enableBlend();
        for (int i = 1; i < pts.size(); i++) {
            Point a = pts.get(i - 1);
            Point b = pts.get(i);
            drawLine(context, a, b, 0xFFAA33FF);
        }
        RenderSystem.disableBlend();
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
