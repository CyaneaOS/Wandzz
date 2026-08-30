package com.wandzz.item;

import com.wandzz.Wandzz;
import com.wandzz.block.ModBlocks;
import com.wandzz.core.WandCoreItem;
import com.wandzz.wand.WandWood;
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
                .icon(() -> new ItemStack(ModItems.wand(WandWood.ARCANE, true)))
                .displayItems((parameters, output) -> {
                    // 1. Rozdzki: najpierw arkanska (najlepsza), potem vanilla wg
                    //    kolejnosci enuma - dwoch tej samej rodziny obok siebie.
                    for (WandWood wood : WandWood.values()) {
                        output.accept(ModItems.wand(wood, false));
                    }
                    for (WandWood wood : WandWood.values()) {
                        output.accept(ModItems.wand(wood, true));
                    }
                    // 2. Rdzenie
                    for (WandCoreItem core : ModItems.CORES.values()) {
                        output.accept(core);
                    }
                    // 3. Patyki (surowiec na rozdzke) - kazde drewno ma wlasny.
                    for (WandWood wood : WandWood.values()) {
                        output.accept(ModItems.stick(wood));
                    }
                    // 4. Bloki arkanum: drewno, sadzonka, liscie i stol.
                    output.accept(ModBlocks.ARCANE_LOG.asItem());
                    output.accept(ModBlocks.ARCANE_PLANKS.asItem());
                    output.accept(ModBlocks.ARCANE_LEAVES.asItem());
                    output.accept(ModBlocks.ARCANE_SAPLING.asItem());
                    output.accept(ModBlocks.ARCANE_TABLE.asItem());
                })
                .build());
    }

    private ModItemGroups() {
    }
}
