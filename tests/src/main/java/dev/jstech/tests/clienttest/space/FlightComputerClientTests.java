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
import dev.jstech.space.flight.StageChoice;
import dev.jstech.space.hangar.RocketDesign;
import dev.jstech.space.rocket.RocketKitBlocks;
import dev.jstech.space.rocket.RocketStack;
import dev.jstech.space.rocket.SkeletonRocketParts;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * The Flight Computer on a player's game: used, it opens its screen on the transfer page, the Sun drawn in its yellow
 * in the middle of the system; its landing key, clicked, opens the landing page, and the map of Mars comes and is
 * drawn where the page was dark. On its stages page, the rocket's stack is drawn with its separation ring in amber,
 * and its ZONE key sets the chosen stage to fall in a zone round the pad.
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
    /*
     * A rocket of two stages, its first with legs: twelve blocks tall, drawn four pixels a block from the floor of the
     * stack's schematic; its first stage's ring, five blocks up, and the ZONE key's cap, in the face's pixels.
     */
    private static final RocketStack STAGED = RocketStack.of(part("sea_level_engine_3"), part("landing_legs_3"),
            part("tank_3"), part("separation_ring_3"), part("vacuum_engine_3"), part("tank_3"), part("capsule_3"));
    private static final int[] RING = {FlightComputerFace.SX0 + 42, FlightComputerFace.SY1 - 34 - 18};
    private static final int[] ZONE_KEY = {FlightComputerFace.SX1 + 19, FlightComputerFace.SY0 + 24 + 30 + 5};
    private static final int AMBER_RED = 200;
    private static final int AMBER_GREEN = 140;
    /* Where the transfer page writes its status, bottom right, in the face's pixels, and the least amber seen there. */
    private static final int[] STATUS = {FlightComputerFace.SX1 - 90, FlightComputerFace.SY1 - 11,
        FlightComputerFace.SX1 - 8, FlightComputerFace.SY1 - 2};
    private static final int LEAST_AMBER = 6;

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

    @ClientTest(timeoutTicks = 1500)
    public static void computer_stagesPageDrawsTheStackAndSetsAFate(final ClientTestContext ctx) {
        final int[][] seen = new int[1][];
        ctx.afterTest(() -> ctx.server().execute(() -> ctx.serverLevel().removeBlock(ctx.abs(COMPUTER), false)));
        ctx.thenTeleport(0, EYE, Direction.SOUTH)
                .thenServer(0, level -> {
                    level.setBlockAndUpdate(ctx.abs(COMPUTER),
                            RocketKitBlocks.FLIGHT_COMPUTER.get().defaultBlockState());
                    if (level.getBlockEntity(ctx.abs(COMPUTER)) instanceof FlightComputerBlockEntity computer) {
                        computer.carry(new RocketDesign("STAGES", STAGED), BlockPos.ZERO, List.of());
                        computer.press(FlightComputerBlockEntity.TOP + FlightPage.KEYS.indexOf(FlightPage.STAGES));
                    }
                })
                .thenRightClick(SETTLE, COMPUTER)
                .thenAwaitScreen(FlightComputerScreen.class, OPEN)
                .then(SETTLE, () -> seen[0] = sample(ctx, RING))
                .then(0, () -> {
                    final int[] at = screenOf(ctx, ZONE_KEY);
                    ctx.click(at[0], at[1]);
                })
                .thenWaitUntil(() -> ctx.screen(FlightComputerScreen.class) != null && ctx.screen(
                        FlightComputerScreen.class).getMenu().computer().choices().containsKey(1), OPEN,
                        "the first stage set to fall in a zone")
                .then(SETTLE, () -> { })
                .thenScreenshot(0, "flight-computer-stages")
                .then(0, () -> {
                    ctx.assertTrue(seen[0][0] > AMBER_RED && seen[0][1] > AMBER_GREEN && seen[0][2] < seen[0][1],
                            "the first stage's separation ring is drawn amber in the stack: " + str(seen[0]));
                    final StageChoice set = ctx.screen(FlightComputerScreen.class).getMenu().computer().choices()
                            .get(1);
                    ctx.assertTrue(!set.lands() && set.radius() == StageChoice.FIRST_RADIUS,
                            "ZONE sets a fall zone round the pad: " + set);
                    ctx.mc().setScreen(null);
                });
    }

    @ClientTest(timeoutTicks = 1500)
    public static void computer_xferTellsAPlanShortOfDeltaV(final ClientTestContext ctx) {
        final int[] amber = new int[1];
        ctx.afterTest(() -> ctx.server().execute(() -> ctx.serverLevel().removeBlock(ctx.abs(COMPUTER), false)));
        ctx.thenTeleport(0, EYE, Direction.SOUTH)
                .thenServer(0, level -> {
                    level.setBlockAndUpdate(ctx.abs(COMPUTER),
                            RocketKitBlocks.FLIGHT_COMPUTER.get().defaultBlockState());
                    if (level.getBlockEntity(ctx.abs(COMPUTER)) instanceof FlightComputerBlockEntity computer) {
                        // A rocket with empty tanks: it has no delta-v for the transfer.
                        computer.carry(new RocketDesign("DRY", STAGED), BlockPos.ZERO, List.of());
                    }
                })
                .thenRightClick(SETTLE, COMPUTER)
                .thenAwaitScreen(FlightComputerScreen.class, OPEN)
                .then(SETTLE, () -> amber[0] = amberIn(ctx, STATUS))
                .thenScreenshot(0, "flight-computer-xfer-short")
                .then(0, () -> {
                    ctx.assertTrue(amber[0] >= LEAST_AMBER, "the plan is told short of delta-v, in amber, where its"
                            + " status is written: " + amber[0] + " amber points");
                    ctx.mc().setScreen(null);
                });
    }

    /* How many points of the face's box {@code box}, x0, y0, x1, y1 in its own pixels, are amber. */
    private static int amberIn(final ClientTestContext ctx, final int[] box) {
        int count = 0;
        try (ClientTestContext.FrameSampler frame = ctx.sampleFrame()) {
            for (int x = box[0]; x < box[2]; x++) {
                for (int y = box[1]; y < box[3]; y++) {
                    final int[] point = screenOf(ctx, new int[] {x, y});
                    final int argb = frame.pixel(point[0], point[1]);
                    if ((argb >> 16 & CHANNEL) > AMBER_RED && (argb >> 8 & CHANNEL) > AMBER_GREEN
                            && (argb & CHANNEL) < (argb >> 8 & CHANNEL) / 2) {
                        count++;
                    }
                }
            }
        }
        return count;
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

    private static RocketStack.Part part(final String path) {
        return new RocketStack.Part(SkeletonRocketParts.id(path), 1);
    }
}
