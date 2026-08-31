package com.wandzz.client;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/**
 * Stan renderowania dla modeli z rodziny "fluff" (jednorozec, feniks, Chronos).
 *
 * Renderer w 1.21.5+ NIE czyta encji w klatce animacji: najpierw leci
 * {@code extractRenderState} (raz na klatke, na watku renderujacym), a model
 * dostaje wylacznie ten obiekt. Dlatego jedyne, co model musi znac poza tym, co
 * daje vanilla, to flaga "oskubany" - wiek, obroty i chod sa juz w stanie.
 */
public class FluffRenderState extends LivingEntityRenderState {

    /** True = jednorozec bez wlosa: model kladzie wtedy roek i siada nizej. */
    public boolean shorn;
}
