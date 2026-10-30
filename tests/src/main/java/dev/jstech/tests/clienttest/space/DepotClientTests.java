/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.space;

import dev.jstech.space.depot.DepotBlocks;
import dev.jstech.space.depot.DepotPieceBlock;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * A propellant depot on the player's game, as the approved one: built of its kit, a sphere nine blocks wide on legs two
 * blocks tall, it forms and its pump draws it whole, the welded sphere in rings of white plates, its eight legs and
 * their braces, the vent stack on top.
 */
public final class DepotClientTests {

    /* The sphere's lowest corner, its edge and its legs; where the player looks from. */
    private static final BlockPos LOW = new BlockPos(0, 4, 30);
    private static final int EDGE = 9;
    private static final int LEGS = 2;
    private static final Vec3 FROM = new Vec3(-12.0, 3.0, -10.0);
    private static final int ARRIVE = 200;
    private static final int SETTLE = 20;

    private DepotClientTests() {
    }

    @ClientTest(timeoutTicks = 800)
    public static void depot_isDrawnAsTheApprovedOne(final ClientTestContext ctx) {
        final Map<BlockPos, BlockState> was = new HashMap<>();
        ctx.afterTest(() -> ctx.server().execute(() -> was.forEach((at, state) -> ctx.serverLevel()
                .setBlockAndUpdate(at, state))));
        final BlockPos[] low = new BlockPos[1];
        ctx.thenServer(0, level -> low[0] = build(ctx, level, was))
                .thenWaitUntil(() -> low[0] != null && ctx.mc().level.getBlockState(low[0]).getBlock()
                        instanceof DepotPieceBlock && ctx.mc().level.getBlockState(low[0])
                        .getValue(DepotPieceBlock.FORMED), ARRIVE, "the formed depot to reach the player")
                .thenServer(0, level -> {
                    final Vec3 at = Vec3.atBottomCenterOf(low[0]).add(FROM);
                    final BlockPos under = BlockPos.containing(at).below();
                    was.putIfAbsent(under, level.getBlockState(under));
                    level.setBlockAndUpdate(under, Blocks.BARRIER.defaultBlockState());
                    level.players().getFirst().teleportTo(level, at.x, at.y, at.z, 0.0F, 0.0F);
                })
                .then(SETTLE, () -> ctx.lookAt(Vec3.atCenterOf(low[0]).add(EDGE / 2.0, EDGE / 2.0 - 1, EDGE / 2.0)))
                .then(SETTLE, () -> { })
                .thenScreenshot(0, "depot");
    }

    /* Builds the depot, keeping what stood there to put back; gives its sphere's lowest corner. */
    private static BlockPos build(final ClientTestContext ctx, final ServerLevel level,
                                  final Map<BlockPos, BlockState> was) {
        final BlockPos low = ctx.abs(LOW);
        for (int x = 0; x < EDGE; x++) {
            for (int y = 0; y < EDGE; y++) {
                for (int z = 0; z < EDGE; z++) {
                    put(level, was, low.offset(x, y, z), DepotBlocks.SPHERE_SHELL.get().defaultBlockState());
                }
            }
        }
        for (final int x : new int[] {0, EDGE - 1}) {
            for (final int z : new int[] {0, EDGE - 1}) {
                for (int k = 1; k <= LEGS; k++) {
                    put(level, was, low.offset(x, -k, z), DepotBlocks.TANK_LEG.get().defaultBlockState());
                }
            }
        }
        put(level, was, low.offset(EDGE / 2, EDGE, EDGE / 2), DepotBlocks.VENT_STACK.get().defaultBlockState());
        put(level, was, low.offset(-1, -LEGS, 0), DepotBlocks.TRANSFER_PUMP.get().defaultBlockState());
        return low;
    }

    private static void put(final ServerLevel level, final Map<BlockPos, BlockState> was, final BlockPos at,
                            final BlockState state) {
        was.putIfAbsent(at, level.getBlockState(at));
        level.setBlockAndUpdate(at, state);
    }
}
