package dev.anvilcraft.rg.sd.gametest;

import com.mojang.authlib.GameProfile;
import dev.anvilcraft.rg.sd.SiliconeDolls;
import dev.anvilcraft.rg.sd.compat.SableCompat;
import dev.anvilcraft.rg.sd.entity.FakeClientConnection;
import dev.anvilcraft.rg.sd.entity.FakePlayer;
import dev.anvilcraft.rg.sd.mixin.EntityInvoker;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

/**
 * Compatibility tests for FakePlayer vs Sable physics entities (sub-levels).
 * <p>
 * Tests that only need a static platform freeze the physics pipeline and drive
 * poses kinematically; tests that need real carrying behaviour keep the physics
 * pipeline running. These tests are only meaningful when Sable is installed;
 * they succeed trivially otherwise.
 */
@GameTestHolder(SiliconeDolls.MODID)
@PrefixGameTestTemplate(false)
public class FakePlayerPhysicsTests {

    /** Top surface Y of the game test server world floor (empirical). */
    private static final double FLOOR_TOP_Y = -60.0;

    private static final double CONTACT_TOLERANCE = 0.75;

    private record Ship(ServerSubLevel subLevel, RigidBodyHandle handle, Pose3d assembledPose,
                        PhysicsPipeline pipeline) {
    }

    /**
     * Freezes all rigid body dynamics for this level: no gravity, no integration.
     * Ships are then driven purely kinematically through {@code handle.teleport},
     * which makes the tests fully deterministic. Entity vs sub-level collision is
     * geometric and keeps working normally.
     */
    private static void freezePhysics(GameTestHelper helper) {
        var container = dev.ryanhcode.sable.api.sublevel.SubLevelContainer.getContainer(helper.getLevel());
        if (container != null) {
            container.physicsSystem().setPaused(true);
        }
    }

    private static void unfreezePhysics(GameTestHelper helper) {
        var container = dev.ryanhcode.sable.api.sublevel.SubLevelContainer.getContainer(helper.getLevel());
        if (container != null) {
            container.physicsSystem().setPaused(false);
        }
    }

    // ------------------------------------------------------------------
    // Test 1: doll falling onto the top of a 3x3x1 physics platform
    // ------------------------------------------------------------------
    @GameTest(template = "physicstest_fall", timeoutTicks = 400, batch = "physicsCompat")
    public static void fakePlayerLandsOnPhysicsPlatform(GameTestHelper helper) {
        if (!SableCompat.isPresent()) {
            helper.succeed();
            return;
        }
        freezePhysics(helper);
        Ship ship = assemblePlatform(helper, new BlockPos(5, 3, 5), 1, 0, 1);

        // physics is frozen: the platform stays exactly where it was assembled
        double expectedTop = helper.absolutePos(new BlockPos(5, 3, 5)).getY() + 1.0;

        Vec3 spawn = centerOf(helper, new BlockPos(5, 12, 5));
        FakePlayer doll = spawnDoll(helper, spawn, "fall_doll");

        helper.startSequence()
            .thenExecuteFor(160, () -> {
            })
            .thenExecute(() -> {
                double feetY = doll.getY();
                helper.assertTrue(doll.isAlive(), "doll should be alive");
                helper.assertTrue(doll.onGround(), "doll should be on ground, y=" + feetY);
                helper.assertTrue(
                    Math.abs(feetY - expectedTop) <= CONTACT_TOLERANCE,
                    "doll feet should rest on top of the physics platform: expected ~" + expectedTop + " got " + feetY
                );
                helper.assertTrue(
                    SableCompat.isStandingOnPhysicsObject(doll),
                    "doll should be tracking the physics platform"
                );
            })
            .thenExecute(() -> doll.kill())
            .thenSucceed();
    }

    // ------------------------------------------------------------------
    // Test 2: physics object rushing into the doll pushes it away
    // ------------------------------------------------------------------
    @GameTest(template = "physicstest_ram", timeoutTicks = 500, batch = "physicsCompat")
    public static void physicsPlatformPushesFakePlayer(GameTestHelper helper) {
        if (!SableCompat.isPresent()) {
            helper.succeed();
            return;
        }
        freezePhysics(helper);
        // stone floor strip so the doll never touches bare void
        for (int x = 1; x <= 21; x++) {
            for (int z = 2; z <= 4; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE.defaultBlockState());
            }
        }
        // 1(thick) x 3(tall) x 3(wide) wall
        Ship wall = assemblePlatform(helper, new BlockPos(4, 1, 3), 0, 1, 1);

        Vec3 spawn = centerOf(helper, new BlockPos(14, 1, 3));
        FakePlayer doll = spawnDoll(helper, spawn, "ram_doll");
        double startX = doll.getX();

        // keep each per-tick step below the doll's half width (0.3) so the swept
        // collision always registers the overlap and pushes accumulate reliably
        final double speed = 0.2;
        final int advanceTicks = 60;
        Vector3d cursor = new Vector3d(wall.assembledPose().position());
        Quaterniondc orientation = wall.assembledPose().orientation();

        helper.startSequence()
            .thenIdle(10)
            .thenExecuteFor(advanceTicks, () -> {
                cursor.x += speed;
                wall.handle().teleport(cursor, orientation);
            })
            .thenIdle(10)
            .thenExecute(() -> {
                double dx = doll.getX() - startX;
                double wallFront = helper.absolutePos(new BlockPos(4, 0, 3)).getX() + 1.0 + advanceTicks * speed;
                SiliconeDolls.LOGGER.info("ram: wallX={} dollX={} startX={} wallFront={}",
                    String.format("%.2f", cursor.x), String.format("%.2f", doll.getX()),
                    String.format("%.2f", startX), String.format("%.2f", wallFront));
                helper.assertTrue(doll.isAlive(), "doll should survive the ram");
                helper.assertTrue(
                    dx >= 1.2,
                    "doll should be pushed by the moving physics object, displacement=" + dx
                );
                helper.assertTrue(
                    doll.getX() > wallFront - 1.5,
                    "doll should not end up inside the physics object: dollX=" + doll.getX() + " wallFront~" + wallFront
                );
            })
            .thenExecute(() -> doll.kill())
            .thenSucceed();
    }

    // ------------------------------------------------------------------
    // Test 3: doll standing on the fine collision of a 45 degree slope
    // ------------------------------------------------------------------
    @GameTest(template = "physicstest_slope", timeoutTicks = 600, batch = "physicsCompat")
    public static void fakePlayerStandsOnSlopedPhysicsSurface(GameTestHelper helper) {
        if (!SableCompat.isPresent()) {
            helper.succeed();
            return;
        }
        freezePhysics(helper);
        // support wall made of regular world blocks; the plank leans against it
        for (int y = 1; y <= 4; y++) {
            for (int z = 4; z <= 8; z++) {
                helper.setBlock(new BlockPos(10, y, z), Blocks.STONE.defaultBlockState());
            }
        }

        // 7(x) x 1(y) x 3(z) plank assembled flat, then teleported into a 45 degree
        // lean against the wall; physics is frozen so the pose stays exact
        Ship plank = assemblePlatform(helper, new BlockPos(5, 2, 6), 3, 0, 1);

        // leaning geometry: bottom long edge on the floor, upper long edge against the
        // wall face at rel x=10.0, rotated 45 degrees around Z through the plank center.
        double cx = helper.absolutePos(new BlockPos(7, 0, 0)).getX() + 0.5 + (2.828 - 2.121);
        double cy = FLOOR_TOP_Y + 2.828;
        double cz = helper.absolutePos(new BlockPos(0, 0, 6)).getZ() + 0.5;
        Quaterniondc tilt = new Quaterniond().rotateZ(Math.PI / 4.0);
        plank.handle().teleport(new Vector3d(cx, cy, cz), tilt);

        final FakePlayer[] dollHolder = new FakePlayer[1];

        helper.startSequence()
            .thenExecute(() -> {
                Vector3dc pos = plank.subLevel().logicalPose().position();
                org.joml.Quaterniondc rot = plank.subLevel().logicalPose().orientation();
                SiliconeDolls.LOGGER.info("slope: plank pose=({}, {}, {}) quat=({}, {}, {}, {})",
                    pos.x(), pos.y(), pos.z(), rot.x(), rot.y(), rot.z(), rot.w());

                // drop the doll up-slope near the wall
                Vec3 spawn = new Vec3(
                    helper.absolutePos(new BlockPos(9, 0, 6)).getX() + 0.5,
                    FLOOR_TOP_Y + 14.0,
                    helper.absolutePos(new BlockPos(0, 0, 6)).getZ() + 0.5
                );
                dollHolder[0] = spawnDoll(helper, spawn, "slope_doll");
            })
            .thenExecuteFor(40, () -> {
                // let the doll land on the incline and start sliding down it
            })
            .thenExecute(() -> helper.assertTrue(dollHolder[0].onGround(),
                "doll should be supported by the slope, y=" + dollHolder[0].getY()))
            .thenExecuteFor(20, () -> {
                FakePlayer doll = dollHolder[0];
                // while sliding, the feet must keep following the fine inclined surface:
                Vector3dc c = plank.subLevel().logicalPose().position();
                org.joml.Quaterniondc r = plank.subLevel().logicalPose().orientation();
                // local up axis of the plank in world space
                Vector3d up = new Vector3d(0, 1, 0).rotate(r);
                Vector3d normal = up.normalize();
                double dist = distanceToPlane(normal, 0.5, new Vector3d(c), doll.position());
                helper.assertTrue(
                    doll.onGround(),
                    "doll should stay supported by the sloped surface, y=" + doll.getY()
                );
                helper.assertTrue(
                    Math.abs(dist) <= CONTACT_TOLERANCE,
                    "doll feet should follow the fine 45 degree collision surface, signed distance=" + dist
                );
            })
            .thenExecute(() -> dollHolder[0].kill())
            .thenSucceed();
    }

    // ------------------------------------------------------------------
    // Test 4: doll riding a physics platform moving at constant high speed.
    // The deck is driven by real physics impulses (no freezing, no manual carry);
    // if the rider's velocity diverges from the deck's, the carry is broken.
    // Phases: accelerate -> measured cruise -> brake to a stop on the track.
    // ------------------------------------------------------------------
    @GameTest(template = "physicstest_ride", timeoutTicks = 900, batch = "physicsCompat")
    public static void fakePlayerRidesAcceleratingPlatform(GameTestHelper helper) {
        if (!SableCompat.isPresent()) {
            helper.succeed();
            return;
        }
        // real rigid body integration is required: the deck must move through the
        // pipeline so that riders are carried by the engine, not by the test
        unfreezePhysics(helper);

        // low-friction blue ice track long enough for accel + cruise + brake
        for (int x = 3; x <= 27; x++) {
            for (int z = 14; z <= 16; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.BLUE_ICE.defaultBlockState());
            }
        }

        // 3x3x1 platform resting directly on the track near its start
        Ship platform = assemblePlatform(helper, new BlockPos(5, 1, 15), 1, 0, 1);

        // doll stands exactly on the deck centre (deck top surface at rel y=2)
        Vec3 spawn = centerOf(helper, new BlockPos(5, 2, 15));
        FakePlayer doll = spawnDoll(helper, spawn, "ride_doll");

        final double cruiseSpeed = 8.0;      // m/s == 0.4 blocks per tick
        final int accelTicks = 15;
        final int cruiseTicks = 20;          // measured window
        final int brakeTicks = 30;

        final double[] dollVx = {0};
        final double[] deckVx = {0};
        final double[] winDollX = {0};
        final double[] winDeckX = {0};

        helper.startSequence()
            .thenIdle(10) // doll settles onto the deck
            .thenExecute(() -> helper.assertTrue(SableCompat.isStandingOnPhysicsObject(doll),
                "doll should be tracking the platform before it moves"))
            // accelerate
            .thenExecuteFor(accelTicks, () -> thrustToward(platform, cruiseSpeed))
            // record window start, keep cruising, record window end
            .thenExecute(() -> {
                winDollX[0] = doll.getX();
                winDeckX[0] = platform.subLevel().logicalPose().position().x();
            })
            .thenExecuteFor(cruiseTicks, () -> thrustToward(platform, cruiseSpeed))
            .thenExecute(() -> {
                dollVx[0] = (doll.getX() - winDollX[0]) / cruiseTicks;
                deckVx[0] = (platform.subLevel().logicalPose().position().x() - winDeckX[0]) / cruiseTicks;
            })
            // brake back down to zero, staying on the track
            .thenExecuteFor(brakeTicks, () -> thrustToward(platform, 0.0))
            .thenIdle(20)
            .thenExecute(() -> {
                Vector3dc endPos = platform.subLevel().logicalPose().position();
                SiliconeDolls.LOGGER.info("ride: deckVx={} dollVx={} endDeckX={} dollX={} feetY={}",
                    String.format("%.3f", deckVx[0]), String.format("%.3f", dollVx[0]),
                    String.format("%.2f", endPos.x()), String.format("%.2f", doll.getX()),
                    String.format("%.3f", doll.getY()));

                helper.assertTrue(deckVx[0] >= 0.25,
                    "platform should be moving fast along the track, vx=" + deckVx[0] + " blocks/tick");
                helper.assertTrue(doll.onGround(), "doll should still stand on the deck");
                helper.assertTrue(
                    Math.abs(dollVx[0] - deckVx[0]) <= 0.08,
                    "rider velocity must match the deck velocity (no double acceleration): deck="
                        + deckVx[0] + " doll=" + dollVx[0]
                );
                double offset = Math.abs(doll.getX() - endPos.x());
                helper.assertTrue(offset <= 2.0,
                    "doll should stay on top of the 3x3 deck: offset=" + offset);
                helper.assertTrue(
                    endPos.x() - helper.absolutePos(new BlockPos(3, 0, 15)).getX() < 24.0,
                    "platform should have braked before leaving the ice track"
                );
            })
            .thenExecute(() -> doll.kill())
            .thenSucceed();
    }

    /** Applies an impulse-based correction so the deck's X velocity approaches {@code target} (m/s). */
    private static void thrustToward(Ship platform, double target) {
        platform.pipeline().wakeUp(platform.subLevel());
        Vector3d vel = platform.handle().getLinearVelocity(new Vector3d());
        double correction = target - vel.x;
        correction = Math.max(-2.0, Math.min(2.0, correction)); // cap per-tick change
        if (Math.abs(correction) > 1.0E-3) {
            platform.handle().addLinearAndAngularVelocity(new Vector3d(correction, 0, 0), new Vector3d(0, 0, 0));
        }
    }

    // ------------------------------------------------------------------

    /**
     * Signed distance of {@code point} from the plane {@code n . (p - center) = offset}.
     */
    private static double distanceToPlane(Vector3d normal, double offset, Vector3d center, Vec3 point) {
        double dx = point.x - center.x;
        double dy = point.y - center.y;
        double dz = point.z - center.z;
        return normal.x * dx + normal.y * dy + normal.z * dz - offset;
    }

    private static Vec3 centerOf(GameTestHelper helper, BlockPos relative) {
        BlockPos abs = helper.absolutePos(relative);
        return new Vec3(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
    }

    /**
     * Assembles the blocks around {@code anchor} (extents given per axis) into one physics object.
     */
    private static Ship assemblePlatform(GameTestHelper helper, BlockPos anchorRel, int extX, int extY, int extZ) {
        ServerLevel level = helper.getLevel();
        BlockPos anchorAbs = helper.absolutePos(anchorRel);

        for (int x = -extX; x <= extX; x++) {
            for (int y = -extY; y <= extY; y++) {
                for (int z = -extZ; z <= extZ; z++) {
                    level.setBlock(anchorAbs.offset(x, y, z), Blocks.STONE.defaultBlockState(), 3);
                }
            }
        }

        // restrict the flood fill to the structure's own volume so it does not
        // leak into adjacent world blocks (e.g. the gametest floor)
        dev.ryanhcode.sable.companion.math.BoundingBox3i allowed =
            new dev.ryanhcode.sable.companion.math.BoundingBox3i(
                anchorAbs.getX() - extX, anchorAbs.getY() - extY, anchorAbs.getZ() - extZ,
                anchorAbs.getX() + extX, anchorAbs.getY() + extY, anchorAbs.getZ() + extZ);
        var gather = SubLevelAssemblyHelper.gatherConnectedBlocks(anchorAbs, level, 512, (o, os, p, s, d) ->
            p.getX() >= allowed.minX() && p.getX() <= allowed.maxX()
                && p.getY() >= allowed.minY() && p.getY() <= allowed.maxY()
                && p.getZ() >= allowed.minZ() && p.getZ() <= allowed.maxZ());
        SiliconeDolls.LOGGER.info("ASSEMDBG anchor={} originState={} result={} blocks={}",
            anchorAbs, level.getBlockState(anchorAbs), gather.assemblyState(),
            gather.blocks() == null ? 0 : gather.blocks().size());
        ServerSubLevel subLevel = SubLevelAssemblyHelper.assembleBlocks(level, anchorAbs, gather.blocks(), gather.boundingBox());
        RigidBodyHandle handle = RigidBodyHandle.of(subLevel);
        PhysicsPipeline pipeline = dev.ryanhcode.sable.api.sublevel.SubLevelContainer.getContainer(level).physicsSystem().getPipeline();
        Pose3d pose = new Pose3d(subLevel.logicalPose());
        return new Ship(subLevel, handle, pose, pipeline);
    }

    private static FakePlayer spawnDoll(GameTestHelper helper, Vec3 pos, String name) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel level = helper.getLevel();
        // remove a stale doll from an earlier (possibly failed) run first
        var stale = server.getPlayerList().getPlayerByName(name);
        if (stale != null) {
            stale.connection.onDisconnect(new net.minecraft.network.DisconnectionDetails(
                net.minecraft.network.chat.Component.literal("test reset")));
        }
        GameProfile profile = new GameProfile(UUIDUtil.createOfflinePlayerUUID(name), name);
        FakePlayer doll = FakePlayer.create(server, level, profile, ClientInformation.createDefault(), false);
        server.getPlayerList().placeNewPlayer(
            new FakeClientConnection(PacketFlow.SERVERBOUND),
            doll,
            new CommonListenerCookie(profile, 0, doll.clientInformation(), false)
        );
        doll.teleportTo(level, pos.x, pos.y, pos.z, 0.0f, 0.0f);
        doll.setHealth(20.0f);
        ((EntityInvoker) doll).invokerUnsetRemoved();
        var stepHeight = doll.getAttribute(Attributes.STEP_HEIGHT);
        if (stepHeight != null) {
            stepHeight.setBaseValue(0.6f);
        }
        doll.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        return doll;
    }
}
