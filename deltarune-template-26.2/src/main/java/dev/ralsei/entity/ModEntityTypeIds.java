package dev.ralsei.entity;

import dev.ralsei.Deltarune;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;

public class ModEntityTypeIds {
    public static final ResourceKey<EntityType<?>> RALSEI = create("ralsei");

    private static ResourceKey<EntityType<?>> create(String name) {
        return ResourceKey.create(Registries.ENTITY_TYPE, Deltarune.id(name));
    }
}