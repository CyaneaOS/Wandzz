package com.wandzz.entity;

import com.mojang.serialization.Codec;
import com.wandzz.block.ModBlocks;
import com.wandzz.item.ModItems;
import com.wandzz.wand.WandWood;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Duch arkanu ({@code wandzz:arcane_sprite}) - mali lotnik, ktory WISI w
 * koronie drzewa arkanskiego. Wisia = drzewo jest magiczne: to nie dekoracja,
 * tylko widoczny znacznik, ze dane drzewo urodzilo sie z feature'u
 * {@code wandzz:arcane_strangler} (szansa ~1/3 przy generowaniu i przy
 * wzroscie z sadzonki).
 *
 * Zachowanie (wszystko celowo proste, zero nawigacji latajacej):
 *  - startPerching(): zero grawitacji, zero ruchu, brak AI dopuki wisi -
 *    "wisi" jest stanem synchronizowanym, wiec klient moze zlozyc skrzydla;
 *  - obrazenia albo gracz z bronia -> odpiecie sie od korony (spada, potem
 *    wrocic na korone - patrz tryToPerch);
 *  - neutralny: {@link HurtByTargetGoal} sprawia, ze oddaje temu, kto go uderzyl
 *    (2 dmg - tyle co pszczola, sygnal "nie rwij gniazda"), ale pierwszy nie atakuje;
 *  - oswojenie: PPM arcanym patykiem = {@code tamed}. Oswojony nie ucieka, nie
 *    oddaje graczowi, a co {@value #RESIN_PERIOD_TICKS} tickow (ok. 3,3 min)
 *    zrzuca pod soba zywice ({@code wandzz:arcane_resin}) - to jest material do
 *    nasacania rozdzki, patrz {@link com.wandzz.wand.WandData#resinated()}.
 *
 * 1.21.11 - dwie rzeczy, ktore tu zaskakuja: zapis encji NIE idzie przez
 * {@code CompoundTag}, tylko przez {@code ValueInput}/{@code ValueOutput} z
 * {@code Codec}ami, a {@code Entity#hurt} zostal rozbity na
 * {@code hurtServer}/{@code hurtClient}.
 */
public class ArcaneSprite extends PathfinderMob {

    private static final EntityDataAccessor<Boolean> DATA_TAMED =
            SynchedEntityData.defineId(ArcaneSprite.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_PERCHED =
            SynchedEntityData.defineId(ArcaneSprite.class, EntityDataSerializers.BOOLEAN);

    /** Co ile tickow oswojony duch zrzuca zywice. */
    public static final int RESIN_PERIOD_TICKS = 4000;

    /** Zasieg, w ktorym wiszacego ducha trzeba zostawic w spokoju. */
    private static final double DISTURB_RANGE = 2.6;

    private int resinCooldown = RESIN_PERIOD_TICKS;

    public ArcaneSprite(final EntityType<? extends ArcaneSprite> type, final Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.FLYING_SPEED, 0.4);
    }

    @Override
    protected void defineSynchedData(final SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_TAMED, false);
        builder.define(DATA_PERCHED, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // ucieczka tylko dla dzikich - oswojony nie reaguje na gracza
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.4) {
            @Override
            public boolean canUse() {
                return !ArcaneSprite.this.isTamed() && ArcaneSprite.this.getLastHurtByMob() != null
                        && super.canUse();
            }
        });
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, false));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F, 0.25F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    // ------------------------------------------------------------------
    // Wiszenie
    // ------------------------------------------------------------------

    public boolean isPerched() {
        return Boolean.TRUE.equals(this.entityData.get(DATA_PERCHED));
    }

    public boolean isTamed() {
        return Boolean.TRUE.equals(this.entityData.get(DATA_TAMED));
    }

    /** Wolywane przez feature przy spawnie i przez {@link #tick()}, gdy wrocilo spokoj. */
    public void startPerching() {
        this.entityData.set(DATA_PERCHED, true);
        this.setNoGravity(true);
        this.setDeltaMovement(0.0, 0.0, 0.0);
        this.setTarget(null);
    }

    public void stopPerching() {
        this.entityData.set(DATA_PERCHED, false);
        this.setNoGravity(false);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.isPerched()) {
            // pozycja jest NIEZMIENNA (tylko wzrok zyje) - stad brak fizyki i brak
            // dryfu, ktory przy setDeltaMovement(ZERO) i tak by wchodzil w liscie
            this.setNoGravity(true);
            this.setDeltaMovement(0.0, 0.0, 0.0);
            this.fallDistance = 0.0F;
        }
        if (!this.level().isClientSide() && this.isTamed() && this.isPerched()) {
            this.tickResin();
        }
    }

    /** Animacja dla renderera: ledwo wyczuwalny puls skrzydel gdy wisi. */
    public float wingPhase() {
        return Mth.sin(this.tickCount * 0.13F);
    }

    private void tickResin() {
        if (--this.resinCooldown > 0) {
            return;
        }
        this.resinCooldown = RESIN_PERIOD_TICKS;
        if (this.level() instanceof ServerLevel level) {
            final ItemStack resin = new ItemStack(ModItems.ARCANE_RESIN);
            final ItemEntity drop = new ItemEntity(level, this.getX(), this.getY() - 0.4, this.getZ(), resin);
            drop.setPickUpDelay(40);
            level.addFreshEntity(drop);
            this.playSound(SoundEvents.ITEM_PICKUP, 0.6F, 1.4F);
        }
    }

    @Override
    protected void customServerAiStep(final ServerLevel level) {
        super.customServerAiStep(level);

        if (this.isPerched()) {
            final LivingEntity target = this.getTarget();
            if (target != null || this.hurtTime > 0) {
                this.stopPerching();
                return;
            }
            // gracz z bronia w rece, ktory wejdzie w korone -> duch sie odpina
            for (final Player player : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(3.0))) {
                if (!this.isTamed() && !player.isCreative() && player.distanceToSqr(this) < DISTURB_RANGE * DISTURB_RANGE) {
                    this.stopPerching();
                    return;
                }
            }
            return;
        }

        // spokoj -> wraca na korone (inaczej po walce zostalby na ziemi na zawsze)
        if (this.getTarget() == null && this.onGround() && this.random.nextInt(20) == 0) {
            this.tryToPerch(level);
        }
    }

    /** Skok w gore, az nad sobie trafi w liscie arkanskiego drzewa. */
    /**
     * Skok w gore, az nad soba trafi w liscie arkanskiego drzewa. Wolamy lisc z
     * POWIETRZEM pod spodem, bo to jest dolna krawedz korony - tylko tam duh
     * wyglada na wiszacego; srodek korony wyglada na "stoi w drzewie" (wlasnie
     * tak to wczesniej bylo). Gdyby cala korona byla zbita (brzeg zasloniety),
     * bierzemy pierwszy napotkany lisc.
     */
    private void tryToPerch(final ServerLevel level) {
        final BlockPos base = this.blockPosition();
        BlockPos firstAny = null;
        for (int dy = 1; dy <= 7; dy++) {
            final BlockPos pos = base.above(dy);
            if (!level.getBlockState(pos).is(ModBlocks.ARCANE_LEAVES)) {
                continue;
            }
            if (level.getBlockState(pos.below()).isAir()) {
                this.hangUnder(pos);
                return;
            }
            if (firstAny == null) {
                firstAny = pos;
            }
        }
        if (firstAny != null) {
            this.hangUnder(firstAny);
        }
    }

    /** Pozycja "pod dolna krawedzia bloku" + stan wiszenia (bez grawitacji). */
    private void hangUnder(final BlockPos leaf) {
        this.setPos(leaf.getX() + 0.5, leaf.getY() - 0.3, leaf.getZ() + 0.5);
        this.startPerching();
    }
    // ------------------------------------------------------------------
    // Interakcje
    // ------------------------------------------------------------------

    @Override
    public boolean hurtServer(final ServerLevel level, final DamageSource source, final float amount) {
        this.stopPerching();
        final boolean hurt = super.hurtServer(level, source, amount);
        if (hurt && source.getEntity() instanceof Player player && this.isTamed() && !player.isCreative()) {
            // oswojony nie odpowija graczowi - zdjac mu cel
            this.setTarget(null);
        }
        return hurt;
    }

    /**
     * PPM arcanym patykiem = oswojenie (bez przedmiotu w rece duch tylko sie
     * odpienie i zejdzie z drzewa). Dostajesz jedna zywice od reki - to jest
     * nagroda za "zlapanie" drzewa, nie za zabicie go.
     */
    @Override
    public InteractionResult mobInteract(final Player player, final InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(ModItems.stick(WandWood.ARCANE))) {
            return InteractionResult.PASS;
        }
        if (this.isTamed()) {
            if (!this.level().isClientSide()) {
                player.displayClientMessage(Component.translatable("wandzz.sprite.already_tamed"), true);
            }
            return InteractionResult.PASS;
        }
        if (!this.level().isClientSide()) {
            this.entityData.set(DATA_TAMED, true);
            this.setTarget(null);
            this.startPerching();
            if (!player.hasInfiniteMaterials()) {
                stack.shrink(1);
            }
            final ItemStack resin = new ItemStack(ModItems.ARCANE_RESIN);
            if (!player.getInventory().add(resin)) {
                player.drop(resin, false);
            }
            this.playSound(SoundEvents.ALLAY_AMBIENT_WITH_ITEM, 1.0F, 1.3F);
            player.displayClientMessage(Component.translatable("wandzz.sprite.tamed"), true);
        }
        // 1.21.11: InteractionResult to sealed interface recordow - NIE MA w nim
        // sidedSuccess(...), wiec sukces zglaszamy wprost (dokladnie tak robi
        // Parrot#mobInteract dla jedzenia).
        return InteractionResult.SUCCESS;
    }

    @Override
    public SoundEvent getAmbientSound() {
        return SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 240;
    }

    // ------------------------------------------------------------------
    // Trwalosc i reguly spawnu
    // ------------------------------------------------------------------

    /** Duch NIE despawni po odejsciu - inaczy drzewo "przestaje byc magiczne" bez powodu. */
    @Override
    public boolean removeWhenFarAway(final double distanceSquared) {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    /**
     * Naturalny spawn tylko tam, gdzie jest arkanskie drzewo w 3 blokach -
     * inaczej duch roznosilby sie po rowninach. Pozostale powody (rozkaz,
     * przywolanie, nasiono spawnu, feature) przechodza bez sprawdzenia, bo
     * one SAME wiedza, gdzie trafia.
     */
    @Override
    public boolean checkSpawnRules(final LevelAccessor level, final EntitySpawnReason reason) {
        if (reason != EntitySpawnReason.NATURAL) {
            return true;
        }
        return hasArcaneTree(level, this.blockPosition());
    }

    private static boolean hasArcaneTree(final LevelAccessor level, final BlockPos pos) {
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                for (int dy = -2; dy <= 4; dy++) {
                    final BlockPos cursor = pos.offset(dx, dy, dz);
                    if (level.getBlockState(cursor).is(ModBlocks.ARCANE_LEAVES)
                            || level.getBlockState(cursor).is(ModBlocks.ARCANE_LOG)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    protected void addAdditionalSaveData(final ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.store("WandzzTamed", Codec.BOOL, this.isTamed());
        output.store("WandzzPerched", Codec.BOOL, this.isPerched());
        output.store("WandzzResinCooldown", Codec.INT, this.resinCooldown);
    }

    @Override
    protected void readAdditionalSaveData(final ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(DATA_TAMED, input.read("WandzzTamed", Codec.BOOL).orElse(false));
        this.entityData.set(DATA_PERCHED, input.read("WandzzPerched", Codec.BOOL).orElse(false));
        this.resinCooldown = input.read("WandzzResinCooldown", Codec.INT).orElse(RESIN_PERIOD_TICKS);
    }
}
