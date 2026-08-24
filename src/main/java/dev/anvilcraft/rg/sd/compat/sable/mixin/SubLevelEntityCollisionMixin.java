package dev.anvilcraft.rg.sd.compat.sable.mixin;

import dev.anvilcraft.rg.sd.entity.FakePlayer;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Sable skips sub-level collision entirely for ServerPlayers because their movement is
 * client authoritative. FakePlayers have no client, so they must run through the regular
 * (mob-like) server side collision path instead.
 */
@Mixin(targets = "dev.ryanhcode.sable.sublevel.entity_collision.SubLevelEntityCollision", remap = false, priority = 1200)
public abstract class SubLevelEntityCollisionMixin {

    @Redirect(
        method = "collide",
        at = @At(
            value = "CONSTANT",
            args = "classValue=net/minecraft/server/level/ServerPlayer"
        )
    )
    private static boolean sd$fakePlayerUsesServerSideCollision(Object instance, Class<?> clazz) {
        return instance instanceof ServerPlayer && !(instance instanceof FakePlayer);
    }
}
