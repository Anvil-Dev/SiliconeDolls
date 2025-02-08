package dev.anvilcraft.rg.sd.util;

import dev.anvilcraft.rg.sd.entity.FakePlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class ClientUtils {
    private static @Nullable PlayerInfo getPlayerInfo(UUID uuid) {
        if (Minecraft.getInstance().getConnection() != null) {
            return Minecraft.getInstance().getConnection().getPlayerInfo(uuid);
        }
        return null;
    }

    public static boolean isFakePlayer(@NotNull Player player) {
        //noinspection resource
        if (player.level().isClientSide()) {
            PlayerInfo info = getPlayerInfo(player.getUUID());
            return info != null && info.getLatency() == 0;
        }
        return player instanceof FakePlayer;
    }
}
