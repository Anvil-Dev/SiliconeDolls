package dev.anvilcraft.rg.sd.mixin;

import dev.anvilcraft.rg.sd.entity.FakePlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Player.class)
public class PlayerMixin {
    @Redirect(
        method = "attack",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/world/entity/Entity;hurtMarked:Z",
            ordinal = 0
        )
    )
    private boolean attack(@NotNull Entity target) {
        return target.hurtMarked && !(target instanceof FakePlayer);
    }
}
