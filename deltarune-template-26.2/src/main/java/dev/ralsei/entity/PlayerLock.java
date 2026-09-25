package dev.ralsei.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;
import java.util.function.BiPredicate;

public class PlayerLock {
    private UUID ownerId = null;

    public boolean tryAcquire(ServerPlayer player) {
        if (ownerId != null && !ownerId.equals(player.getUUID())) {
            return false;
        }
        ownerId = player.getUUID();
        return true;
    }

    public void release(UUID requestingPlayerId) {
        if (ownerId != null && ownerId.equals(requestingPlayerId)) {
            ownerId = null;
        }
    }

    public void forceRelease() {
        ownerId = null;
    }

    public boolean isOwned() {
        return ownerId != null;
    }

    public ServerPlayer getOwner(ServerLevel level) {
        return ownerId == null ? null : level.getServer().getPlayerList().getPlayer(ownerId);
    }

    public ServerPlayer validate(ServerLevel level, BiPredicate<ServerLevel, ServerPlayer> stillValid) {
        if (ownerId == null) {
            return null;
        }
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(ownerId);
        boolean valid = player != null && stillValid.test(level, player);
        if (!valid) {
            forceRelease();
            return player;
        }
        return null;
    }
}