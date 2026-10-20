/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructureEntity;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.StructureResult;
import dev.jstech.core.moving.StructureTexts;
import dev.jstech.core.moving.StructureYard;
import dev.jstech.core.moving.YardBook;
import dev.jstech.core.moving.YardLayout;
import dev.jstech.core.region.Box;
import dev.jstech.core.text.Text;
import dev.jstech.core.uuid.StructureUuid;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.ChunkLoadWatch;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A moving structure made where nothing it needs is loaded yet: the plot of the yard it gets waits to load while its
 * blocks stand in the world as they stood, then they come into it, and nothing of the Core loads a chunk by reading it
 * on the way; such a structure saved keeps that it is still coming; set down, it is only undone; blocks in land that
 * is not loaded make no structure; and a plot taken away before its chunks loaded is given to no other until its
 * blocks are cleared. Each test is in a batch of its own, as each takes the yard's next plot.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MovingStructureArrivalGameTests {

    private static final String ARENA = "empty";
    private static final String CORE = "dev.jstech.core.";

    private MovingStructureArrivalGameTests() {
    }

    @GameTest(template = ARENA, batch = "jscore_moving_arrival_cold", timeoutTicks = 400)
    public static void yard_madeWhereThePlotIsNotLoadedWaitsForIt(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final List<MovingStructure> before = coldPlot(helper);
        final int loadsBefore = ChunkLoadWatch.loadsBy(CORE);
        line(helper);
        final StructureResult made = MovingStructures.assemble(level, helper.absolutePos(new BlockPos(3, 3, 4)),
                helper.absolutePos(new BlockPos(5, 3, 4)));
        helper.assertTrue(made.ok() && made.blocks() == 3, "three blocks make a structure: " + refusal(made));
        final MovingStructure structure = made.structure();
        helper.assertTrue(structure.arriving() && !structure.ready(), "its blocks are on their way");
        helper.assertBlockPresent(Blocks.STONE, new BlockPos(4, 3, 4));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(structure.ready(), "the structure is ready once its plot is"))
                .thenExecute(() -> {
                    helper.assertTrue(!structure.arriving() && structure.blocks() == 3, "with its three blocks");
                    helper.assertBlockPresent(Blocks.AIR, new BlockPos(4, 3, 4));
                    final BlockPos inYard = find(level, structure, state -> state.is(Blocks.STONE));
                    helper.assertTrue(inYard != null && YardLayout.isYardBlock(inYard.getX()),
                            "which are in the yard now: " + inYard);
                    helper.assertTrue(ChunkLoadWatch.loadsBy(CORE) == loadsBefore,
                            "and nothing of the Core loaded a chunk by reading it on the way");
                    MovingStructures.takeAway(structure);
                    takeAll(before);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = "jscore_moving_arrival_saved", timeoutTicks = 400)
    public static void save_keepsAStructureStillComing(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final List<MovingStructure> before = coldPlot(helper);
        line(helper);
        final StructureResult made = MovingStructures.assemble(level, helper.absolutePos(new BlockPos(3, 3, 4)),
                helper.absolutePos(new BlockPos(5, 3, 4)));
        helper.assertTrue(made.ok() && made.structure().arriving(), "a structure is on its way: " + refusal(made));
        final CompoundTag saved = new CompoundTag();
        made.structure().entity().saveWithoutId(saved);
        final MovingStructureEntity copy = MovingStructures.ENTITY.get().create(level);
        copy.load(saved);
        helper.assertTrue(copy.structure() != null && copy.structure().arriving()
                && copy.structure().blocks() == 3, "saved so, it is still coming, with its three blocks");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(made.structure().ready(), "the structure is ready"))
                .thenExecute(() -> {
                    MovingStructures.takeAway(made.structure());
                    takeAll(before);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = "jscore_moving_arrival_undone", timeoutTicks = 400)
    public static void setDown_ofAStructureStillComingUndoesIt(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final List<MovingStructure> before = coldPlot(helper);
        line(helper);
        final int plot = StructureYard.nextPlot(level);
        final StructureResult made = MovingStructures.assemble(level, helper.absolutePos(new BlockPos(3, 3, 4)),
                helper.absolutePos(new BlockPos(5, 3, 4)));
        helper.assertTrue(made.ok() && made.structure().arriving(), "a structure is on its way: " + refusal(made));
        final StructureResult down = MovingStructures.setDown(made.structure());
        helper.assertTrue(down.ok() && down.blocks() == 3, "set down, it is undone: " + refusal(down));
        helper.assertTrue(made.structure().entity().isRemoved(), "its entity gone");
        for (int x = 3; x <= 5; x++) {
            helper.assertBlockPresent(Blocks.STONE, new BlockPos(x, 3, 4));
        }
        helper.assertTrue(StructureYard.nextPlot(level) == plot, "and its plot is the next one given again");
        takeAll(before);
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "jscore_moving_arrival_unloaded")
    public static void assembly_refusesLandThatIsNotLoaded(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final int loadsBefore = ChunkLoadWatch.loadsBy(CORE);
        helper.setBlock(new BlockPos(3, 3, 4), Blocks.STONE);
        final BlockPos near = helper.absolutePos(new BlockPos(3, 3, 4));
        final BlockPos far = near.offset(4_000, 0, 0);
        helper.assertTrue(!level.isLoaded(far), "the land far away is not loaded");
        final StructureResult refused = MovingStructures.assemble(level, near, far);
        helper.assertTrue(!refused.ok() && refused.refusal() instanceof Text.Translated said
                && said.key() == StructureTexts.NOT_LOADED, "a box reaching it is refused: " + refusal(refused));
        helper.assertTrue(!level.isLoaded(far) && ChunkLoadWatch.loadsBy(CORE) == loadsBefore,
                "and it stays unloaded");
        helper.assertBlockPresent(Blocks.STONE, new BlockPos(3, 3, 4));
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "jscore_moving_arrival_book")
    public static void book_givesAPlotLeftToClearToNoOneUntilItIsCleared(final GameTestHelper helper) {
        final YardBook taken = YardBook.EMPTY.taken(StructureUuid.random());
        final Box blocks = new Box(1, 2, 3, 4, 5, 6);
        final YardBook left = taken.leftToClear(0, blocks);
        helper.assertTrue(left.owner(0).isEmpty() && left.nextPlot() == 1,
                "a plot left to clear is no structure's, and not given to the next");
        final YardBook read = YardBook.CODEC.parse(NbtOps.INSTANCE, YardBook.CODEC.encodeStart(NbtOps.INSTANCE, left)
                .getOrThrow()).getOrThrow();
        helper.assertTrue(blocks.equals(read.clearing().get(0)), "the book keeps it left to clear when saved");
        final YardBook cleared = read.cleared(0);
        helper.assertTrue(cleared.clearing().isEmpty() && cleared.nextPlot() == 0,
                "once its blocks are cleared it is the next plot given");
        helper.succeed();
    }

    /* Three blocks of stone in a row, east to west, at y 3. */
    private static void line(final GameTestHelper helper) {
        for (int x = 3; x <= 5; x++) {
            helper.setBlock(new BlockPos(x, 3, 4), Blocks.STONE);
        }
    }

    /*
     * Takes plots with structures of one block until the yard's next plot is one whose chunks are not loaded, as a
     * plot an earlier test gave back may still be; gives the structures it made, to be taken away at the end.
     */
    private static List<MovingStructure> coldPlot(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final List<MovingStructure> taken = new ArrayList<>();
        for (int i = 0; i < 49 && plotLoaded(level); i++) {
            final BlockPos at = new BlockPos(1 + i % 7, 6, 1 + i / 7);
            helper.setBlock(at, Blocks.STONE);
            final StructureResult made = MovingStructures.assemble(level, helper.absolutePos(at),
                    helper.absolutePos(at));
            helper.assertTrue(made.ok() && made.structure().ready(), "a block takes a loaded plot at once");
            taken.add(made.structure());
        }
        helper.assertTrue(!plotLoaded(level), "the yard's next plot is not loaded");
        return taken;
    }

    private static boolean plotLoaded(final ServerLevel level) {
        final int plot = StructureYard.nextPlot(level);
        return level.getChunkSource().getChunkNow(YardLayout.centreX(plot) >> 4, YardLayout.centreZ(plot) >> 4)
                != null;
    }

    private static void takeAll(final List<MovingStructure> structures) {
        for (final MovingStructure structure : structures) {
            MovingStructures.takeAway(structure);
        }
    }

    /* The first block of the structure's yard that is so, or null. */
    private static BlockPos find(final ServerLevel level, final MovingStructure structure,
                                 final Predicate<BlockState> wanted) {
        final Box box = structure.bounds();
        for (final BlockPos at : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(),
                box.maxZ())) {
            if (wanted.test(level.getBlockState(at))) {
                return at.immutable();
            }
        }
        return null;
    }

    private static String refusal(final StructureResult result) {
        return result.refusal() == null ? "done" : result.refusal().english();
    }
}
