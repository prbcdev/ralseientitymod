package dev.ralsei.sound;

import dev.ralsei.Deltarune;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public class ModSounds {
    public static final SoundEvent RALSEI_TALK_BLIP = register("npc.ralsei.talk_blip");

    public static final SoundEvent RALSEI_LULLABY = register("npc.ralsei.lullaby");

    private static SoundEvent register(String name) {
        Identifier id = Deltarune.id(name);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }

    public static void registerModSounds() {
        // no-op, exists to trigger static init -- call from Deltarune.onInitialize
    }
}