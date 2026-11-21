/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.space;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.InputConstants;
import dev.jstech.core.client.input.KeyActionsClient;
import dev.jstech.core.client.mcp.PlayRiding;
import dev.jstech.core.input.CoreKeys;
import dev.jstech.core.input.KeyAction;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.StructureControls;
import dev.jstech.core.moving.StructurePose;
import dev.jstech.core.moving.StructureSeatEntity;
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.multiblock.plan.PlannedBuild;
import dev.jstech.space.JsSpace;
import dev.jstech.space.client.ship.ShipHud;
import dev.jstech.space.landing.ShipLiftOffs;
import dev.jstech.space.ship.AirReserveEntity;
import dev.jstech.space.ship.ShipAutopilot;
import dev.jstech.space.ship.ShipBoost;
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
import net.minecraft.client.KeyMapping;
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
import org.lwjgl.glfw.GLFW;

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
    /* Round TURN SLOW under the boost's bar seen from the seat, clear of the bar: its corners. */
    private static final int[] SEAT_TURN_SLOW = {284, 220, 334, 231};
    /* Inside the boxes of LAND and ORBIT along the bottom seen from the seat, under their words, clear of the frame. */
    private static final int[] SEAT_LAND = {420, 322};
    private static final int[] SEAT_ORBIT = {470, 322};
    /*
     * Dots of the autopilot's symbols, from their middles, all but one of which are drawn, the screen's pixels not
     * always falling on every one: the climb's caret, a chevron over its line; the landing spot's diamond.
     */
    private static final int[][] CARET = {{-4, -5}, {4, -5}, {-6, -7}, {6, -7}};
    private static final int[][] DIAMOND = {{-4, -5}, {4, -5}, {-4, 5}, {4, 5}};
    /* Dots of the home mark, from its middle: its roof's chevron over its square. */
    private static final int[][] HOUSE = {{-2, -4}, {2, -4}, {-3, 2}, {4, 2}};
    /*
     * Round the way home's line at the top right seen from the seat, clear of the heading tape and the ladder: its
     * corners; how many font pixels tall a capital letter's stem is in the game's font.
     */
    private static final int[] SEAT_HOME_LINE = {545, 76, 680, 92};
    private static final int LETTER_TALL = 7;
    /* What a screen pixel worked out from the HUD's scale is let off by, so a whole one is not read as the last. */
    private static final double PIXEL_SLACK = 1e-6;
    /* How far ahead of the ship the spot LAND is sent to stands, in blocks. */
    private static final int LAND_AHEAD = 50;
    /* How far ahead of the ship, and to its right, the place the pilot took off from lies, in blocks. */
    private static final double HOME_AHEAD = 300;
    private static final double HOME_RIGHT = 60;
    /* What reads as the HUD's green: its green high, its blue middling, its red low. */
    private static final int GREEN_AT_LEAST = 200;
    private static final int BLUE_AT_LEAST = 110;
    private static final int RED_AT_MOST = 190;
    /* What reads as the HUD's amber: its red high, its green middling, its blue low; how much of it makes words. */
    private static final int AMBER_RED_AT_LEAST = 210;
    private static final int AMBER_GREEN_AT_LEAST = 150;
    private static final int AMBER_GREEN_AT_MOST = 240;
    private static final int AMBER_BLUE_AT_MOST = 150;
    private static final int LEAST_AMBER = 6;
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

    /*
     * At the helm left Ctrl, the game's sprint key too, boosts the ship, and C takes it down: Ctrl held, it boosts and
     * the helm holds nothing down; C held, the helm holds it down and nothing boosts.
     */
    @ClientTest(timeoutTicks = 2400)
    public static void keys_atTheHelmCtrlBoostsTheShipAndCTakesItDown(final ClientTestContext ctx) {
        final MovingStructure[] ship = new MovingStructure[1];
        final InputConstants.Key ctrl = InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_LEFT_CONTROL);
        final InputConstants.Key c = InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_C);
        final boolean[] boosted = new boolean[1];
        final float[] down = new float[1];
        atTheHelm(ctx, ship)
                .then(0, () -> {
                    ctx.assertTrue(onItsDefault(ctx, ShipBoost.KEY).equals(ctrl), "the boost starts on left Ctrl");
                    ctx.assertTrue(onItsDefault(ctx, CoreKeys.HELM_DOWN).equals(c), "going down at a helm on C");
                    KeyMapping.set(ctrl, true);
                })
                .thenServer(SETTLE, level -> {
                    boosted[0] = ShipBoost.held(ship[0], level.getGameTime());
                    down[0] = ship[0].controls().value(StructureControls.ControlAxis.DOWN, level.getGameTime());
                })
                .then(0, () -> {
                    KeyMapping.set(ctrl, false);
                    ctx.assertTrue(boosted[0] && down[0] == 0, "at the helm, left Ctrl held boosts the ship and"
                            + " takes it down not at all: boosting " + boosted[0] + ", down " + down[0]);
                    KeyMapping.set(c, true);
                })
                .thenServer(SETTLE, level -> {
                    boosted[0] = ShipBoost.held(ship[0], level.getGameTime());
                    down[0] = ship[0].controls().value(StructureControls.ControlAxis.DOWN, level.getGameTime());
                })
                .then(0, () -> {
                    KeyMapping.set(c, false);
                    ctx.assertTrue(!boosted[0] && down[0] > 0, "C held takes it down and boosts it not: boosting "
                            + boosted[0] + ", down " + down[0]);
                });
    }

    /*
     * Boosted at the helm, left Ctrl held, the boost's bar lights amber with TURN SLOW under it, as the approved mock
     * draws it; let go, TURN SLOW goes.
     */
    @ClientTest(timeoutTicks = 2400)
    public static void hud_boostedItsBarLightsAmberWithTurnSlowUnderIt(final ClientTestContext ctx) {
        final InputConstants.Key ctrl = InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_LEFT_CONTROL);
        ctx.afterTest(() -> KeyMapping.set(ctrl, false));
        atTheHelm(ctx, new MovingStructure[1])
                .then(0, () -> {
                    ctx.mc().options.setCameraType(CameraType.FIRST_PERSON);
                    ctx.assertTrue(onItsDefault(ctx, ShipBoost.KEY).equals(ctrl), "the boost starts on left Ctrl");
                    KeyMapping.set(ctrl, true);
                })
                .thenWaitUntil(() -> boosting(ctx) && amber(ctx, SEAT_TURN_SLOW) >= LEAST_AMBER, ARRIVE,
                        "boosted, TURN SLOW in amber under the boost's bar")
                .thenScreenshot(SETTLE, "ship-hud-boost")
                .then(0, () -> KeyMapping.set(ctrl, false))
                .thenWaitUntil(() -> !boosting(ctx) && amber(ctx, SEAT_TURN_SLOW) == 0, ARRIVE,
                        "let go, TURN SLOW goes");
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
     * What the HUD shows is told to an agent too: at the helm, what the player rides carries the ship's flight under
     * J's Space's readings, its name, its throttle and the autopilot engaged, ORBIT once it is.
     */
    @ClientTest(timeoutTicks = 2400)
    public static void readings_anAgentAtTheHelmReadsTheFlightTheHudShows(final ClientTestContext ctx) {
        final MovingStructure[] ship = new MovingStructure[1];
        atTheHelm(ctx, ship)
                .thenServer(0, level -> MovingStructures.move(ship[0], ((ShipFlight) ship[0].motion()).engaged(
                        Optional.of(ShipAutopilot.orbit()))))
                .thenWaitUntil(() -> engaged(ctx).equals("orbit"), ARRIVE, "its console to show ORBIT on the"
                        + " player's game")
                .then(0, () -> {
                    final JsonObject riding = PlayRiding.riding(ctx.player()).orElseThrow();
                    final JsonObject flight = riding.has("readings") ? riding.getAsJsonObject("readings")
                            .getAsJsonObject(JsSpace.MODID).getAsJsonObject("ship") : new JsonObject();
                    ctx.assertTrue(flight.has("name") && flight.has("throttle_percent") && flight.has("speed_mps"),
                            "the pilot's agent reads the ship's flight: " + riding);
                    ctx.assertTrue("orbit".equals(flight.getAsJsonObject("autopilot").get("mode").getAsString()),
                            "and ORBIT engaged: " + flight);
                });
    }

    /*
     * The place the pilot last took off from in the world lying ahead of the ship, a little to its right, as the
     * approved mock of the way home draws it: the place's distance and bearing at the top right, and the home mark on
     * the heading tape and over the place itself, where the pilot looks. Its line is in whole letters: laid out on a
     * screen larger than the window, its rows fell between the screen's pixels and were lost.
     */
    @ClientTest(timeoutTicks = 2400)
    public static void hud_theWayHomeAtTheTopRightWithItsMarkOverThePlace(final ClientTestContext ctx) {
        final MovingStructure[] ship = new MovingStructure[1];
        final Vec3[] home = new Vec3[1];
        atTheHelm(ctx, ship)
                .then(0, () -> ctx.mc().options.setCameraType(CameraType.FIRST_PERSON))
                .thenServer(0, level -> {
                    final StructurePose at = ship[0].pose();
                    final double[] nose = ((ShipFlight) ship[0].motion()).noseIn(ship[0].frame());
                    final BlockPos place = BlockPos.containing(at.x() + nose[0] * HOME_AHEAD - nose[2] * HOME_RIGHT,
                            ctx.abs(new BlockPos(0, Y_GRASS + 1, 0)).getY(), at.z() + nose[2] * HOME_AHEAD
                            + nose[0] * HOME_RIGHT);
                    ShipLiftOffs.note(ctx.serverPlayer(), level, place);
                    home[0] = Vec3.atBottomCenterOf(place);
                })
                .thenWaitUntil(() -> look(ctx, home[0]) && symbol(ctx, home[0], HOUSE), ARRIVE,
                        "the home mark over the place the pilot took off from, where they look")
                .thenScreenshot(SETTLE, "ship-hud-home")
                .then(0, () -> ctx.assertTrue(tallestStroke(ctx, SEAT_HOME_LINE) >= LETTER_TALL, "the way home's "
                        + "line in whole letters, its stems as tall as the font draws them: "
                        + tallestStroke(ctx, SEAT_HOME_LINE) + " screen pixels"));
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
                            CRUISE, 0, CRUISE / 40, 0, 0)));
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

    /*
     * Puts {@code action}'s key on the key it starts on for the test, whatever a run before left it on in the game's
     * settings, and back after; that key.
     */
    private static InputConstants.Key onItsDefault(final ClientTestContext ctx, final KeyAction action) {
        final KeyMapping mapping = KeyActionsClient.mapping(action);
        final InputConstants.Key was = mapping.getKey();
        ctx.afterTest(() -> {
            mapping.setKey(was);
            KeyMapping.resetMapping();
        });
        mapping.setKey(mapping.getDefaultKey());
        KeyMapping.resetMapping();
        return mapping.getDefaultKey();
    }

    /* Whether the console of the ship whose helm the player sits at shows it boosting. */
    private static boolean boosting(final ClientTestContext ctx) {
        final AirReserveEntity console = ShipHud.atTheHelm(ctx.mc());
        return console != null && console.flight().boosting();
    }

    /* Where the engaged autopilot's symbol stands, as the console the player sits at shows it; none away from it. */
    private static @Nullable Vec3 target(final ClientTestContext ctx) {
        final AirReserveEntity console = ShipHud.atTheHelm(ctx.mc());
        return console == null ? null : console.flight().pilot().target();
    }

    /* Turns the player's look to the autopilot's target, from their eyes; whether there is one. */
    private static boolean look(final ClientTestContext ctx) {
        return look(ctx, target(ctx));
    }

    /* Turns the player's look to {@code target}, from their eyes; whether there is one. */
    private static boolean look(final ClientTestContext ctx, final @Nullable Vec3 target) {
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

    /* Whether the engaged mode's symbol is drawn where its target falls on the screen, as below. */
    private static boolean symbol(final ClientTestContext ctx, final int[][] dots) {
        return symbol(ctx, target(ctx), dots);
    }

    /*
     * Whether a symbol is drawn where {@code target} falls on the screen: the dots {@code dots} of it, from its
     * middle, in the approved screen's points, in the HUD's green, all but one each within a point of where it falls.
     */
    private static boolean symbol(final ClientTestContext ctx, final @Nullable Vec3 target, final int[][] dots) {
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

    /*
     * Whether the screen at the approved screen's point {@code at}, as the HUD is scaled, is the HUD's green: read at
     * the screen's own pixel the point is drawn on, as a point of the HUD may be finer than the GUI's.
     */
    private static boolean green(final ClientTestContext ctx, final int[] at) {
        final int[] pixel = pixelOf(ctx, at[0], at[1]);
        if (pixel[0] < 0 || pixel[1] < 0 || pixel[0] >= ctx.mc().getWindow().getWidth()
                || pixel[1] >= ctx.mc().getWindow().getHeight()) {
            // Off the screen, as a symbol whose target the last frame drew behind the player's look is.
            return false;
        }
        try (ClientTestHelper.FrameSampler frame = ctx.sampleFrame()) {
            return hudGreen(frame.screenPixel(pixel[0], pixel[1]));
        }
    }

    /* The screen's own pixel the approved screen's point ({@code x}, {@code y}) is drawn on, as the HUD is scaled. */
    private static int[] pixelOf(final ClientTestContext ctx, final double x, final double y) {
        final double guiScale = ctx.mc().getWindow().getGuiScale();
        final int width = ctx.mc().getWindow().getGuiScaledWidth();
        final int height = ctx.mc().getWindow().getGuiScaledHeight();
        final double scale = Math.min(width / (double) SCREEN_WIDTH, height / (double) SCREEN_HEIGHT);
        return new int[] {(int) Math.floor(((width - SCREEN_WIDTH * scale) / 2 + x * scale) * guiScale + PIXEL_SLACK),
                (int) Math.floor(((height - SCREEN_HEIGHT * scale) / 2 + y * scale) * guiScale + PIXEL_SLACK)};
    }

    /*
     * The tallest run of the HUD's green down a column of the screen's own pixels in the approved screen's box
     * {@code box}, its corners, as the HUD is scaled: a letter's stem, as tall as its rows drawn whole make it.
     */
    private static int tallestStroke(final ClientTestContext ctx, final int[] box) {
        final int[] from = pixelOf(ctx, box[0], box[1]);
        final int[] to = pixelOf(ctx, box[2], box[3]);
        int tallest = 0;
        try (ClientTestHelper.FrameSampler frame = ctx.sampleFrame()) {
            for (int x = from[0]; x <= to[0]; x++) {
                int run = 0;
                for (int y = from[1]; y <= to[1]; y++) {
                    run = hudGreen(frame.screenPixel(x, y)) ? run + 1 : 0;
                    tallest = Math.max(tallest, run);
                }
            }
        }
        return tallest;
    }

    /* Whether the colour {@code argb} is the HUD's green. */
    private static boolean hudGreen(final int argb) {
        final int red = argb >> 16 & CHANNEL;
        final int green = argb >> 8 & CHANNEL;
        final int blue = argb & CHANNEL;
        return green >= GREEN_AT_LEAST && blue >= BLUE_AT_LEAST && red <= RED_AT_MOST;
    }

    /*
     * How many of the screen's pixels in the approved screen's box {@code box}, its corners, as the HUD is scaled, are
     * the HUD's amber.
     */
    private static int amber(final ClientTestContext ctx, final int[] box) {
        final int width = ctx.mc().getWindow().getGuiScaledWidth();
        final int height = ctx.mc().getWindow().getGuiScaledHeight();
        final double scale = Math.min(width / (double) SCREEN_WIDTH, height / (double) SCREEN_HEIGHT);
        final double left = (width - SCREEN_WIDTH * scale) / 2;
        final double top = (height - SCREEN_HEIGHT * scale) / 2;
        final int x0 = Math.max(0, (int) Math.floor(left + box[0] * scale));
        final int y0 = Math.max(0, (int) Math.floor(top + box[1] * scale));
        final int x1 = Math.min(width - 1, (int) Math.ceil(left + box[2] * scale));
        final int y1 = Math.min(height - 1, (int) Math.ceil(top + box[3] * scale));
        int lit = 0;
        try (ClientTestHelper.FrameSampler frame = ctx.sampleFrame()) {
            for (int x = x0; x <= x1; x++) {
                for (int y = y0; y <= y1; y++) {
                    final int argb = frame.pixel(x, y);
                    final int red = argb >> 16 & CHANNEL;
                    final int green = argb >> 8 & CHANNEL;
                    final int blue = argb & CHANNEL;
                    lit += red >= AMBER_RED_AT_LEAST && green >= AMBER_GREEN_AT_LEAST && green <= AMBER_GREEN_AT_MOST
                            && blue <= AMBER_BLUE_AT_MOST ? 1 : 0;
                }
            }
        }
        return lit;
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
