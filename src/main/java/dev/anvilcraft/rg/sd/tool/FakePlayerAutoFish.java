package dev.anvilcraft.rg.sd.tool;

import dev.anvilcraft.rg.sd.entity.PlayerActionPack;
import dev.anvilcraft.rg.sd.event.PlanFunctionListeners;
import dev.anvilcraft.rg.sd.util.IServerPlayerInjector;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

public class FakePlayerAutoFish {
    public static void autoFish(@NotNull Player player) {
        PlayerActionPack ap = ((IServerPlayerInjector) player).getActionPack();
        MinecraftServer server1 = player.getServer();
        if (server1 == null) return;
        long l = server1.overworld().getGameTime();
        PlanFunctionListeners.addPlan(l + 5, (server) -> ap.start(PlayerActionPack.ActionType.USE, PlayerActionPack.Action.once()));
        PlanFunctionListeners.addPlan(l + 15, (server) -> ap.start(PlayerActionPack.ActionType.USE, PlayerActionPack.Action.once()));
    }
}
