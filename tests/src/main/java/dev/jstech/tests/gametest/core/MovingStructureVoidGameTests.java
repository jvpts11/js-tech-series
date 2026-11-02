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
import dev.jstech.core.moving.StructurePose;
import dev.jstech.core.moving.StructureResult;
import dev.jstech.core.moving.StructureYard;
import dev.jstech.core.moving.TowardMotion;
import dev.jstech.core.moving.YardLayout;
import dev.jstech.core.region.Box;
import dev.jstech.tests.JsTests;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A moving structure under its dimension's world, where the game takes away whatever falls that far: it stops there
 * whole, its blocks still in its plot of the yard, moved down by a rule or falling by its physics, and goes on from
 * there.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MovingStructureVoidGameTests {

    private static final String ARENA = "empty";
    /* How far under the dimension's lowest block the game takes away whatever falls, and a hair. */
    private static final int BELOW_THE_WORLD = 64;
    private static final double HAIR = 1.0e-6;
    /* How far under that line a rule sends it, and how fast, in blocks a tick. */
    private static final double SENT_UNDER = 200;
    private static final double SPEED = 4;
    /* How far over the line it starts, in blocks, to fall through it by its physics. */
    private static final double OVER_THE_LINE = 2;
    private static final int WATCHED = 60;

    private MovingStructureVoidGameTests() {
    }

    @GameTest(template = ARENA, batch = "jscore_moving_void", timeoutTicks = 200)
    public static void void_holdsAStructureARuleSendsUnderTheWorldWhole(final GameTestHelper helper) {
        final MovingStructure floor = floor(helper);
        final double line = line(helper.getLevel());
        final StructurePose start = floor.pose();
        floor.move(new TowardMotion(start.movedTo(start.x(), line - SENT_UNDER, start.z()), SPEED, 0));
        helper.startSequence()
                .thenIdle(WATCHED)
                .thenExecute(() -> {
                    helper.assertFalse(floor.entity().isRemoved(), "it is not taken away under the world");
                    helper.assertTrue(floor.pose().y() >= line - HAIR, "it stops on the line under the world, "
                            + line + ": " + floor.pose().y());
                    helper.assertTrue(floor.blocks() == 9 && kept(helper.getLevel(), floor), "with its nine blocks "
                            + "in its plot of the yard");
                    floor.move(new TowardMotion(start, SPEED, 0));
                })
                .thenWaitUntil(() -> helper.assertTrue(floor.resting() && floor.pose().y() > line + 1,
                        "a rule takes it up again from there: " + floor.pose().y()))
                .thenExecute(() -> MovingStructures.takeAway(floor))
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = "jscore_moving_void", timeoutTicks = 200)
    public static void void_holdsAStructureFallingByItsPhysicsWhole(final GameTestHelper helper) {
        final MovingStructure floor = floor(helper);
        final double line = line(helper.getLevel());
        final StructurePose start = floor.pose();
        floor.teleport(start.movedTo(start.x(), line + OVER_THE_LINE, start.z()));
        MovingStructures.freeze(floor, false);
        helper.startSequence()
                .thenIdle(WATCHED)
                .thenExecute(() -> {
                    helper.assertFalse(floor.entity().isRemoved(), "falling, it is not taken away under the world");
                    helper.assertTrue(floor.pose().y() >= line - HAIR, "it stops on the line under the world, "
                            + line + ": " + floor.pose().y());
                    helper.assertTrue(floor.velocity()[1] > -1, "its fall stopped there: " + floor.velocity()[1]
                            + " blocks a tick");
                    helper.assertTrue(kept(helper.getLevel(), floor), "with its blocks in its plot of the yard");
                    MovingStructures.takeAway(floor);
                })
                .thenSucceed();
    }

    /* The height under which the game takes away whatever falls in {@code level}. */
    private static double line(final ServerLevel level) {
        return level.getMinBuildHeight() - BELOW_THE_WORLD;
    }

    /* Whether the structure's blocks still stand in its plot of the yard, and the plot is still its own. */
    private static boolean kept(final ServerLevel level, final MovingStructure structure) {
        final Box box = structure.bounds();
        int stone = 0;
        for (final BlockPos at : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(),
                box.maxZ())) {
            stone += level.getBlockState(at).is(Blocks.STONE) ? 1 : 0;
        }
        return stone == structure.blocks() && StructureYard.owner(level, structure.plot())
                .filter(structure.id()::equals).isPresent();
    }

    /* A floor of stone three blocks a side at y 3, made a structure. */
    private static MovingStructure floor(final GameTestHelper helper) {
        for (int x = 3; x <= 5; x++) {
            for (int z = 3; z <= 5; z++) {
                helper.setBlock(new BlockPos(x, 3, z), Blocks.STONE);
            }
        }
        warm(helper.getLevel());
        final StructureResult made = MovingStructures.assemble(helper.getLevel(),
                helper.absolutePos(new BlockPos(3, 3, 3)), helper.absolutePos(new BlockPos(5, 3, 5)));
        helper.assertTrue(made.ok() && made.structure() != null, "a floor is made: "
                + (made.refusal() == null ? "done" : made.refusal().english()));
        return made.structure();
    }

    /* Loads the chunks round the yard's next plot, so a structure made now takes its blocks at once. */
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
