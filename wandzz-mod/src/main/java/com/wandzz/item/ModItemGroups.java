package com.wandzz.item;

import com.wandzz.Wandzz;
import com.wandzz.core.WandCoreItem;
import com.wandzz.wand.WandItem;
import com.wandzz.wand.WandMaterial;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/**
 * Wlasna zakladka "Wandzz" w kreatywie.
 *
 * Bez tego przedmioty sa zarejestrowane, ale nie ma ich gdzie wziac (trzeba
 * '/give'). Fabric 1.21.11 + Mojmap: buduje sie przez
 * {@code FabricItemGroup.builder()} (zwraca vanilla {@code CreativeModeTab.Builder}),
 * a efekt trzeba recznie wrzucic do rejestru {@code BuiltInRegistries.CREATIVE_MODE_TAB}
 * - tego wymaga sam javadoc FabricItemGroup.
 */
public final class ModItemGroups {

    private static final ResourceKey<CreativeModeTab> WANDZZ = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB,
            Identifier.fromNamespaceAndPath(Wandzz.MOD_ID, "wandzz"));

    public static void bootstrap() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, WANDZZ, FabricItemGroup.builder()
                .title(Component.translatable("itemGroup.wandzz.wandzz"))
                .icon(() -> new ItemStack(ModItems.WANDS.get(WandMaterial.RARE_MAGIC)))
                .displayItems((parameters, output) -> {
                    for (WandItem wand : ModItems.WANDS.values()) {
                        output.accept(wand);
                    }
                    for (WandCoreItem core : ModItems.CORES.values()) {
                        output.accept(core);
                    }
                })
                .build());
    }

    private ModItemGroups() {
    }
}
