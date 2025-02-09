package dev.anvilcraft.rg.sd;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import dev.anvilcraft.rg.api.RGAdditional;
import dev.anvilcraft.rg.api.server.ServerRGRuleManager;
import dev.anvilcraft.rg.api.server.TranslationUtil;
import dev.anvilcraft.rg.event.ServerAboutToStopEvent;
import dev.anvilcraft.rg.event.ServerLoadedLevelEvent;
import dev.anvilcraft.rg.sd.entity.FakePlayer;
import dev.anvilcraft.rg.sd.entity.PlayerActionPack;
import dev.anvilcraft.rg.sd.init.ModCommands;
import dev.anvilcraft.rg.sd.util.ClientMenuTick;
import dev.anvilcraft.rg.sd.util.ClientUtils;
import dev.anvilcraft.rg.sd.util.FakePlayerContainer;
import dev.anvilcraft.rg.sd.util.FakePlayerEnderChestContainer;
import dev.anvilcraft.rg.sd.util.FakePlayerInventoryContainer;
import dev.anvilcraft.rg.sd.util.FakePlayerResident;
import dev.anvilcraft.rg.sd.util.RuleUtils;
import dev.anvilcraft.rg.tools.serializer.DimTypeSerializer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

@Mod(SiliconeDolls.MODID)
public class SiliconeDolls implements RGAdditional {
    public static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .registerTypeHierarchyAdapter(ResourceKey.class, new DimTypeSerializer())
        .registerTypeHierarchyAdapter(ResourceLocation.class, new ResourceLocation.Serializer())
        .registerTypeHierarchyAdapter(PlayerActionPack.class, new PlayerActionPack.Serializer())
        .registerTypeHierarchyAdapter(PlayerActionPack.Action.class, new PlayerActionPack.Action.Serializer())
        .create();
    public static final String MODID = "silicone_dolls";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final Map<Long, Collection<Consumer<MinecraftServer>>> PLAN_FUNCTION = new TreeMap<>();
    public static final HashMap<Player, Map.Entry<FakePlayerInventoryContainer, FakePlayerEnderChestContainer>> FAKE_PLAYER_INVENTORY_CONTAINER_MAP = new HashMap<>();

    public SiliconeDolls(@NotNull @SuppressWarnings("unused") IEventBus modEventBus, @NotNull ModContainer modContainer) {
        NeoForge.EVENT_BUS.register(this);
        modContainer.registerExtensionPoint(RGAdditional.class, this);
    }

    @Override
    public void loadServerRules(@NotNull ServerRGRuleManager manager) {
        manager.register(SiliconeDollsServerRules.class);
        TranslationUtil.loadLanguage(SiliconeDolls.class, SiliconeDolls.MODID, "en_us");
        TranslationUtil.loadLanguage(SiliconeDolls.class, SiliconeDolls.MODID, "zh_cn");
    }

    public static void addPlan(long time, Consumer<MinecraftServer> consumer) {
        PLAN_FUNCTION.computeIfAbsent(time, k -> new ArrayList<>()).add(consumer);
    }

    @SubscribeEvent
    public void registerCommands(@NotNull RegisterCommandsEvent event) {
        ModCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerTick(@NotNull ServerTickEvent.Post event) {
        if (PLAN_FUNCTION.isEmpty()) return;
        MinecraftServer server = event.getServer();
        ServerLevel level = server.overworld();
        long time = level.getGameTime();
        List<Long> remove = new ArrayList<>(PLAN_FUNCTION.size());
        for (Map.Entry<Long, Collection<Consumer<MinecraftServer>>> entry : PLAN_FUNCTION.entrySet()) {
            if (entry.getKey() > time) continue;
            remove.add(entry.getKey());
            if (entry.getKey() == time) {
                entry.getValue().forEach(consumer -> consumer.accept(server));
                break;
            }
        }
        remove.forEach(PLAN_FUNCTION::remove);
    }

    @SubscribeEvent
    public void onPlayerTick(@NotNull PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player instanceof FakePlayer fakePlayer && fakePlayer.isAlive()) {
            Map.Entry<FakePlayerInventoryContainer, FakePlayerEnderChestContainer> entry
                = SiliconeDolls.FAKE_PLAYER_INVENTORY_CONTAINER_MAP.get(player);
            entry.getKey().tick();
            entry.getValue().tick();
        } else //noinspection resource
            if (player.level().isClientSide) {
                if (player.containerMenu instanceof ClientMenuTick tick) {
                    tick.siliconeDolls$tick();
                }
            }
    }

    @SubscribeEvent
    public void onPlayerInteract(PlayerInteractEvent.@NotNull EntityInteract event) {
        Player player = event.getEntity();
        Entity entity = event.getTarget();
        //noinspection resource
        if (player.level().isClientSide()) {
            if (entity instanceof Player && ClientUtils.isFakePlayer(player)) {
                event.setCancellationResult(InteractionResult.CONSUME);
            }
        } else {
            if ((SiliconeDollsServerRules.openFakePlayerInventory || RuleUtils.openFakePlayerEnderChest(player)) && entity instanceof FakePlayer fakePlayer) {
                // 打开物品栏
                InteractionResult result = FakePlayerContainer.openInventory(player, fakePlayer);
                if (result != InteractionResult.PASS) {
                    player.stopUsingItem();
                    event.setCancellationResult(result);
                }
            }
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.@NotNull PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        SiliconeDolls.FAKE_PLAYER_INVENTORY_CONTAINER_MAP.put(player, Map.entry(
            new FakePlayerInventoryContainer(player), new FakePlayerEnderChestContainer(player)
        ));
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.@NotNull PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        SiliconeDolls.FAKE_PLAYER_INVENTORY_CONTAINER_MAP.remove(player);
    }

    @SubscribeEvent
    public void onServerStopping(@NotNull ServerAboutToStopEvent event) {
        MinecraftServer server = event.getServer();
        if (SiliconeDollsServerRules.fakePlayerResident) {
            JsonObject fakePlayerList = new JsonObject();
            SiliconeDolls.FAKE_PLAYER_INVENTORY_CONTAINER_MAP.keySet().forEach(player -> {
                if (!(player instanceof FakePlayer)) return;
                if (player.saveWithoutId(new CompoundTag()).contains("rolling_gate.NoResident")) return;
                String username = player.getGameProfile().getName();
                fakePlayerList.add(username, FakePlayerResident.save(player));
            });
            File file = server.getWorldPath(LevelResource.ROOT).resolve("fake_player.rg.json").toFile();
            // 文件不需要存在
            try (BufferedWriter bfw = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
                bfw.write(GSON.toJson(fakePlayerList));
            } catch (IOException e) {
                SiliconeDolls.LOGGER.error(e.getMessage(), e);
            }
        }
        SiliconeDolls.FAKE_PLAYER_INVENTORY_CONTAINER_MAP.clear();
    }

    @SubscribeEvent
    public void onServerLoadedWorlds(@NotNull ServerLoadedLevelEvent event) {
        MinecraftServer server = event.getServer();
        if (SiliconeDollsServerRules.fakePlayerResident) {
            File file = server.getWorldPath(LevelResource.ROOT).resolve("fake_player.rg.json").toFile();
            if (!file.isFile()) {
                return;
            }
            try (BufferedReader bfr = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
                JsonObject fakePlayerList = GSON.fromJson(bfr, JsonObject.class);
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
