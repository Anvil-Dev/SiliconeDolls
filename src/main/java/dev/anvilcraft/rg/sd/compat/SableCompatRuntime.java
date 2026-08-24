package dev.anvilcraft.rg.sd.compat;

import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.EntityMovementExtension;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.LivingEntityMovementExtension;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.entity_collision.SubLevelEntityCollision;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;

/**
 * Contains all direct references to Sable classes. Only loaded when Sable is present.
 */
final class SableCompatRuntime {
    /** Gravity applied per tick while resting; larger falls mean the deck was truly left. */
    private static final double MAX_GRAZING_FALL_SPEED = 0.5;

    /** Set -Dsilicone_dolls.riding_debug=true to log per-second riding state. */
    private static final boolean RIDING_DEBUG =
        Boolean.getBoolean("silicone_dolls.riding_debug") || Boolean.getBoolean("silicone_dolls.ridingDebug");

    /**
     * Last deck motion per rider. While riding, Sable already applies this delta to the
     * position every tick; remembering it lets us hand the momentum back once the rider
     * leaves the deck (jumping off a moving ship should throw you forward).
     */
    private static final java.util.Map<Entity, Vector3d> LAST_DECK_MOTION =
        java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    private SableCompatRuntime() {
    }

    static void afterFakePlayerTick(LivingEntity player) {
        if (player.isPassenger()) {
            return;
        }
        EntityMovementExtension ext = (EntityMovementExtension) player;
        SubLevelEntityCollision.CollisionInfo info = ext.sable$getCollisionInfo();

        SubLevel tracking = ext.sable$getTrackingSubLevel();
        if (info != null && info.trackingSubLevel == null && tracking != null) {
            ext.sable$setTrackingSubLevel(null);
            tracking = null;
        }

        var inheritedExt = (LivingEntityMovementExtension) player;
        Vector3d inheritedVelocity = inheritedExt.sable$getInheritedVelocity();

        if (tracking != null) {
            // The deck's motion reaches a rider through TWO independent paths:
            //   1. the collision sweep itself - the deck blocks advance across sub-steps,
            //      so the summed MTVs already contain the deck delta, and
            //   2. collisionInfo.inheritedMotion, applied again after travel().
            // A continuously tracked rider therefore receives the deck delta TWICE and
            // surges ahead of an accelerating deck - but only when the pipeline actually
            // stepped this tick, which is visible as a non-zero lastPose -> logicalPose
            // delta. Kinematically frozen decks keep that delta at zero, so there
            // inheritedMotion is the sole carrier and must be kept.
            if (info != null && info.inheritedMotion != null) {
                Vec3 im = info.inheritedMotion;
                Vector3dc last = tracking.lastPose().position();
                Vector3dc cur = tracking.logicalPose().position();
                double pdx = cur.x() - last.x();
                double pdy = cur.y() - last.y();
                double pdz = cur.z() - last.z();
                double poseDeltaSq = pdx * pdx + pdy * pdy + pdz * pdz;
                double imSq = im.x * im.x + im.y * im.y + im.z * im.z;

                boolean mtvAlreadyCarried =
                    poseDeltaSq > 1.0E-8
                        && (im.x * pdx + im.y * pdy + im.z * pdz) > 0
                        && imSq >= 0.25 * poseDeltaSq;

                LAST_DECK_MOTION.put(player, mtvAlreadyCarried
                    ? new Vector3d(pdx, pdy, pdz)
                    : new Vector3d(im.x, im.y, im.z));

                if (mtvAlreadyCarried) {
                    player.setPos(player.getX() - im.x, player.getY() - im.y, player.getZ() - im.z);
                }
            }
        } else {
            // left the deck: return the deck's momentum once (throw inertia)
            Vector3d stored = LAST_DECK_MOTION.remove(player);
            if (stored != null && stored.lengthSquared() > 1.0E-6) {
                player.addDeltaMovement(new net.minecraft.world.phys.Vec3(stored.x, stored.y, stored.z));
            }
        }

        // While grazing along a sub-level surface the SAT resolution can miss the shallow
        // penetration on a given tick, which flips onGround to false and makes travel()
        // damp horizontal movement with 0.98 (ice) instead of the block friction.
        // Keep ground contact while the player is tracked and only falling by gravity.
        if (tracking != null
                && !player.onGround()
                && player.getDeltaMovement().y <= 0.01
                && player.getDeltaMovement().y > -MAX_GRAZING_FALL_SPEED
                && player.fallDistance < 1.0F) {
            player.setOnGround(true);
        }

        // During fast vertical deck motion the discrete collision step can leave the
        // rider sunk into the deck for a tick or two. Pop her back onto the deck
        // surface when she has only sunk a little.
        if (tracking != null && player.getY() < tracking.logicalPose().position().y()) {
            BlockPos below = blockPosBelowThatAffectsMyMovement(player, player.blockPosition());
            var state = player.level().getBlockState(below);
            var shape = state.getCollisionShape(player.level(), below);
            if (!shape.isEmpty()) {
                double surfaceY = below.getY() + shape.max(net.minecraft.core.Direction.Axis.Y);
                double sink = surfaceY - player.getY();
                if (sink > 0.001 && sink < 1.6) {
                    player.setPos(player.getX(), surfaceY, player.getZ());
                    if (player.fallDistance < 1.0F) {
                        player.setOnGround(true);
                    }
                }
            }
        }

        if (RIDING_DEBUG && tracking != null) {
            boolean fast = false;
            Vector3dc deck = tracking.logicalPose().position();
            Vector3d deckVel = dev.ryanhcode.sable.Sable.HELPER.getVelocity(
                player.level(), tracking,
                new org.joml.Vector3d(player.getX(), player.getY(), player.getZ()),
                new org.joml.Vector3d());
            fast = deckVel.lengthSquared() > 1.0;
            if (fast || player.tickCount % 20 == 0) {
                dev.anvilcraft.rg.sd.SiliconeDolls.LOGGER.info(
                    "[RidingDebug] {} pos=({},{},{}) deckPos=({},{},{}) deckVel=({},{},{}) onGround={} vel=({},{},{}) fallDist={}",
                    player.getName().getString(),
                    fmt(player.getX()), fmt(player.getY()), fmt(player.getZ()),
                    fmt(deck.x()), fmt(deck.y()), fmt(deck.z()),
                    fmt(deckVel.x), fmt(deckVel.y), fmt(deckVel.z),
                    player.onGround(),
                    fmt(player.getDeltaMovement().x), fmt(player.getDeltaMovement().y), fmt(player.getDeltaMovement().z),
                    String.format("%.2f", player.fallDistance));
            }
        }
    }

    private static String fmt(double d) {
        return String.format("%.3f", d);
    }

    static boolean isStandingOnPhysicsObject(Entity entity) {
        return ((EntityMovementExtension) entity).sable$getTrackingSubLevel() != null;
    }

    @Nullable
    static BlockPos blockPosBelowThatAffectsMyMovement(Entity player, BlockPos vanillaPos) {
        SubLevel tracking = ((EntityMovementExtension) player).sable$getTrackingSubLevel();
        if (tracking == null) {
            return null;
        }
        Vector3d pos = new Vector3d(player.getX(), player.getY() - 0.500001, player.getZ());
        tracking.logicalPose().transformPositionInverse(pos);
        return BlockPos.containing(pos.x, pos.y, pos.z);
    }
}
