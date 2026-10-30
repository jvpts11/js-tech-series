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
import dev.jstech.core.multiblock.FormedPartBlockEntity;
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.multiblock.plan.PlannedBuild;
import dev.jstech.core.region.Box;
import dev.jstech.core.team.Access;
import dev.jstech.core.team.Ownership;
import dev.jstech.space.api.Propellants;
import dev.jstech.space.flight.FlightComputerBlockEntity;
import dev.jstech.space.flight.FlightPage;
import dev.jstech.space.hangar.RocketDesign;
import dev.jstech.space.hangar.RocketPlans;
import dev.jstech.space.launch.RocketLaunch;
import dev.jstech.space.platform.LandingPlatforms;
import dev.jstech.space.rocket.RocketAscent;
import dev.jstech.space.rocket.RocketKitBlocks;
import dev.jstech.space.rocket.RocketStack;
import dev.jstech.space.rocket.Rockets;
import dev.jstech.space.rocket.SkeletonPropellants;
import dev.jstech.space.rocket.SkeletonRocketParts;
import dev.jstech.space.rocket.StageFate;
import dev.jstech.space.rocket.StageFlights;
import dev.jstech.space.rocket.TankLoad;
import dev.jstech.space.wreck.RocketWreckage;
import dev.jstech.space.wreck.StageImpacts;
import dev.jstech.tests.JsTests;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jetbrains.annotations.Nullable;

/**
 * A rocket's stages let go as it climbs: its first stage burnt out, its flight computer lets it go as a body of its
 * own, its parts drawn by themselves and no more by the computer, the rocket climbing on as it was; a stage set to go
 * anywhere falls by its physics and strikes the ground as wreckage; one with legs, set on the stages page to come
 * back to a platform, flies back and lands on its middle.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class StagingGameTests {

    private static final String ARENA = "bench";
    private static final String FALLS = "jsspace_staging_falls";
    private static final String LANDS = "jsspace_staging_lands";
    /* Where the rocket's foot stands, the block keeping its plan while it is built, and the platform's terminal. */
    private static final BlockPos FOOT = new BlockPos(8, 1, 8);
    private static final BlockPos OWNER = new BlockPos(1, 1, 1);
    private static final BlockPos PLATFORM = new BlockPos(36, 0, 36);
    private static final int PLATFORM_SIDE = 9;
    /*
     * What the first stage's tank holds, burnt out in a moment, and the second's; the rocket whose first stage falls
     * holds next to nothing in it and none in the second, so that stage is let go just over the pad going slowly, as
     * the structures' physics runs in the game's own time and not the test's.
     */
    private static final double FIRST = 300;
    private static final double SECOND = 2_000;
    private static final double FIRST_FALLING = 20;
    /* How far round the rocket its stage is looked for, across and up, and where its wreckage may lie. */
    private static final double LOOK_ACROSS = 96;
    private static final double LOOK_UP = 900;
    private static final int WRECK_ACROSS = 16;

    private StagingGameTests() {
    }

    @GameTest(template = ARENA, batch = FALLS, timeoutTicks = 12000)
    public static void stage_isLetGoDrawnByItselfAndFallsToWreckage(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final RocketStack stack = RocketStack.of(part("sea_level_engine_3"), part("tank_3"), part("separation_ring_3"),
                part("vacuum_engine_3"), part("tank_3"), part("capsule_3"));
        final FlightComputerBlockEntity computer = build(helper, stack, Map.of(1, FIRST_FALLING));
        final MovingStructure body = RocketLaunch.release(level, computer.getBlockPos(), null).orElseThrow();
        final BlockPos[] aboard = new BlockPos[1];
        final MovingStructure[] stage = new MovingStructure[1];
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(body.ready(), "its body stands ready"))
                .thenExecute(() -> {
                    aboard[0] = BlockPos.containing(MovingStructures.toYard(body, Vec3.atCenterOf(
                            computer.getBlockPos())));
                    RocketLaunch.lift(level, body);
                })
                .thenWaitUntil(() -> {
                    stage[0] = stageOf(level, body);
                    helper.assertTrue(stage[0] != null, "its first stage burnt out, it comes loose as a body");
                })
                .thenExecute(() -> {
                    final FlightComputerBlockEntity flying = (FlightComputerBlockEntity) level.getBlockEntity(
                            aboard[0]);
                    helper.assertTrue(flying != null && flying.separated() == 1
                            && flying.formed().equals(List.of(3, 4, 5)), "its computer draws only what is left");
                    helper.assertTrue(body.motion() instanceof RocketAscent && Rockets.get(level.getServer(),
                            body.id()).isPresent(), "the rocket climbs on as it was, by its name");
                    helper.assertTrue(drawers(level, stage[0]) == 3, "each of the stage's three parts draws itself: "
                            + drawers(level, stage[0]));
                    helper.assertTrue(StageFlights.get(level.getServer(), stage[0].id()).map(flight -> flight.near()
                            && !flight.lands()).orElse(false), "set to go nowhere, it falls in sight");
                    MovingStructures.takeAway(body);
                })
                .thenWaitUntil(() -> {
                    final MovingStructure live = MovingStructures.byId(level, stage[0].id());
                    helper.assertTrue(live == null && StageFlights.get(level.getServer(), stage[0].id()).isEmpty(),
                            "it strikes the ground: " + (live == null ? "gone" : live.pose() + " moving by "
                            + live.motion() + " physics " + (live.physics() == null ? "none" : "asleep "
                            + live.physics().asleep())) + ", flight " + StageFlights.get(level.getServer(),
                            stage[0].id()));
                })
                .thenWaitUntil(() -> helper.assertTrue(wreckage(helper), "its wreckage lies where it struck: last at "
                        + stage[0].pose() + ", foot " + helper.absolutePos(FOOT) + ", blasts going "
                        + StageImpacts.pending(level)))
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = LANDS, timeoutTicks = 3000)
    public static void stage_setToAPlatformFliesBackAndLandsOnIt(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final UUID platform = UUID.randomUUID();
        final BlockPos terminal = helper.absolutePos(PLATFORM);
        LandingPlatforms.note(level, platform, terminal, PLATFORM_SIDE,
                Ownership.of(UUID.randomUUID()).withAccess(Access.TEAM));
        final RocketStack stack = RocketStack.of(part("sea_level_engine_3"), part("landing_legs_3"), part("tank_3"),
                part("separation_ring_3"), part("vacuum_engine_3"), part("tank_3"), part("capsule_3"));
        final FlightComputerBlockEntity computer = build(helper, stack, Map.of(2, FIRST, 5, SECOND));
        // On the stages page: the first stage, to come back to the nearest platform.
        computer.press(FlightComputerBlockEntity.TOP + FlightPage.KEYS.indexOf(FlightPage.STAGES));
        computer.press(FlightComputerBlockEntity.LEFT, null);
        computer.press(FlightComputerBlockEntity.RIGHT + 2, null);
        helper.assertTrue(computer.choices().get(1) != null && computer.choices().get(1).platform()
                .map(platform::equals).orElse(false), "its first stage is set to land on the platform");
        final MovingStructure body = RocketLaunch.release(level, computer.getBlockPos(), null).orElseThrow();
        helper.assertTrue(Rockets.get(level.getServer(), body.id()).flatMap(rocket -> rocket.fate(1))
                .map(fate -> fate instanceof StageFate.Platform(final UUID set) && set.equals(platform))
                .orElse(false), "the rocket keeps where its stage goes");
        final MovingStructure[] stage = new MovingStructure[1];
        final Vec3 spot = Vec3.atBottomCenterOf(terminal.above());
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(body.ready(), "its body stands ready"))
                .thenExecute(() -> RocketLaunch.lift(level, body))
                .thenWaitUntil(() -> {
                    stage[0] = stageOf(level, body);
                    helper.assertTrue(stage[0] != null, "its first stage comes loose");
                })
                .thenExecute(() -> {
                    helper.assertTrue(StageFlights.get(level.getServer(), stage[0].id()).map(
                            StageFlights.Flight::lands).orElse(false), "it comes back to land");
                    MovingStructures.takeAway(body);
                })
                .thenWaitUntil(() -> helper.assertTrue(stage[0].motion() == null
                        && Math.abs(stage[0].pose().x() - spot.x) < 0.01 && Math.abs(stage[0].pose().z() - spot.z)
                        < 0.01 && Math.abs(stage[0].pose().y() - spot.y) < 0.01, "it lands on the platform's middle: "
                        + stage[0].pose()))
                .thenExecute(() -> {
                    helper.assertTrue(LandingPlatforms.get(level.getServer(), platform).map(found -> !found.free())
                            .orElse(false), "the platform is kept for it");
                    MovingStructures.takeAway(stage[0]);
                    LandingPlatforms.drop(level.getServer(), platform);
                })
                .thenSucceed();
    }

    /*
     * Builds {@code stack} by its plan at the arena's foot, every part formed, lets its plan go with its flight
     * computer as its hangar does and fills its tanks as {@code loads} say; gives its flight computer.
     */
    private static FlightComputerBlockEntity build(final GameTestHelper helper, final RocketStack stack,
                                                   final Map<Integer, Double> loads) {
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
        helper.assertTrue(computer != null, "its capsule holds its flight computer");
        final List<Integer> formed = new ArrayList<>();
        for (int section = 0; section < stack.sections().size(); section++) {
            formed.add(section);
        }
        computer.carry(new RocketDesign("STAGED", stack), foot.subtract(at), formed);
        Propellants.get(level, SkeletonPropellants.METHALOX).ifPresent(methalox -> loads.forEach((section, kilograms)
                -> computer.load(section, TankLoad.filled(SkeletonPropellants.METHALOX, methalox, kilograms))));
        return computer;
    }

    /* The body near the rocket {@code body} that is not it: its stage, once let go. */
    private static @Nullable MovingStructure stageOf(final ServerLevel level, final MovingStructure body) {
        final Vec3 at = new Vec3(body.pose().x(), body.pose().y(), body.pose().z());
        for (final IStructureBody near : MovingStructures.near(level, new AABB(at, at).inflate(LOOK_ACROSS, LOOK_UP,
                LOOK_ACROSS))) {
            if (near instanceof MovingStructure piece && !piece.id().equals(body.id())
                    && StageFlights.get(level.getServer(), piece.id()).isPresent()) {
                return piece;
            }
        }
        return null;
    }

    /* How many blocks of {@code piece} draw their part's model themselves. */
    private static int drawers(final ServerLevel level, final MovingStructure piece) {
        final Box box = piece.bounds();
        int count = 0;
        for (final BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(),
                box.maxZ())) {
            if (level.getBlockEntity(pos) instanceof FormedPartBlockEntity part && part.drawsPart()) {
                count++;
            }
        }
        return count;
    }

    /* Whether the stage's wreckage lies round where the rocket stood. */
    private static boolean wreckage(final GameTestHelper helper) {
        final BlockPos foot = helper.absolutePos(FOOT);
        for (final BlockPos pos : BlockPos.betweenClosed(foot.offset(-WRECK_ACROSS, -WRECK_ACROSS, -WRECK_ACROSS),
                foot.offset(WRECK_ACROSS, WRECK_ACROSS, WRECK_ACROSS))) {
            if (helper.getLevel().getBlockState(pos).is(RocketWreckage.BLOCK.get())) {
                return true;
            }
        }
        return false;
    }

    private static RocketStack.Part part(final String path) {
        return new RocketStack.Part(SkeletonRocketParts.id(path), 1);
    }
}
