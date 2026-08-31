package com.wandzz.client;

import com.wandzz.entity.Unicorn;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;

/**
 * Renderer wspolny dla jednoroza, feniksa i Chronosa.
 *
 * Jedna klasa na trzy encje jest mozliwa dzieki pierwszym argumencie typu:
 * {@code MobRenderer<T extends Mob, S extends LivingEntityRenderState,
 * M extends EntityModel<S>>}, a rejestracja przyjmuje
 * {@code EntityRenderer<? super Unicorn>} - {@code EntityRenderer<Mob>} miesci
 * sie w tym bez zadnego castu. Roznica miedzy encjami to tekstura i warstwa
 * modelu (skala), obie w konstruktorze.
 *
 * Sygnatury sa kopiowane z vanilla (wzor: BatRenderer, ArcaneSpriteRenderer) -
 * w 1.21.11 to NIE sa nadpisania "z gwaltu":
 *   - {@code createRenderState()},
 *   - {@code extractRenderState(encja, stan, czesc)},
 *   - {@code getTextureLocation(stan)}: bierze STAN, nie encje.
 */
public class FluffRenderer extends MobRenderer<Mob, FluffRenderState, FluffModel> {

    private final Identifier texture;

    public FluffRenderer(final EntityRendererProvider.Context context, final Identifier texture,
            final ModelLayerLocation layer) {
        super(context, new FluffModel(context.bakeLayer(layer)), 0.5F);
        this.texture = texture;
    }

    @Override
    public Identifier getTextureLocation(final FluffRenderState state) {
        return this.texture;
    }

    @Override
    public FluffRenderState createRenderState() {
        return new FluffRenderState();
    }

    @Override
    public void extractRenderState(final Mob entity, final FluffRenderState state,
            final float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        // wzor z ArcaneSpriteRenderer: czytamy encje WYLACZNIE tutaj
        state.shorn = entity instanceof Unicorn && ((Unicorn) entity).isSheared();
    }
}
