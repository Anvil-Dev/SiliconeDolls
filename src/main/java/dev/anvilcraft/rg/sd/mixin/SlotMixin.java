package dev.anvilcraft.rg.sd.mixin;

import com.mojang.datafixers.util.Pair;
import dev.anvilcraft.rg.sd.util.SlotIcon;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Slot.class)
public class SlotMixin implements SlotIcon {
    @Unique
    private Pair<ResourceLocation, ResourceLocation> siliconeDolls$pair;

    @Inject(method = "getNoItemIcon", at = @At("HEAD"), cancellable = true)
    private void getNoItemIcon(
        CallbackInfoReturnable<Pair<ResourceLocation, ResourceLocation>> cir
    ) {
        if (this.siliconeDolls$pair != null) cir.setReturnValue(this.siliconeDolls$pair);
    }

    @Override
    public void siliconeDolls$setIcon(ResourceLocation resource) {
        if (resource != null) {
            this.siliconeDolls$pair = Pair.of(InventoryMenu.BLOCK_ATLAS, resource);
        }
    }
}
