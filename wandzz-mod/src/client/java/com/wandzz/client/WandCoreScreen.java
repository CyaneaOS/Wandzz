package com.wandzz.client;

import com.wandzz.Wandzz;
import com.wandzz.core.CoreType;
import com.wandzz.core.WandCoreItem;
import com.wandzz.item.ModItems;
import com.wandzz.network.WandLoadoutPayload;
import com.wandzz.wand.WandData;
import com.wandzz.wand.WandItem;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Okno stolika arcanicznego: sklad rozdzki trzymanej w rece.
 *
 * Dlaczego WLASNY Screen + pakiety, a nie vanilla `MenuType`/`AbstractContainerMenu`?
 * W 1.21.11 `MenuType` ma prywatny konstruktor, `MenuScreens.register` jest
 * prywatne, a interfejs `ScreenConstructor` tez - bez access widenera / mixins
 * nie da sie zarejestrowac ekranu kontenera. Kontener dalby drag&drop, ale za
 * cene zmiany buildu; tu zamiast tego:
 *   - rozdzka NIE opuszcza ekwipunku (jest w rece, wiec jej komponent jest i tak
 *     synchronizowany do klienta),
 *   - klik na ekwipunek = "chce ten rdzen", klik na gniazdo = "wyciagnij",
 *   - "Zatwierdz" wysyla CALY sklad (WandLoadoutPayload), a serwer liczy gniazda,
 *     zabiera/dodaje przedmioty i dopiero wtedy zapisuje component.
 *
 * UWAGA: `staged` to tylko zamysl gracza - ostateczna decyzja zawsze po stronie
 * serwera (patrz WandzzNetwork#applyLoadout).
 */
public class WandCoreScreen extends Screen {

    private static final int PANEL_WIDTH = 176;
    private static final int PANEL_HEIGHT = 190;

    private static final int SOCKET_X = 66;
    private static final int SOCKET_Y = 34;
    private static final int SOCKET_PITCH = 20;
    private static final int SOCKET_COLUMNS = 3;

    private static final int INV_X = 8;
    private static final int INV_Y = 100;
    private static final int INV_PITCH = 18;
    private static final int INV_COLUMNS = 9;
    private static final int INV_ROWS = 4;

    private static final int SLOT_BG = 0x90313131;
    private static final int SLOT_HOVER = 0xFFFFFFFF;
    private static final int PANEL_FILL = 0xF0150E22;
    private static final int PANEL_BORDER = 0xFF7A4FB8;
    private static final int TEXT_MAIN = 0xFFE6D6FF;
    private static final int TEXT_DIM = 0xFFA89BC0;

    private final List<CoreType> staged = new ArrayList<>();
    private int capacity;
    private ItemStack wand = ItemStack.EMPTY;

    public WandCoreScreen() {
        super(Component.translatable("wandzz.gui.table.title"));
    }

    @Override
    protected void init() {
        super.init();
        readWand();

        int buttonY = this.topPos() + PANEL_HEIGHT + 6;
        addRenderableWidget(Button.builder(Component.translatable("wandzz.gui.table.confirm"),
                button -> confirm()).bounds(leftPos() + 20, buttonY, 76, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("wandzz.gui.table.clear"),
                button -> staged.clear()).bounds(leftPos() + 102, buttonY, 54, 18).build());
    }

    /**
     * Serwer co jakis czas podsyla ten sam stack od nowa (synchronizacja
     * ekwipunku), wiec odswiezamy WSKAZNIK na stack, ale NIE ruszamy `staged`.
     * Sklad resetujemy tylko wtedy, gdy gracz wzial inna rozdzke (inny Item)
     * albo wyciagl ja - wowczas `staged` musi odpowiadac temu, co faktycznie ma.
     */
    private void refreshWand() {
        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        ItemStack held = player == null ? null : WandItem.findWand(player);
        if (held == null) {
            if (!wand.isEmpty()) {
                wand = ItemStack.EMPTY;
                capacity = 0;
                staged.clear();
            }
            return;
        }
        if (wand.isEmpty() || held.getItem() != wand.getItem()) {
            readWand();
        } else {
            wand = held;
        }
    }

    /** Aktualny stan rozdzki (reka glowna, potem druga) - klient czyta te same dane co serwer. */
    private void readWand() {
        staged.clear();
        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        ItemStack held = player == null ? null : WandItem.findWand(player);
        if (held == null) {
            wand = ItemStack.EMPTY;
            capacity = 0;
            return;
        }
        wand = held;
        capacity = WandItem.capacity(held);
        WandData data = WandItem.getData(held);
        staged.addAll(data.cores());
    }

    private void confirm() {
        List<String> ids = new ArrayList<>(staged.size());
        for (CoreType core : staged) {
            ids.add(core.name());
        }
        ClientPlayNetworking.send(new WandLoadoutPayload(ids));
        onClose();
    }

    // ------------------------------------------------------------------
    // Rysowanie
    // ------------------------------------------------------------------

    @Override
    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float tickDelta) {
        refreshWand();
        graphics.fill(leftPos() - 1, topPos() - 1, leftPos() + PANEL_WIDTH + 1, topPos() + PANEL_HEIGHT + 1,
                PANEL_BORDER);
        graphics.fill(leftPos(), topPos(), leftPos() + PANEL_WIDTH, topPos() + PANEL_HEIGHT, PANEL_FILL);

        graphics.drawString(this.font, this.title, leftPos() + 8, topPos() + 8, TEXT_MAIN);
        graphics.drawString(this.font, Component.translatable("wandzz.gui.table.hint"),
                leftPos() + 8, topPos() + 20, TEXT_DIM);

        renderWand(graphics);
        renderSockets(graphics, mouseX, mouseY);
        renderInventory(graphics, mouseX, mouseY);

        if (wand.isEmpty()) {
            graphics.drawString(this.font, Component.translatable("wandzz.gui.table.no_wand")
                    .withStyle(ChatFormatting.RED), leftPos() + 8, topPos() + 76, TEXT_MAIN);
        } else {
            graphics.drawString(this.font, Component.translatable("wandzz.gui.table.slots",
                            staged.size(), capacity),
                    leftPos() + SOCKET_X, topPos() + 76, TEXT_DIM);
        }

        renderHoverLabel(graphics, mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, tickDelta);
    }

    private void renderWand(final GuiGraphics graphics) {
        drawSlotFrame(graphics, leftPos() + 16, topPos() + SOCKET_Y);
        if (!wand.isEmpty()) {
            graphics.renderItem(wand, leftPos() + 18, topPos() + SOCKET_Y + 2);
            graphics.drawString(this.font, Component.translatable("wandzz.gui.table.wand"),
                    leftPos() + 8, topPos() + SOCKET_Y + 24, TEXT_DIM);
            graphics.drawString(this.font, wand.getHoverName(), leftPos() + 8, topPos() + SOCKET_Y + 34,
                    TEXT_MAIN);
        }
    }

    private void renderSockets(final GuiGraphics graphics, final int mouseX, final int mouseY) {
        for (int i = 0; i < capacity; i++) {
            int x = leftPos() + SOCKET_X + (i % SOCKET_COLUMNS) * SOCKET_PITCH;
            int y = topPos() + SOCKET_Y + (i / SOCKET_COLUMNS) * SOCKET_PITCH;
            drawSlotFrame(graphics, x, y);
            boolean hovered = isInside(mouseX, mouseY, x, y);
            if (hovered) {
                graphics.fill(x + 1, y + 1, x + 17, y + 17, 0x40FFFFFF);
            }
            if (i < staged.size()) {
                graphics.renderItem(coreStack(staged.get(i)), x + 2, y + 2);
            }
        }
    }

    private void renderInventory(final GuiGraphics graphics, final int mouseX, final int mouseY) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        for (int row = 0; row < INV_ROWS; row++) {
            for (int column = 0; column < INV_COLUMNS; column++) {
                int slot = row * INV_COLUMNS + column;
                int x = leftPos() + INV_X + column * INV_PITCH;
                int y = topPos() + INV_Y + row * INV_PITCH;
                ItemStack stack = client.player.getInventory().getItem(slot);
                boolean core = stack.getItem() instanceof WandCoreItem;
                drawSlotFrame(graphics, x, y);
                if (!stack.isEmpty()) {
                    graphics.renderItem(stack, x + 1, y + 1);
                }
                if (!core && !stack.isEmpty()) {
                    // przygaszenie - tu klika sie TYLKO rdzenie
                    graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xA0101010);
                }
            }
        }
        graphics.drawString(this.font, Component.translatable("wandzz.gui.table.inventory"),
                leftPos() + INV_X, topPos() + INV_Y - 10, TEXT_DIM);
    }

    private void renderHoverLabel(final GuiGraphics graphics, final int mouseX, final int mouseY) {
        Component label = null;
        int socket = socketAt(mouseX, mouseY);
        if (socket >= 0 && socket < staged.size()) {
            label = coreName(staged.get(socket));
        } else {
            int slot = inventorySlotAt(mouseX, mouseY);
            Minecraft client = Minecraft.getInstance();
            if (slot >= 0 && client.player != null) {
                ItemStack stack = client.player.getInventory().getItem(slot);
                if (stack.getItem() instanceof WandCoreItem) {
                    label = stack.getHoverName();
                }
            }
        }
        if (label != null) {
            graphics.drawString(this.font, label, mouseX + 8, mouseY + 8, SLOT_HOVER);
        }
    }

    // ------------------------------------------------------------------
    // Mysz
    // ------------------------------------------------------------------

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        // Najpierw przyciski (Zatwierdz/Czysc), zeby klik na nie nie "przeszedl"
        // przez gniazda pod spodem.
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        int mouseX = (int) event.x();
        int mouseY = (int) event.y();

        int socket = socketAt(mouseX, mouseY);
        if (socket >= 0) {
            if (socket < staged.size()) {
                staged.remove(socket);
            }
            return true;
        }

        int slot = inventorySlotAt(mouseX, mouseY);
        Minecraft client = Minecraft.getInstance();
        if (slot >= 0 && client.player != null) {
            ItemStack clicked = client.player.getInventory().getItem(slot);
            if (clicked.getItem() instanceof WandCoreItem && staged.size() >= capacity) {
                // All slots full - say so instead of ignoring the click.
                client.player.displayClientMessage(
                        Component.translatable("wandzz.wand.no_free_slot"), true);
                return true;
            }
        }
        if (slot >= 0 && client.player != null && staged.size() < capacity) {
            ItemStack stack = client.player.getInventory().getItem(slot);
            if (stack.getItem() instanceof WandCoreItem coreItem) {
                // Prawy przycisk = wez WSZYSTKO, co sie zmiesci (szybsze uzupelnianie).
                int room = capacity - staged.size();
                int count = event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT ? room : 1;
                for (int i = 0; i < count; i++) {
                    staged.add(coreItem.coreType());
                }
                return true;
            }
        }
        return event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT || event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT;
    }

    /** Indeks gniazda pod kursorem, -1 jesli obok. */
    private int socketAt(final int mouseX, final int mouseY) {
        for (int i = 0; i < capacity; i++) {
            int x = leftPos() + SOCKET_X + (i % SOCKET_COLUMNS) * SOCKET_PITCH;
            int y = topPos() + SOCKET_Y + (i / SOCKET_COLUMNS) * SOCKET_PITCH;
            if (isInside(mouseX, mouseY, x, y)) {
                return i;
            }
        }
        return -1;
    }

    /** Slot ekwipunku (0..35) pod kursorem, -1 jesli obok siatki. */
    private int inventorySlotAt(final int mouseX, final int mouseY) {
        for (int row = 0; row < INV_ROWS; row++) {
            for (int column = 0; column < INV_COLUMNS; column++) {
                int x = leftPos() + INV_X + column * INV_PITCH;
                int y = topPos() + INV_Y + row * INV_PITCH;
                if (isInside(mouseX, mouseY, x, y)) {
                    return row * INV_COLUMNS + column;
                }
            }
        }
        return -1;
    }

    /** Pole slotu to 16x16 px, narysowane od (x+1, y+1). */
    private static boolean isInside(final int mouseX, final int mouseY, final int x, final int y) {
        return mouseX >= x + 1 && mouseX < x + 17 && mouseY >= y + 1 && mouseY < y + 17;
    }

    private void drawSlotFrame(final GuiGraphics graphics, final int x, final int y) {
        graphics.fill(x, y, x + 18, y + 18, SLOT_BG);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xE00A0A12);
    }

    private static ItemStack coreStack(CoreType core) {
        return new ItemStack(ModItems.CORES.get(core));
    }

    private static Component coreName(CoreType core) {
        return Component.translatable("item." + Wandzz.MOD_ID + "." + core.translationKey());
    }

    // Ekran nie jest kontenerem, wiec sami liczymy srodek - te same wzory co
    // w vanilla AbstractContainerScreen#init.
    private int leftPos() {
        return (this.width - PANEL_WIDTH) / 2;
    }

    private int topPos() {
        return (this.height - PANEL_HEIGHT) / 2;
    }

    @Override
    public boolean isPauseScreen() {
        // Bez tego w singleplayerze swiat (i regen many na HUD-zie) stanalby w miejscu.
        return false;
    }
}
