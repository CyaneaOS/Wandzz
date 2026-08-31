package com.wandzz.spell.impl;

import com.wandzz.spell.Spell;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Core poziomu 1 - "niszczenie blokow": magiczny odpowiednik kopania.
 *
 * Poprawki wobec pierwszej wersji, ktora "nie dzialala":
 * <ul>
 *   <li><b>zasieg 6 -&gt; 8.</b> 6 blokow to mniej niz dlugosc ramienia z
 *       pierwszoosobowej kamery - gracz patrzyl "na" blok, a {@code pick()} liczyl
 *       od oka i zatrzymyal sie w polowie sciany. 8 to kompromis miedzy "czare
 *       kladzie sie na to, na co patrze" a "nie kopie z drugiego konca swiata".</li>
 *   <li><b>twardosc 3 -&gt; 6.</b> przy 3 odpadaal caly deepslate i wiekszosc rud
 *       (4.5), wiec czar sprawial wrazenie zepsutego wlasnie tam, gdzie gracz
 *       chcial go uzyc. 6 to kamien, ruda zelaza, kwarc, ale NIE obsydian (50)
 *       ani bloki do nich podobne - zostaje granica "czar nie jest kilofem
 *       diamentowym".</li>
 *   <li><b>brak ciszy.</b> Kazda odmowa (pusty cel / istota na drodze / za twardy
 *       / bloku w ogole nie da sie zniszczyc, twardosc &lt; 0) dostaje action bar i
 *       dzwiek. Dawniej wszystkie te case'y konczyly sie cichym {@code return}, a
 *       "nic sie nie stalo" i "nie dziala" to dla gracza to samo.</li>
 *   <li><b>feedback udanego ciecia</b> - pekanie + dzwiek kruszonego bloku, bo
 *       bez tego nawet udany rzut wyglada jak nietrafiony.</li>
 * </ul>
 */
public class BreakBlockSpell implements Spell {

    /** Twardosc powyzej tej granicy zostaje odmowna (obsydian, ancient debris...). */
    private static final double MAX_HARDNESS = 6.0;
    /** Zasieg spojrzenia w blokach. */
    private static final double RANGE = 8.0;

    @Override
    public String id() {
        return "wandzz:break_block";
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
    public void cast(ServerLevel world, Player caster) {
        final HitResult hit = caster.pick(RANGE, 0.0f, false);

        if (!(hit instanceof BlockHitResult blockHit)) {
            tell(caster, hit.getType() == HitResult.Type.MISS
                    ? "wandzz.spell.no_target"
                    : "wandzz.spell.block_protected");
            return;
        }

        final BlockPos pos = blockHit.getBlockPos();
        final BlockState state = world.getBlockState(pos);
        final float hardness = state.getDestroySpeed(world, pos);

        if (hardness < 0.0f) {
            // twardosc ujemna = "nie do zniszczenia" (bedrock, bariera, command block)
            tell(caster, "wandzz.spell.block_protected");
            return;
        }
        if (hardness > MAX_HARDNESS) {
            tell(caster, "wandzz.spell.block_too_hard");
            return;
        }

        // destroyBlock z true = drop jak przy normalnym kopaniu (loot table bloku),
        // a caster jako sprawca - zeby liczyly sie ewentualne zaklecia i kryteria.
        if (!world.destroyBlock(pos, true, caster)) {
            tell(caster, "wandzz.spell.block_protected");
            return;
        }

        final double x = pos.getX() + 0.5;
        final double y = pos.getY() + 0.5;
        final double z = pos.getZ() + 0.5;
        world.sendParticles(ParticleTypes.EXPLOSION, x, y, z, 2, 0.0, 0.0, 0.0, 0.0);
        world.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.5f, 0.8f);
    }

    private static void tell(final Player player, final String key) {
        player.displayClientMessage(Component.translatable(key), true);
    }
}
