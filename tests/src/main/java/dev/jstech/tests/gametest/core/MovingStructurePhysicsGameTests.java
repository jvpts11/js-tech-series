/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.air.AirMix;
import dev.jstech.core.jstechseries.Element;
import dev.jstech.core.jstechseries.Elements;
import dev.jstech.core.moving.IStructureMotion;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.StructureFrame;
import dev.jstech.core.moving.StructureMotions;
import dev.jstech.core.moving.StructurePose;
import dev.jstech.core.moving.StructureBalance;
import dev.jstech.core.moving.StructureResult;
import dev.jstech.core.moving.StructureYard;
import dev.jstech.core.moving.TowardMotion;
import dev.jstech.core.moving.YardLayout;
import dev.jstech.core.moving.physics.ForceSum;
import dev.jstech.core.moving.physics.PhysicsMotion;
import dev.jstech.core.moving.physics.StructurePhysics;
import dev.jstech.core.region.Box;
import dev.jstech.core.room.Room;
import dev.jstech.core.room.SealedRooms;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestPhysics;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The physics of moving structures in a real world: a block let go falls and rests on the floor, a bar dropped tilted
 * comes to lie flat, one block lands and rests on another, a structure moved by a rule carries one resting on it, a
 * raft floats and a stone sinks, a thruster lifts, a levitator holds, a magnet pulls up to iron, a cart rests on its
 * springs and drives, a balloon of hydrogen and a hot air balloon rise, a heavy block breaks the glass it falls on, a
 * structure asleep wakes when its floor goes, a hole in a room aboard pulls whoever is in it, and how fast one goes is
 * saved with it. The arena's floor is its layer at y 1, so a block resting on it stands from y 2.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MovingStructurePhysicsGameTests {

    private static final String ARENA = "empty";
    /* An arena 32 high, for what flies up. */
    private static final String TALL = "tall";
    private static final String BATCH = "jscore_moving_physics";
    /* How close, in blocks, a resting structure is to where it should rest. */
    private static final double RESTS = 0.03;
    /* The floor's top, in the arena's places. */
    private static final int FLOOR = 2;
    /* How long the weather a test clears stays clear, in ticks. */
    private static final int CLEAR = 12_000;

    private MovingStructurePhysicsGameTests() {
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void physics_aBlockLetGoFallsAndRestsOnTheFloor(final GameTestHelper helper) {
        final MovingStructure block = made(helper, List.of(new BlockPos(4, 4, 4)), Blocks.STONE);
        final double startX = block.worldBox().minX;
        block.move(PhysicsMotion.AT_REST);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(asleep(block), "it comes to rest"))
                .thenExecute(() -> {
                    final AABB box = block.worldBox();
                    helper.assertTrue(Math.abs(box.minY - y(helper, FLOOR)) < RESTS,
                            "it rests on the floor: " + box);
                    helper.assertTrue(Math.abs(box.minX - startX) < RESTS, "where it fell, not slid: " + box);
                    helper.assertTrue(block.pose().angleTo(StructurePose.at(0, 0, 0)) < 0.01,
                            "lying as it was: " + block.pose());
                    MovingStructures.takeAway(block);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 300)
    public static void physics_aBarDroppedTiltedComesToLieFlat(final GameTestHelper helper) {
        final MovingStructure bar = made(helper, List.of(new BlockPos(3, 4, 4), new BlockPos(4, 4, 4),
                new BlockPos(5, 4, 4)), Blocks.STONE);
        final StructurePose was = bar.pose();
        bar.teleport(StructurePose.turned(was.x(), was.y() + 1, was.z(), 0, 0, 1, Math.toRadians(25)));
        bar.move(PhysicsMotion.AT_REST);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(asleep(bar), "it comes to rest"))
                .thenExecute(() -> {
                    final StructureFrame frame = bar.frame();
                    final double upright = Math.max(Math.abs(frame.rotation(1, 0)), Math.max(
                            Math.abs(frame.rotation(1, 1)), Math.abs(frame.rotation(1, 2))));
                    helper.assertTrue(upright > 0.999, "one of its sides lies flat: " + bar.pose());
                    helper.assertTrue(Math.abs(bar.worldBox().minY - y(helper, FLOOR)) < RESTS,
                            "on the floor: " + bar.worldBox());
                    MovingStructures.takeAway(bar);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 300)
    public static void physics_aBlockLandsOnAnotherAndRestsOnIt(final GameTestHelper helper) {
        final MovingStructure lower = made(helper, List.of(new BlockPos(4, 2, 4)), Blocks.STONE);
        final MovingStructure upper = made(helper, List.of(new BlockPos(4, 5, 4)), Blocks.OAK_PLANKS);
        lower.move(PhysicsMotion.AT_REST);
        upper.move(PhysicsMotion.AT_REST);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(asleep(lower) && asleep(upper), "both come to rest"))
                .thenExecute(() -> {
                    helper.assertTrue(Math.abs(lower.worldBox().minY - y(helper, FLOOR)) < RESTS,
                            "the lower on the floor: " + lower.worldBox());
                    helper.assertTrue(Math.abs(upper.worldBox().minY - lower.worldBox().maxY) < RESTS,
                            "the upper on the lower: " + upper.worldBox() + " on " + lower.worldBox());
                    MovingStructures.takeAway(lower);
                    MovingStructures.takeAway(upper);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 300)
    public static void physics_aStructureMovedByARuleCarriesOneRestingOnIt(final GameTestHelper helper) {
        final MovingStructure lift = made(helper, square(3, 2, 3, 5, 5), Blocks.STONE);
        final MovingStructure crate = made(helper, List.of(new BlockPos(4, 3, 4)), Blocks.OAK_PLANKS);
        crate.move(PhysicsMotion.AT_REST);
        final StructurePose start = lift.pose();
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(asleep(crate), "the crate settles on the lift"))
                .thenExecute(() -> lift.move(new TowardMotion(start.movedTo(start.x(), start.y() + 2, start.z()),
                        2, 0)))
                .thenWaitUntil(() -> helper.assertTrue(lift.resting(), "the lift arrives"))
                .thenWaitUntil(() -> helper.assertTrue(asleep(crate), "the crate settles again"))
                .thenExecute(() -> {
                    helper.assertTrue(Math.abs(crate.worldBox().minY - lift.worldBox().maxY) < RESTS,
                            "the crate rode up on the lift: " + crate.worldBox() + " on " + lift.worldBox());
                    MovingStructures.takeAway(crate);
                    MovingStructures.takeAway(lift);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 400)
    public static void physics_aRaftFloatsAndAStoneSinks(final GameTestHelper helper) {
        calm(helper);
        pool(helper);
        final MovingStructure raft = made(helper, square(3, 5, 3, 5, 5), Blocks.OAK_PLANKS);
        final MovingStructure stone = made(helper, List.of(new BlockPos(2, 5, 2)), Blocks.STONE);
        raft.move(PhysicsMotion.AT_REST);
        stone.move(PhysicsMotion.AT_REST);
        // A still pool's top source block is filled to eight ninths; planks, of 600 kilograms a cubic metre, float
        // six tenths in.
        final double floats = y(helper, 4) + 8.0 / 9 - 0.6;
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(asleep(raft) && asleep(stone), "both come to rest"))
                .thenExecute(() -> {
                    final AABB box = raft.worldBox();
                    helper.assertTrue(Math.abs(box.minY - floats) < 0.05,
                            "the raft floats six tenths in: " + box + ", the water's line at " + floats);
                    helper.assertTrue(Math.abs(stone.worldBox().minY - y(helper, FLOOR)) < RESTS,
                            "the stone sank to the bottom: " + stone.worldBox());
                    MovingStructures.takeAway(raft);
                    MovingStructures.takeAway(stone);
                })
                .thenSucceed();
    }

    @GameTest(template = TALL, batch = BATCH, timeoutTicks = 200)
    public static void physics_aThrusterLiftsAndItsSignalThrottlesIt(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        helper.setBlock(new BlockPos(4, 2, 4), TestPhysics.THRUSTER.get().defaultBlockState()
                .setValue(DirectionalBlock.FACING, Direction.UP));
        helper.setBlock(new BlockPos(5, 2, 4), lever(Direction.EAST, false));
        final MovingStructure rocket = assembled(helper, new BlockPos(4, 2, 4), new BlockPos(5, 2, 4));
        rocket.move(PhysicsMotion.AT_REST);
        final double start = rocket.worldBox().minY;
        helper.startSequence()
                .thenExecuteAfter(10, () -> helper.assertTrue(Math.abs(rocket.worldBox().minY - start) < RESTS,
                        "unlit, it stays on the floor: " + rocket.worldBox()))
                .thenExecute(() -> {
                    final BlockPos lever = find(level, rocket, state -> state.is(Blocks.LEVER));
                    helper.assertTrue(lever != null, "the lever is aboard");
                    level.setBlock(lever, level.getBlockState(lever).setValue(LeverBlock.POWERED, true), 3);
                })
                .thenExecuteAfter(8, () -> {
                    helper.assertTrue(rocket.worldBox().minY > start + 1, "lit, it climbs: " + rocket.worldBox());
                    final StructurePhysics physics = rocket.physics();
                    helper.assertTrue(physics != null && physics.body().velocity().y > 1,
                            "and goes up faster and faster");
                    MovingStructures.takeAway(rocket);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void physics_aLevitatorHoldsItUp(final GameTestHelper helper) {
        final MovingStructure hover = made(helper, List.of(new BlockPos(4, 4, 4)), TestPhysics.HOVER.get());
        final double start = hover.worldBox().minY;
        hover.move(PhysicsMotion.AT_REST);
        helper.startSequence()
                .thenExecuteAfter(40, () -> {
                    helper.assertTrue(Math.abs(hover.worldBox().minY - start) < RESTS,
                            "it hangs where it was let go: " + hover.worldBox());
                    MovingStructures.takeAway(hover);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void physics_aMagnetPullsItUpToIron(final GameTestHelper helper) {
        helper.setBlock(new BlockPos(4, 5, 4), Blocks.IRON_BLOCK);
        final MovingStructure magnet = made(helper, List.of(new BlockPos(4, 2, 4)), TestPhysics.MAGNET.get());
        magnet.move(PhysicsMotion.AT_REST);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(asleep(magnet), "it comes to rest"))
                .thenExecute(() -> {
                    helper.assertTrue(Math.abs(magnet.worldBox().maxY - y(helper, 5)) < RESTS,
                            "it hangs from the iron: " + magnet.worldBox());
                    MovingStructures.takeAway(magnet);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 300)
    public static void physics_aCartRestsOnItsSpringsAndDrives(final GameTestHelper helper) {
        calm(helper);
        final ServerLevel level = helper.getLevel();
        for (final BlockPos wheel : List.of(new BlockPos(3, 2, 3), new BlockPos(5, 2, 3), new BlockPos(3, 2, 5),
                new BlockPos(5, 2, 5))) {
            helper.setBlock(wheel, TestPhysics.WHEEL.get().defaultBlockState()
                    .setValue(DirectionalBlock.FACING, Direction.EAST));
        }
        for (final BlockPos deck : square(3, 3, 3, 5, 5)) {
            helper.setBlock(deck, TestPhysics.LIGHT.get());
        }
        // The driving wheels' signals come from behind them, as they face east: from the west.
        helper.setBlock(new BlockPos(2, 2, 3), lever(Direction.WEST, false));
        helper.setBlock(new BlockPos(2, 2, 5), lever(Direction.WEST, false));
        final MovingStructure cart = assembled(helper, new BlockPos(2, 2, 3), new BlockPos(5, 3, 5));
        cart.move(PhysicsMotion.AT_REST);
        final double[] parked = new double[1];
        // Free to roll, a cart this light creeps in the wind, as a real one does: it is waited for to stop bouncing.
        helper.startSequence()
                .thenExecuteAfter(40, () -> { })
                .thenWaitUntil(() -> helper.assertTrue(cart.physics() != null
                        && Math.abs(cart.physics().body().velocity().y) < 0.1, "it settles on its springs"))
                .thenExecute(() -> {
                    final AABB box = cart.worldBox();
                    helper.assertTrue(box.minY > y(helper, FLOOR) + 0.2,
                            "held off the floor by its wheels: " + box);
                    parked[0] = box.minX;
                    for (final BlockPos lever : findAll(level, cart, state -> state.is(Blocks.LEVER))) {
                        level.setBlock(lever, level.getBlockState(lever).setValue(LeverBlock.POWERED, true), 3);
                    }
                })
                .thenExecuteAfter(12, () -> {
                    helper.assertTrue(cart.worldBox().minX > parked[0] + 0.2,
                            "driven, it rolls east: " + cart.worldBox().minX + " from " + parked[0]);
                    MovingStructures.takeAway(cart);
                })
                .thenSucceed();
    }

    @GameTest(template = TALL, batch = BATCH, timeoutTicks = 400)
    public static void physics_aBalloonOfHydrogenRises(final GameTestHelper helper) {
        calm(helper);
        final ServerLevel level = helper.getLevel();
        final MovingStructure balloon = made(helper, shell(2, 2, 2, 7), TestPhysics.ENVELOPE.get());
        final BlockPos inside = centreOf(balloon);
        helper.assertTrue(level.getBlockState(inside).isAir(), "its middle is air");
        SealedRooms.track(level, inside);
        final double[] start = new double[1];
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(SealedRooms.roomAt(level, inside).isPresent(),
                        "its inside is found a room"))
                .thenExecute(() -> {
                    final Room room = SealedRooms.roomAt(level, inside).orElseThrow();
                    final double moles = room.gases().values().stream().mapToDouble(Double::doubleValue).sum();
                    room.empty();
                    room.add(Elements.of(Element.HYDROGEN).id(), moles);
                    start[0] = balloon.worldBox().minY;
                    balloon.move(PhysicsMotion.AT_REST);
                })
                .thenExecuteAfter(30, () -> {
                    helper.assertTrue(balloon.worldBox().minY > start[0] + 0.5,
                            "filled with hydrogen, it rises: " + balloon.worldBox().minY + " from " + start[0]
                                    + forcesOf(balloon));
                    MovingStructures.takeAway(balloon);
                })
                .thenSucceed();
    }

    @GameTest(template = TALL, batch = BATCH, timeoutTicks = 600)
    public static void physics_aHotAirBalloonRisesAsItsBurnerWarmsIt(final GameTestHelper helper) {
        calm(helper);
        final List<BlockPos> envelope = shell(2, 2, 2, 7);
        envelope.remove(new BlockPos(5, 2, 5));
        final MovingStructure balloon = made(helper, envelope, TestPhysics.ENVELOPE.get(),
                new BlockPos(5, 2, 5), TestPhysics.BURNER.get());
        final double start = balloon.worldBox().minY;
        balloon.move(PhysicsMotion.AT_REST);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(balloon.worldBox().minY > start + 1,
                        "warmed, it rises: " + balloon.worldBox().minY + " from " + start))
                .thenExecute(() -> MovingStructures.takeAway(balloon))
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void physics_aHeavyBlockBreaksTheGlassItFallsOn(final GameTestHelper helper) {
        helper.setBlock(new BlockPos(4, 2, 4), Blocks.GLASS);
        final MovingStructure iron = made(helper, List.of(new BlockPos(4, 5, 4)), Blocks.IRON_BLOCK);
        iron.move(PhysicsMotion.AT_REST);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(asleep(iron), "it comes to rest"))
                .thenExecute(() -> {
                    helper.assertBlockPresent(Blocks.AIR, new BlockPos(4, 2, 4));
                    helper.assertTrue(Math.abs(iron.worldBox().minY - y(helper, FLOOR)) < RESTS,
                            "it went through the glass to the floor: " + iron.worldBox());
                    helper.assertTrue(find(helper.getLevel(), iron, state -> state.is(Blocks.IRON_BLOCK)) != null,
                            "the iron itself held");
                    MovingStructures.takeAway(iron);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 300)
    public static void physics_aStructureAsleepWakesWhenItsFloorGoes(final GameTestHelper helper) {
        helper.setBlock(new BlockPos(4, 2, 4), Blocks.STONE);
        final MovingStructure block = made(helper, List.of(new BlockPos(4, 3, 4)), Blocks.OAK_PLANKS);
        block.move(PhysicsMotion.AT_REST);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(asleep(block), "it falls asleep on the stone"))
                .thenExecute(() -> helper.setBlock(new BlockPos(4, 2, 4), Blocks.AIR))
                .thenWaitUntil(() -> helper.assertTrue(asleep(block)
                        && Math.abs(block.worldBox().minY - y(helper, FLOOR)) < RESTS,
                        "it wakes, falls and rests on the floor: " + block.worldBox()))
                .thenExecute(() -> MovingStructures.takeAway(block))
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 600)
    public static void rooms_aHoleInARoomAboardPullsWhoeverIsInItTowardsIt(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final MovingStructure cabin = made(helper, shell(2, 2, 2, 5), Blocks.STONE);
        final BlockPos inside = centreOf(cabin);
        SealedRooms.track(level, inside);
        final Box bounds = cabin.bounds();
        // The middle of its east wall, in the yard.
        final BlockPos wall = new BlockPos(bounds.maxX(), bounds.minY() + 2, bounds.minZ() + 2);
        final ItemEntity[] item = new ItemEntity[1];
        final double[] start = new double[1];
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(SealedRooms.roomAt(level, inside).isPresent(),
                        "its inside is found a room"))
                .thenExecute(() -> {
                    SealedRooms.roomAt(level, inside).orElseThrow().add(AirMix.NITROGEN, 3_000);
                    final Vec3 at = helper.absoluteVec(new Vec3(4.5, 3, 4.5));
                    item[0] = new ItemEntity(level, at.x, at.y, at.z, new ItemStack(Items.PAPER));
                    item[0].setDeltaMovement(Vec3.ZERO);
                    level.addFreshEntity(item[0]);
                    start[0] = item[0].getX();
                    level.setBlock(wall, Blocks.AIR.defaultBlockState(), 3);
                })
                .thenWaitUntil(() -> helper.assertTrue(item[0].getX() > start[0] + 0.3,
                        "the air rushing out pulls a sheet of paper east, to the hole: " + item[0].getX() + " from "
                                + start[0]))
                .thenExecute(() -> {
                    item[0].discard();
                    MovingStructures.takeAway(cabin);
                })
                .thenSucceed();
    }

    @GameTest(template = TALL, batch = "jscore_moving_physics_budget", timeoutTicks = 200)
    public static void physics_aTightBudgetSlowsThemAllAndStarvesNone(final GameTestHelper helper) {
        final List<MovingStructure> falling = new ArrayList<>();
        for (int x = 1; x <= 10; x += 3) {
            for (int z = 1; z <= 10; z += 3) {
                falling.add(made(helper, List.of(new BlockPos(x, 28, z)), Blocks.STONE));
            }
        }
        final double start = falling.getFirst().worldBox().minY;
        final double was = StructureBalance.physicsMillis();
        helper.startSequence()
                .thenExecute(() -> {
                    StructureBalance.physicsMillis(0.1);
                    for (final MovingStructure each : falling) {
                        each.move(PhysicsMotion.AT_REST);
                    }
                })
                .thenExecuteAfter(20, () -> {
                    StructureBalance.physicsMillis(was);
                    double least = Double.MAX_VALUE;
                    double most = 0;
                    for (final MovingStructure each : falling) {
                        final double fell = start - each.worldBox().minY;
                        least = Math.min(least, fell);
                        most = Math.max(most, fell);
                    }
                    // A second falling free is 4.9 metres.
                    helper.assertTrue(least > 1, "every one falls, none left waiting: the least fell " + least);
                    helper.assertTrue(least < 4.5, "the budget slows some: the least fell " + least + ", the most "
                            + most);
                    for (final MovingStructure each : falling) {
                        MovingStructures.takeAway(each);
                    }
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void physics_motionSavesHowFastItGoes(final GameTestHelper helper) {
        final PhysicsMotion going = new PhysicsMotion(1.5, -2, 0.25, 0, 0.5, 0,
                List.of(new PhysicsMotion.WarmRoom(7, 350)));
        final Tag written = StructureMotions.CODEC.encodeStart(NbtOps.INSTANCE, going).getOrThrow();
        final Optional<IStructureMotion> read = StructureMotions.CODEC.parse(NbtOps.INSTANCE, written).result();
        helper.assertTrue(read.isPresent() && read.get().equals(going), "read back as it was: " + read);
        helper.succeed();
    }

    /* Makes the blocks {@code places} of {@code block} into a structure, its own box. */
    private static MovingStructure made(final GameTestHelper helper, final List<BlockPos> places, final Block block) {
        return made(helper, places, block, null, null);
    }

    /* Makes {@code places} of {@code block}, and one {@code other} block at {@code at}, into a structure. */
    private static MovingStructure made(final GameTestHelper helper, final List<BlockPos> places, final Block block,
                                        final BlockPos at, final Block other) {
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
        if (at != null) {
            helper.setBlock(at, other);
        }
        return assembled(helper, new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ));
    }

    /* Makes every block of the arena's box from {@code from} to {@code to} into a structure. */
    private static MovingStructure assembled(final GameTestHelper helper, final BlockPos from, final BlockPos to) {
        warm(helper.getLevel());
        final StructureResult made = MovingStructures.assemble(helper.getLevel(), helper.absolutePos(from),
                helper.absolutePos(to));
        helper.assertTrue(made.ok() && made.structure() != null, "a structure is made: "
                + (made.refusal() == null ? "done" : made.refusal().english()));
        return made.structure();
    }

    /* The places of a square layer at {@code y} from {@code (fromX, fromZ)} to {@code (toX, toZ)}. */
    private static List<BlockPos> square(final int fromX, final int y, final int fromZ, final int toX,
                                         final int toZ) {
        final List<BlockPos> places = new ArrayList<>();
        for (int x = fromX; x <= toX; x++) {
            for (int z = fromZ; z <= toZ; z++) {
                places.add(new BlockPos(x, y, z));
            }
        }
        return places;
    }

    /* The places of the hollow cube of side {@code side} from {@code (x, y, z)}: its walls only. */
    private static List<BlockPos> shell(final int x, final int y, final int z, final int side) {
        final List<BlockPos> places = new ArrayList<>();
        for (int dx = 0; dx < side; dx++) {
            for (int dy = 0; dy < side; dy++) {
                for (int dz = 0; dz < side; dz++) {
                    if (dx == 0 || dy == 0 || dz == 0 || dx == side - 1 || dy == side - 1 || dz == side - 1) {
                        places.add(new BlockPos(x + dx, y + dy, z + dz));
                    }
                }
            }
        }
        return places;
    }

    /* A pool of water three deep from x and z 2 to 6, walled in glass, its bottom the floor. */
    private static void pool(final GameTestHelper helper) {
        for (int y = 2; y <= 4; y++) {
            for (int x = 1; x <= 7; x++) {
                for (int z = 1; z <= 7; z++) {
                    final boolean wall = x == 1 || x == 7 || z == 1 || z == 7;
                    helper.setBlock(new BlockPos(x, y, z), wall ? Blocks.GLASS : Blocks.WATER);
                }
            }
        }
    }

    /* A lever on a wall, facing away from the block it is on, lit or not. */
    private static BlockState lever(final Direction facing, final boolean lit) {
        return Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.WALL)
                .setValue(LeverBlock.FACING, facing).setValue(LeverBlock.POWERED, lit);
    }

    /*
     * Clears the weather, so the wind blows as on a calm day: a storm another test brought would blow a light raft or
     * a balloon against the arena's wall, as it would a real one.
     */
    private static void calm(final GameTestHelper helper) {
        helper.getLevel().setWeatherParameters(CLEAR, 0, false, false);
    }

    /* The forces on the structure in its last tick, for a test's failure to tell. */
    private static String forcesOf(final MovingStructure structure) {
        final StructurePhysics physics = structure.physics();
        if (physics == null) {
            return "; not moving by its physics";
        }
        final StringBuilder said = new StringBuilder("; asleep ").append(physics.asleep());
        for (final ForceSum.ForceKind kind : ForceSum.ForceKind.values()) {
            said.append(", ").append(kind.toString().toLowerCase(Locale.ROOT)).append(' ')
                    .append(physics.forces().of(kind));
        }
        return said.toString();
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

    /* The middle block of the structure's box in the yard. */
    private static BlockPos centreOf(final MovingStructure structure) {
        final Box box = structure.bounds();
        return new BlockPos((box.minX() + box.maxX()) / 2, (box.minY() + box.maxY()) / 2,
                (box.minZ() + box.maxZ()) / 2);
    }

    /* The first block of the structure's yard that is so, or null. */
    private static BlockPos find(final ServerLevel level, final MovingStructure structure,
                                 final Predicate<BlockState> wanted) {
        final List<BlockPos> all = findAll(level, structure, wanted);
        return all.isEmpty() ? null : all.get(0);
    }

    /* Every block of the structure's yard that is so. */
    private static List<BlockPos> findAll(final ServerLevel level, final MovingStructure structure,
                                          final Predicate<BlockState> wanted) {
        final List<BlockPos> found = new ArrayList<>();
        final Box box = structure.bounds();
        for (final BlockPos at : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(),
                box.maxZ())) {
            if (wanted.test(level.getBlockState(at))) {
                found.add(at.immutable());
            }
        }
        return found;
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
