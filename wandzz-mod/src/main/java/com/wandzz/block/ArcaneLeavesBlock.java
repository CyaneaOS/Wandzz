package com.wandzz.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.TintedParticleLeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Liscie arkanskiego drzewa, ktore NIE gnia - i to nie dlatego, ze ktos pamietal
 * ustawic wlasciwosc {@code persistent}, tylko dlatego, ze sciezka gnicia jest tu
 * odlaczona na stale.
 *
 * <p>Jak to liczy vanilla (1.21.11, {@link LeavesBlock}):
 * <pre>
 * isRandomlyTicking(state) = state.distance == 7 &amp;&amp; !state.persistent
 * randomTick(state, ...)   = if (decaying(state)) { dropResources; removeBlock }
 * tick(state, ...)         = level.setBlock(pos, updateDistance(state, ...), 3)
 * </pre>
 * czyli serwer w ogole nie pyta bloku o {@code randomTick}, jesli stan nie jest
 * "randomly ticking". Feature kladacy liscie przez {@code LevelWriter#setBlock}
 * nie przechodzi przez {@code onPlace}/{@code updateDistance}, wiec DISTANCE
 * zostaje 7 - i kazdy lisc z {@code persistent=false} (wszystko, co zostalo
 * wygenerowane przed poprawka, kazdy lisc wstawiony przez {@code /setblock},
 * strukture albo inny mod) doczekal random ticka i znikal.
 *
 * <p>Ustawienie {@code persistent=true} przy generowaniu (patrz
 * {@code ArcaneStranglerFeature#leafState}) zamyka te sciezke dla NOWYCH drzew,
 * ale nie naprawia starych chunkow i nie chroni przed zadnym innym zapisem stanu.
 * Dlatego nadpisujemy wejscia gnicia tutaj:
 * <ul>
 *   <li>{@link #isRandomlyTicking(BlockState)} = false - serwer nie wybiera tych
 *       blokow do random tickow (a wiec {@code randomTick} nie ma szans sie
 *       odpalic),</li>
 *   <li>{@link #decaying(BlockState)} = false - drugi pas: nawet wywolane recznie
 *       (np. przez kopiowanie stanu) mowi "nie gnij",</li>
 *   <li>{@link #tick} = no-op - nie przepisujemy stanu przy zmianie sasiadow,
 *       wiec raz polozone liscie nie robia sie "distance 7" pozniej.</li>
 * </ul>
 *
 * <p>Czego to NIE zabija: opadajacych czasteczek lisci. {@code animateTick} jest
 * wywolywany z {@code ClientLevel#doAnimateTick} dla losowych pozycji bez zadnej
 * kontrolki {@code isRandomlyTicking} (sprawdzone w 1.21.11), wiec tint i czastki
 * z {@link TintedParticleLeavesBlock} dzialaja jak dawniej. {@code Properties
 * .randomTicks()} zostalo usuniete - sluzylolo wylacznie do domyslnej
 * implementacji {@code isRandomlyTicking}.
 */
public class ArcaneLeavesBlock extends TintedParticleLeavesBlock {
    /** Szansa opadniecia czasteczki - tyle co vanilla (0,02). */
    private static final float SZANSA_CZASTECZKI = 0.02f;

    public ArcaneLeavesBlock(final Properties properties) {
        super(SZANSA_CZASTECZKI, properties);
    }

    /** Brak random tickow = brak wejsciowej sciezki do gnicia (patrz javadoc). */
    @Override
    protected boolean isRandomlyTicking(final BlockState state) {
        return false;
    }

    /** Drugi pas: jesli ktos i tak wywola, stan "nie gnije". */
    @Override
    protected boolean decaying(final BlockState state) {
        return false;
    }

    /**
     * Vanilla przepisuje tu DISTANCE przy kazdej zmianie sasiada. Nam to nie je
     * potrzebne (i nie chcialbym pozniej "odswiezac" stanu na 7), wiec nie robimy
     * nic - woda jest obsluzona wyzej, w {@code updateShape}.
     */
    @Override
    protected void tick(final BlockState state, final ServerLevel level, final BlockPos pos,
            final RandomSource random) {
    }
}
