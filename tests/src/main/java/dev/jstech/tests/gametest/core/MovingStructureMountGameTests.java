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
import dev.jstech.core.moving.SpinMotion;
import dev.jstech.core.moving.StructureMount;
import dev.jstech.core.moving.StructurePose;
import dev.jstech.core.moving.StructureResult;
import dev.jstech.core.moving.StructureYard;
import dev.jstech.core.moving.TowardMotion;
import dev.jstech.core.moving.YardLayout;
import dev.jstech.core.moving.physics.PhysicsMotion;
import dev.jstech.core.moving.physics.StructurePhysics;
import dev.jstech.core.uuid.StructureUuid;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestPhysics;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Structures held by structures in a real world: a turret turns on a ship as the ship goes, a structure held falls
 * with its parent as one body of both their masses, a held structure's engine lifts its parent, one let go goes on at
 * its place's speed, the block holding one broken lets it go, what holds what is kept with the world, a structure is
 * never held by one it holds, and what a structure taken away held falls.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MovingStructureMountGameTests {

    private static final String ARENA = "empty";
    private static final String TALL = "tall";
    private static final String BATCH = "jscore_moving_mounts";
    private static final double CLOSE = 1.0e-6;
    private static final double RESTS = 0.03;

    private MovingStructureMountGameTests() {
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void mounts_aTurretTurnsOnAShipAsTheShipGoes(final GameTestHelper helper) {
        final MovingStructure ship = made(helper, square(2, 2, 3, 4, 5), Blocks.STONE);
        final MovingStructure turret = made(helper, List.of(new BlockPos(3, 3, 4)), Blocks.OAK_PLANKS);
        final StructureResult held = MovingStructures.mount(turret, ship, Optional.empty());
        helper.assertTrue(held.ok(), "the ship holds the turret");
        MovingStructures.move(turret, new SpinMotion(0, 1, 0, 90));
        final StructurePose start = ship.pose();
        MovingStructures.move(ship, new TowardMotion(start.movedTo(start.x() + 3, start.y(), start.z()), 2, 0));
        helper.runAfterDelay(40, () -> {
            final StructureMount mount = turret.mount();
            helper.assertTrue(mount != null, "still held");
            final StructurePose placed = ship.frame().placed(mount.local());
            helper.assertTrue(placed.distanceTo(turret.pose()) < CLOSE && placed.angleTo(turret.pose()) < CLOSE,
                    "it stands where the ship has it: " + turret.pose() + ", " + placed);
            helper.assertTrue(Math.abs(ship.pose().x() - start.x() - 3) < CLOSE, "the ship arrived");
            // Two seconds at ninety degrees a second: half a turn on the ship.
            final double turned = Math.toDegrees(turret.pose().angleTo(ship.pose()));
            helper.assertTrue(Math.abs(turned - 180) < 5, "turned half round on the ship: " + turned);
            MovingStructures.takeAway(turret);
            MovingStructures.takeAway(ship);
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 300)
    public static void mounts_aHeldStructureFallsWithItsParentAsOneBody(final GameTestHelper helper) {
        final MovingStructure base = made(helper, List.of(new BlockPos(4, 4, 4)), Blocks.STONE);
        final MovingStructure top = made(helper, List.of(new BlockPos(4, 5, 4)), Blocks.OAK_PLANKS);
        helper.assertTrue(MovingStructures.mount(top, base, Optional.empty()).ok(), "the base holds the top");
        base.move(PhysicsMotion.AT_REST);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(asleep(base), "they come to rest"))
                .thenExecute(() -> {
                    final StructurePhysics physics = base.physics();
                    final double own = physics.parts().mass().mass();
                    helper.assertTrue(physics.mass() > own * 1.05, "the body weighs both: " + physics.mass()
                            + " against its own " + own);
                    helper.assertTrue(Math.abs(base.worldBox().minY - y(helper, 2)) < RESTS, "on the floor: "
                            + base.worldBox());
                    helper.assertTrue(Math.abs(top.worldBox().minY - base.worldBox().maxY) < RESTS,
                            "the top on the base still: " + top.worldBox() + " on " + base.worldBox());
                    MovingStructures.takeAway(top);
                    MovingStructures.takeAway(base);
                })
                .thenSucceed();
    }

    @GameTest(template = TALL, batch = BATCH, timeoutTicks = 200)
    public static void mounts_aHeldEngineLiftsItsParent(final GameTestHelper helper) {
        final MovingStructure deck = made(helper, List.of(new BlockPos(4, 2, 4)), TestPhysics.LIGHT.get());
        helper.setBlock(new BlockPos(4, 3, 4), TestPhysics.THRUSTER.get().defaultBlockState()
                .setValue(DirectionalBlock.FACING, Direction.UP));
        helper.setBlock(new BlockPos(5, 3, 4), Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE,
                AttachFace.WALL).setValue(LeverBlock.FACING, Direction.EAST).setValue(LeverBlock.POWERED, true));
        final MovingStructure engine = assembled(helper, new BlockPos(4, 3, 4), new BlockPos(5, 3, 4));
        helper.assertTrue(MovingStructures.mount(engine, deck, Optional.empty()).ok(), "the deck holds the engine");
        deck.move(PhysicsMotion.AT_REST);
        final double start = deck.worldBox().minY;
        helper.runAfterDelay(15, () -> {
            helper.assertTrue(deck.worldBox().minY > start + 1, "the engine it holds lifts it: "
                    + deck.worldBox().minY + " from " + start);
            final StructurePose placed = deck.frame().placed(engine.mount().local());
            final StructurePose before = deck.previousFrame().placed(engine.mount().local());
            helper.assertTrue(placed.distanceTo(engine.pose()) < CLOSE && placed.angleTo(engine.pose()) < CLOSE,
                    "the engine on it still, as the deck has it: " + engine.pose() + ", " + placed
                            + "; from where the deck stood the tick before " + before.distanceTo(engine.pose())
                            + ", going "
                            + (deck.physics() == null ? "-" : deck.physics().body().velocity() + " turning "
                            + deck.physics().body().spin()));
            MovingStructures.takeAway(engine);
            MovingStructures.takeAway(deck);
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void mounts_oneLetGoGoesOnAtItsPlacesSpeed(final GameTestHelper helper) {
        final MovingStructure carrier = made(helper, square(1, 4, 3, 3, 5), Blocks.STONE);
        final MovingStructure load = made(helper, List.of(new BlockPos(2, 5, 4)), Blocks.OAK_PLANKS);
        helper.assertTrue(MovingStructures.mount(load, carrier, Optional.empty()).ok(), "held");
        final StructurePose start = carrier.pose();
        MovingStructures.move(carrier, new TowardMotion(start.movedTo(start.x() + 4, start.y(), start.z()), 2, 0));
        helper.runAfterDelay(10, () -> {
            MovingStructures.unmount(load, true);
            helper.assertTrue(load.mount() == null && load.motion() instanceof PhysicsMotion,
                    "let go, it moves by its physics");
            helper.runAfterDelay(1, () -> {
                helper.assertTrue(load.physics() != null && Math.abs(load.physics().body().velocity().x - 2) < 0.2,
                        "going on at two metres a second, as its place did: " + load.physics().body().velocity());
                MovingStructures.takeAway(load);
                MovingStructures.takeAway(carrier);
                helper.succeed();
            });
        });
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void mounts_theBlockHoldingOneBrokenLetsItGo(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final MovingStructure base = made(helper, square(3, 2, 3, 5, 5), Blocks.STONE);
        final MovingStructure door = made(helper, List.of(new BlockPos(4, 4, 4)), Blocks.OAK_PLANKS);
        final BlockPos hinge = centreOf(base);
        helper.assertTrue(MovingStructures.mount(door, base, Optional.of(hinge)).ok(), "held by its hinge");
        level.setBlock(hinge, Blocks.AIR.defaultBlockState(), 3);
        helper.assertTrue(door.mount() == null, "the hinge gone, it is let go");
        helper.assertTrue(door.motion() instanceof PhysicsMotion, "and falls");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(asleep(door), "it comes to rest"))
                .thenExecute(() -> {
                    MovingStructures.takeAway(door);
                    MovingStructures.takeAway(base);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void mounts_areKeptWithTheWorld(final GameTestHelper helper) {
        final StructureMount mount = new StructureMount(StructureUuid.random(),
                StructurePose.turned(12_000_003.5, 70, -8, 0, 1, 0, 0.7), Optional.of(new BlockPos(12_000_003, 69,
                -8)));
        final Tag written = StructureMount.CODEC.encodeStart(NbtOps.INSTANCE, mount).getOrThrow();
        final StructureMount read = StructureMount.CODEC.parse(NbtOps.INSTANCE, written).getOrThrow();
        helper.assertTrue(read.equals(mount), "read back as written: " + read);
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void mounts_aStructureIsNeverHeldByOneItHolds(final GameTestHelper helper) {
        final MovingStructure ship = made(helper, List.of(new BlockPos(3, 2, 4)), Blocks.STONE);
        final MovingStructure boat = made(helper, List.of(new BlockPos(5, 2, 4)), Blocks.STONE);
        helper.assertTrue(MovingStructures.mount(boat, ship, Optional.empty()).ok(), "the ship holds the boat");
        helper.assertTrue(!MovingStructures.mount(ship, boat, Optional.empty()).ok(),
                "the boat cannot hold the ship that holds it");
        helper.assertTrue(!MovingStructures.mount(ship, ship, Optional.empty()).ok(), "nor can the ship hold itself");
        MovingStructures.takeAway(boat);
        MovingStructures.takeAway(ship);
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void mounts_whatAStructureTakenAwayHeldFalls(final GameTestHelper helper) {
        final MovingStructure ship = made(helper, square(3, 5, 3, 5, 5), Blocks.STONE);
        final MovingStructure crate = made(helper, List.of(new BlockPos(4, 6, 4)), Blocks.OAK_PLANKS);
        helper.assertTrue(MovingStructures.mount(crate, ship, Optional.empty()).ok(), "held");
        MovingStructures.takeAway(ship);
        helper.assertTrue(crate.mount() == null && crate.motion() instanceof PhysicsMotion, "let go, falling");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(asleep(crate), "it comes to rest"))
                .thenExecute(() -> {
                    helper.assertTrue(Math.abs(crate.worldBox().minY - y(helper, 2)) < RESTS, "on the floor: "
                            + crate.worldBox());
                    MovingStructures.takeAway(crate);
                })
                .thenSucceed();
    }

    /* The middle block of the structure's box in the yard. */
    private static BlockPos centreOf(final MovingStructure structure) {
        return new BlockPos((structure.bounds().minX() + structure.bounds().maxX()) / 2,
                (structure.bounds().minY() + structure.bounds().maxY()) / 2,
                (structure.bounds().minZ() + structure.bounds().maxZ()) / 2);
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
