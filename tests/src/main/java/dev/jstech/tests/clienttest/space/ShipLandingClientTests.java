/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.space;

import dev.jstech.core.team.Access;
import dev.jstech.core.team.Ownership;
import dev.jstech.space.landing.RunwayBeaconBlock;
import dev.jstech.space.landing.ShipLandingBlocks;
import dev.jstech.space.landing.ShipPadBlockEntity;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.UUID;
import java.util.function.IntPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

/**
 * Where a ship sets down, on a player's game, as the approved mocks draw it: a ship landing pad's blocks show bare
 * until it forms, and then it is drawn whole, the raised pad, its hazard band painted round its deck and its flush cyan
 * lights shining round its rim; a runway beacon shows its green lens to the ship coming in, and none to the runway
 * behind it.
 */
public final class ShipLandingClientTests {

    /* The block under the pad's deck's middle, on the ground, and where its player looks down on it from. */
    private static final BlockPos MIDDLE = new BlockPos(0, 2, 18);
    private static final BlockPos ABOVE = new BlockPos(0, 16, 4);
    /* The pattern's bounds round its middle: across, up, and along, north to south. */
    private static final int REACH = ShipLandingBlocks.DECK_RADIUS;
    private static final int TOP_LAYER = 8;
    private static final int SOUTH = 15;
    /* The beacon, facing north; where its player stands to the south of it, coming in, and to the north, behind. */
    private static final BlockPos BEACON = new BlockPos(0, 2, 6);
    private static final BlockPos COMING_IN = new BlockPos(0, 2, 8);
    private static final BlockPos BEHIND = new BlockPos(0, 2, 4);
    /* A point of the hazard band round the deck, in blocks east of its middle. */
    private static final double RING = 8.6;
    /* The deck's top over the ground, where the paint lies. */
    private static final double TOP = ShipLandingBlocks.DECK + 1.0;
    /* The lens's middle in its block, in sixteenths: over its stem, on the south face of its head. */
    private static final double LENS_UP = 11.0 / 16;
    private static final double LENS_OUT = 12.25 / 16;
    /* How far down the player looks as they arrive over the pad, in degrees. */
    private static final float LOOK_DOWN = 45F;
    /* The patch round the crosshair a colour is counted in, GUI pixels each way. */
    private static final int PATCH = 12;
    /* How many of a patch's pixels are the colour looked for, at the least, where it is, and at the most where not. */
    private static final int SEEN = 8;
    private static final int NONE = 1;
    /* How many pixels of the whole frame its lights are, at the least, the pad's 32 in sight. */
    private static final int LIGHTS_SEEN = 40;
    private static final int ARRIVE = 400;
    private static final int SETTLE = 20;
    private static final int CHANNEL = 0xFF;

    private ShipLandingClientTests() {
    }

    @ClientTest(timeoutTicks = 1600)
    public static void shipPad_isDrawnWholeWithItsBandAndItsLightsOnceItForms(final ClientTestContext ctx) {
        final int[] counts = new int[4];
        final BlockPos missing = MIDDLE.offset(REACH, ShipLandingBlocks.DECK, 0);
        ctx.afterTest(() -> ctx.server().execute(() -> {
            // The terminal first, so its merged blocks are given back, then every block of the pad.
            ctx.serverLevel().removeBlock(ctx.abs(MIDDLE.offset(ShipLandingBlocks.TERMINAL_X,
                    ShipLandingBlocks.TERMINAL_Y, ShipLandingBlocks.TERMINAL_Z)), false);
            for (int x = -REACH; x <= REACH; x++) {
                for (int y = 0; y <= TOP_LAYER; y++) {
                    for (int z = -REACH; z <= SOUTH; z++) {
                        ctx.serverLevel().removeBlock(ctx.abs(MIDDLE.offset(x, y, z)), false);
                    }
                }
            }
            fly(ctx, false);
            final BlockPos origin = ctx.origin().above(2);
            ctx.serverPlayer().teleportTo(ctx.server().overworld(), origin.getX() + 0.5, origin.getY(),
                    origin.getZ() + 0.5, 0F, 0F);
        }));
        ctx.afterTest(() -> ctx.mc().options.hideGui = false);
        // Flying, so the player stays where they look down on the pad from; the hand and the HUD hidden, so only the
        // world is in the frame its colours are counted in.
        ctx.then(0, () -> ctx.mc().options.hideGui = true).thenServer(0, level -> {
            fly(ctx, true);
            final BlockPos above = ctx.abs(ABOVE);
            ctx.serverPlayer().teleportTo(level, above.getX() + 0.5, above.getY(), above.getZ() + 0.5, 0F, LOOK_DOWN);
        })
                .thenServer(0, level -> {
                    for (int x = -REACH; x <= REACH; x++) {
                        for (int y = 0; y <= TOP_LAYER; y++) {
                            for (int z = -REACH; z <= SOUTH; z++) {
                                final BlockPos at = MIDDLE.offset(x, y, z);
                                // One plate left out, so the pad stands bare until it is laid.
                                if (!at.equals(missing)) {
                                    place(level, ctx.abs(at), ShipLandingBlocks.letterAt(x, y, z));
                                }
                            }
                        }
                    }
                    ((ShipPadBlockEntity) level.getBlockEntity(ctx.abs(terminal()))).own(Ownership.of(
                            UUID.randomUUID()).withAccess(Access.TEAM));
                })
                .then(SETTLE, () -> ctx.lookAt(ring(ctx)))
                .then(SETTLE, () -> counts[0] = count(ctx, ShipLandingClientTests::yellow))
                .then(0, () -> ctx.lookAt(middle(ctx)))
                .then(SETTLE, () -> counts[1] = countAll(ctx, ShipLandingClientTests::cyan))
                .thenScreenshot(0, "ship-pad-bare")
                .thenServer(0, level -> level.setBlockAndUpdate(ctx.abs(missing), ShipLandingBlocks.PLATE.get()
                        .defaultBlockState()))
                .thenWaitUntil(() -> ctx.mc().level.getBlockEntity(ctx.abs(terminal())) instanceof ShipPadBlockEntity
                        pad && pad.shownFormed(), ARRIVE, "the formed pad to reach the player")
                .then(SETTLE, () -> counts[3] = countAll(ctx, ShipLandingClientTests::cyan))
                .thenScreenshot(0, "ship-pad-formed")
                .then(0, () -> ctx.lookAt(ring(ctx)))
                .then(SETTLE, () -> counts[2] = count(ctx, ShipLandingClientTests::yellow))
                .thenScreenshot(0, "ship-pad-ring")
                .then(0, () -> {
                    ctx.assertTrue(counts[0] <= NONE && counts[1] <= NONE, "bare, it shows no band and no lights: "
                            + counts[0] + " yellow, " + counts[1] + " cyan");
                    ctx.assertTrue(counts[2] >= SEEN, "formed, its hazard band is painted where the mock has it: "
                            + counts[2] + " yellow");
                    ctx.assertTrue(counts[3] >= LIGHTS_SEEN, "and its cyan lights shine round its rim: " + counts[3]
                            + " cyan");
                });
    }

    /* The pad's terminal, on its deck by the stair's head. */
    private static BlockPos terminal() {
        return MIDDLE.offset(ShipLandingBlocks.TERMINAL_X, ShipLandingBlocks.TERMINAL_Y, ShipLandingBlocks.TERMINAL_Z);
    }

    /* Puts the block of the pad's pattern {@code letter} at {@code at}, facing north as the pattern does. */
    private static void place(final ServerLevel level, final BlockPos at, final char letter) {
        final Block block = switch (letter) {
            case '#' -> ShipLandingBlocks.TERMINAL.get();
            case 'P' -> ShipLandingBlocks.PLATE.get();
            case 'T' -> ShipLandingBlocks.TRUSS.get();
            case 'S' -> ShipLandingBlocks.STEP.get();
            case 'M' -> ShipLandingBlocks.MAST_BLOCK.get();
            default -> null;
        };
        if (block != null) {
            level.setBlockAndUpdate(at, block.defaultBlockState());
        }
    }

    @ClientTest(timeoutTicks = 800)
    public static void runwayBeacon_showsItsGreenLensToTheShipComingIn(final ClientTestContext ctx) {
        final int[] counts = new int[2];
        ctx.afterTest(() -> ctx.server().execute(() -> ctx.serverLevel().removeBlock(ctx.abs(BEACON), false)));
        ctx.thenTeleport(0, COMING_IN, Direction.NORTH)
                .thenServer(0, level -> level.setBlockAndUpdate(ctx.abs(BEACON), ShipLandingBlocks.BEACON.get()
                        .defaultBlockState().setValue(RunwayBeaconBlock.FACING, Direction.NORTH)))
                .then(SETTLE, () -> ctx.lookAt(lens(ctx)))
                .then(SETTLE, () -> counts[0] = count(ctx, ShipLandingClientTests::green))
                .thenScreenshot(0, "runway-beacon-coming-in")
                .thenTeleport(0, BEHIND, Direction.SOUTH)
                .then(SETTLE, () -> ctx.lookAt(lens(ctx)))
                .then(SETTLE, () -> counts[1] = count(ctx, ShipLandingClientTests::green))
                .thenScreenshot(0, "runway-beacon-behind")
                .then(0, () -> {
                    ctx.assertTrue(counts[0] >= SEEN, "coming in, its green lens shows: " + counts[0] + " green");
                    ctx.assertTrue(counts[1] <= NONE, "from the runway behind it, none: " + counts[1] + " green");
                });
    }

    /* Sets the player flying, or back to walking. */
    private static void fly(final ClientTestContext ctx, final boolean flying) {
        final ServerPlayer player = ctx.serverPlayer();
        player.getAbilities().flying = flying;
        player.onUpdateAbilities();
    }

    /* A point of the pad's hazard band, east of its middle, on its deck. */
    private static Vec3 ring(final ClientTestContext ctx) {
        return Vec3.atBottomCenterOf(ctx.abs(MIDDLE)).add(RING, TOP, 0);
    }

    /* The middle of the pad's deck, the whole pad in sight round it. */
    private static Vec3 middle(final ClientTestContext ctx) {
        return Vec3.atBottomCenterOf(ctx.abs(MIDDLE)).add(0, TOP, 0);
    }

    /* The middle of the beacon's lens. */
    private static Vec3 lens(final ClientTestContext ctx) {
        final BlockPos at = ctx.abs(BEACON);
        return new Vec3(at.getX() + 0.5, at.getY() + LENS_UP, at.getZ() + LENS_OUT);
    }

    /* How many pixels of the whole frame are {@code colour}. */
    private static int countAll(final ClientTestContext ctx, final IntPredicate colour) {
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

    /*
     * How many pixels of the patch round the crosshair are {@code colour}: the crosshair, drawn inverting what is under
     * it, is never one of the colours looked for, so it only hides a few.
     */
    private static int count(final ClientTestContext ctx, final IntPredicate colour) {
        final int cx = ctx.mc().getWindow().getGuiScaledWidth() / 2;
        final int cy = ctx.mc().getWindow().getGuiScaledHeight() / 2;
        int found = 0;
        try (ClientTestContext.FrameSampler frame = ctx.sampleFrame()) {
            for (int dx = -PATCH; dx <= PATCH; dx++) {
                for (int dy = -PATCH; dy <= PATCH; dy++) {
                    if (colour.test(frame.pixel(cx + dx, cy + dy))) {
                        found++;
                    }
                }
            }
        }
        return found;
    }

    /* The ring's paint, the mock's yellow, as daylight shows it on the pad's top. */
    private static boolean yellow(final int argb) {
        final int r = argb >> 16 & CHANNEL;
        final int g = argb >> 8 & CHANNEL;
        final int b = argb & CHANNEL;
        return r > 150 && g > 110 && b < 90 && r - b > 90;
    }

    /*
     * The lights' cyan, lit, any of its tones: nearly as green as blue, where the sky overhead is far bluer than green
     * and its pale blue at the horizon nearly as red as green.
     */
    private static boolean cyan(final int argb) {
        final int r = argb >> 16 & CHANNEL;
        final int g = argb >> 8 & CHANNEL;
        final int b = argb & CHANNEL;
        return b >= 190 && g >= 160 && g - r >= 40 && b - g < 50;
    }

    /* The lens's green, lit. */
    private static boolean green(final int argb) {
        final int r = argb >> 16 & CHANNEL;
        final int g = argb >> 8 & CHANNEL;
        final int b = argb & CHANNEL;
        return g > 170 && g - r > 60 && g - b > 40;
    }
}
