package dev.anvilcraft.rg.sd.mixin.kubejs;

import dev.anvilcraft.rg.sd.entity.FakePlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PacketDistributor.class, remap = false)
public class PacketDistributorMixin {
    @Inject(method = "sendToPlayer", at = @At("HEAD"), cancellable = true)
    private static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload, CustomPacketPayload[] payloads, CallbackInfo ci) {
        if (player instanceof FakePlayer) ci.cancel();
    }
}
