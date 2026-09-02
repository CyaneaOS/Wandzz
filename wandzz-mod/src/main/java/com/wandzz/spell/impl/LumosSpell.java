package com.wandzz.spell.impl;

import com.wandzz.spell.Spell;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Lumos (poziom 1, 4 many): latarnia bez latarni.
 *
 * <p>Czar kladzie {@code minecraft:light} - blok, ktory vanilla sama definiuje
 * jako "swiatlo bez geometrii" (bez kolizji, {@code replaceable}, nie do zniszczenia
 * reka: {@code strength(-1)}). To nie jest nasz blok ani nasza encja, wiec:
 * zero rejestrowania, zero rendererow, swiatlo jest widoczne dla kazdego klienta
 * (blok idzie w standardowym update'sie chunka), a woda/snieg go nie wypycha.
 *
 * <p>Wybor "blok w swiecie" zamiast "efekt na graczu" jest swiadomy: efekt z
 * wlasnym swiatlem nie istnieje w vanilla (latarka = {@code Item} z NBT albo
 * tytul zaklecia "glowing" - nic z tego nie oswietla terenu), a gracz z pochodnia
 * w rece i tak gubi ja przy szybkim biegu. Tutaj swiatlo zostaje tam, gdzie je
 * postawiles - pod sufitem, w tunelu, na slupie.
 *
 * <p>Kontruje je {@code wandzz:nox}. {@code minecraft:light} ma twardosc -1, wiec
 * NIE da sie go wybic palosciami - nox jest jedynym "oficjalnym" sposobem usuniecia
 * (poza wstawieniem w ten sam kubik jakiegokolwiek innego bloku, bo light jest
 * {@code replaceable}).
 */
public final class LumosSpell implements Spell {

    /** Zasieg rzucenia - tyle co "na co patrze", bez strzelania swiatlem przez mape. */
    private static final double RANGE = 12.0;

    @Override
    public String id() {
        return "wandzz:lumos";
    }

    @Override
    public int requiredLevel() {
        return 1;
    }

    @Override
    public double manaCost() {
        return 4.0;
    }

    @Override
    public boolean canCast(final ServerLevel world, final Player caster) {
        return slot(world, caster) != null;
    }

    @Override
    public void cast(final ServerLevel world, final Player caster) {
        final BlockPos pos = slot(world, caster);
        if (pos == null) {
            return;
        }
        // LEVEL 15 = pelna jasnoca (domyslny stan LightBlock i tak ma 15 -
        // setValue jest tu po to, zeby czytelnik nie musial ufac domyslkom).
        world.setBlock(pos, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, LightBlock.MAX_LEVEL),
                Block.UPDATE_ALL);
        world.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.45F, 1.8F);
    }

    /**
     * Kubik, w ktorym moze stanac swiatlo: powietrze na sciance, na ktora patrzy
     * gracz (nie "w" niej - inaczej czar wybijalby dziury w scianach i kradl
     * bloki). {@code null} = nie ma gdzie postawic, wiec {@link #canCast} zwraca
     * false i mana zostaje w kieszeni.
     */
    private static BlockPos slot(final ServerLevel world, final Player caster) {
        if (!(caster.pick(RANGE, 0.0F, false) instanceof BlockHitResult hit)) {
            return null;
        }
        final BlockPos pos = hit.getBlockPos().relative(hit.getDirection());
        final BlockState stan = world.getBlockState(pos);
        // tylko powietrze: snieg i woda sa "replaceable", ale wkladanie w nie
        // swiatla to zachowanie, ktorego gracz by nie przewidzial
        return stan.isAir() ? pos : null;
    }
}
