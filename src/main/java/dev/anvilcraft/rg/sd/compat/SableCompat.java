package dev.anvilcraft.rg.sd.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.ModList;

/**
 * Public entry points for the Sable physics compatibility.
 * All Sable classes are only touched by {@link SableCompatRuntime}, which is
 * loaded lazily, so this class is safe to reference when Sable is absent.
 */
public final class SableCompat {
    private static final boolean PRESENT = ModList.get() != null && ModList.get().isLoaded("sable");

    private SableCompat() {
    }

    public static boolean isPresent() {
        return PRESENT;
    }

    /**
     * FakePlayers move server side (no client to drive them), so unlike real players they must
     * not keep a stale tracking sub-level once they stop touching the physics object.
     */
    public static void afterFakePlayerTick(LivingEntity player) {
        if (PRESENT) {
            SableCompatRuntime.afterFakePlayerTick(player);
        }
    }

    public static boolean isStandingOnPhysicsObject(Entity entity) {
        return PRESENT && SableCompatRuntime.isStandingOnPhysicsObject(entity);
    }

    /**
     * While the player stands on a physics object, the block governing movement friction
     * lives in the sub-level's plot space, not in the world. Returns {@code null} when the
     * vanilla world-space sample should be used.
     */
    public static BlockPos blockPosBelowThatAffectsMyMovement(Entity player, BlockPos vanillaPos) {
        return PRESENT ? SableCompatRuntime.blockPosBelowThatAffectsMyMovement(player, vanillaPos) : null;
    }
}
