/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import dev.jstech.core.dimension.RuntimeDimensions;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.StructureControls;
import dev.jstech.core.moving.StructurePose;
import dev.jstech.core.moving.StructureSeatEntity;
import dev.jstech.core.multiblock.Orientation;
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.multiblock.plan.PlannedBuild;
import dev.jstech.core.team.Ownership;
import dev.jstech.space.api.SpaceWorlds;
import dev.jstech.space.landing.RunwayBeaconBlock;
import dev.jstech.space.landing.Runways;
import dev.jstech.space.landing.ShipLandingBlocks;
import dev.jstech.space.landing.ShipLandings;
import dev.jstech.space.ship.AirReserveEntity;
import dev.jstech.space.ship.PulseCourse;
import dev.jstech.space.ship.ShipAutopilot;
import dev.jstech.space.ship.ShipAutopilotControl;
import dev.jstech.space.ship.ShipAutopilots;
import dev.jstech.space.ship.ShipDesign;
import dev.jstech.space.ship.ShipFlight;
import dev.jstech.space.ship.ShipFlightModel;
import dev.jstech.space.ship.ShipKitBlocks;
import dev.jstech.space.ship.ShipPulse;
import dev.jstech.space.ship.SmallShipParts;
import dev.jstech.space.shipyard.ShipyardPlans;
import dev.jstech.space.shipyard.ShipyardStructure;
import dev.jstech.space.shipyard.ShipyardTerminalBlockEntity;
import dev.jstech.space.universe.SolarBody;
import dev.jstech.space.universe.SolarSystemWorlds;
import dev.jstech.space.universe.SpaceClimbs;
import dev.jstech.space.universe.StarSystems;
import dev.jstech.space.universe.SystemLayout;
import dev.jstech.space.universe.SystemSpaces;
import dev.jstech.testkit.gametest.GameTestPlayers;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.space.TestShipyards;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestSequence;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A starship's trips between the worlds, end to end, its pilot at the helm all the way: built in the shipyard and let
 * go, it rises off its cradle, ORBIT takes it up into the Solar System's space and levels it onto the low orbits; its
 * pulse jump takes it across to another world, dropping out over that world's low orbit by itself; flown down under
 * the floor of the low orbits it comes into that world, where LAND sets it down, on its pilot's pad on feet and on a
 * runway on wheels; it rises off it and ORBIT takes it up again, on to the next world and home the same way.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ShipRoutesGameTests {

    private static final String ARENA = "shipyard";
    /* Each its own batch: the ships go to the same places of space and of the worlds. */
    private static final String PADS_BATCH = "jsspace_ship_routes_pads";
    private static final String RUNWAYS_BATCH = "jsspace_ship_routes_runways";
    /* How long a trip to the Moon, on to Mars and home takes at the most, in ticks. */
    private static final int TRIP = 72_000;
    /* Where the dock's lowest corner stands in the arena. */
    private static final BlockPos CORNER = new BlockPos(1, 2, 1);
    /* How long its pilot holds the helm's UP as it rises off the land, in ticks, and how far it rises at least. */
    private static final int RISING = 40;
    private static final double ROSE = 1;
    /* How high the ship is taken before ORBIT is engaged, over the arenas north of its own, in blocks. */
    private static final double OVER_THE_ARENAS = 130;
    /*
     * How fast the ship flies once it is set going in space, in metres a second; how fast, and how steeply, it dives
     * down into a world, slow enough to hover soon after it comes into it; and the speed it hovers under there.
     */
    private static final double CRUISING = 40;
    private static final double DIVING = 12;
    private static final double DIVE = -45;
    private static final double HOVERING = 5;
    /*
     * How far into a world's low orbit it flies before it dives, as a share of the orbit's radius: well inside the
     * square of the world's places laid under the orbit, whose edges are the world's border.
     */
    private static final double INWARD = 0.5;
    /*
     * How far short of a world's low orbit the ship starts its jump towards it, south of it, in blocks: clear of every
     * other world's low orbit, where a jump may not begin, the Overworld's lying not far south of the Moon's.
     */
    private static final double SHORT_OF_THE_ORBIT = 2_000;
    /*
     * How high over the land under it its pilot's pad, or a runway, is laid, in blocks, clear of the land's hills; the
     * ship comes into a world far over it, under the world's top. How far north of the ship the pad stands, and how
     * near over it the ship hovers down before its pilot engages LAND, within the range LAND reaches.
     */
    private static final int OVER_THE_LAND = 24;
    private static final int PAD_NORTH = 56;
    private static final int LAND_FROM = 300;
    /* How far round its middle a pad's floor reaches, across and along; how near its middle the ship sets down. */
    private static final int BARE_ACROSS = 6;
    private static final int BARE_ALONG = 11;
    private static final double ON_THE_SPOT = 3;
    /*
     * How far north of the ship a runway's beacon stands, the runway running north, and how far its floor reaches
     * behind the beacon; how near over the runway the ship hovers down before its pilot engages LAND.
     */
    private static final int RUNWAY_NORTH = 150;
    private static final int RUNWAY_BEHIND = 8;
    private static final int OVER_THE_RUNWAY = 6;
    /* How many times its autopilot's key is pressed at most to come to the mode wanted. */
    private static final int PRESSES = 3;
    private static final double SPEED_A_TICK = PulseCourse.SPEED * ShipFlightModel.TICK;
    /*
     * The tickets keeping the places the ship is set at in space, and comes into a world at, ticking, and how far
     * round them, in chunks; in a world, centred a way north of where it came in, past its pad and its runway, and the
     * way LAND flies it to either.
     */
    private static final TicketType<BlockPos> KEPT = TicketType.create("jstests_ship_routes", Vec3i::compareTo);
    private static final int TICKING = 2;
    private static final int SPACE_KEPT = 3;
    private static final int WORLD_KEPT = 10;
    private static final int KEPT_NORTH = 96;
    /* How far round its pilot seats are looked for, for a failure to say, in blocks. */
    private static final double SEATS_NEAR = 16;
    private static final ShipDesign LEGS_SHIP = new ShipDesign("JS-04", List.of(part("orbiter_cockpit"),
            part("airlock"), part("quarters"), part("workshop"), part("reactor_flush")), List.of(part("twin_bells"),
            part("side_intakes"), part("blended_delta"), part("twin_canted_fins"), part("blisters"), part("legs")));
    private static final ShipDesign WHEELS_SHIP = new ShipDesign("JS-07", List.of(part("orbiter_cockpit"),
            part("airlock"), part("quarters"), part("workshop"), part("reactor_flush")), List.of(part("twin_bells"),
            part("side_intakes"), part("blended_delta"), part("twin_canted_fins"), part("blisters"), part("wheels")));

    private ShipRoutesGameTests() {
    }

    /**
     * On feet, from the shipyard to the Moon, on to Mars and home, setting down on its pilot's pad on each world.
     */
    @GameTest(template = ARENA, batch = PADS_BATCH, timeoutTicks = TRIP)
    public static void ship_onFeetFliesToTheMoonAndMarsAndHomeLandingOnItsPilotsPads(final GameTestHelper helper) {
        trip(helper, LEGS_SHIP, ShipRoutesGameTests::landOnAPad);
    }

    /**
     * On wheels, from the shipyard to the Moon, on to Mars and home, setting down on a runway on each world.
     */
    @GameTest(template = ARENA, batch = RUNWAYS_BATCH, timeoutTicks = TRIP)
    public static void ship_onWheelsFliesToTheMoonAndMarsAndHomeLandingOnRunways(final GameTestHelper helper) {
        trip(helper, WHEELS_SHIP, ShipRoutesGameTests::landOnARunway);
    }

    /*
     * The ship of {@code design} built and let go, its pilot sits at its helm and holds UP: it rises off its cradle,
     * and ORBIT takes it into space. It jumps across to the Moon, comes down into it and {@code landing} sets it down
     * there; it rises off the land, ORBIT takes it up, and so on to Mars and home.
     */
    private static void trip(final GameTestHelper helper, final ShipDesign design, final Landing landing) {
        final ServerLevel overworld = helper.getLevel();
        final ServerLevel space = SystemSpaces.spaceOf(overworld.getServer(), StarSystems.SOLAR_SYSTEM);
        final MovingStructure[] ship = new MovingStructure[1];
        final ServerPlayer[] pilot = new ServerPlayer[1];
        final List<Runnable> kept = new ArrayList<>();
        GameTestSequence sequence = letGo(helper, design, ship).thenExecute(() -> {
            pilot[0] = GameTestPlayers.connected(helper, "ship_route_pilot");
            pilot[0].setGameMode(GameType.SURVIVAL);
            final BlockPos seat = aboard(ship[0], plan(helper, design).where(spot(helper, design,
                    ShipKitBlocks.PILOT_SEAT.getId())));
            helper.assertTrue(MovingStructures.sit(pilot[0], seat), "the pilot sits at its helm");
        });
        sequence = rise(helper, sequence, ship, pilot, "its cradle").thenExecute(() -> {
            // Taken over the arenas laid north of its own, which it would fly into, ORBIT is engaged.
            final StructurePose now = ship[0].pose();
            setAt(helper, ship[0], pilot[0], now.movedTo(now.x(), now.y() + OVER_THE_ARENAS, now.z()));
            engage(helper, ship[0], pilot[0], ShipAutopilot.Mode.ORBIT);
        }).thenExecuteAfter(1, () -> seated(helper, ship[0], pilot[0], "taken over the arenas"));
        sequence = orbit(helper, sequence, ship, pilot, overworld, space);
        ServerLevel from = overworld;
        for (final SolarBody to : List.of(SolarBody.MOON, SolarBody.MARS, SolarBody.OVERWORLD)) {
            final ServerLevel world = to == SolarBody.OVERWORLD ? overworld : levelOf(space, to);
            if (from != overworld) {
                sequence = rise(helper, sequence, ship, pilot, "the land of " + from.dimension().location())
                        .thenExecute(() -> engage(helper, ship[0], pilot[0], ShipAutopilot.Mode.ORBIT));
                sequence = orbit(helper, sequence, ship, pilot, from, space);
            }
            sequence = across(helper, sequence, ship, pilot, to, kept);
            sequence = down(helper, sequence, ship, pilot, world, to, kept);
            sequence = landing.on(helper, sequence, ship, pilot, world, kept);
            from = world;
        }
        sequence.thenExecute(() -> {
            kept.forEach(Runnable::run);
            GameTestPlayers.leave(pilot[0]);
            MovingStructures.takeAway(ship[0]);
        }).thenSucceed();
    }

    /*
     * Its pilot holds UP, the ship standing on {@code what}: it rises straight off it, and they let go of UP, else the
     * autopilot's key engaged next would be let go at once, the helm held.
     */
    private static GameTestSequence rise(final GameTestHelper helper, final GameTestSequence sequence,
                                         final MovingStructure[] ship, final ServerPlayer[] pilot,
                                         final String what) {
        final double[] stood = new double[1];
        GameTestSequence rising = sequence.thenExecute(() -> stood[0] = ship[0].pose().y());
        for (int tick = 0; tick < RISING; tick++) {
            rising = rising.thenExecuteAfter(1, () -> held(ship[0], pilot[0],
                    Map.of(StructureControls.ControlAxis.UP, 1F)));
        }
        return rising.thenExecute(() -> {
            helper.assertTrue(ship[0].pose().y() > stood[0] + ROSE && !flight(ship[0]).grounded(),
                    "holding UP it rises off " + what + ": from " + stood[0] + " to " + ship[0].pose().y());
            held(ship[0], pilot[0], Map.of());
        });
    }

    /*
     * ORBIT climbs the ship out of {@code world}, its pilot holding nothing, into the Solar System's {@code space},
     * and levels it onto the plane of the low orbits, letting go there.
     */
    private static GameTestSequence orbit(final GameTestHelper helper, final GameTestSequence sequence,
                                          final MovingStructure[] ship, final ServerPlayer[] pilot,
                                          final ServerLevel world, final ServerLevel space) {
        return sequence.thenWaitUntil(() -> {
            final MovingStructure there = MovingStructures.byId(space, ship[0].id());
            if (there == null) {
                held(ship[0], pilot[0], Map.of());
            }
            helper.assertTrue(there != null && there.ready(), "ORBIT climbs it out of " + world.dimension()
                    .location() + " into space: at " + ship[0].pose().y() + ", " + state(ship[0]));
            ship[0] = there;
        }).thenExecuteAfter(1, () -> seated(helper, ship[0], pilot[0], "climbed into space"))
                .thenWaitUntil(() -> {
            held(ship[0], pilot[0], Map.of());
            helper.assertTrue(flight(ship[0]).autopilot().isEmpty() && Math.abs(ship[0].pose().y()
                    - SystemLayout.ORBIT_Y) < SystemLayout.ORBIT_Y, "and levels it onto the low orbits, letting go: "
                    + ship[0].pose() + ", " + state(ship[0]));
        });
    }

    /*
     * The ship set south of {@code to}'s low orbit, its nose north towards it, at its cruise: its pilot holds its pulse
     * jump's key till its drive spools, and it jumps, dropping out over {@code to}'s low orbit by itself.
     */
    private static GameTestSequence across(final GameTestHelper helper, final GameTestSequence sequence,
                                           final MovingStructure[] ship, final ServerPlayer[] pilot,
                                           final SolarBody to, final List<Runnable> kept) {
        final double[] middle = SystemLayout.centre(to);
        final double reach = SystemLayout.radius(to) * (1 + SystemLayout.LOW_ORBIT);
        final BlockPos start = BlockPos.containing(middle[0], SystemLayout.ORBIT_Y, middle[2] + reach
                + SHORT_OF_THE_ORBIT);
        // Its place there ticking first, as it would round a ship flown there, not set there.
        sequence.thenExecute(() -> keep(helper, (ServerLevel) ship[0].level(), start, SPACE_KEPT, kept))
                .thenWaitUntil(() -> helper.assertTrue(ticking((ServerLevel) ship[0].level(), start, SPACE_KEPT),
                        "the place it is set at ticks"));
        sequence.thenExecute(() -> {
            // Its nose level and north, as it was built: wherever it flew before, it jumps towards the world.
            setAt(helper, ship[0], pilot[0], StructurePose.at(middle[0], SystemLayout.ORBIT_Y, middle[2] + reach
                    + SHORT_OF_THE_ORBIT));
            MovingStructures.move(ship[0], flight(ship[0]).with(new ShipFlightModel.State(CRUISING, 0, 1, 0, 0)));
            helper.assertTrue(ShipPulse.mayJump((ServerLevel) ship[0].level(), ship[0], ship[0].pose()),
                    "out of every world's low orbit, it may jump: " + ship[0].pose());
        });
        sequence.thenExecuteAfter(1, () -> seated(helper, ship[0], pilot[0], "set south of " + to));
        for (int tick = 0; tick <= ShipPulse.SPOOL_TICKS + 1; tick++) {
            sequence.thenExecuteAfter(1, () -> {
                held(ship[0], pilot[0], Map.of());
                ShipPulse.hold(ship[0], ship[0].level().getGameTime());
            });
        }
        return sequence.thenWaitUntil(() -> {
            held(ship[0], pilot[0], Map.of());
            final StructurePose now = ship[0].pose();
            helper.assertTrue(flight(ship[0]).pulse().isEmpty() && Math.abs(now.z() - (middle[2] + reach))
                    < 2 * SPEED_A_TICK && Math.abs(now.x() - middle[0]) < 1, "its pulse jump takes it across to "
                    + to + ", dropping out over its low orbit by itself: " + now + ", " + flight(ship[0]).pulse()
                    + ", its console showing " + console(ship[0]).flight().pulse() + ", deuterium "
                    + console(ship[0]).deuterium() + ", may jump " + ShipPulse.mayJump((ServerLevel) ship[0].level(),
                    ship[0], now) + ", grounded " + flight(ship[0]).grounded());
        }).thenExecute(() -> seated(helper, ship[0], pilot[0], "dropped out over " + to + "'s low orbit"));
    }

    /*
     * The ship flies on north into {@code to}'s low orbit, over the middle of the world under it, and dives slowly
     * from there, its nose pitched down, under the floor of the low orbits into {@code world}, its pilot with it at
     * its helm; there its pilot holds its throttle back till it hovers, and the land round it loads, as round a
     * player's ship it would have loaded before they came to it: the test's server ticks faster than land is made.
     */
    private static GameTestSequence down(final GameTestHelper helper, final GameTestSequence sequence,
                                         final MovingStructure[] ship, final ServerPlayer[] pilot,
                                         final ServerLevel world, final SolarBody to, final List<Runnable> kept) {
        final double[] middle = SystemLayout.centre(to);
        final double inner = SystemLayout.radius(to) * (1 + SystemLayout.LOW_ORBIT) * INWARD;
        final BlockPos[] came = new BlockPos[1];
        return sequence.thenWaitUntil(() -> {
            held(ship[0], pilot[0], Map.of());
            helper.assertTrue(Math.hypot(ship[0].pose().x() - middle[0], ship[0].pose().z() - middle[2]) < inner,
                    "it flies on into " + to + "'s low orbit: at " + ship[0].pose() + ", " + state(ship[0]));
        }).thenExecute(() -> {
            final StructurePose now = ship[0].pose();
            ship[0].teleport(now.turnedBy(1, 0, 0, Math.toRadians(DIVE)));
            MovingStructures.move(ship[0], flight(ship[0]).with(new ShipFlightModel.State(DIVING, 0,
                    DIVING / ShipFlightModel.cruiseIn(0), 0, 0)));
        }).thenWaitUntil(() -> {
            final MovingStructure there = MovingStructures.byId(world, ship[0].id());
            if (there == null) {
                held(ship[0], pilot[0], Map.of());
            }
            helper.assertTrue(there != null && there.ready(), "diving under the low orbits' floor it comes into "
                    + to + ": at " + ship[0].pose() + " in " + ship[0].level().dimension().location() + ", floor "
                    + SpaceClimbs.floor());
            ship[0] = there;
        }).thenExecute(() -> {
            seated(helper, ship[0], pilot[0], "come down into " + to);
            came[0] = BlockPos.containing(ship[0].pose().x(), ship[0].pose().y(), ship[0].pose().z() - KEPT_NORTH);
            keep(helper, world, came[0], WORLD_KEPT, kept);
        }).thenWaitUntil(() -> {
            final boolean hovers = flight(ship[0]).state().speed() < HOVERING;
            held(ship[0], pilot[0], hovers ? Map.of() : Map.of(StructureControls.ControlAxis.BACK, 1F));
            helper.assertTrue(hovers && ticking(world, came[0], WORLD_KEPT), "come into " + to + ", it slows till it"
                    + " hovers, the land round it loaded: " + state(ship[0]));
        });
    }

    /*
     * {@code ship} set at {@code pose}, not flown there, its pilot set back at its helm with it: a structure set far
     * from where it was leaves who sat aboard behind.
     */
    private static void setAt(final GameTestHelper helper, final MovingStructure ship, final ServerPlayer pilot,
                              final StructurePose pose) {
        final StructureSeatEntity sat = pilot.getVehicle() instanceof StructureSeatEntity seat ? seat : null;
        final BlockPos helm = sat == null ? null : sat.seat();
        final String before = sat == null ? "no seat" : "seat on " + sat.structure() + " in " + sat.level()
                .dimension().location() + ", there before " + MovingStructures.at((ServerLevel) ship.level(), helm)
                + ", the ship's yard " + ship.bounds();
        pilot.stopRiding();
        // Its empty seat goes now, not at its next tick, so the helm is free to sit at again.
        if (sat != null) {
            sat.discard();
        }
        ship.teleport(pose);
        pilot.teleportTo((ServerLevel) ship.level(), pose.x(), pose.y(), pose.z(), 0, 0);
        final ServerLevel level = (ServerLevel) ship.level();
        helper.assertTrue(helm != null && MovingStructures.sit(pilot, helm), "set with it, its pilot sits at its"
                + " helm again: " + helm + "; riding " + pilot.isPassenger() + ", in " + pilot.level().dimension()
                .location() + " for " + level.dimension().location() + ", a structure there " + (helm == null ? null
                : MovingStructures.at(level, helm)) + ", seats near it " + (helm == null ? 0 : level.getEntities(
                (Entity) null, new AABB(pilot.position(), pilot.position()).inflate(SEATS_NEAR),
                any -> any instanceof StructureSeatEntity).size()) + "; " + before);
    }

    /* That {@code pilot} sits at {@code ship}'s helm still, {@code when}. */
    private static void seated(final GameTestHelper helper, final MovingStructure ship, final ServerPlayer pilot,
                               final String when) {
        helper.assertTrue(pilot.getVehicle() instanceof StructureSeatEntity seat && seat.helm()
                && seat.structure() == ship, when + ", its pilot sits at its helm still: on " + pilot.getVehicle()
                + " in " + pilot.level().dimension().location() + ", the ship in "
                + ship.level().dimension().location());
    }

    /*
     * A pad of its pilot's on a floor a way north of where the ship came into {@code world}, over the land: the ship
     * hovers down toward it, the key engages LAND to it, and the ship flies there and sets down on its middle, letting
     * go once it stands. The pad and its floor are taken away with what the test {@code kept}.
     */
    private static GameTestSequence landOnAPad(final GameTestHelper helper, final GameTestSequence sequence,
                                               final MovingStructure[] ship, final ServerPlayer[] pilot,
                                               final ServerLevel world, final List<Runnable> kept) {
        final UUID pad = UUID.randomUUID();
        final BlockPos[] floor = new BlockPos[1];
        final Vec3[] spot = new Vec3[1];
        return sequence.thenExecute(() -> {
            floor[0] = padNorthOf(world, ship[0].pose());
            ShipLandings.notePad(world, pad, floor[0], Ownership.of(pilot[0].getUUID()));
            kept.add(() -> {
                ShipLandings.dropPad(world.getServer(), pad);
                takeAwayPad(world, floor[0]);
            });
        }).thenWaitUntil(() -> {
            // Hovering down from where it came in, its pilot holding DOWN, till its pad is near enough for LAND.
            held(ship[0], pilot[0], Map.of(StructureControls.ControlAxis.DOWN, 1F));
            helper.assertTrue(ship[0].pose().y() < floor[0].getY() + LAND_FROM, "it comes down toward the land: at "
                    + ship[0].pose() + ", " + state(ship[0]));
        }).thenExecute(() -> {
            held(ship[0], pilot[0], Map.of());
            spot[0] = ShipLandings.padSpot(world, ShipLandings.pad(world.getServer(), pad).orElseThrow());
            engage(helper, ship[0], pilot[0], ShipAutopilot.Mode.LAND);
        }).thenWaitUntil(() -> {
            held(ship[0], pilot[0], Map.of());
            helper.assertTrue(ship[0].motion() instanceof ShipFlight flying && flying.autopilot().isEmpty()
                    && flying.grounded(), "LAND sets it down on its pilot's pad in " + world.dimension().location()
                    + ": at " + ship[0].pose() + " for " + spot[0] + ", moved by " + ship[0].motion() + ", in "
                    + ship[0].level().dimension().location() + ", gone " + ship[0].entity().isRemoved()
                    + ", under it " + under(world, ship[0]));
        }).thenExecute(() -> {
            final StructurePose at = ship[0].pose();
            helper.assertTrue(Math.hypot(at.x() - spot[0].x, at.z() - spot[0].z) < ON_THE_SPOT, "on the pad's"
                    + " middle: " + at + " for " + spot[0]);
        });
    }

    /*
     * A runway laid a way north of where the ship came into {@code world}, over the land, running north from its
     * beacon: the ship hovers down near it, the key engages LAND onto it, its stretch found clear, and the ship lines
     * up on it, sets down past the beacon and rolls to a stop on its stretch, letting go. The runway is taken away with
     * what the test {@code kept}.
     */
    private static GameTestSequence landOnARunway(final GameTestHelper helper, final GameTestSequence sequence,
                                                  final MovingStructure[] ship, final ServerPlayer[] pilot,
                                                  final ServerLevel world, final List<Runnable> kept) {
        final BlockPos[] beacon = new BlockPos[1];
        return sequence.thenExecute(() -> {
            beacon[0] = runwayNorthOf(world, ship[0].pose());
            kept.add(() -> takeAwayRunway(world, beacon[0]));
        }).thenWaitUntil(() -> {
            // Hovering down from where it came in, its pilot holding DOWN, till it flies as high as the runway.
            held(ship[0], pilot[0], Map.of(StructureControls.ControlAxis.DOWN, 1F));
            helper.assertTrue(ship[0].worldBox().minY < beacon[0].getY() + OVER_THE_RUNWAY, "it comes down toward"
                    + " the runway: at " + ship[0].worldBox().minY + " for " + beacon[0] + ", " + state(ship[0]));
        }).thenExecute(() -> {
            held(ship[0], pilot[0], Map.of());
            engage(helper, ship[0], pilot[0], ShipAutopilot.Mode.LAND);
            final ShipAutopilot engaged = flight(ship[0]).autopilot().orElseThrow();
            helper.assertTrue(engaged.runway().isPresent() && engaged.cleared() && engaged.spot().orElseThrow()
                    .equals(Vec3.atBottomCenterOf(beacon[0])), "on wheels, LAND takes the runway, its stretch found"
                    + " clear: " + engaged);
        }).thenWaitUntil(() -> {
            held(ship[0], pilot[0], Map.of());
            helper.assertTrue(flight(ship[0]).autopilot().isEmpty() && flight(ship[0]).grounded(), "LAND lines it"
                    + " up on the runway in " + world.dimension().location() + ", sets it down past the beacon and"
                    + " rolls it to a stop: at " + ship[0].pose() + " over " + ship[0].worldBox().minY + " for the"
                    + " beacon at " + beacon[0] + ", " + state(ship[0]) + ", under it " + under(world, ship[0]));
        }).thenExecute(() -> {
            final StructurePose at = ship[0].pose();
            final double along = beacon[0].getZ() + 0.5 - at.z();
            helper.assertTrue(along > 0 && along <= ShipAutopilots.RUNWAY_LENGTH
                    && Math.abs(at.x() - beacon[0].getX() - 0.5) < ON_THE_SPOT, "stopped on the runway's stretch, on"
                    + " its line: " + along + " along, " + (at.x() - beacon[0].getX() - 0.5) + " across");
        });
    }

    /* The first block that is not air under the middle of {@code ship}'s hull, for a failure to say. */
    private static String under(final ServerLevel world, final MovingStructure ship) {
        final BlockPos.MutableBlockPos at = BlockPos.containing(ship.pose().x(), ship.worldBox().minY - 1,
                ship.pose().z()).mutable();
        while (at.getY() > world.getMinBuildHeight() && world.getBlockState(at).isAir()) {
            at.move(0, -1, 0);
        }
        return world.getBlockState(at) + " at " + at.toShortString();
    }

    /* Its pilot presses the autopilot's key till {@code mode} is engaged, a few times at most. */
    private static void engage(final GameTestHelper helper, final MovingStructure ship, final ServerPlayer pilot,
                               final ShipAutopilot.Mode mode) {
        for (int press = 0; press < PRESSES && flight(ship).autopilot().map(ShipAutopilot::mode)
                .filter(mode::equals).isEmpty(); press++) {
            ShipAutopilotControl.pressed(pilot);
        }
        helper.assertTrue(flight(ship).autopilot().map(ShipAutopilot::mode).filter(mode::equals).isPresent(),
                "its key at the helm engages " + mode + ": " + flight(ship).autopilot() + "; its pilot on "
                        + pilot.getVehicle() + " in " + pilot.level().dimension().location() + ", the ship in "
                        + ship.level().dimension().location() + " at " + ship.pose() + ", pads its pilot may use there "
                        + ShipLandings.padsUsableBy(pilot, (ServerLevel) ship.level()).size());
    }

    /* A ship of {@code design} built in the dock and let go, {@code ship} told it once it came in. */
    private static GameTestSequence letGo(final GameTestHelper helper, final ShipDesign design,
                                          final MovingStructure[] ship) {
        final ServerLevel level = helper.getLevel();
        final BlockPos terminal = TestShipyards.build(level, helper.absolutePos(CORNER));
        return helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(level.getBlockEntity(terminal)
                        instanceof ShipyardTerminalBlockEntity dock && dock.formed(), "the dock forms"))
                .thenExecute(() -> {
                    final ShipyardTerminalBlockEntity dock = (ShipyardTerminalBlockEntity) level.getBlockEntity(
                            terminal);
                    dock.design(design);
                    helper.assertTrue(dock.confirm(), "the ship is sent");
                    final PlannedBuild plan = dock.build().orElseThrow();
                    for (final PlanSpot spot : plan.plan().spots()) {
                        level.setBlockAndUpdate(plan.where(spot), BuiltInRegistries.BLOCK.get(spot.block())
                                .defaultBlockState());
                    }
                })
                .thenExecuteAfter(2, () -> ship[0] = ((ShipyardTerminalBlockEntity) level.getBlockEntity(terminal))
                        .release(null).orElseThrow())
                .thenWaitUntil(() -> helper.assertTrue(ship[0].ready(), "its blocks come into the structure"));
    }

    /*
     * A floor laid a way north of {@code at}, {@link #OVER_THE_LAND} over the land there and under the ship, in the
     * air where nothing else stands: stone as wide and as long as a ship sets down on; its middle block, where the
     * pad's terminal is taken to stand.
     */
    private static BlockPos padNorthOf(final ServerLevel level, final StructurePose at) {
        final int x = Mth.floor(at.x());
        final int z = Mth.floor(at.z()) - PAD_NORTH;
        int land = land(level, x, Mth.floor(at.z()));
        for (int across = -BARE_ACROSS; across <= BARE_ACROSS; across++) {
            for (int along = -BARE_ALONG; along <= BARE_ALONG; along++) {
                land = Math.max(land, land(level, x + across, z + along));
            }
        }
        final BlockPos middle = new BlockPos(x, land + OVER_THE_LAND, z);
        for (int across = -BARE_ACROSS; across <= BARE_ACROSS; across++) {
            for (int along = -BARE_ALONG; along <= BARE_ALONG; along++) {
                level.setBlockAndUpdate(middle.offset(across, 0, along), Blocks.STONE.defaultBlockState());
            }
        }
        return middle;
    }

    private static void takeAwayPad(final ServerLevel level, final BlockPos middle) {
        for (int across = -BARE_ACROSS; across <= BARE_ACROSS; across++) {
            for (int along = -BARE_ALONG; along <= BARE_ALONG; along++) {
                level.removeBlock(middle.offset(across, 0, along), false);
            }
        }
    }

    /*
     * A runway laid a way north of {@code at}, {@link #OVER_THE_LAND} over the land under it and under the ship, in
     * the air where nothing else stands: a floor of stone from a little behind its beacon to the end of its stretch, as
     * wide as the stretch, its beacon on it facing north, the way it runs; where the beacon stands.
     */
    private static BlockPos runwayNorthOf(final ServerLevel level, final StructurePose at) {
        final int x = Mth.floor(at.x());
        final int z = Mth.floor(at.z()) - RUNWAY_NORTH;
        int land = land(level, x, Mth.floor(at.z()));
        for (int along = -RUNWAY_BEHIND; along <= ShipAutopilots.RUNWAY_LENGTH; along++) {
            for (int across = -Runways.HALF_WIDTH; across <= Runways.HALF_WIDTH; across++) {
                land = Math.max(land, land(level, x + across, z - along));
            }
        }
        final BlockPos beacon = new BlockPos(x, land + OVER_THE_LAND + 1, z);
        for (int along = -RUNWAY_BEHIND; along <= ShipAutopilots.RUNWAY_LENGTH; along++) {
            for (int across = -Runways.HALF_WIDTH; across <= Runways.HALF_WIDTH; across++) {
                level.setBlockAndUpdate(beacon.offset(across, -1, -along), Blocks.STONE.defaultBlockState());
            }
        }
        level.setBlockAndUpdate(beacon, ShipLandingBlocks.BEACON.get().defaultBlockState()
                .setValue(RunwayBeaconBlock.FACING, Direction.NORTH));
        return beacon;
    }

    private static void takeAwayRunway(final ServerLevel level, final BlockPos beacon) {
        level.removeBlock(beacon, false);
        for (int along = -RUNWAY_BEHIND; along <= ShipAutopilots.RUNWAY_LENGTH; along++) {
            for (int across = -Runways.HALF_WIDTH; across <= Runways.HALF_WIDTH; across++) {
                level.removeBlock(beacon.offset(across, -1, -along), false);
            }
        }
    }

    /* How high the land stands at ({@code x}, {@code z}): the height of its top block. */
    private static int land(final ServerLevel level, final int x, final int z) {
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1;
    }

    /*
     * Keeps {@code level} loaded and ticking by this test's own tickets {@code chunks} round {@code at}, the tickets'
     * letting go added to {@code kept}.
     */
    private static void keep(final GameTestHelper helper, final ServerLevel level, final BlockPos at,
                             final int chunks, final List<Runnable> kept) {
        final ChunkPos middle = new ChunkPos(at);
        final BlockPos own = helper.absolutePos(CORNER);
        for (int x = middle.x - chunks; x <= middle.x + chunks; x++) {
            for (int z = middle.z - chunks; z <= middle.z + chunks; z++) {
                final ChunkPos chunk = new ChunkPos(x, z);
                level.getChunkSource().addRegionTicket(KEPT, chunk, TICKING, own);
                kept.add(() -> level.getChunkSource().removeRegionTicket(KEPT, chunk, TICKING, own));
            }
        }
    }

    /* Whether every chunk {@code chunks} round {@code at} ticks the entities in it. */
    private static boolean ticking(final ServerLevel level, final BlockPos at, final int chunks) {
        final ChunkPos middle = new ChunkPos(at);
        for (int x = middle.x - chunks; x <= middle.x + chunks; x++) {
            for (int z = middle.z - chunks; z <= middle.z + chunks; z++) {
                if (!level.isPositionEntityTicking(new ChunkPos(x, z).getWorldPosition())) {
                    return false;
                }
            }
        }
        return true;
    }

    /* {@code body}'s world, made from its dimension's file under its own name. */
    private static ServerLevel levelOf(final ServerLevel any, final SolarBody body) {
        final ResourceKey<Level> key = SpaceWorlds.get(any, SolarSystemWorlds.worldOf(body)).orElseThrow()
                .dimension();
        return RuntimeDimensions.getOrCreate(any.getServer(), key, key.location());
    }

    /*
     * Its pilot's game tells the server, as it does each tick, the keys they hold at {@code ship}'s helm and where they
     * are, so the land round them loads as they go, as round a player playing: a test's player sends nothing, and a
     * ship flown out of the land loaded is put away with its chunk.
     */
    private static void held(final MovingStructure ship, final ServerPlayer pilot,
                             final Map<StructureControls.ControlAxis, Float> keys) {
        ship.controls().pilot(ship.level().getGameTime(), keys, new Vec3(0, 0, -1), new Vec3(0, 1, 0));
        pilot.serverLevel().getChunkSource().move(pilot);
    }

    /* Where the block that stood at {@code built} in the dock is aboard {@code ship} now, in its yard. */
    private static BlockPos aboard(final MovingStructure ship, final BlockPos built) {
        return BlockPos.containing(MovingStructures.toYard(ship, Vec3.atCenterOf(built)));
    }

    private static PlannedBuild plan(final GameTestHelper helper, final ShipDesign design) {
        return new PlannedBuild(ShipyardPlans.of(helper.getLevel(), design, Orientation.NORTH),
                ShipyardStructure.shipAt(TestShipyards.terminalOf(helper.absolutePos(CORNER)), Orientation.NORTH));
    }

    private static PlanSpot spot(final GameTestHelper helper, final ShipDesign design, final ResourceLocation block) {
        return plan(helper, design).plan().spots().stream().filter(each -> each.block().equals(block)).findFirst()
                .orElseThrow();
    }

    private static ShipFlight flight(final MovingStructure ship) {
        return (ShipFlight) ship.motion();
    }

    /* {@code ship}'s flight console, which shows its flight. */
    private static AirReserveEntity console(final MovingStructure ship) {
        return (AirReserveEntity) ship.level().getBlockEntity(flight(ship).consoleIn(ship).orElseThrow());
    }

    private static String state(final MovingStructure ship) {
        return ship.motion() instanceof ShipFlight flight ? flight.state() + ", " + flight.autopilot() : "no flight";
    }

    private static ResourceLocation part(final String path) {
        return SmallShipParts.id("small_" + path);
    }

    /* How the ship sets down on a world it came into: on a pad, or on a runway; what it laid, added to what is kept. */
    @FunctionalInterface
    private interface Landing {

        GameTestSequence on(GameTestHelper helper, GameTestSequence sequence, MovingStructure[] ship,
                            ServerPlayer[] pilot, ServerLevel world, List<Runnable> kept);
    }
}
