/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.diagnostic.Diagnostics;
import dev.jstech.core.diagnostic.Timings;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.StructureResult;
import dev.jstech.core.moving.StructureYard;
import dev.jstech.core.moving.YardLayout;
import dev.jstech.core.moving.physics.PhysicsMotion;
import dev.jstech.core.moving.physics.StructurePhysics;
import dev.jstech.tests.JsTests;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * What the physics of moving structures costs the server: twenty structures let go at once, eight crates of stone
 * falling to the floor, four stacks of two landing on each other and four rafts dropped in a pool, timed as they fall,
 * land and float, and once they have all come to rest and fallen asleep. The times are written to the log; the test
 * asks only that they all come to rest, a machine's speed being its own.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MovingStructurePhysicsBenchGameTests {

    private static final String BENCH = "bench";
    /* How many ticks the timings are read after they are let go, while they fall and land. */
    private static final int BUSY = 30;

    private MovingStructurePhysicsBenchGameTests() {
    }

    @GameTest(template = BENCH, batch = "jscore_moving_physics_bench", timeoutTicks = 600)
    public static void bench_twentyStructuresFallLandFloatAndSleep(final GameTestHelper helper) {
        final List<MovingStructure> all = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            final int x = 2 + (i % 4) * 4;
            final int z = 2 + (i / 4) * 4;
            all.add(box(helper, x, 5, z, x + 1, 6, z + 1, Blocks.STONE));
        }
        final List<MovingStructure> landing = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            final int x = 2 + i * 4;
            landing.add(box(helper, x, 2, 12, x + 1, 2, 13, Blocks.OAK_PLANKS));
            landing.add(box(helper, x, 6, 12, x + 1, 6, 13, Blocks.OAK_PLANKS));
        }
        landing.addAll(all);
        pool(helper, 20, 2, 44, 18);
        final List<MovingStructure> rafts = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            final int x = 23 + (i % 2) * 9;
            final int z = 5 + (i / 2) * 7;
            rafts.add(box(helper, x, 6, z, x + 2, 6, z + 2, Blocks.OAK_PLANKS));
        }
        all.clear();
        all.addAll(landing);
        all.addAll(rafts);
        for (final MovingStructure structure : all) {
            structure.move(PhysicsMotion.AT_REST);
        }
        final long[] busy = new long[2];
        helper.startSequence()
                .thenExecuteAfter(BUSY, () -> {
                    final Timings.Summary summary = summary();
                    busy[0] = summary.averageNanos() * all.size();
                    busy[1] = summary.maxNanos();
                })
                .thenWaitUntil(() -> helper.assertTrue(landing.stream().allMatch(
                        MovingStructurePhysicsBenchGameTests::asleep), "all those landing come to rest"))
                .thenExecuteAfter(10, () -> {
                    final Timings.Summary summary = summary();
                    final long floating = rafts.stream().filter(raft -> !asleep(raft)).count();
                    JsTests.LOGGER.info("PHYSICS BENCH {} structures: falling and landing {} ms a tick, the longest"
                                    + " of one {} ms; {} landed asleep and {} rafts afloat {} ms a tick", all.size(),
                            Diagnostics.millis(busy[0]), Diagnostics.millis(busy[1]), landing.size(), floating,
                            Diagnostics.millis(summary.averageNanos() * all.size()));
                    for (final MovingStructure structure : all) {
                        MovingStructures.takeAway(structure);
                    }
                })
                .thenSucceed();
    }

    /* The summary of the structures' physics in the diagnostics. */
    private static Timings.Summary summary() {
        return Diagnostics.timings().summaries().stream()
                .filter(each -> each.section().equals(StructurePhysics.TIMED.toString())).findFirst()
                .orElse(new Timings.Summary(StructurePhysics.TIMED.toString(), 0, 0, 0, 0));
    }

    /* A box of {@code block} from one corner to the other, made a structure. */
    private static MovingStructure box(final GameTestHelper helper, final int fromX, final int fromY,
                                       final int fromZ, final int toX, final int toY, final int toZ,
                                       final Block block) {
        for (int x = fromX; x <= toX; x++) {
            for (int y = fromY; y <= toY; y++) {
                for (int z = fromZ; z <= toZ; z++) {
                    helper.setBlock(new BlockPos(x, y, z), block);
                }
            }
        }
        warm(helper.getLevel());
        final StructureResult made = MovingStructures.assemble(helper.getLevel(),
                helper.absolutePos(new BlockPos(fromX, fromY, fromZ)), helper.absolutePos(new BlockPos(toX, toY,
                        toZ)));
        helper.assertTrue(made.ok() && made.structure() != null, "a structure is made");
        return made.structure();
    }

    /* A pool of water three deep over the floor, walled in glass, from {@code (fromX, fromZ)} to {@code (toX, toZ)}. */
    private static void pool(final GameTestHelper helper, final int fromX, final int fromZ, final int toX,
                             final int toZ) {
        for (int y = 2; y <= 4; y++) {
            for (int x = fromX; x <= toX; x++) {
                for (int z = fromZ; z <= toZ; z++) {
                    final boolean wall = x == fromX || x == toX || z == fromZ || z == toZ;
                    helper.setBlock(new BlockPos(x, y, z), wall ? Blocks.GLASS : Blocks.WATER);
                }
            }
        }
    }

    private static boolean asleep(final MovingStructure structure) {
        final StructurePhysics physics = structure.physics();
        return physics != null && physics.asleep();
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
