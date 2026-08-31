package com.wandzz.client;

import com.wandzz.Wandzz;
import com.wandzz.entity.ArcaneSprite;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

/**
 * Renderer ducha arkanu.
 *
 * Trzy sygnatury musza byc w 1.21.11 dokladnie takie jak w vanilla (wzor:
 * {@code BatRenderer}), bo to nie sa nadpisania "z gwaltu":
 *   - {@code createRenderState()} - nowy stan na kazda encje,
 *   - {@code extractRenderState(encja, stan, czesc)} - jedyne miejsce, gdzie
 *     wolno czytac encje (robione na watku renderujacym, raz na klatke),
 *   - {@code getTextureLocation(stan)} - bierze STAN, nie encje: vanilla nie ufa
 *     juz temu, ze renderer moze siegac do swiata.
 */
public class ArcaneSpriteRenderer extends MobRenderer<ArcaneSprite, ArcaneSpriteRenderState> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(Wandzz.MOD_ID, "textures/entity/arcane_sprite.png");

    public ArcaneSpriteRenderer(final EntityRendererProvider.Context context) {
        super(context, new ArcaneSpriteModel(context.bakeLayer(ArcaneSpriteModel.LAYER_LOCATION)), 0.28F);
    }

    @Override
    public Identifier getTextureLocation(final ArcaneSpriteRenderState state) {
        return TEXTURE;
    }

    @Override
    public ArcaneSpriteRenderState createRenderState() {
        return new ArcaneSpriteRenderState();
    }

    @Override
    public void extractRenderState(final ArcaneSprite entity, final ArcaneSpriteRenderState state,
            final float partialTick) {

        super.extractRenderState(entity, state, partialTick);
        state.perched = entity.isPerched();
        state.tamed = entity.isTamed();
        state.wingPhase = entity.wingPhase();
    }
}
