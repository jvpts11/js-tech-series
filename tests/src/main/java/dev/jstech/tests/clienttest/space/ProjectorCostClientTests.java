/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.space;

import com.mojang.logging.LogUtils;
import dev.jstech.core.jstechseries.projector.ProjectorSettings;
import dev.jstech.core.jstechseries.projector.ProjectorView;
import dev.jstech.core.jstechseries.projector.StructureProjector;
import dev.jstech.core.jstechseries.projector.StructureProjectorBlockEntity;
import dev.jstech.core.jstechseries.projector.client.ProjectorClient;
import dev.jstech.space.hangar.HangarStructure;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * What the Structure Projector costs a frame when it shows the biggest multiblock of the series, the Rocket Hangar:
 * tens of thousands of ghosts, which drawn afresh every frame took the game down to a few frames a second.
 */
public final class ProjectorCostClientTests {

    private static final Logger LOGGER = LogUtils.getLogger();
    /*
     * A projector on the land's grass, as a player south of it places it, the hangar shown north of it; the eyes far
     * behind and over it, so the whole building is in sight; the middle of the building.
     */
    private static final BlockPos PROJECTOR = new BlockPos(60, 2, 70);
    private static final Vec3 EYES = new Vec3(0, 40, 40);
    private static final Vec3 MIDDLE = new Vec3(0, 40, -30);
    /* The grid the frame is compared on, in steps of the screen, and how far a cell must change to count. */
    private static final int STEP = 8;
    private static final int CHANGED = 24;
    private static final int CHANNEL = 0xFF;
    /* A share of the grid the hangar's ghosts must change, in hundredths. */
    private static final int MOST_OF_IT = 20;
    private static final int HUNDRED = 100;
    /* The frames measured, one each tick, after the ghosts have had time to be built. */
    private static final int MEASURED = 40;
    private static final int BUILT = 60;
    /* What drawing the whole hangar may cost a frame on the game's thread, in nanoseconds. */
    private static final long MOST_NANOS = 3_000_000L;
    private static final int ARRIVE = 600;
    private static final int SETTLE = 20;

    private ProjectorCostClientTests() {
    }

    @ClientTest(timeoutTicks = 1800)
    public static void projector_showsTheRocketHangarWithoutSlowingTheGame(final ClientTestContext ctx) {
        final ServerLevel[] placed = new ServerLevel[1];
        final int[][] before = new int[1][];
        final long[] nanos = new long[MEASURED];
        final int[] changed = new int[2];
        ctx.afterTest(() -> ctx.server().execute(() -> {
            if (placed[0] != null) {
                placed[0].setBlock(ctx.abs(PROJECTOR), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                placed[0].players().forEach(player -> player.setGameMode(GameType.CREATIVE));
            }
        }));
        ctx.thenServer(0, level -> {
            placed[0] = level;
            level.setBlock(ctx.abs(PROJECTOR), StructureProjector.BLOCK.get().defaultBlockState()
                    .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH), Block.UPDATE_ALL);
            ctx.serverPlayer().setGameMode(GameType.SPECTATOR);
            final Vec3 eyes = Vec3.atCenterOf(ctx.abs(PROJECTOR)).add(EYES);
            ctx.serverPlayer().teleportTo(level, eyes.x, eyes.y - ctx.serverPlayer().getEyeHeight(), eyes.z, 0, 0);
        }).thenWaitUntil(() -> projector(ctx) != null, ARRIVE, "the projector to reach the player's game")
                .then(2, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(PROJECTOR)).add(MIDDLE)))
                .then(SETTLE, () -> before[0] = grid(ctx))
                .thenServer(0, level -> {
                    if (level.getBlockEntity(ctx.abs(PROJECTOR)) instanceof StructureProjectorBlockEntity projector) {
                        projector.apply(ProjectorSettings.FRESH.choosing(HangarStructure.ID));
                    }
                })
                .thenWaitUntil(() -> projector(ctx) != null && projector(ctx).settings().chosen().isPresent(), ARRIVE,
                        "the projector to show the hangar");
        for (int i = 0; i < MEASURED; i++) {
            final int frame = i;
            ctx.then(frame == 0 ? BUILT : 1, () -> nanos[frame] = ProjectorClient.lastDrawNanos());
        }
        ctx.thenScreenshot(0, "projector-hangar")
                .then(0, () -> {
                    final int[] after = grid(ctx);
                    for (int i = 0; i < after.length; i++) {
                        changed[0] += differs(before[0][i], after[i]) ? 1 : 0;
                    }
                    changed[1] = after.length;
                    long sum = 0;
                    long most = 0;
                    for (final long each : nanos) {
                        sum += each;
                        most = Math.max(most, each);
                    }
                    final long mean = sum / MEASURED;
                    final long reading = System.nanoTime();
                    ProjectorView.of(ctx.mc().level, projector(ctx));
                    final long read = System.nanoTime() - reading;
                    LOGGER.info("[PROJECTOR] the hangar costs {} us a frame on the game's thread, at most {} us; "
                            + "{} fps; reading it from the world {} us", mean / 1000, most / 1000, ctx.mc().getFps(),
                            read / 1000);
                    ctx.assertTrue(changed[0] * HUNDRED > changed[1] * MOST_OF_IT, "the hangar's ghosts are drawn over "
                            + "the land: " + changed[0] + " of " + changed[1] + " places of the frame changed");
                    ctx.assertTrue(mean < MOST_NANOS, "drawing the hangar costs a frame " + mean / 1000
                            + " microseconds on the game's thread, at most " + most / 1000 + "; the frame rate is "
                            + ctx.mc().getFps());
                });
    }

    /* The projector shown on this player's side, once it is there. */
    @Nullable
    private static StructureProjectorBlockEntity projector(final ClientTestContext ctx) {
        return ctx.mc().level != null && ctx.mc().level.getBlockEntity(ctx.abs(PROJECTOR))
                instanceof StructureProjectorBlockEntity projector ? projector : null;
    }

    /* The frame's colours on a coarse grid, row by row. */
    private static int[] grid(final ClientTestContext ctx) {
        final int wide = ctx.mc().getWindow().getGuiScaledWidth();
        final int tall = ctx.mc().getWindow().getGuiScaledHeight();
        final int[] out = new int[(wide / STEP) * (tall / STEP)];
        try (ClientTestContext.FrameSampler frame = ctx.sampleFrame()) {
            int i = 0;
            for (int y = 0; y < tall / STEP; y++) {
                for (int x = 0; x < wide / STEP; x++) {
                    out[i++] = frame.pixel(x * STEP + STEP / 2, y * STEP + STEP / 2);
                }
            }
        }
        return out;
    }

    /* Whether two colours stand apart by more than a little in any channel. */
    private static boolean differs(final int a, final int b) {
        return Math.abs((a >> 16 & CHANNEL) - (b >> 16 & CHANNEL)) > CHANGED
                || Math.abs((a >> 8 & CHANNEL) - (b >> 8 & CHANNEL)) > CHANGED
                || Math.abs((a & CHANNEL) - (b & CHANNEL)) > CHANGED;
    }
}
