package com.wandzz.mana;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Stan many gracza. Przechowywany jako Fabric Data Attachment (patrz
 * ManaAttachments) - nie wymaga mixinow ani wlasnej implementacji Capability.
 */
public record ManaComponent(double current, double max) {

    public static final Codec<ManaComponent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.fieldOf("current").forGetter(ManaComponent::current),
            Codec.DOUBLE.fieldOf("max").forGetter(ManaComponent::max)
    ).apply(instance, ManaComponent::new));

    public static final double DEFAULT_MAX = 100.0;
    public static final double DEFAULT_REGEN_PER_SECOND = 1.0;

    public static ManaComponent full() {
        return new ManaComponent(DEFAULT_MAX, DEFAULT_MAX);
    }

    public boolean has(double amount) {
        return current >= amount;
    }

    public ManaComponent spend(double amount) {
        return new ManaComponent(Math.max(0, current - amount), max);
    }

    public ManaComponent regen(double amount) {
        return new ManaComponent(Math.min(max, current + amount), max);
    }
}
