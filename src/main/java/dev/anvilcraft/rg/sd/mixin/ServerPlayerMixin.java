package dev.anvilcraft.rg.sd.mixin;

import com.mojang.authlib.GameProfile;
import dev.anvilcraft.rg.sd.entity.PlayerActionPack;
import dev.anvilcraft.rg.sd.tool.PlayerEnderChestContainer;
import dev.anvilcraft.rg.sd.tool.PlayerInventoryContainer;
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
    public PlayerActionPack sd$actionPack;
    @Unique
    PlayerInventoryContainer sd$inventoryContainer;
    @Unique
    PlayerEnderChestContainer sd$enderChestContainer;

    @Inject(method = "<init>", at = @At(value = "RETURN"))
    private void onServerPlayerEntityContructor(MinecraftServer minecraftServer, ServerLevel serverLevel, GameProfile gameProfile, ClientInformation cli, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        this.sd$actionPack = new PlayerActionPack(player);
        this.sd$inventoryContainer = new PlayerInventoryContainer(player);
        this.sd$enderChestContainer = new PlayerEnderChestContainer(player);
    }

    @Inject(method = "tick", at = @At(value = "HEAD"))
    private void onTick(CallbackInfo ci) {
        this.sd$actionPack.onUpdate();
        this.sd$inventoryContainer.tick();
        this.sd$enderChestContainer.tick();
    }

    @Override
    @SuppressWarnings("AddedMixinMembersNamePattern")
    public @NotNull PlayerActionPack getActionPack() {
        return sd$actionPack;
    }

    @Override
    @SuppressWarnings("AddedMixinMembersNamePattern")
    public @NotNull PlayerEnderChestContainer getEnderChestContainer() {
        return this.sd$enderChestContainer;
    }

    @Override
    @SuppressWarnings("AddedMixinMembersNamePattern")
    public @NotNull PlayerInventoryContainer getInventoryContainer() {
        return this.sd$inventoryContainer;
    }
}
