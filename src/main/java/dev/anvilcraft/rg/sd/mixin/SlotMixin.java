package dev.anvilcraft.rg.sd.mixin;

import dev.anvilcraft.rg.sd.util.ISlotIconInjector;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Slot.class)
abstract class SlotMixin implements ISlotIconInjector {
    @Unique
    private ResourceLocation siliconeDolls$location;

    @Inject(method = "getNoItemIcon", at = @At("HEAD"), cancellable = true)
    private void getNoItemIcon(@NotNull CallbackInfoReturnable<ResourceLocation> cir) {
        cir.setReturnValue(this.siliconeDolls$location);
    }

    @Override
    public void siliconeDolls$setIcon(ResourceLocation resource) {
        if (resource != null) {
            this.siliconeDolls$location = resource;
        }
    }
}
