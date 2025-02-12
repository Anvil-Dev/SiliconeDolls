package dev.anvilcraft.rg.sd.entity;

import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.RelativeMovement;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class FakePlayerNetHandler extends ServerGamePacketListenerImpl {
    public FakePlayerNetHandler(MinecraftServer server, Connection connection, ServerPlayer player, CommonListenerCookie cookie) {
        super(server, connection, player, cookie);
    }

    @Override
    public void send(final @NotNull Packet<?> packetIn) {
    }

    @Override
    public void disconnect(@NotNull Component message) {
        if (message.getContents() instanceof TranslatableContents text && (text.getKey().equals("multiplayer.disconnect.idling") || text.getKey().equals("multiplayer.disconnect.duplicate_login"))) {
            ((FakePlayer) player).kill(message);
        }
    }

    @Override
    public void teleport(double d, double e, double f, float g, float h, @NotNull Set<RelativeMovement> set) {
        super.teleport(d, e, f, g, h, set);
        //noinspection resource
        if (player.serverLevel().getPlayerByUUID(player.getUUID()) != null) {
            resetPosition();
            //noinspection resource
            player.serverLevel().getChunkSource().move(player);
        }
    }
}
