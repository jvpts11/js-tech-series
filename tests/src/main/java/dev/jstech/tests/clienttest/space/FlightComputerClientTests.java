/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.space;

import dev.jstech.space.client.flight.FlightComputerScreen;
import dev.jstech.space.client.flight.LandingMapsClient;
import dev.jstech.space.client.rocket.FlightComputerFace;
import dev.jstech.space.flight.FlightComputerBlockEntity;
import dev.jstech.space.flight.FlightPage;
import dev.jstech.space.rocket.RocketKitBlocks;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * The Flight Computer on a player's game: used, it opens its screen on the transfer page, the Sun drawn in its yellow
 * in the middle of the system; its landing key, clicked, opens the landing page, and the map of Mars comes and is
 * drawn where the page was dark.
 */
public final class FlightComputerClientTests {

    private static final BlockPos COMPUTER = new BlockPos(0, 2, 2);
    private static final BlockPos EYE = new BlockPos(0, 2, 0);
    /* The Sun on the transfer page, the landing key's cap, and a spot of the landing map, in the face's pixels. */
    private static final int[] SUN = {FlightComputerFace.SX0 + 96, FlightComputerFace.SY0 + 86};
    private static final int[] LANDING_KEY = {184, 21};
    private static final int[] ON_MAP = {FlightComputerFace.SX0 + 70, FlightComputerFace.SY0 + 70};
    private static final int SPACE = 0x0A0F17;
    private static final int OPEN = 200;
    private static final int MAP_WAIT = 600;
    private static final int SETTLE = 10;
    private static final int CHANNEL = 0xFF;
    private static final int YELLOW = 180;
    private static final int APART = 40;

    private FlightComputerClientTests() {
    }

    @ClientTest(timeoutTicks = 1500)
    public static void computer_opensOnTheTransferAndDrawsTheLandingMap(final ClientTestContext ctx) {
        final int[][] seen = new int[2][];
        ctx.afterTest(() -> ctx.server().execute(() -> ctx.serverLevel().removeBlock(ctx.abs(COMPUTER), false)));
        ctx.thenTeleport(0, EYE, Direction.SOUTH)
                .thenServer(0, level -> level.setBlockAndUpdate(ctx.abs(COMPUTER),
                        RocketKitBlocks.FLIGHT_COMPUTER.get().defaultBlockState()))
                .thenRightClick(SETTLE, COMPUTER)
                .thenAwaitScreen(FlightComputerScreen.class, OPEN)
                .then(SETTLE, () -> seen[0] = sample(ctx, SUN))
                .thenScreenshot(0, "flight-computer-transfer")
                .then(0, () -> {
                    final int[] at = screenOf(ctx, LANDING_KEY);
                    ctx.click(at[0], at[1]);
                })
                .thenWaitUntil(() -> ctx.screen(FlightComputerScreen.class) != null
                        && ctx.screen(FlightComputerScreen.class).getMenu().computer().page() == FlightPage.LANDING
                        && LandingMapsClient.shown() != null, MAP_WAIT, "the landing page and its map to come")
                .then(SETTLE, () -> seen[1] = sample(ctx, ON_MAP))
                .thenScreenshot(0, "flight-computer-landing")
                .then(0, () -> {
                    ctx.assertTrue(seen[0][0] > YELLOW && seen[0][1] > YELLOW && seen[0][2] < seen[0][1],
                            "the Sun is drawn yellow in the middle of the system: " + str(seen[0]));
                    final int dr = Math.abs(seen[1][0] - (SPACE >> 16 & CHANNEL));
                    final int dg = Math.abs(seen[1][1] - (SPACE >> 8 & CHANNEL));
                    final int db = Math.abs(seen[1][2] - (SPACE & CHANNEL));
                    ctx.assertTrue(dr + dg + db > APART, "the map of Mars is drawn where the page was dark: "
                            + str(seen[1]));
                    ctx.assertTrue(LandingMapsClient.shown().blocks() == FlightComputerBlockEntity.WIDE_BLOCKS,
                            "the wider map first");
                    ctx.mc().setScreen(null);
                });
    }

    /* Where the face's pixel {@code at} is on the window, in the screen's own scaled units. */
    private static int[] screenOf(final ClientTestContext ctx, final int[] at) {
        final FlightComputerScreen screen = ctx.screen(FlightComputerScreen.class);
        final float scale = screen.getXSize() / (float) FlightComputerFace.WIDTH;
        return new int[] {Math.round(screen.getGuiLeft() + (at[0] + 0.5F) * scale),
            Math.round(screen.getGuiTop() + (at[1] + 0.5F) * scale)};
    }

    private static int[] sample(final ClientTestContext ctx, final int[] at) {
        final int[] point = screenOf(ctx, at);
        try (ClientTestContext.FrameSampler frame = ctx.sampleFrame()) {
            final int argb = frame.pixel(point[0], point[1]);
            return new int[] {argb >> 16 & CHANNEL, argb >> 8 & CHANNEL, argb & CHANNEL};
        }
    }

    private static String str(final int[] colour) {
        return "(" + colour[0] + ", " + colour[1] + ", " + colour[2] + ")";
    }
}
