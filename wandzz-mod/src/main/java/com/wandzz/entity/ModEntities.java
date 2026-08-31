package com.wandzz.entity;

import com.wandzz.Wandzz;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/**
 * Rejestracja encji moda.
 *
 * Uwagi 1.21.11, ktore kosztowalyby blad kompilacji:
 *  - budowniczy to {@code EntityType.Builder.of(factory, category)}, NIE
 *    {@code create(...)} (nazwa z Yarna),
 *  - {@code build(...)} bierze {@code ResourceKey<EntityType<?>>}, a nie String -
 *    i z tego samego klucza vanilla WYLICZA sciezke tabeli lootu
 *    ({@code id.identifier().withPrefix("entities/")}), dlatego tablica lezy w
 *    {@code data/wandzz/loot_table/entities/arcane_sprite.json} i nie trzeba jej
 *    podawac recznie (a {@code noLootTable()} zostawiamy swiadomie nieuzyte),
 *  - atrybuty NIE ida przez {@code DefaultAttributes} (prywatna, statyczna mapa
 *    vanilla) i nie ma juz {@code EntityAttributeCreationEvent} - jest
 *    {@link FabricDefaultAttributeRegistry#register}, ktory doklada wpis do tej
 *    samej mapy przez accessor.
 */
public final class ModEntities {

    public static final ResourceKey<EntityType<?>> ARCANE_SPRITE_KEY = ResourceKey.create(
            Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(Wandzz.MOD_ID, "arcane_sprite"));

    public static final EntityType<ArcaneSprite> ARCANE_SPRITE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ARCANE_SPRITE_KEY,
            EntityType.Builder.of(ArcaneSprite::new, MobCategory.CREATURE)
                    .sized(0.5F, 0.6F)
                    .eyeHeight(0.3F)
                    // CREATURE + mob, ktory NIE despawni: tracking 6 chunkow, a
                    // synchronizacja rzadziej niz co tick (wisi w miejscu, po co
                    // wysylac klatki, ktore sie nie zmieniaja)
                    .clientTrackingRange(6)
                    .updateInterval(3)
                    .build(ARCANE_SPRITE_KEY));

    /** Wolane z common entrypointu, przed pierwszym wejsciem na swiat. */
    public static void bootstrap() {
        FabricDefaultAttributeRegistry.register(ARCANE_SPRITE, ArcaneSprite.createAttributes());
    }

    private ModEntities() {
    }
}
