package dev.anvilcraft.rg.sd.mixin;

import dev.anvilcraft.rg.sd.entity.FakePlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerList.class)
abstract class PlayerListMixin {
    @Inject(method = "load", at = @At(value = "RETURN"))
    private void fixStartingPos(ServerPlayer serverPlayerEntity_1, CallbackInfoReturnable<CompoundTag> cir) {
        if (serverPlayerEntity_1 instanceof FakePlayer player) {
            player.fixStartingPosition.run();
        }
    }
}
