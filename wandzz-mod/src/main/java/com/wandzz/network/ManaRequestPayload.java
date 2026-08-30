package com.wandzz.network;

import com.wandzz.Wandzz;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * C2S: "prosze o aktualny stan many". Wysylane przez klienta przy dolaczeniu do
 * swiata - dzieki temu HUD jest poprawny rowniez po re-spawnie, zmianie wymiaru
 * i ponownym polaczeniu, bez trzymania po stronie serwera zadnego bufora "czy
 * juz wyslalem".
 */
public record ManaRequestPayload() implements CustomPacketPayload {

    public static final ManaRequestPayload INSTANCE = new ManaRequestPayload();

    public static final CustomPacketPayload.Type<ManaRequestPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Wandzz.MOD_ID, "mana_request"));

    /** Pakiet bez pola - kodek tylko czyta/nadpisuje zero bajtow. */
    public static final StreamCodec<FriendlyByteBuf, ManaRequestPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
            },
            buf -> INSTANCE
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
