package com.wandzz.recipe;

import com.wandzz.Wandzz;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

/**
 * Rejestracja wlasnych typow przepisow (`type` w JSON-ie z data/wandzz/recipe).
 *
 * Musi sie wydac PRZED wczytaniem data-packow - serializery sa w rejestrze, z ktorego
 * RecipeManager buduje kodeke kazdego pliku przepisu; pozna rejestracja =
 * "Unknown recipe type wandzz:wand_core_smithing" przy wejsciu na swiat.
 */
public final class ModRecipes {

    public static void bootstrap() {
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,
                Identifier.fromNamespaceAndPath(Wandzz.MOD_ID, "wand_core_smithing"),
                WandCoreSmithingRecipe.SERIALIZER);
    }

    private ModRecipes() {
    }
}
