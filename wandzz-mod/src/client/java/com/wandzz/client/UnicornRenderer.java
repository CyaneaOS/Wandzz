package com.wandzz.client;

import com.wandzz.Wandzz;
import com.wandzz.entity.Unicorn;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

/**
 * Renderer jednorozca (model koniowaty, wlasna warstwa, wlasna tekstura).
 *
 * <p>Sciezka tekstury jest tu, a nie w {@code FluffRendererTextures}, bo od tej
 * rundy jednorozec nie dzieli juz modelu z feniksem i Chronosem - ma arkusz
 * 64x64 zamiast 32x32, wiec trzymanie go we wspolnej tabelce byloby klamstwem.
 * Nazwe pliku {@code unicorn_txt.png} zostawiamy taka, jaka gracz wpisal do
 * repo (patrz README, sekcja "Tekstury: co jest czyje").
 *
 * <p>Sygnatury nadpisanek sa kopiowane z FluffRenderer (sprawdzone na 1.21.11):
 * {@code createRenderState()}, {@code extractRenderState(encja, stan, czesc)},
 * {@code getTextureLocation(stan)} - ten ostatni bierze STAN, nie encje.
 */
public class UnicornRenderer extends MobRenderer<Unicorn, UnicornRenderState, UnicornModel> {

    public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            Wandzz.MOD_ID, "textures/entity/unicorn_txt.png");

    public UnicornRenderer(final EntityRendererProvider.Context context) {
        // 0.5F = promien cienia jak u konia (AbstractHorseRenderer); cienie idzie
        // od encji, nie od gabrytu modelu, wiec przy 1.5F glowa by sie swiecila
        super(context, new UnicornModel(context.bakeLayer(UnicornModel.LAYER)), 0.5F);
    }

    @Override
    public Identifier getTextureLocation(final UnicornRenderState state) {
        return TEXTURE;
    }

    @Override
    public UnicornRenderState createRenderState() {
        return new UnicornRenderState();
    }

    @Override
    public void extractRenderState(final Unicorn unicorn, final UnicornRenderState state,
            final float partialTick) {
        super.extractRenderState(unicorn, state, partialTick);
        // jedyny most encja -> stan; reszta (chod, obroty glowy, woda) juz tu jest
        state.shorn = unicorn.isSheared();
    }
}
