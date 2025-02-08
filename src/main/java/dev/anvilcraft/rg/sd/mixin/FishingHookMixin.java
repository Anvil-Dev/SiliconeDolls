package dev.anvilcraft.rg.sd.mixin;

import dev.anvilcraft.rg.sd.SiliconeDollsServerRules;
import dev.anvilcraft.rg.sd.entity.FakePlayer;
import dev.anvilcraft.rg.sd.util.FakePlayerAutoFish;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FishingHook.class)
abstract class FishingHookMixin {
    @Inject(method = "catchingFish", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/syncher/SynchedEntityData;set(Lnet/minecraft/network/syncher/EntityDataAccessor;Ljava/lang/Object;)V", ordinal = 1))
    private void catchingFish(BlockPos pos, CallbackInfo ci) {
        Entity entity = ((FishingHook) (Object) this).getOwner();
        if (SiliconeDollsServerRules.fakePlayerAutoFish && entity instanceof FakePlayer player) {
            FakePlayerAutoFish.autoFish(player);
        }
    }
}
