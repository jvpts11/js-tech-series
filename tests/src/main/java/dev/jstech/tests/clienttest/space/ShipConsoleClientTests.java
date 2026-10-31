/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.space;

import com.mojang.blaze3d.platform.InputConstants;
import dev.jstech.space.client.rocket.FlightComputerFace;
import dev.jstech.space.client.ship.ShipConsoleFace;
import dev.jstech.space.client.ship.ShipConsoleScreen;
import dev.jstech.space.crew.CrewRole;
import dev.jstech.space.crew.ShipConsole;
import dev.jstech.space.crew.ShipCrew;
import dev.jstech.space.ship.AirReserveEntity;
import dev.jstech.space.ship.ShipKitBlocks;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.UUID;
import java.util.function.IntPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;

/**
 * A small ship's console's screen, as the approved crew page draws it: the flight computer's display with the crew a
 * row a key, each role in its colour, the pilots magenta, the crew green, the passengers cyan; its owner chooses a
 * row by its left key, the row boxed on its dark green ground, and gives it a role by a right key.
 */
public final class ShipConsoleClientTests {

    private static final BlockPos CONSOLE = new BlockPos(0, 2, 4);
    private static final BlockPos STAND = new BlockPos(0, 2, 2);
    private static final UUID STEVE = UUID.fromString("00000000-0000-4000-8000-00000000c501");
    private static final UUID ALEX = UUID.fromString("00000000-0000-4000-8000-00000000c502");
    private static final String TEAM = "jstests_ship_console";
    /* The rows: the owner's, the team's, everyone else's, then those named; Steve's is the fourth. */
    private static final int STEVE_ROW = 3;
    /* The right key that gives the crew's role. */
    private static final int CREW_KEY = 1;
    /* How many pixels of each role's colour the page shows, at the least. */
    private static final int SEEN = 20;
    private static final int RGB = 0xFFFFFF;
    private static final int OPEN = 200;
    private static final int ARRIVE = 200;
    private static final int SETTLE = 10;
    private static final int CHANNEL = 0xFF;
    private static final int MARGIN = 10;

    private ShipConsoleClientTests() {
    }

    @ClientTest(timeoutTicks = 1200)
    public static void console_showsTheCrewPageAsApprovedAndItsOwnerSetsARole(final ClientTestContext ctx) {
        final int[] seen = new int[4];
        ctx.afterTest(() -> {
            ctx.mc().setScreen(null);
            ctx.server().execute(() -> {
                ctx.serverLevel().removeBlock(ctx.abs(CONSOLE), false);
                final PlayerTeam team = ctx.server().getScoreboard().getPlayerTeam(TEAM);
                if (team != null) {
                    ctx.server().getScoreboard().removePlayerTeam(team);
                }
            });
        });
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> {
                    // The owner on a team, as the approved page shows one.
                    final PlayerTeam kept = level.getScoreboard().getPlayerTeam(TEAM);
                    final PlayerTeam team = kept != null ? kept : level.getScoreboard().addPlayerTeam(TEAM);
                    team.setDisplayName(Component.literal("Orbital Works"));
                    level.getScoreboard().addPlayerToTeam(ctx.serverPlayer().getScoreboardName(), team);
                    level.setBlockAndUpdate(ctx.abs(CONSOLE), ShipKitBlocks.FLIGHT_CONSOLE.get().defaultBlockState());
                    final AirReserveEntity console = (AirReserveEntity) level.getBlockEntity(ctx.abs(CONSOLE));
                    final ServerPlayer player = ctx.serverPlayer();
                    console.crew(ShipCrew.of(player.getUUID(), player.getScoreboardName())
                            .withNamed(STEVE, "STEVE", CrewRole.PASSENGER).withNamed(ALEX, "ALEX", CrewRole.PILOT));
                })
                .thenWaitUntil(() -> ctx.mc().level.getBlockEntity(ctx.abs(CONSOLE)) instanceof AirReserveEntity console
                        && console.crew().isPresent(), ARRIVE, "the console and its crew to reach the player")
                .thenServer(0, level -> ShipConsole.open(ctx.serverPlayer(),
                        (AirReserveEntity) level.getBlockEntity(ctx.abs(CONSOLE)), CrewRole.PILOT))
                .thenAwaitScreen(ShipConsoleScreen.class, OPEN)
                .then(SETTLE, () -> {
                    seen[0] = count(ctx, ShipConsoleClientTests::magenta);
                    seen[1] = count(ctx, ShipConsoleClientTests::green);
                    seen[2] = count(ctx, ShipConsoleClientTests::cyan);
                })
                .thenScreenshot(0, "ship-console-owner")
                .then(0, () -> click(ctx, FlightComputerFace.SX0 - 19, FlightComputerFace.sideKeyY(STEVE_ROW) + 5))
                .then(SETTLE, () -> seen[3] = count(ctx, argb -> (argb & RGB)
                        == (ShipConsoleFace.PALETTE.get().chosenGround() & RGB)))
                .thenScreenshot(0, "ship-console-chosen")
                .then(0, () -> click(ctx, FlightComputerFace.SX1 + 19, FlightComputerFace.sideKeyY(CREW_KEY) + 5))
                .thenWaitUntil(() -> ctx.mc().level.getBlockEntity(ctx.abs(CONSOLE)) instanceof AirReserveEntity console
                        && console.crew().map(crew -> crew.named().stream().anyMatch(member -> member.id().equals(STEVE)
                        && member.role() == CrewRole.CREW)).orElse(false), ARRIVE, "Steve made crew")
                .thenScreenshot(SETTLE, "ship-console-set")
                .then(0, () -> {
                    ctx.assertTrue(seen[0] >= SEEN && seen[1] >= SEEN && seen[2] >= SEEN, "each role in its colour,"
                            + " the pilots magenta, the crew green, the passengers cyan: " + seen[0] + ", " + seen[1]
                            + ", " + seen[2]);
                    ctx.assertTrue(seen[3] >= SEEN, "the row chosen boxed on its dark green ground: " + seen[3]);
                });
    }

    /* Clicks the face's pixel {@code (x, y)} on the screen open, scaled as the screen scales the face. */
    private static void click(final ClientTestContext ctx, final int x, final int y) {
        final ShipConsoleScreen screen = (ShipConsoleScreen) ctx.mc().screen;
        final float scale = Math.max(0.5F, Math.min((screen.width - 2F * MARGIN) / FlightComputerFace.WIDTH,
                (screen.height - 2F * MARGIN) / FlightComputerFace.HEIGHT));
        final int width = Math.round(FlightComputerFace.WIDTH * scale);
        final int height = Math.round(FlightComputerFace.HEIGHT * scale);
        final double sx = (screen.width - width) / 2 + x * scale;
        final double sy = (screen.height - height) / 2 + y * scale;
        screen.mouseClicked(sx, sy, InputConstants.MOUSE_BUTTON_LEFT);
        screen.mouseReleased(sx, sy, InputConstants.MOUSE_BUTTON_LEFT);
    }

    /* How many pixels of the whole frame are {@code colour}. */
    private static int count(final ClientTestContext ctx, final IntPredicate colour) {
        final int width = ctx.mc().getWindow().getGuiScaledWidth();
        final int height = ctx.mc().getWindow().getGuiScaledHeight();
        int found = 0;
        try (ClientTestContext.FrameSampler frame = ctx.sampleFrame()) {
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    if (colour.test(frame.pixel(x, y))) {
                        found++;
                    }
                }
            }
        }
        return found;
    }

    /* The pilots' magenta. */
    private static boolean magenta(final int argb) {
        final int r = argb >> 16 & CHANNEL;
        final int g = argb >> 8 & CHANNEL;
        final int b = argb & CHANNEL;
        return r > 200 && g < 110 && b > 180;
    }

    /* The crew's green. */
    private static boolean green(final int argb) {
        final int r = argb >> 16 & CHANNEL;
        final int g = argb >> 8 & CHANNEL;
        final int b = argb & CHANNEL;
        return g > 200 && r < 110 && b < 150;
    }

    /* The passengers' cyan. */
    private static boolean cyan(final int argb) {
        final int r = argb >> 16 & CHANNEL;
        final int g = argb >> 8 & CHANNEL;
        final int b = argb & CHANNEL;
        return b > 200 && g > 180 && r < 110;
    }
}
