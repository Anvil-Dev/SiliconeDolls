package dev.anvilcraft.rg.sd.tool;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import dev.anvilcraft.rg.sd.SiliconeDolls;
import dev.anvilcraft.rg.sd.SiliconeDollsServerRules;
import dev.anvilcraft.rg.sd.entity.FakeClientConnection;
import dev.anvilcraft.rg.sd.entity.FakePlayer;
import dev.anvilcraft.rg.sd.entity.PlayerActionPack;
import dev.anvilcraft.rg.sd.mixin.EntityInvoker;
import dev.anvilcraft.rg.sd.mixin.PlayerAccessor;
import dev.anvilcraft.rg.sd.util.IServerPlayerInjector;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class FakePlayerResident {
    public static @NotNull JsonObject save(Player player) {
        JsonObject fakePlayer = new JsonObject();
        if (SiliconeDollsServerRules.fakePlayerReloadAction) {
            PlayerActionPack actionPack = ((IServerPlayerInjector) player).getActionPack();
            fakePlayer.add("actions", SiliconeDolls.GSON.toJsonTree(actionPack));
        }
        return fakePlayer;
    }

    public static void createFake(String username, @NotNull MinecraftServer server, final JsonObject actions) {
        GameProfileCache.setUsesAuthentication(false);
        GameProfile gameprofile;
        try {
            GameProfileCache profileCache = server.getProfileCache();
            if (profileCache == null) {
                return;
            }
            gameprofile = profileCache.get(username).orElse(null);
        } finally {
            GameProfileCache.setUsesAuthentication(server.isDedicatedServer() && server.usesAuthentication());
        }
        if (gameprofile == null) {
            if (!SiliconeDollsServerRules.allowSpawningOfflinePlayers) {
                SiliconeDolls.LOGGER.error("Spawning offline players {} is not allowed!", username);
                return;
            }
            gameprofile = new GameProfile(UUIDUtil.createOfflinePlayerUUID(username), username);
        }
        GameProfile finalGameprofile = gameprofile;
        SkullBlockEntity.fetchGameProfile(gameprofile.getName()).thenAcceptAsync((p) -> {
            GameProfile current = finalGameprofile;
            if (p.isPresent()) {
                current = p.get();
            }
            FakePlayer playerMPFake = FakePlayer.create(server, server.overworld(), current, ClientInformation.createDefault(), false);
            //noinspection deprecation
            server.getPlayerList().placeNewPlayer(new FakeClientConnection(PacketFlow.SERVERBOUND), playerMPFake,
                new CommonListenerCookie(current, 0, playerMPFake.clientInformation(), false));
            playerMPFake.setHealth(20.0F);
            AttributeInstance attribute = playerMPFake.getAttribute(Attributes.STEP_HEIGHT);
            if (attribute != null) attribute.setBaseValue(0.6F);
            @SuppressWarnings("resource") ServerLevel level = playerMPFake.serverLevel();
            server.getPlayerList().broadcastAll(new ClientboundRotateHeadPacket(playerMPFake, ((byte) (playerMPFake.yHeadRot * 256.0F / 360.0F))), level.dimension());
            server.getPlayerList().broadcastAll(new ClientboundTeleportEntityPacket(playerMPFake), level.dimension());
            playerMPFake.getEntityData().set(PlayerAccessor.getCustomisationData(), (byte) 127);
            PlayerActionPack actionPack = SiliconeDolls.GSON.fromJson(actions, PlayerActionPack.class);
            ((IServerPlayerInjector) playerMPFake).getActionPack().copyFrom(actionPack);
            ((EntityInvoker) playerMPFake).invokerUnsetRemoved();
        }, server);
    }

    public static void load(Map.@NotNull Entry<String, JsonElement> entry, MinecraftServer server) {
        String username = entry.getKey();
        JsonObject fakePlayer = entry.getValue().getAsJsonObject();
        JsonObject actions = new JsonObject();
        if (SiliconeDollsServerRules.fakePlayerReloadAction && fakePlayer.has("actions")) {
            actions = fakePlayer.get("actions").getAsJsonObject();
        }
        FakePlayerResident.createFake(username, server, actions);
    }
}
