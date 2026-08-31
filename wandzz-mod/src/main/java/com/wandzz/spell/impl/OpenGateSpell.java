package com.wandzz.spell.impl;

import com.wandzz.block.ArcaneEmberBlock;
import com.wandzz.block.ModBlocks;
import com.wandzz.spell.Spell;
import com.wandzz.world.GateService;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.Nullable;

/**
 * Otwieranie bramy do Arkanum - jedyne zaklecie z moda, ktore dziala NA blok,
 * a nie "w powietrze": trzeba patrzec na Arkanny Zar (wandzz:arcane_ember),
 * ktory generuje sie w jeziorach lawy w podziemiach.
 *
 * Przebieg:
 *   gest "brama" (luk: sciana - ledweg - sciana) -> canCast (cel = zimny zar)
 *   -> placenie many (40) -> GateService.ignite -> zar zapala sie, a po stronie
 *   Arkanum powstaje (albo jest odnajdywana) brama powrotna
 *
 * Po co canCast, skoro cast i tak to sprawdza: CastingHandler pobiera mane PRZED
 * efektem. Bez tego sprawdzenia pudlo (gest w sciane) kosztowaloby polowe paska
 * many i gracz nie wiedzialby, czy zaklecie w ogole probowilo dzialac.
 */
public class OpenGateSpell implements Spell {

    /** Ten sam zasieg co BreakBlockSpell: "na co patrze, to celuje". */
    private static final double REACH = 6.0;

    @Override
    public String id() {
        return "wandzz:open_gate";
    }

    @Override
    public int requiredLevel() {
        return 3;
    }

    @Override
    public double manaCost() {
        return 40.0;
    }

    @Override
    public boolean canCast(final ServerLevel world, final Player caster) {
        return target(world, caster) != null;
    }

    @Override
    public void cast(final ServerLevel world, final Player caster) {
        BlockPos pos = target(world, caster);
        if (pos == null || !(caster instanceof ServerPlayer player)) {
            return;
        }
        GateService.ignite(world, pos, world.getBlockState(pos), player);
    }

    /**
     * Zimny zar w zasiegu patrzenia, albo null. Komunikat o przyczynie odmowy
     * leci wlasnie stad (action bar), a nie z canCast - obie sciezki musza
     * mowic to samo, a tylko ta zna konkretny powod.
     */
    private static @Nullable BlockPos target(final ServerLevel world, final Player caster) {
        HitResult hit = caster.pick(REACH, 0.0f, false);
        if (!(hit instanceof BlockHitResult blockHit)) {
            return say(caster, "wandzz.gate.need_aim");
        }
        BlockPos pos = blockHit.getBlockPos();
        BlockState state = world.getBlockState(pos);
        if (!state.is(ModBlocks.ARCANE_EMBER)) {
            return say(caster, "wandzz.gate.no_ember");
        }
        if (state.getValue(ArcaneEmberBlock.LIT)) {
            // Zapalona brama nie potrzebuje drugiego zaklecia - przechodzi sie
            // przez nia PPM. Mowimy to wprost, bo inaczej "zaklecie nie dziala".
            return say(caster, "wandzz.gate.lit");
        }
        return pos;
    }

    /** Zwraca zawsze null - wygodne "return say(...)" w kazdym bledzie. */
    private static @Nullable BlockPos say(final Player caster, final String key) {
        caster.displayClientMessage(Component.translatable(key), true);
        return null;
    }
}
