package com.wandzz.spell.impl;

import com.wandzz.core.CoreType;
import com.wandzz.spell.Spell;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.portal.TeleportTransition;

/**
 * Dragon Breath Core (lvl 3) - teleportacja w zasiegu spojrzenia.
 *
 * Dwie rzeczy, ktore sprawialy, ze "nie dzialalo":
 *
 * <ol>
 *   <li><b>Cel bywal wewnatrz bloku.</b> Dawniej lecialo {@code hit.getLocation()},
 *       czyli punkt na POWIERZCHNI trafionej sciany - przelozony o ulamek bloka do
 *       wewnatrz, gracz ladowal w bryle, a vanilla nie wpuszcza gracza w kolizje.
 *       Teraz celem jest powietrze PRZED trafiona sciana ( {@code pos.relative(face)})
 *       plus krotki skan w gore, zeby wejsc na polke albo skarpke.</li>
 *   <li><b>Zly wejscie teleportu.</b> {@code Player#teleportTo(ServerLevel, ...)} to
 *       sciezka {@code @Deprecated}, ktora dla gracza na serwerze w 1.21.11 jest
 *       obsluzona polowicznie (istnieje, nie rzuca bledu, nie przenosi). Wlasciwa
 *       metoda to {@code Entity#teleport(TeleportTransition)} - dokladnie ta, ktorej
 *       uz{@code GateService#travel} i ktora dziala (brama archaniczna chodzi).</li>
 * </ol>
 *
 * Dodatkowo kazde odmowienie dostaje komunikat: bez tego "czar nie dziala" i
 * "nie patrzysz w cel" wygladaja identycznie.
 */
public class TeleportSpell implements Spell {

    /** Zasieg spojrzenia - tyle co {@code OpenGateSpell}, zeby gesty nie roznilly sie "na oko". */
    private static final double MAX_RANGE = 20.0;
    /** Ile blokow w gore szukamy wolnego miejsca, jesli cel jest przy podlodze. */
    private static final int RISE_SCAN = 3;

    @Override
    public String id() {
        return "wandzz:teleport";
    }

    @Override
    public int requiredLevel() {
        return 3;
    }

    @Override
    public double manaCost() {
        return 15.0;
    }

    @Override
    public boolean isProvidedBy(CoreType core) {
        // Zaklecie dostepne dla KAZDEGO core'a poziomu 3+, nie tylko Dragon Breath.
        return core.level() >= 3;
    }

    @Override
    public void cast(ServerLevel world, Player caster) {
        HitResult hit = caster.pick(MAX_RANGE, 0.0f, false);
        if (hit.getType() == HitResult.Type.MISS) {
            tell(caster, "wandzz.spell.no_target");
            return;
        }
        if (!(hit instanceof BlockHitResult blockHit)) {
            // trafilismy w istote - teleport "przez" cos jest ta sama kolizja co wyzej
            tell(caster, "wandzz.spell.no_target");
            return;
        }

        final BlockPos standing = findStandingSpot(world, blockHit);
        if (standing == null) {
            tell(caster, "wandzz.spell.teleport_blocked");
            return;
        }

        caster.teleport(new TeleportTransition(
                world,
                Vec3.atBottomCenterOf(standing),
                Vec3.ZERO,
                caster.getYRot(),
                caster.getXRot(),
                TeleportTransition.PLAY_PORTAL_SOUND.then(TeleportTransition.PLACE_PORTAL_TICKET)));

        world.sendParticles(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL,
                standing.getX() + 0.5, standing.getY() + 0.6, standing.getZ() + 0.5,
                24, 0.35, 0.5, 0.35, 0.04);
        world.playSound(null, standing, SoundEvents.PORTAL_TRIGGER, SoundSource.PLAYERS, 0.5f, 1.4f);
    }

    /**
     * Powietrze przed trafiona sciana, a jesli tam jest kolizja - dwa bloki wyzej
     * (polki, skarpy, wejscia do jaskin). {@code null} = nigdzie nie ma miejsca i
     * nie probujemy na sile: lepiej odmowic czaru niz wmurowac gracza.
     */
    private static BlockPos findStandingSpot(final ServerLevel world, final BlockHitResult hit) {
        final BlockPos base = hit.getBlockPos().relative(hit.getDirection());
        for (int rise = 0; rise <= RISE_SCAN; rise++) {
            final BlockPos cand = base.above(rise);
            if (isFreeSpot(world, cand)) {
                return cand;
            }
        }
        return null;
    }

    private static boolean isFreeSpot(final ServerLevel world, final BlockPos pos) {
        final BlockState state = world.getBlockState(pos);
        if (!state.isAir() && !state.blocksMotion()) {
            return false;
        }
        // dwa bloki wysokosci gracza: stopa i glowa, inaczej wyjdzimy "przez sufit"
        final BlockState head = world.getBlockState(pos.above());
        return !head.blocksMotion() || head.isAir();
    }

    private static void tell(final Player player, final String key) {
        player.displayClientMessage(Component.translatable(key), true);
    }
}
