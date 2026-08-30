package com.wandzz.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Wysylane z klienta do serwera po zakonczeniu rysowania gestu i pozytywnym
 * rozpoznaniu przez $1 (klient rozpoznaje ksztalt, ale SERWER jest zrodlem
 * prawdy co do wymagan core'ow/many - patrz CastingHandler).
 */
public record CastPayload(String spellId) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<CastPayload> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("wandzz", "cast"));

    public static final StreamCodec<FriendlyByteBuf, CastPayload> CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeUtf(payload.spellId()),
            buf -> new CastPayload(buf.readUtf())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
