/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.series;

import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.cable.CableItem;
import dev.jstech.core.client.cable.CableDragClient;
import dev.jstech.core.jstechseries.lines.SharedPipes;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.testkit.client.ClientTestFailure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Dragging a run of pipe on a player's game: the click held, the run from its first block to the one looked at is
 * outlined; let go, it is laid all at once, each block joined to the next.
 */
public final class CableDragClientTests {

    private static final BlockPos STAND = new BlockPos(0, 2, -2);
    private static final BlockPos FROM = new BlockPos(-2, 2, 1);
    private static final BlockPos TO = new BlockPos(3, 2, 3);
    /* Where a snow layer lies on the ground, inside the run's area so clearing it clears that too. */
    private static final BlockPos SNOW = new BlockPos(0, 2, 1);
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

    /* A click on a snow layer, held a moment as a player's click is, lays one pipe in the snow's place. */
    @ClientTest(timeoutTicks = 600)
    public static void click_onASnowLayerLaysOnePipeInIt(final ClientTestContext ctx) {
        ctx.afterTest(() -> ctx.mc().options.keyUse.setDown(false));
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> {
                    clear(ctx, level);
                    level.setBlockAndUpdate(ctx.abs(SNOW), Blocks.SNOW.defaultBlockState());
                })
                .thenGive(0, SharedPipes.COPPER_PIPE.stack(64))
                .thenWaitUntil(() -> ctx.mc().level != null && ctx.mc().level.getBlockState(ctx.abs(SNOW))
                        .is(Blocks.SNOW), WAIT, "the snow layer to reach the player's game")
                .then(0, () -> {
                    ctx.selectHotbar(0);
                    ctx.lookAt(Vec3.atBottomCenterOf(ctx.abs(SNOW)).add(0, 1.0 / 16.0, 0));
                })
                .thenWaitUntil(() -> ctx.mc().hitResult instanceof BlockHitResult hit
                        && hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(ctx.abs(SNOW)), WAIT,
                        "the crosshair to rest on the snow")
                .then(0, () -> ctx.mc().options.keyUse.setDown(true))
                .then(4, () -> ctx.mc().options.keyUse.setDown(false))
                .thenWaitUntilServer(level -> level.getBlockEntity(ctx.abs(SNOW)) instanceof CableBlockEntity cable
                        && cable.holds(SharedPipes.COPPER_PIPE.get()), WAIT, "the pipe to take the snow's place",
                        level -> "at " + SNOW + ": " + level.getBlockState(ctx.abs(SNOW)))
                .thenServer(10, level -> {
                    if (!level.getBlockState(ctx.abs(SNOW.above())).isAir()) {
                        throw new ClientTestFailure("nothing is laid above the snow's place, but there is "
                                + level.getBlockState(ctx.abs(SNOW.above())));
                    }
                })
                .thenServer(0, level -> {
                    clear(ctx, level);
                    level.setBlockAndUpdate(ctx.abs(SNOW.above()), Blocks.AIR.defaultBlockState());
                });
    }

    /* A pipe laid beside another shows joined to it the first moment it shows at all, the two arriving together. */
    @ClientTest(timeoutTicks = 600)
    public static void laying_showsTheJoinTheMomentThePipeAppears(final ClientTestContext ctx) {
        final BlockPos first = SNOW;
        final BlockPos second = SNOW.east();
        final boolean[] joined = new boolean[2];
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> {
                    clear(ctx, level);
                    CableItem.lay(level, ctx.abs(first), SharedPipes.COPPER_PIPE.get());
                })
                .thenGive(0, SharedPipes.COPPER_PIPE.stack(64))
                .thenWaitUntil(() -> clientPipe(ctx, first) != null, WAIT, "the first pipe to reach the player's game")
                .then(0, () -> {
                    ctx.selectHotbar(0);
                    ctx.rightClick(second.below(), Direction.UP);
                })
                .thenWaitUntil(() -> {
                    final CableBlockEntity laid = clientPipe(ctx, second);
                    final CableBlockEntity before = clientPipe(ctx, first);
                    if (laid == null || before == null) {
                        return false;
                    }
                    joined[0] = before.crosses(SharedPipes.COPPER_PIPE.get(), Direction.EAST);
                    joined[1] = laid.crosses(SharedPipes.COPPER_PIPE.get(), Direction.WEST);
                    return true;
                }, WAIT, "the second pipe to reach the player's game")
                .thenAssert(0, () -> joined[0] && joined[1], "the first tick the new pipe shows, the two are joined"
                        + " both ways: " + joined[0] + ", " + joined[1])
                .thenScreenshot(2, "laid-joined")
                .thenServer(0, level -> clear(ctx, level));
    }

    /* The pipe at {@code relative} as the player's game has it, or null while it has none there. */
    private static @Nullable CableBlockEntity clientPipe(final ClientTestContext ctx, final BlockPos relative) {
        return ctx.mc().level != null && ctx.mc().level.getBlockEntity(ctx.abs(relative)) instanceof CableBlockEntity
                cable && cable.holds(SharedPipes.COPPER_PIPE.get()) ? cable : null;
    }

    private static void clear(final ClientTestContext ctx, final ServerLevel level) {
        for (int x = FROM.getX(); x <= TO.getX(); x++) {
            for (int z = FROM.getZ(); z <= TO.getZ(); z++) {
                level.setBlockAndUpdate(ctx.abs(new BlockPos(x, FROM.getY(), z)), Blocks.AIR.defaultBlockState());
            }
        }
    }
}
