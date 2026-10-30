/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.space;

import dev.jstech.core.multiblock.FormedParts;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.space.TestShipyards;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * A Small Shipyard on a player's game: built and formed, it is drawn as the approved dry dock, seen from where its
 * drawing was made, from the open front and from the back; its crane's bridge drawn in its hazard yellow.
 */
public final class ShipyardClientTests {

    /* The pattern's lowest corner, so the dock's ground is the land's and its bay's middle on the test's x of 0. */
    private static final BlockPos CORNER = new BlockPos(-11, -1, 6);
    /*
     * The dock's own places, as the approved drawing's cameras saw them: where they looked, and the eyes, brought
     * nearer along the same line so the game's wider view frames the dock as the drawing's narrower one did.
     */
    private static final Vec3 FRONT_EYES = new Vec3(-15.3, 17.5, -20.5);
    private static final Vec3 FRONT_AT = new Vec3(0, 2.5, 0.5);
    private static final Vec3 BACK_EYES = new Vec3(16.1, 10.2, 22.1);
    private static final Vec3 BACK_AT = new Vec3(6, 4, 6);
    /* The top of the crane's bridge, over the bay's middle; and a tower's block, which merges as the dock forms. */
    private static final Vec3 BRIDGE = new Vec3(0, 12.0, 1.5);
    private static final BlockPos TOWER_BLOCK = dock(-11, 1, -13);
    /* The box the dock and its ground fill, cleared after, and the land's own layers put back under it. */
    private static final int X0 = -13;
    private static final int X1 = 13;
    private static final int Z0 = 4;
    private static final int Z1 = 34;
    private static final int Y_BOTTOM = -2;
    private static final int Y_GRASS = 1;
    private static final int Y_TOP = 16;
    private static final int PATCH = 1;
    private static final int CHANNEL = 0xFF;
    /* How far red and green stand over blue on the bridge's yellow, in the open sun. */
    private static final int YELLOW = 40;
    private static final int ARRIVE = 600;
    private static final int SETTLE = 20;

    private ShipyardClientTests() {
    }

    @ClientTest(timeoutTicks = 1500)
    public static void shipyard_isDrawnFormedAsTheApprovedDock(final ClientTestContext ctx) {
        final int[][] seen = new int[1][];
        final ServerLevel[] built = new ServerLevel[1];
        // Put back on the server's own thread, from the level built in, as the last test's game may be closing.
        ctx.afterTest(() -> ctx.server().execute(() -> {
            if (built[0] != null) {
                clear(ctx, built[0]);
                built[0].players().forEach(player -> player.setGameMode(GameType.CREATIVE));
            }
        }));
        ctx.thenServer(0, level -> {
            built[0] = level;
            TestShipyards.build(level, ctx.abs(CORNER));
            ctx.serverPlayer().setGameMode(GameType.SPECTATOR);
        }).thenWaitUntil(() -> ctx.mc().level.getBlockState(ctx.abs(TOWER_BLOCK)).is(FormedParts.BLOCK.get()),
                ARRIVE, "the formed dock to reach the player's game");
        stand(ctx, FRONT_EYES);
        ctx.then(2, () -> ctx.lookAt(place(ctx, BRIDGE)))
                .then(SETTLE, () -> seen[0] = mean(ctx))
                .then(2, () -> ctx.lookAt(place(ctx, FRONT_AT)))
                .thenScreenshot(SETTLE, "shipyard-front");
        stand(ctx, BACK_EYES);
        ctx.then(2, () -> ctx.lookAt(place(ctx, BACK_AT)))
                .thenScreenshot(SETTLE, "shipyard-back")
                .then(0, () -> ctx.assertTrue(seen[0][0] - seen[0][2] > YELLOW && seen[0][1] - seen[0][2] > YELLOW,
                        "the crane's bridge is drawn in its hazard yellow: " + seen[0][0] + ", " + seen[0][1] + ", "
                                + seen[0][2]));
    }

    /* Floats the player, a spectator, with their eyes at the dock's place {@code eyes}. */
    private static void stand(final ClientTestContext ctx, final Vec3 eyes) {
        ctx.thenServer(0, level -> {
            final Vec3 there = place(ctx, eyes);
            ctx.serverPlayer().teleportTo(level, there.x, there.y - ctx.serverPlayer().getEyeHeight(), there.z, 0,
                    0);
        }).thenWaitUntil(() -> ctx.player().getEyePosition().distanceTo(place(ctx, eyes)) < 0.1, ARRIVE,
                "the player to float where the drawing's camera stood");
    }

    /* The world's point of the dock's place {@code at}: across from the bay's middle, up from the ground, along. */
    private static Vec3 place(final ClientTestContext ctx, final Vec3 at) {
        return Vec3.atLowerCornerOf(ctx.abs(dock(0, 0, 0))).add(at);
    }

    private static BlockPos dock(final int x, final int y, final int z) {
        return CORNER.offset(x + 11, y + 3, z + 13);
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

    /* The mean colour, red, green and blue, of a patch round the crosshair, its own middle left out. */
    private static int[] mean(final ClientTestContext ctx) {
        final int x = ctx.mc().getWindow().getGuiScaledWidth() / 2;
        final int y = ctx.mc().getWindow().getGuiScaledHeight() / 2;
        final long[] sum = new long[3];
        int count = 0;
        try (ClientTestContext.FrameSampler frame = ctx.sampleFrame()) {
            for (int dx = -PATCH * 3; dx <= PATCH * 3; dx++) {
                for (int dy = -PATCH * 3; dy <= PATCH * 3; dy++) {
                    if (Math.abs(dx) <= PATCH && Math.abs(dy) <= PATCH) {
                        continue;
                    }
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
