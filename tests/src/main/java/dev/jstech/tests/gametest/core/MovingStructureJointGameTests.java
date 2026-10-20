/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.StructureFrame;
import dev.jstech.core.moving.StructurePose;
import dev.jstech.core.moving.StructureResult;
import dev.jstech.core.moving.StructureYard;
import dev.jstech.core.moving.TowardMotion;
import dev.jstech.core.moving.YardLayout;
import dev.jstech.core.moving.joint.JointBook;
import dev.jstech.core.moving.joint.JointKind;
import dev.jstech.core.moving.joint.JointSignal;
import dev.jstech.core.moving.joint.StructureJoint;
import dev.jstech.core.moving.joint.StructureJoints;
import dev.jstech.core.moving.physics.BlockPhysics;
import dev.jstech.core.moving.physics.PhysicsMotion;
import dev.jstech.core.moving.physics.StructurePhysics;
import dev.jstech.tests.JsTests;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.joml.Vector3d;

/**
 * The joints of moving structures in a real world: a rope holds a crate swinging under its anchor at its length, a
 * hinged bar swings down to its limit, a bearing turns a platform at its motor's speed, a spring hangs its load as far
 * as its weight stretches it, a tow bar keeps two structures at its length as they go, a rope pulled past what it
 * holds breaks, a winch reels its rope in by its signal, a slider driven lifts its load along its line, a weld holds
 * two structures as one, the joints are kept with the world, they go with a structure taken away, and a rope on a
 * structure moved by a rule carries its load along. The tall arena's floor is its layer at y 1.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MovingStructureJointGameTests {

    private static final String ARENA = "empty";
    private static final String TALL = "tall";
    private static final String BATCH = "jscore_moving_joints";
    private static final double HOLDS = 0.03;
    private static final double GRAVITY = 9.81;

    private MovingStructureJointGameTests() {
    }

    @GameTest(template = TALL, batch = BATCH, timeoutTicks = 200)
    public static void joints_aRopeHoldsACrateSwingingAtItsLength(final GameTestHelper helper) {
        final MovingStructure crate = made(helper, List.of(new BlockPos(6, 14, 4)), Blocks.STONE);
        final Vec3 top = topOf(crate);
        final Vec3 anchor = top.add(-2, 2, 0);
        final double length = top.distanceTo(anchor);
        StructureJoints.join(helper.getLevel(), new JointKind.Rope(length, JointKind.RopeLook.ROPE,
                Optional.empty()), StructureJoints.Anchor.on(crate, top, new Vec3(0, 1, 0)),
                StructureJoints.Anchor.world(anchor, new Vec3(0, 1, 0)), Optional.empty());
        crate.move(PhysicsMotion.AT_REST);
        final double[] longest = {0};
        final double[] lowest = {Double.MAX_VALUE};
        helper.onEachTick(() -> {
            longest[0] = Math.max(longest[0], ropePoint(crate).distanceTo(anchor));
            lowest[0] = Math.min(lowest[0], ropePoint(crate).y);
        });
        helper.runAfterDelay(80, () -> {
            helper.assertTrue(longest[0] < length + HOLDS, "the rope holds it at its length " + length + ": "
                    + longest[0]);
            helper.assertTrue(lowest[0] < anchor.y - length + 0.1, "it swung down under its anchor: " + lowest[0]);
            helper.assertTrue(crate.worldBox().minY > y(helper, 4), "and never fell: " + crate.worldBox());
            MovingStructures.takeAway(crate);
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 300)
    public static void joints_aHingedBarSwingsDownToItsLimit(final GameTestHelper helper) {
        final MovingStructure bar = made(helper, List.of(new BlockPos(3, 6, 4), new BlockPos(4, 6, 4),
                new BlockPos(5, 6, 4)), Blocks.OAK_PLANKS);
        final Vec3 hinge = new Vec3(bar.worldBox().minX, bar.worldBox().minY + 0.5, bar.worldBox().minZ + 0.5);
        StructureJoints.join(helper.getLevel(), new JointKind.Hinge(Optional.of(new JointKind.Limits(
                        Math.toRadians(-50), Math.toRadians(50))), Optional.empty()),
                StructureJoints.Anchor.world(hinge, new Vec3(0, 0, 1)),
                StructureJoints.Anchor.on(bar, hinge, new Vec3(0, 0, 1)), Optional.empty());
        bar.move(PhysicsMotion.AT_REST);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(asleep(bar), "it comes to rest"))
                .thenExecute(() -> {
                    final StructureFrame frame = bar.frame();
                    final double down = Math.toDegrees(Math.atan2(frame.turnY(1, 0, 0), frame.turnX(1, 0, 0)));
                    helper.assertTrue(Math.abs(down + 50) < 2, "it hangs at its limit, 50 degrees down: " + down);
                    final Vec3 end = new Vec3(frame.worldX(bar.bounds().minX(), bar.bounds().minY() + 0.5,
                            bar.bounds().minZ() + 0.5), frame.worldY(bar.bounds().minX(), bar.bounds().minY() + 0.5,
                            bar.bounds().minZ() + 0.5), frame.worldZ(bar.bounds().minX(), bar.bounds().minY() + 0.5,
                            bar.bounds().minZ() + 0.5));
                    helper.assertTrue(end.distanceTo(hinge) < HOLDS, "on its hinge: " + end + " from " + hinge);
                    MovingStructures.takeAway(bar);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void joints_aBearingTurnsAPlatformAtItsMotorsSpeed(final GameTestHelper helper) {
        final MovingStructure platform = made(helper, square(3, 5, 3, 5, 5), Blocks.OAK_PLANKS);
        final Vec3 centre = new Vec3((platform.worldBox().minX + platform.worldBox().maxX) / 2,
                platform.worldBox().minY, (platform.worldBox().minZ + platform.worldBox().maxZ) / 2);
        StructureJoints.join(helper.getLevel(), new JointKind.Hinge(Optional.empty(), Optional.of(
                        JointKind.Drive.always(1.0, 1.0e6))), StructureJoints.Anchor.world(centre, new Vec3(0, 1, 0)),
                StructureJoints.Anchor.on(platform, centre, new Vec3(0, 1, 0)), Optional.empty());
        platform.move(PhysicsMotion.AT_REST);
        helper.runAfterDelay(40, () -> {
            final StructurePhysics physics = platform.physics();
            helper.assertTrue(physics != null, "it moves by its physics");
            final Vector3d spin = physics.body().spin();
            helper.assertTrue(Math.abs(spin.y - 1.0) < 0.02 && Math.abs(spin.x) < 0.02 && Math.abs(spin.z) < 0.02,
                    "it turns at a radian a second about the bearing's axis: " + spin);
            helper.assertTrue(Math.abs(platform.worldBox().minY - centre.y) < HOLDS,
                    "held up on its bearing: " + platform.worldBox());
            MovingStructures.takeAway(platform);
            helper.succeed();
        });
    }

    @GameTest(template = TALL, batch = BATCH, timeoutTicks = 400)
    public static void joints_aSpringHangsItsLoadStretchedByItsWeight(final GameTestHelper helper) {
        final MovingStructure crate = made(helper, List.of(new BlockPos(4, 14, 4)), Blocks.OAK_PLANKS);
        final Vec3 top = topOf(crate);
        final Vec3 anchor = top.add(0, 3, 0);
        final double stiffness = 20_000;
        StructureJoints.join(helper.getLevel(), new JointKind.Spring(3, stiffness, 3_000),
                StructureJoints.Anchor.on(crate, top, new Vec3(0, 1, 0)),
                StructureJoints.Anchor.world(anchor, new Vec3(0, 1, 0)), Optional.empty());
        crate.move(PhysicsMotion.AT_REST);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(asleep(crate), "it comes to rest on its spring"))
                .thenExecute(() -> {
                    final double mass = crate.physics().parts().mass().mass();
                    final double stretched = anchor.y - ropePoint(crate).y;
                    final double expected = 3 + mass * GRAVITY / stiffness;
                    helper.assertTrue(Math.abs(stretched - expected) < 0.02, "it hangs " + expected
                            + " under its anchor, as its weight of " + mass + " kg stretches it: " + stretched);
                    MovingStructures.takeAway(crate);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void joints_aTowBarKeepsTwoStructuresAtItsLength(final GameTestHelper helper) {
        final MovingStructure first = made(helper, List.of(new BlockPos(5, 2, 4)), Blocks.OAK_PLANKS);
        final MovingStructure second = made(helper, List.of(new BlockPos(2, 2, 4)), Blocks.OAK_PLANKS);
        final Vec3 hitch = middleOf(first);
        final Vec3 towed = middleOf(second);
        StructureJoints.join(helper.getLevel(), new JointKind.Bar(3), StructureJoints.Anchor.on(first, hitch,
                new Vec3(1, 0, 0)), StructureJoints.Anchor.on(second, towed, new Vec3(1, 0, 0)), Optional.empty());
        // Sliding on the floor, the two share the push and stop within a metre, as two crates on a tow bar would.
        first.move(PhysicsMotion.going(6, 0, 0));
        second.move(PhysicsMotion.AT_REST);
        final double startSecond = second.worldBox().minX;
        final double[] furthest = {0};
        helper.onEachTick(() -> furthest[0] = Math.max(furthest[0],
                Math.abs(middleOf(first).distanceTo(middleOf(second)) - 3)));
        helper.runAfterDelay(15, () -> {
            helper.assertTrue(furthest[0] < HOLDS, "the bar holds them at its length: off by " + furthest[0]);
            helper.assertTrue(second.worldBox().minX > startSecond + 0.3, "the towed one is pulled along: "
                    + second.worldBox().minX + " from " + startSecond);
            MovingStructures.takeAway(first);
            MovingStructures.takeAway(second);
            helper.succeed();
        });
    }

    @GameTest(template = TALL, batch = BATCH, timeoutTicks = 200)
    public static void joints_aRopePulledPastWhatItHoldsBreaks(final GameTestHelper helper) {
        // Two metres up: a stone falling much further breaks as it lands, as stone does.
        final MovingStructure crate = made(helper, List.of(new BlockPos(4, 4, 4)), Blocks.STONE);
        final Vec3 top = topOf(crate);
        final StructureJoint rope = StructureJoints.join(helper.getLevel(), new JointKind.Rope(2,
                        JointKind.RopeLook.ROPE, Optional.empty()),
                StructureJoints.Anchor.on(crate, top, new Vec3(0, 1, 0)),
                StructureJoints.Anchor.world(top.add(0, 2, 0), new Vec3(0, 1, 0)), Optional.of(100.0));
        crate.move(PhysicsMotion.AT_REST);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(StructureJoints.byId(helper.getLevel(), rope.id()) == null,
                        "the rope breaks under a weight it cannot hold"))
                .thenWaitUntil(() -> helper.assertTrue(asleep(crate), "and the crate falls and rests"))
                .thenExecute(() -> {
                    helper.assertTrue(Math.abs(crate.worldBox().minY - y(helper, 2)) < HOLDS,
                            "on the floor: " + crate.worldBox());
                    MovingStructures.takeAway(crate);
                })
                .thenSucceed();
    }

    @GameTest(template = TALL, batch = BATCH, timeoutTicks = 200)
    public static void joints_aWinchReelsItsRopeInByItsSignal(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final MovingStructure crate = made(helper, List.of(new BlockPos(4, 6, 4)), Blocks.OAK_PLANKS);
        final Vec3 top = topOf(crate);
        // The winch reads the signal at a place of the floor; a block of redstone set beside it lights it.
        final BlockPos switchAt = helper.absolutePos(new BlockPos(6, 2, 6));
        final JointKind.Winch winch = new JointKind.Winch(1.0, 0.5, 20, Optional.of(new JointSignal(
                JointSignal.Side.B, switchAt, BlockPhysics.SignalSide.ANY)), Optional.empty());
        final StructureJoint rope = StructureJoints.join(level, new JointKind.Rope(4, JointKind.RopeLook.CHAIN,
                        Optional.of(winch)), StructureJoints.Anchor.on(crate, top, new Vec3(0, 1, 0)),
                StructureJoints.Anchor.world(top.add(0, 4, 0), new Vec3(0, 1, 0)), Optional.empty());
        crate.move(PhysicsMotion.AT_REST);
        final double start = crate.worldBox().minY;
        helper.startSequence()
                .thenExecuteAfter(10, () -> helper.assertTrue(Math.abs(crate.worldBox().minY - start) < HOLDS,
                        "with no signal, the winch holds it where it hangs: " + crate.worldBox()))
                .thenExecute(() -> level.setBlock(switchAt.above(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3))
                .thenExecuteAfter(40, () -> {
                    final StructureJoint now = StructureJoints.byId(level, rope.id());
                    helper.assertTrue(now != null && now.kind() instanceof JointKind.Rope reeled
                                    && Math.abs(reeled.length() - 2) < 0.15,
                            "two seconds at a metre a second reel the rope in to two metres: " + now);
                    helper.assertTrue(crate.worldBox().minY > start + 1.7, "and lift the crate with it: "
                            + crate.worldBox());
                    MovingStructures.takeAway(crate);
                })
                .thenSucceed();
    }

    @GameTest(template = TALL, batch = BATCH, timeoutTicks = 200)
    public static void joints_aDrivenSliderLiftsItsLoadAlongItsLine(final GameTestHelper helper) {
        final MovingStructure lift = made(helper, List.of(new BlockPos(4, 3, 4)), Blocks.STONE);
        final Vec3 base = middleOf(lift);
        StructureJoints.join(helper.getLevel(), new JointKind.Slider(Optional.empty(), Optional.of(
                        JointKind.Drive.always(0.5, 1.0e6))), StructureJoints.Anchor.world(base, new Vec3(0, 1, 0)),
                StructureJoints.Anchor.on(lift, base, new Vec3(0, 1, 0)), Optional.empty());
        lift.move(PhysicsMotion.AT_REST);
        helper.runAfterDelay(40, () -> {
            final Vec3 now = middleOf(lift);
            helper.assertTrue(Math.abs(now.y - base.y - 1.0) < 0.1, "two seconds at half a metre a second lift it a"
                    + " metre: " + (now.y - base.y));
            helper.assertTrue(Math.abs(now.x - base.x) < HOLDS && Math.abs(now.z - base.z) < HOLDS,
                    "along its line: " + now + " from " + base);
            helper.assertTrue(lift.pose().angleTo(StructurePose.at(0, 0, 0)) < 0.01, "not turning: " + lift.pose());
            MovingStructures.takeAway(lift);
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 300)
    public static void joints_aWeldHoldsTwoStructuresAsOne(final GameTestHelper helper) {
        final MovingStructure upper = made(helper, List.of(new BlockPos(3, 7, 4)), Blocks.OAK_PLANKS);
        final MovingStructure lower = made(helper, List.of(new BlockPos(4, 6, 4)), Blocks.OAK_PLANKS);
        final Vec3 corner = new Vec3(upper.worldBox().maxX, upper.worldBox().minY, upper.worldBox().minZ + 0.5);
        StructureJoints.join(helper.getLevel(), JointKind.Weld.INSTANCE, StructureJoints.Anchor.on(upper, corner,
                new Vec3(0, 1, 0)), StructureJoints.Anchor.on(lower, corner, new Vec3(0, 1, 0)), Optional.empty());
        final double apartX = middleOf(lower).x - middleOf(upper).x;
        final double apartY = middleOf(lower).y - middleOf(upper).y;
        upper.move(PhysicsMotion.AT_REST);
        lower.move(PhysicsMotion.AT_REST);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(asleep(upper) && asleep(lower), "they come to rest"))
                .thenExecute(() -> {
                    helper.assertTrue(upper.pose().angleTo(lower.pose()) < 0.01, "turned as one: " + upper.pose()
                            + " and " + lower.pose());
                    final double nowX = middleOf(lower).x - middleOf(upper).x;
                    final double nowY = middleOf(lower).y - middleOf(upper).y;
                    final double turned = Math.hypot(nowX, nowY) - Math.hypot(apartX, apartY);
                    helper.assertTrue(Math.abs(turned) < HOLDS, "and as far apart as they were: " + turned);
                    MovingStructures.takeAway(upper);
                    MovingStructures.takeAway(lower);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void joints_areKeptWithTheWorld(final GameTestHelper helper) {
        final MovingStructure crate = made(helper, List.of(new BlockPos(4, 3, 4)), Blocks.OAK_PLANKS);
        final Vec3 top = topOf(crate);
        final JointKind.Winch winch = new JointKind.Winch(2, 1, 9, Optional.of(new JointSignal(JointSignal.Side.A,
                new BlockPos(1, 2, 3), BlockPhysics.SignalSide.TOP)), Optional.empty());
        final StructureJoint rope = StructureJoints.join(helper.getLevel(), new JointKind.Rope(3,
                        JointKind.RopeLook.CHAIN, Optional.of(winch)),
                StructureJoints.Anchor.on(crate, top, new Vec3(0, 1, 0)),
                StructureJoints.Anchor.world(top.add(0, 3, 0), new Vec3(0, 1, 0)), Optional.of(5_000.0));
        final StructureJoint hinge = StructureJoints.join(helper.getLevel(), new JointKind.Hinge(Optional.of(
                        new JointKind.Limits(-1, 1)), Optional.of(JointKind.Drive.always(2, 300))),
                StructureJoints.Anchor.on(crate, top, new Vec3(1, 0, 0)),
                StructureJoints.Anchor.world(top, new Vec3(1, 0, 0)), Optional.empty());
        final JointBook book = new JointBook(List.of(rope, hinge));
        final Tag written = JointBook.CODEC.encodeStart(NbtOps.INSTANCE, book).getOrThrow();
        final JointBook read = JointBook.CODEC.parse(NbtOps.INSTANCE, written).getOrThrow();
        helper.assertTrue(read.equals(book), "read back as written: " + read);
        MovingStructures.takeAway(crate);
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void joints_goWithAStructureTakenAway(final GameTestHelper helper) {
        final MovingStructure crate = made(helper, List.of(new BlockPos(4, 3, 4)), Blocks.OAK_PLANKS);
        final StructureJoint rope = StructureJoints.join(helper.getLevel(), new JointKind.Rope(3,
                        JointKind.RopeLook.ROPE, Optional.empty()),
                StructureJoints.Anchor.on(crate, topOf(crate), new Vec3(0, 1, 0)),
                StructureJoints.Anchor.world(topOf(crate).add(0, 3, 0), new Vec3(0, 1, 0)), Optional.empty());
        helper.assertTrue(StructureJoints.of(helper.getLevel(), crate.id()).contains(rope), "the crate has its rope");
        MovingStructures.takeAway(crate);
        helper.assertTrue(StructureJoints.byId(helper.getLevel(), rope.id()) == null, "the rope goes with it");
        helper.succeed();
    }

    @GameTest(template = TALL, batch = BATCH, timeoutTicks = 200)
    public static void joints_aRopeOnAStructureMovedByARuleCarriesItsLoad(final GameTestHelper helper) {
        final MovingStructure crane = made(helper, square(2, 20, 3, 3, 5), Blocks.STONE);
        final MovingStructure load = made(helper, List.of(new BlockPos(2, 15, 4)), Blocks.OAK_PLANKS);
        final Vec3 hook = new Vec3(middleOf(load).x, crane.worldBox().minY, middleOf(load).z);
        StructureJoints.join(helper.getLevel(), new JointKind.Rope(hook.distanceTo(topOf(load)),
                        JointKind.RopeLook.CHAIN, Optional.empty()),
                StructureJoints.Anchor.on(crane, hook, new Vec3(0, 1, 0)),
                StructureJoints.Anchor.on(load, topOf(load), new Vec3(0, 1, 0)), Optional.empty());
        load.move(PhysicsMotion.AT_REST);
        final StructurePose start = crane.pose();
        crane.move(new TowardMotion(start.movedTo(start.x() + 5, start.y(), start.z()), 2, 0));
        final double from = middleOf(load).x;
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(crane.resting(), "the crane arrives"))
                .thenExecuteAfter(40, () -> {
                    // Swinging under the hook after the crane stopped, a metre or so either way.
                    helper.assertTrue(middleOf(load).x > from + 3, "the load came along under it: "
                            + middleOf(load).x + " from " + from);
                    helper.assertTrue(load.worldBox().minY > y(helper, 10), "hanging, not fallen: "
                            + load.worldBox());
                    MovingStructures.takeAway(load);
                    MovingStructures.takeAway(crane);
                })
                .thenSucceed();
    }

    /* The world's place of the middle of the top face of a structure of one block, where its rope is tied. */
    private static Vec3 topOf(final MovingStructure structure) {
        final StructureFrame frame = structure.frame();
        final double x = structure.bounds().minX() + 0.5;
        final double y = structure.bounds().maxY() + 1.0;
        final double z = structure.bounds().minZ() + 0.5;
        return new Vec3(frame.worldX(x, y, z), frame.worldY(x, y, z), frame.worldZ(x, y, z));
    }

    /* Where the rope tied at a structure's top is now, as it turned. */
    private static Vec3 ropePoint(final MovingStructure structure) {
        return topOf(structure);
    }

    /* The world's place of the middle of a structure's box. */
    private static Vec3 middleOf(final MovingStructure structure) {
        final StructureFrame frame = structure.frame();
        final double x = (structure.bounds().minX() + structure.bounds().maxX() + 1) / 2.0;
        final double y = (structure.bounds().minY() + structure.bounds().maxY() + 1) / 2.0;
        final double z = (structure.bounds().minZ() + structure.bounds().maxZ() + 1) / 2.0;
        return new Vec3(frame.worldX(x, y, z), frame.worldY(x, y, z), frame.worldZ(x, y, z));
    }

    /* Makes the blocks {@code places} of {@code block} into a structure, its own box. */
    private static MovingStructure made(final GameTestHelper helper, final List<BlockPos> places, final Block block) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (final BlockPos place : places) {
            helper.setBlock(place, block);
            minX = Math.min(minX, place.getX());
            minY = Math.min(minY, place.getY());
            minZ = Math.min(minZ, place.getZ());
            maxX = Math.max(maxX, place.getX());
            maxY = Math.max(maxY, place.getY());
            maxZ = Math.max(maxZ, place.getZ());
        }
        warm(helper.getLevel());
        final StructureResult made = MovingStructures.assemble(helper.getLevel(),
                helper.absolutePos(new BlockPos(minX, minY, minZ)), helper.absolutePos(new BlockPos(maxX, maxY, maxZ)));
        helper.assertTrue(made.ok() && made.structure() != null, "a structure is made: "
                + (made.refusal() == null ? "done" : made.refusal().english()));
        return made.structure();
    }

    /* The places of a square layer at {@code y} from {@code (fromX, fromZ)} to {@code (toX, toZ)}. */
    private static List<BlockPos> square(final int fromX, final int y, final int fromZ, final int toX,
                                         final int toZ) {
        return BlockPos.betweenClosedStream(fromX, y, fromZ, toX, y, toZ).map(BlockPos::immutable).toList();
    }

    /* Whether the structure moves by its physics and has fallen asleep, at rest. */
    private static boolean asleep(final MovingStructure structure) {
        final StructurePhysics physics = structure.physics();
        return physics != null && physics.asleep();
    }

    /* The world's height of the arena's {@code y}. */
    private static double y(final GameTestHelper helper, final int y) {
        return helper.absolutePos(new BlockPos(0, y, 0)).getY();
    }

    /* Loads the chunks round the yard's next plot, so a structure made comes into it at once. */
    private static void warm(final ServerLevel level) {
        final int plot = StructureYard.nextPlot(level);
        final int cx = YardLayout.centreX(plot) >> 4;
        final int cz = YardLayout.centreZ(plot) >> 4;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                level.getChunk(cx + dx, cz + dz);
            }
        }
    }
}
