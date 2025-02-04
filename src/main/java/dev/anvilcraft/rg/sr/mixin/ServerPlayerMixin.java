package dev.anvilcraft.rg.sr.mixin;

import dev.anvilcraft.rg.sr.entity.PlayerActionPack;
import dev.anvilcraft.rg.sr.util.ServerPlayerInjector;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin implements ServerPlayerInjector {
    @Override
    @SuppressWarnings("AddedMixinMembersNamePattern")
    public @NotNull PlayerActionPack getActionPack() {
        return new PlayerActionPack();
    }
}
