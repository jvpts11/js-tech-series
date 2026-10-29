/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.space;

import dev.jstech.space.JsSpace;
import dev.jstech.space.client.rocket.RocketCabins;
import dev.jstech.space.rocket.CabinLamp;
import dev.jstech.space.rocket.CabinReadings;
import dev.jstech.space.rocket.RocketStack;
import dev.jstech.space.rocket.SkeletonRocketParts;
import dev.jstech.tests.TestRockets;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * A capsule's live cabin on a player's game, seen from where its crew lie: its lamps unlit at rest, the engine's lamp
 * green and the master alarm red once lit, and the attitude ball turned by the readings, its sky towards the crew when
 * it pitches one way and its ground the other.
 */
public final class RocketCabinClientTests {

    private static final BlockPos STAND = new BlockPos(0, 4, 0);
    private static final ResourceLocation CAPSULE = ResourceLocation.fromNamespaceAndPath(JsSpace.MODID,
            "rocket/capsule_3");
    /*
     * Where a crew member lying on the capsule of three's couch has their eyes, from the middle of its foot: over the
     * couch's block, its foot a block up, as low over it as a sleeper's, its edge towards their head.
     */
    private static final Vec3 EYES = new Vec3(0, 1.3, 0.5);
    /* The lamps looked at: the engine's, at the head of the bank's left column, and the master alarm's under it. */
    private static final int ENGINE = 0;
    private static final int ALARM = 4;
    private static final float PITCH = 60;
    private static final float NORTH = 180;
    private static final float UP = -90;
    private static final int ARRIVE = 600;
    private static final int SETTLE = 10;
    /* How far one channel of a lamp or the ball outshines the others, read from the frame. */
    private static final int SHINES = 50;
    private static final int CHANNEL = 0xFF;

    private RocketCabinClientTests() {
    }

    @ClientTest(timeoutTicks = 1500)
    public static void cabin_showsItsLampsAndItsBallAsTheReadingsSay(final ClientTestContext ctx) {
        final int[][] seen = new int[6][];
        ctx.afterTest(() -> ctx.server().execute(() -> {
            ctx.serverLevel().removeBlock(ctx.abs(STAND), false);
            ctx.serverPlayer().setGameMode(GameType.CREATIVE);
        }));
        ctx.thenServer(0, level -> {
                    level.setBlockAndUpdate(ctx.abs(STAND), TestRockets.DISPLAY.get().defaultBlockState());
                    final TestRockets.DisplayEntity stand = (TestRockets.DisplayEntity) level.getBlockEntity(
                            ctx.abs(STAND));
                    stand.show(RocketStack.of(new RocketStack.Part(SkeletonRocketParts.id("capsule_3"), 1)), false);
                    stand.read(CabinReadings.AT_REST);
                    // A spectator floats where the crew lie, inside the model, through the stand's own block; their
                    // feet to the north, as the crew's, so the panel stands upright above them.
                    ctx.serverPlayer().setGameMode(GameType.SPECTATOR);
                    final Vec3 eyes = foot(ctx).add(EYES);
                    ctx.serverPlayer().teleportTo(level, eyes.x, eyes.y - ctx.serverPlayer().getEyeHeight(), eyes.z,
                            NORTH, UP);
                })
                .thenWaitUntil(() -> ctx.mc().level.getBlockEntity(ctx.abs(STAND))
                        instanceof TestRockets.DisplayEntity stand && !stand.stack().sections().isEmpty()
                        && RocketCabins.of(CAPSULE).isPresent() && ctx.player().getXRot() < UP + 1, ARRIVE,
                        "the stand's capsule to reach the player, looking up")
                .then(SETTLE, () -> {
                    seen[0] = sample(ctx, lamp(ctx, ENGINE));
                    seen[1] = sample(ctx, lamp(ctx, ALARM));
                })
                .thenServer(0, level -> ((TestRockets.DisplayEntity) level.getBlockEntity(ctx.abs(STAND)))
                        .read(readings(PITCH, Set.of(CabinLamp.ENGINE, CabinLamp.MASTER_ALARM))))
                .thenWaitUntil(() -> ctx.mc().level.getBlockEntity(ctx.abs(STAND))
                        instanceof TestRockets.DisplayEntity stand && stand.readings().lit(CabinLamp.ENGINE), ARRIVE,
                        "the lit lamps to reach the player")
                .then(SETTLE, () -> {
                    seen[2] = sample(ctx, lamp(ctx, ENGINE));
                    seen[3] = sample(ctx, lamp(ctx, ALARM));
                    seen[4] = sample(ctx, ball(ctx));
                })
                .thenScreenshot(0, "rocket-cabin-lit")
                .thenServer(0, level -> ((TestRockets.DisplayEntity) level.getBlockEntity(ctx.abs(STAND)))
                        .read(readings(-PITCH, Set.of())))
                .thenWaitUntil(() -> ctx.mc().level.getBlockEntity(ctx.abs(STAND))
                        instanceof TestRockets.DisplayEntity stand && stand.readings().pitch() < 0, ARRIVE,
                        "the ball pitched the other way to reach the player")
                .then(SETTLE, () -> seen[5] = sample(ctx, ball(ctx)))
                .then(0, () -> {
                    ctx.assertTrue(!greenest(seen[0]), "the engine's lamp is unlit at rest: " + str(seen[0]));
                    ctx.assertTrue(greenest(seen[2]), "lit, the engine's lamp shines green: " + str(seen[2]));
                    ctx.assertTrue(seen[1][0] - seen[1][1] < SHINES, "the master alarm is unlit at rest: "
                            + str(seen[1]));
                    ctx.assertTrue(seen[3][0] - seen[3][1] > SHINES && seen[3][0] - seen[3][2] > SHINES,
                            "lit, the master alarm shines red: " + str(seen[3]));
                    ctx.assertTrue(seen[4][2] > seen[4][0], "pitched one way, the ball turns its blue sky to the "
                            + "crew: " + str(seen[4]));
                    ctx.assertTrue(seen[5][0] > seen[5][2], "pitched the other, its brown ground: " + str(seen[5]));
                });
    }

    private static CabinReadings readings(final float pitch, final Set<CabinLamp> lit) {
        return new CabinReadings(10, 1, 1, 0, 0, pitch, 0, 0, 1, 1, lit);
    }

    /* The middle of the stand's floor, where the rocket's foot stands. */
    private static Vec3 foot(final ClientTestContext ctx) {
        return Vec3.atBottomCenterOf(ctx.abs(STAND));
    }

    /* The middle of the glass of the lamp {@code k} of the capsule's panel, in the world. */
    private static Vec3 lamp(final ClientTestContext ctx, final int k) {
        final RocketCabins.Cabin cabin = RocketCabins.of(CAPSULE).orElseThrow();
        final List<RocketCabins.Region> lamps = cabin.regions().stream().filter(region -> region.kind().equals(
                "lamp")).toList();
        final RocketCabins.Region lamp = lamps.get(k);
        final RocketCabins.Face face = cabin.face(lamp.face()).orElseThrow();
        final float across = ((lamp.left() + lamp.right()) / 2 - 0.5F) * 2 * face.halfWidth();
        final float up = (0.5F - (lamp.top() + lamp.bottom()) / 2) * 2 * face.halfHeight();
        final Vector3f at = new Vector3f(face.centre()).add(new Vector3f(face.right()).mul(across))
                .add(new Vector3f(face.up()).mul(up));
        return foot(ctx).add(at.x(), at.y(), at.z());
    }

    /* The attitude ball's middle, in the world. */
    private static Vec3 ball(final ClientTestContext ctx) {
        final Vector3f centre = RocketCabins.of(CAPSULE).orElseThrow().ball().orElseThrow().centre();
        return foot(ctx).add(centre.x(), centre.y(), centre.z());
    }

    /*
     * The colour, red, green and blue, where {@code point} is drawn, the player's eyes looking straight up with their
     * feet to the north: the brightest of it and the four pixels beside it. Looking up so, the screen's right is east
     * and its top south, each at the camera's focal length a block of depth.
     */
    private static int[] sample(final ClientTestContext ctx, final Vec3 point) {
        final Vec3 eye = ctx.player().getEyePosition();
        final int w = ctx.mc().getWindow().getGuiScaledWidth();
        final int h = ctx.mc().getWindow().getGuiScaledHeight();
        // The game widens the view for one flying, as a spectator always is.
        final double fov = ctx.mc().options.fov().get() * Mth.lerp(ctx.mc().options.fovEffectScale().get(), 1.0,
                ctx.player().getFieldOfViewModifier());
        final double focal = h / 2.0 / Math.tan(Math.toRadians(fov) / 2);
        final double depth = point.y - eye.y;
        final int x = (int) Math.round(w / 2.0 + (point.x - eye.x) / depth * focal);
        final int y = (int) Math.round(h / 2.0 - (point.z - eye.z) / depth * focal);
        int[] best = {0, 0, 0};
        try (ClientTestContext.FrameSampler frame = ctx.sampleFrame()) {
            for (final int[] off : new int[][] {{0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                final int argb = frame.pixel(x + off[0], y + off[1]);
                final int[] colour = {argb >> 16 & CHANNEL, argb >> 8 & CHANNEL, argb & CHANNEL};
                if (colour[0] + colour[1] + colour[2] > best[0] + best[1] + best[2]) {
                    best = colour;
                }
            }
        }
        return best;
    }

    /* Whether green outshines red and blue. */
    private static boolean greenest(final int[] colour) {
        return colour[1] - colour[0] > SHINES && colour[1] - colour[2] > SHINES;
    }

    private static String str(final int[] colour) {
        return "(" + colour[0] + ", " + colour[1] + ", " + colour[2] + ")";
    }
}
