/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.space;

import dev.jstech.space.launcher.LauncherBlocks;
import dev.jstech.space.launcher.LauncherPieceBlock;
import dev.jstech.space.pad.PadBlocks;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * A launch pad on the player's game, as the approved one: its flame trench along the launcher's length, lined with
 * Trench Lining on its walls and floor, the Flame Deflector's ridge across it under the opening, and the Pad Pedestals
 * three to a column either side, taking their shape; seen from the trench's end, as the approved trench is.
 */
public final class LaunchPadClientTests {

    /* The trench along x, five wide inside its walls, two deep under the ground at y 1; its deflector's row. */
    private static final int LENGTH = 12;
    private static final int HALF = 2;
    private static final int GROUND = 1;
    private static final int DEFLECTOR_X = 8;
    private static final BlockPos CORNER = new BlockPos(0, GROUND, 20);
    private static final int ARRIVE = 200;
    private static final int SETTLE = 20;

    private LaunchPadClientTests() {
    }

    @ClientTest(timeoutTicks = 800)
    public static void pad_isDrawnAsTheApprovedOne(final ClientTestContext ctx) {
        final Map<BlockPos, BlockState> was = new HashMap<>();
        final BlockPos[] pedestal = new BlockPos[1];
        ctx.afterTest(() -> ctx.server().execute(() -> was.forEach((at, state) -> ctx.serverLevel()
                .setBlockAndUpdate(at, state))));
        ctx.thenServer(0, level -> pedestal[0] = build(ctx, level, was))
                .thenWaitUntil(() -> pedestal[0] != null && ctx.mc().level.getBlockState(pedestal[0]).getBlock()
                        instanceof LauncherPieceBlock && ctx.mc().level.getBlockState(pedestal[0])
                        .getValue(LauncherPieceBlock.FORMED), ARRIVE, "the pad's pedestals to reach the player")
                .thenServer(0, level -> {
                    final BlockPos origin = ctx.abs(CORNER);
                    level.players().getFirst().teleportTo(level, origin.getX() + 0.5, origin.getY() - 1.0,
                            origin.getZ() + 0.5, 0.0F, 0.0F);
                })
                .then(SETTLE, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(CORNER.offset(DEFLECTOR_X, -1, 0)))))
                .then(SETTLE, () -> { })
                .thenScreenshot(0, "pad-trench");
    }

    /* Builds the pad, keeping what stood there to put back; gives the place of one of its pedestals. */
    private static BlockPos build(final ClientTestContext ctx, final ServerLevel level,
                                  final Map<BlockPos, BlockState> was) {
        final BlockPos corner = ctx.abs(CORNER);
        for (int x = -1; x <= LENGTH; x++) {
            for (int z = -HALF - 1; z <= HALF + 1; z++) {
                for (int y = -3; y <= 3; y++) {
                    final BlockPos at = corner.offset(x, y, z);
                    final boolean wall = Math.abs(z) == HALF + 1 && y < 0;
                    final boolean floor = y == -3;
                    put(level, was, at, wall || floor ? PadBlocks.TRENCH_LINING.get().defaultBlockState()
                            : Blocks.AIR.defaultBlockState());
                }
            }
        }
        for (int z = -HALF; z <= HALF; z++) {
            put(level, was, corner.offset(DEFLECTOR_X, -2, z), PadBlocks.FLAME_DEFLECTOR.get().defaultBlockState()
                    .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));
        }
        for (final int x : new int[] {2, 6, 10}) {
            for (final int z : new int[] {-HALF - 2, HALF + 2}) {
                for (int y = 0; y < 3; y++) {
                    put(level, was, corner.offset(x, y, z), LauncherBlocks.PAD_PEDESTAL.get().defaultBlockState());
                }
            }
        }
        return corner.offset(2, 1, -HALF - 2);
    }

    private static void put(final ServerLevel level, final Map<BlockPos, BlockState> was, final BlockPos at,
                            final BlockState state) {
        was.putIfAbsent(at, level.getBlockState(at));
        level.setBlockAndUpdate(at, state);
    }
}
