/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.world.field.ChunkFields;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestWorldChanges;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Fields over the chunks: a value added where its source is spreads to the four neighbours and fades a step at a
 * time, never below nothing; what a player near it is sent; and a field that moves on by itself, away from the
 * server's thread. Each test works at chunks of its own each run, far from the others.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ChunkFieldGameTests {

    private static final String ARENA = "empty";
    private static final float SOURCE = 100F;
    private static final float CLOSE = 0.01F;
    private static final int FAR = 50_000;
    private static final int SPREAD = 100_000;

    private ChunkFieldGameTests() {
    }

    @GameTest(template = ARENA)
    public static void field_spreadsToItsNeighboursAndFadesAStepAtATime(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final ChunkPos at = farChunk();
        TestWorldChanges.STILL.add(level, at, SOURCE);
        helper.assertTrue(TestWorldChanges.STILL.get(level, at) == SOURCE, "what is added is there");
        TestWorldChanges.STILL.stepNow(level);
        // A fifth spreads and a tenth fades: the middle keeps 100 * 0.8 * 0.9, each side 100 * 0.05 * 0.9.
        helper.assertTrue(Math.abs(TestWorldChanges.STILL.get(level, at) - 72F) < CLOSE,
                "the middle keeps its share, not " + TestWorldChanges.STILL.get(level, at));
        final ChunkPos east = new ChunkPos(at.x + 1, at.z);
        helper.assertTrue(Math.abs(TestWorldChanges.STILL.get(level, east) - 4.5F) < CLOSE
                && Math.abs(TestWorldChanges.STILL.get(level, new ChunkPos(at.x, at.z - 1)) - 4.5F) < CLOSE,
                "each neighbour gets a quarter of what spreads");
        helper.assertTrue(TestWorldChanges.STILL.get(level, new ChunkPos(at.x + 1, at.z + 1)) == 0,
                "and a corner nothing yet");
        final float[] near = TestWorldChanges.STILL.near(level, at, 1);
        helper.assertTrue(near.length == 9 && near[4] == TestWorldChanges.STILL.get(level, at)
                && near[5] == TestWorldChanges.STILL.get(level, east), "a player near it is sent the chunks round");
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                TestWorldChanges.STILL.add(level, new ChunkPos(at.x + dx, at.z + dz), -SOURCE);
            }
        }
        helper.assertTrue(TestWorldChanges.STILL.get(level, at) == 0 && TestWorldChanges.STILL.get(level, east) == 0,
                "taking away more than is there leaves nothing, never less");
        helper.assertTrue(ChunkFields.byId(TestWorldChanges.STILL.id()).isPresent()
                && TestWorldChanges.STILL.synced(), "the field is declared, and sent to the players near it");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void field_movesOnByItselfAwayFromTheTick(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final ChunkPos at = farChunk();
        final ChunkPos north = new ChunkPos(at.x, at.z - 1);
        TestWorldChanges.DRIFT.add(level, at, SOURCE);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(TestWorldChanges.DRIFT.get(level, north) > 0,
                        "the field has not spread yet"))
                .thenExecute(() -> helper.assertTrue(TestWorldChanges.DRIFT.get(level, at) < SOURCE,
                        "it spread from where it was added, by itself"))
                .thenSucceed();
    }

    /* A chunk far out, of its own each run. */
    private static ChunkPos farChunk() {
        final int offset = Math.floorMod(UUID.randomUUID().hashCode(), SPREAD);
        return new ChunkPos(FAR + offset * 4, FAR - offset * 4);
    }
}
