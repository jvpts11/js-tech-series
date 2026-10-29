/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.space;

import dev.jstech.space.hangar.HangarBlocks;
import dev.jstech.space.hangar.HangarPieceBlock;
import dev.jstech.space.hangar.HangarTerminalBlockEntity;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * A Rocket Hangar on a player's game: built of stone round its bay, its crane formed, the bridge across the bay is
 * drawn as its yellow girder, seen from the bay's floor, where the open roof above it would be grey stone.
 */
public final class RocketHangarClientTests {

    /* The bay, from (2, 2, 6) to (8, 13, 12) of the test's place, the player standing on its floor. */
    private static final int X0 = 2;
    private static final int X1 = 8;
    private static final int Z0 = 6;
    private static final int Z1 = 12;
    private static final int Y0 = 2;
    private static final int TOP = 13;
    private static final BlockPos TERMINAL = new BlockPos(5, 3, Z1 + 1);
    private static final BlockPos EYE = new BlockPos(5, 2, 7);
    private static final BlockPos BRIDGE = new BlockPos(5, TOP, 9);
    private static final int ARRIVE = 400;
    private static final int SETTLE = 20;
    private static final int PATCH = 4;
    private static final int CHANNEL = 0xFF;
    private static final int YELLOW = 40;
    private static final int LAMPS = 3;

    private RocketHangarClientTests() {
    }

    @ClientTest(timeoutTicks = 1200)
    public static void hangar_drawsItsFormedBridgeAsAGirder(final ClientTestContext ctx) {
        final int[][] seen = new int[1][];
        ctx.afterTest(() -> ctx.server().execute(() -> clear(ctx, ctx.serverLevel())));
        ctx.thenServer(0, level -> build(ctx, level))
                .thenTeleport(SETTLE, EYE, Direction.NORTH)
                .thenWaitUntil(() -> ctx.mc().level.getBlockState(ctx.abs(BRIDGE)).is(HangarBlocks.CRANE_BRIDGE.get())
                        && ctx.mc().level.getBlockState(ctx.abs(BRIDGE)).getValue(HangarPieceBlock.FORMED), ARRIVE,
                        "the formed hangar to reach the player")
                .then(SETTLE, () -> ctx.lookAt(Vec3.atBottomCenterOf(ctx.abs(BRIDGE))))
                .then(SETTLE, () -> seen[0] = mean(ctx))
                .thenScreenshot(0, "hangar-formed")
                .then(0, () -> ctx.assertTrue(seen[0][0] - seen[0][2] > YELLOW && seen[0][1] - seen[0][2] > YELLOW,
                        "the bridge's girder is drawn yellow over the bay: " + seen[0][0] + ", " + seen[0][1] + ", "
                                + seen[0][2]));
    }

    /* The hangar of stone round the bay, its terminal in the south wall, its crane, its door; formed at once. */
    private static void build(final ClientTestContext ctx, final ServerLevel level) {
        for (int x = X0 - 1; x <= X1 + 1; x++) {
            for (int z = Z0 - 1; z <= Z1 + 1; z++) {
                for (int y = Y0 - 1; y <= TOP + 1; y++) {
                    final boolean shell = x < X0 || x > X1 || z < Z0 || z > Z1 || y < Y0 || y > TOP;
                    // Lamps in the roof, as a hangar has, so the bay is lit.
                    final boolean lamp = y > TOP && (x - X0) % LAMPS == 1 && (z - Z0) % LAMPS == 1;
                    level.setBlockAndUpdate(ctx.abs(new BlockPos(x, y, z)), lamp ? Blocks.GLOWSTONE
                            .defaultBlockState() : shell ? Blocks.STONE.defaultBlockState()
                            : Blocks.AIR.defaultBlockState());
                }
            }
        }
        level.setBlockAndUpdate(ctx.abs(TERMINAL), HangarBlocks.TERMINAL.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        final BlockState rail = HangarBlocks.CRANE_RAIL.get().defaultBlockState();
        for (int x = X0; x <= X1; x++) {
            level.setBlockAndUpdate(ctx.abs(new BlockPos(x, TOP, Z0)), rail);
            level.setBlockAndUpdate(ctx.abs(new BlockPos(x, TOP, Z1)), rail);
        }
        for (int z = Z0 + 1; z < Z1; z++) {
            level.setBlockAndUpdate(ctx.abs(new BlockPos(5, TOP, z)), HangarBlocks.CRANE_BRIDGE.get()
                    .defaultBlockState());
        }
        level.setBlockAndUpdate(ctx.abs(new BlockPos(5, TOP - 1, 10)), HangarBlocks.CRANE_HOIST.get()
                .defaultBlockState());
        level.setBlockAndUpdate(ctx.abs(new BlockPos(X0 - 1, Y0, 9)), HangarBlocks.BAY_DOOR.get().defaultBlockState());
        for (int x = X0; x <= X1; x += 2) {
            level.setBlockAndUpdate(ctx.abs(new BlockPos(x, 6, Z0 + 2)), HangarBlocks.WORK_PLATFORM.get()
                    .defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        }
        ((HangarTerminalBlockEntity) level.getBlockEntity(ctx.abs(TERMINAL))).look();
    }

    private static void clear(final ClientTestContext ctx, final ServerLevel level) {
        for (int x = X0 - 1; x <= X1 + 1; x++) {
            for (int z = Z0 - 1; z <= Z1 + 1; z++) {
                for (int y = Y0 - 1; y <= TOP + 1; y++) {
                    level.setBlockAndUpdate(ctx.abs(new BlockPos(x, y, z)), y == Y0 - 1 ? Blocks.GRASS_BLOCK
                            .defaultBlockState() : Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    /* The mean colour of a patch beside the crosshair, which whitens its own middle. */
    private static int[] mean(final ClientTestContext ctx) {
        final int x = ctx.mc().getWindow().getGuiScaledWidth() / 2 + PATCH;
        final int y = ctx.mc().getWindow().getGuiScaledHeight() / 2 + PATCH;
        final long[] sum = new long[3];
        int count = 0;
        try (ClientTestContext.FrameSampler frame = ctx.sampleFrame()) {
            for (int dx = -PATCH / 2; dx <= PATCH / 2; dx++) {
                for (int dy = -PATCH / 2; dy <= PATCH / 2; dy++) {
                    final int argb = frame.pixel(x + dx, y + dy);
                    sum[0] += argb >> 16 & CHANNEL;
                    sum[1] += argb >> 8 & CHANNEL;
                    sum[2] += argb & CHANNEL;
                    count++;
                }
            }
        }
        return new int[] {(int) (sum[0] / count), (int) (sum[1] / count), (int) (sum[2] / count)};
    }
}
