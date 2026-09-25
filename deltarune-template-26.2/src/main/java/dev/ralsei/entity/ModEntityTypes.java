package dev.ralsei.entity;

import dev.ralsei.Deltarune;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class ModEntityTypes {

    public static final EntityType<RalseiEntity> RALSEI = register(
            ModEntityTypeIds.RALSEI,
            EntityType.Builder.<RalseiEntity>of(RalseiEntity::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.9f)
    );

    private static <T extends Entity> EntityType<T> register(ResourceKey<EntityType<?>> key, EntityType.Builder<T> builder) {
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
    }

    public static void registerModEntityTypes() {
        Deltarune.LOGGER.info("registering entity types for " + Deltarune.MOD_ID);
    }

    public static void registerAttributes() {
        FabricDefaultAttributeRegistry.register(RALSEI, RalseiEntity.createRalseiAttributes());
    }
}