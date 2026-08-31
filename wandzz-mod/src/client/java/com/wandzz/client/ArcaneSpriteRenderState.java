package com.wandzz.client;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/**
 * Stan renderowania ducha arkanu. 1.21.5+ renderer NIE czyta encji w klatce
 * animacji - najpierw leci {@code extractRenderState} (watek renderujacy, raz na
 * klatke), a model dostaje wylacznie ten obiekt. Dlatego wszystko, co animacja
 * musi znac, ladowane jest tu.
 */
public class ArcaneSpriteRenderState extends LivingEntityRenderState {

    /** True = wisi w koronie: skrzydla zlozone, brak machania. */
    public boolean perched;

    /** True = oswojony (renderer doklada wtedy cieplejszy odcien). */
    public boolean tamed;

    /** Faza skrzydel liczona na serwerze (patrz {@code ArcaneSprite#wingPhase}). */
    public float wingPhase;
}
