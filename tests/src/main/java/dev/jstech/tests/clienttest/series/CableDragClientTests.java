/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.series;

import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.client.cable.CableDragClient;
import dev.jstech.core.jstechseries.lines.SharedPipes;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Dragging a run of pipe on a player's game: the click held, the run from its first block to the one looked at is
 * outlined; let go, it is laid all at once, each block joined to the next.
 */
public final class CableDragClientTests {

    private static final BlockPos STAND = new BlockPos(0, 2, -2);
    private static final BlockPos FROM = new BlockPos(-2, 2, 1);
    private static final BlockPos TO = new BlockPos(3, 2, 3);
    private static final int WAIT = 200;
    /* The run from FROM to TO: the first block, five along x, then two along z. */
    private static final int RUN = 8;

    private CableDragClientTests() {
    }

    @ClientTest(timeoutTicks = 600)
    public static void drag_outlinesTheRunAndLaysItWhenLetGo(final ClientTestContext ctx) {
        ctx.afterTest(() -> ctx.mc().options.keyUse.setDown(false));
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> clear(ctx, level))
                .thenGive(0, SharedPipes.COPPER_PIPE.stack(64))
                .then(2, () -> {
                    ctx.selectHotbar(0);
                    ctx.mc().options.keyUse.setDown(true);
                    CableDragClient.begin(InteractionHand.MAIN_HAND, ctx.abs(FROM));
                    ctx.lookAt(Vec3.atCenterOf(ctx.abs(TO.below())).add(0, 0.5, 0));
                })
                .thenWaitUntil(() -> CableDragClient.preview().size() == RUN, WAIT,
                        "the run to the block looked at to be outlined")
                .thenScreenshot(5, "drag-preview")
                .then(0, () -> ctx.mc().options.keyUse.setDown(false))
                .thenWaitUntilServer(level -> level.getBlockEntity(ctx.abs(TO)) instanceof CableBlockEntity cable
                        && cable.holds(SharedPipes.COPPER_PIPE.get()), WAIT, "the run to be laid when let go",
                        level -> "nothing at " + TO)
                .thenScreenshot(10, "drag-laid")
                .thenServer(0, level -> clear(ctx, level));
    }

    private static void clear(final ClientTestContext ctx, final ServerLevel level) {
        for (int x = FROM.getX(); x <= TO.getX(); x++) {
            for (int z = FROM.getZ(); z <= TO.getZ(); z++) {
                level.setBlockAndUpdate(ctx.abs(new BlockPos(x, FROM.getY(), z)), Blocks.AIR.defaultBlockState());
            }
        }
    }
}
