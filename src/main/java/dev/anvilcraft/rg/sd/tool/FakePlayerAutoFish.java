package dev.anvilcraft.rg.sd.tool;

import dev.anvilcraft.rg.sd.entity.PlayerActionPack;
import dev.anvilcraft.rg.sd.util.IServerPlayerInjector;
import dev.anvilcraft.rg.tools.PlanFunction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

public class FakePlayerAutoFish {
    public static void autoFish(@NotNull Player player) {
        PlayerActionPack ap = ((IServerPlayerInjector) player).getActionPack();
        if (!(player.level() instanceof ServerLevel level)) return;
        long l = level.getGameTime();
        PlanFunction.addPlan(l + 5, (server) -> ap.start(PlayerActionPack.ActionType.USE, PlayerActionPack.Action.once()));
        PlanFunction.addPlan(l + 15, (server) -> ap.start(PlayerActionPack.ActionType.USE, PlayerActionPack.Action.once()));
    }
}
