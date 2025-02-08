package dev.anvilcraft.rg.sd.mixin;

import com.mojang.authlib.GameProfile;
import dev.anvilcraft.rg.sd.entity.PlayerActionPack;
import dev.anvilcraft.rg.sd.util.IServerPlayerInjector;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
abstract class ServerPlayerMixin implements IServerPlayerInjector {
    @Unique
    public PlayerActionPack siliconeDolls$actionPack;

    @Override
    @SuppressWarnings("AddedMixinMembersNamePattern")
    public @NotNull PlayerActionPack getActionPack() {
        return siliconeDolls$actionPack;
    }

    @Inject(method = "<init>", at = @At(value = "RETURN"))
    private void onServerPlayerEntityContructor(MinecraftServer minecraftServer, ServerLevel serverLevel, GameProfile gameProfile, ClientInformation cli, CallbackInfo ci) {
        this.siliconeDolls$actionPack = new PlayerActionPack((ServerPlayer) (Object) this);
    }

    @Inject(method = "tick", at = @At(value = "HEAD"))
    private void onTick(CallbackInfo ci) {
        siliconeDolls$actionPack.onUpdate();
    }
}
