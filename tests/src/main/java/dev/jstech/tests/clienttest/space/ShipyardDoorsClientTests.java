/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.space;

import com.mojang.logging.LogUtils;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.multiblock.Orientation;
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.multiblock.plan.PlannedBuild;
import dev.jstech.space.ship.ShipDesign;
import dev.jstech.space.ship.SmallShipParts;
import dev.jstech.space.shipyard.ShipyardDoors;
import dev.jstech.space.shipyard.ShipyardStructure;
import dev.jstech.space.shipyard.ShipyardTerminalBlockEntity;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.space.TestShipyards;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

/**
 * The Small Shipyard opening round a finished ship and closing round it again on a player's game, as the approved
 * storyboard draws it: closed, its crane's yellow bridge over the ship and its cradle's jaws standing up by the hull;
 * open, the bridge at the back of the bay, the floor's hazard band level with the ground and the jaws lying flat in
 * it; the floor rising and going down again drawn on the way. The camera looks straight at each place it checks, so
 * what it sees there is at the middle of the picture.
 */
public final class ShipyardDoorsClientTests {

    /* The dock dug into the land where the test stands, as the shipyard's client test builds it. */
    private static final BlockPos CORNER = new BlockPos(-11, -1, 6);
    /* The wide camera, in the dock's own places: high off its front left, as the storyboard's. */
    private static final Vec3 WIDE = new Vec3(-15, 17, -26);
    private static final Vec3 BAY = new Vec3(0, 1, 0);
    /*
     * The crane's bridge over the bay, and parked at the back of the bay, at its middle, its top; and high over each,
     * looking straight down on it, the pit or the ship under it.
     */
    private static final Vec3 BRIDGE_OVER = new Vec3(-0.5, 12, 1.5);
    private static final Vec3 BRIDGE_PARKED = new Vec3(-0.5, 12, 11.5);
    private static final Vec3 OVER_THE_BAY = new Vec3(-0.5, 28, 1.5);
    private static final Vec3 OVER_THE_BACK = new Vec3(-0.5, 28, 11.5);
    /* The floor's hazard band along the bay's open front, on its top. */
    private static final Vec3 BAND = new Vec3(-3, 0.02, -11.75);
    /* Low by the front station's east jaw, as the storyboard's close-up; near the jaw's top standing up. */
    private static final Vec3 LOW = new Vec3(5.5, 0.9, -9.5);
    private static final Vec3 JAW = new Vec3(2.25, 1.4, -6.5);
    private static final int X0 = -13;
    private static final int X1 = 13;
    private static final int Z0 = 4;
    private static final int Z1 = 34;
    private static final int Y_BOTTOM = -6;
    private static final int Y_GRASS = 1;
    private static final int Y_TOP = 20;
    private static final int ARRIVE = 600;
    private static final int SETTLE = 20;
    /*
     * How far round the middle of the picture is read, in pixels; how much of it yellow is the bridge or a jaw, the
     * band, striped yellow and black, and nothing yellow.
     */
    private static final int ROUND = 3;
    private static final double YELLOW = 0.3;
    private static final double STRIPED = 0.15;
    private static final double NONE = 0.05;
    /* How far the floor has gone when it is pictured moving and its cradle looked at, of its way. */
    private static final double SOME_WAY = 0.3;
    /* How long a picture's name taken from what it checks is, at the most. */
    private static final int PICTURE_NAME = 48;
    private static final int CHANNEL = 0xFF;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ShipDesign SHIP = new ShipDesign("JS-02", List.of(part("orbiter_cockpit"), part("airlock"),
            part("quarters"), part("workshop"), part("reactor_flush")), List.of(part("twin_bells"),
            part("side_intakes"), part("blended_delta"), part("twin_canted_fins"), part("blisters"), part("wheels")));

    private ShipyardDoorsClientTests() {
    }

    @ClientTest(timeoutTicks = 4800)
    public static void doors_openAndCloseAsTheApprovedStoryboard(final ClientTestContext ctx) {
        final ServerLevel[] built = new ServerLevel[1];
        final BlockPos[] terminal = new BlockPos[1];
        final MovingStructure[] ship = new MovingStructure[1];
        ctx.afterTest(() -> {
            ctx.mc().options.hideGui = false;
            ctx.server().execute(() -> {
                // Taken back, it is the dock's blocks again; still flying, it is taken away.
                if (ship[0] != null && built[0] != null && MovingStructures.byId(built[0], ship[0].id()) != null) {
                    MovingStructures.takeAway(ship[0]);
                }
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
            ctx.serverPlayer().setGameMode(GameType.SPECTATOR);
        }).thenWaitUntilServer(level -> dock(level, terminal[0]) != null && dock(level, terminal[0]).formed(), ARRIVE,
                "the dock to form", level -> "not formed")
                .thenServer(0, level -> {
                    final ShipyardTerminalBlockEntity dock = dock(level, terminal[0]);
                    dock.design(SHIP);
                    ctx.assertTrue(dock.confirm(), "the ship is sent to the dock");
                    final PlannedBuild plan = dock.build().orElseThrow();
                    for (final PlanSpot spot : plan.plan().spots()) {
                        level.setBlockAndUpdate(plan.where(spot), BuiltInRegistries.BLOCK.get(spot.block())
                                .defaultBlockState());
                    }
                })
                .thenWaitUntilServer(level -> dock(level, terminal[0]).built(), ARRIVE, "the ship whole in the dock",
                        level -> "building")
                .then(SETTLE, () -> ctx.mc().options.hideGui = true);
        // Closed: the bridge over the ship, the floor down in the pit, the jaws standing up by the hull.
        look(ctx, WIDE, BAY);
        ctx.thenScreenshot(SETTLE, "shipyard-doors-closed");
        seen(ctx, OVER_THE_BAY, BRIDGE_OVER, YELLOW, "closed, the crane's yellow bridge stands over the ship");
        seen(ctx, OVER_THE_BACK, BRIDGE_PARKED, 0, "closed, nothing yellow at the back of the bay over it");
        seen(ctx, WIDE, BAND, 0, "closed, the floor's band is down in the pit, not at the ground");
        seen(ctx, LOW, JAW, YELLOW, "closed, the cradle's jaw stands up by the hull");
        // Opening: the floor rising to the ship's gear, the cradle still holding the ship, never in the air.
        look(ctx, WIDE, BAY);
        ctx.thenServer(0, level -> ctx.assertTrue(dock(level, terminal[0]).launch(null, why -> { }), "the dock opens"))
                .thenWaitUntil(() -> client(ctx, terminal[0]).doors() == ShipyardDoors.State.FLOOR_UP
                        && client(ctx, terminal[0]).doorsProgress(now(ctx)) > SOME_WAY, ARRIVE, "the floor rising")
                .thenScreenshot(0, "shipyard-doors-rising");
        seen(ctx, LOW, JAW, YELLOW, "rising, the cradle's jaw still holds the ship");
        ctx.thenWaitUntilServer(level -> {
                    ship[0] = shipIn(level, terminal[0]);
                    return ship[0] != null && ship[0].ready() && dock(level, terminal[0]).open();
                }, ARRIVE, "the dock open, the ship let go", level -> String.valueOf(dock(level, terminal[0]).doors()))
                .thenWaitUntil(() -> client(ctx, terminal[0]).doors() == ShipyardDoors.State.OPEN, ARRIVE,
                        "the dock open on the player's game");
        // Open: the bridge at the back, the band level with the ground, the jaws lying flat.
        look(ctx, WIDE, BAY);
        ctx.thenScreenshot(SETTLE, "shipyard-doors-open");
        seen(ctx, OVER_THE_BAY, BRIDGE_OVER, 0, "open, nothing yellow over the ship where the bridge stood");
        seen(ctx, OVER_THE_BACK, BRIDGE_PARKED, YELLOW, "open, the crane's yellow bridge stands at the back of the"
                + " bay");
        seen(ctx, WIDE, BAND, STRIPED, "open, the floor's hazard band is level with the ground");
        seen(ctx, LOW, JAW, 0, "open, the cradle's jaw lies flat in the floor");
        look(ctx, LOW, JAW);
        ctx.thenScreenshot(SETTLE, "shipyard-doors-jaws");
        // Closing round it again: the cradle holding the ship first, then the floor going down from under its gear.
        look(ctx, WIDE, BAY);
        ctx.thenServer(0, level -> ctx.assertTrue(dock(level, terminal[0]).seat(ship[0]), "the ship is taken back"))
                .thenWaitUntil(() -> client(ctx, terminal[0]).doors() == ShipyardDoors.State.FLOOR_DOWN
                        && client(ctx, terminal[0]).doorsProgress(now(ctx)) > SOME_WAY, ARRIVE, "the floor going down")
                .thenScreenshot(0, "shipyard-doors-closing");
        seen(ctx, LOW, JAW, YELLOW, "going down, the cradle's jaw holds the ship");
        ctx.thenWaitUntil(() -> client(ctx, terminal[0]).doors() == ShipyardDoors.State.CLOSED, ARRIVE,
                        "the dock closed round the ship again");
        seen(ctx, OVER_THE_BAY, BRIDGE_OVER, YELLOW, "closed again, the bridge over the ship");
        seen(ctx, LOW, JAW, YELLOW, "closed again, the jaw stands up by the hull");
    }

    /* The player, a spectator, floated with their eyes at the dock's place {@code eyes}, looking at {@code at}. */
    private static void look(final ClientTestContext ctx, final Vec3 eyes, final Vec3 at) {
        final Vec3 from = place(ctx, eyes);
        final Vec3 way = place(ctx, at).subtract(from);
        final float yaw = (float) Math.toDegrees(Math.atan2(-way.x, way.z));
        final float pitch = (float) -Math.toDegrees(Math.atan2(way.y, Math.hypot(way.x, way.z)));
        ctx.thenServer(0, level -> ctx.serverPlayer().teleportTo(level, from.x, from.y - ctx.serverPlayer()
                .getEyeHeight(), from.z, yaw, pitch))
                .thenWaitUntil(() -> ctx.player().getEyePosition().distanceTo(from) < 0.1
                        && Math.abs(ctx.player().getXRot() - pitch) < 0.5, ARRIVE, "the player to look at the dock");
    }

    /*
     * Looking from {@code eyes} straight at {@code at}, whether the middle of the picture is yellow, as the dock's
     * bridge, band and jaws are painted: at least {@code least} of it, or, for none, next to nothing.
     */
    private static void seen(final ClientTestContext ctx, final Vec3 eyes, final Vec3 at, final double least,
                             final String what) {
        look(ctx, eyes, at);
        final double[] share = new double[1];
        ctx.then(SETTLE, () -> {
            share[0] = yellowAtTheMiddle(ctx);
            LOGGER.info("[CT] shipyard doors: {} of the middle is yellow; {}", share[0], what);
        }).thenScreenshot(0, picture(what))
                .then(0, () -> ctx.assertTrue(least > 0 ? share[0] >= least : share[0] <= NONE, what + ": "
                        + share[0] + " of the middle is yellow"));
    }

    /* The name of the picture taken of what {@code what} checks: its words, joined. */
    private static String picture(final String what) {
        final String name = "shipyard-doors-" + what.toLowerCase(Locale.ROOT).replaceAll("[^a-z]+", "-");
        return name.substring(0, Math.min(PICTURE_NAME, name.length()));
    }

    /* The share of the pixels round the middle of the picture that are the dock's yellow. */
    private static double yellowAtTheMiddle(final ClientTestContext ctx) {
        final int width = ctx.mc().getWindow().getGuiScaledWidth();
        final int height = ctx.mc().getWindow().getGuiScaledHeight();
        int yellow = 0;
        int seen = 0;
        try (ClientTestContext.FrameSampler frame = ctx.sampleFrame()) {
            for (int y = height / 2 - ROUND; y <= height / 2 + ROUND; y++) {
                for (int x = width / 2 - ROUND; x <= width / 2 + ROUND; x++) {
                    final int argb = frame.pixel(x, y);
                    final int r = argb >> 16 & CHANNEL;
                    final int g = argb >> 8 & CHANNEL;
                    final int b = argb & CHANNEL;
                    yellow += r > 120 && g > 90 && b * 2 < g && r - b > 70 ? 1 : 0;
                    seen++;
                }
            }
        }
        return yellow / (double) seen;
    }

    /* The game's time on the player's game, to the tick. */
    private static double now(final ClientTestContext ctx) {
        return ctx.mc().level.getGameTime();
    }

    /* The ship flying in the dock's bay, or none. */
    private static MovingStructure shipIn(final ServerLevel level, final BlockPos terminal) {
        final AABB bay = ShipyardStructure.bay(terminal, Orientation.NORTH).inflate(2);
        return MovingStructures.near(level, bay).stream().filter(MovingStructure.class::isInstance)
                .map(MovingStructure.class::cast).filter(structure -> structure.blocks() > 1).findFirst()
                .orElse(null);
    }

    private static ShipyardTerminalBlockEntity dock(final ServerLevel level, final BlockPos terminal) {
        return level.getBlockEntity(terminal) instanceof ShipyardTerminalBlockEntity dock ? dock : null;
    }

    /* The dock's terminal on the player's game. */
    private static ShipyardTerminalBlockEntity client(final ClientTestContext ctx, final BlockPos terminal) {
        return (ShipyardTerminalBlockEntity) ctx.mc().level.getBlockEntity(terminal);
    }

    /* The world's point of the dock's place {@code at}: across from the bay's middle, up from the ground, along. */
    private static Vec3 place(final ClientTestContext ctx, final Vec3 at) {
        return Vec3.atLowerCornerOf(ctx.abs(CORNER.offset(11, 3, 13))).add(at);
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
