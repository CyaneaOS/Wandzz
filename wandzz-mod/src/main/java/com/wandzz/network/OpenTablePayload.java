package com.wandzz.network;

import com.wandzz.Wandzz;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * S2C: "otworz okno stolka arcanicznego". Wysylane przez
 * {@code ArcaneTableBlock#useWithoutItem}.
 *
 * Bez pozycji bloku celowo: okno operuje na rozdzce trzymanej w rece, a nie na
 * kontenerze przy bloku. Brak danych w pakiecie = brak menuId/Container, czyli
 * cala obsluga GUI zostaje wlasnym kodem (patrz WandCoreScreen) zamiast
 * MenuType + access widener, ktorego w 1.21.11 nie da sie uzyc bez zmian w buildzie.
 */
public record OpenTablePayload() implements CustomPacketPayload {

    public static final OpenTablePayload INSTANCE = new OpenTablePayload();

    public static final CustomPacketPayload.Type<OpenTablePayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Wandzz.MOD_ID, "open_table"));

    public static final StreamCodec<FriendlyByteBuf, OpenTablePayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
            },
            buf -> INSTANCE
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
