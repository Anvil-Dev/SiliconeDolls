package dev.anvilcraft.rg.sd.mixin.combat;

import com.mojang.authlib.GameProfile;
import dev.anvilcraft.rg.sd.entity.FakePlayer;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class ServerCommonPacketListenerImplMixin {
    @Shadow
    @Final
    protected MinecraftServer server;

    @Shadow
    public abstract GameProfile getOwner();

    @Inject(
        method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V",
        at = @At("HEAD"), cancellable = true
    )
    private void send(Packet<?> packet, ChannelFutureListener sendListener, CallbackInfo ci) {
        ServerPlayer player = this.server.getPlayerList().getPlayer(this.getOwner().getId());
        if (player instanceof FakePlayer) ci.cancel();
    }
}
