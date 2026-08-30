package com.wandzz.network;

import com.wandzz.Wandzz;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * S2C: aktualny stan many. Konieczny, bo mana jest Fabric Data Attachment na
 * Playerze - takie attachments NIE sa synchronizowane do klienta (vanilla syncuje
 * tylko entity data / crafted attributes), a bez tego HUD nie ma co rysowac.
 *
 * Podwojne double (nie float): wartosc jest ulamkowa (regen co tick = 0.05), a
 * klient ma ja tylko wygladzic, wiec precyzja "paska" nie gra roli.
 */
public record ManaSyncPayload(double current, double max) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ManaSyncPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Wandzz.MOD_ID, "mana_sync"));

    public static final StreamCodec<FriendlyByteBuf, ManaSyncPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeDouble(payload.current());
                buf.writeDouble(payload.max());
            },
            buf -> new ManaSyncPayload(buf.readDouble(), buf.readDouble())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
