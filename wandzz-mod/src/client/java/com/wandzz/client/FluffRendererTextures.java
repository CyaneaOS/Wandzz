package com.wandzz.client;

import com.wandzz.Wandzz;
import net.minecraft.resources.Identifier;

/**
 * Sciezki tekstur dwoch encji rysowanych jednym modelem (jednorozec przeszedl
 * na wlasny model koniowaty - patrz {@code UnicornRenderer.TEXTURE}).
 *
 * Osobna klasa, bo sciezke musza znac obie strony (tu renderer, a przy
 * generowaniu placeholderow - skrypt), a trzy konstruktory w rendererze bylyby
 * czystym szumem.
 *
 * Kontrakt na grafike (tekstury robi gracz): kazdy plik 32x32, UV z FluffModel.
 */
public final class FluffRendererTextures {

    public static final Identifier PHOENIX =
            Identifier.fromNamespaceAndPath(Wandzz.MOD_ID, "textures/entity/phoenix.png");
    public static final Identifier CHRONOS =
            Identifier.fromNamespaceAndPath(Wandzz.MOD_ID, "textures/entity/chronos_boss.png");

    private FluffRendererTextures() {
    }
}
