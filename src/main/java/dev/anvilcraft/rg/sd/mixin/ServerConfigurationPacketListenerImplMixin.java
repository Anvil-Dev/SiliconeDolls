package dev.anvilcraft.rg.sd.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.anvilcraft.rg.sd.entity.FakePlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.UUID;

@Mixin(ServerConfigurationPacketListenerImpl.class)
abstract class ServerConfigurationPacketListenerImplMixin {
    @WrapOperation(
        method = "handleConfigurationFinished",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/players/PlayerList;getPlayer(Ljava/util/UUID;)Lnet/minecraft/server/level/ServerPlayer;"
        )
    )
    private @Nullable ServerPlayer handleConfigurationFinished(PlayerList instance, UUID playerUUID, @NotNull Operation<ServerPlayer> original) {
        ServerPlayer player = original.call(instance, playerUUID);
        if (player instanceof FakePlayer fake) {
            fake.disconnect();
            return null;
        } else {
            return player;
        }
    }
}
