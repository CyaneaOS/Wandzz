package com.wandzz.mana;

import com.wandzz.Wandzz;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

import java.util.Objects;

/**
 * Rejestracja many jako Fabric Data Attachment - dziala tak jak "capability"
 * ze starszych wersji, ale bez potrzeby wlasnego systemu synchronizacji/NBT.
 *
 * Uwaga (Fabric API dla 1.21.11): {@code AttachmentRegistry.builder()} jest
 * oznaczone jako {@code @Deprecated} - wlasciwa sciezka to
 * {@link AttachmentRegistry#create(Identifier, java.util.function.Consumer)}.
 */
public final class ManaAttachments {

    public static final AttachmentType<ManaComponent> MANA = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(Wandzz.MOD_ID, "mana"),
            builder -> builder
                    .persistent(ManaComponent.CODEC)
                    .initializer(ManaComponent::full)
    );

    /**
     * Attachment musi byc zarejestrowany w fazie inicjalizacji moda (przed
     * ladowaniem danych gracza), dlatego dotykamy pole jawnie - bez tego klasa
     * zostalaby zainicjalizowana dopiero przy pierwszym rzucanym zakleciu.
     */
    public static void bootstrap() {
        Objects.requireNonNull(MANA, "mana attachment nie zostal zarejestrowany");
    }

    private ManaAttachments() {
    }
}
