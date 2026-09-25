package dev.ralsei.item;

import dev.ralsei.Deltarune;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public class ModItemIds {
    public static final ResourceKey<Item> RALSEI_SPAWN_EGG = create("ralsei_spawn_egg");

    private static ResourceKey<Item> create(String name) {
        return ResourceKey.create(Registries.ITEM, Deltarune.id(name));
    }
}