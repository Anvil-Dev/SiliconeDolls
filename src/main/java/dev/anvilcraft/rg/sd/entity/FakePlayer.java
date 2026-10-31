package dev.anvilcraft.rg.sd.entity;

import com.mojang.authlib.GameProfile;
import dev.anvilcraft.rg.sd.SiliconeDolls;
import dev.anvilcraft.rg.sd.SiliconeDollsServerRules;
import dev.anvilcraft.rg.sd.compat.SableCompat;
import dev.anvilcraft.rg.sd.util.IServerPlayerInjector;
import net.minecraft.core.BlockPos;
import net.minecraft.Util;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.RemoteChatSession;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.Locale;
import java.util.concurrent.CompletionException;

public class FakePlayer extends ServerPlayer {
    private static final SpawnPreparation<MinecraftServer> SPAWNS = new SpawnPreparation<>();
    public Runnable fixStartingPosition = () -> {
    };
    private final boolean shadow;

    private FakePlayer(MinecraftServer server, ServerLevel level, GameProfile gameProfile, ClientInformation clientInformation, boolean shadow) {
        super(server, level, gameProfile, clientInformation);
        this.shadow = shadow;
    }

    public static boolean createFake(@NotNull MinecraftServer server, @NotNull String username, @NotNull Vec3 pos, @NotNull Vec2 facing, @NotNull ServerLevel level, @NotNull GameType gameMode, boolean flying) {
        return FakePlayer.createFake(server, username, pos, facing, level, gameMode, flying, (player) -> {
        });
    }

    public static boolean createFake(
        @NotNull MinecraftServer server,
        @NotNull String username,
        @NotNull Vec3 pos,
        @NotNull Vec2 facing,
        @NotNull ServerLevel level,
        @NotNull GameType gamemode,
        boolean flying,
        @NotNull Consumer<FakePlayer> callback
    ) {
        return createFake(server, username, pos, facing, level, gamemode, flying, callback,
            failure -> SiliconeDolls.LOGGER.warn("Fake player preparation failed: {}", username, failure));
    }

    public static boolean createFake(MinecraftServer server, String username, Vec3 pos, Vec2 facing,
        ServerLevel level, GameType gamemode, boolean flying, Consumer<FakePlayer> callback,
        Consumer<Throwable> failed) {
        if (!server.isRunning()) return false;
        var ticket = SPAWNS.begin(server, username);
        if (ticket == null) return false;
        SiliconeDolls.LOGGER.info("SD_SPAWN phase=requested name={}", username);
        GameProfileCache cache = server.getProfileCache();
        boolean allowOffline = SiliconeDollsServerRules.allowSpawningOfflinePlayers;
        float yaw = facing.y, pitch = facing.x;
        var prepared = SpawnPreparation.profile(() -> {
            // Vanilla may contact Mojang here. Never perform it on the server tick thread,
            // and never temporarily change the process-wide authentication policy.
            GameProfile profile = cache == null ? null : cache.get(username).orElse(null);
            if (profile == null && allowOffline) {
                String name = username.toLowerCase(Locale.ROOT);
                profile = new GameProfile(UUIDUtil.createOfflinePlayerUUID(name), name);
            }
            if (profile == null) throw new IllegalStateException("No permitted player profile was found");
            return profile;
        }, Util.backgroundExecutor(), profile -> !profile.getId().equals(UUIDUtil.createOfflinePlayerUUID(profile.getName()))
            && !profile.getProperties().containsKey("textures"), profile ->
            SkullBlockEntity.fetchGameProfile(profile.getId()).handle((skin, error) -> {
                if (error != null) {
                    SiliconeDolls.LOGGER.warn("Skin lookup failed; keeping resolved player identity: {}", username, error);
                    return profile;
                }
                return skin.filter(p -> p.getId().equals(profile.getId())).orElse(profile);
            }));
        prepared.whenComplete((profile, error) -> {
            if (!SPAWNS.current(ticket)) return;
            SiliconeDolls.LOGGER.info("SD_SPAWN phase=prepared name={} elapsedMs={} failed={}", username,
                (System.nanoTime() - ticket.startedNanos()) / 1_000_000L, error != null);
            server.tell(new TickTask(server.getTickCount(), () -> {
                if (!SPAWNS.current(ticket)) return;
                try {
                    if (!server.isRunning() || server.getLevel(level.dimension()) != level) return;
                    if (error != null) {
                        failed.accept(error instanceof CompletionException && error.getCause() != null ? error.getCause() : error);
                        return;
                    }
                    if (server.getPlayerList().getPlayerByName(username) != null
                        || server.getPlayerList().getPlayer(profile.getId()) != null) {
                        failed.accept(new IllegalStateException("A player with this identity is already present"));
                        return;
                    }
                    FakePlayer instance = new FakePlayer(server, level, profile, ClientInformation.createDefault(), false);
                    instance.fixStartingPosition = () -> instance.moveTo(pos.x, pos.y, pos.z, yaw, pitch);
                    //noinspection deprecation
                    server.getPlayerList().placeNewPlayer(new FakeClientConnection(PacketFlow.SERVERBOUND), instance, new CommonListenerCookie(profile, 0, instance.clientInformation(), false));
                    instance.teleportTo(level, pos.x, pos.y, pos.z, yaw, pitch);
                    instance.setHealth(20.0F);
                    instance.unsetRemoved();
                    AttributeInstance attribute = instance.getAttribute(Attributes.STEP_HEIGHT);
                    if (attribute != null) attribute.setBaseValue(0.6F);
                    instance.gameMode.changeGameModeForPlayer(gamemode);
                    server.getPlayerList().broadcastAll(new ClientboundRotateHeadPacket(instance, (byte) (instance.yHeadRot * 256 / 360)), level.dimension());
                    server.getPlayerList().broadcastAll(new ClientboundTeleportEntityPacket(instance), level.dimension());
                    instance.entityData.set(DATA_PLAYER_MODE_CUSTOMISATION, (byte) 0x7f);
                    callback.accept(instance);
                    instance.getAbilities().flying = flying && instance.getAbilities().mayfly;
                    SiliconeDolls.LOGGER.info("SD_SPAWN phase=placed name={} elapsedMs={}", username,
                        (System.nanoTime() - ticket.startedNanos()) / 1_000_000L);
                } catch (Exception failure) {
                    failed.accept(failure);
                } finally {
                    SPAWNS.finish(ticket);
                }
            }));
        });
        return true;
    }

    public static boolean cancelPending(MinecraftServer server, String name) {
        return SPAWNS.cancel(server, name);
    }

    public static void cancelPending(MinecraftServer server) {
        SPAWNS.cancelAll(server);
    }

    @SuppressWarnings("UnusedReturnValue")
    public static @NotNull FakePlayer createShadow(@NotNull ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) throw new IllegalStateException("Server is null");
        server.getPlayerList().remove(player);
        player.connection.disconnect(Component.translatable("multiplayer.disconnect.duplicate_login"));
        ServerLevel worldIn = player.serverLevel();//.getWorld(player.dimension);
        GameProfile gameprofile = player.getGameProfile();
        FakePlayer playerShadow = FakePlayer.create(server, worldIn, gameprofile, player.clientInformation(), true);
        RemoteChatSession session = player.getChatSession();
        if (session != null) playerShadow.setChatSession(session);
        //noinspection deprecation
        server.getPlayerList().placeNewPlayer(new FakeClientConnection(PacketFlow.SERVERBOUND), playerShadow, new CommonListenerCookie(gameprofile, 0, player.clientInformation(), true));
        playerShadow.setHealth(player.getHealth());
        playerShadow.connection.teleport(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        playerShadow.gameMode.changeGameModeForPlayer(player.gameMode.getGameModeForPlayer());
        ((IServerPlayerInjector) playerShadow).getActionPack().copyFrom(((IServerPlayerInjector) player).getActionPack());
        AttributeInstance attribute = playerShadow.getAttribute(Attributes.STEP_HEIGHT);
        if (attribute != null) attribute.setBaseValue(0.6F);
        playerShadow.entityData.set(DATA_PLAYER_MODE_CUSTOMISATION, player.getEntityData().get(DATA_PLAYER_MODE_CUSTOMISATION));
        //noinspection resource
        server.getPlayerList().broadcastAll(new ClientboundRotateHeadPacket(playerShadow, (byte) (player.yHeadRot * 256 / 360)), playerShadow.level().dimension());
        server.getPlayerList().broadcastAll(new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, playerShadow));
        playerShadow.getAbilities().flying = player.getAbilities().flying;
        return playerShadow;
    }

    public static @NotNull FakePlayer create(MinecraftServer server, ServerLevel level, GameProfile gameProfile, ClientInformation clientInformation, boolean shadow) {
        return new FakePlayer(server, level, gameProfile, clientInformation, shadow);
    }

    @SuppressWarnings("unused")
    public boolean isShadow() {
        return shadow;
    }

    @Override
    public void onEquipItem(@NotNull EquipmentSlot slot, @NotNull ItemStack oldItem, @NotNull ItemStack newItem) {
        if (!this.isUsingItem()) super.onEquipItem(slot, oldItem, newItem);
    }

    @Override
    public void kill() {
        kill(Component.literal("Killed"));
    }

    public void kill(@NotNull Component reason) {
        this.shakeOff();
        if (reason.getContents() instanceof TranslatableContents text && text.getKey().equals("multiplayer.disconnect.duplicate_login")) {
            this.connection.onDisconnect(new DisconnectionDetails(reason));
        } else {
            this.server.tell(new TickTask(this.server.getTickCount(), () -> this.connection.onDisconnect(new DisconnectionDetails(reason))));
        }
    }

    @Override
    public void tick() {
        MinecraftServer server1 = this.getServer();
        if (server1 == null) return;
        if (server1.getTickCount() % 10 == 0) {
            this.connection.resetPosition();
            //noinspection resource
            this.serverLevel().getChunkSource().move(this);
        }
        try {
            super.tick();
            this.doTick();
        } catch (NullPointerException ignored) {
        }
        SableCompat.afterFakePlayerTick(this);
    }

    private void shakeOff() {
        if (getVehicle() instanceof Player) stopRiding();
        for (Entity passenger : getIndirectPassengers()) {
            if (passenger instanceof Player) passenger.stopRiding();
        }
    }

    @Override
    public void die(@NotNull DamageSource cause) {
        shakeOff();
        // 清除经验值
        this.setExperiencePoints(0);
        this.setExperienceLevels(0);
        // 清除速度
        this.setDeltaMovement(Vec3.ZERO);
        // 清除着火时间
        this.setRemainingFireTicks(0);
        // 清除摔落高度
        this.fallDistance = 0;
        // 清除药水效果
        this.removeAllEffects();
        super.die(cause);
        setHealth(20);
        this.foodData = new FoodData();
        kill(this.getCombatTracker().getDeathMessage());
    }

    @Override
    public @NotNull String getIpAddress() {
        return "127.0.0.1";
    }

    @Override
    public boolean allowsListing() {
        return SiliconeDollsServerRules.allowListingFakePlayers;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, @NotNull BlockState state, @NotNull BlockPos pos) {
        doCheckFallDamage(0.0, y, 0.0, onGround);
    }

    @Override
    public @NotNull BlockPos getBlockPosBelowThatAffectsMyMovement() {
        if (SableCompat.isPresent()) {
            BlockPos overridden = SableCompat.blockPosBelowThatAffectsMyMovement(this, super.getBlockPosBelowThatAffectsMyMovement());
            if (overridden != null) {
                return overridden;
            }
        }
        return super.getBlockPosBelowThatAffectsMyMovement();
    }

    @Override
    public Entity changeDimension(@NotNull DimensionTransition serverLevel) {
        super.changeDimension(serverLevel);
        if (wonGame) {
            ServerboundClientCommandPacket p = new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN);
            connection.handleClientCommand(p);
        }

        // If above branch was taken, *this* has been removed and replaced, the new instance has been set
        // on 'our' connection (which is now theirs, but we still have a ref).
        if (connection.player.isChangingDimension()) {
            connection.player.hasChangedDimension();
        }
        return connection.player;
    }

}
