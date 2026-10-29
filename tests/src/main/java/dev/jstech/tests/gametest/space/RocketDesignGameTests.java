/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import com.mojang.serialization.JsonOps;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import dev.jstech.space.api.RocketPart;
import dev.jstech.space.api.RocketParts;
import dev.jstech.space.hangar.AssemblyConsoleBlock;
import dev.jstech.space.hangar.DesignDestinations;
import dev.jstech.space.hangar.DesignEdits;
import dev.jstech.space.hangar.DesignReport;
import dev.jstech.space.hangar.DesignTexts;
import dev.jstech.space.hangar.HangarBay;
import dev.jstech.space.hangar.HangarBlocks;
import dev.jstech.space.hangar.HangarTerminalBlockEntity;
import dev.jstech.space.hangar.RocketDesign;
import dev.jstech.space.rocket.RocketStack;
import dev.jstech.space.rocket.RocketStages;
import dev.jstech.space.rocket.SkeletonRocketParts;
import dev.jstech.space.rocket.StageMath;
import dev.jstech.space.universe.SolarBody;
import dev.jstech.tests.JsTests;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Rocket Design: where a rocket can go from the Overworld and from the Moon and what each destination asks; each
 * stage's delta-v and the thrust against weight as a rocket leaves the ground; what a design lacks, a stage with
 * propellant and no engine, a rocket taller than its bay; the edits of the screen, a part picked up with what is on its
 * far side from the root, set at a node, turned, stretched, boosters hung round it; and the hangar's terminal keeping
 * the design, saving it as a plan, and sending it to the bay only once it is ready, its Assembly Console finding it.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class RocketDesignGameTests {

    private static final String ARENA = "empty";
    private static final String HANGAR = "tall";
    private static final String BATCH = "jsspace_rocket_design";
    private static final double CLOSE = 1e-6;
    /* The hangar's bay, from (2, 2, 2) to (8, 13, 8), its hoist at 12: ten blocks clear under the crane. */
    private static final int LOW = 2;
    private static final int HIGH_XZ = 8;
    private static final int TOP = 13;
    private static final BlockPos TERMINAL = new BlockPos(5, 3, 9);
    private static final BlockPos DOOR = new BlockPos(1, 2, 5);
    private static final BlockPos HOIST = new BlockPos(5, 12, 5);
    private static final BlockPos CONSOLE = new BlockPos(3, 2, 3);
    private static final ResourceLocation SEA_ENGINE = SkeletonRocketParts.id("sea_level_engine_3");
    private static final ResourceLocation VACUUM_ENGINE = SkeletonRocketParts.id("vacuum_engine_3");
    private static final ResourceLocation TANK = SkeletonRocketParts.id("tank_3");
    private static final ResourceLocation RING = SkeletonRocketParts.id("separation_ring_3");
    private static final ResourceLocation CAPSULE = SkeletonRocketParts.id("capsule_3");

    private RocketDesignGameTests() {
    }

    @GameTest(template = ARENA)
    public static void destinations_sumTheLegsOfEachTrip(final GameTestHelper helper) {
        final List<DesignDestinations.Reach> overworld = DesignDestinations.from(SolarBody.OVERWORLD);
        helper.assertTrue(overworld.size() == 3 && overworld.get(0).world().isEmpty()
                && Math.abs(overworld.get(0).need() - 9_400) < CLOSE
                && Math.abs(need(overworld, SolarBody.MOON) - 15_200) < CLOSE
                && Math.abs(need(overworld, SolarBody.MARS) - 13_000) < CLOSE,
                "from the Overworld: a low orbit 9,400 m/s, the Moon 15,200, Mars 13,000: " + overworld);
        final List<DesignDestinations.Reach> moon = DesignDestinations.from(SolarBody.MOON);
        helper.assertTrue(Math.abs(moon.get(0).need() - 1_700) < CLOSE
                && Math.abs(need(moon, SolarBody.OVERWORLD) - 5_800) < CLOSE,
                "from the Moon: its low orbit 1,700, home 5,800, braking in the Earth's air: " + moon);
        helper.assertTrue(Math.abs(overworld.get(1).shortBy(10_000) - 5_200) < CLOSE
                && overworld.get(0).shortBy(10_000) == 0, "short by what it lacks, nothing where it reaches");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void report_givesEachStagesDeltaVAndTheLiftOff(final GameTestHelper helper) {
        final Map<ResourceLocation, RocketPart> parts = RocketParts.all(helper.getLevel());
        final RocketStack stack = two();
        final DesignReport report = DesignReport.of(parts, stack, 1, 1, Optional.of(SolarBody.OVERWORLD),
                Optional.empty());
        final List<StageMath.Burn> burns = RocketStages.burns(parts, stack).orElseThrow();
        helper.assertTrue(report.stages().size() == 2 && report.stages().get(0).engine().orElseThrow()
                .equals(SEA_ENGINE) && report.stages().get(1).engine().orElseThrow().equals(VACUUM_ENGINE),
                "two stages, each with its engine: " + report.stages());
        final double first = StageMath.deltaV(burns.get(0), 1);
        final double second = StageMath.deltaV(burns.get(1), 0);
        helper.assertTrue(Math.abs(report.stages().get(0).deltaV() - first) < CLOSE
                && Math.abs(report.stages().get(1).deltaV() - second) < CLOSE
                && Math.abs(report.deltaV() - first - second) < CLOSE,
                "the first burn in the air it leaves, the second in a vacuum, summed: " + report.deltaV());
        helper.assertTrue(Math.abs(report.liftOff() - StageMath.thrustToWeight(burns.get(0), 1, 1)) < CLOSE
                && report.liftOff() > 1, "it leaves the Overworld: " + report.liftOff());
        helper.assertTrue(report.height() == RocketStages.height(parts, stack) && report.width() == 3
                && report.seats() == 1, "its height, its width, its seat");
        helper.assertTrue(report.ready() && says(report, DesignTexts.SEAT_ONE) && says(report, DesignTexts.SHIELD)
                && says(report, DesignTexts.TANKS), "ready, its crew, its heat shield and its tanks said: "
                + report.checks());
        final DesignReport moon = DesignReport.of(parts, stack, 0.165, 0, Optional.of(SolarBody.MOON),
                Optional.empty());
        helper.assertTrue(moon.liftOff() > report.liftOff() * 5, "it leaps off the Moon: " + moon.liftOff());
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void report_saysWhatADesignLacks(final GameTestHelper helper) {
        final Map<ResourceLocation, RocketPart> parts = RocketParts.all(helper.getLevel());
        final RocketStack noEngine = RocketStack.of(part(SEA_ENGINE), new RocketStack.Part(TANK, 3), part(RING),
                new RocketStack.Part(TANK, 2), part(CAPSULE));
        final DesignReport report = DesignReport.of(parts, noEngine, 1, 1, Optional.of(SolarBody.OVERWORLD),
                Optional.empty());
        helper.assertTrue(!report.ready() && says(report, DesignTexts.NO_ENGINE)
                && report.checks().get(0).severity() == DesignReport.Severity.FAULT,
                "its second stage has propellant and no engine: not ready, said first: " + report.checks());
        final DesignReport bare = DesignReport.of(parts, RocketStack.of(part(SEA_ENGINE), part(CAPSULE)), 1, 1,
                Optional.of(SolarBody.OVERWORLD), Optional.empty());
        helper.assertTrue(!bare.ready() && says(bare, DesignTexts.NO_PROPELLANT), "an engine with nothing to burn");
        final DesignReport empty = DesignReport.of(parts, new RocketStack(List.of()), 1, 1, Optional.empty(),
                Optional.empty());
        helper.assertTrue(!empty.ready() && says(empty, DesignTexts.NO_PARTS) && empty.destinations().isEmpty(),
                "no parts yet, and no routes from a world that is not the Solar System's");
        final HangarBay bay = new HangarBay(new BlockPos(0, 0, 0), new BlockPos(6, 11, 6), Direction.Axis.Z,
                new BlockPos(3, 11, 3), new BlockPos(3, 10, 3));
        final DesignReport tall = DesignReport.of(parts, two(), 1, 1, Optional.of(SolarBody.OVERWORLD),
                Optional.of(bay));
        helper.assertTrue(DesignReport.clearHeight(bay) == 10 && !tall.ready() && says(tall, DesignTexts.TOO_TALL),
                "taller than the ten blocks under its crane: " + tall.height());
        final DesignReport crewless = DesignReport.of(parts, RocketStack.of(part(SEA_ENGINE),
                new RocketStack.Part(TANK, 2)), 1, 1, Optional.of(SolarBody.OVERWORLD), Optional.empty());
        helper.assertTrue(crewless.ready() && says(crewless, DesignTexts.NO_SEATS),
                "no seats is worth knowing, not a fault: " + crewless.checks());
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void edits_pickTheFarSideOfTheRootAndSetPartsAtNodes(final GameTestHelper helper) {
        final RocketStack stack = RocketStack.of(part(SEA_ENGINE), part(TANK), part(RING), part(CAPSULE));
        final DesignEdits.Picked above = DesignEdits.pick(stack, 2, 0);
        helper.assertTrue(above.left().sections().size() == 2 && above.taken().size() == 2
                && above.taken().get(0).part().id().equals(RING) && above.root() == 0,
                "over the root, a part takes those above it");
        final DesignEdits.Picked under = DesignEdits.pick(stack, 1, 3);
        helper.assertTrue(under.taken().size() == 2 && under.taken().get(1).part().id().equals(TANK)
                && under.left().sections().get(0).part().id().equals(RING) && under.root() == 1,
                "under the root, a part takes those under it, the root moving down with what is left");
        final DesignEdits.Picked whole = DesignEdits.pick(stack, 0, 0);
        helper.assertTrue(whole.left().sections().isEmpty() && whole.taken().size() == 4, "the root takes all");
        final RocketStack back = DesignEdits.insert(above.left(), 2, above.taken());
        helper.assertTrue(back.equals(stack), "set back at the top node, the rocket is as it was");
        final RocketStack under2 = DesignEdits.insert(stack, 0, part(TANK));
        helper.assertTrue(under2.sections().size() == 5 && under2.sections().get(0).part().id().equals(TANK)
                && DesignEdits.nodes(stack) == 5, "a part set at the bottom node stands under the rest");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void edits_turnStretchAndHangBoosters(final GameTestHelper helper) {
        RocketStack stack = RocketStack.of(part(SEA_ENGINE), part(TANK), part(CAPSULE));
        for (int i = 0; i < 5; i++) {
            stack = DesignEdits.turn(stack, 2);
        }
        helper.assertTrue(stack.sections().get(2).part().turns() == 1, "five quarter turns are one");
        helper.assertTrue(DesignEdits.stretch(stack, 1, 6).sections().get(1).part().layers() == 6
                && DesignEdits.stretch(stack, 1, 0).sections().get(1).part().layers() == 1
                && DesignEdits.stretch(stack, 1, 1_000).sections().get(1).part().layers() == DesignEdits.MOST_LAYERS,
                "a tank built 6 tall, never under one layer nor over the most");
        final RocketStack hung = DesignEdits.hang(stack, 1, part(TANK), 3);
        helper.assertTrue(hung.sections().get(1).boosters().size() == 3, "the mirror hangs three round it");
        final RocketStack taller = DesignEdits.hang(hung, 1, part(SEA_ENGINE), 1);
        helper.assertTrue(taller.sections().get(1).boosters().stream().allMatch(booster -> booster.size() == 2
                && booster.get(1).id().equals(SEA_ENGINE)), "a segment stacks on each of them");
        helper.assertTrue(DesignEdits.unhang(taller, 1).sections().get(1).boosters().isEmpty(), "and they come off");
        final RocketStack.Part turned = new RocketStack.Part(CAPSULE, 1, 3);
        helper.assertTrue(RocketStack.Part.CODEC.parse(JsonOps.INSTANCE, RocketStack.Part.CODEC.encodeStart(
                JsonOps.INSTANCE, turned).getOrThrow()).getOrThrow().equals(turned),
                "a part keeps its turns when written down");
        helper.succeed();
    }

    @GameTest(template = HANGAR, batch = BATCH, timeoutTicks = 200)
    public static void terminal_keepsTheDesignSavesItAndSendsItWhenReady(final GameTestHelper helper) {
        build(helper);
        final HangarTerminalBlockEntity terminal = helper.getBlockEntity(TERMINAL);
        terminal.look();
        helper.assertTrue(terminal.bay().isPresent(), "the hangar is formed");
        final RocketDesign lacking = new RocketDesign("LACKING", RocketStack.of(part(TANK), part(CAPSULE)));
        terminal.design(lacking);
        helper.assertTrue(terminal.design().equals(lacking), "it keeps the design on its screen");
        helper.assertTrue(!terminal.confirm() && terminal.plan().isEmpty(), "one that is not ready is not sent");
        final RocketDesign unknown = new RocketDesign("UNKNOWN", RocketStack.of(part(SkeletonRocketParts.id(
                "no_such_part"))));
        terminal.design(unknown);
        helper.assertTrue(terminal.design().equals(lacking), "nor kept, one of a part the world does not know");
        final RocketDesign ready = new RocketDesign("ORBITAL I", RocketStack.of(part(SEA_ENGINE),
                new RocketStack.Part(TANK, 3), part(CAPSULE)));
        terminal.design(ready);
        terminal.save();
        terminal.design(ready.with(RocketStack.of(part(SEA_ENGINE), new RocketStack.Part(TANK, 2), part(CAPSULE))));
        terminal.save();
        helper.assertTrue(terminal.plans().size() == 1 && terminal.plans().get(0).stack().sections().get(1).part()
                .layers() == 2, "saved again under its name, its plan is the latest");
        terminal.design(ready);
        helper.assertTrue(terminal.confirm() && terminal.plan().orElseThrow().equals(ready)
                && terminal.plans().get(0).equals(ready), "a ready design is sent to the bay, and kept as a plan");
        terminal.delete(0);
        helper.assertTrue(terminal.plans().isEmpty() && terminal.plan().isPresent(),
                "a plan deleted, the one in the bay stays");
        helper.setBlock(CONSOLE, HangarBlocks.ASSEMBLY_CONSOLE.get());
        helper.assertTrue(AssemblyConsoleBlock.hangarOf(helper.getLevel(), helper.absolutePos(CONSOLE))
                .filter(found -> found == terminal).isPresent(), "a console in its bay finds its terminal");
        helper.assertTrue(AssemblyConsoleBlock.hangarOf(helper.getLevel(), helper.absolutePos(new BlockPos(0, 2,
                0))).isEmpty(), "and one outside finds none");
        helper.succeed();
    }

    /* A rocket of two stages: a sea-level engine and its tank, the ring, a vacuum engine, its tank, the capsule. */
    private static RocketStack two() {
        return RocketStack.of(part(SEA_ENGINE), new RocketStack.Part(TANK, 6), part(RING), part(VACUUM_ENGINE),
                new RocketStack.Part(TANK, 3), part(CAPSULE));
    }

    private static RocketStack.Part part(final ResourceLocation id) {
        return new RocketStack.Part(id, 1);
    }

    private static double need(final List<DesignDestinations.Reach> reaches, final SolarBody world) {
        return reaches.stream().filter(reach -> reach.world().filter(body -> body == world).isPresent()).findFirst()
                .orElseThrow().need();
    }

    private static boolean says(final DesignReport report, final TextKey key) {
        return report.checks().stream().map(DesignReport.Check::text).anyMatch(text -> text
                instanceof Text.Translated translated && translated.key().equals(key));
    }

    /* The hangar of stone, its terminal in the south wall looking north, its crane, its door: formed when looked. */
    private static void build(final GameTestHelper helper) {
        for (int x = LOW - 1; x <= HIGH_XZ + 1; x++) {
            for (int z = LOW - 1; z <= HIGH_XZ + 1; z++) {
                for (int y = LOW - 1; y <= TOP + 1; y++) {
                    final boolean shell = x < LOW || x > HIGH_XZ || z < LOW || z > HIGH_XZ || y < LOW || y > TOP;
                    helper.setBlock(new BlockPos(x, y, z), shell ? Blocks.STONE : Blocks.AIR);
                }
            }
        }
        helper.setBlock(TERMINAL, HangarBlocks.TERMINAL.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        for (int x = LOW; x <= HIGH_XZ; x++) {
            helper.setBlock(new BlockPos(x, TOP, LOW), HangarBlocks.CRANE_RAIL.get());
            helper.setBlock(new BlockPos(x, TOP, HIGH_XZ), HangarBlocks.CRANE_RAIL.get());
        }
        for (int z = LOW + 1; z < HIGH_XZ; z++) {
            helper.setBlock(new BlockPos(5, TOP, z), HangarBlocks.CRANE_BRIDGE.get());
        }
        helper.setBlock(HOIST, HangarBlocks.CRANE_HOIST.get());
        helper.setBlock(DOOR, HangarBlocks.BAY_DOOR.get());
    }
}
