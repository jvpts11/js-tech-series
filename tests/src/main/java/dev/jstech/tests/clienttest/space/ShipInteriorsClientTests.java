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
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * The starship parts' interiors on a player's game, each in its maker's style: the Orbiter's airlock in Meridian
 * Orbital's padded white panels, the world seen through its door's window; the Hauler's hold in Ironside Yards' bare
 * metal; the Courier's docking module in Kosmozavod's turquoise.
 */
public final class ShipInteriorsClientTests {

    private static final BlockPos STAND = new BlockPos(0, 2, 0);
    /* The hull's floor over the stand's, in blocks. */
    private static final double HULL = 2;
    /* Where the player's eyes are and what they look at, inside each ship, from the stand's north-west corner. */
    private static final Vec3 AIRLOCK_EYES = new Vec3(0, HULL + 1.65, 6.5);
    private static final Vec3 AIRLOCK_WALL = new Vec3(1.84, HULL + 1.75, 6.5);
    private static final Vec3 DOOR_WINDOW = new Vec3(-1.9, HULL + 1.65, 7.15);
    /* Where the Orbiter's pilot sits, on the raised flight deck, and what is ahead through the windscreen. */
    private static final Vec3 PILOT_EYES = new Vec3(0, HULL + 2.1, 4.2);
    private static final Vec3 AHEAD = new Vec3(0, HULL + 2.0, -2);
    private static final Vec3 HOLD_EYES = new Vec3(0, HULL + 1.65, 12.5);
    private static final Vec3 HOLD_WALL = new Vec3(1.84, HULL + 1.05, 12.5);
    private static final Vec3 DOCKING_EYES = new Vec3(0, HULL + 1.65, 6.5);
    private static final Vec3 DOCKING_WALL = new Vec3(1.84, HULL + 2.3, 6.5);
    /* The red wall outside the airlock's door, west of the ship. */
    private static final int WALL_X = -6;
    private static final int PATCH = 1;
    private static final int APART = 30;
    private static final int TINT = 30;
    /* How far green and blue outshine red on Kosmozavod's panels, which the cabin's sloping wall takes dimly. */
    private static final int TURQUOISE = 20;
    private static final int ARRIVE = 600;
    private static final int SETTLE = 10;
    private static final int CHANNEL = 0xFF;

    private ShipInteriorsClientTests() {
    }

    @ClientTest(timeoutTicks = 1500)
    public static void interiors_areEachInTheirMakersStyle(final ClientTestContext ctx) {
        final int[][] seen = new int[4][];
        ctx.afterTest(() -> ctx.server().execute(() -> {
            ctx.serverLevel().removeBlock(ctx.abs(STAND), false);
            for (final BlockPos pos : wall(ctx)) {
                ctx.serverLevel().removeBlock(pos, false);
            }
            ctx.serverPlayer().setGameMode(GameType.CREATIVE);
        }));
        ctx.thenServer(0, level -> {
            level.setBlockAndUpdate(ctx.abs(STAND), TestShips.DISPLAY.get().defaultBlockState());
            for (final BlockPos pos : wall(ctx)) {
                level.setBlockAndUpdate(pos, Blocks.RED_WOOL.defaultBlockState());
            }
            ctx.serverPlayer().setGameMode(GameType.SPECTATOR);
        });
        show(ctx, ship("ORBITER", List.of("orbiter_cockpit", "airlock", "bay", "reactor_flush"),
                List.of("twin_bells", "side_intakes", "blended_delta", "twin_canted_fins", "blisters", "wheels")));
        stand(ctx, AIRLOCK_EYES);
        ctx.then(2, () -> ctx.lookAt(corner(ctx).add(AIRLOCK_WALL)))
                .then(SETTLE, () -> seen[0] = mean(ctx))
                .thenScreenshot(0, "ship-interior-meridian")
                .then(2, () -> ctx.lookAt(corner(ctx).add(DOOR_WINDOW)))
                .then(SETTLE, () -> seen[1] = mean(ctx))
                .thenScreenshot(0, "ship-interior-door-window");
        stand(ctx, PILOT_EYES);
        ctx.then(2, () -> ctx.lookAt(corner(ctx).add(AHEAD)))
                .thenScreenshot(SETTLE, "ship-interior-cockpit");
        show(ctx, ship("HAULER", List.of("cab_cockpit", "airlock", "reactor_fins", "ramp"),
                List.of("magnetic_nozzle", "nacelle_intake", "stub_wings", "quads", "legs")));
        stand(ctx, HOLD_EYES);
        ctx.then(2, () -> ctx.lookAt(corner(ctx).add(HOLD_WALL)))
                .then(SETTLE, () -> seen[2] = mean(ctx))
                .thenScreenshot(0, "ship-interior-ironside");
        show(ctx, ship("COURIER", List.of("greenhouse_cockpit", "docking", "side_door", "reactor_fins"),
                List.of("cluster", "side_intakes", "swept_wings", "single_fin", "quads", "wheels")));
        stand(ctx, DOCKING_EYES);
        ctx.then(2, () -> ctx.lookAt(corner(ctx).add(DOCKING_WALL)))
                .then(SETTLE, () -> seen[3] = mean(ctx))
                .thenScreenshot(0, "ship-interior-kosmozavod")
                .then(0, () -> {
                    final int[] meridian = seen[0];
                    final int[] window = seen[1];
                    final int[] ironside = seen[2];
                    final int[] kosmozavod = seen[3];
                    ctx.assertTrue(meridian[0] - ironside[0] > APART && meridian[1] - ironside[1] > APART
                            && meridian[2] - ironside[2] > APART, "Meridian's white panels are lighter than "
                            + "Ironside's bare metal: " + str(meridian) + " against " + str(ironside));
                    ctx.assertTrue(kosmozavod[1] - kosmozavod[0] > TURQUOISE
                                    && kosmozavod[2] - kosmozavod[0] > TURQUOISE,
                            "Kosmozavod's panels are turquoise: " + str(kosmozavod));
                    ctx.assertTrue(window[0] - window[1] > TINT && window[0] - window[2] > TINT,
                            "the red wall outside is seen through the door's window: " + str(window));
                });
    }

    /* Has the stand show {@code design}, in the orbiter's finish, and waits until the player's game has it. */
    private static void show(final ClientTestContext ctx, final ShipDesign design) {
        ctx.thenServer(0, level -> ((TestShips.DisplayEntity) level.getBlockEntity(ctx.abs(STAND)))
                        .show(design, SmallShipFinishes.DEFAULT))
                .thenWaitUntil(() -> ctx.mc().level.getBlockEntity(ctx.abs(STAND))
                        instanceof TestShips.DisplayEntity stand && stand.design().equals(design), ARRIVE,
                        "the stand's " + design.name() + " to reach the player's game");
    }

    /* Floats the player, a spectator, with their eyes at {@code eyes} from the stand's corner. */
    private static void stand(final ClientTestContext ctx, final Vec3 eyes) {
        ctx.thenServer(0, level -> {
            final Vec3 at = corner(ctx).add(eyes);
            ctx.serverPlayer().teleportTo(level, at.x, at.y - ctx.serverPlayer().getEyeHeight(), at.z, 0, 0);
        }).thenWaitUntil(() -> ctx.player().getEyePosition().distanceTo(corner(ctx).add(eyes)) < 0.1, ARRIVE,
                "the player to float inside the ship");
    }

    private static ShipDesign ship(final String name, final List<String> spine, final List<String> hung) {
        return new ShipDesign(name, spine.stream().map(ShipInteriorsClientTests::part).toList(),
                hung.stream().map(ShipInteriorsClientTests::part).toList());
    }

    private static ResourceLocation part(final String path) {
        return SmallShipParts.id("small_" + path);
    }

    /* The red wall west of the airlock: four blocks high and long, beside its door. */
    private static List<BlockPos> wall(final ClientTestContext ctx) {
        final BlockPos base = BlockPos.containing(corner(ctx)).offset(WALL_X, 0, 0);
        return BlockPos.betweenClosedStream(base.offset(0, 0, 5), base.offset(0, 6, 9)).map(BlockPos::immutable)
                .toList();
    }

    /* The stand's north-west corner, where the ship's nose meets its centre line at the stand's floor. */
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
