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
import dev.jstech.core.moving.StructureYard;
import dev.jstech.core.moving.YardLayout;
import dev.jstech.core.multiblock.FormedPartBlock;
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.multiblock.plan.PlannedBuild;
import dev.jstech.core.region.Box;
import dev.jstech.core.uuid.StructureUuid;
import dev.jstech.space.api.Propellants;
import dev.jstech.space.api.RocketPart;
import dev.jstech.space.api.RocketParts;
import dev.jstech.space.api.SpaceWorlds;
import dev.jstech.space.config.SpaceServerConfig;
import dev.jstech.space.cruise.CouchSleep;
import dev.jstech.space.descent.DescentPhase;
import dev.jstech.space.flight.FlightComputerBlockEntity;
import dev.jstech.space.flight.FlightPage;
import dev.jstech.space.flight.TransferPlan;
import dev.jstech.space.hangar.RocketDesign;
import dev.jstech.space.hangar.RocketPlans;
import dev.jstech.space.launch.RocketLaunch;
import dev.jstech.space.orbit.Rocketry;
import dev.jstech.space.rocket.RocketAscent;
import dev.jstech.space.rocket.RocketKitBlocks;
import dev.jstech.space.rocket.RocketPieces;
import dev.jstech.space.rocket.RocketStack;
import dev.jstech.space.rocket.SkeletonPropellants;
import dev.jstech.space.rocket.SkeletonRocketParts;
import dev.jstech.space.rocket.TankLoad;
import dev.jstech.space.universe.SolarBody;
import dev.jstech.space.universe.SolarSystemWorlds;
import dev.jstech.space.universe.SpaceClimbs;
import dev.jstech.space.universe.SphereMap;
import dev.jstech.space.universe.StarSystems;
import dev.jstech.space.universe.SystemLayout;
import dev.jstech.space.universe.SystemSpaces;
import dev.jstech.tests.JsTests;
import dev.jstech.testkit.gametest.GameTestPlayers;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ServerLevelData;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jetbrains.annotations.Nullable;

/**
 * A rocket's trips between the worlds, flown as a player flies them: the climb off a world into its low orbit by the
 * rocket's own engines, which spends what the climb asks, and short of it falls back; and the trip from the Overworld
 * to the Moon's ground and home in one launch, by the flight computer's keys.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class RocketRoutesGameTests {

    private static final String TALL = "tall";
    private static final String BATCH = "jsspace_routes_climb";
    private static final String SHORT_BATCH = "jsspace_routes_climb_short";
    private static final BlockPos FOOT = new BlockPos(6, 2, 6);
    private static final BlockPos OWNER = new BlockPos(0, 2, 0);
    private static final String TANK = "tank_3";
    private static final String BIG_TANK = "tank_5";
    private static final String BIG_ENGINE = "sea_level_engine_5";
    private static final String MOON_BATCH = "jsspace_routes_moon";
    private static final String MARS_BATCH = "jsspace_routes_mars";
    private static final String MARS_HOME_BATCH = "jsspace_routes_mars_home";
    private static final String MOON_TO_MARS_BATCH = "jsspace_routes_moon_mars";
    private static final String MARS_TO_MOON_BATCH = "jsspace_routes_mars_moon";
    /* Where a site built on another world stands, its place across and along, and the room round a rocket's foot. */
    private static final int SITE = 96;
    private static final int SITE_ROOM = 7;
    /*
     * How far into a window the clock is moved, in ticks, a window lasting some 20,000: room for the climb to orbit
     * inside it. The Mars rocket's spent pieces: two boosters and a stage.
     */
    private static final long WINDOW_INSIDE = 5_000;
    private static final int MARS_PIECES = 3;
    /* The Mars rocket's third stage on the ground, its engine, legs, tank and capsule, at most this many blocks. */
    private static final int MARS_STAGE_TALL = 13;
    /* Where the big rocket's foot stands in its arena, and where the block keeping its plan stands. */
    private static final BlockPos BIG_FOOT = new BlockPos(8, 1, 8);
    private static final BlockPos BIG_OWNER = new BlockPos(1, 1, 1);
    /* Where it lands on each world, the middle of the near side: the Moon's and, home again, the Overworld's. */
    private static final int MARKED = 0;
    private static final int HOME = 64;
    /* The small rocket's second stage, its engine, tank and capsule, at most this many blocks tall. */
    private static final int SECOND_STAGE_TALL = 13;
    /* The lander on the Moon's ground, its descent stage and its ascent stage, at most this many blocks tall. */
    private static final int LANDER_TALL = 12;
    /* The plots of the yard its spent pieces take as they part in the climb: four boosters and the first stage. */
    private static final int PIECES = 5;
    /* How near the climb's cost and the page's count the flight comes, in metres a second. */
    private static final double ABOUT = 300;
    /* How long a rocket come into its orbit is watched for blocks read as lost: three of its computer's counts. */
    private static final int WATCHED = 3 * 20;
    /* The longest a tick may take as stages are let go, the climb's own work, chunks made on the way, and all. */
    private static final long LONGEST_TICK_NANOS = 2_000_000_000L;
    private static final long NANOS_A_MILLI = 1_000_000L;
    /* A rocket of one stage, about 2,500 m/s, and one of two, about 10,000. */
    private static final RocketStack ONE_STAGE = RocketStack.of(part("sea_level_engine_3"), part(TANK),
            part("capsule_3"));
    private static final RocketStack TWO_STAGES = RocketStack.of(part("sea_level_engine_3"), part(TANK, 14),
            part("separation_ring_3"), part("vacuum_engine_3"), part(TANK, 6), part("capsule_3"));
    /*
     * The lightest rocket today's parts give that takes a lunar lander to the Moon's ground and home: four boosters of
     * fourteen layers beside a first stage of thirty-two, a second of eight, the lander on top, some 1,680 t.
     */
    private static final RocketStack MOON_AND_BACK = boosted(RocketStack.of(part(BIG_ENGINE), part(BIG_TANK, 32),
            part("separation_ring_5"), part("vacuum_engine_5"), part(BIG_TANK, 8), part("separation_ring_5"),
            part("lunar_lander_descent"), part("lunar_lander_ascent")), List.of(part(BIG_ENGINE), part(BIG_TANK, 14)),
            4);
    /*
     * The lightest rocket today's parts give that takes a crew to Mars' ground: two boosters of ten layers beside a
     * first stage of seventeen, a second of nineteen, and a third with legs and four layers under its capsule, which
     * brakes in Mars' air on its heat shield and lands on its legs, some 1,060 t.
     */
    private static final RocketStack TO_MARS = boosted(RocketStack.of(part(BIG_ENGINE), part(BIG_TANK, 17),
            part("separation_ring_5"), part("vacuum_engine_5"), part(BIG_TANK, 19), part("separation_ring_5"),
            part("vacuum_engine_5"), part("landing_legs_5"), part(BIG_TANK, 4), part("capsule_5")),
            List.of(part(BIG_ENGINE), part(BIG_TANK, 10)), 2);
    /*
     * A rocket a site on Mars builds to bring its crew home: a first stage of four layers and a second of six under
     * its capsule, some 8,400 m/s of the 7,700 the climb off Mars and the transfer home ask.
     */
    private static final RocketStack MARS_HOME = RocketStack.of(part(BIG_ENGINE), part(BIG_TANK, 4),
            part("separation_ring_5"), part("vacuum_engine_5"), part(BIG_TANK, 6), part("capsule_5"));
    /*
     * A rocket a site on the Moon builds for Mars: one stage with legs and five layers under its capsule, some 6,300
     * m/s of the 5,900 the climb off the Moon, the transfer and the landing on Mars ask.
     */
    private static final RocketStack MOON_TO_MARS = RocketStack.of(part("vacuum_engine_5"), part("landing_legs_5"),
            part(BIG_TANK, 5), part("capsule_5"));
    /*
     * A rocket a site on Mars builds for the Moon: a first stage of twelve layers and a second with legs and six under
     * its capsule, some 9,900 m/s of the 9,400 the climb off Mars, the transfer and the landing on the Moon ask.
     */
    private static final RocketStack MARS_TO_MOON = RocketStack.of(part(BIG_ENGINE), part(BIG_TANK, 12),
            part("separation_ring_5"), part("vacuum_engine_5"), part("landing_legs_5"), part(BIG_TANK, 6),
            part("capsule_5"));

    private RocketRoutesGameTests() {
    }

    /* The cruise's events back on after the trips, as the server's file has them. */
    @AfterBatch(batch = MOON_BATCH)
    public static void eventsBack(final ServerLevel level) {
        SpaceServerConfig.FILE.set(SpaceServerConfig.CRUISE_EVENTS, true);
    }

    /* The same after the trip to Mars. */
    @AfterBatch(batch = MARS_BATCH)
    public static void eventsBackAfterMars(final ServerLevel level) {
        SpaceServerConfig.FILE.set(SpaceServerConfig.CRUISE_EVENTS, true);
    }

    /*
     * The world's clock moved on into a window of a transfer from the Overworld to Mars before the trip begins, as a
     * player waits for one; moved during a test, it would run the test's own time out.
     */
    @BeforeBatch(batch = MARS_BATCH)
    public static void intoMarsWindow(final ServerLevel level) {
        intoWindow(level, SolarBody.OVERWORLD, SolarBody.MARS);
    }

    /* The clock moved on into a window home from Mars, as the crew of a site built there waits for one. */
    @BeforeBatch(batch = MARS_HOME_BATCH)
    public static void intoWindowHome(final ServerLevel level) {
        intoWindow(level, SolarBody.MARS, SolarBody.OVERWORLD);
    }

    /* The cruise's events back on after the trip home from Mars. */
    @AfterBatch(batch = MARS_HOME_BATCH)
    public static void eventsBackAfterHome(final ServerLevel level) {
        SpaceServerConfig.FILE.set(SpaceServerConfig.CRUISE_EVENTS, true);
    }

    /* The clock moved on into Mars' window for a site on the Moon, the Moon going round the Sun with the Overworld. */
    @BeforeBatch(batch = MOON_TO_MARS_BATCH)
    public static void intoMarsWindowFromTheMoon(final ServerLevel level) {
        intoWindow(level, SolarBody.MOON, SolarBody.MARS);
    }

    /* The cruise's events back on after the trip from the Moon to Mars. */
    @AfterBatch(batch = MOON_TO_MARS_BATCH)
    public static void eventsBackAfterMoonToMars(final ServerLevel level) {
        SpaceServerConfig.FILE.set(SpaceServerConfig.CRUISE_EVENTS, true);
    }

    /* The clock moved on into the window from Mars to the Moon. */
    @BeforeBatch(batch = MARS_TO_MOON_BATCH)
    public static void intoMoonWindowFromMars(final ServerLevel level) {
        intoWindow(level, SolarBody.MARS, SolarBody.MOON);
    }

    /* The cruise's events back on after the trip from Mars to the Moon. */
    @AfterBatch(batch = MARS_TO_MOON_BATCH)
    public static void eventsBackAfterMarsToMoon(final ServerLevel level) {
        SpaceServerConfig.FILE.set(SpaceServerConfig.CRUISE_EVENTS, true);
    }

    /*
     * A rocket of two stages with more delta-v than the climb to the Overworld's low orbit asks, released and lifted
     * as its count releases it, climbs by itself into space, its first stage let go on the way, and comes to its orbit
     * having spent what the climb asks.
     */
    @GameTest(template = TALL, batch = BATCH, timeoutTicks = 6_000)
    public static void climb_spendsWhatTheClimbToTheLowOrbitAsks(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final FlightComputerBlockEntity computer = build(helper, TWO_STAGES);
        final double onThePad = computer.available().orElse(-1);
        final double[] whole = new double[1];
        final MovingStructure released = RocketLaunch.release(level, computer.getBlockPos(), null).orElseThrow();
        final StructureUuid id = released.id();
        final ServerLevel space = SystemSpaces.spaceOf(level.getServer(), StarSystems.SOLAR_SYSTEM);
        final double width = Math.max(1, level.getWorldBorder().getSize());
        final double[] spot = SystemLayout.orbitSpot(SolarBody.OVERWORLD, SphereMap.longitude(released.pose().x(),
                width), SphereMap.latitude(released.pose().z(), width));
        final int chunkX = SectionPos.blockToSectionCoord(spot[0]);
        final int chunkZ = SectionPos.blockToSectionCoord(spot[1]);
        // The plot its first stage takes as it parts, held loaded so it parts before the rocket reaches its orbit.
        final List<Integer> plots = heldPlots(level, 1);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(released.ready(), "its body stands ready"))
                .thenExecute(() -> {
                    // Off the ground, its blocks in the yard, the computer counts all its stages give.
                    whole[0] = computerOf(level, released).available().orElse(-1);
                    space.setChunkForced(chunkX, chunkZ, true);
                    RocketLaunch.lift(level, released);
                })
                .thenWaitUntil(() -> helper.assertTrue(MovingStructures.byId(space, id) != null
                        && MovingStructures.byId(space, id).ready(), "it climbs into space"))
                // Its computer counts the blocks it has lost once a second: none of a rocket come whole into orbit.
                .thenExecuteFor(WATCHED, () -> {
                    final FlightComputerBlockEntity aboard = computerOf(space, MovingStructures.byId(space, id));
                    whole(helper, aboard, TWO_STAGES, "come into its orbit");
                    helper.assertTrue(aboard.shield(), "and its capsule's shield is there");
                })
                // Its computer reads its tanks once a second.
                .thenWaitUntil(() -> {
                    final MovingStructure there = MovingStructures.byId(space, id);
                    final FlightComputerBlockEntity aboard = computerOf(space, there);
                    final double left = aboard.available().orElse(-1);
                    final double spent = whole[0] - left;
                    helper.assertTrue(spent >= Rocketry.SURFACE_TO_LOW_ORBIT && spent < Rocketry.SURFACE_TO_LOW_ORBIT
                                    + ABOUT && Math.abs(left - onThePad) < ABOUT,
                            "the climb to the low orbit spends what it asks, " + Rocketry.SURFACE_TO_LOW_ORBIT
                                    + " m/s, and what the pad's page said is left: the rocket had " + whole[0]
                                    + " m/s, the page said " + onThePad + " m/s for a transfer, and it has " + left
                                    + " m/s left in orbit; " + flightOf(aboard, TWO_STAGES) + "; "
                                    + missing(space, aboard, TWO_STAGES));
                    final Box box = there.bounds();
                    helper.assertTrue(box.maxY() - box.minY() + 1 <= SECOND_STAGE_TALL, "its first stage stayed"
                            + " behind, not carried into space: " + (box.maxY() - box.minY() + 1) + " blocks tall");
                })
                .thenExecute(() -> {
                    space.setChunkForced(chunkX, chunkZ, false);
                    plots.forEach(plot -> forcedPlot(level, plot, false));
                    MovingStructures.takeAway(MovingStructures.byId(space, id));
                })
                .thenSucceed();
    }

    /*
     * A rocket of one stage with less delta-v than the climb to the Overworld's low orbit asks burns all it has at the
     * edge of space and falls back to its pad, never reaching its orbit.
     */
    @GameTest(template = TALL, batch = SHORT_BATCH, timeoutTicks = 4_000)
    public static void climb_shortOfTheLowOrbitFallsBack(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final FlightComputerBlockEntity computer = build(helper, ONE_STAGE);
        final MovingStructure released = RocketLaunch.release(level, computer.getBlockPos(), null).orElseThrow();
        final StructureUuid id = released.id();
        final ServerLevel space = SystemSpaces.spaceOf(level.getServer(), StarSystems.SOLAR_SYSTEM);
        final double start = released.pose().y();
        final double[] highest = new double[1];
        final boolean[] plumeFalling = new boolean[1];
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(released.ready(), "its body stands ready"))
                .thenExecute(() -> RocketLaunch.lift(level, released))
                .thenWaitUntil(() -> {
                    highest[0] = Math.max(highest[0], released.pose().y());
                    if (highest[0] >= SpaceClimbs.ceiling(level) - 1 && released.motion() instanceof RocketAscent ascent
                            && ascent.burning(level, released)) {
                        plumeFalling[0] = true;
                    }
                    helper.assertTrue(MovingStructures.byId(space, id) == null, "it never reaches space");
                    helper.assertTrue(released.motion() == null && released.pose().y() < start + 1,
                            "it falls back to its pad: at " + released.pose().y() + ", highest " + highest[0]);
                })
                .thenExecute(() -> helper.assertTrue(highest[0] >= SpaceClimbs.ceiling(level) - 1,
                        "it climbed to the edge of space: " + highest[0]))
                .thenExecute(() -> helper.assertFalse(plumeFalling[0], "burnt out at the edge of space, it falls back"
                        + " with its engines out, no plume under it"))
                // Its computer reads its tanks once a second.
                .thenWaitUntil(() -> helper.assertTrue(computerOf(level, released).available().orElse(-1) < 1,
                        "having burnt all it had: " + computerOf(level, released).available().orElse(-1)
                                + " m/s left"))
                .thenExecute(() -> MovingStructures.takeAway(released))
                .thenSucceed();
    }

    /*
     * The trip from the Overworld to the Moon's ground and home in one launch, flown by the flight computer's own keys:
     * the rocket climbs off its pad by itself, its boosters and first stage let go on the way; in the Overworld's orbit
     * XFER and EXEC send it to the Moon; it comes to rest in the Moon's orbit, LAND and SET bring the lander down on
     * its legs; on the ground XFER and EXEC send it home, the ascent stage lifting off and leaving the descent stage;
     * it comes home braking in the Overworld's air on its heat shield and lands under its parachutes.
     */
    @GameTest(template = "bench", batch = MOON_BATCH, timeoutTicks = 60_000)
    public static void route_toTheMoonsGroundAndHomeInOneLaunch(final GameTestHelper helper) {
        // The cruise's events come by chance, a micrometeoroid breaching the heat shield a crew must patch: they have
        // their tests of their own, and the trip here is flown without them.
        SpaceServerConfig.FILE.set(SpaceServerConfig.CRUISE_EVENTS, false);
        final ServerLevel level = helper.getLevel();
        final FlightComputerBlockEntity computer = build(helper, MOON_AND_BACK, BIG_FOOT, BIG_OWNER);
        final MovingStructure released = RocketLaunch.release(level, computer.getBlockPos(), null).orElseThrow();
        final StructureUuid id = released.id();
        final ServerLevel space = SystemSpaces.spaceOf(level.getServer(), StarSystems.SOLAR_SYSTEM);
        final ServerLevel moon = levelOf(level, SolarBody.MOON);
        final double width = Math.max(1, level.getWorldBorder().getSize());
        // Nobody aboard: chunks forced keep going the places it passes, in its crew's stead.
        final List<double[]> spots = List.of(SystemLayout.orbitSpot(SolarBody.OVERWORLD, SphereMap.longitude(
                released.pose().x(), width), SphereMap.latitude(released.pose().z(), width)),
                SystemLayout.orbitSpot(SolarBody.MOON, 0, 0), SystemLayout.orbitSpot(SolarBody.OVERWORLD, 0, 0));
        final List<Integer> plots = heldPlots(level, PIECES);
        final int[] down = new int[2];
        final String[] home = new String[1];
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(released.ready(), "its body stands ready"))
                .thenExecute(() -> {
                    spots.forEach(spot -> forcedAround(space, spot[0], spot[1], true));
                    RocketLaunch.lift(level, released);
                })
                .thenWaitUntil(() -> helper.assertTrue(at(space, id) != null && at(space, id).ready()
                        && at(space, id).motion() == null, "it climbs into the Overworld's orbit"))
                .thenExecute(() -> {
                    final FlightComputerBlockEntity aboard = computerOf(space, at(space, id));
                    aboard.press(FlightComputerBlockEntity.TOP + FlightPage.KEYS.indexOf(FlightPage.TRANSFER));
                    aboard.press(FlightComputerBlockEntity.LEFT + aboard.destinations().indexOf(
                            SolarSystemWorlds.MOON));
                    helper.assertTrue(aboard.planned().isPresent(), "in orbit it plans the transfer to the Moon, with "
                            + aboard.available() + " m/s");
                    aboard.press(FlightComputerBlockEntity.RIGHT + 4);
                    helper.assertTrue(aboard.executed().isPresent(), "EXEC sets the transfer to the Moon, with "
                            + aboard.available() + " m/s for " + aboard.planned());
                })
                .thenWaitUntil(() -> helper.assertTrue(computerOf(space, at(space, id)).cruising(),
                        "it sets out for the Moon"))
                .thenWaitUntil(() -> helper.assertTrue(!computerOf(space, at(space, id)).cruising()
                        && at(space, id).motion() == null, "it comes to rest in the Moon's orbit"))
                .thenExecute(() -> {
                    final FlightComputerBlockEntity aboard = computerOf(space, at(space, id));
                    aboard.press(FlightComputerBlockEntity.TOP + FlightPage.KEYS.indexOf(FlightPage.LANDING));
                    aboard.markAt(MARKED, MARKED);
                    aboard.press(FlightComputerBlockEntity.RIGHT + 4);
                    helper.assertTrue(aboard.landing().isPresent(), "LAND and SET set its landing on the Moon");
                })
                .thenWaitUntil(() -> helper.assertTrue(at(moon, id) != null, "it passes into the Moon's sky"))
                .thenExecute(() -> made(moon, at(moon, id), down))
                .thenWaitUntil(() -> helper.assertTrue(at(moon, id) != null && at(moon, id).motion() == null
                        && computerOf(moon, at(moon, id)).descent() == DescentPhase.LANDED,
                        "it comes down on the Moon's ground"))
                .thenExecuteFor(WATCHED, () -> whole(helper, computerOf(moon, at(moon, id)), MOON_AND_BACK,
                        "landed on the Moon's ground"))
                // A stage let go parts once its own plot of the yard has loaded, at the disk's pace and not in so many
                // ticks: on a loaded machine it still stands under the lander as it touches down, and is waited for.
                .thenWaitUntil(() -> {
                    final Box box = at(moon, id).bounds();
                    final FlightComputerBlockEntity aboard = computerOf(moon, at(moon, id));
                    helper.assertTrue(box.maxY() - box.minY() + 1 <= LANDER_TALL, "the lander stands on its own legs,"
                            + " no spent stage under it: " + (box.maxY() - box.minY() + 1) + " blocks tall, "
                            + aboard.separated() + " stages let go, the descent stage kept " + aboard.landerDescent());
                })
                .thenExecute(() -> {
                    final MovingStructure lander = at(moon, id);
                    // Nobody aboard: the chunk it came down in, not the one it came into the sky over, is kept going
                    // in its crew's stead, or its ascent stage freezes on its climb once the descent's hold lapses.
                    moon.setChunkForced(down[0], down[1], false);
                    made(moon, lander, down);
                    final FlightComputerBlockEntity aboard = computerOf(moon, lander);
                    aboard.press(FlightComputerBlockEntity.TOP + FlightPage.KEYS.indexOf(FlightPage.TRANSFER));
                    aboard.press(FlightComputerBlockEntity.LEFT + aboard.destinations().indexOf(
                            SolarSystemWorlds.OVERWORLD));
                    aboard.press(FlightComputerBlockEntity.RIGHT + 4);
                    helper.assertTrue(aboard.executed().isPresent(), "on the ground EXEC sets the way home, with "
                            + aboard.available() + " m/s for " + aboard.planned());
                    home[0] = aboard.executed() + ", " + aboard.available() + " m/s, shield " + aboard.shield();
                })
                .thenWaitUntil(() -> helper.assertTrue(at(space, id) != null || at(moon, id) != null
                        && at(moon, id).motion() instanceof RocketAscent, "the ascent stage lifts off the Moon: "
                        + where(moon, space, id)))
                .thenWaitUntil(() -> helper.assertTrue(at(space, id) != null && at(space, id).motion() == null,
                        "the ascent stage climbs off the Moon into its orbit: " + where(moon, space, id)))
                .thenWaitUntil(() -> helper.assertTrue(computerOf(space, at(space, id)).cruising(),
                        "it sets out for home"))
                .thenWaitUntil(() -> helper.assertTrue(!computerOf(space, at(space, id)).cruising()
                        && at(space, id).motion() == null, "it comes to rest in the Overworld's orbit"))
                .thenExecute(() -> {
                    moon.setChunkForced(down[0], down[1], false);
                    final FlightComputerBlockEntity aboard = computerOf(space, at(space, id));
                    helper.assertTrue(aboard.wideOrbit().isEmpty(), "braking in the Overworld's air, it is captured: "
                            + "set out by " + home[0] + "; now " + aboard.available() + " m/s, shield "
                            + aboard.shield());
                    aboard.press(FlightComputerBlockEntity.TOP + FlightPage.KEYS.indexOf(FlightPage.LANDING));
                    aboard.markAt(HOME, HOME);
                    aboard.press(FlightComputerBlockEntity.RIGHT + 4);
                    helper.assertTrue(aboard.landing().isPresent(), "LAND and SET set its landing at home");
                })
                .thenWaitUntil(() -> helper.assertTrue(at(level, id) != null && at(level, id).pose().y()
                        < SpaceClimbs.ceiling(level), "it passes into the Overworld's sky"))
                .thenExecute(() -> made(level, at(level, id), down))
                .thenWaitUntil(() -> helper.assertTrue(at(level, id) != null && at(level, id).motion() == null
                        && computerOf(level, at(level, id)).descent() == DescentPhase.LANDED,
                        "it comes down home under its parachutes"))
                .thenExecute(() -> {
                    spots.forEach(spot -> forcedAround(space, spot[0], spot[1], false));
                    plots.forEach(plot -> forcedPlot(level, plot, false));
                    MovingStructures.takeAway(at(level, id));
                    SpaceServerConfig.FILE.set(SpaceServerConfig.CRUISE_EVENTS, true);
                })
                .thenSucceed();
    }

    /*
     * The trip from the Overworld to Mars' ground in one launch, a crew aboard: the rocket climbs off its pad by
     * itself, its boosters and first stage let go on the way; in the Overworld's orbit, the window come, XFER and EXEC
     * send it to Mars; its crew sleeps in the capsule's seat and the cruise passes; it brakes in Mars' air on its heat
     * shield into the low orbit, its second stage, spent, gone; LAND and SET bring it down through Mars' thin air on
     * its parachutes and its engine to its legs. Coming back from Mars is a launch from a site built there.
     */
    @GameTest(template = "bench", batch = MARS_BATCH, timeoutTicks = 60_000)
    public static void route_toTheGroundOfMarsInOneLaunch(final GameTestHelper helper) {
        SpaceServerConfig.FILE.set(SpaceServerConfig.CRUISE_EVENTS, false);
        final ServerLevel level = helper.getLevel();
        final FlightComputerBlockEntity computer = build(helper, TO_MARS, BIG_FOOT, BIG_OWNER);
        final BlockPos seat = seatOf(helper, TO_MARS, BIG_FOOT);
        final ServerPlayer crew = GameTestPlayers.connected(helper, "mars_crew");
        crew.moveTo(seat.getX() + 0.5, seat.getY() + 1, seat.getZ() + 0.5);
        helper.assertTrue(MovingStructures.sit(crew, seat), "the crew sits in the capsule's seat on the pad");
        final MovingStructure released = RocketLaunch.release(level, computer.getBlockPos(), crew.getUUID())
                .orElseThrow();
        final StructureUuid id = released.id();
        final ServerLevel space = SystemSpaces.spaceOf(level.getServer(), StarSystems.SOLAR_SYSTEM);
        final ServerLevel mars = levelOf(level, SolarBody.MARS);
        final List<Integer> plots = heldPlots(level, MARS_PIECES);
        final int[] down = new int[2];
        final boolean[] climbing = {false};
        final long[] ticks = longestTick(helper, climbing);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(released.ready(), "its body stands ready"))
                .thenExecute(() -> {
                    climbing[0] = true;
                    RocketLaunch.lift(level, released);
                })
                .thenWaitUntil(() -> helper.assertTrue(at(space, id) != null && at(space, id).ready()
                        && at(space, id).motion() == null, "it climbs into the Overworld's orbit"))
                .thenExecute(() -> {
                    climbing[0] = false;
                    stalledNone(helper, ticks, "its boosters and first stage let go together at the edge of space");
                    helper.assertTrue(TransferPlan.of(SolarBody.OVERWORLD, SolarBody.MARS, space.getGameTime())
                            .orElseThrow().inWindow(), "Mars' window is open as it reaches its orbit");
                    final FlightComputerBlockEntity aboard = computerOf(space, at(space, id));
                    aboard.press(FlightComputerBlockEntity.TOP + FlightPage.KEYS.indexOf(FlightPage.TRANSFER));
                    aboard.press(FlightComputerBlockEntity.LEFT + aboard.destinations().indexOf(
                            SolarSystemWorlds.MARS));
                    aboard.press(FlightComputerBlockEntity.RIGHT + 4);
                    helper.assertTrue(aboard.executed().isPresent(), "in Mars' window EXEC sets the transfer, with "
                            + aboard.available() + " m/s for " + aboard.planned());
                })
                .thenWaitUntil(() -> helper.assertTrue(computerOf(space, at(space, id)).cruising(),
                        "it sets out for Mars: " + computerOf(space, at(space, id)).executed() + ", "
                                + computerOf(space, at(space, id)).available() + " m/s at tick "
                                + space.getGameTime()))
                .thenWaitUntil(() -> {
                    CouchSleep.pressed(crew);
                    helper.assertTrue(CouchSleep.asleep(crew), "its crew falls asleep in the capsule's seat");
                })
                .thenWaitUntil(() -> helper.assertTrue(!computerOf(space, at(space, id)).cruising()
                        && at(space, id).motion() == null, "the crew asleep, the cruise passes and it rests in Mars'"
                        + " orbit"))
                .thenExecute(() -> {
                    final FlightComputerBlockEntity aboard = computerOf(space, at(space, id));
                    helper.assertTrue(aboard.wideOrbit().isEmpty(), "braking in Mars' air, it is captured");
                    aboard.press(FlightComputerBlockEntity.TOP + FlightPage.KEYS.indexOf(FlightPage.LANDING));
                    aboard.markAt(MARKED, MARKED);
                    aboard.press(FlightComputerBlockEntity.RIGHT + 4);
                    helper.assertTrue(aboard.landing().isPresent(), "LAND and SET set its landing on Mars");
                })
                .thenWaitUntil(() -> helper.assertTrue(at(mars, id) != null, "it passes into Mars' sky"))
                .thenExecute(() -> made(mars, at(mars, id), down))
                .thenWaitUntil(() -> helper.assertTrue(at(mars, id) != null && at(mars, id).motion() == null
                        && computerOf(mars, at(mars, id)).descent() == DescentPhase.LANDED,
                        "it comes down on Mars' ground"))
                .thenExecute(() -> {
                    final MovingStructure landed = at(mars, id);
                    final Box box = landed.bounds();
                    helper.assertTrue(box.maxY() - box.minY() + 1 <= MARS_STAGE_TALL, "it stands on its own legs, no"
                            + " spent stage under it: " + (box.maxY() - box.minY() + 1) + " blocks tall");
                    helper.assertTrue(computerOf(mars, landed).lost(TO_MARS.sections().size() - 1) == 0,
                            "its capsule whole");
                    GameTestPlayers.leave(crew);
                    plots.forEach(plot -> forcedPlot(level, plot, false));
                    mars.setChunkForced(down[0], down[1], false);
                    MovingStructures.takeAway(landed);
                    SpaceServerConfig.FILE.set(SpaceServerConfig.CRUISE_EVENTS, true);
                })
                .thenSucceed();
    }

    /*
     * The way home from Mars, launched from a site built on its ground: a rocket of two stages, a crew aboard, climbs
     * off Mars into its low orbit, its first stage let go; in Mars' window home XFER and EXEC send it to the
     * Overworld; its crew sleeps and the cruise passes; it brakes in the Overworld's air on its heat shield and lands
     * under its parachutes.
     */
    @GameTest(template = "bench", batch = MARS_HOME_BATCH, timeoutTicks = 60_000)
    public static void route_homeFromASiteBuiltOnMars(final GameTestHelper helper) {
        fromASite(helper, SolarBody.MARS, SolarBody.OVERWORLD, MARS_HOME, 1, "mars_home_crew");
    }

    /*
     * From a site built on the Moon to Mars' ground: a rocket of one stage with legs, a crew aboard, climbs off the
     * Moon, sets out in Mars' window, brakes in Mars' air and comes down on its legs.
     */
    @GameTest(template = "bench", batch = MOON_TO_MARS_BATCH, timeoutTicks = 60_000)
    public static void route_toMarsFromASiteBuiltOnTheMoon(final GameTestHelper helper) {
        fromASite(helper, SolarBody.MOON, SolarBody.MARS, MOON_TO_MARS, 0, "moon_mars_crew");
    }

    /*
     * From a site built on Mars to the Moon's ground: a rocket of two stages, its second with legs, a crew aboard,
     * climbs off Mars, sets out in its window, comes to rest in the Moon's orbit and brakes down to its legs.
     */
    @GameTest(template = "bench", batch = MARS_TO_MOON_BATCH, timeoutTicks = 60_000)
    public static void route_toTheMoonFromASiteBuiltOnMars(final GameTestHelper helper) {
        fromASite(helper, SolarBody.MARS, SolarBody.MOON, MARS_TO_MOON, 1, "mars_moon_crew");
    }

    /*
     * A crew's trip from a site built on {@code from} to the ground of {@code to} in a rocket of {@code stack}, which
     * lets go {@code pieces} stages climbing: built and filled on the site, its crew sat in its capsule, it climbs into
     * the low orbit, the window come XFER and EXEC send it on, its crew sleeps through the cruise, it comes to rest in
     * the other world's orbit, LAND and SET bring it down, and it stands there landed.
     */
    private static void fromASite(final GameTestHelper helper, final SolarBody from, final SolarBody to,
                                  final RocketStack stack, final int pieces, final String crewName) {
        SpaceServerConfig.FILE.set(SpaceServerConfig.CRUISE_EVENTS, false);
        final ServerLevel level = helper.getLevel();
        final ServerLevel start = levelOf(level, from);
        final ServerLevel ground = levelOf(level, to);
        final ServerLevel space = SystemSpaces.spaceOf(level.getServer(), StarSystems.SOLAR_SYSTEM);
        final BlockPos[] places = buildAt(start, stack, groundOf(start, SITE, SITE));
        final ServerPlayer crew = GameTestPlayers.connected(helper, crewName);
        crew.teleportTo(start, places[1].getX() + 0.5, places[1].getY() + 1, places[1].getZ() + 0.5, 0, 0);
        helper.assertTrue(MovingStructures.sit(crew, places[1]), "the crew sits in the capsule's seat");
        final MovingStructure released = RocketLaunch.release(start, places[0], crew.getUUID()).orElseThrow();
        final StructureUuid id = released.id();
        final List<Integer> plots = heldPlots(start, pieces);
        final int mark = to == SolarBody.OVERWORLD ? HOME : MARKED;
        final int[] down = new int[2];
        final boolean[] climbing = {false};
        final long[] ticks = longestTick(helper, climbing);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(released.ready(), "its body stands ready"))
                .thenExecute(() -> {
                    climbing[0] = true;
                    RocketLaunch.lift(start, released);
                })
                .thenWaitUntil(() -> helper.assertTrue(at(space, id) != null && at(space, id).ready()
                        && at(space, id).motion() == null, "it climbs off " + from + " into its orbit"))
                .thenExecute(() -> {
                    climbing[0] = false;
                    stalledNone(helper, ticks, "its stages let go as it climbs off " + from);
                })
                .thenExecuteFor(WATCHED, () -> whole(helper, computerOf(space, at(space, id)), stack,
                        "come off " + from + " into its orbit"))
                .thenExecute(() -> {
                    final FlightComputerBlockEntity aboard = computerOf(space, at(space, id));
                    aboard.press(FlightComputerBlockEntity.TOP + FlightPage.KEYS.indexOf(FlightPage.TRANSFER));
                    aboard.press(FlightComputerBlockEntity.LEFT + aboard.destinations().indexOf(
                            SolarSystemWorlds.worldOf(to)));
                    aboard.press(FlightComputerBlockEntity.RIGHT + 4);
                    helper.assertTrue(aboard.executed().isPresent(), "EXEC sets the transfer to " + to + ", with "
                            + aboard.available() + " m/s for " + aboard.planned());
                })
                .thenWaitUntil(() -> helper.assertTrue(computerOf(space, at(space, id)).cruising(),
                        "it sets out for " + to + ": " + computerOf(space, at(space, id)).executed() + ", "
                                + computerOf(space, at(space, id)).available() + " m/s; "
                                + flightOf(computerOf(space, at(space, id)), stack) + "; "
                                + missing(space, computerOf(space, at(space, id)), stack)))
                .thenWaitUntil(() -> {
                    CouchSleep.pressed(crew);
                    helper.assertTrue(CouchSleep.asleep(crew), "its crew falls asleep in the capsule's seat");
                })
                .thenWaitUntil(() -> helper.assertTrue(!computerOf(space, at(space, id)).cruising()
                        && at(space, id).motion() == null, "the cruise passes and it rests in " + to + "'s orbit"))
                .thenExecute(() -> {
                    final FlightComputerBlockEntity aboard = computerOf(space, at(space, id));
                    helper.assertTrue(aboard.wideOrbit().isEmpty(), "it is captured into " + to + "'s low orbit, with "
                            + aboard.available() + " m/s left");
                    aboard.press(FlightComputerBlockEntity.TOP + FlightPage.KEYS.indexOf(FlightPage.LANDING));
                    aboard.markAt(mark, mark);
                    aboard.press(FlightComputerBlockEntity.RIGHT + 4);
                    helper.assertTrue(aboard.landing().isPresent(), "LAND and SET set its landing on " + to);
                })
                .thenWaitUntil(() -> helper.assertTrue(at(ground, id) != null && at(ground, id).pose().y()
                        < SpaceClimbs.ceiling(ground), "it passes into " + to + "'s sky"))
                .thenExecute(() -> made(ground, at(ground, id), down))
                .thenWaitUntil(() -> helper.assertTrue(at(ground, id) != null && at(ground, id).motion() == null
                        && computerOf(ground, at(ground, id)).descent() == DescentPhase.LANDED,
                        "it comes down on " + to + "'s ground"))
                .thenExecute(() -> {
                    GameTestPlayers.leave(crew);
                    plots.forEach(plot -> forcedPlot(start, plot, false));
                    ground.setChunkForced(down[0], down[1], false);
                    MovingStructures.takeAway(at(ground, id));
                    SpaceServerConfig.FILE.set(SpaceServerConfig.CRUISE_EVENTS, true);
                })
                .thenSucceed();
    }

    /* The flight computer aboard {@code body}, in its yard. */
    private static FlightComputerBlockEntity computerOf(final ServerLevel level, final MovingStructure body) {
        final FlightComputerBlockEntity computer = computerAboard(level, body);
        if (computer == null) {
            throw new IllegalStateException("no flight computer aboard");
        }
        return computer;
    }

    /* The flight computer aboard {@code body}, or null while none stands in its plot. */
    private static @Nullable FlightComputerBlockEntity computerAboard(final ServerLevel level,
                                                                      final MovingStructure body) {
        final Box box = body.bounds();
        for (final BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(),
                box.maxZ())) {
            if (level.getBlockEntity(pos) instanceof FlightComputerBlockEntity computer) {
                return computer;
            }
        }
        return null;
    }

    /* What {@code computer} keeps of its rocket of {@code stack}, for a failure to say: stages, holed parts, tanks. */
    private static String flightOf(final FlightComputerBlockEntity computer, final RocketStack stack) {
        final StringBuilder lost = new StringBuilder();
        for (int section = 0; section < stack.sections().size(); section++) {
            lost.append(section).append('=').append(computer.lost(section)).append(' ');
        }
        return "stages let go " + computer.separated() + ", blocks lost by part " + lost + "tanks " + computer.tanks();
    }

    /*
     * The longest a tick of the test takes while {@code watching[0]} is set, in nanoseconds, at its first place; the
     * last tick's start at its second.
     */
    private static long[] longestTick(final GameTestHelper helper, final boolean[] watching) {
        final long[] watch = new long[2];
        helper.onEachTick(() -> {
            final long now = System.nanoTime();
            if (watching[0] && watch[1] != 0) {
                watch[0] = Math.max(watch[0], now - watch[1]);
            }
            watch[1] = now;
        });
        return watch;
    }

    /*
     * That no tick watched by {@link #longestTick} stalled the game while {@code what}: pieces pressed together, as
     * stages let go together are, are met at a bounded number of points, so their physics never takes seconds.
     */
    private static void stalledNone(final GameTestHelper helper, final long[] watch, final String what) {
        helper.assertTrue(watch[0] < LONGEST_TICK_NANOS, what + ", no tick stalls the game: the longest took "
                + watch[0] / NANOS_A_MILLI + " ms");
    }

    /*
     * That none of the parts of {@code stack} its flight computer {@code computer} counts reads as holed, {@code when}:
     * a part read as holed leaks its tank and gives no thrust from its engine.
     */
    private static void whole(final GameTestHelper helper, final FlightComputerBlockEntity computer,
                              final RocketStack stack, final String when) {
        final List<Integer> holed = new ArrayList<>();
        for (int section = 0; section < stack.sections().size(); section++) {
            if (computer.lost(section) > 0) {
                holed.add(section);
            }
        }
        helper.assertTrue(holed.isEmpty(), when + ", none of its parts reads as holed: holed " + holed + "; "
                + flightOf(computer, stack));
    }

    /* The places of the rocket {@code computer} keeps that do not hold their part's block, for a failure to say. */
    private static String missing(final ServerLevel level, final FlightComputerBlockEntity computer,
                                  final RocketStack stack) {
        final StringBuilder out = new StringBuilder("missing:");
        final BlockPos base = computer.getBlockPos().offset(computer.foot().orElse(BlockPos.ZERO));
        for (final PlanSpot spot : RocketPlans.of(level, stack).spots()) {
            final BlockPos at = base.offset(spot.at());
            final BlockState there = FormedPartBlock.originalAt(level, at, level.getBlockState(at));
            if (spot.place().isPresent() && computer.lost(spot.part()) > 0
                    && !BuiltInRegistries.BLOCK.getKey(there.getBlock()).equals(spot.block())) {
                out.append(' ').append(spot.part()).append(':').append(spot.block().getPath()).append('@')
                        .append(spot.at().toShortString()).append('=').append(level.getBlockState(at));
            }
        }
        return out.toString();
    }

    /*
     * Where the rocket {@code id} is, on {@code ground} or in {@code space}, and what its flight computer keeps: for a
     * failure to say why it did not climb off the ground.
     */
    private static String where(final ServerLevel ground, final ServerLevel space, final StructureUuid id) {
        final MovingStructure there = at(ground, id) != null ? at(ground, id) : at(space, id);
        if (there == null) {
            return "in neither world";
        }
        final FlightComputerBlockEntity computer = computerAboard((ServerLevel) there.level(), there);
        return "in " + there.level().dimension().location() + " at " + there.pose() + ", ready " + there.ready()
                + ", moved by " + there.motion() + ", " + (computer == null ? "no computer" : "descent "
                + computer.descent() + ", executed " + computer.executed() + ", " + computer.available()
                + " m/s, " + computer.rocket().map(design -> flightOf(computer, design.stack())).orElse("no rocket"));
    }

    /* The structure {@code id} in {@code level}, if it is there. */
    private static @Nullable MovingStructure at(final ServerLevel level, final StructureUuid id) {
        return MovingStructures.byId(level, id);
    }

    /* {@code world}'s level, made from its dimension's file under its own name. */
    private static ServerLevel levelOf(final ServerLevel any, final SolarBody world) {
        final ResourceKey<Level> key = SpaceWorlds.get(any, SolarSystemWorlds.worldOf(world)).orElseThrow()
                .dimension();
        return RuntimeDimensions.getOrCreate(any.getServer(), key, key.location());
    }

    /*
     * Makes now the land under {@code body} come down into {@code level}, keeping it loaded: the test server runs its
     * ticks far faster than land is made. {@code down} keeps the chunk forced.
     */
    private static void made(final ServerLevel level, final MovingStructure body, final int[] down) {
        down[0] = SectionPos.blockToSectionCoord(body.pose().x());
        down[1] = SectionPos.blockToSectionCoord(body.pose().z());
        level.setChunkForced(down[0], down[1], true);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                level.getChunk(down[0] + dx, down[1] + dz);
            }
        }
    }

    /* Forces the chunks round the place {@code x}, {@code z} of {@code level} loaded, or lets them go. */
    private static void forcedAround(final ServerLevel level, final double x, final double z, final boolean kept) {
        final int cx = SectionPos.blockToSectionCoord(x);
        final int cz = SectionPos.blockToSectionCoord(z);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                level.setChunkForced(cx + dx, cz + dz, kept);
            }
        }
    }

    /*
     * The yard's next {@code count} plots of {@code level}, the plots the pieces let go next take, their chunks forced
     * loaded: a piece parts once its plot has loaded, and the test server runs its ticks far faster than chunks load.
     */
    private static List<Integer> heldPlots(final ServerLevel level, final int count) {
        final List<Integer> plots = new ArrayList<>();
        int plot = StructureYard.nextPlot(level);
        while (plots.size() < count) {
            plots.add(plot);
            forcedPlot(level, plot, true);
            do {
                plot++;
            } while (StructureYard.owner(level, plot).isPresent());
        }
        return plots;
    }

    /* Forces the chunks round the middle of the yard's plot {@code plot} loaded, or lets them go. */
    private static void forcedPlot(final ServerLevel level, final int plot, final boolean kept) {
        forcedAround(level, YardLayout.centreX(plot), YardLayout.centreZ(plot), kept);
    }

    /* {@code core} with {@code count} boosters of {@code booster} hung beside its engine. */
    private static RocketStack boosted(final RocketStack core, final List<RocketStack.Part> booster, final int count) {
        final List<RocketStack.Section> sections = new ArrayList<>(core.sections());
        final List<List<RocketStack.Part>> hung = new ArrayList<>();
        for (int k = 0; k < count; k++) {
            hung.add(booster);
        }
        sections.set(0, new RocketStack.Section(sections.get(0).part(), hung));
        return new RocketStack(sections);
    }

    /* Moves the world's clock on into a window of a transfer from {@code from} to {@code to}, if none is open now. */
    private static void intoWindow(final ServerLevel level, final SolarBody from, final SolarBody to) {
        final TransferPlan plan = TransferPlan.of(from, to, level.getGameTime()).orElseThrow();
        if (!plan.inWindow()) {
            final ServerLevelData time = (ServerLevelData) level.getServer().overworld().getLevelData();
            time.setGameTime(time.getGameTime() + plan.toWindow() + WINDOW_INSIDE);
        }
    }

    /* The place over the ground of {@code level} at {@code x}, {@code z}, its land made now. */
    private static BlockPos groundOf(final ServerLevel level, final int x, final int z) {
        level.getChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z));
        return new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z), z);
    }

    /*
     * The rocket {@code stack} built by its plan on {@code level}, its foot at {@code foot}, as a site's hangar builds
     * it, kept by its computer, its tanks full: where its flight computer and its crew seat stand.
     */
    private static BlockPos[] buildAt(final ServerLevel level, final RocketStack stack, final BlockPos foot) {
        final PlannedBuild build = new PlannedBuild(RocketPlans.of(level, stack), foot);
        build.start(level, foot.offset(-SITE_ROOM, 0, -SITE_ROOM), () -> { });
        BlockPos computerAt = foot;
        BlockPos seat = foot;
        for (final PlanSpot spot : build.plan().spots()) {
            level.setBlockAndUpdate(build.where(spot), BuiltInRegistries.BLOCK.get(spot.block()).defaultBlockState());
            if (spot.block().equals(RocketKitBlocks.FLIGHT_COMPUTER.getId())) {
                computerAt = build.where(spot);
            } else if (spot.block().equals(RocketKitBlocks.CREW_SEAT.getId())) {
                seat = build.where(spot);
            }
        }
        build.stop(false);
        final FlightComputerBlockEntity computer = (FlightComputerBlockEntity) level.getBlockEntity(computerAt);
        final List<Integer> formed = new ArrayList<>();
        for (int piece = 0; piece < RocketPieces.count(stack); piece++) {
            formed.add(piece);
        }
        computer.carry(new RocketDesign("ROUTE", stack), foot.subtract(computerAt), formed);
        final Map<ResourceLocation, RocketPart> parts = RocketParts.all(level);
        for (int section = 0; section < stack.sections().size(); section++) {
            final RocketStack.Part part = stack.sections().get(section).part();
            final double holds = parts.get(part.id()).propellant() * part.layers();
            if (holds > 0) {
                final int tank = section;
                Propellants.get(level, SkeletonPropellants.METHALOX).ifPresent(methalox -> computer.load(tank,
                        TankLoad.filled(SkeletonPropellants.METHALOX, methalox, holds)));
            }
        }
        return new BlockPos[] {computerAt, seat};
    }

    /* Where the crew seat of {@code stack}, built with its foot at {@code standing} of the arena, stands. */
    private static BlockPos seatOf(final GameTestHelper helper, final RocketStack stack, final BlockPos standing) {
        final PlannedBuild build = new PlannedBuild(RocketPlans.of(helper.getLevel(), stack),
                helper.absolutePos(standing));
        return build.plan().spots().stream().filter(spot -> spot.block().equals(RocketKitBlocks.CREW_SEAT.getId()))
                .findFirst().map(build::where).orElseThrow();
    }


    /* The rocket {@code stack}, built by its plan as its hangar builds it, kept by its computer, its tanks full. */
    private static FlightComputerBlockEntity build(final GameTestHelper helper, final RocketStack stack) {
        return build(helper, stack, FOOT, OWNER);
    }

    /* The rocket {@code stack}, its foot at {@code standing} of the arena, its plan kept at {@code owner}. */
    private static FlightComputerBlockEntity build(final GameTestHelper helper, final RocketStack stack,
                                                   final BlockPos standing, final BlockPos owner) {
        final ServerLevel level = helper.getLevel();
        final BlockPos foot = helper.absolutePos(standing);
        final PlannedBuild build = new PlannedBuild(RocketPlans.of(level, stack), foot);
        build.start(level, helper.absolutePos(owner), () -> { });
        BlockPos at = foot;
        for (final PlanSpot spot : build.plan().spots()) {
            level.setBlockAndUpdate(build.where(spot), BuiltInRegistries.BLOCK.get(spot.block()).defaultBlockState());
            if (spot.block().equals(RocketKitBlocks.FLIGHT_COMPUTER.getId())) {
                at = build.where(spot);
            }
        }
        helper.assertTrue(build.complete(), "the rocket is built whole");
        build.stop(false);
        final FlightComputerBlockEntity computer = (FlightComputerBlockEntity) level.getBlockEntity(at);
        helper.assertTrue(computer != null, "its capsule holds its flight computer");
        // Every piece formed: the parts of its core and of its boosters.
        final List<Integer> formed = new ArrayList<>();
        for (int piece = 0; piece < RocketPieces.count(stack); piece++) {
            formed.add(piece);
        }
        computer.carry(new RocketDesign("ROUTE", stack), foot.subtract(at), formed);
        // Every part that holds propellant filled, as the pad's service port fills it; boosters come full.
        final Map<ResourceLocation, RocketPart> parts = RocketParts.all(level);
        for (int section = 0; section < stack.sections().size(); section++) {
            final RocketStack.Part part = stack.sections().get(section).part();
            final double holds = parts.get(part.id()).propellant() * part.layers();
            if (holds > 0) {
                final int tank = section;
                Propellants.get(level, SkeletonPropellants.METHALOX).ifPresent(methalox -> computer.load(tank,
                        TankLoad.filled(SkeletonPropellants.METHALOX, methalox, holds)));
            }
        }
        return computer;
    }

    private static RocketStack.Part part(final String path) {
        return part(path, 1);
    }

    private static RocketStack.Part part(final String path, final int layers) {
        return new RocketStack.Part(SkeletonRocketParts.id(path), layers);
    }
}
