package dev.ralsei.item;

import dev.ralsei.Deltarune;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class ModCreativeTabs {

    public static final ResourceKey<CreativeModeTab> DELTARUNE_TAB_KEY = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB, Deltarune.id("deltarune_tab")
    );

    public static final CreativeModeTab DELTARUNE_TAB = FabricCreativeModeTab.builder()
            .icon(() -> new ItemStack(ModItems.RALSEI_SPAWN_EGG))
            .title(Component.translatable("creativeTab.deltarune"))
            .displayItems((params, output) -> {
                output.accept(ModItems.RALSEI_SPAWN_EGG);
                // future NPCs/items go here
            })
            .build();

    public static void registerModCreativeTabs() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, DELTARUNE_TAB_KEY, DELTARUNE_TAB);
    }
}