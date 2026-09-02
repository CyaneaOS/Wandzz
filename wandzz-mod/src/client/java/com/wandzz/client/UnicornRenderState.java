package com.wandzz.client;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/**
 * Stan renderowania jednorozca.
 *
 * <p>W 1.21.5+ renderer NIE czyta encji w kazdej klatce: najpierw leci
 * {@code extractRenderState} (raz na klatke), a model dostaje wylacznie ten
 * obiekt. Dlatego lista pol jest tu prawie pusta - vanilla wklada w
 * {@code LivingEntityRenderState} wszystko, czego animacja koniowata potrzebuje:
 * {@code walkAnimationPos}, {@code walkAnimationSpeed}, {@code xRot}, {@code yRot},
 * {@code ageInTicks}, {@code ageScale}, {@code isInWater}, {@code deathTime}
 * (nazwy sprawdzone w zrzucie zrodel 1.21.11, plik
 * {@code client/renderer/entity/state/LivingEntityRenderState.java}).
 *
 * <p>Zostaje jedna rzecz wlasna: informacja o strzyzeniu, bo jej zaden stan
 * encji nie niesie.
 */
public class UnicornRenderState extends LivingEntityRenderState {

    /** True = jednorozec bez grzywy i ogona (patrz UnicornModel#setupAnim). */
    public boolean shorn;
}
