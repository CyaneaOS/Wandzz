package com.wandzz.network;

import com.wandzz.Wandzz;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * C2S: pelna (docelowa) lista rdzeni w rozdzce, wysylana przyciskiem
 * "Zatwierdz" w oknie stolika.
 *
 * Celowo "ustaw sklad", a nie "wloz/wyjmij pojedynczy": serwer ma jedna,
 * atomowa operacje do zwalidowania (liczy gniazda i zasob rdzeni w ekwipunku),
 * a klient nie musi sie martwic o kolejnosc ani o zgubione pakiety.
 * Nazwy ida jako stringi (nie ordinaly enuma) - przetrwaja dopisanie nowego
 * CoreType na srodek listy.
 */
public record WandLoadoutPayload(List<String> coreIds) implements CustomPacketPayload {

    /** Tyle gniazd ma najlepsza rozdzka; wiecej klient i tak nie wysle. */
    private static final int MAX_CORES = 8;

    public static final CustomPacketPayload.Type<WandLoadoutPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Wandzz.MOD_ID, "wand_loadout"));

    public WandLoadoutPayload {
        coreIds = List.copyOf(coreIds);
    }

    public static final StreamCodec<FriendlyByteBuf, WandLoadoutPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.coreIds().size());
                for (String id : payload.coreIds()) {
                    buf.writeUtf(id, 64);
                }
            },
            buf -> {
                int count = buf.readVarInt();
                List<String> ids = new ArrayList<>(Math.max(0, Math.min(count, MAX_CORES)));
                for (int i = 0; i < count; i++) {
                    if (i < MAX_CORES) {
                        ids.add(buf.readUtf(64));
                    }
                }
                return new WandLoadoutPayload(ids);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
