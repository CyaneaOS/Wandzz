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

    // --- Jednorozec, feniks, Chronos (runda 15) ---------------------------------
    // Wszystkie trzy to CREATURE/MONSTER z removeWhenFarAway()=false: mob, ktory
    // znika, kiedy gracz wraca po miecz, jest gorszy niz mob, ktory czeka.

    public static final ResourceKey<EntityType<?>> UNICORN_KEY = ResourceKey.create(
            Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(Wandzz.MOD_ID, "unicorn"));

    public static final ResourceKey<EntityType<?>> PHOENIX_KEY = ResourceKey.create(
            Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(Wandzz.MOD_ID, "phoenix"));

    public static final ResourceKey<EntityType<?>> CHRONOS_BOSS_KEY = ResourceKey.create(
            Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(Wandzz.MOD_ID, "chronos_boss"));

    // Hitbox jednorozca: 1.3 x 1.5 (vanilla HORSE to 1.3964844 x 1.6,
    // eyeHeight 1.52 - wziete z rejestru EntityType). Nasz model to geometrycznie
    // kon, wiec box musi miec te same proporcje: 1.1 wczesniej (kula z FluffModel)
    // obcinata mu glowe i kark - graczy celowal w pustke nad grzbietem.
    // Builder#sized() tworzy EntityDimensions.scalable(...), a nie fixed(...):
    // dzieki temu ageScale (baby, jezelb kiedys dojdzie) skaluje tez hitbox.
    public static final EntityType<Unicorn> UNICORN = Registry.register(
            BuiltInRegistries.ENTITY_TYPE, UNICORN_KEY,
            EntityType.Builder.of(Unicorn::new, MobCategory.CREATURE)
                    .sized(1.3F, 1.5F)
                    .eyeHeight(1.42F)
                    .clientTrackingRange(8)
                    .updateInterval(3)
                    .build(UNICORN_KEY));

    public static final EntityType<Phoenix> PHOENIX = Registry.register(
            BuiltInRegistries.ENTITY_TYPE, PHOENIX_KEY,
            EntityType.Builder.of(Phoenix::new, MobCategory.CREATURE)
                    // fireImmune() to jedyna poprawna droga do "feniks nie boi sie
                    // wlasnego plomienia": w 1.21.11 to flaga rejestru encji, a nie
                    // nadpisanie w hurt()
                    .fireImmune()
                    .sized(0.9F, 1.0F)
                    .eyeHeight(0.8F)
                    .clientTrackingRange(8)
                    .updateInterval(3)
                    .build(PHOENIX_KEY));

    public static final EntityType<ChronosBoss> CHRONOS_BOSS = Registry.register(
            BuiltInRegistries.ENTITY_TYPE, CHRONOS_BOSS_KEY,
            EntityType.Builder.of(ChronosBoss::new, MobCategory.MONSTER)
                    .sized(1.6F, 2.2F)
                    .eyeHeight(1.8F)
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .build(CHRONOS_BOSS_KEY));

    /** Wolane z common entrypointu, przed pierwszym wejsciem na swiat. */
    public static void bootstrap() {
        FabricDefaultAttributeRegistry.register(ARCANE_SPRITE, ArcaneSprite.createAttributes());
        FabricDefaultAttributeRegistry.register(UNICORN, Unicorn.createAttributes());
        FabricDefaultAttributeRegistry.register(PHOENIX, Phoenix.createAttributes());
        FabricDefaultAttributeRegistry.register(CHRONOS_BOSS, ChronosBoss.createAttributes());
    }

    private ModEntities() {
    }
}
