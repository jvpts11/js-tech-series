/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.space;

import com.mojang.logging.LogUtils;
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.multiblock.plan.PlannedBuild;
import dev.jstech.space.ship.ShipDesign;
import dev.jstech.space.ship.ShipKitBlocks;
import dev.jstech.space.ship.SmallShipParts;
import dev.jstech.space.shipyard.ShipyardTerminalBlockEntity;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.space.TestShipyards;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

/**
 * A ship built whole in its dock, on a player's game, seen by a player standing in it, looking at its nose, the dock's
 * terminal behind: the ship is drawn round the player, its deck and hull filling the view, not left out because its
 * walls hide the terminal's own chunk from the game's culling. And seen from above its back: through its canopy the
 * cockpit shows, not the ground beyond the ship, its glass drawn after everything solid behind it.
 */
public final class ShipDockedClientTests {

    /* The dock dug into the land where the test stands, as the shipyard's client test builds it. */
    private static final BlockPos CORNER = new BlockPos(-11, -1, 6);
    private static final int X0 = -13;
    private static final int X1 = 13;
    private static final int Z0 = 4;
    private static final int Z1 = 34;
    private static final int Y_BOTTOM = -2;
    private static final int Y_GRASS = 1;
    private static final int Y_TOP = 16;
    private static final int ARRIVE = 600;
    private static final int SETTLE = 20;
    /* Behind the console, standing in the cockpit: how far back and how high the eyes are; how far down they look. */
    private static final double BEHIND = 1.5;
    private static final double EYE = 1.62;
    private static final float DOWN = 35F;
    /*
     * How light the deck's grey is at least in every channel, and how far its channels are apart at most; how much of
     * the lower view it fills, drawn: the cockpit's pilot's seat on its plinth and its console take part of the view,
     * leaving the deck and the hull about two fifths of it, where a ship left out leaves next to none.
     */
    private static final int LIGHT = 140;
    private static final int FLAT = 20;
    private static final int CHANNEL = 0xFF;
    private static final double DRAWN = 0.3;
    /*
     * Where the canopy is seen from, from the flight console, and where it is looked at; where its glass stands in the
     * frame, as shares of its width and height; how light the cockpit's walls seen through the glass are at least in
     * every channel; and how much of the glass they fill at least (they fill about a third; the dark ground beyond the
     * ship, seen when the cockpit was not drawn behind the glass, left a sixteenth that light).
     */
    private static final Vec3 CANOPY_FROM = new Vec3(0.5, 1.8, -2.2);
    private static final Vec3 CANOPY_AT = new Vec3(0.5, 1.4, 0.5);
    private static final double GLASS_LEFT = 0.44;
    private static final double GLASS_RIGHT = 0.66;
    private static final double GLASS_TOP = 0.39;
    private static final double GLASS_BOTTOM = 0.52;
    private static final int WALLS = 80;
    private static final double CABIN = 0.2;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ShipDesign SHIP = new ShipDesign("JS-09", List.of(part("orbiter_cockpit"), part("airlock"),
            part("quarters"), part("workshop"), part("reactor_flush")), List.of(part("twin_bells"),
            part("side_intakes"), part("blended_delta"), part("twin_canted_fins"), part("blisters"), part("legs")));

    private ShipDockedClientTests() {
    }

    @ClientTest(timeoutTicks = 2400)
    public static void docked_isDrawnFromItsCockpitWithTheTerminalBehind(final ClientTestContext ctx) {
        final ServerLevel[] built = new ServerLevel[1];
        final BlockPos[] terminal = new BlockPos[1];
        final PlannedBuild[] plan = new PlannedBuild[1];
        final BlockPos[] console = new BlockPos[1];
        ctx.afterTest(() -> {
            ctx.mc().options.hideGui = false;
            ctx.server().execute(() -> {
                if (built[0] != null) {
                    clear(ctx, built[0]);
                    built[0].players().forEach(player -> player.setGameMode(GameType.CREATIVE));
                }
            });
        });
        ctx.thenServer(0, level -> {
            built[0] = level;
            clear(ctx, level);
            terminal[0] = TestShipyards.build(level, ctx.abs(CORNER));
            // A player in creative, standing in the ship as a player builds and boards it, not a spectator, whose
            // camera the game culls otherwise.
            ctx.serverPlayer().setGameMode(GameType.CREATIVE);
        }).thenWaitUntilServer(level -> level.getBlockEntity(terminal[0]) instanceof ShipyardTerminalBlockEntity dock
                && dock.formed(), ARRIVE, "the dock to form", level -> "not formed")
                .thenServer(0, level -> {
                    final ShipyardTerminalBlockEntity dock = (ShipyardTerminalBlockEntity) level
                            .getBlockEntity(terminal[0]);
                    dock.design(SHIP);
                    ctx.assertTrue(dock.confirm(), "the ship is sent to the dock");
                    plan[0] = dock.build().orElseThrow();
                    for (final PlanSpot spot : plan[0].plan().spots()) {
                        final BlockPos at = plan[0].where(spot);
                        level.setBlockAndUpdate(at, BuiltInRegistries.BLOCK.get(spot.block()).defaultBlockState());
                        if (spot.block().equals(BuiltInRegistries.BLOCK.getKey(ShipKitBlocks.FLIGHT_CONSOLE.get()))
                                && (console[0] == null || at.getX() > console[0].getX())) {
                            console[0] = at;
                        }
                    }
                    ctx.assertTrue(console[0] != null, "the ship has a flight console");
                    // Behind the console, standing in the ship, looking at the nose: the dock's terminal is at the back.
                    final Vec3 eyes = Vec3.atBottomCenterOf(console[0]).add(0, EYE, BEHIND);
                    ctx.serverPlayer().teleportTo(level, eyes.x, eyes.y - ctx.serverPlayer().getEyeHeight(),
                            eyes.z, 180F, DOWN);
                })
                .thenWaitUntil(() -> ctx.mc().level.getBlockState(console[0]).is(ShipKitBlocks.FLIGHT_CONSOLE.get()),
                        ARRIVE, "the ship's blocks on the player's game")
                .then(SETTLE, () -> ctx.mc().options.hideGui = true)
                .then(SETTLE, () -> {
                    final double share = deck(frame(ctx), ctx.mc().getWindow().getGuiScaledWidth());
                    LOGGER.info("[CT] docked: the deck and the hull fill {} of the lower view", share);
                    ctx.assertTrue(share > DRAWN, "from its cockpit, the terminal behind, the docked ship is drawn "
                            + "round the player: " + share + " of the lower view is its light grey deck and hull");
                })
                .thenScreenshot(0, "ship-docked-cockpit");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void canopy_showsTheCabinBehindItsGlassNotTheGroundBeyond(final ClientTestContext ctx) {
        final ServerLevel[] built = new ServerLevel[1];
        final BlockPos[] terminal = new BlockPos[1];
        final BlockPos[] console = new BlockPos[1];
        ctx.afterTest(() -> {
            ctx.mc().options.hideGui = false;
            ctx.server().execute(() -> {
                if (built[0] != null) {
                    clear(ctx, built[0]);
                    built[0].players().forEach(player -> player.setGameMode(GameType.CREATIVE));
                }
            });
        });
        ctx.thenServer(0, level -> {
            built[0] = level;
            clear(ctx, level);
            terminal[0] = TestShipyards.build(level, ctx.abs(CORNER));
            ctx.serverPlayer().setGameMode(GameType.CREATIVE);
            ctx.serverPlayer().getAbilities().flying = true;
            ctx.serverPlayer().onUpdateAbilities();
        }).thenWaitUntilServer(level -> level.getBlockEntity(terminal[0]) instanceof ShipyardTerminalBlockEntity dock
                && dock.formed(), ARRIVE, "the dock to form", level -> "not formed")
                .thenServer(0, level -> {
                    final ShipyardTerminalBlockEntity dock = (ShipyardTerminalBlockEntity) level
                            .getBlockEntity(terminal[0]);
                    dock.design(SHIP);
                    ctx.assertTrue(dock.confirm(), "the ship is sent to the dock");
                    final PlannedBuild plan = dock.build().orElseThrow();
                    for (final PlanSpot spot : plan.plan().spots()) {
                        final BlockPos at = plan.where(spot);
                        level.setBlockAndUpdate(at, BuiltInRegistries.BLOCK.get(spot.block()).defaultBlockState());
                        if (spot.block().equals(BuiltInRegistries.BLOCK.getKey(ShipKitBlocks.FLIGHT_CONSOLE.get()))
                                && (console[0] == null || at.getX() > console[0].getX())) {
                            console[0] = at;
                        }
                    }
                })
                .thenWaitUntil(() -> ctx.mc().level.getBlockState(console[0]).is(ShipKitBlocks.FLIGHT_CONSOLE.get()),
                        ARRIVE, "the ship's blocks on the player's game")
                .then(SETTLE, () -> ctx.mc().options.hideGui = true)
                // Above the ship's back, looking down through its canopy into the cockpit.
                .thenServer(0, level -> ctx.serverPlayer().teleportTo(level, console[0].getX() + CANOPY_FROM.x,
                        console[0].getY() + CANOPY_FROM.y, console[0].getZ() + CANOPY_FROM.z, 0F, 0F))
                .then(SETTLE, () -> ctx.lookAt(Vec3.atLowerCornerOf(console[0]).add(CANOPY_AT)))
                .then(SETTLE, () -> {
                    final int width = ctx.mc().getWindow().getGuiScaledWidth();
                    final int height = ctx.mc().getWindow().getGuiScaledHeight();
                    final double share = cabin(frame(ctx), width, height);
                    LOGGER.info("[CT] docked canopy: the cockpit's walls fill {} of the glass", share);
                    ctx.assertTrue(share > CABIN, "through its canopy the cockpit is seen, its walls, seats and "
                            + "consoles, not the ground beyond the ship: its walls fill " + share + " of the glass");
                })
                .thenScreenshot(0, "ship-docked-canopy");
    }

    /*
     * The share of the canopy's glass, in the middle of a frame {@code width} by {@code height}, that is the cockpit's
     * light walls seen through it; the ground beyond the ship, seen where they should stand, is dark.
     */
    private static double cabin(final int[] frame, final int width, final int height) {
        int light = 0;
        int seen = 0;
        for (int y = (int) (height * GLASS_TOP); y < (int) (height * GLASS_BOTTOM); y++) {
            for (int x = (int) (width * GLASS_LEFT); x < (int) (width * GLASS_RIGHT); x++) {
                final int argb = frame[y * width + x];
                light += Math.min(argb >> 16 & CHANNEL, Math.min(argb >> 8 & CHANNEL, argb & CHANNEL)) > WALLS ? 1 : 0;
                seen++;
            }
        }
        return light / (double) seen;
    }

    /*
     * The share of the lower half of a frame {@code width} wide that is the deck's and the hull's light flat grey;
     * the pit's stone, dirt and grass under an undrawn ship are darker and mottled.
     */
    private static double deck(final int[] frame, final int width) {
        final int height = frame.length / width;
        int grey = 0;
        int seen = 0;
        for (int y = height / 2; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final int argb = frame[y * width + x];
                final int r = argb >> 16 & CHANNEL;
                final int g = argb >> 8 & CHANNEL;
                final int b = argb & CHANNEL;
                grey += Math.min(r, Math.min(g, b)) > LIGHT && Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g,
                        b)) < FLAT ? 1 : 0;
                seen++;
            }
        }
        return grey / (double) seen;
    }

    /* The whole frame, its pixels row by row. */
    private static int[] frame(final ClientTestContext ctx) {
        final int width = ctx.mc().getWindow().getGuiScaledWidth();
        final int height = ctx.mc().getWindow().getGuiScaledHeight();
        final int[] out = new int[width * height];
        try (ClientTestContext.FrameSampler frame = ctx.sampleFrame()) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    out[y * width + x] = frame.pixel(x, y);
                }
            }
        }
        return out;
    }

    private static ResourceLocation part(final String path) {
        return SmallShipParts.id("small_" + path);
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
}
