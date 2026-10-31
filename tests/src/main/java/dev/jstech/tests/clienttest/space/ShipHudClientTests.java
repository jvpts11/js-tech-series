/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.space;

import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.StructurePose;
import dev.jstech.core.moving.StructureSeatEntity;
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.multiblock.plan.PlannedBuild;
import dev.jstech.space.client.ship.ShipHud;
import dev.jstech.space.ship.AirReserveEntity;
import dev.jstech.space.ship.ShipAutopilot;
import dev.jstech.space.ship.ShipDesign;
import dev.jstech.space.ship.ShipFlight;
import dev.jstech.space.ship.ShipFlightModel;
import dev.jstech.space.ship.ShipKitBlocks;
import dev.jstech.space.ship.SmallShipParts;
import dev.jstech.space.shipyard.ShipyardTerminalBlockEntity;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.space.TestShipyards;
import dev.jstech.testkit.client.ClientTestHelper;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * A ship's HUD on a player's game, as the approved flight draws it: projected on the canopy for the pilot at its helm,
 * in green, its speed boxed on the left and its heading's mark across the top; seen from behind the ship it spreads
 * over the screen; up from the helm, it is gone. The autopilot engaged, its mode is lit along the bottom and its
 * symbol stands where its target falls on the screen: ORBIT's caret on the climb ahead, LAND's diamond on its spot.
 */
public final class ShipHudClientTests {

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
    /* How high the ship is taken to fly, in blocks, and how fast, in metres a second. */
    private static final double ALOFT = 60;
    private static final double CRUISE = 30;
    /*
     * The approved screen the HUD is laid out on, and points of it its symbols are drawn on: the top of the speed's
     * box seen from the seat and from behind, and the mark of the nose's heading.
     */
    private static final int SCREEN_WIDTH = 960;
    private static final int SCREEN_HEIGHT = 540;
    private static final int[] SEAT_SPEED = {306, 182};
    private static final int[] BEHIND_SPEED = {106, 272};
    private static final int[] SEAT_HEADING = {480, 54};
    /* Inside the boxes of LAND and ORBIT along the bottom seen from the seat, under their words, clear of the frame. */
    private static final int[] SEAT_LAND = {420, 322};
    private static final int[] SEAT_ORBIT = {470, 322};
    /*
     * Dots of the autopilot's symbols, from their middles, all but one of which are drawn, the screen's pixels not
     * always falling on every one: the climb's caret, a chevron over its line; the landing spot's diamond.
     */
    private static final int[][] CARET = {{-4, -5}, {4, -5}, {-6, -7}, {6, -7}};
    private static final int[][] DIAMOND = {{-4, -5}, {4, -5}, {-4, 5}, {4, 5}};
    /* How far ahead of the ship the spot LAND is sent to stands, in blocks. */
    private static final int LAND_AHEAD = 50;
    /* What reads as the HUD's green: its green high, its blue middling, its red low. */
    private static final int GREEN_AT_LEAST = 200;
    private static final int BLUE_AT_LEAST = 110;
    private static final int RED_AT_MOST = 190;
    private static final int CHANNEL = 0xFF;
    private static final ShipDesign SHIP = new ShipDesign("JS-07", List.of(part("orbiter_cockpit"), part("airlock"),
            part("quarters"), part("lab"), part("reactor_flush")), List.of(part("twin_bells"), part("side_intakes"),
            part("blended_delta"), part("twin_canted_fins"), part("blisters"), part("wheels")));

    private ShipHudClientTests() {
    }

    @ClientTest(timeoutTicks = 2400)
    public static void hud_onTheCanopyAtTheHelmOverTheScreenFromBehindAndGoneUpFromIt(final ClientTestContext ctx) {
        atTheHelm(ctx, new MovingStructure[1])
                .then(0, () -> ctx.mc().options.setCameraType(CameraType.FIRST_PERSON))
                .thenWaitUntil(() -> green(ctx, SEAT_SPEED) && green(ctx, SEAT_HEADING), ARRIVE,
                        "the HUD on the canopy: the speed's box and the heading's mark in green")
                .thenScreenshot(SETTLE, "ship-hud")
                .then(0, () -> ctx.mc().options.setCameraType(CameraType.THIRD_PERSON_BACK))
                .thenWaitUntil(() -> green(ctx, BEHIND_SPEED), ARRIVE,
                        "from behind the ship, the HUD spread over the screen")
                .thenScreenshot(SETTLE, "ship-hud-behind")
                .then(0, () -> ctx.mc().options.setCameraType(CameraType.FIRST_PERSON))
                .thenServer(0, level -> ctx.serverPlayer().stopRiding())
                .thenWaitUntil(() -> !(ctx.player().getVehicle() instanceof StructureSeatEntity), ARRIVE,
                        "the player to get up from the helm on their game")
                .then(SETTLE, () -> ctx.assertTrue(!green(ctx, SEAT_SPEED) && !green(ctx, SEAT_HEADING),
                        "up from the helm, the HUD is gone"));
    }

    @ClientTest(timeoutTicks = 2400)
    public static void hud_theEngagedAutopilotIsLitAndItsSymbolStandsOnItsTarget(final ClientTestContext ctx) {
        final MovingStructure[] ship = new MovingStructure[1];
        atTheHelm(ctx, ship)
                .then(0, () -> ctx.mc().options.setCameraType(CameraType.FIRST_PERSON))
                .thenWaitUntil(() -> green(ctx, SEAT_SPEED), ARRIVE, "the HUD on the canopy")
                .then(0, () -> ctx.assertTrue(!green(ctx, SEAT_ORBIT), "no autopilot engaged, ORBIT is not lit"))
                .thenServer(0, level -> MovingStructures.move(ship[0], ((ShipFlight) ship[0].motion()).engaged(
                        Optional.of(ShipAutopilot.orbit()))))
                .thenWaitUntil(() -> engaged(ctx).equals("orbit"), ARRIVE, "its console to show ORBIT on the"
                        + " player's game")
                // The pilot looks at the climb ahead, where ORBIT's caret stands.
                .thenWaitUntil(() -> look(ctx) && green(ctx, SEAT_ORBIT) && !green(ctx, SEAT_LAND)
                        && symbol(ctx, CARET), ARRIVE, "ORBIT lit along the bottom, LAND not, and the climb's caret"
                        + " on the climb ahead")
                .thenScreenshot(SETTLE, "ship-hud-orbit")
                // LAND onto a spot on the land ahead of it.
                .thenServer(0, level -> {
                    final StructurePose at = ship[0].pose();
                    final BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING,
                            BlockPos.containing(at.x(), at.y(), at.z() - LAND_AHEAD));
                    MovingStructures.move(ship[0], ((ShipFlight) ship[0].motion()).engaged(Optional.of(
                            ShipAutopilot.land(Vec3.atBottomCenterOf(ground)))));
                })
                .thenWaitUntil(() -> engaged(ctx).equals("land"), ARRIVE, "its console to show LAND")
                .thenWaitUntil(() -> look(ctx) && green(ctx, SEAT_LAND) && !green(ctx, SEAT_ORBIT)
                        && symbol(ctx, DIAMOND), ARRIVE, "LAND lit, ORBIT not, and the diamond on the spot it sets"
                        + " down on")
                .thenScreenshot(SETTLE, "ship-hud-land");
    }

    /*
     * The dock built where the test stands, a ship built in it and let go, {@code ship} told it, taken up and flying
     * on at a cruise, the player at its helm.
     */
    private static ClientTestContext atTheHelm(final ClientTestContext ctx, final MovingStructure[] ship) {
        final ServerLevel[] built = new ServerLevel[1];
        final BlockPos[] terminal = new BlockPos[1];
        final PlannedBuild[] plan = new PlannedBuild[1];
        ctx.afterTest(() -> {
            ctx.mc().options.setCameraType(CameraType.FIRST_PERSON);
            ctx.server().execute(() -> {
                ctx.serverPlayer().stopRiding();
                if (ship[0] != null) {
                    MovingStructures.takeAway(ship[0]);
                }
                if (built[0] != null) {
                    clear(ctx, built[0]);
                }
                ctx.serverPlayer().setGameMode(GameType.CREATIVE);
            });
        });
        return ctx.thenServer(0, level -> {
            built[0] = level;
            clear(ctx, level);
            terminal[0] = TestShipyards.build(level, ctx.abs(CORNER));
        }).thenWaitUntilServer(level -> level.getBlockEntity(terminal[0]) instanceof ShipyardTerminalBlockEntity dock
                && dock.formed(), ARRIVE, "the dock to form", level -> "not formed")
                .thenServer(0, level -> {
                    final ShipyardTerminalBlockEntity dock = (ShipyardTerminalBlockEntity) level
                            .getBlockEntity(terminal[0]);
                    dock.design(SHIP);
                    ctx.assertTrue(dock.confirm(), "the ship is sent to the dock");
                    plan[0] = dock.build().orElseThrow();
                    for (final PlanSpot spot : plan[0].plan().spots()) {
                        level.setBlockAndUpdate(plan[0].where(spot), BuiltInRegistries.BLOCK.get(spot.block())
                                .defaultBlockState());
                    }
                })
                .thenServer(SETTLE, level -> ship[0] = ((ShipyardTerminalBlockEntity) level.getBlockEntity(
                        terminal[0])).release(null).orElseThrow())
                .thenWaitUntilServer(level -> ship[0].ready(), ARRIVE, "its blocks to come into the structure",
                        level -> "arriving")
                .thenServer(0, level -> {
                    final BlockPos seat = BlockPos.containing(MovingStructures.toYard(ship[0], Vec3.atCenterOf(
                            plan[0].where(first(plan[0], ShipKitBlocks.PILOT_SEAT.getId())))));
                    // Taken up and flying on at a cruise, the player at its helm.
                    ship[0].teleport(ship[0].pose().movedTo(ship[0].pose().x(), ship[0].pose().y() + ALOFT,
                            ship[0].pose().z()));
                    MovingStructures.move(ship[0], ((ShipFlight) ship[0].motion()).with(new ShipFlightModel.State(
                            CRUISE, 0, CRUISE / 40, 1, 0, 0)));
                    ctx.serverPlayer().setGameMode(GameType.SURVIVAL);
                    ctx.assertTrue(MovingStructures.sit(ctx.serverPlayer(), seat), "the player sits at its helm");
                })
                .thenWaitUntil(() -> ctx.player().getVehicle() instanceof StructureSeatEntity, ARRIVE,
                        "the player to sit at its helm on their game");
    }

    /* The mode the console of the ship whose helm the player sits at shows engaged, by its name; empty with none. */
    private static String engaged(final ClientTestContext ctx) {
        final AirReserveEntity console = ShipHud.atTheHelm(ctx.mc());
        return console == null ? "" : console.flight().pilot().mode();
    }

    /* Where the engaged autopilot's symbol stands, as the console the player sits at shows it; none away from it. */
    private static @Nullable Vec3 target(final ClientTestContext ctx) {
        final AirReserveEntity console = ShipHud.atTheHelm(ctx.mc());
        return console == null ? null : console.flight().pilot().target();
    }

    /* Turns the player's look to the autopilot's target, from their eyes; whether there is one. */
    private static boolean look(final ClientTestContext ctx) {
        final Vec3 target = target(ctx);
        if (target == null) {
            return false;
        }
        final Vec3 way = target.subtract(ctx.player().getEyePosition());
        final float yaw = (float) Math.toDegrees(Math.atan2(-way.x, way.z));
        final float pitch = (float) -Math.toDegrees(Math.atan2(way.y, Math.hypot(way.x, way.z)));
        ctx.player().setYRot(yaw);
        ctx.player().yRotO = yaw;
        ctx.player().setXRot(pitch);
        ctx.player().xRotO = pitch;
        return true;
    }

    /*
     * Whether the engaged mode's symbol is drawn where its target falls on the screen: the dots {@code dots} of it,
     * from its middle, in the approved screen's points, in the HUD's green, each within a point of where it falls.
     */
    private static boolean symbol(final ClientTestContext ctx, final int[][] dots) {
        final Vec3 target = target(ctx);
        final Vec2 at = target == null ? null : ShipHud.onScreen(target);
        if (at == null) {
            return false;
        }
        final int width = ctx.mc().getWindow().getGuiScaledWidth();
        final int height = ctx.mc().getWindow().getGuiScaledHeight();
        final double scale = Math.min(width / (double) SCREEN_WIDTH, height / (double) SCREEN_HEIGHT);
        final int tx = (int) ((at.x - (width - SCREEN_WIDTH * scale) / 2) / scale);
        final int ty = (int) ((at.y - (height - SCREEN_HEIGHT * scale) / 2) / scale);
        int lit = 0;
        for (final int[] dot : dots) {
            boolean near = false;
            for (int dx = -1; dx <= 1 && !near; dx++) {
                for (int dy = -1; dy <= 1 && !near; dy++) {
                    near = green(ctx, new int[] {tx + dot[0] + dx, ty + dot[1] + dy});
                }
            }
            lit += near ? 1 : 0;
        }
        return lit >= dots.length - 1;
    }

    /* Whether the screen at the approved screen's point {@code at}, as the HUD is scaled, is the HUD's green. */
    private static boolean green(final ClientTestContext ctx, final int[] at) {
        final int width = ctx.mc().getWindow().getGuiScaledWidth();
        final int height = ctx.mc().getWindow().getGuiScaledHeight();
        final double scale = Math.min(width / (double) SCREEN_WIDTH, height / (double) SCREEN_HEIGHT);
        final int x = (int) Math.round((width - SCREEN_WIDTH * scale) / 2 + at[0] * scale);
        final int y = (int) Math.round((height - SCREEN_HEIGHT * scale) / 2 + at[1] * scale);
        if (x < 0 || y < 0 || x >= width || y >= height) {
            // Off the screen, as a symbol whose target the last frame drew behind the player's look is.
            return false;
        }
        try (ClientTestHelper.FrameSampler frame = ctx.sampleFrame()) {
            final int argb = frame.pixel(x, y);
            final int red = argb >> 16 & CHANNEL;
            final int green = argb >> 8 & CHANNEL;
            final int blue = argb & CHANNEL;
            return green >= GREEN_AT_LEAST && blue >= BLUE_AT_LEAST && red <= RED_AT_MOST;
        }
    }

    /* The first place of {@code plan} that wants {@code block}. */
    private static PlanSpot first(final PlannedBuild plan, final ResourceLocation block) {
        return plan.plan().spots().stream().filter(each -> each.block().equals(block)).findFirst().orElseThrow();
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
