package com.wandzz.network;

import com.wandzz.Wandzz;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Wysylane z klienta do serwera po zakonczeniu rysowania gestu i pozytywnym
 * rozpoznaniu przez $1 (klient rozpoznaje ksztalt, ale SERWER jest zrodlem
 * prawdy co do wymagan core'ow/many - patrz CastingHandler).
 *
 * Minecraft 1.21.11: id pakietu to {@code Identifier} (dawniej
 * ResourceLocation) i wymaga rejestracji w {@code PayloadTypeRegistry.playC2S()}
 * po obu stronach, PRZED zarejestrowaniem odbiornika.
 */
public record CastPayload(String spellId) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<CastPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Wandzz.MOD_ID, "cast"));

    public static final StreamCodec<FriendlyByteBuf, CastPayload> CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeUtf(payload.spellId()),
            buf -> new CastPayload(buf.readUtf())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
