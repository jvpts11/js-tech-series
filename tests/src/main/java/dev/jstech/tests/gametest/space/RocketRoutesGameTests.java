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
import dev.jstech.space.descent.DescentPhase;
import dev.jstech.space.flight.FlightComputerBlockEntity;
import dev.jstech.space.flight.FlightPage;
import dev.jstech.space.hangar.RocketDesign;
import dev.jstech.space.hangar.RocketPlans;
import dev.jstech.space.launch.RocketLaunch;
import dev.jstech.space.orbit.Rocketry;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
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
            part("lunar_lander_descent"), part("lunar_lander_ascent")), List.of(part(BIG_ENGINE), part(BIG_TANK, 14)));

    private RocketRoutesGameTests() {
    }

    /* The cruise's events back on after the trips, as the server's file has them. */
    @AfterBatch(batch = MOON_BATCH)
    public static void eventsBack(final ServerLevel level) {
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
        final double onThePad = computer.availableToLand().orElse(-1);
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
                    whole[0] = computerOf(level, released).availableToLand().orElse(-1);
                    space.setChunkForced(chunkX, chunkZ, true);
                    RocketLaunch.lift(level, released);
                })
                .thenWaitUntil(() -> helper.assertTrue(MovingStructures.byId(space, id) != null
                        && MovingStructures.byId(space, id).ready(), "it climbs into space"))
                // Its computer reads its tanks once a second.
                .thenWaitUntil(() -> {
                    final MovingStructure there = MovingStructures.byId(space, id);
                    final FlightComputerBlockEntity aboard = computerOf(space, there);
                    final double left = aboard.availableToLand().orElse(-1);
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
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(released.ready(), "its body stands ready"))
                .thenExecute(() -> RocketLaunch.lift(level, released))
                .thenWaitUntil(() -> {
                    highest[0] = Math.max(highest[0], released.pose().y());
                    helper.assertTrue(MovingStructures.byId(space, id) == null, "it never reaches space");
                    helper.assertTrue(released.motion() == null && released.pose().y() < start + 1,
                            "it falls back to its pad: at " + released.pose().y() + ", highest " + highest[0]);
                })
                .thenExecute(() -> helper.assertTrue(highest[0] >= SpaceClimbs.ceiling(level) - 1,
                        "it climbed to the edge of space: " + highest[0]))
                // Its computer reads its tanks once a second.
                .thenWaitUntil(() -> helper.assertTrue(computerOf(level, released).availableToLand().orElse(-1) < 1,
                        "having burnt all it had: " + computerOf(level, released).availableToLand().orElse(-1)
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
                .thenExecute(() -> {
                    final MovingStructure lander = at(moon, id);
                    final Box box = lander.bounds();
                    helper.assertTrue(box.maxY() - box.minY() + 1 <= LANDER_TALL, "the lander stands on its own legs,"
                            + " no spent stage under it: " + (box.maxY() - box.minY() + 1) + " blocks tall");
                    final FlightComputerBlockEntity aboard = computerOf(moon, lander);
                    aboard.press(FlightComputerBlockEntity.TOP + FlightPage.KEYS.indexOf(FlightPage.TRANSFER));
                    aboard.press(FlightComputerBlockEntity.LEFT + aboard.destinations().indexOf(
                            SolarSystemWorlds.OVERWORLD));
                    aboard.press(FlightComputerBlockEntity.RIGHT + 4);
                    helper.assertTrue(aboard.executed().isPresent(), "on the ground EXEC sets the way home, with "
                            + aboard.available() + " m/s for " + aboard.planned());
                    home[0] = aboard.executed() + ", " + aboard.available() + " m/s, shield " + aboard.shield();
                })
                .thenWaitUntil(() -> helper.assertTrue(at(space, id) != null && at(space, id).motion() == null,
                        "the ascent stage climbs off the Moon into its orbit"))
                .thenWaitUntil(() -> helper.assertTrue(computerOf(space, at(space, id)).cruising(),
                        "it sets out for home"))
                .thenWaitUntil(() -> helper.assertTrue(!computerOf(space, at(space, id)).cruising()
                        && at(space, id).motion() == null, "it comes to rest in the Overworld's orbit"))
                .thenExecute(() -> {
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

    /* The flight computer aboard {@code body}, in its yard. */
    private static FlightComputerBlockEntity computerOf(final ServerLevel level, final MovingStructure body) {
        final Box box = body.bounds();
        for (final BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(),
                box.maxZ())) {
            if (level.getBlockEntity(pos) instanceof FlightComputerBlockEntity computer) {
                return computer;
            }
        }
        throw new IllegalStateException("no flight computer aboard");
    }

    /* What {@code computer} keeps of its rocket of {@code stack}, for a failure to say: stages, holed parts, tanks. */
    private static String flightOf(final FlightComputerBlockEntity computer, final RocketStack stack) {
        final StringBuilder lost = new StringBuilder();
        for (int section = 0; section < stack.sections().size(); section++) {
            lost.append(section).append('=').append(computer.lost(section)).append(' ');
        }
        return "stages let go " + computer.separated() + ", blocks lost by part " + lost + "tanks " + computer.tanks();
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

    /* {@code core} with four boosters of {@code booster} hung beside its engine. */
    private static RocketStack boosted(final RocketStack core, final List<RocketStack.Part> booster) {
        final List<RocketStack.Section> sections = new ArrayList<>(core.sections());
        sections.set(0, new RocketStack.Section(sections.get(0).part(), List.of(booster, booster, booster, booster)));
        return new RocketStack(sections);
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
