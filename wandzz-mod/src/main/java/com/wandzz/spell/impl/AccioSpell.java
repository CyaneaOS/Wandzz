package com.wandzz.spell.impl;

import com.wandzz.spell.Spell;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Accio (poziom 2, 8 many): wszystko, co lezy, leci do Ciebie.
 *
 * <p>Czar NIE wkada itemow do ekwipunku sila - dostaje oni predkosc w strone
 * klatki piersiowej rzucajacego, a zbiera ich vanilla ({@code ItemEntity} plus
 * styk z bounding boxem gracza). Trzy powody, dla ktorych to jest lepsze niz
 * {@code inventory.add(stack)}:
 * <ol>
 *   <li>pelny pasek nie kasuje niczego - itemy wpadaja z powrotem pod nogi,</li>
 *   <li>stacki z NBT/foilami/komponentami przechodz te sama sciezke co przy
 *       normalnym podnoszeniu, wiec nie da sie zgubic ani zdublowac zawartosci,</li>
 *   <li>itemy lezace w wodzie, na ogniu albo cudzym claimie nie musza miec dla
 *       nas wyjatkow.</li>
 * </ol>
 *
 * <p>Przedmiot swiezo zrzucony (np. loot po zabitum mobie) ma 10 tickow opoznienia
 * odbioru - wpadnie do reki cile pozniej, bo predkosc nadajemy i tak. To jest
 * zachowanie vanilla, nie blad: bez niego czar dalby sie uzyc do odbierania dropu
 * innemu graczowi w tej samej klatce, w ktorej ten go zgubil.
 */
public final class AccioSpell implements Spell {

    /** Promien "ssania", dobrze ponizej limitow synchronizacji encji (patrz RevealSpell). */
    private static final double RADIUS = 24.0;
    private static final double PREDKOSC = 0.9;
    private static final double PODNIESIENIE = 0.28;

    @Override
    public String id() {
        return "wandzz:accio";
    }

    @Override
    public int requiredLevel() {
        return 2;
    }

    @Override
    public double manaCost() {
        return 8.0;
    }

    @Override
    public boolean canCast(final ServerLevel world, final Player caster) {
        return !itemy(world, caster).isEmpty();
    }

    @Override
    public void cast(final ServerLevel world, final Player caster) {
        final Vec3 cel = caster.position().add(0.0, 0.6, 0.0);
        for (final ItemEntity item : itemy(world, caster)) {
            final Vec3 doCelu = cel.subtract(item.position()).normalize().scale(PREDKOSC);
            item.setDeltaMovement(doCelu.x, doCelu.y + PODNIESIENIE, doCelu.z);
            // 0,1 w gore, zeby item wpasowany w snieg/trawe/meble nie zaczepil
            // sie o krawedz bloku zamiast wystartowac
            item.setPos(item.getX(), item.getY() + 0.1, item.getZ());
        }
        caster.playSound(SoundEvents.ITEM_PICKUP, 0.6F, 1.5F);
    }

    /** Itemy w kuli wokolo gracza. */
    private static List<ItemEntity> itemy(final ServerLevel world, final Player caster) {
        final AABB box = caster.getBoundingBox().inflate(RADIUS, RADIUS * 0.5, RADIUS);
        return world.getEntitiesOfClass(ItemEntity.class, box, item -> !item.isRemoved());
    }
}
