/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.space;

import dev.jstech.core.jstechseries.projector.ProjectorSettings;
import dev.jstech.core.jstechseries.projector.ProjectorView;
import dev.jstech.core.jstechseries.projector.StructureProjector;
import dev.jstech.core.jstechseries.projector.StructureProjectorBlockEntity;
import dev.jstech.core.jstechseries.projector.client.ProjectorClient;
import dev.jstech.core.multiblock.FormedParts;
import dev.jstech.space.shipyard.ShipyardStructure;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.space.TestShipyards;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * A Small Shipyard on a player's game: built and formed, it is drawn as the approved dry dock, seen from where its
 * drawing was made, from the open front and from the back; its crane's bridge drawn in its hazard yellow. Its control
 * booth is the approved one, seen from where that drawing's cameras stood: the terminal the console under its windows,
 * its screen showing the Starship Design, and the bench at its back. Shown by a projector, it stands dug into the
 * ground the projector stands on, the land where its pit goes outlined in red.
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
    /*
     * The approved booth's cameras, in the dock's own places: from over the bay looking at it, brought nearer for the
     * game's wider view; inside, from the back by the bench looking at the windows; and by the console looking at the
     * bench, each turned as the drawing's, in degrees.
     */
    private static final Vec3 BOOTH_OUT_EYES = new Vec3(3.99, 6.79, 7.16);
    private static final Vec3 BOOTH_OUT_AT = new Vec3(10.3, 5.5, 10.8);
    private static final Vec3 BOOTH_IN_EYES = new Vec3(11.1, 5.62, 12.1);
    private static final float BOOTH_IN_YAW = 65F;
    private static final float BOOTH_IN_PITCH = 14F;
    private static final Vec3 BOOTH_DESK_EYES = new Vec3(9.9, 5.62, 10.1);
    private static final float BOOTH_DESK_YAW = -25F;
    private static final float BOOTH_DESK_PITCH = 22F;
    /* Standing before the console, looking down at its two halves, the terminal's and the next's. */
    private static final Vec3 CONSOLE_EYES = new Vec3(11.2, 5.62, 11.0);
    private static final Vec3 CONSOLE_AT = new Vec3(9.4, 5.0, 11.0);
    /*
     * The screen's colours, never the sky's: little red, blue well over it, and green either low (its grid) or high
     * (its ship); how many such.
     */
    private static final int DESIGN_RED = 130;
    private static final int DESIGN_BLUE = 30;
    private static final int DESIGN_DARK = 60;
    private static final int DESIGN_CYAN = 170;
    private static final int DESIGN_LEAST = 300;
    /*
     * A projector on the land's grass, as a player south of it places it, the dock shown north of it; the eyes behind
     * and over it, looking at the middle of what it shows; the booth's terminal over the ground.
     */
    private static final BlockPos PROJECTOR = new BlockPos(0, 2, 38);
    private static final Vec3 PROJECTOR_EYES = new Vec3(0, 16, 10);
    private static final Vec3 PROJECTED_MIDDLE = new Vec3(0, -1, -15);
    private static final int BOOTH_ABOVE = 4;
    /* The projector's red: strong red, weak green and blue; and how many more such pixels the dug land makes. */
    private static final int RED_LEAST = 200;
    private static final int OTHER_MOST = 120;
    private static final int MORE_RED = 300;

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

    @ClientTest(timeoutTicks = 1200)
    public static void projector_showsTheDockDugIntoTheGroundItStandsOn(final ClientTestContext ctx) {
        final ServerLevel[] placed = new ServerLevel[1];
        final int[] reds = new int[2];
        ctx.afterTest(() -> ctx.server().execute(() -> {
            if (placed[0] != null) {
                placed[0].setBlock(ctx.abs(PROJECTOR), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                placed[0].players().forEach(player -> player.setGameMode(GameType.CREATIVE));
            }
        }));
        ctx.thenServer(0, level -> {
            placed[0] = level;
            level.setBlock(ctx.abs(PROJECTOR), StructureProjector.BLOCK.get().defaultBlockState()
                    .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH), Block.UPDATE_ALL);
            ctx.serverPlayer().setGameMode(GameType.SPECTATOR);
            final Vec3 eyes = Vec3.atCenterOf(ctx.abs(PROJECTOR)).add(PROJECTOR_EYES);
            ctx.serverPlayer().teleportTo(level, eyes.x, eyes.y - ctx.serverPlayer().getEyeHeight(), eyes.z, 0, 0);
        }).thenWaitUntil(() -> projector(ctx) != null, ARRIVE, "the projector to reach the player's game")
                .then(2, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(PROJECTOR)).add(PROJECTED_MIDDLE)))
                .then(SETTLE, () -> reds[0] = reds(ctx))
                .thenServer(0, level -> {
                    if (level.getBlockEntity(ctx.abs(PROJECTOR)) instanceof StructureProjectorBlockEntity projector) {
                        projector.apply(ProjectorSettings.FRESH.choosing(ShipyardStructure.ID));
                    }
                })
                .thenWaitUntil(() -> projector(ctx) != null && projector(ctx).settings().chosen().isPresent(), ARRIVE,
                        "the projector to show the dock")
                .thenScreenshot(SETTLE, "projector-shipyard")
                .then(0, () -> reds[1] = reds(ctx))
                .then(0, () -> {
                    final ProjectorView view = ProjectorClient.seen(Objects.requireNonNull(projector(ctx)));
                    ctx.assertTrue(view != null && view.terminal().getY() - ctx.abs(PROJECTOR).getY() == BOOTH_ABOVE,
                            "the booth's terminal stands four blocks over the ground the projector stands on");
                    ctx.assertTrue(view.toClear() > 0, "the land where the pit goes is to be cleared");
                    ctx.assertTrue(reds[1] - reds[0] > MORE_RED, "the land in the pit is outlined in red: "
                            + reds[0] + " red pixels before, " + reds[1] + " after");
                });
    }

    @ClientTest(timeoutTicks = 1500)
    public static void booth_isDrawnAsTheApprovedOne(final ClientTestContext ctx) {
        final ServerLevel[] built = new ServerLevel[1];
        final int[] screen = new int[1];
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
        stand(ctx, BOOTH_OUT_EYES);
        ctx.then(2, () -> ctx.lookAt(place(ctx, BOOTH_OUT_AT)))
                .thenScreenshot(SETTLE, "booth-out");
        look(ctx, BOOTH_IN_EYES, BOOTH_IN_YAW, BOOTH_IN_PITCH);
        ctx.thenScreenshot(SETTLE, "booth-in")
                .then(0, () -> screen[0] = designPixels(ctx));
        look(ctx, BOOTH_DESK_EYES, BOOTH_DESK_YAW, BOOTH_DESK_PITCH);
        ctx.thenScreenshot(SETTLE, "booth-desk");
        stand(ctx, CONSOLE_EYES);
        ctx.then(2, () -> ctx.lookAt(place(ctx, CONSOLE_AT)))
                .thenScreenshot(SETTLE, "booth-console")
                .then(0, () -> screen[0] = Math.max(screen[0], designPixels(ctx)))
                .then(0, () -> ctx.assertTrue(screen[0] > DESIGN_LEAST, "the console's screen shows the Starship"
                        + " Design, its blue grid and its ship: " + screen[0] + " pixels"));
    }

    /* The projector shown on this player's side, once it is there. */
    @Nullable
    private static StructureProjectorBlockEntity projector(final ClientTestContext ctx) {
        return ctx.mc().level != null && ctx.mc().level.getBlockEntity(ctx.abs(PROJECTOR))
                instanceof StructureProjectorBlockEntity projector ? projector : null;
    }

    /* How many pixels of the frame are the projector's red, which nothing else in a flat land's day is. */
    private static int reds(final ClientTestContext ctx) {
        final int wide = ctx.mc().getWindow().getGuiScaledWidth();
        final int tall = ctx.mc().getWindow().getGuiScaledHeight();
        int count = 0;
        try (ClientTestContext.FrameSampler frame = ctx.sampleFrame()) {
            for (int y = 0; y < tall; y++) {
                for (int x = 0; x < wide; x++) {
                    final int argb = frame.pixel(x, y);
                    if ((argb >> 16 & CHANNEL) >= RED_LEAST && (argb >> 8 & CHANNEL) <= OTHER_MOST
                            && (argb & CHANNEL) <= OTHER_MOST) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    /* How many pixels of the frame are the Starship Design's blue or its ship's cyan, which nothing else here is. */
    private static int designPixels(final ClientTestContext ctx) {
        final int wide = ctx.mc().getWindow().getGuiScaledWidth();
        final int tall = ctx.mc().getWindow().getGuiScaledHeight();
        int count = 0;
        try (ClientTestContext.FrameSampler frame = ctx.sampleFrame()) {
            for (int y = 0; y < tall; y++) {
                for (int x = 0; x < wide; x++) {
                    final int argb = frame.pixel(x, y);
                    final int r = argb >> 16 & CHANNEL;
                    final int g = argb >> 8 & CHANNEL;
                    final int b = argb & CHANNEL;
                    if (r < DESIGN_RED && b - r > DESIGN_BLUE && b > g && (g < DESIGN_DARK || g > DESIGN_CYAN)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    /* Floats the player, a spectator, with their eyes at the dock's place {@code eyes}, turned as given. */
    private static void look(final ClientTestContext ctx, final Vec3 eyes, final float yaw, final float pitch) {
        ctx.thenServer(0, level -> {
            final Vec3 there = place(ctx, eyes);
            ctx.serverPlayer().teleportTo(level, there.x, there.y - ctx.serverPlayer().getEyeHeight(), there.z, yaw,
                    pitch);
        }).thenWaitUntil(() -> ctx.player().getEyePosition().distanceTo(place(ctx, eyes)) < 0.1
                && Math.abs(ctx.player().getYRot() - yaw) < 1, ARRIVE, "the player to float where the drawing's"
                + " camera stood, turned as it was");
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
