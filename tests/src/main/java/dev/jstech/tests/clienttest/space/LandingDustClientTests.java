/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.space;

import dev.jstech.core.client.effect.ClientEffects;
import dev.jstech.core.client.effect.EffectEngine;
import dev.jstech.core.client.moving.ClientMovingStructures;
import dev.jstech.core.client.moving.ClientStructure;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.space.descent.DescentFlight;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The dust a lander's or a ship's jets raise from Mars' ground stops at its hull: under a closed hut made a moving
 * structure, as a ship's cabin is, the dust raised from the ground rises round it and against its floor, and none of
 * it ever stands in the air inside. Its blocks are kept in its yard, where the world's blocks a particle stops at
 * know nothing of them; dust once came up through a ship's floor into its cabin.
 */
public final class LandingDustClientTests {

    /* The scene, its ground's top, and the hut: its floor, its walls and its roof, a room of air inside. */
    private static final int X0 = -4;
    private static final int X1 = 4;
    private static final int Z0 = 4;
    private static final int Z1 = 14;
    private static final int Y_BOTTOM = -2;
    private static final int Y_GRASS = 1;
    private static final int Y_TOP = 12;
    private static final BlockPos HUT_FROM = new BlockPos(-2, 5, 7);
    private static final BlockPos HUT_TO = new BlockPos(2, 8, 11);
    private static final BlockPos STAND = new BlockPos(0, 2, 20);
    private static final Vec3 UP = new Vec3(0, 1, 0);
    private static final int ARRIVE = 600;
    private static final int SETTLE = 10;
    /* How often the dust is raised, as a ship's jets raise it, and for how long its rising is watched, in ticks. */
    private static final int RAISED_EVERY = 10;
    private static final int WATCHED = 80;

    private LandingDustClientTests() {
    }

    @ClientTest(timeoutTicks = 1500)
    public static void dust_raisedUnderAClosedHutNeverComesInsideIt(final ClientTestContext ctx) {
        final MovingStructure[] hut = new MovingStructure[1];
        final int[] inside = new int[1];
        final int[] raised = new int[1];
        ctx.afterTest(() -> ctx.server().execute(() -> {
            if (hut[0] != null) {
                MovingStructures.takeAway(hut[0]);
            }
            clear(ctx, ctx.serverLevel());
        }));
        ctx.thenServer(0, level -> {
            clear(ctx, level);
            for (final BlockPos at : BlockPos.betweenClosed(HUT_FROM, HUT_TO)) {
                final boolean shell = at.getY() == HUT_FROM.getY() || at.getY() == HUT_TO.getY()
                        || at.getX() == HUT_FROM.getX() || at.getX() == HUT_TO.getX()
                        || at.getZ() == HUT_FROM.getZ() || at.getZ() == HUT_TO.getZ();
                if (shell) {
                    level.setBlockAndUpdate(ctx.abs(at), Blocks.STONE.defaultBlockState());
                }
            }
            hut[0] = MovingStructures.assemble(level, ctx.abs(HUT_FROM), ctx.abs(HUT_TO)).structure();
        })
                .thenTeleport(0, STAND, Direction.NORTH)
                .thenAwaitLandDrawn(ARRIVE)
                .thenWaitUntil(() -> drawn(hut[0]), ARRIVE, "the hut to reach the player's game")
                .then(SETTLE, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(new BlockPos(0, Y_GRASS + 2, 9)))));
        final Vec3 foot = Vec3.atBottomCenterOf(new BlockPos(0, Y_GRASS + 1, 9));
        final AABB room = new AABB(Vec3.atLowerCornerOf(HUT_FROM.offset(1, 1, 1)), Vec3.atLowerCornerOf(HUT_TO));
        for (int tick = 0; tick < WATCHED; tick++) {
            final boolean raise = tick % RAISED_EVERY == 0;
            ctx.then(1, () -> {
                final Vec3 origin = Vec3.atLowerCornerOf(ctx.abs(BlockPos.ZERO));
                if (raise) {
                    ClientEffects.play(DescentFlight.MARS_DUST, foot.add(origin), UP);
                }
                raised[0] = Math.max(raised[0], EffectEngine.particles());
                inside[0] = Math.max(inside[0], EffectEngine.within(room.move(origin)));
            });
        }
        ctx.thenScreenshot(0, "landing-dust-under-the-hut")
                .then(0, () -> {
                    ctx.assertTrue(raised[0] > 0, "the dust was raised: " + raised[0] + " particles at most");
                    ctx.assertTrue(inside[0] == 0, "none of it stood in the hut's room: " + inside[0]
                            + " particles inside at most");
                });
    }

    /* Whether the hut has reached the player's game and been built there. */
    private static boolean drawn(final MovingStructure hut) {
        final ClientStructure seen = hut == null ? null : ClientMovingStructures.byPlot(hut.plot());
        return seen != null && seen.ready() && seen.meshes().built() > 0 && !seen.meshes().waiting();
    }

    private static void clear(final ClientTestContext ctx, final ServerLevel level) {
        for (int x = X0; x <= X1; x++) {
            for (int z = Z0; z <= Z1; z++) {
                for (int y = Y_BOTTOM; y <= Y_TOP; y++) {
                    final Block block = y == Y_BOTTOM ? Blocks.BEDROCK : y < Y_GRASS ? Blocks.DIRT
                            : y == Y_GRASS ? Blocks.GRASS_BLOCK : Blocks.AIR;
                    level.setBlock(ctx.abs(new BlockPos(x, y, z)), block.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
    }
}
