/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.space;

import dev.jstech.space.rocket.CabinLamp;
import dev.jstech.space.rocket.CabinReadings;
import dev.jstech.space.ship.ShipDesign;
import dev.jstech.space.ship.SmallShipFinishes;
import dev.jstech.space.ship.SmallShipParts;
import dev.jstech.testkit.client.ClientTestHelper;
import dev.jstech.tests.TestShips;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/**
 * A starship's live cockpit on a player's game, parked and in flight, seen from the pilot's seat: the horizon shows sky
 * over its middle parked, and ground when the nose is pitched down; the master caution is dark parked and amber when
 * the master alarm is lit.
 */
public final class ShipCockpitClientTests {

    private static final BlockPos STAND = new BlockPos(0, 2, 0);
    private static final double HULL = 2;
    /* The pilot's eyes in the Orbiter's seat, and the upper middle of the horizon before them. */
    private static final Vec3 PILOT_EYES = new Vec3(0, HULL + 2.1, 4.2);
    private static final Vec3 HORIZON = new Vec3(0, HULL + 1.86, 2.93);
    /* Over the seat, looking down on the glare shield, where the master caution is. */
    private static final Vec3 OVER_EYES = new Vec3(0, HULL + 2.65, 3.7);
    private static final Vec3 CAUTION = new Vec3(0, HULL + 2.075, 2.92);
    /* The Kosmozavod pilot's eyes, and the middle of its panel round the globe. */
    private static final Vec3 KOSMOZAVOD_EYES = new Vec3(0, HULL + 1.75, 4.3);
    private static final Vec3 KOSMOZAVOD_PANEL = new Vec3(0, HULL + 1.1, 3.1);
    private static final CabinReadings FLYING = new CabinReadings(30, 1, 1, 0.5F, 0.9F, -40, 20, 90, 1, 1,
            Set.of(CabinLamp.MASTER_ALARM));
    private static final int PATCH = 1;
    private static final int APART = 30;
    private static final int ARRIVE = 600;
    private static final int SETTLE = 10;
    private static final int CHANNEL = 0xFF;

    private ShipCockpitClientTests() {
    }

    @ClientTest(timeoutTicks = 1500)
    public static void cockpit_showsTheFlightOnItsScreensAndLamps(final ClientTestContext ctx) {
        final int[][] seen = new int[4][];
        ctx.afterTest(() -> ctx.server().execute(() -> {
            ctx.serverLevel().removeBlock(ctx.abs(STAND), false);
            ctx.serverPlayer().setGameMode(GameType.CREATIVE);
        }));
        final ShipDesign ship = new ShipDesign("ORBITER", List.of(part("orbiter_cockpit"), part("airlock"),
                part("bay"), part("reactor_flush")), List.of(part("twin_bells"), part("side_intakes"),
                part("blended_delta"), part("twin_canted_fins"), part("blisters"), part("wheels")));
        ctx.thenServer(0, level -> {
            level.setBlockAndUpdate(ctx.abs(STAND), TestShips.DISPLAY.get().defaultBlockState());
            final TestShips.DisplayEntity stand = (TestShips.DisplayEntity) level.getBlockEntity(ctx.abs(STAND));
            stand.show(ship, SmallShipFinishes.DEFAULT);
            stand.read(CabinReadings.AT_REST);
            ctx.serverPlayer().setGameMode(GameType.SPECTATOR);
        }).thenWaitUntil(() -> ctx.mc().level.getBlockEntity(ctx.abs(STAND))
                instanceof TestShips.DisplayEntity stand && stand.design().equals(ship), ARRIVE,
                "the stand's ship to reach the player's game");
        stand(ctx, PILOT_EYES);
        ctx.then(2, () -> ctx.lookAt(corner(ctx).add(HORIZON)))
                .then(SETTLE, () -> seen[0] = mean(ctx))
                .thenScreenshot(0, "ship-cockpit-parked");
        stand(ctx, OVER_EYES);
        ctx.then(2, () -> ctx.lookAt(corner(ctx).add(CAUTION))).then(SETTLE, () -> seen[1] = mean(ctx))
                .thenServer(0, level -> ((TestShips.DisplayEntity) level.getBlockEntity(ctx.abs(STAND))).read(FLYING))
                .thenWaitUntil(() -> ctx.mc().level.getBlockEntity(ctx.abs(STAND))
                        instanceof TestShips.DisplayEntity stand && stand.readings().lit(CabinLamp.MASTER_ALARM),
                        ARRIVE, "the flight's readings to reach the player's game")
                .then(SETTLE, () -> seen[2] = mean(ctx));
        stand(ctx, PILOT_EYES);
        ctx.then(2, () -> ctx.lookAt(corner(ctx).add(HORIZON)))
                .then(SETTLE, () -> seen[3] = mean(ctx))
                .thenScreenshot(0, "ship-cockpit-flight");
        // Kosmozavod's cockpit in flight, for its lamps, guards, dials and globe.
        final ShipDesign courier = new ShipDesign("COURIER", List.of(part("greenhouse_cockpit"), part("docking"),
                part("side_door"), part("reactor_fins")), List.of(part("cluster"), part("side_intakes"),
                part("swept_wings"), part("single_fin"), part("quads"), part("wheels")));
        ctx.thenServer(0, level -> ((TestShips.DisplayEntity) level.getBlockEntity(ctx.abs(STAND)))
                        .show(courier, SmallShipFinishes.DEFAULT))
                .thenWaitUntil(() -> ctx.mc().level.getBlockEntity(ctx.abs(STAND))
                        instanceof TestShips.DisplayEntity stand && stand.design().equals(courier), ARRIVE,
                        "the Courier to reach the player's game");
        stand(ctx, KOSMOZAVOD_EYES);
        ctx.then(2, () -> ctx.lookAt(corner(ctx).add(KOSMOZAVOD_PANEL)))
                .thenScreenshot(SETTLE, "ship-cockpit-kosmozavod-flight")
                .then(0, () -> {
                    ctx.assertTrue(seen[0][2] - seen[0][0] > APART, "parked, the horizon shows sky over its middle: "
                            + str(seen[0]));
                    ctx.assertTrue(seen[3][0] - seen[3][2] > APART, "the nose pitched down, it shows ground there: "
                            + str(seen[3]));
                    ctx.assertTrue(seen[2][0] - seen[1][0] > APART && seen[2][0] - seen[2][2] > APART,
                            "the master caution lights amber with the master alarm: " + str(seen[1]) + " then "
                                    + str(seen[2]));
                });
    }

    /* Floats the player, a spectator, with their eyes at {@code eyes} from the stand's corner. */
    private static void stand(final ClientTestContext ctx, final Vec3 eyes) {
        ctx.thenServer(0, level -> {
            final Vec3 there = corner(ctx).add(eyes);
            ctx.serverPlayer().teleportTo(level, there.x, there.y - ctx.serverPlayer().getEyeHeight(), there.z, 0,
                    0);
        }).thenWaitUntil(() -> ctx.player().getEyePosition().distanceTo(corner(ctx).add(eyes)) < 0.1, ARRIVE,
                "the player to float inside the ship");
    }

    private static ResourceLocation part(final String path) {
        return SmallShipParts.id("small_" + path);
    }

    private static Vec3 corner(final ClientTestContext ctx) {
        return Vec3.atLowerCornerOf(ctx.abs(STAND));
    }

    /* The mean colour, red, green and blue, of a patch at the middle of the screen, round the crosshair. */
    private static int[] mean(final ClientTestContext ctx) {
        final int x = ctx.mc().getWindow().getGuiScaledWidth() / 2;
        final int y = ctx.mc().getWindow().getGuiScaledHeight() / 2;
        final long[] sum = new long[3];
        int count = 0;
        try (ClientTestHelper.FrameSampler frame = ctx.sampleFrame()) {
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

    private static String str(final int[] colour) {
        return "(" + colour[0] + ", " + colour[1] + ", " + colour[2] + ")";
    }
}
