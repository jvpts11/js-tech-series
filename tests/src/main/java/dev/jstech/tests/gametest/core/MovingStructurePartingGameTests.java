/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.moving.IStructureBody;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.StructureBalance;
import dev.jstech.core.moving.StructurePose;
import dev.jstech.core.moving.StructureResult;
import dev.jstech.core.moving.StructureYard;
import dev.jstech.core.moving.TowardMotion;
import dev.jstech.core.moving.YardLayout;
import dev.jstech.core.moving.physics.PhysicsMotion;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestAboard;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Structures parting, joining and docking, and their tools, in a real world: a bar broken in the middle becomes two,
 * and the piece falls; a setting holds a broken structure together; a decoupler parts a stage and pushes it away; two
 * structures square to each other join for good, and two that are not are refused; a clamp holds another met face to
 * face where it is, draws it in, and lets it go as its signal goes off; a drill mines into a chest aboard; a harvester
 * takes ripe wheat and plants it again; a plough tills; a placer sets the blocks it carries; and a combine drives and
 * harvests at once.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MovingStructurePartingGameTests {

    private static final String ARENA = "empty";
    private static final String TALL = "tall";
    private static final String BATCH = "jscore_moving_parting";
    private static final String SETTING_BATCH = "jscore_moving_parting_setting";
    private static final double RESTS = 0.03;

    private MovingStructurePartingGameTests() {
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void parting_aBarBrokenInTheMiddleBecomesTwoAndThePieceFalls(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final MovingStructure bar = made(helper, row(2, 5, 4, 6), Blocks.STONE);
        final BlockPos middle = new BlockPos((bar.bounds().minX() + bar.bounds().maxX()) / 2, bar.bounds().minY(),
                bar.bounds().minZ());
        final AABB arena = helper.getBounds();
        helper.startSequence()
                .thenExecute(() -> level.setBlock(middle, Blocks.AIR.defaultBlockState(), 3))
                .thenWaitUntil(() -> helper.assertTrue(structures(level, arena).size() == 2,
                        "broken in the middle, the bar is two"))
                .thenExecute(() -> helper.assertTrue(bar.blocks() == 2, "the bar keeps one side: " + bar.blocks()))
                .thenWaitUntil(() -> {
                    final MovingStructure piece = other(level, arena, bar);
                    helper.assertTrue(piece != null && piece.physics() != null && piece.physics().asleep(),
                            "the piece falls and comes to rest");
                })
                .thenExecute(() -> {
                    final MovingStructure piece = other(level, arena, bar);
                    helper.assertTrue(Math.abs(piece.worldBox().minY - y(helper, 2)) < RESTS,
                            "the piece lies on the floor: " + piece.worldBox());
                    helper.assertTrue(Math.abs(bar.worldBox().minY - y(helper, 5)) < RESTS,
                            "the bar, at rest by no rule, stays where it was: " + bar.worldBox());
                    MovingStructures.takeAway(piece);
                    MovingStructures.takeAway(bar);
                })
                .thenSucceed();
    }

    // In a batch of its own: the setting is the server's, and would hold the others' structures together too.
    @GameTest(template = ARENA, batch = SETTING_BATCH, timeoutTicks = 100)
    public static void parting_aSettingHoldsABrokenStructureTogether(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final MovingStructure bar = made(helper, row(2, 5, 4, 6), Blocks.STONE);
        final BlockPos middle = new BlockPos((bar.bounds().minX() + bar.bounds().maxX()) / 2, bar.bounds().minY(),
                bar.bounds().minZ());
        StructureBalance.split(false);
        level.setBlock(middle, Blocks.AIR.defaultBlockState(), 3);
        helper.runAfterDelay(10, () -> {
            StructureBalance.split(true);
            helper.assertTrue(bar.blocks() == 4, "it holds together: " + bar.blocks());
            helper.assertTrue(structures(level, helper.getBounds()).size() == 1, "one structure still");
            MovingStructures.takeAway(bar);
            helper.succeed();
        });
    }

    @GameTest(template = TALL, batch = BATCH, timeoutTicks = 200)
    public static void parting_aDecouplerPartsAStageAndPushesItAway(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        // The lever on the upper stage powers the stone it is on, and that the decoupler under it.
        helper.setBlock(new BlockPos(4, 12, 4), Blocks.STONE);
        helper.setBlock(new BlockPos(5, 12, 4), lever(Direction.EAST, false));
        helper.setBlock(new BlockPos(4, 11, 4), TestAboard.DECOUPLER.get().defaultBlockState()
                .setValue(DirectionalBlock.FACING, Direction.DOWN));
        helper.setBlock(new BlockPos(4, 10, 4), Blocks.STONE);
        final MovingStructure rocket = assembled(helper, new BlockPos(4, 10, 4), new BlockPos(5, 12, 4));
        final AABB arena = helper.getBounds();
        helper.startSequence()
                .thenExecuteAfter(5, () -> helper.assertTrue(structures(level, arena).size() == 1,
                        "with no signal, it holds"))
                .thenExecute(() -> {
                    final BlockPos lever = find(level, rocket, state -> state.is(Blocks.LEVER));
                    level.setBlock(lever, level.getBlockState(lever).setValue(LeverBlock.POWERED, true), 3);
                })
                .thenWaitUntil(() -> helper.assertTrue(structures(level, arena).size() == 2,
                        "fired, the decoupler parts the stage"))
                .thenExecute(() -> {
                    final MovingStructure stage = other(level, arena, rocket);
                    helper.assertTrue(stage.motion() instanceof PhysicsMotion going && going.vy() < -0.5,
                            "pushed away down, the way the decoupler looked: " + stage.motion());
                    helper.assertTrue(stage.worldBox().maxY <= rocket.worldBox().minY + 1.0e-6,
                            "the stage was below: " + stage.worldBox() + ", " + rocket.worldBox());
                    MovingStructures.takeAway(stage);
                    MovingStructures.takeAway(rocket);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void joining_twoSquareStructuresJoinForGood(final GameTestHelper helper) {
        final MovingStructure host = made(helper, row(2, 2, 4, 3), Blocks.STONE);
        final MovingStructure visitor = made(helper, List.of(new BlockPos(5, 2, 4)), Blocks.OAK_PLANKS);
        visitor.teleport(StructurePose.turned(visitor.pose().x(), visitor.pose().y(), visitor.pose().z(), 0, 1, 0,
                Math.PI / 2));
        final StructureResult joined = MovingStructures.join(host, visitor);
        helper.assertTrue(joined.ok(), "two structures square to each other join: "
                + (joined.refusal() == null ? "" : joined.refusal().english()));
        helper.assertTrue(host.blocks() == 3, "the host holds all three blocks: " + host.blocks());
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(visitor.entity().isRemoved(), "and the visitor is no more");
            MovingStructures.takeAway(host);
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void joining_isRefusedForTwoNotSquareToEachOther(final GameTestHelper helper) {
        final MovingStructure host = made(helper, List.of(new BlockPos(2, 2, 4)), Blocks.STONE);
        final MovingStructure visitor = made(helper, List.of(new BlockPos(5, 2, 4)), Blocks.OAK_PLANKS);
        visitor.teleport(StructurePose.turned(visitor.pose().x(), visitor.pose().y(), visitor.pose().z(), 0, 1, 0,
                Math.toRadians(30)));
        helper.assertTrue(!MovingStructures.join(host, visitor).ok(), "refused, turned thirty degrees");
        MovingStructures.takeAway(visitor);
        MovingStructures.takeAway(host);
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void docking_aClampHoldsAClampMetFaceToFaceAndLetsItGo(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        helper.setBlock(new BlockPos(2, 3, 4), TestAboard.CLAMP.get().defaultBlockState()
                .setValue(DirectionalBlock.FACING, Direction.EAST));
        helper.setBlock(new BlockPos(1, 3, 4), Blocks.REDSTONE_BLOCK);
        final MovingStructure host = assembled(helper, new BlockPos(1, 3, 4), new BlockPos(2, 3, 4));
        helper.setBlock(new BlockPos(4, 3, 4), TestAboard.CLAMP.get().defaultBlockState()
                .setValue(DirectionalBlock.FACING, Direction.WEST));
        final MovingStructure visitor = assembled(helper, new BlockPos(4, 3, 4), new BlockPos(4, 3, 4));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(visitor.mount() != null, "the clamp holds the other"))
                .thenExecute(() -> helper.assertTrue(visitor.worldBox().minX - host.worldBox().maxX > 0.5,
                        "held where it was, not yet drawn in: " + visitor.worldBox() + " from " + host.worldBox()))
                .thenWaitUntil(() -> helper.assertTrue(visitor.resting(), "and drawn in on the host"))
                .thenExecute(() -> helper.assertTrue(Math.abs(visitor.worldBox().minX - host.worldBox().maxX) < 1.0e-6,
                        "drawn in, face on face: " + visitor.worldBox() + " against " + host.worldBox()))
                .thenExecute(() -> {
                    final BlockPos power = find(level, host, state -> state.is(Blocks.REDSTONE_BLOCK));
                    level.setBlock(power, Blocks.STONE.defaultBlockState(), 3);
                })
                .thenWaitUntil(() -> helper.assertTrue(visitor.mount() == null, "its signal off, it lets go"))
                .thenExecute(() -> {
                    MovingStructures.takeAway(visitor);
                    MovingStructures.takeAway(host);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void tools_aDrillMinesTheBlockBeforeItIntoAChestAboard(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        helper.setBlock(new BlockPos(3, 3, 4), TestAboard.DRILL.get().defaultBlockState()
                .setValue(DirectionalBlock.FACING, Direction.EAST));
        helper.setBlock(new BlockPos(2, 3, 4), Blocks.CHEST);
        final MovingStructure rig = assembled(helper, new BlockPos(2, 3, 4), new BlockPos(3, 3, 4));
        helper.setBlock(new BlockPos(4, 3, 4), Blocks.STONE);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertBlockPresent(Blocks.AIR, new BlockPos(4, 3, 4)))
                .thenExecute(() -> {
                    final BlockPos chest = find(level, rig, state -> state.is(Blocks.CHEST));
                    helper.assertTrue(level.getBlockEntity(chest) instanceof ChestBlockEntity box
                            && box.countItem(Items.COBBLESTONE) == 1, "the cobblestone went into the chest aboard");
                    MovingStructures.takeAway(rig);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void tools_aHarvesterTakesRipeWheatAndPlantsItAgain(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        helper.setBlock(new BlockPos(4, 1, 4), Blocks.FARMLAND);
        helper.setBlock(new BlockPos(4, 2, 4), Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7));
        helper.setBlock(new BlockPos(4, 3, 4), TestAboard.HARVESTER.get().defaultBlockState()
                .setValue(DirectionalBlock.FACING, Direction.DOWN));
        helper.setBlock(new BlockPos(4, 4, 4), Blocks.CHEST);
        final MovingStructure combine = assembled(helper, new BlockPos(4, 3, 4), new BlockPos(4, 4, 4));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertBlockProperty(new BlockPos(4, 2, 4), CropBlock.AGE, 0))
                .thenExecute(() -> {
                    final BlockPos chest = find(level, combine, state -> state.is(Blocks.CHEST));
                    helper.assertTrue(level.getBlockEntity(chest) instanceof ChestBlockEntity box
                            && box.countItem(Items.WHEAT) == 1, "the wheat taken into the chest");
                    MovingStructures.takeAway(combine);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void tools_aPloughTillsTheGround(final GameTestHelper helper) {
        helper.setBlock(new BlockPos(4, 1, 4), Blocks.DIRT);
        helper.setBlock(new BlockPos(4, 2, 4), TestAboard.PLOUGH.get().defaultBlockState()
                .setValue(DirectionalBlock.FACING, Direction.DOWN));
        final MovingStructure plough = assembled(helper, new BlockPos(4, 2, 4), new BlockPos(4, 2, 4));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertBlockPresent(Blocks.FARMLAND, new BlockPos(4, 1, 4)))
                .thenExecute(() -> MovingStructures.takeAway(plough))
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void tools_aPlacerSetsTheBlocksItCarries(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        helper.setBlock(new BlockPos(4, 3, 4), TestAboard.PLACER.get().defaultBlockState()
                .setValue(DirectionalBlock.FACING, Direction.DOWN));
        helper.setBlock(new BlockPos(4, 4, 4), Blocks.CHEST);
        final BlockPos chestAt = helper.absolutePos(new BlockPos(4, 4, 4));
        if (level.getBlockEntity(chestAt) instanceof ChestBlockEntity box) {
            box.setItem(0, new ItemStack(Items.COBBLESTONE, 4));
        }
        final MovingStructure placer = assembled(helper, new BlockPos(4, 3, 4), new BlockPos(4, 4, 4));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertBlockPresent(Blocks.COBBLESTONE, new BlockPos(4, 2, 4)))
                .thenExecute(() -> {
                    final BlockPos chest = find(level, placer, state -> state.is(Blocks.CHEST));
                    helper.assertTrue(level.getBlockEntity(chest) instanceof ChestBlockEntity box
                            && box.countItem(Items.COBBLESTONE) == 3, "one taken from what it carries");
                    MovingStructures.takeAway(placer);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void tools_aCombineDrivesAndHarvestsAtOnce(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        for (int x = 2; x <= 6; x++) {
            helper.setBlock(new BlockPos(x, 1, 4), Blocks.FARMLAND);
            helper.setBlock(new BlockPos(x, 2, 4), Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7));
        }
        helper.setBlock(new BlockPos(1, 3, 4), TestAboard.HARVESTER.get().defaultBlockState()
                .setValue(DirectionalBlock.FACING, Direction.DOWN));
        helper.setBlock(new BlockPos(1, 4, 4), Blocks.CHEST);
        final MovingStructure combine = assembled(helper, new BlockPos(1, 3, 4), new BlockPos(1, 4, 4));
        final StructurePose start = combine.pose();
        MovingStructures.move(combine, new TowardMotion(start.movedTo(start.x() + 6, start.y(), start.z()), 2, 0));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(combine.resting(), "the combine drove along the row"))
                .thenExecute(() -> {
                    for (int x = 2; x <= 6; x++) {
                        helper.assertBlockProperty(new BlockPos(x, 2, 4), CropBlock.AGE, 0);
                    }
                    final BlockPos chest = find(level, combine, state -> state.is(Blocks.CHEST));
                    helper.assertTrue(level.getBlockEntity(chest) instanceof ChestBlockEntity box
                            && box.countItem(Items.WHEAT) == 5, "every ear taken as it passed");
                    MovingStructures.takeAway(combine);
                })
                .thenSucceed();
    }

    /* The moving structures loaded in {@code box}, of the arena. */
    private static List<MovingStructure> structures(final ServerLevel level, final AABB box) {
        final List<MovingStructure> found = new ArrayList<>();
        for (final IStructureBody body : MovingStructures.near(level, box)) {
            if (body instanceof MovingStructure structure && !structure.entity().isRemoved()) {
                found.add(structure);
            }
        }
        return found;
    }

    /* The structure in {@code box} that is not {@code one}, or null. */
    private static MovingStructure other(final ServerLevel level, final AABB box, final MovingStructure one) {
        for (final MovingStructure structure : structures(level, box)) {
            if (structure != one) {
                return structure;
            }
        }
        return null;
    }

    /* The places of a row along x at {@code y} and {@code z} from {@code fromX} to {@code toX}. */
    private static List<BlockPos> row(final int fromX, final int y, final int z, final int toX) {
        final List<BlockPos> places = new ArrayList<>();
        for (int x = fromX; x <= toX; x++) {
            places.add(new BlockPos(x, y, z));
        }
        return places;
    }

    /* A lever on a wall, facing away from the block it is on, lit or not. */
    private static BlockState lever(final Direction facing, final boolean lit) {
        return Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.WALL)
                .setValue(LeverBlock.FACING, facing).setValue(LeverBlock.POWERED, lit);
    }

    /* The first block of the structure's yard that is so, or null. */
    private static BlockPos find(final ServerLevel level, final MovingStructure structure,
                                 final Predicate<BlockState> wanted) {
        for (final BlockPos at : BlockPos.betweenClosed(structure.bounds().minX(), structure.bounds().minY(),
                structure.bounds().minZ(), structure.bounds().maxX(), structure.bounds().maxY(),
                structure.bounds().maxZ())) {
            if (wanted.test(level.getBlockState(at))) {
                return at.immutable();
            }
        }
        return null;
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
