package com.wandzz.spell.impl;

import com.wandzz.spell.Spell;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;

/**
 * Nox (poziom 1, 3 many): gasi swiatla postawione przez {@code wandzz:lumos}.
 *
 * <p>Realizacja jest uczciwie prosta: petla po kubikach w okolo rzucajacego
 * (25x13x25, czyli ~8100 odczytow {@code getBlockState}) i {@code removeBlock}
 * tam, gdzie sto {@code minecraft:light}. Zero indeksow, zero wlasnego
 * "rejestru swiatel" - rejestr musialby przezyw restart serwera i edycje
 * blokami (/setblock, inny mod), a {@code BlockState} juz jest tym indeksem.
 * 8100 odczytow raz na zaklecie to uamek w porownaniu z jednym {@code /fill}.
 *
 * <p>Po co wogole ten czar: {@code minecraft:light} ma twardosc -1 (nie da sie
 * go zlamac ani wybic), wiec bez noxa kazde lumos byloby trwalym smieciem w
 * swiecie - zostawaloby tylko wstawienie innego bloku w ten sam kubik, bo light
 * jest {@code replaceable}. Lumos i nox to nie dwa dekoracyjne czary, tylko
 * jedno narzedzie z klawiszem undo.
 */
public final class NoxSpell implements Spell {

    /** Promien sprzatania w osi XZ (25 na 25 kratek) i po 6 kratek w gore i w dol. */
    private static final int PROMIEN = 12;
    private static final int PION = 6;

    @Override
public String id() {
        return "wandzz:nox";
    }

    @Override
    public int requiredLevel() {
        return 1;
    }

    @Override
    public double manaCost() {
        return 3.0;
    }

    @Override
    public boolean canCast(final ServerLevel world, final Player caster) {
        return gaszenie(world, caster, false) > 0;
    }

    @Override
    public void cast(final ServerLevel world, final Player caster) {
        if (gaszenie(world, caster, true) == 0) {
            return;
        }
        world.playSound(null, caster.blockPosition(), SoundEvents.CANDLE_EXTINGUISH, SoundSource.PLAYERS,
                0.9F, 1.3F);
    }

    /**
     * Liczy swiatla w zasiegu; jesli {@code usuwaj}, dodatkowo je zdjmuje.
     * Ten sam przejazd petla jest {@link #canCast} - placa za pudlo ("okolica
     * jest ciemna") byleby nie byla platna.
     */
    private static int gaszenie(final ServerLevel world, final Player caster, final boolean usuwaj) {
        final BlockPos srodek = caster.blockPosition();
        int znalezione = 0;
        for (int dx = -PROMIEN; dx <= PROMIEN; dx++) {
            for (int dz = -PROMIEN; dz <= PROMIEN; dz++) {
                for (int dy = -PION; dy <= PION; dy++) {
                    final BlockPos pos = srodek.offset(dx, dy, dz);
                    if (!world.getBlockState(pos).is(Blocks.LIGHT)) {
                        continue;
                    }
                    znalezione++;
                    if (usuwaj) {
                        world.removeBlock(pos, false);
                    }
                }
            }
        }
        return znalezione;
    }
}
