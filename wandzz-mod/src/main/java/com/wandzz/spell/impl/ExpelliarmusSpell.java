package com.wandzz.spell.impl;

import com.wandzz.spell.Spell;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Expelliarmus (poziom 3, 20 many): rozbronic i odepchnac.
 *
 * <p>Robi dwie rzeczy: (1) odrzut od rzucajacego - {@code
 * LivingEntity#knockback(1.6, dx, dz)}, czyli mocniej niz uderzenie tarcza,
 * slabiej niz strzal z prochu; (2) jesli ofiara cos trzyma w glownej rece, ten
 * przedmiot zostaje jej WYBITY na ziemie przed nia.
 *
 * <p>Przedmiot leci na ziemie przy OFIERZE, a nie do naszego ekwipunku. To nie
 * jest niedbalstwo - gdyby czar wrzucal lup do taszczy, bylby kradzieza z
 * 20-krotnym zapleczem many zamiast rozbrojeniem, a na mobach dalby sie
 * "farmic" w kolo.
 *
 * <p>Inna swiadoma decyzja: inni gracze NIE sa rozbrajani. Item wypadajacy z
 * reki to jedyny efekt tego moda, ktory realnie potrafi zabic (klucz, mapa,
 * narzedzie, ktorego nie da sie podniesc pod ostrzalem) i ktorego nie da sie
 * odwrocic bez ryzyka. Warunek jest jedna linia ponizej - jesli chcesz grac na
 * zasadzie "pelna PvP", usun {@code || cel instanceof ServerPlayer} i zostaw
 * komunikat dla wlasnego sumienia.
 */
public final class ExpelliarmusSpell implements Spell {

    private static final double RANGE = 14.0;
    private static final double ODZUT = 1.6;
    private static final float OBRAZENIA = 3.0F;

    @Override
    public String id() {
        return "wandzz:expelliarmus";
    }

    @Override
    public int requiredLevel() {
        return 3;
    }

    @Override
    public double manaCost() {
        return 20.0;
    }

    @Override
    public void cast(final ServerLevel world, final Player caster) {
        final HitResult hit = caster.pick(RANGE, 0.0F, false);
        if (!(hit instanceof EntityHitResult entityHit)
                || !(entityHit.getEntity() instanceof LivingEntity cel)
                || cel == caster
                || !cel.isAlive()) {
            tell(caster, "wandzz.spell.no_target");
            return;
        }
        if (cel instanceof ServerPlayer) {
            tell(caster, "wandzz.spell.expelliarmus_no_player");
            return;
        }

        final double dx = cel.getX() - caster.getX();
        final double dz = cel.getZ() - caster.getZ();
        cel.knockback(ODZUT, -dx, -dz);
        cel.hurtServer(world, world.damageSources().magic(), OBRAZENIA);

        final ItemStack trzymany = cel.getItemBySlot(EquipmentSlot.MAINHAND);
        if (!trzymany.isEmpty()) {
            final ItemEntity wyrzucony = new ItemEntity(world, cel.getX(), cel.getY() + 1.0, cel.getZ(),
                    trzymany.copy());
            // wyrzucamy OD siebie, nie do siebie - inaczej czar oddawalby nam lup
            // po 2 tickach i przestalby byc rozbrojeniem
            wyrzucony.setDeltaMovement(dx * 0.2, 0.42, dz * 0.2);
            world.addFreshEntity(wyrzucony);
            cel.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
        world.sendParticles(ParticleTypes.CLOUD, cel.getX(), cel.getY() + 1.1, cel.getZ(),
                14, 0.3, 0.4, 0.3, 0.02);
        caster.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.9F, 1.7F);
    }

    private static void tell(final Player player, final String key) {
        player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.RED), true);
    }
}
