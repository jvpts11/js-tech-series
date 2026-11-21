/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.space;

import dev.jstech.core.client.dimension.TravelScreens;
import dev.jstech.core.client.input.KeyActionsClient;
import dev.jstech.core.client.moving.ClientMovingStructures;
import dev.jstech.core.client.moving.ClientStructure;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.StructureFrame;
import dev.jstech.core.moving.StructurePose;
import dev.jstech.core.moving.StructureSeatEntity;
import dev.jstech.core.moving.TowardMotion;
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.multiblock.plan.PlannedBuild;
import dev.jstech.core.room.SealedRooms;
import dev.jstech.space.api.SpaceWorlds;
import dev.jstech.space.client.rocket.FlightComputerFace;
import dev.jstech.space.client.ship.ShipConsoleFace;
import dev.jstech.space.client.ship.ShipConsoleScreen;
import dev.jstech.space.client.ship.ShipEngineSounds;
import dev.jstech.space.client.ship.ShipHud;
import dev.jstech.space.client.ship.ShipPulseClient;
import dev.jstech.space.crew.CrewRole;
import dev.jstech.space.crew.ShipConsole;
import dev.jstech.space.ship.AirReserveEntity;
import dev.jstech.space.ship.PulseCourse;
import dev.jstech.space.ship.ShipDesign;
import dev.jstech.space.ship.ShipDestination;
import dev.jstech.space.ship.ShipDestinations;
import dev.jstech.space.ship.ShipFlight;
import dev.jstech.space.ship.ShipFlightModel;
import dev.jstech.space.ship.ShipKitBlocks;
import dev.jstech.space.ship.ShipPulse;
import dev.jstech.space.ship.ShipSounds;
import dev.jstech.space.ship.SmallShipParts;
import dev.jstech.space.shipyard.ShipyardTerminalBlockEntity;
import dev.jstech.space.universe.SolarBody;
import dev.jstech.space.universe.SolarSystemWorlds;
import dev.jstech.space.universe.SpaceClimbs;
import dev.jstech.space.universe.StarSystems;
import dev.jstech.space.universe.SystemLayout;
import dev.jstech.space.universe.SystemSpaces;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.space.TestShipyards;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * A small ship climbing to space on a player's game, the player its pilot at its helm: pitched up and flying on, it
 * climbs over the line where the Overworld becomes its space and crosses into it with no loading screen at all, the
 * player still sitting at its helm on the other side, the ship drawn round them there. Come down from space onto the
 * Moon and flying on fast over it, its pilot stays at its helm, and gets up in it. Got up from its helm in flight,
 * its pilot stands on its deck and goes on with it.
 */
public final class ShipOrbitClientTests {

    private static final ResourceKey<Level> SPACE = SystemSpaces.keyOf(StarSystems.SOLAR_SYSTEM);
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
    /* How far under the line the ship waits, in blocks; how far its nose is pitched up, in degrees; its cruise. */
    private static final int UNDER_THE_LINE = 40;
    private static final double CLIMBING_PITCH = 60;
    private static final double CRUISE = 40;
    /*
     * The console's screen as it lays its face out: how far the face keeps from the window's edges, and the least it
     * shrinks to; and how far into a key a click lands, in the face's pixels.
     */
    private static final int FACE_MARGIN = 10;
    private static final float MOST_SHRUNK = 0.5F;
    private static final int TO_THE_KEY = 12;
    /*
     * How far under the floor of space the ship is sent over the Moon, in blocks; and how fast it flies on there, in
     * metres a second, and how far.
     */
    private static final double UNDER_THE_FLOOR = 10;
    private static final double FAST = 120;
    private static final double FLIES_ON = 400;
    /*
     * How long a pilot got up in flight is watched on the deck, in ticks, and the longest step a tick on it a crouching
     * walk makes, with room to spare, in blocks: one set back where the deck was goes the deck's own tick, two here.
     */
    private static final int WATCHED = 40;
    private static final double A_STEP = 0.5;
    /* Far out in the Solar System's space, over no world; how often a mode is switched; how still a crew stands. */
    private static final double DEEP_SPACE = 15_000;
    private static final int SWITCHES = 4;
    /*
     * How far its drive has spooled, in hundredths, when the HUD's ring is photographed; how long it jumps before its
     * pilot holds back, in ticks; how much further than the jump goes in the time between the space may be drawn from
     * one frame to the next, in blocks: two ticks of the jump, far under the thousands it would jump at its end were
     * it drawn wrong.
     */
    private static final int SPOOL_SHOWN = 50;
    private static final int JUMPING = 20;
    private static final float NORTH = 180F;
    private static final double MOST_A_FRAME = 2 * PulseCourse.SPEED * ShipFlightModel.TICK;
    private static final double STOOD_STILL = 0.3;
    private static final ShipDesign SHIP = new ShipDesign("JS-06", List.of(part("orbiter_cockpit"), part("airlock"),
            part("quarters"), part("lab"), part("reactor_flush")), List.of(part("twin_bells"), part("side_intakes"),
            part("blended_delta"), part("twin_canted_fins"), part("blisters"), part("wheels")));

    private ShipOrbitClientTests() {
    }

    @ClientTest(timeoutTicks = 2400)
    public static void orbit_itClimbsIntoSpaceWithNoLoadingScreenItsPilotAtTheHelm(final ClientTestContext ctx) {
        final MovingStructure[] ship = new MovingStructure[1];
        final int[] plot = new int[1];
        final boolean[] screen = new boolean[1];
        final int[] passages = new int[1];
        // From its launch on, the least air its pilot had and the most cold they took, each tick on the server.
        final boolean[] watching = new boolean[1];
        final int[] breath = {Integer.MAX_VALUE, 0};
        final Consumer<ServerTickEvent.Post> watch = event -> {
            if (watching[0]) {
                breath[0] = Math.min(breath[0], ctx.serverPlayer().getAirSupply());
                breath[1] = Math.max(breath[1], ctx.serverPlayer().getTicksFrozen());
            }
        };
        NeoForge.EVENT_BUS.addListener(watch);
        ctx.afterTest(() -> NeoForge.EVENT_BUS.unregister(watch));
        atTheHelm(ctx, ship)
                .then(SETTLE, () -> passages[0] = TravelScreens.seamlessPassages())
                .thenServer(0, level -> {
                    // Its nose pitched up and its throttle open at its cruise: it climbs on into space.
                    watching[0] = true;
                    ship[0].teleport(ship[0].pose().turnedBy(1, 0, 0, Math.toRadians(CLIMBING_PITCH)));
                    MovingStructures.move(ship[0], ((ShipFlight) ship[0].motion()).with(new ShipFlightModel.State(
                            CRUISE, 0, 1, 0, 0)));
                })
                .thenWaitUntil(() -> arrived(ctx, screen), ARRIVE, "the ship and its pilot to cross into space")
                .thenServer(0, level -> plot[0] = MovingStructures.byId(space(ctx), ship[0].id()).plot())
                .thenWaitUntil(() -> ctx.player().getVehicle() instanceof StructureSeatEntity && drawn(plot[0]),
                        ARRIVE, "the player to sit at its helm still, the ship drawn round them in space")
                .then(SETTLE, () -> ctx.assertTrue(!screen[0] && TravelScreens.seamlessPassages() > passages[0],
                        "the passage opened no loading screen: a screen seen " + screen[0] + ", seamless passages "
                                + passages[0] + " to " + TravelScreens.seamlessPassages()))
                .thenServer(0, level -> ctx.assertTrue(breath[0] == ctx.serverPlayer().getMaxAirSupply()
                        && breath[1] == 0, "its pilot breathed its cockpit's air all the way up and across, never short"
                        + " of it, never cold: least air " + breath[0] + ", most cold " + breath[1]))
                .thenScreenshot(0, "ship-orbit");
    }

    /*
     * Climbed into orbit over the Overworld and stopped there, its pilot opens the console at the helm and its
     * destination page, as the approved page draws it: the world below named, the dock the ship left and where its
     * pilot last took off from offered, each how far off; the dock chosen by its left key, boxed, and the line at the
     * foot saying the ship comes down to it.
     */
    @ClientTest(timeoutTicks = 2400)
    public static void console_inOrbitItsDestinationPageOffersThePlacesOfTheWorldBelow(final ClientTestContext ctx) {
        final MovingStructure[] ship = new MovingStructure[1];
        final AirReserveEntity[] console = new AirReserveEntity[1];
        final int[] plot = new int[1];
        final boolean[] screen = new boolean[1];
        ctx.afterTest(() -> ctx.mc().setScreen(null));
        atTheHelm(ctx, ship)
                .thenServer(0, level -> {
                    ship[0].teleport(ship[0].pose().turnedBy(1, 0, 0, Math.toRadians(CLIMBING_PITCH)));
                    MovingStructures.move(ship[0], ((ShipFlight) ship[0].motion()).with(new ShipFlightModel.State(
                            CRUISE, 0, 1, 0, 0)));
                })
                .thenWaitUntil(() -> arrived(ctx, screen), ARRIVE, "the ship and its pilot to cross into space")
                .thenServer(0, level -> plot[0] = MovingStructures.byId(space(ctx), ship[0].id()).plot())
                .thenWaitUntil(() -> ctx.player().getVehicle() instanceof StructureSeatEntity && drawn(plot[0]),
                        ARRIVE, "the player to sit at its helm still, the ship drawn round them in space")
                .thenServer(0, level -> {
                    // Stopped over the Overworld, its console opened by its pilot at the helm.
                    final MovingStructure there = MovingStructures.byId(space(ctx), ship[0].id());
                    MovingStructures.move(there, ((ShipFlight) there.motion()).with(ShipFlightModel.State.RESTING));
                    console[0] = (AirReserveEntity) space(ctx).getBlockEntity(((ShipFlight) there.motion())
                            .consoleIn(there).orElseThrow());
                    ShipConsole.open(ctx.serverPlayer(), console[0], CrewRole.PILOT);
                })
                .thenWaitUntil(() -> ctx.mc().screen instanceof ShipConsoleScreen, ARRIVE,
                        "its console's screen to open")
                .then(SETTLE, () -> {
                    final ShipDestinations.Offer offer = ((ShipConsoleScreen) ctx.mc().screen).getMenu().offer();
                    ctx.assertTrue(offer.body().equals(SolarBody.OVERWORLD.name()) && offer.places().stream()
                            .anyMatch(place -> place.kind() == ShipDestination.Kind.DOCK) && offer.places().stream()
                            .anyMatch(place -> place.kind() == ShipDestination.Kind.LIFT_OFF), "over the Overworld,"
                            + " it offers the dock the ship left and where its pilot last took off from: " + offer);
                    click(ctx, FlightComputerFace.topKeyX(ShipConsoleFace.DEST_KEY, ShipConsoleFace.TOP_KEYS)
                            + TO_THE_KEY, FlightComputerFace.SY0 + TO_THE_KEY / 2);
                })
                .then(SETTLE, () -> click(ctx, FlightComputerFace.SX0 - TO_THE_KEY, FlightComputerFace.sideKeyY(0)
                        + TO_THE_KEY / 2))
                .thenWaitUntil(() -> ((ShipConsoleScreen) ctx.mc().screen).getMenu().console().destination()
                        .filter(place -> place.kind() == ShipDestination.Kind.DOCK).isPresent(), ARRIVE,
                        "the dock chosen by its left key, as the player's game hears of it")
                .thenScreenshot(SETTLE, "ship-console-dest");
    }

    /* A click on the console's screen open at {@code (x, y)} of its face, as its screen lays the face out. */
    private static void click(final ClientTestContext ctx, final double x, final double y) {
        final ShipConsoleScreen open = (ShipConsoleScreen) ctx.mc().screen;
        final float scale = Math.max(MOST_SHRUNK, Math.min((open.width - 2F * FACE_MARGIN) / FlightComputerFace.WIDTH,
                (open.height - 2F * FACE_MARGIN) / FlightComputerFace.HEIGHT));
        final int left = (open.width - Math.round(FlightComputerFace.WIDTH * scale)) / 2;
        final int top = (open.height - Math.round(FlightComputerFace.HEIGHT * scale)) / 2;
        open.mouseClicked(left + x * scale, top + y * scale, 0);
    }

    /*
     * Come down through the floor of space over the Moon, the ship crosses into it and flies on fast over its land,
     * made only as the ship comes: its pilot sits at its helm all the way, on the server and on their game, the seat
     * going on with the ship; and the ship stopped, they get up and stand in it.
     */
    @ClientTest(timeoutTicks = 3600)
    public static void descent_comingDownOnTheMoonItsPilotStaysAtTheHelmAndGetsUpInIt(final ClientTestContext ctx) {
        final MovingStructure[] ship = new MovingStructure[1];
        final MovingStructure[] there = new MovingStructure[1];
        final ResourceLocation[] moon = new ResourceLocation[1];
        final double[] arrivedAt = new double[2];
        final KeyMapping crouch = ctx.mc().options.keyShift;
        ctx.afterTest(() -> crouch.setDown(false));
        atTheHelm(ctx, ship)
                .thenServer(0, level -> {
                    moon[0] = moon(ctx).dimension().location();
                    final double[] over = SystemLayout.centre(SolarBody.MOON);
                    ctx.assertTrue(MovingStructures.travel(ship[0], space(ctx), new Vec3(over[0], SpaceClimbs.floor()
                            - UNDER_THE_FLOOR, over[2])).ok(), "the ship is sent into space, under its floor over the"
                            + " Moon");
                })
                .thenWaitUntilServer(level -> {
                    there[0] = MovingStructures.byId(moon(ctx), ship[0].id());
                    return there[0] != null && there[0].ready();
                }, ARRIVE, "the ship to come down through space onto the Moon", level -> "here "
                        + MovingStructures.byId(level, ship[0].id()) + " at " + ship[0].pose() + ", in space "
                        + MovingStructures.byId(space(ctx), ship[0].id()))
                .thenWaitUntil(() -> ctx.mc().level != null && ctx.mc().level.dimension().location().equals(moon[0]),
                        ARRIVE, "its pilot's game to be on the Moon")
                .thenServer(0, level -> {
                    arrivedAt[0] = there[0].pose().x();
                    arrivedAt[1] = there[0].pose().z();
                    // Flying on fast over the Moon's land, made only as it comes, as a ship come down from space does.
                    MovingStructures.move(there[0], ((ShipFlight) there[0].motion()).with(new ShipFlightModel.State(
                            FAST, 0, 1, 0, 0)));
                })
                .thenWaitUntilServer(level -> Math.hypot(there[0].pose().x() - arrivedAt[0], there[0].pose().z()
                        - arrivedAt[1]) > FLIES_ON, ARRIVE, "the ship to fly on over the Moon", level -> "at "
                        + there[0].pose())
                .thenServer(0, level -> {
                    final ServerPlayer pilot = ctx.serverPlayer();
                    ctx.assertTrue(pilot.getVehicle() instanceof StructureSeatEntity seat
                            && seat.structure() == there[0], "its pilot sits at its helm on the server: "
                            + pilot.getVehicle());
                    final AABB box = there[0].worldBox().inflate(1);
                    ctx.assertTrue(box.contains(pilot.getVehicle().position()) && box.contains(pilot.position()),
                            "the seat and its pilot going on with the ship, " + there[0].worldBox() + ", not left"
                            + " behind: the seat at " + pilot.getVehicle().position() + ", its chunk loaded "
                            + moon(ctx).isPositionEntityTicking(pilot.getVehicle().blockPosition()) + ", the pilot at "
                            + pilot.position());
                })
                .then(0, () -> ctx.assertTrue(ctx.player().getVehicle() instanceof StructureSeatEntity, "its pilot"
                        + " sits at its helm on their game"))
                // Brought to a stop over the land, as a ship that has set down is, its pilot gets up.
                .thenServer(0, level -> MovingStructures.move(there[0], new TowardMotion(there[0].pose(), 1, 0)))
                .then(SETTLE, () -> crouch.setDown(true))
                .thenWaitUntil(() -> !(ctx.player().getVehicle() instanceof StructureSeatEntity), ARRIVE,
                        "its pilot to get up from the helm, crouching")
                .then(0, () -> crouch.setDown(false))
                .thenServer(SETTLE, level -> {
                    final ServerPlayer pilot = ctx.serverPlayer();
                    ctx.assertTrue(!pilot.isPassenger() && there[0].worldBox().inflate(1).contains(pilot.position()),
                            "and stands in the ship, " + there[0].worldBox() + ", on the server too: "
                            + pilot.position() + ", riding " + pilot.getVehicle());
                });
    }

    /*
     * Got up from its helm crouching while the ship flies on at its cruise, its pilot walks back along its deck,
     * crouching, a crouching walk's step a tick on the server, never a jump: they go on with the ship, on the server
     * and on their game, the deck under them all the while.
     */
    @ClientTest(timeoutTicks = 3600)
    public static void helm_gotUpInFlightThePilotWalksTheDeckCrouchingAndGoesOnWithIt(final ClientTestContext ctx) {
        final MovingStructure[] ship = new MovingStructure[1];
        final KeyMapping crouch = ctx.mc().options.keyShift;
        final KeyMapping back = ctx.mc().options.keyDown;
        final boolean[] flying = new boolean[1];
        // On the server, each tick once up: where on the deck they stood the tick before, their longest step on it.
        final int[] watched = new int[1];
        final double[] before = new double[3];
        final double[] longest = new double[1];
        final Consumer<ServerTickEvent.Post> watch = event -> {
            final ServerPlayer player = ctx.serverPlayer();
            if (!flying[0] || ship[0] == null || player.isPassenger() || watched[0] >= WATCHED) {
                return;
            }
            final StructureFrame frame = ship[0].frame();
            final double x = frame.yardX(player.getX(), player.getY(), player.getZ());
            final double z = frame.yardZ(player.getX(), player.getY(), player.getZ());
            if (watched[0]++ > 0) {
                longest[0] = Math.max(longest[0], Math.hypot(x - before[0], z - before[2]));
            }
            before[0] = x;
            before[2] = z;
        };
        NeoForge.EVENT_BUS.addListener(watch);
        ctx.afterTest(() -> NeoForge.EVENT_BUS.unregister(watch));
        ctx.afterTest(() -> {
            crouch.setDown(false);
            back.setDown(false);
        });
        atTheHelm(ctx, ship)
                .thenServer(0, level -> {
                    flying[0] = true;
                    MovingStructures.move(ship[0], ((ShipFlight) ship[0].motion()).with(
                            new ShipFlightModel.State(CRUISE, 0, 1, 0, 0)));
                })
                .then(SETTLE, () -> crouch.setDown(true))
                .thenWaitUntil(() -> !(ctx.player().getVehicle() instanceof StructureSeatEntity), ARRIVE,
                        "its pilot to get up from the helm, crouching")
                .then(0, () -> {
                    // Looking along the ship's nose, as the pilot at its helm does.
                    final ShipFlight flight = (ShipFlight) ship[0].motion();
                    final StructureFrame frame = ship[0].frame();
                    ctx.player().setYRot((float) Math.toDegrees(Math.atan2(-frame.turnX(flight.noseX(), 0,
                            flight.noseZ()), frame.turnZ(flight.noseX(), 0, flight.noseZ()))));
                    back.setDown(true);
                })
                // The server's own step met the deck's blocks as it had them, a moment apart from the deck the
                // player's game moved against, stopped short, and sent them back to where it last had them, a place
                // the deck had left.
                .thenWaitUntilServer(level -> watched[0] >= WATCHED, ARRIVE, "its pilot to be watched on the deck",
                        level -> watched[0] + " ticks")
                .then(0, () -> {
                    crouch.setDown(false);
                    back.setDown(false);
                })
                .thenServer(0, level -> {
                    final ServerPlayer pilot = ctx.serverPlayer();
                    ctx.assertTrue(longest[0] < A_STEP, "its pilot walking back along the deck, crouching, the ship"
                            + " flying on at " + CRUISE + " metres a second, steps along it a crouching walk's step a"
                            + " tick, never set back where the deck was: their longest step " + longest[0] + " blocks");
                    ctx.assertTrue(!pilot.isPassenger() && ship[0].worldBox().contains(pilot.position()), "and goes on"
                            + " with the ship, " + ship[0].worldBox() + ": " + pilot.position());
                })
                .then(0, () -> ctx.assertTrue(ship[0].worldBox().inflate(1).contains(ctx.player().position()),
                        "on their game too: " + ctx.player().position()));
    }

    /*
     * In space, the ship at a stop, its pilot got up and standing in its cockpit has their game mode switched, from
     * survival to creative and back: they stand on in the cockpit, where they stood.
     */
    @ClientTest(timeoutTicks = 3600)
    public static void modes_switchedStandingAboardInSpaceTheCrewStaysWhereItStood(final ClientTestContext ctx) {
        final MovingStructure[] ship = new MovingStructure[1];
        final MovingStructure[] there = new MovingStructure[1];
        final Vec3[] stood = new Vec3[1];
        final KeyMapping crouch = ctx.mc().options.keyShift;
        ctx.afterTest(() -> crouch.setDown(false));
        ClientTestContext steps = atTheHelm(ctx, ship)
                .thenServer(0, level -> ctx.assertTrue(MovingStructures.travel(ship[0], space(ctx), new Vec3(0,
                        SystemLayout.ORBIT_Y, DEEP_SPACE)).ok(), "the ship is sent into space, over no world"))
                .thenWaitUntilServer(level -> {
                    there[0] = MovingStructures.byId(space(ctx), ship[0].id());
                    return there[0] != null && there[0].ready();
                }, ARRIVE, "the ship to come into space", level -> "at " + ship[0].pose())
                .thenWaitUntil(() -> ctx.mc().level != null && ctx.mc().level.dimension().equals(SPACE), ARRIVE,
                        "its pilot's game to be in space")
                .thenServer(0, level -> MovingStructures.move(there[0], new TowardMotion(there[0].pose(), 1, 0)))
                .then(SETTLE, () -> crouch.setDown(true))
                .thenWaitUntil(() -> !(ctx.player().getVehicle() instanceof StructureSeatEntity), ARRIVE,
                        "its pilot to get up from the helm, crouching")
                .then(SETTLE, () -> crouch.setDown(false))
                .thenServer(SETTLE, level -> stood[0] = MovingStructures.toYard(there[0], ctx.serverPlayer()
                        .position()));
        for (int k = 0; k < SWITCHES; k++) {
            final GameType mode = k % 2 == 0 ? GameType.SURVIVAL : GameType.CREATIVE;
            steps = steps.thenServer(SETTLE, level -> ctx.serverPlayer().setGameMode(mode));
        }
        steps.thenServer(SETTLE, level -> {
            final Vec3 now = MovingStructures.toYard(there[0], ctx.serverPlayer().position());
            ctx.assertTrue(now.distanceTo(stood[0]) < STOOD_STILL, "switched from survival to creative and back "
                    + SWITCHES + " times, the player stands where they stood in the cockpit: " + stood[0] + " then "
                    + now);
        });
    }

    /*
     * In deep space, its pilot at the helm holding the pulse jump's key: the HUD's ring fills as its drive spools,
     * and it jumps, PULSE lit and the stars streaking, the space drawn moving round the ship standing still; its pilot
     * holding back, it drops out, PULSE OFF framed, set where the jump took it, and the space drawn from as far on
     * as before, never jumping, from the first frame of the jump to after it.
     */
    @ClientTest(timeoutTicks = 3600)
    public static void pulse_itsPilotJumpsTheShipAndDropsOutTheSpaceDrawnOnWithoutAJump(final ClientTestContext ctx) {
        final MovingStructure[] ship = new MovingStructure[1];
        final MovingStructure[] there = new MovingStructure[1];
        final KeyMapping pulse = KeyActionsClient.mapping(ShipPulse.KEY);
        final KeyMapping back = ctx.mc().options.keyDown;
        ctx.afterTest(() -> {
            pulse.setDown(false);
            back.setDown(false);
        });
        // Each frame from the jump on: where the space was drawn from and when, and the most a step from one frame to
        // the next went past what the jump goes in the time between, a frame drawn late going on as far as it should.
        final boolean[] watching = new boolean[1];
        final Vec3[] seenFrom = new Vec3[1];
        final double[] seenAt = new double[1];
        final double[] longest = new double[1];
        final Consumer<RenderLevelStageEvent> watch = event -> {
            if (!watching[0] || event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY || ctx.mc().level == null) {
                return;
            }
            final float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
            final double[] jumped = ShipPulseClient.offset(partial);
            final Vec3 from = event.getCamera().getPosition().add(jumped[0], 0, jumped[1]);
            final double at = ctx.mc().level.getGameTime() + partial;
            if (seenFrom[0] != null) {
                longest[0] = Math.max(longest[0], from.distanceTo(seenFrom[0])
                        - (at - seenAt[0]) * PulseCourse.SPEED * ShipFlightModel.TICK);
            }
            seenFrom[0] = from;
            seenAt[0] = at;
        };
        NeoForge.EVENT_BUS.addListener(watch);
        ctx.afterTest(() -> NeoForge.EVENT_BUS.unregister(watch));
        atTheHelm(ctx, ship)
                .thenServer(0, level -> ctx.assertTrue(MovingStructures.travel(ship[0], space(ctx), new Vec3(0,
                        SystemLayout.ORBIT_Y, DEEP_SPACE)).ok(), "the ship is sent into space, over no world"))
                .thenWaitUntilServer(level -> {
                    there[0] = MovingStructures.byId(space(ctx), ship[0].id());
                    return there[0] != null && there[0].ready();
                }, ARRIVE, "the ship to come into space", level -> "at " + ship[0].pose())
                .thenWaitUntil(() -> ctx.mc().level != null && ctx.mc().level.dimension().equals(SPACE), ARRIVE,
                        "its pilot's game to be in space")
                .thenServer(0, level -> MovingStructures.move(there[0], ((ShipFlight) there[0].motion()).with(
                        new ShipFlightModel.State(CRUISE, 0, CRUISE / FAST, 0, 0))))
                .then(SETTLE, () -> {
                    ctx.mc().options.setCameraType(CameraType.FIRST_PERSON);
                    // Looking out ahead along its nose, north, as a pilot at its helm does.
                    ctx.player().setYRot(NORTH);
                    ctx.player().setXRot(0);
                    pulse.setDown(true);
                })
                .thenWaitUntil(() -> spooled(ctx) >= SPOOL_SHOWN, ARRIVE, "the HUD's ring to fill as its drive"
                        + " spools")
                .thenScreenshot(0, "ship-pulse-spool")
                .thenWaitUntil(ShipPulseClient::jumping, ARRIVE, "the ship to jump")
                .then(0, () -> {
                    pulse.setDown(false);
                    watching[0] = true;
                })
                .thenScreenshot(JUMPING, "ship-pulse-jump")
                .then(0, () -> ctx.assertTrue(ShipEngineSounds.playing(ShipHud.atTheHelm(ctx.mc())).equals(
                        List.of(ShipSounds.PULSE_LOOP)), "in the jump its pulse drive roars over its engines: "
                        + ShipEngineSounds.playing(ShipHud.atTheHelm(ctx.mc()))))
                .then(0, () -> ctx.mc().options.setCameraType(CameraType.THIRD_PERSON_BACK))
                .thenScreenshot(SETTLE / 2, "ship-pulse-jump-behind")
                .then(0, () -> {
                    ctx.mc().options.setCameraType(CameraType.FIRST_PERSON);
                    back.setDown(true);
                })
                .thenWaitUntilServer(level -> ((ShipFlight) there[0].motion()).pulse().isEmpty(), ARRIVE,
                        "its pilot holding back, the ship to drop out", level -> "jumping")
                .then(0, () -> back.setDown(false))
                .thenWaitUntil(() -> !ShipPulseClient.jumping() && dropped(ctx), ARRIVE, "PULSE OFF on the HUD")
                .thenScreenshot(SETTLE, "ship-pulse-exit")
                .then(0, () -> {
                    watching[0] = false;
                    ctx.assertTrue(longest[0] < MOST_A_FRAME, "the space drawn on smoothly through the jump and out of"
                            + " it, never jumping: a step from one frame to the next went at most " + longest[0]
                            + " blocks past the jump's own");
                });
    }

    /* How far the drive of the ship whose helm the player sits at has spooled, in hundredths, as its console shows. */
    private static int spooled(final ClientTestContext ctx) {
        final AirReserveEntity console = ShipHud.atTheHelm(ctx.mc());
        return console == null ? 0 : console.flight().pulse().spool();
    }

    /* Whether the console of the ship whose helm the player sits at shows it just dropped out of a jump. */
    private static boolean dropped(final ClientTestContext ctx) {
        final AirReserveEntity console = ShipHud.atTheHelm(ctx.mc());
        return console != null && !console.flight().pulse().dropped().isEmpty();
    }

    /*
     * The dock built where the test stands, a ship built in it and let go, {@code ship} told it, hovering under the
     * line to space with the player at its helm in its sealed cockpit; after the test, the ship taken away from
     * wherever it went and the test's place put back.
     */
    private static ClientTestContext atTheHelm(final ClientTestContext ctx, final MovingStructure[] ship) {
        final ServerLevel[] built = new ServerLevel[1];
        final BlockPos[] terminal = new BlockPos[1];
        final PlannedBuild[] plan = new PlannedBuild[1];
        ctx.afterTest(() -> ctx.server().execute(() -> {
            ctx.serverPlayer().stopRiding();
            if (ship[0] != null) {
                for (final ServerLevel level : ctx.server().getAllLevels()) {
                    final MovingStructure found = MovingStructures.byId(level, ship[0].id());
                    if (found != null) {
                        MovingStructures.takeAway(found);
                    }
                }
            }
            land(ctx);
            if (built[0] != null) {
                clear(ctx, built[0]);
            }
            ctx.serverPlayer().setGameMode(GameType.CREATIVE);
        }));
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
                    // Taken up under the line to space, hovering there while the player takes its helm.
                    final StructurePose now = ship[0].pose();
                    ship[0].teleport(now.movedTo(now.x(), SpaceClimbs.ceiling(level) - UNDER_THE_LINE, now.z()));
                    ctx.serverPlayer().setGameMode(GameType.SURVIVAL);
                    ctx.assertTrue(MovingStructures.sit(ctx.serverPlayer(), seat), "the player sits at its helm");
                })
                .thenWaitUntil(() -> ctx.player().getVehicle() instanceof StructureSeatEntity
                        && drawn(ship[0].plot()), ARRIVE, "the player to sit at the helm of the ship drawn round them")
                .thenWaitUntilServer(level -> SealedRooms.roomBeside(level, ((ShipFlight) ship[0].motion())
                        .consoleIn(ship[0]).orElseThrow()).isPresent(), ARRIVE, "its cockpit to be a sealed room",
                        level -> "no room by its console");
    }

    /* Whether the player's game is in the Solar System's space; any loading screen seen on the way is noted. */
    private static boolean arrived(final ClientTestContext ctx, final boolean[] screen) {
        if (ctx.mc().screen instanceof ReceivingLevelScreen) {
            screen[0] = true;
        }
        return ctx.mc().level != null && ctx.mc().level.dimension().equals(SPACE);
    }

    /* Whether the ship in the yard's plot {@code plot} is drawn whole on the player's game. */
    private static boolean drawn(final int plot) {
        final ClientStructure seen = ClientMovingStructures.byPlot(plot);
        return seen != null && seen.ready() && seen.meshes().built() > 0 && !seen.meshes().waiting();
    }

    /* Puts the player back on the ground of the test's place, as the next test expects them. */
    private static void land(final ClientTestContext ctx) {
        final ServerPlayer player = ctx.serverPlayer();
        final BlockPos origin = ctx.origin().above(2);
        player.teleportTo(ctx.server().overworld(), origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5, 0F, 0F);
    }

    private static ServerLevel space(final ClientTestContext ctx) {
        return SystemSpaces.spaceOf(ctx.server(), StarSystems.SOLAR_SYSTEM);
    }

    private static ServerLevel moon(final ClientTestContext ctx) {
        return ctx.server().getLevel(SpaceWorlds.get(ctx.server().overworld(), SolarSystemWorlds.worldOf(
                SolarBody.MOON)).orElseThrow().dimension());
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
