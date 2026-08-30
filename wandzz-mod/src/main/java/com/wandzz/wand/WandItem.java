package com.wandzz.wand;

import com.wandzz.Wandzz;
import com.wandzz.core.CoreType;
import com.wandzz.item.ModComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Rozdzka. Sama w sobie nie "rzuca" zaklec - to robi CastingHandler po stronie
 * serwera, na podstawie gestu rozpoznanego po stronie klienta (patrz WandzzClient
 * / CastingScreen). WandItem przechowuje tylko gatunek drewna, flage
 * "magiczna" oraz liste zainstalowanych rdzeni ({@link WandData}).
 *
 * Gatunek drewna jest polem ITEMU (jeden zarejestrowany WandItem = jedno
 * drewno), nie komponentu - dzieki temu liczba gniazd jest zawsze zgodna z
 * tym, co gracz zobaczy w tooltipie i w oknie stolika.
 */
public class WandItem extends Item {

    private final WandWood wood;
    private final boolean magic;

    public WandItem(WandWood wood, boolean magic, Properties properties) {
        super(properties.component(ModComponents.WAND_DATA, WandData.EMPTY));
        this.wood = wood;
        this.magic = magic;
    }

    public WandWood wood() {
        return wood;
    }

    public boolean isMagic() {
        return magic;
    }

    /** Liczba gniazd na rdzenie dana rozdzka (1 bazowe + dodatki z drewna). */
    public int coreCapacity() {
        return wood.totalSlots(magic);
    }

    // ------------------------------------------------------------------
    // Operacje na stacku - wolane z serwera (stolik, PPM-zwrot, CastingHandler)
    // ------------------------------------------------------------------

    public static WandData getData(ItemStack stack) {
        WandData data = stack.get(ModComponents.WAND_DATA);
        return data != null ? data : WandData.EMPTY;
    }

    /** Liczba gniazd; 0 jesli podany stack to nie rozdzka. */
    public static int capacity(@Nullable ItemStack stack) {
        return stack != null && stack.getItem() instanceof WandItem wand ? wand.coreCapacity() : 0;
    }

    /** Rozdzka trzymana przez gracza: najpierw reka glowna, potem druga; null jesli brak. */
    public static @Nullable ItemStack findWand(Player player) {
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof WandItem) return main;
        ItemStack off = player.getOffhandItem();
        return off.getItem() instanceof WandItem ? off : null;
    }

    /** Proba wlozenia rdzenia - true, jesli bylo wolne gniazdo. */
    public static boolean insertCore(ItemStack wandStack, CoreType core) {
        WandData current = getData(wandStack);
        WandData updated = current.withCoreAdded(core, capacity(wandStack));
        if (updated == current) return false;
        wandStack.set(ModComponents.WAND_DATA, updated);
        return true;
    }

    /** Wyjecie pierwszego takiego rdzenia (sneak+PPM / stolik). */
    public static boolean removeCore(ItemStack wandStack, CoreType core) {
        WandData current = getData(wandStack);
        WandData updated = current.withCoreRemoved(core);
        if (updated == current) return false;
        wandStack.set(ModComponents.WAND_DATA, updated);
        return true;
    }

    /**
     * Ustawienie calego skladu naraz (sciezka "Zatwierdz" w oknie stolika).
     * Lista jest przycinana do liczby gniazd, wiec klient nie da sie wcisnac
     * wiecej rdzeni, niz rozdzka uniesie.
     */
    public static boolean setLoadout(ItemStack wandStack, List<CoreType> cores) {
        int capacity = capacity(wandStack);
        List<CoreType> fitted = new ArrayList<>(cores.subList(0, Math.min(capacity, cores.size())));
        wandStack.set(ModComponents.WAND_DATA, new WandData(List.copyOf(fitted)));
        return true;
    }

    // ------------------------------------------------------------------
    // Tooltip: z jakiego drewna, ile gniazd, jakie rdzenie
    // ------------------------------------------------------------------

    /**
     * Minecraft 1.21.11: {@code Item#appendHoverText(ItemStack, TooltipContext, List, TooltipFlag)}
     * zostal zastapiony wersja z {@code TooltipDisplay} i {@code Consumer<Component>},
     * oznaczona {@code @Deprecated} (vanilla docelowo chce generowac linie z
     * komponentow). Nadpisujemy ja, bo to jedyne miejsce, gdzie linia moze byc
     * liczona ze stanu przedmiotu, a nie statyczna (jak LORE).
     */
    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(final ItemStack itemStack, final Item.TooltipContext context,
            final TooltipDisplay display, final Consumer<Component> tooltip, final TooltipFlag flag) {

        WandData data = getData(itemStack);
        List<CoreType> cores = data.cores();

        tooltip.accept(Component.translatable("wandzz.tooltip.wood",
                        Component.translatable(wood.woodTranslationKey()))
                .withStyle(ChatFormatting.GRAY));

        if (magic) {
            tooltip.accept(Component.translatable("wandzz.tooltip.magic")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }

        tooltip.accept(Component.translatable("wandzz.tooltip.slots",
                        cores.size(), coreCapacity())
                .withStyle(cores.size() >= coreCapacity() ? ChatFormatting.GREEN : ChatFormatting.AQUA));

        if (cores.isEmpty()) {
            tooltip.accept(Component.translatable("wandzz.tooltip.no_cores")
                    .withStyle(ChatFormatting.DARK_GRAY));
            return;
        }

        int index = 0;
        for (CoreType core : cores) {
            tooltip.accept(Component.translatable("wandzz.tooltip.core",
                            ++index,
                            Component.translatable("item." + Wandzz.MOD_ID + "." + core.translationKey()),
                            core.level())
                    .withStyle(ChatFormatting.YELLOW));
        }
    }
}
