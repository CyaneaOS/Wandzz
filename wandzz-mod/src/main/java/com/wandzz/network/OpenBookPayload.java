package com.wandzz.network;

import com.wandzz.Wandzz;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * S2C: "otworz ksiezke zaklec". Wysylane przez {@code SpellBookItem#use}.
 *
 * Dokladnie ten sam schemat co {@link OpenTablePayload} (i z tych samych
 * wzgledow): to serwer decyduje, ze gracz moze otworzyc okno, a klient tylko
 * rysuje. Pakiet jest pusty, bo ksiezka nie ma zadnego stanu - lista zaklec
 * jest po stronie klienta w {@code SpellRegistry} (common entrypoint rejestruje
 * ja rowniez na kliencie).
 */
public record OpenBookPayload() implements CustomPacketPayload {

    public static final OpenBookPayload INSTANCE = new OpenBookPayload();

    public static final CustomPacketPayload.Type<OpenBookPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Wandzz.MOD_ID, "open_book"));

    public static final StreamCodec<FriendlyByteBuf, OpenBookPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
            },
            buf -> INSTANCE
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
