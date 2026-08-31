package com.wandzz.entity;

import com.mojang.serialization.Codec;
import com.wandzz.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Jednorozec - jedyne zrodlo wlosa, ktory razem z osmioma piormi daje
 * {@code wandzz:core_feather} (patrz data/wandzz/recipe/core_feather.json).
 *
 * Dlaczego strzyzenie, a nie zabijanie? Vanilla ma na to gotowy interfejs
 * {@link Shearable}: owce strzyze sie przez {@code Items.SHEARS} w
 * {@code mobInteract}, a nie w {@code die}. Ten sam ksztolt daje zrodlo
 * ODNAWIALNE - zabity jednorozec bylby rdzeniem jednorazowym, czyli kara za
 * postepowanie, ktore ma byc nagroda. Wlos od rasta samo (5 minut), wiec stado
 * kolo glady nigdy nie wysycha.
 *
 * 1.21.11 - trzy drobiazgi, ktore kosztowalyby blad kompilacji:
 *  - {@code Shearable#shear} bierze {@code ServerLevel} + {@code SoundSource} +
 *    {@code ItemStack}, a pytanie brzmi {@code readyForShearing()} (nie
 *    {@code canShear()}, jak w starszych mappingach),
 *  - {@code InteractionResult.SUCCESS_SERVER} (nie {@code SUCCESS}): od 1.21.9
 *    wynik z "serwer cos zrobil, klient ma dostac potwierdzenie" nazywa sie inaczej,
 *  - zapis encji idzie przez {@code ValueInput}/{@code ValueOutput} bez
 *    {@code CompoundTag}, a boolean czyta sie {@code getBooleanOr(...)}.
 */
public class Unicorn extends PathfinderMob implements Shearable {

    /** Tyle tyka odrost wlosia (5 minut). Do tego czasu strzyzenie milczy. */
    public static final int HAIR_REGROW_TICKS = 6000;

    private static final EntityDataAccessor<Boolean> DATA_SHEARED =
            SynchedEntityData.defineId(Unicorn.class, EntityDataSerializers.BOOLEAN);

    private int regrowTimer;

    public Unicorn(final EntityType<? extends Unicorn> type, final Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 14.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void defineSynchedData(final SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SHEARED, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // jednorozec nie jest drapieznikiem: ucieka, a oddaje ciosy tylko w obronie
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.5));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F, 0.25F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Override
    protected InteractionResult mobInteract(final Player player, final InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(Items.SHEARS)) {
            return super.mobInteract(player, hand);
        }
        if (this.level() instanceof ServerLevel level) {
            if (this.readyForShearing()) {
                this.shear(level, SoundSource.PLAYERS, stack);
                this.gameEvent(GameEvent.SHEAR, player);
                if (!player.hasInfiniteMaterials()) {
                    stack.hurtAndBreak(1, player, hand);
                }
                return InteractionResult.SUCCESS_SERVER;
            }
            // nozyce weszly w interakcje, ale runo jeszcze nie odroslo: komunikat
            // w action barze, zeby "nic sie nie stalo" nie czytalo sie jako bug
            player.displayClientMessage(Component.translatable("wandzz.unicorn.not_ready"), true);
            return InteractionResult.CONSUME;
        }
        // klient nie ma nic do powiedzenia - dokladnie jak w vanilla Sheep
        return InteractionResult.CONSUME;
    }

    @Override
    public void shear(final ServerLevel level, final SoundSource source, final ItemStack tool) {
        this.playSound(SoundEvents.SHEEP_SHEAR, 1.0F, 1.15F);
        this.entityData.set(DATA_SHEARED, true);
        this.regrowTimer = HAIR_REGROW_TICKS;

        final int count = 1 + this.random.nextInt(2);
        final BlockPos pos = this.blockPosition();
        final ItemEntity drop = new ItemEntity(level,
                pos.getX() + 0.5, this.getY() + 0.5, pos.getZ() + 0.5,
                new ItemStack(ModItems.UNICORN_HAIR, count));
        // krotkie opoznienie: wlos ma wpasc do reki gracza stojacego przy grzbiecie,
        // a nie zostac na ziemi jako kolejna rzecz do podniesienia
        drop.setPickUpDelay(30);
        level.addFreshEntity(drop);
    }

    @Override
    public boolean readyForShearing() {
        return this.isAlive() && !this.isSheared();
    }

    public boolean isSheared() {
        return Boolean.TRUE.equals(this.entityData.get(DATA_SHEARED));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide() && this.regrowTimer > 0 && --this.regrowTimer == 0) {
            this.entityData.set(DATA_SHEARED, false);
        }
    }

    /** Strzyzony jednorozec nie moze zniknac - gracz musi dac mu odrosnac. */
    @Override
    public boolean removeWhenFarAway(final double distanceSquared) {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(final ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("WandzzSheared", this.isSheared());
        output.store("WandzzRegrow", Codec.INT, this.regrowTimer);
    }

    @Override
    protected void readAdditionalSaveData(final ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(DATA_SHEARED, input.getBooleanOr("WandzzSheared", false));
        this.regrowTimer = input.read("WandzzRegrow", Codec.INT).orElse(0);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        // rzenie konia o ton w dol brzmi jak cos duzo wiekszego; jednorozec nie ma
        // wlasnego glosu w tym projekcie i nie potrzebuje
        return SoundEvents.HORSE_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(final DamageSource source) {
        return SoundEvents.HORSE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.HORSE_DEATH;
    }
}
