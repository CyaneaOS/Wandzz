package com.wandzz.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Feniks: zrodlo {@code wandzz:phoenix_feather}, a te sa surowcem na
 * {@code wandzz:core_phoenix} (patrz data/wandzz/recipe/core_phoenix.json).
 *
 * Jesli go zabraknie, rdzen feniksa jest nieosiagalny - to swiadoma roznica
 * wobec reszty rdzeni: te sa rzemieiosla, ten polowaniem.
 *
 * Dwie rzeczy, ktore trzymaja cene w ryzach:
 *  <ul>
 *    <li>ognioodpornosc nie jest tu robiona w {@code hurt} tylko w rejestrze:
 *        {@code EntityType.Builder#fireImmune()} (patrz ModEntities) - dzieki temu
 *        feniks nie spala sam siebie wlasnym plomieniem i nie tonie w lawie,</li>
 *    <li>smiertelne uderzenie podpala atakujacego: mob, ktory tylko dostaje,
 *        jest irytujacy; mob, ktory gryzie w reke, jest ryzykiem.</li>
 *  </ul>
 *
 * Model i renderer sa wspolne z jednorozcem (patrz {@code FluffRenderer}):
 * ten sam kwadruped, inna tekstura i inna skala - dwa nowe modele 32x32 to dwa
 *razy wiecej pracy nad grafika, ktora i tak robi gracz.
 */
public class Phoenix extends PathfinderMob {

    /** Co tyle tickow feniks sypie iskrami (5 = co 0,25 s, zeby nie multic clienta). */
    private static final int EMBER_PERIOD_TICKS = 5;

    public Phoenix(final EntityType<? extends Phoenix> type, final Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 12.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.FLYING_SPEED, 0.6);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.6));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, false));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F, 0.25F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        // iskra leci tylko na serwerze i tylko co kilka tickow: sendParticles to
        // packet do kazdego gracza w zasiegu, a feniks moze byc ich naraz tuzin
        if (!this.level().isClientSide() && this.tickCount % EMBER_PERIOD_TICKS == 0
                && this.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.FLAME,
                    this.getX(), this.getY() + 0.6, this.getZ(),
                    2, 0.25, 0.3, 0.25, 0.01);
        }
    }

    @Override
    protected void die(final DamageSource source) {
        super.die(source);
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        // ostatni oddech: plomien + podpalenie sprawcy. Bez tego feniks bylby
        // "kurkiem z piorami", a to psuje cene rdzenia
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                this.getX(), this.getY() + 0.5, this.getZ(),
                32, 0.7, 0.5, 0.7, 0.05);
        this.playSound(SoundEvents.BLAZE_AMBIENT, 1.0F, 0.7F);
        final Entity attacker = source.getCausingEntity();
        if (attacker instanceof LivingEntity living && living.isAlive()) {
            living.setSecondsOnFire(4);
        }
    }

    /** Feniks nie ucieka w gre - to mob nadajacy sie do chowu w klatce. */
    @Override
    public boolean removeWhenFarAway(final double distanceSquared) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.BLAZE_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(final DamageSource source) {
        return SoundEvents.BLAZE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PARROT_DEATH;
    }
}
