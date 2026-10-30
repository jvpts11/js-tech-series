/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.space;

import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.multiblock.plan.PlannedBuild;
import dev.jstech.space.hangar.HangarBlocks;
import dev.jstech.space.hangar.HangarTerminalBlockEntity;
import dev.jstech.space.hangar.RocketDesign;
import dev.jstech.space.rocket.RocketStack;
import dev.jstech.space.rocket.SkeletonRocketParts;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * A rocket built by its hangar's plan on a player's game: once every block of its parts is down, the parts form, their
 * blocks merging into nothing the world draws, and the hangar draws each part with its own model where it stands, so
 * the rocket is seen where the bay's far wall was, not the wall through invisible blocks.
 */
public final class RocketBuildClientTests {

    /* The bay, from (-2, 2, 4) to (12, 17, 18) of the test's place; the rocket on (5, 2, 11). */
    private static final int X0 = -2;
    private static final int X1 = 12;
    private static final int Z0 = 4;
    private static final int Z1 = 18;
    private static final int Y0 = 2;
    private static final int TOP = 17;
    private static final int MIDDLE_X = 5;
    private static final BlockPos TERMINAL = new BlockPos(MIDDLE_X, 3, Z1 + 1);
    private static final BlockPos EYE = new BlockPos(MIDDLE_X, 2, Z1 - 1);
    private static final Vec3 ROCKET = new Vec3(MIDDLE_X + 0.5, Y0 + 3.5, 11.5);
    private static final int ARRIVE = 400;
    private static final int FORM = 200;
    private static final int SETTLE = 20;
    private static final int PATCH = 2;
    private static final int OFF_CROSSHAIR = 6;
    private static final int CHANNEL = 0xFF;
    private static final int APART = 45;
    private static final int GREENER = 25;
    private static final int LAMPS = 3;
    private static final int PARTS = 3;

    private RocketBuildClientTests() {
    }

    @ClientTest(timeoutTicks = 2400)
    public static void rocket_isDrawnByItsModelsOnceItsPartsForm(final ClientTestContext ctx) {
        final int[][] seen = new int[2][];
        ctx.afterTest(() -> ctx.server().execute(() -> clear(ctx, ctx.serverLevel())));
        ctx.thenServer(0, level -> build(ctx, level))
                .thenTeleport(SETTLE, EYE, Direction.NORTH)
                .thenWaitUntil(() -> ctx.mc().level.getBlockEntity(ctx.abs(TERMINAL))
                        instanceof HangarTerminalBlockEntity terminal && terminal.bay().isPresent(), ARRIVE,
                        "the formed hangar to reach the player")
                .then(SETTLE, () -> ctx.lookAt(Vec3.atLowerCornerOf(ctx.abs(BlockPos.ZERO)).add(ROCKET)))
                .then(SETTLE, () -> seen[0] = mean(ctx))
                .thenServer(0, level -> lay(ctx, level))
                .thenWaitUntil(() -> ctx.mc().level.getBlockEntity(ctx.abs(TERMINAL))
                        instanceof HangarTerminalBlockEntity terminal && terminal.formedParts().size() == PARTS,
                        FORM, "the rocket's parts to form on the player's game")
                .then(SETTLE, () -> seen[1] = mean(ctx))
                .thenScreenshot(0, "rocket-built")
                .then(0, () -> {
                    ctx.assertTrue(green(seen[0]), "before it is built, the green far wall is seen: " + str(seen[0]));
                    ctx.assertTrue(apart(seen[0], seen[1]) > APART && !green(seen[1]), "the formed rocket is drawn "
                            + "by its models where the far wall was: " + str(seen[0]) + " then " + str(seen[1]));
                });
    }

    /* The hangar, lamps in its roof and along its walls, its far wall green, formed. */
    private static void build(final ClientTestContext ctx, final ServerLevel level) {
        for (int x = X0 - 1; x <= X1 + 1; x++) {
            for (int z = Z0 - 1; z <= Z1 + 1; z++) {
                for (int y = Y0 - 1; y <= TOP + 1; y++) {
                    final boolean shell = x < X0 || x > X1 || z < Z0 || z > Z1 || y < Y0 || y > TOP;
                    final boolean lamp = shell && (y > TOP && (x - X0) % LAMPS == 1 && (z - Z0) % LAMPS == 1
                            || y == Y0 + LAMPS && Math.floorMod(x + z, LAMPS) == 0);
                    // The far wall behind the rocket a green nothing of the rocket is, so its model shows against it.
                    final BlockState wall = z < Z0 && y >= Y0 && y <= TOP ? Blocks.LIME_CONCRETE.defaultBlockState()
                            : Blocks.STONE.defaultBlockState();
                    level.setBlockAndUpdate(ctx.abs(new BlockPos(x, y, z)), lamp ? Blocks.GLOWSTONE
                            .defaultBlockState() : shell ? wall : Blocks.AIR.defaultBlockState());
                }
            }
        }
        level.setBlockAndUpdate(ctx.abs(TERMINAL), HangarBlocks.TERMINAL.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        final BlockState rail = HangarBlocks.CRANE_RAIL.get().defaultBlockState();
        for (int x = X0; x <= X1; x++) {
            level.setBlockAndUpdate(ctx.abs(new BlockPos(x, TOP - 1, Z0)), rail);
            level.setBlockAndUpdate(ctx.abs(new BlockPos(x, TOP - 1, Z1)), rail);
        }
        for (int z = Z0; z <= Z1; z++) {
            level.setBlockAndUpdate(ctx.abs(new BlockPos(X1 - 1, TOP, z)), HangarBlocks.CRANE_BRIDGE.get()
                    .defaultBlockState());
        }
        level.setBlockAndUpdate(ctx.abs(new BlockPos(X1 - 1, TOP - 1, Z0 + 2)), HangarBlocks.CRANE_HOIST.get()
                .defaultBlockState());
        level.setBlockAndUpdate(ctx.abs(new BlockPos(X0 - 1, Y0, Z0 + 5)), HangarBlocks.BAY_DOOR.get()
                .defaultBlockState());
        ((HangarTerminalBlockEntity) level.getBlockEntity(ctx.abs(TERMINAL))).look();
    }

    /*
     * Its plan sent, an engine, a tank and a capsule, and every block of it laid at once, each the block wanted
     * there, so the rocket is whole before its hologram is ever drawn.
     */
    private static void lay(final ClientTestContext ctx, final ServerLevel level) {
        final HangarTerminalBlockEntity terminal = (HangarTerminalBlockEntity) level.getBlockEntity(ctx.abs(TERMINAL));
        terminal.design(new RocketDesign("BUILT", RocketStack.of(part("sea_level_engine_3", 1), part("tank_3", 2),
                part("capsule_3", 1))));
        terminal.confirm();
        final PlannedBuild plan = terminal.build().orElseThrow();
        for (final PlanSpot spot : plan.plan().spots()) {
            level.setBlockAndUpdate(plan.where(spot), BuiltInRegistries.BLOCK.get(spot.block()).defaultBlockState());
        }
    }

    private static void clear(final ClientTestContext ctx, final ServerLevel level) {
        if (level.getBlockEntity(ctx.abs(TERMINAL)) instanceof HangarTerminalBlockEntity terminal) {
            terminal.undo();
        }
        for (int x = X0 - 1; x <= X1 + 1; x++) {
            for (int z = Z0 - 1; z <= Z1 + 1; z++) {
                for (int y = Y0 - 1; y <= TOP + 1; y++) {
                    level.setBlockAndUpdate(ctx.abs(new BlockPos(x, y, z)), y == Y0 - 1 ? Blocks.GRASS_BLOCK
                            .defaultBlockState() : Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    private static RocketStack.Part part(final String path, final int layers) {
        return new RocketStack.Part(SkeletonRocketParts.id(path), layers);
    }

    /* The mean colour of a patch just under the crosshair, which whitens its own middle. */
    private static int[] mean(final ClientTestContext ctx) {
        final int x = ctx.mc().getWindow().getGuiScaledWidth() / 2;
        final int y = ctx.mc().getWindow().getGuiScaledHeight() / 2 + OFF_CROSSHAIR;
        final long[] sum = new long[3];
        int count = 0;
        try (ClientTestContext.FrameSampler frame = ctx.sampleFrame()) {
            for (int dx = -PATCH; dx <= PATCH; dx++) {
                for (int dy = -PATCH; dy <= PATCH; dy++) {
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

    /* Whether a colour is the far wall's green, under the lamps' warm light: green well over red and blue. */
    private static boolean green(final int[] colour) {
        return colour[1] > colour[0] + GREENER && colour[1] > colour[2] + APART;
    }

    private static int apart(final int[] a, final int[] b) {
        return Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]) + Math.abs(a[2] - b[2]);
    }

    private static String str(final int[] colour) {
        return "(" + colour[0] + ", " + colour[1] + ", " + colour[2] + ")";
    }
}
