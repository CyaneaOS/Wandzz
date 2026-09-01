package org.first.wandzz.item;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;


public class ModItems {
    public static final Item WAND = Registry.register(
            Registries.ITEM, Identifier.of("wandzz", "wand"),
                new WandItem(new Item.Settings().registryKey(
                        RegistryKey.of(
                                RegistryKeys.ITEM,
                                Identifier.of("wandzz", "wand")
                        )
                ))
    );

    public static void registerItems() { System.out.println("registering items"); }

}

