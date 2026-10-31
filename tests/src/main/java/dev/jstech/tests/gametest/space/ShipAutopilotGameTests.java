/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.StructureControls;
import dev.jstech.core.moving.StructurePose;
import dev.jstech.core.multiblock.Orientation;
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.multiblock.plan.PlannedBuild;
import dev.jstech.core.region.Box;
import dev.jstech.core.team.Ownership;
import dev.jstech.space.landing.RunwayBeaconBlock;
import dev.jstech.space.landing.Runways;
import dev.jstech.space.landing.ShipLandingBlocks;
import dev.jstech.space.landing.ShipLandings;
import dev.jstech.space.ship.AirReserveEntity;
import dev.jstech.space.ship.ShipAutopilot;
import dev.jstech.space.ship.ShipAutopilotControl;
import dev.jstech.space.ship.ShipAutopilots;
import dev.jstech.space.ship.ShipCollars;
import dev.jstech.space.ship.ShipDocks;
import dev.jstech.space.ship.ShipDesign;
import dev.jstech.space.ship.ShipFlight;
import dev.jstech.space.ship.ShipFlightModel;
import dev.jstech.space.ship.ShipKitBlocks;
import dev.jstech.space.ship.ShipReadings;
import dev.jstech.space.ship.SmallShipParts;
import dev.jstech.space.shipyard.ShipyardPlans;
import dev.jstech.space.shipyard.ShipyardStructure;
import dev.jstech.space.shipyard.ShipyardTerminalBlockEntity;
import dev.jstech.space.universe.FarMaps;
import dev.jstech.space.universe.SpaceClimbs;
import dev.jstech.space.universe.StarSystems;
import dev.jstech.space.universe.SystemSpaces;
import dev.jstech.testkit.gametest.GameTestPlayers;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.space.TestShipyards;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestSequence;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A ship's autopilot flies it as a pilot would: its key at the helm engages LAND to the nearest ship landing pad its
 * pilot may use, and LAND flies it there and sets it down on the pad, letting go once it stands; on wheels, LAND lines
 * it up on a runway whose stretch is clear, sets it down past the beacon and rolls it to a stop on the stretch, and
 * passes a runway whose stretch is not clear; ORBIT pitches
 * its nose up to the climb's angle, its throttle open, its console showing how high the climb tops out, and its pilot
 * taking the helm lets it go; in space its key engages DOCK with the nearest ship with a collar, and DOCK turns it to
 * face the other's collar and brings the two collars together till they touch; touching, the two are made one, held
 * collar to collar, its pilot goes through the collars into the other ship and back, and let go, it is clear of the
 * other and flies by its own flight again.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ShipAutopilotGameTests {

    private static final String ARENA = "shipyard";
    private static final String BATCH = "jsspace_ship_autopilot";
    /* Where the dock's lowest corner stands in the arena. */
    private static final BlockPos CORNER = new BlockPos(1, 2, 1);
    /* How far north of the dock, and how far west, the land the tests fly over is kept loaded, in chunks. */
    private static final int NORTH_CHUNKS = 8;
    private static final int WEST_CHUNKS = 1;
    private static final TicketType<BlockPos> LAND = TicketType.create("jstests_ship_autopilot", Vec3i::compareTo);
    private static final int TICKING = 2;
    /* How high the ship is taken up before its autopilot is engaged, in blocks. */
    private static final double HIGH = 60;
    /*
     * How far north of the ship the landing platform stands, in blocks, at the nearest and the farthest, tried a step
     * apart, and how wide it is; how far round it, across and along, the land is bare for the ship to set down.
     */
    private static final int PLATFORM_NORTH = 56;
    private static final int PLATFORM_FARTHEST = 112;
    private static final int PLATFORM_STEP = 8;
    private static final int BARE_ACROSS = 6;
    private static final int BARE_ALONG = 11;
    /*
     * How far north of the ship a runway's beacon stands, and how far its floor reaches behind it; how far north the
     * land is kept loaded for it, in chunks; how far along its stretch a block stands in its way.
     */
    private static final int RUNWAY_NORTH = 150;
    private static final int RUNWAY_BEHIND = 8;
    private static final int RUNWAY_CHUNKS = 15;
    private static final int BLOCKED_AT = 30;
    /* How high over the runway a ship rolling out may be, at the most, in blocks: no hop off it. */
    private static final double NO_HOP = 0.1;
    /* How near the platform's middle the ship sets down, in blocks across, and over its top. */
    private static final double ON_THE_SPOT = 3;
    private static final double ON_THE_TOP = 1.5;
    /* How fast it flies when ORBIT is engaged, in metres a second, and how long it is left to climb, in ticks. */
    private static final double CRUISING = 40;
    private static final int CLIMBING = 160;
    /* How near the climb's angle it holds its nose, in degrees, and how open its throttle is by then. */
    private static final double ON_THE_CLIMB = 3;
    private static final double OPEN = 0.95;
    /* Where the two ships stand in space docking: the other above, ours below, off to one side, in blocks. */
    private static final double SPACE_HEIGHT = 200;
    private static final double ABOVE = 24;
    private static final double ASIDE = 8;
    /* How near the two collars come, in blocks, and how far round the space's land is kept loaded, in chunks. */
    private static final double TOUCHING = 0.3;
    private static final int SPACE_CHUNKS = 3;
    /* How long the two docked ships are watched, in ticks; how near under its collar one passing through stands. */
    private static final int HELD = 20;
    private static final double UNDER = 1.5;
    private static final ShipDesign LEGS_SHIP = new ShipDesign("JS-04", List.of(part("orbiter_cockpit"),
            part("airlock"), part("quarters"), part("workshop"), part("reactor_flush")), List.of(part("twin_bells"),
            part("side_intakes"), part("blended_delta"), part("twin_canted_fins"), part("blisters"), part("legs")));
    private static final ShipDesign WHEELS_SHIP = new ShipDesign("JS-07", List.of(part("orbiter_cockpit"),
            part("airlock"), part("quarters"), part("workshop"), part("reactor_flush")), List.of(part("twin_bells"),
            part("side_intakes"), part("blended_delta"), part("twin_canted_fins"), part("blisters"), part("wheels")));
    private static final ShipDesign DOCKING_SHIP = new ShipDesign("JS-06", List.of(part("orbiter_cockpit"),
            part("docking"), part("quarters"), part("reactor_flush")), List.of(part("twin_bells"),
            part("side_intakes"), part("blended_delta"), part("twin_canted_fins"), part("blisters"), part("legs")));

    private ShipAutopilotGameTests() {
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 3000)
    public static void autopilot_landSetsItDownOnTheNearestPadItsPilotMayUse(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final MovingStructure[] ship = new MovingStructure[1];
        final ServerPlayer[] pilot = new ServerPlayer[1];
        final UUID pad = UUID.randomUUID();
        final Vec3[] spot = new Vec3[1];
        letGo(helper, ship, LEGS_SHIP).thenExecute(() -> keep(helper, true, NORTH_CHUNKS))
                .thenWaitUntil(() -> loaded(helper, NORTH_CHUNKS))
                .thenExecute(() -> {
                    final BlockPos seat = aboard(ship[0], plan(helper, LEGS_SHIP).where(spot(helper, LEGS_SHIP,
                            ShipKitBlocks.PILOT_SEAT.getId())));
                    aloft(ship[0], HIGH);
                    pilot[0] = seated(helper, seat);
                    // A ship landing pad of its pilot's on the land to the north, its top level with the land, where
                    // the land is bare round it: the tests beside this one stand on the land too.
                    final BlockPos terminal = bareNorthOf(helper, ship[0].pose());
                    ShipLandings.notePad(level, pad, terminal, Ownership.of(pilot[0].getUUID()));
                    spot[0] = ShipLandings.padSpot(level, ShipLandings.pad(level.getServer(), pad).orElseThrow());
                    ShipAutopilotControl.pressed(pilot[0]);
                    final Optional<ShipAutopilot> engaged = flight(ship[0]).autopilot();
                    helper.assertTrue(engaged.isPresent() && engaged.get().mode() == ShipAutopilot.Mode.LAND
                            && engaged.get().spot().orElseThrow().equals(spot[0])
                            && engaged.get().runway().isEmpty(), "its key at the helm engages LAND to its pilot's"
                            + " pad: " + engaged);
                })
                .thenExecuteAfter(2, () -> {
                    final ShipReadings.Pilot shown = console(ship[0]).flight().pilot();
                    helper.assertTrue(shown.mode().equals("land") && shown.target().distanceTo(spot[0]) < 0.1,
                            "its console shows LAND and where it sets down: " + shown);
                })
                .thenWaitUntil(() -> helper.assertTrue(flight(ship[0]).autopilot().isEmpty()
                        && flight(ship[0]).grounded(), "it flies to the pad and sets down on it, letting go once it"
                        + " stands: at " + ship[0].pose().x() + " " + ship[0].worldBox().minY + " "
                        + ship[0].pose().z() + " for " + spot[0] + ", " + flight(ship[0]).state() + ", grounded "
                        + flight(ship[0]).grounded() + ", " + flight(ship[0]).autopilot()))
                .thenExecute(() -> {
                    final StructurePose at = ship[0].pose();
                    helper.assertTrue(Math.hypot(at.x() - spot[0].x, at.z() - spot[0].z) < ON_THE_SPOT, "on the"
                            + " pad's middle: " + at + " for " + spot[0]);
                    helper.assertTrue(Math.abs(ship[0].worldBox().minY - spot[0].y) < ON_THE_TOP, "standing on its"
                            + " top: " + ship[0].worldBox().minY + " for " + spot[0].y);
                    ShipLandings.dropPad(level.getServer(), pad);
                    GameTestPlayers.leave(pilot[0]);
                    MovingStructures.takeAway(ship[0]);
                    keep(helper, false, NORTH_CHUNKS);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 4000)
    public static void autopilot_landOnWheelsLinesUpOnARunwayAndRollsToAStopOnIt(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final MovingStructure[] ship = new MovingStructure[1];
        final ServerPlayer[] pilot = new ServerPlayer[1];
        final BlockPos[] beacon = new BlockPos[1];
        final double[] touched = {Double.NaN};
        final boolean[] flew = {false};
        final double[] hop = {0};
        letGo(helper, ship, WHEELS_SHIP).thenExecute(() -> keep(helper, true, RUNWAY_CHUNKS))
                .thenWaitUntil(() -> loaded(helper, RUNWAY_CHUNKS))
                .thenExecute(() -> {
                    final BlockPos seat = aboard(ship[0], plan(helper, WHEELS_SHIP).where(spot(helper, WHEELS_SHIP,
                            ShipKitBlocks.PILOT_SEAT.getId())));
                    aloft(ship[0], HIGH);
                    pilot[0] = seated(helper, seat);
                    // A runway laid far to the north in the sky, its beacon facing north, the way it runs.
                    beacon[0] = skyRunwayNorthOf(level, ship[0].pose());
                    ShipAutopilotControl.pressed(pilot[0]);
                    final Optional<ShipAutopilot> engaged = flight(ship[0]).autopilot();
                    helper.assertTrue(engaged.isPresent() && engaged.get().mode() == ShipAutopilot.Mode.LAND
                            && engaged.get().runway().orElse(-1.0) == 0 && engaged.get().cleared()
                            && engaged.get().spot().orElseThrow().equals(Vec3.atBottomCenterOf(beacon[0])),
                            "on wheels, its key at the helm engages LAND onto the runway, its stretch found clear: "
                            + engaged);
                })
                .thenWaitUntil(() -> {
                    helper.assertTrue(beacon[0] != null, "a runway was laid");
                    // Where it first stands on the land once it has flown, along the runway from its beacon.
                    flew[0] |= !flight(ship[0]).grounded();
                    if (flew[0] && Double.isNaN(touched[0]) && flight(ship[0]).grounded()) {
                        touched[0] = beacon[0].getZ() + 0.5 - ship[0].pose().z();
                    }
                    if (!Double.isNaN(touched[0])) {
                        hop[0] = Math.max(hop[0], ship[0].worldBox().minY - beacon[0].getY());
                    }
                    helper.assertTrue(flight(ship[0]).autopilot().isEmpty() && flight(ship[0]).grounded(),
                            "it lines up on the runway, sets down past the beacon and rolls to a stop, letting go:"
                            + " at " + ship[0].pose().x() + " " + ship[0].worldBox().minY + " " + ship[0].pose().z()
                            + " for the beacon at " + beacon[0] + ", " + flight(ship[0]).state() + ", "
                            + flight(ship[0]).autopilot());
                })
                .thenExecute(() -> {
                    final StructurePose at = ship[0].pose();
                    final double along = beacon[0].getZ() + 0.5 - at.z();
                    helper.assertTrue(touched[0] > 0, "it touched down past the beacon: " + touched[0]
                            + " along the runway");
                    helper.assertTrue(hop[0] < NO_HOP, "and rolled out on the runway, never lifting off it: up to "
                            + hop[0] + " over it");
                    helper.assertTrue(along > 0 && along <= ShipAutopilots.RUNWAY_LENGTH
                            && Math.abs(at.x() - beacon[0].getX() - 0.5) < ON_THE_SPOT, "and stopped on its stretch,"
                            + " on its line: " + along + " along, " + (at.x() - beacon[0].getX() - 0.5) + " across");
                    helper.assertTrue(Math.abs(ship[0].worldBox().minY - beacon[0].getY()) < ON_THE_TOP, "standing"
                            + " on the land: " + ship[0].worldBox().minY + " for " + beacon[0].getY());
                    helper.assertTrue(level.getBlockState(beacon[0]).is(ShipLandingBlocks.BEACON.get()), "the"
                            + " beacon still standing");
                    takeAwayRunway(level, beacon[0]);
                    GameTestPlayers.leave(pilot[0]);
                    MovingStructures.takeAway(ship[0]);
                    keep(helper, false, RUNWAY_CHUNKS);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 1200)
    public static void autopilot_landOnWheelsPassesARunwayWhoseStretchIsNotClear(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final MovingStructure[] ship = new MovingStructure[1];
        final ServerPlayer[] pilot = new ServerPlayer[1];
        final BlockPos[] beacon = new BlockPos[1];
        letGo(helper, ship, WHEELS_SHIP).thenExecute(() -> keep(helper, true, RUNWAY_CHUNKS))
                .thenWaitUntil(() -> loaded(helper, RUNWAY_CHUNKS))
                .thenExecute(() -> {
                    final BlockPos seat = aboard(ship[0], plan(helper, WHEELS_SHIP).where(spot(helper, WHEELS_SHIP,
                            ShipKitBlocks.PILOT_SEAT.getId())));
                    aloft(ship[0], HIGH);
                    pilot[0] = seated(helper, seat);
                    // A runway laid in the sky to the north, and a block across its stretch, where the ship would roll.
                    beacon[0] = skyRunwayNorthOf(level, ship[0].pose());
                    level.setBlockAndUpdate(beacon[0].north(BLOCKED_AT), Blocks.STONE.defaultBlockState());
                    ShipAutopilotControl.pressed(pilot[0]);
                    final Optional<ShipAutopilot> engaged = flight(ship[0]).autopilot();
                    // Another test's runway may be in range, clear: only this one's is to be passed.
                    helper.assertTrue(engaged.isEmpty() || engaged.get().spot().isEmpty()
                            || !engaged.get().spot().get().equals(Vec3.atBottomCenterOf(beacon[0])), "LAND is not"
                            + " offered onto a runway whose stretch is not clear: " + engaged);
                    level.removeBlock(beacon[0].north(BLOCKED_AT), false);
                    takeAwayRunway(level, beacon[0]);
                    GameTestPlayers.leave(pilot[0]);
                    MovingStructures.takeAway(ship[0]);
                    keep(helper, false, RUNWAY_CHUNKS);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 1200)
    public static void autopilot_orbitPitchesItUpToTheClimbAndItsPilotTakingTheHelmLetsItGo(
            final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final MovingStructure[] ship = new MovingStructure[1];
        letGo(helper, ship, LEGS_SHIP).thenExecute(() -> keep(helper, true, NORTH_CHUNKS))
                .thenWaitUntil(() -> loaded(helper, NORTH_CHUNKS))
                .thenExecute(() -> {
                    aloft(ship[0], HIGH);
                    MovingStructures.move(ship[0], flight(ship[0]).with(new ShipFlightModel.State(CRUISING, 0, 0.75,
                            1, 0, 0)).engaged(Optional.of(ShipAutopilot.orbit())));
                })
                .thenIdle(CLIMBING)
                .thenExecute(() -> {
                    final ShipFlight flight = flight(ship[0]);
                    final double[] nose = flight.noseIn(ship[0].frame());
                    final double pitch = Math.toDegrees(Math.asin(Mth.clamp(nose[1], -1, 1)));
                    helper.assertTrue(Math.abs(pitch - 55) < ON_THE_CLIMB, "ORBIT pitches its nose up to the climb:"
                            + " " + pitch);
                    helper.assertTrue(flight.state().throttle() > OPEN, "its throttle open: " + flight.state());
                    final ShipReadings.Pilot shown = console(ship[0]).flight().pilot();
                    helper.assertTrue(shown.mode().equals("orbit") && shown.apogee() == SpaceClimbs.ceiling(level)
                            - FarMaps.seaOf(level) && shown.seconds() > 0, "its console shows how high the climb"
                            + " tops out and how long till then: " + shown);
                    // Its pilot takes the helm, holding forward: the autopilot lets go.
                    ship[0].controls().pilot(level.getGameTime(), Map.of(StructureControls.ControlAxis.FORWARD, 1F),
                            new Vec3(0, 0, -1), new Vec3(0, 1, 0));
                })
                .thenExecuteAfter(2, () -> {
                    helper.assertTrue(flight(ship[0]).autopilot().isEmpty(), "its pilot taking the helm lets the"
                            + " autopilot go");
                    MovingStructures.takeAway(ship[0]);
                    keep(helper, false, NORTH_CHUNKS);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 4000)
    public static void autopilot_dockTurnsItToFaceTheOtherShipsCollarAndBringsTheTwoTogether(
            final GameTestHelper helper) {
        final ServerLevel overworld = helper.getLevel();
        final ServerLevel space = SystemSpaces.spaceOf(overworld.getServer(), StarSystems.SOLAR_SYSTEM);
        final MovingStructure[] ours = new MovingStructure[1];
        final MovingStructure[] theirs = new MovingStructure[1];
        final ServerPlayer[] pilot = new ServerPlayer[1];
        final BlockPos far = helper.absolutePos(CORNER).offset(0, 0, -PLATFORM_NORTH * 4);
        final Vec3 them = new Vec3(far.getX() + 0.5, SPACE_HEIGHT, far.getZ() + 0.5);
        final GameTestSequence sequence = letGo(helper, theirs, DOCKING_SHIP).thenExecute(() -> {
            keepSpace(helper, space, far, true);
            helper.assertTrue(MovingStructures.travel(theirs[0], space, them).ok(), "the other ship goes to space");
        }).thenWaitUntil(() -> {
            final MovingStructure there = MovingStructures.byId(space, theirs[0].id());
            helper.assertTrue(there != null && there.ready(), "and arrives there");
            theirs[0] = there;
        });
        build(helper, sequence, ours, DOCKING_SHIP).thenExecute(() -> {
            final BlockPos seat = aboard(ours[0], plan(helper, DOCKING_SHIP).where(spot(helper, DOCKING_SHIP,
                    ShipKitBlocks.PILOT_SEAT.getId())));
            pilot[0] = seated(helper, seat);
            helper.assertTrue(MovingStructures.travel(ours[0], space, them.add(ASIDE, ABOVE, ASIDE / 2)).ok(),
                    "ours goes to space, its pilot at the helm");
        }).thenWaitUntil(() -> {
            final MovingStructure there = MovingStructures.byId(space, ours[0].id());
            helper.assertTrue(there != null && there.ready() && pilot[0].level() == space, "and arrives there with"
                    + " its pilot");
            ours[0] = there;
        }).thenExecute(() -> {
            ShipAutopilotControl.pressed(pilot[0]);
            final Optional<ShipAutopilot> engaged = flight(ours[0]).autopilot();
            helper.assertTrue(engaged.isPresent() && engaged.get().mode() == ShipAutopilot.Mode.DOCK
                    && engaged.get().other().orElseThrow().equals(theirs[0].id()), "in space its key engages DOCK"
                    + " with the ship beside it: " + engaged);
        }).thenWaitUntil(() -> {
            final Vec3 a = ShipCollars.mouth(space, ours[0]).orElseThrow();
            final Vec3 b = ShipCollars.mouth(space, theirs[0]).orElseThrow();
            helper.assertTrue(a.distanceTo(b) < TOUCHING, "DOCK brings its collar to the other's: " + a.distanceTo(b)
                    + " off, " + flight(ours[0]).autopilot());
        }).thenExecute(() -> {
            final double[] up = {ours[0].frame().turnX(0, 1, 0), ours[0].frame().turnY(0, 1, 0),
                    ours[0].frame().turnZ(0, 1, 0)};
            helper.assertTrue(up[1] < -0.99, "turned to face the other's collar, collar to collar: " + up[1]);
            final ShipReadings.Pilot shown = console(ours[0]).flight().pilot();
            helper.assertTrue(shown.mode().equals("dock") && shown.aligned() && shown.range() < 1, "its console"
                    + " shows the docking cross, in line: " + shown);
        }).thenWaitUntil(() -> helper.assertTrue(ShipDocks.docked(ours[0]), "collar to collar, the two are made"
                + " one"))
                .thenExecuteAfter(HELD, () -> {
                    helper.assertTrue(flight(ours[0]).autopilot().isEmpty(), "docked, its autopilot is done");
                    helper.assertTrue(ShipCollars.mouth(space, ours[0]).orElseThrow().distanceTo(ShipCollars.mouth(
                            space, theirs[0]).orElseThrow()) < TOUCHING, "and held collar to collar");
                    // Its pilot gets up and goes through the two collars into the other ship, and back.
                    pilot[0].stopRiding();
                    helper.assertTrue(ShipDocks.passThrough(pilot[0], ours[0]), "its pilot goes through the collars");
                    helper.assertTrue(underCollar(space, theirs[0], pilot[0]), "into the other ship, under its"
                            + " collar: " + MovingStructures.toYard(theirs[0], pilot[0].position()));
                    helper.assertTrue(ShipDocks.passThrough(pilot[0], theirs[0]), "and back through them");
                    helper.assertTrue(underCollar(space, ours[0], pilot[0]), "into their own ship, under its collar: "
                            + MovingStructures.toYard(ours[0], pilot[0].position()));
                    helper.assertTrue(ShipDocks.undock(ours[0]) && !ShipDocks.docked(ours[0]), "let go, it is its"
                            + " own again");
                })
                .thenExecuteAfter(HELD, () -> {
                    helper.assertTrue(ShipCollars.mouth(space, ours[0]).orElseThrow().distanceTo(ShipCollars.mouth(
                            space, theirs[0]).orElseThrow()) > TOUCHING, "and clear of the other");
                    helper.assertTrue(ours[0].motion() instanceof ShipFlight flown && flown.autopilot().isEmpty(),
                            "flying by its own flight again: " + ours[0].motion());
                    GameTestPlayers.leave(pilot[0]);
                    MovingStructures.takeAway(ours[0]);
                    MovingStructures.takeAway(theirs[0]);
                    keepSpace(helper, space, far, false);
                })
                .thenSucceed();
    }

    /* Whether {@code player} stands inside {@code ship}, in its yard, under its collar. */
    private static boolean underCollar(final ServerLevel level, final MovingStructure ship,
                                       final ServerPlayer player) {
        final Vec3 at = MovingStructures.toYard(ship, player.position());
        final Vec3 mouth = ShipCollars.inYard(level, ship).orElseThrow();
        final Box bounds = ship.bounds();
        return at.x >= bounds.minX() && at.x <= bounds.maxX() + 1 && at.z >= bounds.minZ()
                && at.z <= bounds.maxZ() + 1 && at.y >= bounds.minY() && at.y < mouth.y
                && Math.hypot(at.x - mouth.x, at.z - mouth.z) < UNDER;
    }

    /* The dock built, a ship of {@code design} built in it and let go, {@code ship} told it once it came in. */
    private static GameTestSequence letGo(final GameTestHelper helper, final MovingStructure[] ship,
                                          final ShipDesign design) {
        final ServerLevel level = helper.getLevel();
        final BlockPos terminal = TestShipyards.build(level, helper.absolutePos(CORNER));
        final GameTestSequence sequence = helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(level.getBlockEntity(terminal)
                        instanceof ShipyardTerminalBlockEntity dock && dock.formed(), "the dock forms"));
        return build(helper, sequence, ship, design);
    }

    /* Another ship of {@code design} built in the dock and let go, {@code ship} told it once it came in. */
    private static GameTestSequence build(final GameTestHelper helper, final GameTestSequence sequence,
                                          final MovingStructure[] ship, final ShipDesign design) {
        final ServerLevel level = helper.getLevel();
        final BlockPos terminal = terminal(helper);
        return sequence.thenExecute(() -> {
            final ShipyardTerminalBlockEntity dock = (ShipyardTerminalBlockEntity) level.getBlockEntity(terminal);
            dock.design(design);
            helper.assertTrue(dock.confirm(), "the ship is sent");
            final PlannedBuild plan = dock.build().orElseThrow();
            for (final PlanSpot spot : plan.plan().spots()) {
                level.setBlockAndUpdate(plan.where(spot), BuiltInRegistries.BLOCK.get(spot.block())
                        .defaultBlockState());
            }
            ship[0] = dock.release(null).orElseThrow();
        }).thenWaitUntil(() -> helper.assertTrue(ship[0].ready(), "its blocks come into the structure"));
    }

    /*
     * The top block of the land, a way north of {@code at}, the land bare and level round it as far as a ship sets
     * down on it: the nearest such place, a platform's length apart, within the land the test keeps.
     */
    private static BlockPos bareNorthOf(final GameTestHelper helper, final StructurePose at) {
        final ServerLevel level = helper.getLevel();
        final int x = Mth.floor(at.x());
        for (int north = PLATFORM_NORTH; north <= PLATFORM_FARTHEST; north += PLATFORM_STEP) {
            final int z = Mth.floor(at.z()) - north;
            final int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            boolean bare = true;
            for (int dx = -BARE_ACROSS; dx <= BARE_ACROSS && bare; dx++) {
                for (int dz = -BARE_ALONG; dz <= BARE_ALONG && bare; dz++) {
                    bare = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x + dx, z + dz) == ground;
                }
            }
            if (bare) {
                return new BlockPos(x, ground - 1, z);
            }
        }
        throw new IllegalStateException("no bare land north of the dock to set the pad on");
    }

    /*
     * A runway laid in the sky a way north of {@code at}, as high as the ship flies there, clear of whatever the tests
     * beside this one stand on the land: a floor of stone from a little behind its beacon to the end of its stretch,
     * as wide as the stretch, its beacon on it facing north, the way it runs; where the beacon stands.
     */
    private static BlockPos skyRunwayNorthOf(final ServerLevel level, final StructurePose at) {
        final BlockPos beacon = new BlockPos(Mth.floor(at.x()), Mth.floor(at.y()), Mth.floor(at.z()) - RUNWAY_NORTH);
        for (int along = -RUNWAY_BEHIND; along <= ShipAutopilots.RUNWAY_LENGTH; along++) {
            for (int across = -Runways.HALF_WIDTH; across <= Runways.HALF_WIDTH; across++) {
                level.setBlockAndUpdate(beacon.offset(across, -1, -along), Blocks.STONE.defaultBlockState());
            }
        }
        level.setBlockAndUpdate(beacon, ShipLandingBlocks.BEACON.get().defaultBlockState()
                .setValue(RunwayBeaconBlock.FACING, Direction.NORTH));
        return beacon;
    }

    /* Takes the sky runway whose beacon stands at {@code beacon} away. */
    private static void takeAwayRunway(final ServerLevel level, final BlockPos beacon) {
        level.removeBlock(beacon, false);
        for (int along = -RUNWAY_BEHIND; along <= ShipAutopilots.RUNWAY_LENGTH; along++) {
            for (int across = -Runways.HALF_WIDTH; across <= Runways.HALF_WIDTH; across++) {
                level.removeBlock(beacon.offset(across, -1, -along), false);
            }
        }
    }

    /* A player connected and sat at the seat {@code seat} of a ship's yard, playing as a survivor. */
    private static ServerPlayer seated(final GameTestHelper helper, final BlockPos seat) {
        final ServerPlayer pilot = GameTestPlayers.connected(helper, "pilot");
        pilot.setGameMode(GameType.SURVIVAL);
        helper.assertTrue(MovingStructures.sit(pilot, seat), "the pilot sits at its helm");
        return pilot;
    }

    private static ShipFlight flight(final MovingStructure ship) {
        return (ShipFlight) ship.motion();
    }

    /* {@code ship}'s flight console, which shows its flight. */
    private static AirReserveEntity console(final MovingStructure ship) {
        return (AirReserveEntity) ship.level().getBlockEntity(flight(ship).consoleIn(ship).orElseThrow());
    }

    /* Takes {@code ship} straight up by {@code blocks}. */
    private static void aloft(final MovingStructure ship, final double blocks) {
        final StructurePose now = ship.pose();
        ship.teleport(now.movedTo(now.x(), now.y() + blocks, now.z()));
    }

    /* Whether the land round the dock and on {@code north} chunks to its north has loaded. */
    private static void loaded(final GameTestHelper helper, final int north) {
        final ChunkPos dock = new ChunkPos(helper.absolutePos(CORNER));
        for (int x = dock.x - WEST_CHUNKS; x <= dock.x + 1; x++) {
            for (int z = dock.z - north; z <= dock.z + 1; z++) {
                helper.assertTrue(helper.getLevel().getChunkSource().getChunkNow(x, z) != null, "the land it flies"
                        + " over loads");
            }
        }
    }

    /*
     * Keeps the land round the dock and on {@code north} chunks to its north loaded and ticking by this test's own
     * tickets, or lets go.
     */
    private static void keep(final GameTestHelper helper, final boolean kept, final int north) {
        final ChunkPos dock = new ChunkPos(helper.absolutePos(CORNER));
        final BlockPos own = helper.absolutePos(CORNER);
        for (int x = dock.x - WEST_CHUNKS; x <= dock.x + 1; x++) {
            for (int z = dock.z - north; z <= dock.z + 1; z++) {
                ticket(helper.getLevel(), new ChunkPos(x, z), own, kept);
            }
        }
    }

    /* Keeps space round {@code far} loaded and ticking, where the two ships dock, or lets it go. */
    private static void keepSpace(final GameTestHelper helper, final ServerLevel space, final BlockPos far,
                                  final boolean kept) {
        final ChunkPos middle = new ChunkPos(far);
        final BlockPos own = helper.absolutePos(CORNER);
        for (int x = middle.x - SPACE_CHUNKS; x <= middle.x + SPACE_CHUNKS; x++) {
            for (int z = middle.z - SPACE_CHUNKS; z <= middle.z + SPACE_CHUNKS; z++) {
                ticket(space, new ChunkPos(x, z), own, kept);
            }
        }
    }

    private static void ticket(final ServerLevel level, final ChunkPos chunk, final BlockPos own, final boolean kept) {
        if (kept) {
            level.getChunkSource().addRegionTicket(LAND, chunk, TICKING, own);
        } else {
            level.getChunkSource().removeRegionTicket(LAND, chunk, TICKING, own);
        }
    }

    /* Where the block that stood at {@code built} in the dock is aboard {@code ship} now, in its yard. */
    private static BlockPos aboard(final MovingStructure ship, final BlockPos built) {
        return BlockPos.containing(MovingStructures.toYard(ship, Vec3.atCenterOf(built)));
    }

    private static BlockPos terminal(final GameTestHelper helper) {
        return helper.absolutePos(CORNER).offset(21, 7, 23);
    }

    /* The plan of a ship of {@code design} in the dock, where it was built. */
    private static PlannedBuild plan(final GameTestHelper helper, final ShipDesign design) {
        return new PlannedBuild(ShipyardPlans.of(helper.getLevel(), design, Orientation.NORTH),
                ShipyardStructure.shipAt(terminal(helper), Orientation.NORTH));
    }

    /* The first place of the plan of a ship of {@code design} that wants {@code block}. */
    private static PlanSpot spot(final GameTestHelper helper, final ShipDesign design, final ResourceLocation block) {
        return plan(helper, design).plan().spots().stream().filter(each -> each.block().equals(block)).findFirst()
                .orElseThrow();
    }

    private static ResourceLocation part(final String path) {
        return SmallShipParts.id("small_" + path);
    }
}
