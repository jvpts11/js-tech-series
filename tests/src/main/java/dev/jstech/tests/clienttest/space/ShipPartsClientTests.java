/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.space;

import dev.jstech.space.ship.ShipDesign;
import dev.jstech.space.ship.SmallShipFinishes;
import dev.jstech.space.ship.SmallShipParts;
import dev.jstech.testkit.client.ClientTestHelper;
import dev.jstech.tests.TestShips;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/**
 * The starship parts' formed models on a player's game: the Orbiter on a stand, drawn part by part where the bare
 * stand shows only what is behind it, its hold's side painted in each finish chosen apart from its parts: the
 * orbiter's white blankets, bare metal's grey plates, enamel's crimson above its gold line.
 */
public final class ShipPartsClientTests {

    /* The player, floating as a spectator, east of the ship's hold at its height, looking west at its side. */
    private static final BlockPos EYE = new BlockPos(9, 4, 10);
    private static final BlockPos STAND = new BlockPos(0, 2, 0);
    /* Where the ship is seen whole, as the approved catalogue saw it: in front of it and to its left, above it. */
    private static final BlockPos OVERVIEW = new BlockPos(-13, 9, -8);
    private static final Vec3 MIDDLE = new Vec3(0, 3, 8);
    /*
     * The hold's side looked at, from the stand's north-west corner: on the hull's right side, above the band, along
     * the bay doors' hold clear of the wings below and the thrusters behind.
     */
    private static final Vec3 SIDE = new Vec3(1.9, 2 + 1.7, 10.5);
    private static final int PATCH = 2;
    private static final int DRAWN = 40;
    /* How much lighter the white blankets are than bare metal on the hull's side, which takes the light aslant. */
    private static final int LIGHTER = 15;
    private static final int RED = 40;
    private static final int GREY = 25;
    private static final int ARRIVE = 600;
    private static final int SETTLE = 10;
    private static final int CHANNEL = 0xFF;

    private ShipPartsClientTests() {
    }

    @ClientTest(timeoutTicks = 1500)
    public static void ship_isDrawnPartByPartInEachFinish(final ClientTestContext ctx) {
        final List<String> finishes = List.of("orbiter", "bare_metal", "enamel");
        final List<int[]> seen = new ArrayList<>();
        ctx.afterTest(() -> ctx.server().execute(() -> {
            ctx.serverLevel().removeBlock(ctx.abs(STAND), false);
            ctx.serverPlayer().setGameMode(GameType.CREATIVE);
        }));
        ctx.thenServer(0, level -> {
            level.setBlockAndUpdate(ctx.abs(STAND), TestShips.DISPLAY.get().defaultBlockState());
            ctx.serverPlayer().setGameMode(GameType.SPECTATOR);
        });
        for (final String finish : finishes) {
            final ResourceLocation paint = SmallShipFinishes.id(finish);
            ctx.thenServer(0, level -> ((TestShips.DisplayEntity) level.getBlockEntity(ctx.abs(STAND)))
                            .show(orbiter(), paint))
                    .thenWaitUntil(() -> ctx.mc().level.getBlockEntity(ctx.abs(STAND))
                            instanceof TestShips.DisplayEntity stand && !stand.design().spine().isEmpty()
                            && stand.finish().equals(paint), ARRIVE, "the stand's ship in " + finish
                            + " to reach the player's game")
                    .thenTeleport(0, OVERVIEW, Direction.SOUTH)
                    .then(2, () -> ctx.lookAt(corner(ctx).add(MIDDLE)))
                    .thenScreenshot(SETTLE, "ship-parts-" + finish)
                    .thenTeleport(0, EYE, Direction.WEST)
                    .then(2, () -> ctx.lookAt(corner(ctx).add(SIDE)))
                    .then(SETTLE, () -> seen.add(mean(ctx)));
        }
        ctx.thenServer(0, level -> ((TestShips.DisplayEntity) level.getBlockEntity(ctx.abs(STAND)))
                        .show(new ShipDesign("", List.of(), List.of()), SmallShipFinishes.DEFAULT))
                .thenWaitUntil(() -> ctx.mc().level.getBlockEntity(ctx.abs(STAND))
                        instanceof TestShips.DisplayEntity stand && stand.design().spine().isEmpty(), ARRIVE,
                        "the bare stand to reach the player's game")
                .then(2, () -> ctx.lookAt(corner(ctx).add(SIDE)))
                .then(SETTLE, () -> {
                    final int[] bare = mean(ctx);
                    final int[] orbiter = seen.get(0);
                    final int[] metal = seen.get(1);
                    final int[] enamel = seen.get(2);
                    ctx.assertTrue(distance(orbiter, bare) > DRAWN, "the ship is drawn: " + str(orbiter)
                            + " where the bare stand shows " + str(bare));
                    ctx.assertTrue(orbiter[0] - metal[0] > LIGHTER && orbiter[1] - metal[1] > LIGHTER
                            && orbiter[2] - metal[2] > LIGHTER, "the orbiter's white blankets are lighter than "
                            + "bare metal: " + str(orbiter) + " against " + str(metal));
                    ctx.assertTrue(Math.abs(metal[0] - metal[1]) < GREY && Math.abs(metal[1] - metal[2]) < GREY,
                            "bare metal is grey: " + str(metal));
                    ctx.assertTrue(enamel[0] - enamel[1] > RED && enamel[0] - enamel[2] > RED,
                            "enamel is crimson above its gold line: " + str(enamel));
                });
    }

    /* The Orbiter of the approved catalogue: its cockpit, airlock, bay doors' hold, reactor, and what hangs on them. */
    private static ShipDesign orbiter() {
        return new ShipDesign("ORBITER", List.of(part("orbiter_cockpit"), part("airlock"), part("bay"),
                part("reactor_flush")), List.of(part("twin_bells"), part("side_intakes"), part("blended_delta"),
                part("twin_canted_fins"), part("blisters"), part("wheels")));
    }

    private static ResourceLocation part(final String path) {
        return SmallShipParts.id("small_" + path);
    }

    /* The stand's north-west corner, where the ship's nose meets its centre line at the ground. */
    private static Vec3 corner(final ClientTestContext ctx) {
        return Vec3.atLowerCornerOf(ctx.abs(STAND));
    }

    /* The mean colour, red, green and blue, of a patch round the middle of the screen, the crosshair's left out. */
    private static int[] mean(final ClientTestContext ctx) {
        final int x = ctx.mc().getWindow().getGuiScaledWidth() / 2;
        final int y = ctx.mc().getWindow().getGuiScaledHeight() / 2;
        final long[] sum = new long[3];
        int count = 0;
        try (ClientTestHelper.FrameSampler frame = ctx.sampleFrame()) {
            for (int dx = -PATCH * 2; dx <= PATCH * 2; dx++) {
                for (int dy = -PATCH * 2; dy <= PATCH * 2; dy++) {
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

    private static int distance(final int[] a, final int[] b) {
        return Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]) + Math.abs(a[2] - b[2]);
    }

    private static String str(final int[] colour) {
        return "(" + colour[0] + ", " + colour[1] + ", " + colour[2] + ")";
    }
}
