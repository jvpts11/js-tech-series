/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import dev.jstech.core.hazard.Radiation;
import dev.jstech.core.jstechseries.hazard.IonizingRadiation;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.multiblock.FormedPartBlockEntity;
import dev.jstech.core.multiblock.damage.DamageStamps;
import dev.jstech.core.multiblock.damage.FormedDamage;
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.multiblock.plan.PlannedBuild;
import dev.jstech.core.region.Box;
import dev.jstech.core.uuid.StructureUuid;
import dev.jstech.space.api.Propellants;
import dev.jstech.space.cruise.CruiseEvents;
import dev.jstech.space.cruise.CruiseFlight;
import dev.jstech.space.cruise.RocketCruise;
import dev.jstech.space.cruise.SolarStorms;
import dev.jstech.space.flight.FlightComputerBlockEntity;
import dev.jstech.space.flight.PlannedTransfer;
import dev.jstech.space.hangar.RocketDesign;
import dev.jstech.space.hangar.RocketPlans;
import dev.jstech.space.launch.RocketLaunch;
import dev.jstech.space.orbit.OrbitObjects;
import dev.jstech.space.rocket.RocketKitBlocks;
import dev.jstech.space.rocket.RocketStack;
import dev.jstech.space.rocket.Rockets;
import dev.jstech.space.rocket.SkeletonPropellants;
import dev.jstech.space.rocket.SkeletonRocketParts;
import dev.jstech.space.rocket.TankLoad;
import dev.jstech.space.universe.SolarBody;
import dev.jstech.space.universe.SolarSystemWorlds;
import dev.jstech.space.universe.StarSystems;
import dev.jstech.space.universe.SystemLayout;
import dev.jstech.space.universe.SystemSpaces;
import dev.jstech.tests.JsTests;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jetbrains.annotations.Nullable;

/**
 * The cruise between worlds: a rocket in the Overworld's orbit with a transfer set burns its transfer burn and sets
 * out, and comes to rest in the Moon's orbit, told there; its crew sleeping, the cruise passes at once; on the way a
 * micrometeoroid holes the hull, and a patch put on mends it, and a solar storm is told ahead and then fills the space
 * with radiation.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class CruiseGameTests {

    private static final String ARENA = "bench";
    private static final String BATCH = "jsspace_cruise";
    private static final String SLEEP_BATCH = "jsspace_cruise_slept";
    private static final BlockPos FOOT = new BlockPos(8, 1, 8);
    private static final BlockPos OWNER = new BlockPos(1, 1, 1);
    private static final double LOAD = 2_000;
    private static final int TANK = 1;
    /* The transfer burn, in metres a second, and how long the cruise takes, in ticks; a cruise slept through. */
    private static final double BURN = 100;
    private static final long TRAVEL = 1_200;
    private static final long LONG_WAY = 1_000_000;
    /* How far from the Moon's orbit spot it may come to rest, and how far a storm is told ahead, in ticks. */
    private static final double THERE = 0.5;
    private static final int STORM_AHEAD = 400;
    /* Far less than a storm's radiation in the open, in millisieverts an hour. */
    private static final double STORMY = 1_000;

    private CruiseGameTests() {
    }

    // Its cruise waits for the space ahead to load, and a test server runs its ticks faster than it loads: long ticks.
    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 12_000)
    public static void cruise_burnsSetsOutAndArrivesInTheMoonsOrbit(final GameTestHelper helper) {
        fly(helper, TRAVEL, false);
    }

    // A batch of its own: two rockets set at the same orbit spot at once would stand in each other.
    @GameTest(template = ARENA, batch = SLEEP_BATCH, timeoutTicks = 1200)
    public static void cruise_passesAtOnceAsTheCrewSleeps(final GameTestHelper helper) {
        fly(helper, LONG_WAY, true);
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 1200)
    public static void cruise_holedByAMicrometeoroidIsPatchedAndAStormIsToldAhead(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final FlightComputerBlockEntity computer = build(helper);
        final MovingStructure body = RocketLaunch.release(level, computer.getBlockPos(), null).orElseThrow();
        final FlightComputerBlockEntity[] aboard = new FlightComputerBlockEntity[1];
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(body.ready(), "its body stands ready"))
                .thenExecute(() -> {
                    aboard[0] = computerOf(level, body);
                    helper.assertTrue(CruiseEvents.micrometeoroid(level, body, aboard[0]), "a micrometeoroid strikes");
                    final BlockPos holed = aboard[0].formedPlaces(level).stream().filter(pos -> level.getBlockEntity(
                            pos) instanceof FormedPartBlockEntity part && part.damage().stream().anyMatch(kept -> kept
                            .mark().stamp().equals(DamageStamps.MICROMETEOROID))).findFirst().orElse(null);
                    helper.assertTrue(holed != null, "and holes the hull");
                    helper.assertTrue(FormedDamage.erase(level, holed, DamageStamps.MICROMETEOROID) > 0
                            && ((FormedPartBlockEntity) level.getBlockEntity(holed)).damage().isEmpty(),
                            "a patch put on mends the hole");
                    CruiseEvents.storm(level, body);
                    helper.assertTrue(SolarStorms.passing(level) && !SolarStorms.raging(level),
                            "a solar storm is told ahead, not there yet");
                })
                .thenIdle(STORM_AHEAD)
                .thenExecute(() -> {
                    final Vec3 open = Vec3.atCenterOf(helper.absolutePos(new BlockPos(1, 30, 1)));
                    helper.assertTrue(SolarStorms.raging(level) && Radiation.rateAt(level, open,
                            IonizingRadiation.RAYS) > STORMY, "then it rages, its radiation filling the open");
                    MovingStructures.takeAway(body);
                })
                .thenSucceed();
    }

    /*
     * Builds a rocket, lets it go into the Overworld's orbit, sets a transfer to the Moon taking {@code travel} ticks
     * and watches it fly there, its crew sleeping through it when {@code slept}.
     */
    private static void fly(final GameTestHelper helper, final long travel, final boolean slept) {
        final ServerLevel level = helper.getLevel();
        final FlightComputerBlockEntity computer = build(helper);
        final MovingStructure released = RocketLaunch.release(level, computer.getBlockPos(), null).orElseThrow();
        final StructureUuid id = released.id();
        final ServerLevel space = SystemSpaces.spaceOf(level.getServer(), StarSystems.SOLAR_SYSTEM);
        final FlightComputerBlockEntity[] aboard = new FlightComputerBlockEntity[1];
        final double[] before = new double[1];
        final double[] spot = SystemLayout.orbitSpot(SolarBody.OVERWORLD, 0, 0);
        final int chunkX = SectionPos.blockToSectionCoord(spot[0]);
        final int chunkZ = SectionPos.blockToSectionCoord(spot[1]);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(released.ready(), "its body stands ready"))
                .thenExecute(() -> {
                    // Nobody is aboard, and a space no player is in and no chunk loader holds stops a little after:
                    // a chunk forced keeps it going, in its crew's stead, till the flight is over.
                    space.setChunkForced(chunkX, chunkZ, true);
                    MovingStructures.travel(released, space, new Vec3(spot[0], SystemLayout.ORBIT_Y, spot[1]));
                })
                .thenWaitUntil(() -> helper.assertTrue(body(space, id) != null && body(space, id).ready(),
                        "it rests in the Overworld's orbit"))
                .thenExecute(() -> {
                    aboard[0] = computerOf(space, body(space, id));
                    before[0] = held(space, id);
                    aboard[0].execute(new PlannedTransfer(SolarSystemWorlds.MOON, space.getGameTime(), BURN, 0, travel,
                            false, false));
                })
                .thenWaitUntil(() -> helper.assertTrue(body(space, id).motion() instanceof RocketCruise
                        && aboard[0].cruising(), "its window come, it sets out"))
                .thenExecute(() -> {
                    helper.assertTrue(held(space, id) < before[0], "its transfer burn burnt propellant: "
                            + before[0] + " kg to " + held(space, id) + " kg");
                    if (slept) {
                        helper.assertTrue(CruiseFlight.skip(space, body(space, id)), "the crew asleep, it passes");
                    }
                })
                .thenWaitUntil(() -> helper.assertTrue(!aboard[0].cruising() && body(space, id).motion() == null,
                        "it comes to rest at the end of its cruise"))
                .thenExecute(() -> {
                    space.setChunkForced(chunkX, chunkZ, false);
                    final MovingStructure there = body(space, id);
                    final double[] moon = SystemLayout.orbitSpot(SolarBody.MOON, 0, 0);
                    helper.assertTrue(Math.abs(there.pose().x() - moon[0]) < THERE
                            && Math.abs(there.pose().z() - moon[1]) < THERE, "over the Moon's orbit spot: "
                            + there.pose());
                    helper.assertTrue(OrbitObjects.around(level.getServer(), SolarSystemWorlds.MOON)
                            .containsKey(id.value()) && aboard[0].executed().isEmpty(),
                            "told in the Moon's orbit, its plan flown");
                    MovingStructures.takeAway(there);
                })
                .thenSucceed();
    }

    private static @Nullable MovingStructure body(final ServerLevel space, final StructureUuid id) {
        return MovingStructures.byId(space, id);
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

    /* What its tank holds now, its losses reckoned. */
    private static double held(final ServerLevel level, final StructureUuid id) {
        return Rockets.settle(level.getServer(), id).map(rocket -> rocket.tanks().get(TANK))
                .map(TankLoad::total).orElse(0.0);
    }

    /* Builds an engine, a tank and a capsule at the arena's foot, every part formed, its tank filled. */
    private static FlightComputerBlockEntity build(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final RocketStack stack = RocketStack.of(part("sea_level_engine_3"), part("tank_3"), part("capsule_3"));
        final BlockPos foot = helper.absolutePos(FOOT);
        final PlannedBuild build = new PlannedBuild(RocketPlans.of(level, stack), foot);
        build.start(level, helper.absolutePos(OWNER), () -> { });
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
        final List<Integer> formed = new ArrayList<>();
        for (int section = 0; section < stack.sections().size(); section++) {
            formed.add(section);
        }
        computer.carry(new RocketDesign("CRUISER", stack), foot.subtract(at), formed);
        Propellants.get(level, SkeletonPropellants.METHALOX).ifPresent(methalox -> computer.load(TANK,
                TankLoad.filled(SkeletonPropellants.METHALOX, methalox, LOAD)));
        return computer;
    }

    private static RocketStack.Part part(final String path) {
        return new RocketStack.Part(SkeletonRocketParts.id(path), 1);
    }
}
