package dev.ralsei.item;

import dev.ralsei.entity.ModEntityTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;

import java.util.function.Function;

public class ModItems {

    public static final Item RALSEI_SPAWN_EGG = register(
            ModItemIds.RALSEI_SPAWN_EGG,
            SpawnEggItem::new,
            new Item.Properties().spawnEgg(ModEntityTypes.RALSEI)
    );

    private static Item register(ResourceKey<Item> key, Function<Item.Properties, Item> factory, Item.Properties properties) {
        return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
    }

    public static void registerModItems() {
        // no-op, exists to trigger static init when called from Deltarune.onInitialize
    }
}