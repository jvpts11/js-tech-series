/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import com.simibubi.create.api.contraption.BlockMovementChecks;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.bearing.MechanicalBearingBlockEntity;
import dev.jstech.core.integration.create.CreateIntegration;
import dev.jstech.core.integration.sable.SableIntegration;
import dev.jstech.core.moving.ForeignMover;
import dev.jstech.core.moving.ForeignMovers;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.StructureFrame;
import dev.jstech.core.moving.StructurePose;
import dev.jstech.core.moving.StructureResult;
import dev.jstech.core.moving.StructureYard;
import dev.jstech.core.moving.YardLayout;
import dev.jstech.core.moving.physics.PhysicsMotion;
import dev.jstech.core.moving.physics.StructurePhysics;
import dev.jstech.core.region.Box;
import dev.jstech.tests.JsTests;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The layer between the kit's moving structures and those of Create and of Sable, the one Create Aeronautics flies:
 * it is on exactly when their mods are; Create moves no block a structure of the kit's keeps; a contraption turning
 * on a bearing is met where its entity places it, as fast as it turns, and a block dropped on it rests on it; a
 * sub-level of Sable's is met where it stands, and one falling as fast as it falls, a block dropped on it rests on
 * it, a place of its plot is where the sub-level has it, the kit takes none of its blocks, and its plots and the yard
 * are apart. Each test with a mod absent checks only that the layer is off; run them with {@code -PwithAeronautics}
 * for the mods. The classes of the mods are reached only from the nested classes, which load only when their mod is
 * present. A bearing holds its contraption only while it turns.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MovingStructureLayerGameTests {

    private static final String ARENA = "empty";
    /* An arena 32 high, for a block dropped on what turns above a bearing. */
    private static final String TALL = "tall";
    private static final String BATCH = "jscore_moving_layer";
    /* Its own batch, run after the others: a sub-level falls by Sable's physics, which they hold still. */
    private static final String FLIGHT = "jscore_moving_layer_flight";
    /* How close, in blocks, a resting structure is to where it should rest. */
    private static final double RESTS = 0.05;
    /* How close, in blocks, a sub-level stands to where its blocks stood, settled on the floor. */
    private static final double STANDS = 0.25;
    /* A bearing at 16 turns a minute turns 4.8 degrees a tick, about the axis it faces. */
    private static final double BEARING_SPIN = Math.toRadians(4.8) * 20;

    private MovingStructureLayerGameTests() {
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void layer_isOnExactlyWhenItsModsArePresent(final GameTestHelper helper) {
        helper.assertTrue(CreateIntegration.meeting() == create(), "the kit meets Create's contraptions exactly when"
                + " Create is present: " + CreateIntegration.meeting());
        helper.assertTrue(SableIntegration.meeting() == sable(), "the kit meets Sable's sub-levels exactly when"
                + " Sable is present: " + SableIntegration.meeting());
        helper.assertTrue(ForeignMovers.any() == (create() || sable()), "and has other mods' structures to meet"
                + " only then");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void layer_createMovesNoBlockAStructureKeeps(final GameTestHelper helper) {
        if (!create()) {
            helper.assertFalse(CreateIntegration.meeting(), "without Create the layer is off");
            helper.succeed();
            return;
        }
        final MovingStructure block = made(helper, new BlockPos(4, 2, 4), Blocks.STONE);
        final Box kept = block.bounds();
        final BlockPos yard = new BlockPos(kept.minX(), kept.minY(), kept.minZ());
        helper.setBlock(new BlockPos(2, 2, 2), Blocks.STONE);
        helper.assertFalse(WithCreate.movable(helper.getLevel(), yard), "Create moves no block of a structure: "
                + yard.toShortString());
        helper.assertTrue(WithCreate.movable(helper.getLevel(), helper.absolutePos(new BlockPos(2, 2, 2))),
                "and moves the world's own");
        MovingStructures.takeAway(block);
        helper.succeed();
    }

    @GameTest(template = TALL, batch = BATCH, timeoutTicks = 300)
    public static void layer_aBlockDroppedOnATurningContraptionRestsOnIt(final GameTestHelper helper) {
        if (!create()) {
            helper.assertFalse(CreateIntegration.meeting(), "without Create the layer is off");
            helper.succeed();
            return;
        }
        final BlockPos bearing = turningBearing(helper);
        final MovingStructure[] crate = new MovingStructure[1];
        final StructurePose[] landed = new StructurePose[1];
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(WithCreate.isContraption(helper, bearing),
                        "the bearing, turned, takes the stone into its contraption"))
                .thenExecute(() -> {
                    crate[0] = made(helper, new BlockPos(6, 8, 6), Blocks.OAK_PLANKS);
                    crate[0].move(PhysicsMotion.AT_REST);
                })
                .thenWaitUntil(() -> helper.assertTrue(Math.abs(crate[0].worldBox().minY - y(helper, 6)) < RESTS,
                        "the crate lands on the contraption's stone: " + crate[0].worldBox()))
                .thenIdle(5)
                .thenExecute(() -> landed[0] = crate[0].pose())
                .thenIdle(20)
                .thenExecute(() -> {
                    final List<ForeignMover> near = ForeignMovers.near(helper.getLevel(), arena(helper));
                    helper.assertTrue(near.size() == 1 && "Create".equals(near.get(0).name()),
                            "the contraption is met: " + near);
                    helper.assertTrue(Math.abs(crate[0].worldBox().minY - y(helper, 6)) < RESTS,
                            "and rests there as it turns, above the bearing: " + crate[0].worldBox());
                    // In a second the bearing turns 96 degrees: the crate on it, held by its grip, turns with it.
                    final double turned = Math.toDegrees(crate[0].pose().angleTo(landed[0]));
                    helper.assertTrue(turned > 60, "carried round as it turns: " + turned + " degrees");
                    final ItemEntity item = new ItemEntity(helper.getLevel(), 0, 0, 0, new ItemStack(Items.STICK));
                    helper.assertTrue(WithCreate.isContraption(helper, bearing), "a contraption is a mover the"
                            + " kit's structures never push");
                    helper.assertFalse(ForeignMovers.isMover(item), "an item is none");
                    MovingStructures.takeAway(crate[0]);
                    WithCreate.disassemble(helper, bearing);
                })
                .thenSucceed();
    }

    @GameTest(template = TALL, batch = BATCH, timeoutTicks = 200)
    public static void layer_aTurningContraptionIsMetWhereItTurns(final GameTestHelper helper) {
        if (!create()) {
            helper.assertFalse(CreateIntegration.meeting(), "without Create the layer is off");
            helper.succeed();
            return;
        }
        final BlockPos bearing = turningBearing(helper);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(WithCreate.isContraption(helper, bearing),
                        "the bearing, turned, takes the stone into its contraption"))
                .thenIdle(10)
                .thenExecute(() -> {
                    final List<ForeignMover> near = ForeignMovers.near(helper.getLevel(), arena(helper));
                    helper.assertTrue(near.size() == 1, "the contraption is met: " + near);
                    final ForeignMover mover = near.get(0);
                    final Vec3 corner = new Vec3(1, 1, 1);
                    final Vec3 placed = WithCreate.placed(helper, bearing, corner);
                    final StructureFrame frame = mover.frame();
                    final Vec3 met = new Vec3(frame.worldX(corner.x, corner.y, corner.z),
                            frame.worldY(corner.x, corner.y, corner.z), frame.worldZ(corner.x, corner.y, corner.z));
                    helper.assertTrue(met.distanceTo(placed) < 1.0e-6, "a corner of it is met where its entity"
                            + " places it: " + met + " and " + placed);
                    helper.assertTrue(Math.abs(Math.abs(mover.spin().y) - BEARING_SPIN) < BEARING_SPIN * 0.15
                                    && Math.abs(mover.spin().x) < 1.0e-6 && Math.abs(mover.spin().z) < 1.0e-6,
                            "turning about the bearing's axis as fast as it turns it: " + mover.spin());
                    WithCreate.disassemble(helper, bearing);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void layer_aSubLevelIsMetWhereItStands(final GameTestHelper helper) {
        if (!sable()) {
            helper.assertFalse(SableIntegration.meeting(), "without Sable the layer is off");
            helper.succeed();
            return;
        }
        platform(helper, 2, true);
        helper.startSequence()
                .thenIdle(10)
                .thenExecute(() -> {
                    final ForeignMover mover = subLevel(helper);
                    final Vec3 middle = middle(mover);
                    final Vec3 stood = Vec3.atCenterOf(helper.absolutePos(new BlockPos(4, 2, 4)));
                    helper.assertTrue(middle.distanceTo(stood) < STANDS, "the middle of its blocks is met where"
                            + " they stood: " + middle + " and " + stood);
                    helper.assertTrue(mover.speed().length() < 1.0e-6 && mover.spin().length() < 1.0e-6,
                            "standing still: " + mover.speed() + ", " + mover.spin());
                    clear(helper, true);
                })
                .thenSucceed();
    }

    @GameTest(template = TALL, batch = FLIGHT, timeoutTicks = 200)
    public static void layer_aFallingSubLevelIsMetAsFastAsItFalls(final GameTestHelper helper) {
        if (!sable()) {
            helper.assertFalse(SableIntegration.meeting(), "without Sable the layer is off");
            helper.succeed();
            return;
        }
        platform(helper, 20, false);
        final double[] first = new double[3];
        helper.startSequence()
                .thenIdle(5)
                .thenExecute(() -> {
                    final ForeignMover mover = subLevel(helper);
                    first[0] = middle(mover).y;
                    first[1] = mover.speed().y;
                    first[2] = helper.getLevel().getGameTime();
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    final ForeignMover mover = subLevel(helper);
                    final double ticks = helper.getLevel().getGameTime() - first[2];
                    final double fell = (middle(mover).y - first[0]) / ticks * 20;
                    final double mean = (first[1] + mover.speed().y) / 2;
                    helper.assertTrue(mover.speed().y < -1, "it falls: " + mover.speed());
                    helper.assertTrue(Math.abs(fell - mean) < 0.5, "met as fast as it goes from tick to tick: "
                            + fell + " and " + mean + " metres a second");
                    helper.assertTrue(mover.spin().length() < 0.05, "and without turning: " + mover.spin());
                    clear(helper, false);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 300)
    public static void layer_aBlockDroppedOnASubLevelRestsOnIt(final GameTestHelper helper) {
        if (!sable()) {
            helper.assertFalse(SableIntegration.meeting(), "without Sable the layer is off");
            helper.succeed();
            return;
        }
        platform(helper, 2, true);
        final MovingStructure crate = made(helper, new BlockPos(4, 5, 4), Blocks.OAK_PLANKS);
        crate.move(PhysicsMotion.AT_REST);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(asleep(crate), "the crate comes to rest"))
                .thenExecute(() -> {
                    final ForeignMover mover = subLevel(helper);
                    final double top = middle(mover).y + 0.5;
                    helper.assertTrue(Math.abs(crate.worldBox().minY - top) < RESTS
                                    && Math.abs(crate.worldBox().minY - y(helper, 3)) < RESTS,
                            "the crate rests on the sub-level: " + crate.worldBox() + " on " + top);
                    MovingStructures.takeAway(crate);
                    clear(helper, true);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void layer_aPlaceOfASubLevelIsWhereItHasIt(final GameTestHelper helper) {
        if (!sable()) {
            helper.assertFalse(SableIntegration.meeting(), "without Sable the layer is off");
            helper.succeed();
            return;
        }
        platform(helper, 2, true);
        helper.startSequence()
                .thenIdle(10)
                .thenExecute(() -> {
                    final ForeignMover mover = subLevel(helper);
                    final Vec3 place = Vec3.atCenterOf(plotMiddle(mover));
                    final Vec3 there = MovingStructures.inWorld(helper.getLevel(), place);
                    final Vec3 projected = WithSable.projected(helper.getLevel(), place);
                    helper.assertTrue(there.distanceTo(projected) < 1.0e-9, "a place of its plot is where Sable"
                            + " has it: " + there + " and " + projected);
                    helper.assertTrue(there.distanceTo(middle(mover)) < 1.0e-6, "where the sub-level is met: "
                            + there + " and " + middle(mover));
                    final Vec3 world = Vec3.atCenterOf(helper.absolutePos(new BlockPos(1, 4, 1)));
                    helper.assertTrue(MovingStructures.inWorld(helper.getLevel(), world) == world,
                            "and a place of the world is where it is");
                    clear(helper, true);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void layer_theKitTakesNoBlockOfASubLevel(final GameTestHelper helper) {
        if (!sable()) {
            helper.assertFalse(SableIntegration.meeting(), "without Sable the layer is off");
            helper.succeed();
            return;
        }
        platform(helper, 2, true);
        helper.startSequence()
                .thenIdle(5)
                .thenExecute(() -> {
                    final BlockPos plot = plotMiddle(subLevel(helper));
                    final StructureResult made = MovingStructures.assemble(helper.getLevel(), plot, plot);
                    helper.assertTrue(!made.ok() && made.refusal() != null
                                    && made.refusal().english().contains("Sable"),
                            "a structure is never made of a sub-level's blocks: "
                                    + (made.refusal() == null ? "made" : made.refusal().english()));
                    clear(helper, true);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void layer_sablesPlotsAndTheYardAreApart(final GameTestHelper helper) {
        if (!sable()) {
            helper.assertFalse(SableIntegration.meeting(), "without Sable the layer is off");
            helper.succeed();
            return;
        }
        final MovingStructure block = made(helper, new BlockPos(4, 2, 4), Blocks.STONE);
        final Box kept = block.bounds();
        final BlockPos yard = new BlockPos(kept.minX(), kept.minY(), kept.minZ());
        helper.assertFalse(WithSable.inPlots(helper.getLevel(), yard), "a block of the yard is in none of Sable's"
                + " plots: " + yard.toShortString());
        helper.assertTrue(ForeignMovers.holding(helper.getLevel(), yard) == null, "nor kept by another mod");
        helper.assertFalse(WithSable.inPlots(helper.getLevel(), new BlockPos(YardLayout.FIRST_BLOCK_X, 64, 0)),
                "the yard starts past them");
        MovingStructures.takeAway(block);
        helper.succeed();
    }

    private static boolean create() {
        return "present".equals(System.getProperty("jsc.gametests.create"));
    }

    private static boolean sable() {
        return "present".equals(System.getProperty("jsc.gametests.sable"));
    }

    /*
     * A bearing turned by a creative motor at 16 turns a minute, a stone on it: it turns the stone in a contraption of
     * its own once the motor's turning reaches it. The bearing's place.
     */
    private static BlockPos turningBearing(final GameTestHelper helper) {
        final BlockPos bearing = new BlockPos(6, 4, 6);
        WithCreate.motor(helper, bearing.below());
        WithCreate.bearing(helper, bearing);
        helper.setBlock(bearing.above(), Blocks.STONE);
        return bearing;
    }

    /*
     * A platform of stone three by three at {@code y}, made a sub-level of Sable's. Sable's physics is held still
     * while it is {@code held}, so the sub-level stands where its blocks stood: a sub-level made where the land is
     * new to Sable's physics, as a test's arena is, falls a little before that land comes into it.
     */
    private static void platform(final GameTestHelper helper, final int y, final boolean held) {
        if (held) {
            WithSable.hold(helper.getLevel());
        }
        final List<BlockPos> blocks = new ArrayList<>();
        for (int x = 3; x <= 5; x++) {
            for (int z = 3; z <= 5; z++) {
                helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
                blocks.add(helper.absolutePos(new BlockPos(x, y, z)));
            }
        }
        WithSable.assemble(helper.getLevel(), helper.absolutePos(new BlockPos(4, y, 4)), blocks);
        helper.assertTrue(helper.getBlockState(new BlockPos(4, y, 4)).isAir(), "Sable took the platform's blocks");
    }

    /* Takes away the sub-levels of the arena, and lets Sable's physics go on when no other test holds it. */
    private static void clear(final GameTestHelper helper, final boolean held) {
        WithSable.removeAll(helper.getLevel(), arena(helper));
        if (held) {
            WithSable.release(helper.getLevel());
        }
    }

    /* The one sub-level of Sable's in the arena, as the kit meets it. */
    private static ForeignMover subLevel(final GameTestHelper helper) {
        final List<ForeignMover> near = ForeignMovers.near(helper.getLevel(), arena(helper));
        helper.assertTrue(near.size() == 1 && "Sable".equals(near.get(0).name()), "the sub-level is met: " + near);
        return near.get(0);
    }

    /* The block of its plot at the middle of where its blocks are. */
    private static BlockPos plotMiddle(final ForeignMover mover) {
        final Box blocks = mover.blocks();
        return new BlockPos(Math.floorDiv(blocks.minX() + blocks.maxX(), 2),
                Math.floorDiv(blocks.minY() + blocks.maxY(), 2), Math.floorDiv(blocks.minZ() + blocks.maxZ(), 2));
    }

    /* Where the middle of the block at the middle of its plot is met in the world. */
    private static Vec3 middle(final ForeignMover mover) {
        final Vec3 place = Vec3.atCenterOf(plotMiddle(mover));
        final StructureFrame frame = mover.frame();
        return new Vec3(frame.worldX(place.x, place.y, place.z), frame.worldY(place.x, place.y, place.z),
                frame.worldZ(place.x, place.y, place.z));
    }

    /* The arena's box in the world, and a little around it. */
    private static AABB arena(final GameTestHelper helper) {
        return helper.getBounds().inflate(2);
    }

    /* Makes the one block {@code at} of {@code block} into a structure. */
    private static MovingStructure made(final GameTestHelper helper, final BlockPos at, final Block block) {
        helper.setBlock(at, block);
        warm(helper.getLevel());
        final StructureResult made = MovingStructures.assemble(helper.getLevel(), helper.absolutePos(at),
                helper.absolutePos(at));
        helper.assertTrue(made.ok() && made.structure() != null, "a structure is made: "
                + (made.refusal() == null ? "done" : made.refusal().english()));
        return made.structure();
    }

    private static boolean asleep(final MovingStructure structure) {
        final StructurePhysics physics = structure.physics();
        return physics != null && physics.asleep();
    }

    /* The world's height of the arena's {@code y}. */
    private static double y(final GameTestHelper helper, final int y) {
        return helper.absolutePos(new BlockPos(0, y, 0)).getY();
    }

    /* Loads the chunks of the next plot, so a structure made arrives at once. */
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

    /* What reaches Create's classes, loaded only when Create is present. */
    private static final class WithCreate {

        private static final ResourceLocation BEARING = ResourceLocation.fromNamespaceAndPath("create",
                "mechanical_bearing");
        private static final ResourceLocation MOTOR = ResourceLocation.fromNamespaceAndPath("create",
                "creative_motor");

        private WithCreate() {
        }

        static boolean movable(final Level level, final BlockPos pos) {
            return BlockMovementChecks.isMovementAllowed(level.getBlockState(pos), level, pos);
        }

        /* A mechanical bearing at {@code at}, facing up. */
        static void bearing(final GameTestHelper helper, final BlockPos at) {
            helper.setBlock(at, BuiltInRegistries.BLOCK.get(BEARING).defaultBlockState()
                    .setValue(BlockStateProperties.FACING, Direction.UP));
        }

        /* A creative motor at {@code at}, turning what is above it. */
        static void motor(final GameTestHelper helper, final BlockPos at) {
            helper.setBlock(at, BuiltInRegistries.BLOCK.get(MOTOR).defaultBlockState()
                    .setValue(BlockStateProperties.FACING, Direction.UP));
        }

        static void disassemble(final GameTestHelper helper, final BlockPos at) {
            final MechanicalBearingBlockEntity bearing = helper.getBlockEntity(at);
            bearing.disassemble();
        }

        /* Whether the bearing at {@code at} turns a contraption, one the kit sees as a mover of Create's. */
        static boolean isContraption(final GameTestHelper helper, final BlockPos at) {
            final MechanicalBearingBlockEntity bearing = helper.getBlockEntity(at);
            final Entity moved = bearing.getMovedContraption();
            return moved != null && ForeignMovers.isMover(moved);
        }

        /* Where the contraption of the bearing at {@code at} places its own place {@code local} now. */
        static Vec3 placed(final GameTestHelper helper, final BlockPos at, final Vec3 local) {
            final MechanicalBearingBlockEntity bearing = helper.getBlockEntity(at);
            final AbstractContraptionEntity moved = bearing.getMovedContraption();
            return moved.toGlobalVector(local, 1f);
        }
    }

    /* What reaches Sable's classes, loaded only when Sable is present. */
    private static final class WithSable {

        /* How many tests hold Sable's physics still. */
        private static int holding;

        private WithSable() {
        }

        static void hold(final ServerLevel level) {
            if (holding++ == 0) {
                SubLevelPhysicsSystem.require(level).setPaused(true);
            }
        }

        static void release(final ServerLevel level) {
            if (--holding == 0) {
                SubLevelPhysicsSystem.require(level).setPaused(false);
            }
        }

        /* Makes {@code blocks} a sub-level, as Sable's own assembly does about {@code anchor}. */
        static void assemble(final ServerLevel level, final BlockPos anchor, final List<BlockPos> blocks) {
            final BoundingBox3i box = new BoundingBox3i(anchor, anchor);
            for (final BlockPos at : blocks) {
                box.expandTo(at.getX(), at.getY(), at.getZ(), box);
            }
            SubLevelAssemblyHelper.assembleBlocks(level, anchor, blocks, box);
        }

        static boolean inPlots(final Level level, final BlockPos pos) {
            return SableCompanion.INSTANCE.isInPlotGrid(level, pos);
        }

        static Vec3 projected(final Level level, final Vec3 place) {
            return SableCompanion.INSTANCE.projectOutOfSubLevel(level, place);
        }

        /* Takes away every sub-level in {@code box}, so the next test finds none. */
        static void removeAll(final ServerLevel level, final AABB box) {
            final List<SubLevel> found = new ArrayList<>();
            for (final SubLevelAccess sub : SableCompanion.INSTANCE.getAllIntersecting(level, new BoundingBox3d(box))) {
                if (sub instanceof SubLevel own) {
                    found.add(own);
                }
            }
            for (final SubLevel own : found) {
                SubLevelContainer.getContainer(level).removeSubLevel(own, SubLevelRemovalReason.REMOVED);
            }
        }
    }
}
