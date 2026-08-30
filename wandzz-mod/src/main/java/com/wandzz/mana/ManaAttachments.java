package com.wandzz.mana;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.ResourceLocation;

/**
 * Rejestracja many jako Fabric Data Attachment - dziala tak jak "capability"
 * ze starszych wersji, ale bez potrzeby wlasnego systemu synchronizacji/NBT.
 */
public final class ManaAttachments {

    public static final AttachmentType<ManaComponent> MANA = AttachmentRegistry.<ManaComponent>builder()
            .persistent(ManaComponent.CODEC)
            .initializer(ManaComponent::full)
            .buildAndRegister(ResourceLocation.fromNamespaceAndPath("wandzz", "mana"));

    private ManaAttachments() {
    }
}
