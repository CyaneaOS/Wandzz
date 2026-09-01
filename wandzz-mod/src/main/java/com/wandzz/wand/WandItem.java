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

    /**
     * Poswiata zaklec na magicznych rozdzkach - ROZWIAZANIE TYMCZASOWE.
     *
     * Magiczne warianty nie maja jeszcze wlasnej grafiki: ich model wskazuje
     * te sama teksture co wariant zwyczajny (wandzz:item/<drewno>_wand), wiec
     * bez tego nadpisania obie rozdzki bylyby nie do rozroznienia. Vanilla
     * 1.21.11 pyta o to w ItemStack#hasFoil(): najpierw patrzy na komponent
     * minecraft:enchantment_glint_override, potem na Item#isFoil(ItemStack)
     * (domylnie: stack.isEnchanted()), a BlockModelWrapper doklada warstwe
     * "foil" w ekwipunku, rece i na ziemi - czyli jeden override obsluguje
     * wszystkie widoki: bez kodu klienckiego i bez dokladania komponentu do
     * kazdego stacka.
     *
     * Kiedy bedzie wlasna grafika (<drewno>_wand_magic.png), trzeba tylko
     * przestawic warstwe w modelu na nia, a ten nadpis usunac.
     */
    @Override
    public boolean isFoil(ItemStack stack) {
        return magic || super.isFoil(stack);
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

    /**
     * Liczba gniazd; 0 jesli podany stack to nie rozdzka.
     *
     * TO JEST zrodlo prawdy o gniazdach (nie {@link #coreCapacity()}): baza idzie
     * z gatunku drewna, a +{@value #RESIN_SLOTS} doklada zywica, ktora LEZY NA
     * STACKU. Stad wszystkie sciezki (stolik klienta, insertCore,
     * setLoadout, tooltip) musza liczyc z tej funkcji, a nie z itemu - inaczej rozdzka nasaczona
     * przyjmowalaby rdzenie, ktorych UI nie pokazuje.
     */
    public static int capacity(@Nullable ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof WandItem wand)) {
            return 0;
        }
        return wand.coreCapacity() + (getData(stack).resinated() ? RESIN_SLOTS : 0);
    }

    /** Bonus z zywicy arkanu - patrz {@link WandData#withResin()}. */
    public static final int RESIN_SLOTS = 1;

    /**
     * Nasacenie rozdzki zywica. True = przyjel; false = byla juz nasaczona.
     * Wolane z {@code WandInteractions} (PPM zywica), czyli tylko z serwera.
     */
    public static boolean applyResin(ItemStack wandStack) {
        WandData current = getData(wandStack);
        WandData updated = current.withResin();
        if (updated == current) {
            return false;
        }
        wandStack.set(ModComponents.WAND_DATA, updated);
        return true;
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
        // stan czytamy w calosci: nadpisanie skladow bez `resinated` zdjeloby
        // nalozony wczesniej laczny bonus (+1 gniazdo, x1.2 regenu, PPM na ember)
        WandData current = getData(wandStack);
        int slots = capacity(wandStack);
        List<CoreType> fitted = new ArrayList<>(cores.subList(0, Math.min(slots, cores.size())));
        wandStack.set(ModComponents.WAND_DATA, new WandData(List.copyOf(fitted), current.resinated()));
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

        if (data.resinated()) {
            tooltip.accept(Component.translatable("wandzz.tooltip.resinated")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }

        if (magic) {
            tooltip.accept(Component.translatable("wandzz.tooltip.magic")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }

        // capacity(itemStack), NIE coreCapacity(): nasaczona rozdzka ma jedno
        // gniazdo wiecej i tooltip musilby to pokazac, inaczej "pelna" rozdzka
        // z 5 rdzeniami wygladalaby na zbugowana.
        int capacity = capacity(itemStack);
        tooltip.accept(Component.translatable("wandzz.tooltip.slots", cores.size(), capacity)
                .withStyle(cores.size() >= capacity ? ChatFormatting.GREEN : ChatFormatting.AQUA));

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
