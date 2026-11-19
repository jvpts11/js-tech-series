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
import dev.jstech.core.multiblock.shape.FormedDoors;
import dev.jstech.core.multiblock.shape.IFormedPlaced;
import dev.jstech.space.flight.FlightComputerBlockEntity;
import dev.jstech.space.hangar.RocketDesign;
import dev.jstech.space.hangar.RocketPlans;
import dev.jstech.space.launch.RocketLaunch;
import dev.jstech.space.rocket.RocketKitBlocks;
import dev.jstech.space.rocket.RocketStack;
import dev.jstech.space.rocket.SkeletonRocketParts;
import dev.jstech.space.ship.DoorMotion;
import dev.jstech.testkit.client.ClientTestHelper;
import dev.jstech.tests.TestRockets;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.List;
import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * The rocket parts' formed models on a player's game: a small rocket on a stand is drawn part by part where the bare
 * stand shows only what is behind it, its engine's copper bell at the foot, its tank's white paint, the black
 * interstage skirt its separation ring raises round the vacuum engine above it, and its capsule at the top.
 */
public final class RocketPartsClientTests {

    /* The player twelve blocks south of the stand, looking north at it. */
    private static final BlockPos EYE = new BlockPos(0, 2, -12);
    private static final BlockPos STAND = new BlockPos(0, 2, 0);
    /*
     * The heights looked at: the sea-level engine's bell, the first tank, the skirt round the vacuum engine, the
     * capsule.
     */
    private static final double BELL = 0.5;
    private static final double TANK = 3.5;
    private static final double SKIRT = 7.5;
    private static final double CAPSULE = 13.2;
    /* How far out from the rocket's axis, towards the player, each of them is looked at: on its surface. */
    private static final double[] OUT = {0.3, 1.4, 1.4, 0.9};
    private static final double MIDDLE = 7.5;
    private static final int PATCH = 2;
    /* The patch read is this many scaled pixels below the crosshair, about this many blocks at the rocket. */
    private static final int BESIDE = 8;
    private static final double RAISED = 0.4;
    private static final int DRAWN = 40;
    private static final int BLACK = 80;
    private static final int COPPER = 20;
    private static final int ARRIVE = 600;
    private static final int SETTLE = 10;
    private static final int CHANNEL = 0xFF;
    /*
     * A hatch seen from out beside it, how far out and how far up; how long it takes to swing half way
     * and all the way, in ticks; how much two pixels may differ and be the same, and how many must differ to move.
     */
    private static final double HATCH_OUT = 3.5;
    private static final double HATCH_UP = 0.3;
    private static final double HALF = 0.5;
    private static final int HALF_SWING = (int) Math.round(DoorMotion.SWING * 20 / 2);
    private static final int SWUNG = (int) Math.ceil(DoorMotion.SWING * 20) + 10;
    private static final int SAME = 6;
    private static final int MOVED = 200;
    private static final int DUST_SETTLES = 80;
    /* How far round the middle of the frame its pixels are compared, in the game's scaled pixels. */
    private static final int LOOKED_AT = 50;

    private RocketPartsClientTests() {
    }

    @ClientTest(timeoutTicks = 900)
    public static void rocket_isDrawnPartByPartAsItsModelsShowThem(final ClientTestContext ctx) {
        final double[] heights = {BELL, TANK, SKIRT, CAPSULE};
        final int[][] drawn = new int[heights.length][];
        ctx.afterTest(() -> ctx.server().execute(() -> ctx.serverLevel().removeBlock(ctx.abs(STAND), false)));
        ctx.thenTeleport(0, EYE, Direction.SOUTH)
                .thenServer(0, level -> {
                    level.setBlockAndUpdate(ctx.abs(STAND), TestRockets.DISPLAY.get().defaultBlockState());
                    ((TestRockets.DisplayEntity) level.getBlockEntity(ctx.abs(STAND))).show(rocket(), false);
                })
                .thenWaitUntil(() -> ctx.mc().level.getBlockEntity(ctx.abs(STAND))
                        instanceof TestRockets.DisplayEntity stand && !stand.stack().sections().isEmpty(), ARRIVE,
                        "the stand's rocket to reach the player's game")
                .then(2, () -> look(ctx, MIDDLE, 0))
                .thenScreenshot(SETTLE, "rocket-parts-formed");
        for (int i = 0; i < heights.length; i++) {
            final int k = i;
            ctx.then(2, () -> look(ctx, heights[k], OUT[k])).then(4, () -> drawn[k] = mean(ctx));
        }
        ctx.then(0, () -> {
            ctx.assertTrue(drawn[2][0] < BLACK && drawn[2][1] < BLACK && drawn[2][2] < BLACK,
                    "the interstage skirt round the vacuum engine is black: " + str(drawn[2]));
            ctx.assertTrue(lighter(drawn[1], drawn[2]), "the tank is painted white, lighter than the skirt: "
                    + str(drawn[1]) + " against " + str(drawn[2]));
            ctx.assertTrue(lighter(drawn[3], drawn[2]), "the capsule is light, lighter than the skirt: "
                    + str(drawn[3]) + " against " + str(drawn[2]));
            ctx.assertTrue(drawn[0][0] - drawn[0][2] > COPPER && drawn[0][0] > drawn[0][1],
                    "the engine's bell is copper, redder than green and than blue: " + str(drawn[0]));
        }).thenServer(0, level -> ((TestRockets.DisplayEntity) level.getBlockEntity(ctx.abs(STAND)))
                .show(new RocketStack(List.of()), false))
                .thenWaitUntil(() -> ctx.mc().level.getBlockEntity(ctx.abs(STAND))
                        instanceof TestRockets.DisplayEntity stand && stand.stack().sections().isEmpty(), ARRIVE,
                        "the bare stand to reach the player's game");
        for (int i = 0; i < heights.length; i++) {
            final int k = i;
            ctx.then(2, () -> look(ctx, heights[k], OUT[k])).then(4, () -> {
                final int[] bare = mean(ctx);
                ctx.assertTrue(distance(drawn[k], bare) > DRAWN, "the rocket is drawn at " + heights[k]
                        + " blocks up: " + str(drawn[k]) + " where the bare stand shows " + str(bare));
            });
        }
    }

    /*
     * A capsule built on a short rocket, its flight computer drawing it, seen from out beside its hatch: shut,
     * swinging, and open, each drawn otherwise than the one before, the hatch turned out about its hinge.
     */
    @ClientTest(timeoutTicks = 900)
    public static void capsuleHatch_swingsOutAboutItsHingeAsItOpens(final ClientTestContext ctx) {
        hatchSwings(ctx, RocketStack.of(part("sea_level_engine_3", 1), part("tank_3", 1), part("capsule_3", 1)),
                "capsule-hatch");
    }

    /*
     * A lunar lander built whole, its flight computer drawing it, seen from out in front of its forward hatch: shut,
     * swinging into its cabin, and open, each drawn otherwise than the one before.
     */
    @ClientTest(timeoutTicks = 900)
    public static void landerHatch_swingsInAboutItsHingeAsItOpens(final ClientTestContext ctx) {
        hatchSwings(ctx, RocketStack.of(part("lunar_lander_descent", 1), part("lunar_lander_ascent", 1)),
                "lander-hatch");
    }

    /*
     * Builds {@code stack} whole, its flight computer drawing it, and watches its hatch from out beside it: shut,
     * swinging and open, each drawn otherwise than the one before, a shot of each named after {@code shots}.
     */
    private static void hatchSwings(final ClientTestContext ctx, final RocketStack stack, final String shots) {
        final PlannedBuild[] build = new PlannedBuild[1];
        final BlockPos[] hatch = new BlockPos[1];
        final int[][] frames = new int[3][];
        ctx.afterTest(() -> {
            ctx.mc().options.hideGui = false;
            ctx.server().execute(() -> {
                if (build[0] != null) {
                    build[0].plan().spots().forEach(spot -> ctx.serverLevel().removeBlock(build[0].where(spot),
                            false));
                }
            });
        });
        ctx.thenServer(0, level -> {
            final BlockPos foot = ctx.abs(STAND);
            build[0] = new PlannedBuild(RocketPlans.of(level, stack), foot);
            build[0].start(level, foot.below(), () -> { });
            for (final PlanSpot spot : build[0].plan().spots()) {
                level.setBlockAndUpdate(build[0].where(spot), BuiltInRegistries.BLOCK.get(spot.block())
                        .defaultBlockState());
            }
            build[0].stop(false);
            final BlockPos computer = where(build[0], RocketKitBlocks.FLIGHT_COMPUTER.getId());
            ((FlightComputerBlockEntity) level.getBlockEntity(computer)).carry(new RocketDesign("HATCH", stack),
                    foot.subtract(computer), IntStream.range(0, stack.sections().size()).boxed().toList());
            hatch[0] = where(build[0], RocketKitBlocks.CAPSULE_HATCH.getId());
            // Out from the hatch, away from the rocket's axis, a little up, looking back at it.
            final Vec3 middle = Vec3.atCenterOf(hatch[0]);
            final Vec3 out = middle.subtract(foot.getX() + HALF, middle.y, foot.getZ() + HALF).normalize();
            final Vec3 eye = middle.add(out.scale(HATCH_OUT)).add(0, HATCH_UP, 0);
            ctx.serverPlayer().teleportTo(level, eye.x, eye.y - ctx.serverPlayer().getEyeHeight(), eye.z, 0, 0);
        })
                .then(SETTLE * 2, () -> {
                    ctx.lookAt(Vec3.atCenterOf(hatch[0]));
                    ctx.mc().options.hideGui = true;
                })
                // Long enough for the dust of its blocks set down to settle, so only the hatch moves after.
                .then(DUST_SETTLES, () -> frames[0] = frame(ctx))
                .thenScreenshot(0, shots + "-shut")
                .thenServer(0, level -> ((IFormedPlaced) level.getBlockEntity(hatch[0])).places().stream()
                        .flatMap(place -> FormedDoors.named(level, hatch[0], place, RocketLaunch.HATCH).stream())
                        .findFirst().ifPresent(door -> FormedDoors.use(level, null, door)))
                .then(HALF_SWING, () -> {
                    frames[1] = frame(ctx);
                    ctx.assertTrue(!FormedDoors.shut(ctx.mc().level, hatch[0], RocketLaunch.HATCH, true),
                            "the player's game holds the hatch open");
                })
                .thenScreenshot(0, shots + "-swinging")
                .then(SWUNG, () -> {
                    frames[2] = frame(ctx);
                    ctx.assertTrue(changed(frames[0], frames[1]) > MOVED && changed(frames[1], frames[2]) > MOVED,
                            "half way, its hatch is drawn between shut and open: " + changed(frames[0], frames[1])
                                    + " and " + changed(frames[1], frames[2]) + " pixels");
                })
                .thenScreenshot(0, shots + "-open");
    }

    /*
     * A small rocket: a sea-level engine, three layers of tank, a separation ring, a vacuum engine in its skirt, two
     * layers of tank, the instrument ring and the capsule, fifteen blocks tall.
     */
    private static RocketStack rocket() {
        return RocketStack.of(part("sea_level_engine_3", 1), part("tank_3", 3), part("separation_ring_3", 1),
                part("vacuum_engine_3", 1), part("tank_3", 2), part("instrument_ring_3", 1), part("capsule_3", 1));
    }

    private static RocketStack.Part part(final String path, final int layers) {
        return new RocketStack.Part(SkeletonRocketParts.id(path), layers);
    }

    /*
     * Looks at the rocket a little above {@code height} up, {@code out} from its axis towards the player, to the
     * north, so the patch read below the crosshair falls at that height.
     */
    private static void look(final ClientTestContext ctx, final double height, final double out) {
        ctx.lookAt(Vec3.atBottomCenterOf(ctx.abs(STAND)).add(0, height + RAISED, -out));
    }

    /*
     * The mean colour, red, green and blue, of a patch just below the crosshair, which would whiten it; the look is
     * raised as far, so the patch falls where the rocket is looked at.
     */
    private static int[] mean(final ClientTestContext ctx) {
        final int x = ctx.mc().getWindow().getGuiScaledWidth() / 2;
        final int y = ctx.mc().getWindow().getGuiScaledHeight() / 2 + BESIDE;
        final long[] sum = new long[3];
        int count = 0;
        try (ClientTestHelper.FrameSampler frame = ctx.sampleFrame()) {
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

    /* Whether {@code a} is lighter than {@code b} in every channel, by {@link #DRAWN} at least. */
    private static boolean lighter(final int[] a, final int[] b) {
        return a[0] - b[0] > DRAWN && a[1] - b[1] > DRAWN && a[2] - b[2] > DRAWN;
    }

    private static int distance(final int[] a, final int[] b) {
        return Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]) + Math.abs(a[2] - b[2]);
    }

    private static String str(final int[] colour) {
        return "(" + colour[0] + ", " + colour[1] + ", " + colour[2] + ")";
    }

    /* Where the plan of {@code build} wants the block {@code block}, the first place of it. */
    private static BlockPos where(final PlannedBuild build, final ResourceLocation block) {
        return build.where(build.plan().spots().stream().filter(spot -> spot.block().equals(block)).findFirst()
                .orElseThrow());
    }

    /* How many pixels of {@code a} and {@code b} differ. */
    private static int changed(final int[] a, final int[] b) {
        int out = 0;
        for (int i = 0; i < Math.min(a.length, b.length); i++) {
            if (differ(a[i], b[i])) {
                out++;
            }
        }
        return out;
    }

    /* The frame's middle, round where the eye looks, its pixels row by row: the sky's drifting clouds left out. */
    private static int[] frame(final ClientTestContext ctx) {
        final int x0 = ctx.mc().getWindow().getGuiScaledWidth() / 2 - LOOKED_AT;
        final int y0 = ctx.mc().getWindow().getGuiScaledHeight() / 2 - LOOKED_AT;
        final int side = 2 * LOOKED_AT;
        final int[] out = new int[side * side];
        try (ClientTestContext.FrameSampler frame = ctx.sampleFrame()) {
            for (int y = 0; y < side; y++) {
                for (int x = 0; x < side; x++) {
                    out[y * side + x] = frame.pixel(x0 + x, y0 + y);
                }
            }
        }
        return out;
    }

    /* Whether two pixels differ by more than {@link #SAME} in a channel. */
    private static boolean differ(final int a, final int b) {
        for (int shift = 0; shift <= 16; shift += 8) {
            if (Math.abs((a >> shift & CHANNEL) - (b >> shift & CHANNEL)) > SAME) {
                return true;
            }
        }
        return false;
    }
}
