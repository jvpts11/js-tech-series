/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import dev.jstech.core.moving.IStructureBody;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.multiblock.plan.PlannedBuild;
import dev.jstech.core.region.Box;
import dev.jstech.core.uuid.StructureUuid;
import dev.jstech.space.api.Propellants;
import dev.jstech.space.api.SpaceWorlds;
import dev.jstech.space.config.SpaceServerConfig;
import dev.jstech.space.descent.DescentFlight;
import dev.jstech.space.descent.DescentPhase;
import dev.jstech.space.flight.FlightComputerBlockEntity;
import dev.jstech.space.flight.FlightPage;
import dev.jstech.space.flight.PlannedLanding;
import dev.jstech.space.flight.PlannedTransfer;
import dev.jstech.space.hangar.RocketDesign;
import dev.jstech.space.hangar.RocketPlans;
import dev.jstech.space.launch.RocketLaunch;
import dev.jstech.space.orbit.OrbitObjects;
import dev.jstech.space.rocket.RocketAscent;
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
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jetbrains.annotations.Nullable;

/**
 * Coming down to a world: a rocket in the Moon's orbit with its landing set leaves its orbit, passes into the Moon's
 * sky and brakes on its engines to its legs, landing within its guidance's ellipse round the spot marked, its
 * propellant spent; with no legs it crashes, losing blocks, or, crash landings off, it cannot begin and stays. In the
 * Overworld's thick air it comes down under its parachutes. A lunar lander leaving the Moon lifts its ascent stage
 * off, its descent stage left standing on the ground.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class DescentGameTests {

    private static final String ARENA = "bench";
    private static final BlockPos FOOT = new BlockPos(8, 1, 8);
    private static final BlockPos OWNER = new BlockPos(1, 1, 1);
    /* What the test's rockets carry, in kilograms, and where it is in each stack. */
    private static final double FULL = 5_000;
    private static final double SOME = 2_000;
    private static final int LONG = 6_000;
    /* The spot marked on each world, at its middle, and how far past its ellipse it may stand. */
    private static final int MARKED = 0;
    private static final double SLACK = 4;
    /* How far round the lander's place its stages are looked for, in blocks. */
    private static final double NEAR = 64;
    /* How far over the ground a stage on its legs may stand, in blocks: its legs' feet reach down from its box. */
    private static final double ON_THE_GROUND = 1.5;

    private DescentGameTests() {
    }

    @GameTest(template = ARENA, batch = "jsspace_descent_moon", timeoutTicks = LONG)
    public static void descent_landsOnTheMoonOnItsLegsWithinTheEllipse(final GameTestHelper helper) {
        down(helper, RocketStack.of(part("sea_level_engine_3"), part("landing_legs_3"), part("tank_3"),
                part("capsule_3")), 2, SolarBody.MOON, false);
    }

    @GameTest(template = ARENA, batch = "jsspace_descent_crash", timeoutTicks = LONG)
    public static void descent_withNoLegsCrashesOnTheMoon(final GameTestHelper helper) {
        down(helper, RocketStack.of(part("sea_level_engine_3"), part("tank_3"), part("capsule_3")), 1,
                SolarBody.MOON, true);
    }

    @GameTest(template = ARENA, batch = "jsspace_descent_air", timeoutTicks = LONG)
    public static void descent_underParachutesInTheOverworldsAir(final GameTestHelper helper) {
        down(helper, RocketStack.of(part("sea_level_engine_3"), part("tank_3"), part("capsule_3")), 1,
                SolarBody.OVERWORLD, false);
    }

    @GameTest(template = ARENA, batch = "jsspace_descent_held", timeoutTicks = 1_200)
    public static void descent_heldWithNoLegsWhenCrashesAreOff(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final FlightComputerBlockEntity computer = build(helper, RocketStack.of(part("sea_level_engine_3"),
                part("tank_3"), part("capsule_3")), 1, FULL);
        final MovingStructure released = RocketLaunch.release(level, computer.getBlockPos(), null).orElseThrow();
        final StructureUuid id = released.id();
        final ServerLevel space = SystemSpaces.spaceOf(level.getServer(), StarSystems.SOLAR_SYSTEM);
        final double[] spot = SystemLayout.orbitSpot(SolarBody.MOON, 0, 0);
        final boolean[] began = new boolean[1];
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(released.ready(), "its body stands ready"))
                .thenExecute(() -> {
                    space.setChunkForced(chunk(spot[0]), chunk(spot[1]), true);
                    MovingStructures.travel(released, space, new Vec3(spot[0], SystemLayout.ORBIT_Y, spot[1]));
                })
                .thenWaitUntil(() -> helper.assertTrue(body(space, id) != null && body(space, id).ready(),
                        "it rests in the Moon's orbit"))
                .thenExecute(() -> {
                    OrbitObjects.arrived(space, body(space, id), SolarBody.MOON);
                    SpaceServerConfig.FILE.set(SpaceServerConfig.CRASH_LANDINGS, false);
                    try {
                        began[0] = DescentFlight.begin(space, body(space, id), computerOf(space, body(space, id)),
                                new PlannedLanding(SolarSystemWorlds.MOON, MARKED, MARKED));
                    } finally {
                        SpaceServerConfig.FILE.set(SpaceServerConfig.CRASH_LANDINGS, true);
                    }
                    helper.assertTrue(!began[0] && body(space, id).motion() == null
                            && computerOf(space, body(space, id)).descent() == DescentPhase.NONE,
                            "with no legs and crash landings off, it cannot begin and stays in its orbit");
                    space.setChunkForced(chunk(spot[0]), chunk(spot[1]), false);
                    MovingStructures.takeAway(body(space, id));
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = "jsspace_descent_lift", timeoutTicks = LONG)
    public static void liftOff_leavesTheLandersDescentStageOnTheGround(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final FlightComputerBlockEntity computer = build(helper, RocketStack.of(part("lunar_lander_descent"),
                part("lunar_lander_ascent")), 1, SOME);
        final MovingStructure released = RocketLaunch.release(level, computer.getBlockPos(), null).orElseThrow();
        final StructureUuid id = released.id();
        final ServerLevel moon = levelOf(level, SolarBody.MOON);
        final int[] ground = new int[1];
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(released.ready(), "its body stands ready"))
                .thenExecute(() -> {
                    moon.setChunkForced(0, 0, true);
                    ground[0] = moon.getChunk(0, 0).getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 8, 8);
                    MovingStructures.travel(released, moon, new Vec3(8.5, ground[0], 8.5));
                })
                .thenWaitUntil(() -> helper.assertTrue(body(moon, id) != null && body(moon, id).ready(),
                        "it stands on the Moon"))
                .thenExecute(() -> {
                    final FlightComputerBlockEntity there = computerOf(moon, body(moon, id));
                    there.descent(DescentPhase.LANDED);
                    there.execute(new PlannedTransfer(SolarSystemWorlds.OVERWORLD, moon.getGameTime(), 100, 0,
                            1_200, false, false));
                })
                .thenWaitUntil(() -> helper.assertTrue(body(moon, id).motion() instanceof RocketAscent
                        && computerOf(moon, body(moon, id)).separated() >= 1,
                        "its transfer set and its window come, it lifts off, its descent stage let go"))
                .thenWaitUntil(() -> helper.assertTrue(!left(moon, id, ground[0]).isEmpty(),
                        "its descent stage stands on the ground: " + MovingStructures.near(moon, new AABB(-NEAR,
                        ground[0] - NEAR, -NEAR, NEAR, ground[0] + NEAR, NEAR)).stream().map(b -> b.id() + " "
                        + b.worldBox().minY).toList()))
                .thenExecute(() -> {
                    left(moon, id, ground[0]).forEach(MovingStructures::takeAway);
                    if (body(moon, id) != null) {
                        MovingStructures.takeAway(body(moon, id));
                    }
                    moon.setChunkForced(0, 0, false);
                })
                .thenSucceed();
    }

    /*
     * Builds {@code stack}, its tank at {@code tank} full, lets it go into {@code world}'s orbit, sets a landing at the
     * world's middle and watches it come down: on its legs or under its parachutes, or, when it {@code crashes},
     * losing blocks.
     */
    private static void down(final GameTestHelper helper, final RocketStack stack, final int tank,
                             final SolarBody world, final boolean crashes) {
        final ServerLevel level = helper.getLevel();
        final FlightComputerBlockEntity computer = build(helper, stack, tank, FULL);
        final MovingStructure released = RocketLaunch.release(level, computer.getBlockPos(), null).orElseThrow();
        final StructureUuid id = released.id();
        final ServerLevel space = SystemSpaces.spaceOf(level.getServer(), StarSystems.SOLAR_SYSTEM);
        final ServerLevel ground = levelOf(level, world);
        final ResourceLocation worldId = SolarSystemWorlds.worldOf(world);
        final double[] spot = SystemLayout.orbitSpot(world, 0, 0);
        final double[] before = new double[2];
        final int[] forced = new int[2];
        final Set<DescentPhase> seen = EnumSet.noneOf(DescentPhase.class);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(released.ready(), "its body stands ready"))
                .thenExecute(() -> {
                    // Nobody aboard: forced chunks keep the spaces it passes going, in its crew's stead.
                    space.setChunkForced(chunk(spot[0]), chunk(spot[1]), true);
                    MovingStructures.travel(released, space, new Vec3(spot[0], SystemLayout.ORBIT_Y, spot[1]));
                })
                .thenWaitUntil(() -> helper.assertTrue(body(space, id) != null && body(space, id).ready(),
                        "it rests in the world's orbit"))
                .thenExecute(() -> {
                    OrbitObjects.arrived(space, body(space, id), world);
                    final FlightComputerBlockEntity aboard = computerOf(space, body(space, id));
                    before[0] = held(space, id, tank);
                    before[1] = aboard.formedPlaces(space).size();
                    aboard.press(FlightComputerBlockEntity.LEFT + aboard.destinations().indexOf(worldId));
                    aboard.press(FlightComputerBlockEntity.TOP + FlightPage.KEYS.indexOf(FlightPage.LANDING));
                    aboard.markAt(MARKED, MARKED);
                    aboard.press(FlightComputerBlockEntity.RIGHT + 4);
                    helper.assertTrue(aboard.landing().isPresent(), "its landing is set");
                })
                .thenWaitUntil(() -> helper.assertTrue(body(ground, id) != null, "it passes into the world's sky"))
                .thenExecute(() -> {
                    final MovingStructure falling = body(ground, id);
                    forced[0] = chunk(falling.pose().x());
                    forced[1] = chunk(falling.pose().z());
                    ground.setChunkForced(forced[0], forced[1], true);
                })
                .thenWaitUntil(() -> {
                    final MovingStructure falling = body(ground, id);
                    final FlightComputerBlockEntity aboard = falling == null ? null : computerOf(ground, falling);
                    if (aboard != null) {
                        seen.add(aboard.descent());
                    }
                    helper.assertTrue(aboard != null && aboard.descent() == DescentPhase.LANDED
                            && falling.motion() == null, "it comes to stand on the ground");
                })
                .thenExecute(() -> {
                    final MovingStructure landed = body(ground, id);
                    final FlightComputerBlockEntity aboard = computerOf(ground, landed);
                    space.setChunkForced(chunk(spot[0]), chunk(spot[1]), false);
                    ground.setChunkForced(forced[0], forced[1], false);
                    final double length = FlightComputerBlockEntity.ellipseLength(aboard.guidance()) / 2.0 + SLACK;
                    final double width = FlightComputerBlockEntity.ellipseWidth(aboard.guidance()) / 2.0 + SLACK;
                    final double dx = (landed.pose().x() - MARKED) / length;
                    final double dz = (landed.pose().z() - MARKED) / width;
                    helper.assertTrue(dx * dx + dz * dz <= 1, "it lands within its ellipse: " + landed.pose());
                    final int lost = (int) before[1] - aboard.formedPlaces(ground).size();
                    if (crashes) {
                        helper.assertTrue(lost > 0, "it crashes, losing blocks");
                    } else {
                        helper.assertTrue(lost == 0, "it lands whole: " + lost + " blocks lost");
                    }
                    if (world == SolarBody.MOON && !crashes) {
                        helper.assertTrue(held(ground, id, tank) < before[0], "its braking burnt propellant: "
                                + before[0] + " kg to " + held(ground, id, tank));
                        helper.assertTrue(seen.contains(DescentPhase.BRAKING), "it braked on its engines: " + seen);
                    }
                    if (world == SolarBody.OVERWORLD) {
                        helper.assertTrue(seen.contains(DescentPhase.PARACHUTES), "it came down under its"
                                + " parachutes: " + seen);
                    }
                    MovingStructures.takeAway(landed);
                })
                .thenSucceed();
    }

    /* The structures other than {@code id} standing on the ground at {@code ground} round the lander's place. */
    private static List<MovingStructure> left(final ServerLevel level, final StructureUuid id, final int ground) {
        final List<MovingStructure> out = new ArrayList<>();
        for (final IStructureBody near : MovingStructures.near(level, new AABB(-NEAR, ground - NEAR, -NEAR, NEAR,
                ground + NEAR, NEAR))) {
            if (near instanceof MovingStructure standing && !standing.id().equals(id)
                    && Math.abs(standing.worldBox().minY - ground) <= ON_THE_GROUND) {
                out.add(standing);
            }
        }
        return out;
    }

    private static @Nullable MovingStructure body(final ServerLevel level, final StructureUuid id) {
        return MovingStructures.byId(level, id);
    }

    private static ServerLevel levelOf(final ServerLevel level, final SolarBody body) {
        return level.getServer().getLevel(SpaceWorlds.get(level, SolarSystemWorlds.worldOf(body)).orElseThrow()
                .dimension());
    }

    private static int chunk(final double block) {
        return SectionPos.blockToSectionCoord(block);
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

    /* What its tank at {@code tank} holds now, its losses reckoned. */
    private static double held(final ServerLevel level, final StructureUuid id, final int tank) {
        return Rockets.settle(level.getServer(), id).map(rocket -> rocket.tanks().get(tank)).map(TankLoad::total)
                .orElse(0.0);
    }

    /* Builds {@code stack} at the arena's foot, every part formed, its tank at {@code tank} holding {@code load}. */
    private static FlightComputerBlockEntity build(final GameTestHelper helper, final RocketStack stack,
                                                   final int tank, final double load) {
        final ServerLevel level = helper.getLevel();
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
        helper.assertTrue(computer != null, "it holds its flight computer");
        final List<Integer> formed = new ArrayList<>();
        for (int section = 0; section < stack.sections().size(); section++) {
            formed.add(section);
        }
        computer.carry(new RocketDesign("LANDER", stack), foot.subtract(at), formed);
        Propellants.get(level, SkeletonPropellants.METHALOX).ifPresent(methalox -> computer.load(tank,
                TankLoad.filled(SkeletonPropellants.METHALOX, methalox, load)));
        return computer;
    }

    private static RocketStack.Part part(final String path) {
        return new RocketStack.Part(SkeletonRocketParts.id(path), 1);
    }
}
