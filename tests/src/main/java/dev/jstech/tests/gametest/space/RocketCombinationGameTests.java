/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.multiblock.plan.PlannedBuild;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import dev.jstech.space.api.Propellants;
import dev.jstech.space.api.RocketPart;
import dev.jstech.space.api.RocketParts;
import dev.jstech.space.flight.FlightComputerBlockEntity;
import dev.jstech.space.hangar.DesignDestinations;
import dev.jstech.space.hangar.DesignReport;
import dev.jstech.space.hangar.DesignTexts;
import dev.jstech.space.hangar.HangarBay;
import dev.jstech.space.hangar.HangarStructure;
import dev.jstech.space.hangar.RocketDesign;
import dev.jstech.space.hangar.RocketPlans;
import dev.jstech.space.rocket.RocketKitBlocks;
import dev.jstech.space.rocket.RocketPieces;
import dev.jstech.space.rocket.RocketStack;
import dev.jstech.space.rocket.RocketStages;
import dev.jstech.space.rocket.SkeletonPropellants;
import dev.jstech.space.rocket.SkeletonRocketParts;
import dev.jstech.space.rocket.StageMath;
import dev.jstech.space.rocket.TankLoad;
import dev.jstech.space.universe.SolarBody;
import dev.jstech.tests.JsTests;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The rockets today's parts make, combined: each part fits only on a part of its own diameter, and put on one of the
 * other is refused at its place; from one stage to more than a hangar builds, in each diameter, each is ready or
 * refused only for standing too tall or being too weak to lift off; a stage too heavy for its engine stays down on the
 * Overworld and leaps off the Moon; a capsule of each diameter and a lunar lander's ascent stage carry the heat shield
 * the screen names and the flight computer counts; what the screen says each rocket has past the climb to the low
 * orbit, with boosters and without, a lander on top or a capsule, is what its flight computer counts on the pad; and
 * every part of each diameter stacked in one rocket, and one with four boosters, lays whole, each part formed.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class RocketCombinationGameTests {

    private static final String ARENA = "empty";
    private static final String BATCH = "jsspace_rocket_combinations";
    private static final double CLOSE = 1e-6;
    /* Where a flight computer stands in the arena, given each rocket's plan in turn. */
    private static final BlockPos COMPUTER = new BlockPos(1, 2, 1);
    /* Where the first rocket laid whole stands, the middle of its foot, and how far east of it each next one does. */
    private static final BlockPos FIRST_FOOT = new BlockPos(8, 2, 8);
    private static final int APART = 14;
    /* The diameters today's parts come in. */
    private static final List<Integer> DIAMETERS = List.of(3, 5);
    /* Every part made in both diameters, from the bottom of a stack of two stages in which each fits. */
    private static final List<String> EVERY_PART = List.of("sea_level_engine", "tank", "separation_ring",
            "vacuum_engine", "landing_legs", "tank", "instrument_ring", "capsule");
    /* A tank's layers in the stacks of every part and in each stage counted to a hangar's height. */
    private static final int LAYERS = 2;
    private static final int STAGE_LAYERS = 1;
    /* The most stages counted, well past what a hangar builds. */
    private static final int MOST_STAGES = 40;
    /* A small tank's layers that make a rocket too heavy for its one engine on the Overworld. */
    private static final int HEAVY = 40;
    /* The Moon's pull, of the Overworld's. */
    private static final double MOON_PULL = 0.165;
    /* How near the screen's count and the flight computer's agree, in metres a second. */
    private static final double AGREE = 1.0;
    /* A hangar's bay, for the height it builds to. */
    private static final HangarBay BAY = new HangarBay(new BlockPos(0, 0, 0), new BlockPos(28, 107, 36),
            Direction.Axis.X, new BlockPos(0, 107, 18), new BlockPos(14, 106, 18));

    private RocketCombinationGameTests() {
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void parts_fitOnlyOnPartsOfTheirOwnDiameter(final GameTestHelper helper) {
        final Map<ResourceLocation, RocketPart> parts = RocketParts.all(helper.getLevel());
        for (final int diameter : DIAMETERS) {
            final RocketStack whole = everyPart(diameter);
            final DesignReport report = overworld(parts, whole);
            helper.assertTrue(report.ready() && RocketStages.misfit(parts, whole).isEmpty(), "every part of"
                    + " diameter " + diameter + " stands in order on one of its own: " + report.checks());
            final int other = diameter == DIAMETERS.get(0) ? DIAMETERS.get(1) : DIAMETERS.get(0);
            for (int place = 0; place < whole.sections().size(); place++) {
                // The part over the one swapped is the first that misfits at the foot; else the swapped one.
                final int refused = place == 0 ? 2 : place + 1;
                final DesignReport swapped = overworld(parts, swap(whole, place, other));
                helper.assertTrue(!swapped.ready() && swapped.checks().contains(new DesignReport.Check(
                        DesignReport.Severity.FAULT, DesignTexts.MISFIT.with(refused))), EVERY_PART.get(place) + " of"
                        + " diameter " + other + " in a stack of " + diameter + " is refused at part " + refused + ": "
                        + swapped.checks());
            }
        }
        helper.succeed();
    }

    /**
     * From one stage to more than a hangar's bay builds, in each diameter: the screen counts each stage, its delta-v
     * its vacuum's, and each rocket is ready but where it stands taller than the bay builds or too heavy to lift off,
     * refused then for that and for nothing else.
     */
    @GameTest(template = ARENA, batch = BATCH)
    public static void stages_fromOneToMoreThanAHangarBuildsInEachDiameter(final GameTestHelper helper) {
        final Map<ResourceLocation, RocketPart> parts = RocketParts.all(helper.getLevel());
        for (final int diameter : DIAMETERS) {
            int ready = 0;
            boolean tall = false;
            for (int count = 1; count <= MOST_STAGES && !tall; count++) {
                final RocketStack stack = staged(diameter, count);
                final DesignReport report = DesignReport.of(parts, stack, 1, 1, Optional.of(SolarBody.OVERWORLD),
                        Optional.of(BAY));
                final double counted = StageMath.deltaV(RocketStages.burns(parts, stack).orElseThrow(), 0);
                helper.assertTrue(report.stages().size() == count && Math.abs(report.deltaV() - counted) < CLOSE,
                        count + " stages of diameter " + diameter + ", each counted: " + report.stages());
                tall = report.height() > HangarStructure.TALLEST_ROCKET;
                final boolean weak = report.liftOff() <= 1;
                final List<DesignReport.Check> faults = report.checks().stream().filter(check -> check.severity()
                        == DesignReport.Severity.FAULT).toList();
                helper.assertTrue(report.ready() == (!tall && !weak) && says(report, DesignTexts.TOO_TALL) == tall
                        && says(report, DesignTexts.STAYS_DOWN) == weak && faults.stream().allMatch(fault
                        -> is(fault.text(), DesignTexts.TOO_TALL) || is(fault.text(), DesignTexts.STAYS_DOWN)),
                        count + " stages of diameter " + diameter + ", " + report.height() + " tall, lifting off at "
                        + report.liftOff() + ": " + report.checks());
                ready += report.ready() ? 1 : 0;
            }
            helper.assertTrue(tall && ready > 1, "of diameter " + diameter + ", rockets of " + ready + " stages are"
                    + " ready, and more stand taller than a hangar builds");
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void report_tooHeavyForItsEngineStaysDownAndLeapsOffTheMoon(final GameTestHelper helper) {
        final Map<ResourceLocation, RocketPart> parts = RocketParts.all(helper.getLevel());
        final RocketStack heavy = RocketStack.of(sized("sea_level_engine", 3, 1), sized("tank", 3, HEAVY),
                sized("capsule", 3, 1));
        final DesignReport home = overworld(parts, heavy);
        helper.assertTrue(!home.ready() && home.liftOff() <= 1 && says(home, DesignTexts.STAYS_DOWN),
                "too heavy for its one engine, it stays down on the Overworld: " + home.liftOff());
        final DesignReport moon = DesignReport.of(parts, heavy, MOON_PULL, 0, Optional.of(SolarBody.MOON),
                Optional.empty());
        helper.assertTrue(moon.ready() && moon.liftOff() > 1 && !says(moon, DesignTexts.STAYS_DOWN),
                "and leaps off the Moon: " + moon.liftOff());
        helper.succeed();
    }

    /**
     * A capsule of each diameter and a lunar lander's ascent stage, which brings its crew home from the Moon, each
     * carry a heat shield, as the screen says and the flight computer counts for a landing through air.
     */
    @GameTest(template = ARENA, batch = BATCH)
    public static void shield_ofEachCapsuleAndOfALandersAscentStage(final GameTestHelper helper) {
        final Map<ResourceLocation, RocketPart> parts = RocketParts.all(helper.getLevel());
        final List<RocketStack> shielded = List.of(
                RocketStack.of(sized("sea_level_engine", 3, 1), sized("tank", 3, LAYERS), sized("capsule", 3, 1)),
                RocketStack.of(sized("sea_level_engine", 5, 1), sized("tank", 5, LAYERS), sized("capsule", 5, 1)),
                RocketStack.of(sized("sea_level_engine", 5, 1), sized("tank", 5, LAYERS),
                        sized("separation_ring", 5, 1), lander("descent"), lander("ascent")),
                RocketStack.of(lander("descent"), lander("ascent")));
        for (final RocketStack stack : shielded) {
            final DesignReport report = overworld(parts, stack);
            helper.assertTrue(says(report, DesignTexts.SHIELD) && !says(report, DesignTexts.NO_SHIELD),
                    "the screen names its heat shield: " + report.checks());
            helper.assertTrue(carried(helper, stack).shield(), "and its flight computer counts it: " + stack);
        }
        helper.succeed();
    }

    /**
     * What the screen says a rocket has past the climb to the low orbit is what its flight computer counts on the pad:
     * of one stage and of two, in each diameter, with one, two or four boosters, and a lander on top.
     */
    @GameTest(template = ARENA, batch = BATCH)
    public static void screen_andFlightComputerCountAlikeWhatIsLeftPastTheClimb(final GameTestHelper helper) {
        final Map<ResourceLocation, RocketPart> parts = RocketParts.all(helper.getLevel());
        final List<RocketStack> rockets = new ArrayList<>();
        for (final int diameter : DIAMETERS) {
            final RocketStack two = RocketStack.of(sized("sea_level_engine", diameter, 1),
                    sized("tank", diameter, 14), sized("separation_ring", diameter, 1),
                    sized("vacuum_engine", diameter, 1), sized("tank", diameter, 6), sized("capsule", diameter, 1));
            rockets.add(RocketStack.of(sized("sea_level_engine", diameter, 1), sized("tank", diameter, 10),
                    sized("capsule", diameter, 1)));
            rockets.add(two);
            for (final int count : List.of(1, 2, 4)) {
                rockets.add(boosted(two, List.of(sized("sea_level_engine", diameter, 1), sized("tank", diameter,
                        LAYERS * 4)), count));
            }
        }
        rockets.add(RocketStack.of(sized("sea_level_engine", 5, 1), sized("tank", 5, 20),
                sized("separation_ring", 5, 1), sized("vacuum_engine", 5, 1), sized("tank", 5, 8),
                sized("separation_ring", 5, 1), lander("descent"), lander("ascent")));
        final double climb = need(DesignDestinations.from(SolarBody.OVERWORLD));
        for (final RocketStack stack : rockets) {
            final double screen = Math.max(0, overworld(parts, stack).deltaV() - climb);
            final double counted = carried(helper, stack).availableToLand().orElse(-1);
            helper.assertTrue(Math.abs(screen - counted) < AGREE, "past the climb the screen says " + screen
                    + " m/s and the flight computer counts " + counted + " m/s: " + stack);
        }
        helper.succeed();
    }

    /**
     * Every part of each diameter stacked in one rocket, and a rocket of two stages with four boosters, laid block by
     * block as the crane lays them: each part forms as its last block goes down, every booster's too.
     */
    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 400)
    public static void combinations_layWholeEveryPartFormed(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final RocketStack two = RocketStack.of(sized("sea_level_engine", 3, 1), sized("tank", 3, LAYERS * 2),
                sized("separation_ring", 3, 1), sized("vacuum_engine", 3, 1), sized("tank", 3, LAYERS),
                sized("capsule", 3, 1));
        final List<RocketStack> rockets = List.of(everyPart(3), everyPart(5), boosted(two,
                List.of(sized("sea_level_engine", 3, 1), sized("tank", 3, LAYERS)), 4));
        final List<PlannedBuild> laid = new ArrayList<>();
        for (int k = 0; k < rockets.size(); k++) {
            final BlockPos foot = helper.absolutePos(FIRST_FOOT.east(k * APART));
            final PlannedBuild build = new PlannedBuild(RocketPlans.of(level, rockets.get(k)), foot);
            build.start(level, helper.absolutePos(COMPUTER), () -> { });
            for (final PlanSpot spot : build.plan().spots()) {
                level.setBlockAndUpdate(build.where(spot), BuiltInRegistries.BLOCK.get(spot.block())
                        .defaultBlockState());
            }
            laid.add(build);
        }
        helper.startSequence()
                .thenWaitUntil(() -> {
                    for (int k = 0; k < laid.size(); k++) {
                        final PlannedBuild build = laid.get(k);
                        helper.assertTrue(build.complete() && build.formedParts().size() == build.plan().parts(),
                                "laid whole, every part of it formed: " + build.formedParts() + " of "
                                        + build.plan().parts() + ", " + rockets.get(k));
                    }
                })
                .thenExecute(() -> laid.forEach(build -> {
                    build.stop(false);
                    // Taken away again, so nothing of it stands beyond the arena for another test.
                    for (final PlanSpot spot : build.plan().spots()) {
                        level.setBlockAndUpdate(build.where(spot), Blocks.AIR.defaultBlockState());
                    }
                }))
                .thenSucceed();
    }

    /* A stack of two stages of every part made in {@code diameter}, from the bottom. */
    private static RocketStack everyPart(final int diameter) {
        final List<RocketStack.Section> sections = new ArrayList<>();
        for (final String kind : EVERY_PART) {
            sections.add(new RocketStack.Section(sized(kind, diameter, kind.equals("tank") ? LAYERS : 1), List.of()));
        }
        return new RocketStack(sections);
    }

    /* {@code stack} with its part at {@code place} made in {@code diameter} instead. */
    private static RocketStack swap(final RocketStack stack, final int place, final int diameter) {
        final List<RocketStack.Section> sections = new ArrayList<>(stack.sections());
        final RocketStack.Part was = sections.get(place).part();
        sections.set(place, new RocketStack.Section(sized(EVERY_PART.get(place), diameter, was.layers()), List.of()));
        return new RocketStack(sections);
    }

    /*
     * A rocket of {@code count} stages of {@code diameter}: the first on a sea-level engine, the others on vacuum
     * engines, each with a tank of one layer, a separation ring between each two, the capsule on top.
     */
    private static RocketStack staged(final int diameter, final int count) {
        final List<RocketStack.Part> placed = new ArrayList<>();
        for (int stage = 1; stage <= count; stage++) {
            placed.add(sized(stage == 1 ? "sea_level_engine" : "vacuum_engine", diameter, 1));
            placed.add(sized("tank", diameter, STAGE_LAYERS));
            if (stage < count) {
                placed.add(sized("separation_ring", diameter, 1));
            }
        }
        placed.add(sized("capsule", diameter, 1));
        return RocketStack.of(placed.toArray(RocketStack.Part[]::new));
    }

    /* {@code core} with {@code count} boosters of {@code booster} beside its first engine. */
    private static RocketStack boosted(final RocketStack core, final List<RocketStack.Part> booster, final int count) {
        final List<RocketStack.Section> sections = new ArrayList<>(core.sections());
        final List<List<RocketStack.Part>> hung = new ArrayList<>();
        for (int k = 0; k < count; k++) {
            hung.add(booster);
        }
        sections.set(0, new RocketStack.Section(sections.get(0).part(), hung));
        return new RocketStack(sections);
    }

    private static RocketStack.Part sized(final String kind, final int diameter, final int layers) {
        return new RocketStack.Part(SkeletonRocketParts.id(kind + "_" + diameter), layers);
    }

    private static RocketStack.Part lander(final String stage) {
        return new RocketStack.Part(SkeletonRocketParts.id("lunar_lander_" + stage), 1);
    }

    /*
     * The flight computer standing in the arena given {@code stack}'s plan, every part formed, every tank filled as the
     * pad's service port fills it, its boosters full.
     */
    private static FlightComputerBlockEntity carried(final GameTestHelper helper, final RocketStack stack) {
        final ServerLevel level = helper.getLevel();
        helper.setBlock(COMPUTER, RocketKitBlocks.FLIGHT_COMPUTER.get());
        final FlightComputerBlockEntity computer = (FlightComputerBlockEntity) level.getBlockEntity(
                helper.absolutePos(COMPUTER));
        helper.assertTrue(computer != null, "the flight computer stands in the arena");
        final List<Integer> formed = new ArrayList<>();
        for (int piece = 0; piece < RocketPieces.count(stack); piece++) {
            formed.add(piece);
        }
        computer.carry(new RocketDesign("COMBINED", stack), BlockPos.ZERO, formed);
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

    /* The report of {@code stack} leaving the Overworld, in no hangar. */
    private static DesignReport overworld(final Map<ResourceLocation, RocketPart> parts, final RocketStack stack) {
        return DesignReport.of(parts, stack, 1, 1, Optional.of(SolarBody.OVERWORLD), Optional.empty());
    }

    /* What the climb to the low orbit asks, among {@code reaches}: the one going to no world. */
    private static double need(final List<DesignDestinations.Reach> reaches) {
        return reaches.stream().filter(reach -> reach.world().isEmpty()).findFirst().orElseThrow().need();
    }

    private static boolean says(final DesignReport report, final TextKey key) {
        return report.checks().stream().map(DesignReport.Check::text).anyMatch(text -> is(text, key));
    }

    private static boolean is(final Text text, final TextKey key) {
        return text instanceof Text.Translated translated && translated.key().equals(key);
    }
}
