package dev.anvilcraft.rg.sd.entity;

import com.mojang.authlib.GameProfile;
import dev.anvilcraft.rg.sd.SiliconeDollsServerRules;
import dev.anvilcraft.rg.sd.util.ServerPlayerInjector;
import net.minecraft.core.BlockPos;
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

public class FakePlayer extends ServerPlayer {
    public Runnable fixStartingPosition = () -> {
    };
    private final boolean shadow;

    private FakePlayer(MinecraftServer server, ServerLevel level, GameProfile gameProfile, ClientInformation clientInformation, boolean shadow) {
        super(server, level, gameProfile, clientInformation);
        this.shadow = shadow;
    }

    public static boolean createFake(@NotNull MinecraftServer server, @NotNull String username, @NotNull Vec3 pos, @NotNull Vec2 facing, @NotNull ServerLevel level, @NotNull GameType gameMode) {
        return FakePlayer.createFake(server, username, pos, facing, level, gameMode, (player) -> {
        });
    }

    public static boolean createFake(
        @NotNull MinecraftServer server,
        @NotNull String username,
        @NotNull Vec3 pos,
        @NotNull Vec2 facing,
        @NotNull ServerLevel level,
        @NotNull GameType gamemode,
        @NotNull Consumer<FakePlayer> callback
    ) {
        if (
            !SiliconeDollsServerRules.fakePlayerNamePrefix.equals("#none") &&
                !username.toLowerCase().startsWith(SiliconeDollsServerRules.fakePlayerNamePrefix.toLowerCase())
        ) {
            username = SiliconeDollsServerRules.fakePlayerNamePrefix + username;
        }
        if (
            !SiliconeDollsServerRules.fakePlayerNameSuffix.equals("#none") &&
                !username.toLowerCase().endsWith(SiliconeDollsServerRules.fakePlayerNameSuffix.toLowerCase())
        ) {
            username = username + SiliconeDollsServerRules.fakePlayerNameSuffix;
        }
        GameProfileCache.setUsesAuthentication(false);
        GameProfile gameprofile;
        try {
            GameProfileCache profileCache = server.getProfileCache();
            if (profileCache == null) gameprofile = null;
            else gameprofile = profileCache.get(username).orElse(null); //findByName  .orElse(null)
        } finally {
            GameProfileCache.setUsesAuthentication(server.isDedicatedServer() && server.usesAuthentication());
        }
        if (gameprofile == null) {
            if (!SiliconeDollsServerRules.allowSpawningOfflinePlayers) {
                return false;
            } else {
                gameprofile = new GameProfile(UUIDUtil.createOfflinePlayerUUID(username), username);
            }
        }
        GameProfile finalGP = gameprofile;
        float yaw = facing.y, pitch = facing.x;
        SkullBlockEntity.fetchGameProfile(gameprofile.getName()).thenAcceptAsync(p -> {
            GameProfile current = finalGP;
            if (p.isPresent()) {
                current = p.get();
            }
            FakePlayer instance = new FakePlayer(server, level, current, ClientInformation.createDefault(), false);
            instance.fixStartingPosition = () -> instance.moveTo(pos.x, pos.y, pos.z, yaw, pitch);
            //noinspection deprecation
            server.getPlayerList().placeNewPlayer(new FakeClientConnection(PacketFlow.SERVERBOUND), instance, new CommonListenerCookie(current, 0, instance.clientInformation(), false));
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
        }, server);
        return true;
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
        ((ServerPlayerInjector) playerShadow).getActionPack().copyFrom(((ServerPlayerInjector) player).getActionPack());
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
