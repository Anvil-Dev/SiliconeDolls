package dev.anvilcraft.rg.sd.mixin.compat;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.contraptions.ContraptionCollider", remap = false)
public abstract class CreateContraptionColliderMixin {
    @Inject(method = "getPlayerType", at = @At("RETURN"), cancellable = true, remap = false)
    private static void siliconeDolls$adjustFakePlayerType(Entity entity, CallbackInfoReturnable<Enum<?>> cir) {
        if (entity instanceof dev.anvilcraft.rg.sd.entity.FakePlayer) {
            try {
                @SuppressWarnings("unchecked")
                Class<Enum> typeClass = (Class<Enum>) Class.forName("com.simibubi.create.content.contraptions.ContraptionCollider$PlayerType");
                Enum<?> none = Enum.valueOf(typeClass, "NONE");
                cir.setReturnValue(none);
            } catch (ClassNotFoundException ignored) {
            }
        }
    }
}
