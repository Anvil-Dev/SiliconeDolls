package dev.anvilcraft.rg.sd.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.anvilcraft.rg.sd.entity.FakePlayer;
import dev.anvilcraft.rg.sd.entity.FakePlayerNetHandler;
import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.ValueInput;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(PlayerList.class)
abstract class PlayerListMixin {
    @Inject(method = "load", at = @At(value = "RETURN"))
    private void fixStartingPos(ServerPlayer serverPlayer, ProblemReporter problemReporter, CallbackInfoReturnable<Optional<ValueInput>> cir) {
        if (serverPlayer instanceof FakePlayer player) {
            player.fixStartingPosition.run();
        }
    }

    @WrapOperation(method = "placeNewPlayer", at = @At(value = "NEW", target = "(Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/network/Connection;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/server/network/CommonListenerCookie;)Lnet/minecraft/server/network/ServerGamePacketListenerImpl;"))
    private @NotNull ServerGamePacketListenerImpl replaceNetworkHandler(MinecraftServer server, Connection connection, ServerPlayer player, CommonListenerCookie cookie, Operation<ServerGamePacketListenerImpl> original) {
        if (player instanceof FakePlayer fake) {
            return new FakePlayerNetHandler(server, connection, fake, cookie);
        } else {
            return original.call(server, connection, player, cookie);
        }
    }
}
