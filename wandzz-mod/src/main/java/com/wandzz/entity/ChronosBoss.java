package com.wandzz.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Chronos - boss oltarza w Arkanum, jedyne zrodlo {@code wandzz:core_chronos}
 * (rdzen poziomu 4: od niego zalezy, czy wogole da sie rzucic cos drozszego).
 *
 * Pasek zycia bossa: w 1.21.11 NIE ma "flagi boss" przy encji. Vanilla robi to
 * przez {@code ServerBossEvent} trzymany w encji + dwa nadpisania
 * ({@code startSeenByPlayer}/{@code stopSeenByPlayer}), dokladnie jak
 * {@code WitherBoss}. Latwiej to przepisac niz wymyslac, wiec przepisalem.
 *
 * Pelen opis (dlaczego brak tu struktury z {@code /locate}) jest w README, punkt
 * "Chronos". W skrocie: wlasny {@code worldgen/structure} wymagalyby szablonu
 * NBT, a te pliki sa binarne i nie do recenzji w gicie - oltarz stawia wiec
 * feature ({@code ChronosAltarFeature}), ktory istnieje, jest JSON-em i nie
 * potrzebuje mixinow.
 */
public class ChronosBoss extends PathfinderMob {

    private final ServerBossEvent bossEvent = (ServerBossEvent) new ServerBossEvent(
            this.getDisplayName(), BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.NOTCHED_10)
            .setDarkenScreen(true);

    public ChronosBoss(final EntityType<? extends ChronosBoss> type, final Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 120.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.ARMOR, 8.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 40.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // trzeciego argumentu nie wymyslilem: MeleeAttackGoal moba "nie bije w reke"
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, false));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F, 0.1F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Override
    protected void customServerAiStep(final ServerLevel level) {
        super.customServerAiStep(level);
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
    }

    @Override
    public void startSeenByPlayer(final ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(final ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void die(final DamageSource source) {
        super.die(source);
        if (this.level() instanceof ServerLevel level) {
            // boss, ktory umiera bez sladu, odbiera nagrode polowe frajdy;
            // komunikat idzie do wszystkich w wymiarze, nie tylko do zabojcy
            level.players().forEach(p -> p.displayClientMessage(
                    Component.translatable("wandzz.chronos.defeated"), false));
        }
    }

    /** Oltarz jest jeden na kolumne - boss nie moze zniknac, kiedy gracz wrocil po ekwipunek. */
    @Override
    public boolean removeWhenFarAway(final double distanceSquared) {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        // dzwon amethystu zapetlony jako "mowa" boga czasu - wlasnych glosow nie ma
        // w tym projekcie, a cisza przy bossie wygladalaby na blad
        return SoundEvents.AMETHYST_BLOCK_CHIME;
    }

    @Override
    protected SoundEvent getHurtSound(final DamageSource source) {
        return SoundEvents.BLAZE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BLAZE_DEATH;
    }
}
