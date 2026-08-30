package com.wandzz.recipe;

import com.wandzz.core.CoreType;
import com.wandzz.core.WandCoreItem;
import com.wandzz.wand.WandItem;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Wkladanie rdzenia do rozdzki w STOLE KOWALSKIM (smithing table).
 *
 *   slot 1 (szablon) : pusty - swiadomie, patrz nizej
 *   slot 2 (base)    : rozdzka (#wandzz:wands)
 *   slot 3 (addition): dowolny rdzen (#wandzz:cores)
 *   wynik            : TA SAMA rozdzka + jeden rdzen w wolnym slocie
 *
 * Dlaczego wlasna klasa przepisu, a nie vanilla `minecraft:smithing_transform`?
 * Dwa powody:
 *  1. `smithing_transform` wymaga wypelnienia `result` na sztywno (TransmuteResult),
 *     a tutaj wynik zalezy od tego, co JUZ jest w rozdzce - przepis data-driven
 *     skasowalby poprzednio wlozone rdzenie przy kazdej rozbudowie.
 *  2. `SmithingRecipe#templateIngredient()` zwraca `Optional`, wiec pusty szablon
 *     jest legalny (match odbywa sie przez `Ingredient#testOptionalIngredient`);
 *     nie trzeba wiec wymyslac przedmiotu-szablonu tylko po to,eby spelnic slot.
 *
 * RecipeType jest bezplatny: `SmithingRecipe` ma `default RecipeType getType()`
 * zwracajace `RecipeType.SMITHING`, wiec menu SmithingMenu znajdzie ten przepis
 * mimo ze jego `type` w JSON-ie jest nasz.
 */
public class WandCoreSmithingRecipe implements SmithingRecipe {

    public static final RecipeSerializer<WandCoreSmithingRecipe> SERIALIZER = new Serializer();

    private final Ingredient base;
    private final Ingredient addition;

    /**
     * PlacementInfo liczone leniwie - dokladnie jak w vanilla SmithingTransformRecipe.
     *
     * Budowanie PlacementInfo w konstruktorze wywala caly load swiata:
     *   IllegalStateException: Trying to access unbound tag 'TagKey[minecraft:item / wandzz:wands]'
     * Bo (1) RecipeManager parseruje JSON rownolegle w watkach ForkJoin (przed
     * `apply`), a (2) PlacementInfo wypycha skladniki (PlacementInfo#createFromOptionals
     * -> Ingredient#values -> RegistryFixedCodec) - a tagi `data/wandzz/tags/item/*`
     * sa powiazane z rejestrem dopiero pozniej, w fazie `apply`. Sam Ingredient z
     * kodka trzyma HolderSet leniwie, wiec do momentu gdy ktos go nie "wypcha",
     * nie ma problemu - stad w vanilla `if (this.placementInfo == null)`.
     */
    @Nullable
    private PlacementInfo placementInfo;

    public WandCoreSmithingRecipe(final Ingredient base, final Ingredient addition) {
        this.base = base;
        this.addition = addition;
    }

    @Override
    public boolean matches(final SmithingRecipeInput input, final Level level) {
        if (!this.base.test(input.base()) || !this.addition.test(input.addition())) {
            return false;
        }
        if (coreOf(input.addition()) == null) {
            return false;
        }
        // Wolny slot to warunek wyswietlenia wyniku - gracz widzi puste okno
        // zamiast "cichego" braku efektu, gdy rozdzka jest pelna.
        return WandItem.getData(input.base()).freeSlots() > 0;
    }

    @Override
    public ItemStack assemble(final SmithingRecipeInput input, final HolderLookup.Provider registries) {
        ItemStack result = input.base().copy();
        result.setCount(1);
        CoreType core = coreOf(input.addition());
        if (core != null) {
            WandItem.insertCore(result, core);
        }
        return result;
    }

    /** Wynik zalezy od wejscia, wiec przepis jest "special" (vanilla tak oznacza reczne receptury). */
    @Override
    public boolean isSpecial() {
        return true;
    }

    private static CoreType coreOf(final ItemStack stack) {
        return stack.getItem() instanceof WandCoreItem coreItem ? coreItem.coreType() : null;
    }

    @Override
    public Optional<Ingredient> templateIngredient() {
        return Optional.empty();
    }

    @Override
    public Ingredient baseIngredient() {
        return this.base;
    }

    @Override
    public Optional<Ingredient> additionIngredient() {
        return Optional.of(this.addition);
    }

    @Override
    public PlacementInfo placementInfo() {
        if (this.placementInfo == null) {
            // Slot szablonu (0) jest pusty (Optional.empty), wiec shift-click
            // w stol kowalski nie probuje tam nic kladz.
            this.placementInfo = PlacementInfo.createFromOptionals(
                    List.of(Optional.empty(), Optional.of(this.base), Optional.of(this.addition)));
        }
        return this.placementInfo;
    }

    @Override
    public RecipeSerializer<WandCoreSmithingRecipe> getSerializer() {
        return SERIALIZER;
    }

    /** RecipeSerializer#streamCodec() jest @Deprecated w vanilla, ale interfejs
     *  nadal go wymaga - wyciszamy notatke z logu builda. */
    @SuppressWarnings("deprecation")
    public static class Serializer implements RecipeSerializer<WandCoreSmithingRecipe> {

        private static final MapCodec<WandCoreSmithingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Ingredient.CODEC.fieldOf("base").forGetter(recipe -> recipe.base),
                Ingredient.CODEC.fieldOf("addition").forGetter(recipe -> recipe.addition)
        ).apply(instance, WandCoreSmithingRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, WandCoreSmithingRecipe> STREAM_CODEC =
                StreamCodec.composite(
                        Ingredient.CONTENTS_STREAM_CODEC, recipe -> recipe.base,
                        Ingredient.CONTENTS_STREAM_CODEC, recipe -> recipe.addition,
                        WandCoreSmithingRecipe::new);

        @Override
        public MapCodec<WandCoreSmithingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, WandCoreSmithingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
