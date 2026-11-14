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
import dev.jstech.core.multiblock.plan.BuildPlan;
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.multiblock.plan.PlannedBuild;
import dev.jstech.core.team.Access;
import dev.jstech.core.team.Ownership;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import dev.jstech.space.api.Propellants;
import dev.jstech.space.api.RocketPart;
import dev.jstech.space.api.RocketParts;
import dev.jstech.space.flight.FlightComputerBlockEntity;
import dev.jstech.space.flight.FlightPage;
import dev.jstech.space.flight.StageChoice;
import dev.jstech.space.hangar.DesignEdits;
import dev.jstech.space.hangar.DesignReport;
import dev.jstech.space.hangar.DesignTexts;
import dev.jstech.space.hangar.RocketDesign;
import dev.jstech.space.hangar.RocketPlans;
import dev.jstech.space.launch.RocketLaunch;
import dev.jstech.space.launcher.LauncherBlocks;
import dev.jstech.space.platform.LandingPlatforms;
import dev.jstech.space.rocket.RocketAscent;
import dev.jstech.space.rocket.RocketKitBlocks;
import dev.jstech.space.rocket.RocketPieces;
import dev.jstech.space.rocket.RocketStack;
import dev.jstech.space.rocket.RocketStages;
import dev.jstech.space.rocket.Rockets;
import dev.jstech.space.rocket.SkeletonPropellants;
import dev.jstech.space.rocket.SkeletonRocketParts;
import dev.jstech.space.rocket.StageFate;
import dev.jstech.space.rocket.StageFlights;
import dev.jstech.space.rocket.TankLoad;
import dev.jstech.space.universe.SolarBody;
import dev.jstech.tests.JsTests;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Boosters, columns of a rocket's own parts hung beside its first stage's engine: the design takes them there alone,
 * each an engine at its foot with tanks of its size over it, one, two or four round the engine, the rocket as wide as
 * they stand; its plan lays each booster's parts beside the engine, its foot level with the core's, and the launcher
 * opens round their feet; let go, they fall where the first stage was set to go, never to a platform; and in the climb
 * they come loose before the first stage, each a body of its own, the flight computer drawing them no more.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class BoosterGameTests {

    private static final String ARENA = "bench";
    private static final String CLIMB = "jsspace_boosters_climb";
    /* Where the rocket's foot stands, the block keeping its plan while built, and a landing platform's terminal. */
    private static final BlockPos FOOT = new BlockPos(8, 1, 8);
    private static final BlockPos OWNER = new BlockPos(1, 1, 1);
    private static final BlockPos PLATFORM = new BlockPos(36, 0, 36);
    private static final int PLATFORM_SIDE = 9;
    private static final ResourceLocation SEA_ENGINE = SkeletonRocketParts.id("sea_level_engine_3");
    private static final ResourceLocation VACUUM_ENGINE = SkeletonRocketParts.id("vacuum_engine_3");
    private static final ResourceLocation TANK = SkeletonRocketParts.id("tank_3");
    private static final ResourceLocation BIG_TANK = SkeletonRocketParts.id("tank_5");
    private static final ResourceLocation RING = SkeletonRocketParts.id("separation_ring_3");
    private static final ResourceLocation LEGS = SkeletonRocketParts.id("landing_legs_3");
    private static final ResourceLocation CAPSULE = SkeletonRocketParts.id("capsule_3");
    private static final ResourceLocation BIG_ENGINE = SkeletonRocketParts.id("sea_level_engine_5");
    private static final ResourceLocation BIG_CAPSULE = SkeletonRocketParts.id("capsule_5");
    /*
     * What the first stage's tank holds, some eight ticks of its engine, and the share the boosters hold, under a
     * tick of theirs: the boosters come loose a read of the flight computer before the stage, and the rocket, its
     * second stage an empty tank of many layers, climbs a hundred blocks or so and stays in the world's sky.
     */
    private static final double FIRST = 210;
    private static final double BOOSTERS_LEFT = 0.002;
    private static final int HEAVY = 16;
    /* How long the flight computer aboard ticks before the launch, for it to read the flight at its own pace. */
    private static final int SETTLE = 25;
    /* How far round the rocket its boosters are looked for, across and up. */
    private static final double LOOK_ACROSS = 96;
    private static final double LOOK_UP = 900;

    private BoosterGameTests() {
    }

    @GameTest(template = "empty")
    public static void report_takesBoostersBesideTheFirstEngineAsColumnsOfItsParts(final GameTestHelper helper) {
        final Map<ResourceLocation, RocketPart> parts = RocketParts.all(helper.getLevel());
        final DesignReport hung = overworld(parts, boosted(List.of(part(SEA_ENGINE), part(TANK)), 2));
        helper.assertTrue(hung.ready() && !says(hung, DesignTexts.BOOSTERS_AT_ENGINE)
                && !says(hung, DesignTexts.BOOSTER_COLUMN), "two boosters beside the engine fly: " + hung.checks());
        final List<RocketStack.Section> sections = new ArrayList<>(core().sections());
        sections.set(1, new RocketStack.Section(sections.get(1).part(), List.of(List.of(part(SEA_ENGINE),
                part(TANK)))));
        final DesignReport atTank = overworld(parts, new RocketStack(sections));
        helper.assertTrue(!atTank.ready() && says(atTank, DesignTexts.BOOSTERS_AT_ENGINE),
                "a booster beside a tank is refused: " + atTank.checks());
        final DesignReport tankOnly = overworld(parts, boosted(List.of(part(TANK)), 1));
        helper.assertTrue(!tankOnly.ready() && says(tankOnly, DesignTexts.BOOSTER_COLUMN),
                "a booster of a tank alone, with no engine, is refused: " + tankOnly.checks());
        final DesignReport engineOnly = overworld(parts, boosted(List.of(part(SEA_ENGINE)), 1));
        helper.assertTrue(!engineOnly.ready() && says(engineOnly, DesignTexts.BOOSTER_COLUMN),
                "an engine with nothing to burn is refused: " + engineOnly.checks());
        final DesignReport upsideDown = overworld(parts, boosted(List.of(part(TANK), part(SEA_ENGINE)), 2));
        helper.assertTrue(!upsideDown.ready() && says(upsideDown, DesignTexts.BOOSTER_COLUMN),
                "an engine over its tank is refused: " + upsideDown.checks());
        final DesignReport mixed = overworld(parts, boosted(List.of(part(SEA_ENGINE), part(BIG_TANK)), 2));
        helper.assertTrue(!mixed.ready() && says(mixed, DesignTexts.BOOSTER_COLUMN),
                "a tank of another size over the engine is refused: " + mixed.checks());
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void report_widthCountsEachBoosterOnItsOwnSide(final GameTestHelper helper) {
        final Map<ResourceLocation, RocketPart> parts = RocketParts.all(helper.getLevel());
        final List<RocketStack.Part> booster = List.of(part(SEA_ENGINE), part(TANK));
        helper.assertTrue(overworld(parts, big(0)).width() == 5, "a core of five stands five wide");
        helper.assertTrue(overworld(parts, big(1)).width() == 8, "one booster of three widens it on its side alone: "
                + overworld(parts, big(1)).width());
        helper.assertTrue(overworld(parts, big(2)).width() == 11, "two, one each side: "
                + overworld(parts, big(2)).width());
        helper.assertTrue(overworld(parts, big(4)).width() == 11, "four, two each way: "
                + overworld(parts, big(4)).width());
        helper.assertTrue(overworld(parts, boosted(booster, 2)).width() == 9, "a core of three and two of three: "
                + overworld(parts, boosted(booster, 2)).width());
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mirror_hangsOneTwoOrFour(final GameTestHelper helper) {
        helper.assertTrue(DesignEdits.nextMirror(1) == 2 && DesignEdits.nextMirror(2) == 4
                && DesignEdits.nextMirror(4) == 1, "the mirror steps one, two, four and round");
        final RocketStack three = DesignEdits.hang(core(), 0, part(SEA_ENGINE), 3);
        helper.assertTrue(three.sections().get(0).boosters().size() == 1, "three, off the blocks, hangs one");
        final RocketStack four = DesignEdits.hang(core(), 0, part(SEA_ENGINE), 4);
        final RocketStack stacked = DesignEdits.hang(four, 0, part(TANK), 1);
        helper.assertTrue(stacked.sections().get(0).boosters().size() == 4 && stacked.sections().get(0).boosters()
                .stream().allMatch(column -> column.size() == 2), "a tank stacks on each of the four");
        final List<List<RocketStack.Part>> threeHung = List.of(List.of(part(SEA_ENGINE)), List.of(part(SEA_ENGINE)),
                List.of(part(SEA_ENGINE)));
        final List<RocketStack.Section> sections = new ArrayList<>(core().sections());
        sections.set(0, new RocketStack.Section(sections.get(0).part(), threeHung));
        helper.assertTrue(!new RocketDesign("THREE", new RocketStack(sections)).bounded()
                && new RocketDesign("FOUR", stacked).bounded(), "a design of three boosters is not taken, of four is");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void plan_laysEachBoosterBesideTheEngineAsItsOwnParts(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final RocketStack stack = boosted(List.of(part(SEA_ENGINE), part(TANK, 2)), 2);
        final BuildPlan plan = RocketPlans.of(level, stack);
        helper.assertTrue(plan.parts() == stack.sections().size() + 4 && RocketPieces.count(stack) == plan.parts(),
                "its parts are the core's and each booster segment: " + plan.parts());
        final Set<BlockPos> places = new HashSet<>();
        int east = Integer.MIN_VALUE;
        int west = Integer.MAX_VALUE;
        int lowestBooster = Integer.MAX_VALUE;
        for (final PlanSpot spot : plan.spots()) {
            helper.assertTrue(places.add(spot.at()), "no two blocks of its plan share a place: " + spot.at());
            if (spot.part() >= stack.sections().size()) {
                helper.assertTrue(spot.place().isPresent(), "each block of a booster merges into its part's model");
                east = Math.max(east, spot.at().getX());
                west = Math.min(west, spot.at().getX());
                lowestBooster = Math.min(lowestBooster, spot.at().getY());
                helper.assertTrue(Math.abs(spot.at().getZ()) <= 1, "east and west, across the core: " + spot.at());
            }
        }
        helper.assertTrue(east == 4 && west == -4, "each booster of three touches the core of three, its middle three"
                + " out: " + west + " to " + east);
        helper.assertTrue(lowestBooster == 0, "their feet stand level with the core's: " + lowestBooster);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void launcher_opensRoundTheBoostersFeet(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final RocketStack stack = boosted(List.of(part(SEA_ENGINE), part(TANK, 2)), 4);
        final BuildPlan withLauncher = RocketPlans.of(level, stack, Direction.NORTH);
        final int rocketParts = RocketPieces.count(stack);
        final Set<BlockPos> rocket = new HashSet<>();
        for (final PlanSpot spot : withLauncher.spots()) {
            if (spot.part() < rocketParts) {
                rocket.add(spot.at());
            }
        }
        for (final PlanSpot spot : withLauncher.spots()) {
            if (spot.part() >= rocketParts) {
                helper.assertTrue(!rocket.contains(spot.at()), "nothing of the launcher where the rocket stands: "
                        + spot.at());
                if (spot.block().equals(LauncherBlocks.LAUNCHER_DECK.getId())) {
                    helper.assertTrue(Math.max(Math.abs(spot.at().getX()), Math.abs(spot.at().getZ())) > 4,
                            "the deck opens past each booster's foot: " + spot.at());
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void fates_sendTheBoostersWhereTheFirstStageGoesButNeverToAPlatform(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final UUID platform = UUID.randomUUID();
        LandingPlatforms.note(level, platform, helper.absolutePos(PLATFORM), PLATFORM_SIDE,
                Ownership.of(UUID.randomUUID()).withAccess(Access.TEAM));
        // Its first stage with legs, so it may be set to come back.
        final List<RocketStack.Section> sections = new ArrayList<>(flying().sections());
        sections.add(1, new RocketStack.Section(part(LEGS), List.of()));
        final FlightComputerBlockEntity computer = build(helper, new RocketStack(sections));
        final BlockPos at = helper.absolutePos(FOOT);
        helper.assertTrue(!computer.fates(Level.OVERWORLD, at).containsKey(RocketStages.BOOSTERS),
                "the first stage set nowhere, the boosters fall anywhere too");
        computer.press(FlightComputerBlockEntity.TOP + FlightPage.KEYS.indexOf(FlightPage.STAGES));
        computer.press(FlightComputerBlockEntity.LEFT, null);
        computer.press(FlightComputerBlockEntity.RIGHT + 1, null);
        helper.assertTrue(computer.fates(Level.OVERWORLD, at).get(RocketStages.BOOSTERS)
                .equals(computer.fates(Level.OVERWORLD, at).get(1)), "the first stage to a fall zone, so the boosters");
        computer.press(FlightComputerBlockEntity.RIGHT + 2, null);
        helper.assertTrue(computer.choices().get(1) != null && computer.choices().get(1).lands(),
                "the first stage is set to come back to the platform");
        helper.assertTrue(computer.fates(Level.OVERWORLD, at).get(RocketStages.BOOSTERS) instanceof StageFate.FallZone
                zone && zone.radius() == StageChoice.FIRST_RADIUS && zone.centre().equals(at),
                "the boosters, with no legs, fall in a zone round the pad");
        LandingPlatforms.drop(level.getServer(), platform);
        helper.succeed();
    }

    /**
     * In the climb the boosters come loose once their burn is behind and before the first stage, each a body of its
     * own pushed out from the core, falling where they were set to; the flight computer draws them no more.
     */
    @GameTest(template = ARENA, batch = CLIMB, timeoutTicks = 12000)
    public static void climb_letsTheBoostersGoBeforeTheFirstStage(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final List<RocketStack.Section> sections = new ArrayList<>(flying().sections());
        sections.set(4, new RocketStack.Section(part(TANK, HEAVY), List.of()));
        final RocketStack stack = new RocketStack(sections);
        final FlightComputerBlockEntity computer = build(helper, stack);
        final List<Integer> boosters = RocketPieces.boosterNumbers(stack);
        helper.assertTrue(computer.formed().containsAll(boosters), "built whole, its computer draws the boosters too");
        final MovingStructure body = RocketLaunch.release(level, computer.getBlockPos(), null).orElseThrow();
        final BlockPos[] aboard = new BlockPos[1];
        final long[] lifted = new long[1];
        final double dry = RocketStages.boosterDryMass(RocketParts.all(level), stack.sections().get(0).boosters()
                .get(0));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(body.ready(), "its body stands ready"))
                .thenExecute(() -> aboard[0] = BlockPos.containing(MovingStructures.toYard(body, Vec3.atCenterOf(
                        computer.getBlockPos()))))
                .thenWaitUntil(() -> helper.assertTrue(ticking(level, aboard[0]),
                        "its flight computer's place in the yard ticks"))
                // On the ground its computer looked a second apart: aboard, it reads the flight a quarter second apart.
                .thenIdle(SETTLE)
                .thenExecute(() -> {
                    // Next to nothing left in the boosters: their burn is over in a tick.
                    Rockets.boosters(level.getServer(), body.id(), 0, BOOSTERS_LEFT);
                    RocketLaunch.lift(level, body);
                    lifted[0] = level.getGameTime();
                })
                .thenWaitUntil(() -> {
                    final MovingStructure now = MovingStructures.byId(level, body.id());
                    helper.assertTrue(level.getBlockEntity(aboard[0]) instanceof FlightComputerBlockEntity,
                            "its computer stays aboard: " + (now == null ? "the body gone" : "the body of "
                            + now.blocks() + " blocks in " + now.bounds() + " at " + now.pose() + " moving by "
                            + now.motion()) + "; pieces " + pieces(level, body, dry).size());
                    helper.assertTrue(flying(level, aboard[0]).boostersGone(), "the boosters come loose");
                })
                .thenExecute(() -> {
                    final FlightComputerBlockEntity flying = flying(level, aboard[0]);
                    helper.assertTrue(flying.separated() == 0 && flying.formed().stream().noneMatch(boosters::contains)
                            && flying.formed().containsAll(List.of(0, 1, 2)), "before the first stage, its computer"
                            + " drawing them no more and its first stage still: separated " + flying.separated()
                            + ", drawn " + flying.formed() + ", the climb " + MovingStructures.byId(level, body.id())
                            .motion() + " " + (level.getGameTime() - lifted[0]) + " ticks after lift-off, its burns "
                            + RocketAscent.burns(level, body).stream().map(burn -> burn.stage() + (burn.boosters()
                            ? "B" : "") + ":" + Math.round(burn.startMass()) + ">" + Math.round(burn.endMass()))
                            .toList());
                })
                .thenWaitUntil(() -> helper.assertTrue(pieces(level, body, dry).size() == 2,
                        "each booster a body of its own, a booster's mass: " + pieces(level, body, dry).size()))
                .thenExecute(() -> {
                    final List<MovingStructure> fallen = pieces(level, body, dry);
                    for (final MovingStructure piece : fallen) {
                        helper.assertTrue(StageFlights.get(level.getServer(), piece.id()).map(flight -> !flight.lands())
                                .orElse(false), "it falls, never coming back");
                        MovingStructures.takeAway(piece);
                    }
                    MovingStructures.takeAway(body);
                })
                .thenSucceed();
    }

    /*
     * Builds {@code stack} by its plan at the arena's foot, every part formed, lets its plan go with its flight
     * computer as its hangar does, every part of it formed, and fills the first stage's tank; gives its flight
     * computer.
     */
    private static FlightComputerBlockEntity build(final GameTestHelper helper, final RocketStack stack) {
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
        helper.assertTrue(build.complete(), "the rocket is built whole, its boosters with it");
        build.stop(false);
        final FlightComputerBlockEntity computer = (FlightComputerBlockEntity) level.getBlockEntity(at);
        helper.assertTrue(computer != null, "its capsule holds its flight computer");
        final List<Integer> formed = new ArrayList<>();
        for (int part = 0; part < RocketPieces.count(stack); part++) {
            formed.add(part);
        }
        computer.carry(new RocketDesign("BOOSTED", stack), foot.subtract(at), formed);
        Propellants.get(level, SkeletonPropellants.METHALOX).ifPresent(methalox -> computer.load(1,
                TankLoad.filled(SkeletonPropellants.METHALOX, methalox, FIRST)));
        return computer;
    }

    /*
     * Whether the block entity at {@code pos} ticks: the game ticks one only where its blocks tick and its chunk's
     * entities have loaded, which may come later.
     */
    private static boolean ticking(final ServerLevel level, final BlockPos pos) {
        return level.shouldTickBlocksAt(pos) && level.areEntitiesLoaded(ChunkPos.asLong(pos));
    }

    /* The flight computer standing at {@code at}, in the yard; the test fails where none does. */
    private static FlightComputerBlockEntity flying(final ServerLevel level, final BlockPos at) {
        if (level.getBlockEntity(at) instanceof FlightComputerBlockEntity computer) {
            return computer;
        }
        throw new GameTestAssertException("no flight computer aboard at " + at + ": " + level.getBlockState(at)
                + ", the structure there " + MovingStructures.at(level, at));
    }

    /* The bodies near the rocket {@code body} on their way as spent pieces, each of {@code dry} kilograms empty. */
    private static List<MovingStructure> pieces(final ServerLevel level, final MovingStructure body,
                                                final double dry) {
        final Vec3 at = new Vec3(body.pose().x(), body.pose().y(), body.pose().z());
        final List<MovingStructure> out = new ArrayList<>();
        for (final IStructureBody near : MovingStructures.near(level, new AABB(at, at).inflate(LOOK_ACROSS, LOOK_UP,
                LOOK_ACROSS))) {
            if (near instanceof MovingStructure piece && !piece.id().equals(body.id())
                    && StageFlights.get(level.getServer(), piece.id()).map(flight -> Math.abs(flight.dry() - dry)
                    < 1).orElse(false)) {
                out.add(piece);
            }
        }
        return out;
    }

    /* A two-stage rocket of three, a capsule on top, its first stage's engine at its foot. */
    private static RocketStack core() {
        return RocketStack.of(part(SEA_ENGINE), part(TANK), part(RING), part(VACUUM_ENGINE), part(TANK),
                part(CAPSULE));
    }

    /* {@link #core()} with {@code count} boosters of {@code booster} hung beside its engine. */
    private static RocketStack boosted(final List<RocketStack.Part> booster, final int count) {
        final List<RocketStack.Section> sections = new ArrayList<>(core().sections());
        final List<List<RocketStack.Part>> hung = new ArrayList<>();
        for (int b = 0; b < count; b++) {
            hung.add(booster);
        }
        sections.set(0, new RocketStack.Section(sections.get(0).part(), hung));
        return new RocketStack(sections);
    }

    /* A rocket of five, its engine with {@code count} boosters of three beside it. */
    private static RocketStack big(final int count) {
        final List<List<RocketStack.Part>> hung = new ArrayList<>();
        for (int b = 0; b < count; b++) {
            hung.add(List.of(part(SEA_ENGINE), part(TANK)));
        }
        return new RocketStack(List.of(new RocketStack.Section(part(BIG_ENGINE), hung),
                new RocketStack.Section(part(BIG_TANK), List.of()),
                new RocketStack.Section(part(BIG_CAPSULE), List.of())));
    }

    /* The rocket the flight tests fly: {@link #core()} with two boosters of an engine and a tank. */
    private static RocketStack flying() {
        return boosted(List.of(part(SEA_ENGINE), part(TANK)), 2);
    }

    private static DesignReport overworld(final Map<ResourceLocation, RocketPart> parts, final RocketStack stack) {
        return DesignReport.of(parts, stack, 1, 1, Optional.of(SolarBody.OVERWORLD), Optional.empty());
    }

    private static boolean says(final DesignReport report, final TextKey key) {
        return report.checks().stream().map(DesignReport.Check::text).anyMatch(text -> text
                instanceof Text.Translated translated && translated.key().equals(key));
    }

    private static RocketStack.Part part(final ResourceLocation id) {
        return new RocketStack.Part(id, 1);
    }

    private static RocketStack.Part part(final ResourceLocation id, final int layers) {
        return new RocketStack.Part(id, layers);
    }
}
