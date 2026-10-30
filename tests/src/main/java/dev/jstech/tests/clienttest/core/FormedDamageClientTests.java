/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import dev.jstech.core.multiblock.damage.DamageStamps;
import dev.jstech.core.multiblock.damage.FormedDamage;
import dev.jstech.testkit.client.ClientTestHelper;
import dev.jstech.tests.TestModels;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/**
 * Damage to a formed structure's model on a player's game: an impact laid on the test turbine's top on the server is
 * drawn there, the faces round it pushed in and holed and its rim torn bare, and once erased the turbine is drawn
 * whole again, as it was.
 */
public final class FormedDamageClientTests {

    /*
     * The turbine facing north, its terminal here, its body north of it; the player on a barrier south of it. Its
     * north-west top corner is struck, out of the way of the rotor, which turns all the while.
     */
    private static final BlockPos TERMINAL = new BlockPos(0, 2, 0);
    private static final BlockPos STRUCK = TERMINAL.offset(-1, 1, -2);
    private static final BlockPos STAND = new BlockPos(0, 5, 1);
    private static final int WAIT = 300;
    private static final int PUFFS_CLEAR = 40;
    /* Ticks for the server's word of the damage to reach the player's game and be drawn. */
    private static final int SENT = 10;
    private static final double SIZE = 0.8;
    private static final long SEED = 7;
    /* Where across the struck block it is struck, along x and along z: its plating, clear of the hub. */
    private static final double ACROSS = 3.0 / 16;
    /*
     * The window of the screen read round the crosshair, in the GUI's pixels; how different a pixel is, at least, how
     * many must differ to show the hole, and how few may once mended.
     */
    private static final int REACH = 30;
    private static final int DIFFERENT = 40;
    private static final int HOLED = 200;
    private static final int MENDED = 20;

    private FormedDamageClientTests() {
    }

    @ClientTest(timeoutTicks = 1200)
    public static void turbine_isDrawnHoledWhereStruckAndWholeOnceErased(final ClientTestContext ctx) {
        final int[][][] seen = new int[4][][];
        ctx.afterTest(() -> ctx.mc().gameRenderer.setRenderBlockOutline(true));
        ctx.thenServer(0, level -> build(ctx, level))
                .thenTeleport(0, STAND, Direction.NORTH)
                .thenWaitUntilServer(level -> level.getBlockEntity(ctx.abs(TERMINAL))
                        instanceof TestModels.TurbineEntity turbine && turbine.structure().merged(), WAIT,
                        "the turbine to form and merge", level -> level.getBlockEntity(ctx.abs(TERMINAL))
                        instanceof TestModels.TurbineEntity turbine ? String.valueOf(turbine.structure().structure())
                        : "no turbine")
                .then(PUFFS_CLEAR, () -> {
                    // The outline would change with the faces too: only the model is compared.
                    ctx.mc().gameRenderer.setRenderBlockOutline(false);
                    final BlockPos pos = ctx.abs(STRUCK);
                    ctx.lookAt(new Vec3(pos.getX() + ACROSS, pos.getY() + 1, pos.getZ() + ACROSS));
                })
                .then(3, () -> seen[0] = window(ctx))
                // A second look at the whole turbine: how much the picture flickers frame to frame by itself.
                .then(SENT, () -> seen[3] = window(ctx))
                .thenScreenshot(0, "formed-damage-whole")
                .thenServer(0, level -> {
                    final BlockPos pos = ctx.abs(STRUCK);
                    final int reached = FormedDamage.strike(level, pos, DamageStamps.IMPACT, top(level, pos),
                            new Vec3(0, 1, 0), SIZE, SEED);
                    ctx.assertTrue(reached > 0, "the impact is laid on the turbine");
                })
                .then(SENT, () -> seen[1] = window(ctx))
                .thenScreenshot(3, "formed-damage-holed")
                .thenServer(0, level -> FormedDamage.erase(level, ctx.abs(STRUCK)))
                .then(SENT, () -> {
                    seen[2] = window(ctx);
                    final int flicker = differing(seen[0], seen[3]);
                    ctx.assertTrue(differing(seen[0], seen[1]) >= HOLED + flicker, "the hole is drawn where it struck: "
                            + differing(seen[0], seen[1]) + " pixels changed, the picture flickering by " + flicker);
                    ctx.assertTrue(differing(seen[0], seen[2]) <= MENDED + flicker, "and once erased the turbine is"
                            + " drawn as it was: " + differing(seen[0], seen[2]) + " pixels still apart, the picture"
                            + " flickering by " + flicker);
                })
                .thenServer(0, level -> clear(ctx, level));
    }

    /* The turbine facing north with its terminal at {@link #TERMINAL}, and a barrier for the player to stand on. */
    private static void build(final ClientTestContext ctx, final ServerLevel level) {
        for (int y = 0; y < 2; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -2; z <= 0; z++) {
                    final BlockPos at = TERMINAL.offset(x, y, z);
                    if (!at.equals(TERMINAL)) {
                        level.setBlockAndUpdate(ctx.abs(at), Blocks.IRON_BLOCK.defaultBlockState());
                    }
                }
            }
        }
        level.setBlockAndUpdate(ctx.abs(TERMINAL), TestModels.TURBINE_BLOCK.get().defaultBlockState());
        level.setBlockAndUpdate(ctx.abs(STAND.below()), Blocks.BARRIER.defaultBlockState());
    }

    private static void clear(final ClientTestContext ctx, final ServerLevel level) {
        for (final BlockPos at : BlockPos.betweenClosed(ctx.abs(TERMINAL.offset(-1, 0, -2)),
                ctx.abs(TERMINAL.offset(1, 1, 0)))) {
            level.removeBlock(at, false);
        }
        level.removeBlock(ctx.abs(STAND.below()), false);
    }

    /* Where a ray straight down through the block at {@code pos}, over its plating, meets its top, on the server. */
    private static Vec3 top(final ServerLevel level, final BlockPos pos) {
        final BlockHitResult hit = level.clip(new ClipContext(new Vec3(pos.getX() + ACROSS, pos.getY() + 3,
                pos.getZ() + ACROSS), new Vec3(pos.getX() + ACROSS, pos.getY() - 1, pos.getZ() + ACROSS),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
        return hit.getLocation();
    }

    /* The brightness of the pixels round the crosshair. */
    private static int[][] window(final ClientTestContext ctx) {
        final int x = ctx.mc().getWindow().getGuiScaledWidth() / 2;
        final int y = ctx.mc().getWindow().getGuiScaledHeight() / 2;
        final int[][] out = new int[2 * REACH + 1][2 * REACH + 1];
        try (ClientTestHelper.FrameSampler frame = ctx.sampleFrame()) {
            for (int dx = -REACH; dx <= REACH; dx++) {
                for (int dy = -REACH; dy <= REACH; dy++) {
                    final int argb = frame.pixel(x + dx, y + dy);
                    out[dx + REACH][dy + REACH] = (argb >> 16 & 0xFF) + (argb >> 8 & 0xFF) + (argb & 0xFF);
                }
            }
        }
        return out;
    }

    /* How many pixels clearly differ between {@code a} and {@code b}. */
    private static int differing(final int[][] a, final int[][] b) {
        int count = 0;
        for (int i = 0; i < a.length; i++) {
            for (int j = 0; j < a[i].length; j++) {
                if (Math.abs(a[i][j] - b[i][j]) > DIFFERENT) {
                    count++;
                }
            }
        }
        return count;
    }
}
