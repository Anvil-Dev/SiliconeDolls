package dev.anvilcraft.rg.sd.event;

import dev.anvilcraft.rg.sd.SiliconeDolls;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

@EventBusSubscriber(modid = SiliconeDolls.MODID)
public class PlanFunctionListeners {
    public static final Map<Long, Collection<Consumer<MinecraftServer>>> PLAN_FUNCTION = new TreeMap<>();

    public static void addPlan(long time, Consumer<MinecraftServer> consumer) {
        PlanFunctionListeners.PLAN_FUNCTION.computeIfAbsent(time, k -> new ArrayList<>()).add(consumer);
    }

    @SubscribeEvent
    public static void onServerTick(@NotNull ServerTickEvent.Post event) {
        if (PlanFunctionListeners.PLAN_FUNCTION.isEmpty()) return;
        MinecraftServer server = event.getServer();
        ServerLevel level = server.overworld();
        long time = level.getGameTime();
        List<Long> remove = new ArrayList<>(PlanFunctionListeners.PLAN_FUNCTION.size());
        for (Map.Entry<Long, Collection<Consumer<MinecraftServer>>> entry : PlanFunctionListeners.PLAN_FUNCTION.entrySet()) {
            if (entry.getKey() > time) continue;
            remove.add(entry.getKey());
            if (entry.getKey() == time) {
                entry.getValue().forEach(consumer -> consumer.accept(server));
                break;
            }
        }
        remove.forEach(PlanFunctionListeners.PLAN_FUNCTION::remove);
    }
}
