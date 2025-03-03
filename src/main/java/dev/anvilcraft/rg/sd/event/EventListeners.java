package dev.anvilcraft.rg.sd.event;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.anvilcraft.rg.api.RGValidator;
import dev.anvilcraft.rg.api.event.ServerAboutToStopEvent;
import dev.anvilcraft.rg.api.event.ServerLoadedLevelEvent;
import dev.anvilcraft.rg.sd.SiliconeDolls;
import dev.anvilcraft.rg.sd.SiliconeDollsServerRules;
import dev.anvilcraft.rg.sd.combat.CombatManager;
import dev.anvilcraft.rg.sd.entity.FakePlayer;
import dev.anvilcraft.rg.sd.init.ModCommands;
import dev.anvilcraft.rg.sd.util.IClientMenuTickInjector;
import dev.anvilcraft.rg.sd.util.ClientUtils;
import dev.anvilcraft.rg.sd.tool.FakePlayerResident;
import dev.anvilcraft.rg.sd.tool.PlayerContainer;
import dev.anvilcraft.rg.sd.util.RuleUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

@EventBusSubscriber(modid = SiliconeDolls.MODID)
public class EventListeners {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void registerCommands(@NotNull RegisterCommandsEvent event) {
        ModCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onPlayerTick(@NotNull PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        //noinspection resource
        if (player.level().isClientSide) {
            if (player.containerMenu instanceof IClientMenuTickInjector tick) {
                tick.siliconeDolls$tick();
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerInteract(PlayerInteractEvent.@NotNull EntityInteract event) {
        Player player = event.getEntity();
        if (CombatManager.isLoaded("carryon") && player.isShiftKeyDown() && event.getItemStack().isEmpty()) return;
        Entity entity = event.getTarget();
        //noinspection resource
        if (player.level().isClientSide()) {
            if (entity instanceof Player && ClientUtils.isFakePlayer(player)) {
                event.setCancellationResult(InteractionResult.CONSUME);
            }
        } else if (player instanceof ServerPlayer serverPlayer && entity instanceof ServerPlayer otherPlayer) {
            boolean flag = RGValidator.CommandRuleValidator.hasPermission(() -> SiliconeDollsServerRules.openRealPlayerInventory, player.createCommandSourceStack());
            flag = (entity instanceof FakePlayer fake && !fake.isShadow()) || flag;
            if ((SiliconeDollsServerRules.openFakePlayerInventory || RuleUtils.openFakePlayerEnderChest(player)) && flag) {
                // 打开物品栏
                InteractionResult result = PlayerContainer.openInventory(serverPlayer, otherPlayer);
                if (result != InteractionResult.PASS) {
                    player.stopUsingItem();
                    event.setCancellationResult(result);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.@NotNull PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (SiliconeDollsServerRules.fakePlayerSpawnNoKnockback && player instanceof FakePlayer) {
            // 清除速度
            player.setDeltaMovement(Vec3.ZERO);
            // 清除着火时间
            player.setRemainingFireTicks(0);
            // 清除摔落高度
            player.fallDistance = 0;
        }
    }

    @SubscribeEvent
    public static void onServerStopping(@NotNull ServerAboutToStopEvent event) {
        MinecraftServer server = event.getServer();
        if (SiliconeDollsServerRules.fakePlayerResident) {
            JsonObject fakePlayerList = new JsonObject();
            server.getPlayerList().getPlayers().forEach(player -> {
                if (!(player instanceof FakePlayer)) return;
                if (player.saveWithoutId(new CompoundTag()).contains("rolling_gate.NoResident")) return;
                String username = player.getGameProfile().getName();
                fakePlayerList.add(username, FakePlayerResident.save(player));
            });
            File file = server.getWorldPath(LevelResource.ROOT).resolve("fake_player.rg.json").toFile();
            // 文件不需要存在
            try (BufferedWriter bfw = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
                bfw.write(SiliconeDolls.GSON.toJson(fakePlayerList));
            } catch (IOException e) {
                SiliconeDolls.LOGGER.error(e.getMessage(), e);
            }
        }
    }

    @SubscribeEvent
    public static void onServerLoadedWorlds(@NotNull ServerLoadedLevelEvent event) {
        MinecraftServer server = event.getServer();
        if (SiliconeDollsServerRules.fakePlayerResident) {
            File file = server.getWorldPath(LevelResource.ROOT).resolve("fake_player.rg.json").toFile();
            if (!file.isFile()) {
                return;
            }
            try (BufferedReader bfr = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
                JsonObject fakePlayerList = SiliconeDolls.GSON.fromJson(bfr, JsonObject.class);
                for (Map.Entry<String, JsonElement> entry : fakePlayerList.entrySet()) {
                    FakePlayerResident.load(entry, server);
                }
            } catch (IOException e) {
                SiliconeDolls.LOGGER.error(e.getMessage(), e);
            }
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
    }
}
