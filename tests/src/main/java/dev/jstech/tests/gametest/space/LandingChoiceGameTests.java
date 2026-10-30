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
import dev.jstech.space.exploration.ReliefCoverage;
import dev.jstech.space.exploration.WorldCoverage;
import dev.jstech.space.flight.FlightComputerBlockEntity;
import dev.jstech.space.flight.FlightPage;
import dev.jstech.space.flight.LandingMaps;
import dev.jstech.space.flight.PlannedLanding;
import dev.jstech.space.hangar.RocketDesign;
import dev.jstech.space.hangar.RocketPlans;
import dev.jstech.space.network.FarMapPayload;
import dev.jstech.space.network.LandingSight;
import dev.jstech.space.rocket.RocketKitBlocks;
import dev.jstech.space.rocket.RocketStack;
import dev.jstech.space.rocket.SkeletonRocketParts;
import dev.jstech.space.universe.SolarSystemWorlds;
import dev.jstech.tests.JsTests;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biomes;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Choosing where to land: the landing map shows only the ground the team knows, from its instruments, the passes its
 * rockets photographed from orbit and where its crews stood, and marks the known ground too steep to land on; the
 * world's coverage counts the passes. On the LAND page, SET fixes the landing where the spot is marked, a spot marked
 * elsewhere or cleared lets it go, and HAZARD shows or hides the hazards; an instrument ring's guidance platform, once
 * formed, guides the rocket down in a smaller ellipse than the computer alone.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class LandingChoiceGameTests {

    private static final String ARENA = "empty";
    private static final String BENCH = "bench";
    private static final String BATCH = "jsspace_landing_choice";
    private static final BlockPos COMPUTER = new BlockPos(2, 2, 2);
    private static final BlockPos FOOT = new BlockPos(8, 1, 8);
    private static final BlockPos OWNER = new BlockPos(1, 1, 1);
    /* A small map: eight pixels each way, sixteen blocks a pixel, round the world's middle, of a world so wide. */
    private static final int SIZE = 8;
    private static final int BLOCKS = 16;
    private static final double WIDTH = 10_000_000;
    private static final int GROUND = 64;
    /* A cliff down the map's middle column, higher than steep ground over a pixel. */
    private static final int CLIFF = 40;
    private static final int GUIDED = 60;

    private LandingChoiceGameTests() {
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void map_showsOnlyTheGroundTheTeamKnowsAndItsHazards(final GameTestHelper helper) {
        final FarMapPayload map = cliffMap();
        final int all = SIZE * SIZE;
        helper.assertTrue(LandingMaps.known(map, WIDTH, ReliefCoverage.NONE, 0).isEmpty(),
                "a world the team never saw lies all under the grey");
        final ReliefCoverage pass = ReliefCoverage.NONE.photographed(0);
        helper.assertTrue(LandingMaps.known(map, WIDTH, pass, 0).cardinality() == all,
                "a pass photographed over the world's middle shows the ground round it");
        helper.assertTrue(LandingMaps.known(map, WIDTH, ReliefCoverage.NONE, 1).cardinality() == all,
                "the relief known whole shows all of it");
        final ReliefCoverage stood = ReliefCoverage.NONE.visitedAt(0, 0);
        helper.assertTrue(LandingMaps.known(map, WIDTH, stood, 0).cardinality() == all,
                "the ground round where a crew stood is known");
        final BitSet unknownSteep = LandingMaps.steep(map, LandingMaps.known(map, WIDTH, ReliefCoverage.NONE, 0));
        final BitSet steep = LandingMaps.steep(map, LandingMaps.known(map, WIDTH, pass, 0));
        helper.assertTrue(unknownSteep.isEmpty() && steep.cardinality() == 2 * (SIZE - 1)
                        && steep.get(SIZE / 2 - 1) && steep.get(SIZE / 2) && !steep.get(0),
                "only the known ground's cliff is a hazard, its edges on every row reckoned: " + steep);
        final LandingSight sight = LandingMaps.payload(SolarSystemWorlds.MARS, WIDTH, map, pass, 0).sight();
        final BitSet cover = BitSet.valueOf(sight.cover());
        helper.assertTrue(sight.share() > 0 && !cover.isEmpty()
                && cover.cardinality() < LandingSight.COVER_WIDE * LandingSight.COVER_TALL,
                "the world's coverage shows the pass's strip: " + sight.share());
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void coverage_growsAPassAtATimeForATeam(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final String team = "jstests:coverage_" + UUID.randomUUID();
        WorldCoverage.photographAt(level.getServer(), team, SolarSystemWorlds.MOON, 0);
        WorldCoverage.photographAt(level.getServer(), team, SolarSystemWorlds.MOON, ReliefCoverage.PASS_TICKS - 1);
        helper.assertTrue(WorldCoverage.of(level.getServer(), team, SolarSystemWorlds.MOON).strips().size() == 1,
                "a pass is photographed once");
        WorldCoverage.photographAt(level.getServer(), team, SolarSystemWorlds.MOON, ReliefCoverage.PASS_TICKS);
        WorldCoverage.visit(level.getServer(), team, SolarSystemWorlds.MOON, 300, 300);
        final ReliefCoverage seen = WorldCoverage.of(level.getServer(), team, SolarSystemWorlds.MOON);
        helper.assertTrue(seen.strips().size() == 2 && seen.strips().get(1) == ReliefCoverage.PASS_SHIFT
                && !seen.visited().isEmpty(), "the next pass the world has turned under the orbit: " + seen);
        helper.assertTrue(WorldCoverage.of(level.getServer(), team, SolarSystemWorlds.MARS).strips().isEmpty(),
                "each world's coverage is its own");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void land_setFixesTheMarkAndHazardToggles(final GameTestHelper helper) {
        helper.setBlock(COMPUTER, RocketKitBlocks.FLIGHT_COMPUTER.get());
        final FlightComputerBlockEntity computer = helper.getBlockEntity(COMPUTER);
        computer.press(FlightComputerBlockEntity.TOP + FlightPage.KEYS.indexOf(FlightPage.LANDING));
        computer.press(FlightComputerBlockEntity.RIGHT + 4);
        helper.assertTrue(computer.landing().isEmpty(), "with no spot marked there is nothing to set");
        computer.markAt(1200, -800);
        computer.press(FlightComputerBlockEntity.RIGHT + 4);
        helper.assertTrue(computer.landing().equals(Optional.of(new PlannedLanding(computer.target(), 1200, -800))),
                "SET fixes the landing where the spot is marked: " + computer.landing());
        computer.markAt(1200, -800);
        helper.assertTrue(computer.landing().isPresent(), "marking the same spot keeps it");
        computer.markAt(0, 0);
        helper.assertTrue(computer.landing().isEmpty(), "a spot marked elsewhere lets it go");
        computer.press(FlightComputerBlockEntity.RIGHT + 4);
        computer.press(FlightComputerBlockEntity.LEFT + 3);
        helper.assertTrue(computer.landing().isEmpty() && computer.mark() == null, "CLEAR lets both go");
        helper.assertTrue(computer.hazards(), "the hazards are shown at first");
        computer.press(FlightComputerBlockEntity.RIGHT + 1);
        helper.assertTrue(!computer.hazards(), "HAZARD hides them");
        helper.assertTrue(FlightComputerBlockEntity.ellipseLength(FlightComputerBlockEntity.PLATFORM_GUIDANCE)
                        < FlightComputerBlockEntity.ellipseLength(FlightComputerBlockEntity.COMPUTER_GUIDANCE)
                && FlightComputerBlockEntity.ellipseWidth(FlightComputerBlockEntity.PLATFORM_GUIDANCE)
                        < FlightComputerBlockEntity.ellipseWidth(FlightComputerBlockEntity.COMPUTER_GUIDANCE),
                "a guidance platform lands in a smaller ellipse than the computer alone");
        helper.succeed();
    }

    @GameTest(template = BENCH, batch = BATCH, timeoutTicks = 200)
    public static void guidance_isTheRingsPlatformOnceItForms(final GameTestHelper helper) {
        final FlightComputerBlockEntity ringed = build(helper, RocketStack.of(part("sea_level_engine_3"),
                part("tank_3"), part("instrument_ring_3"), part("capsule_3")));
        helper.assertTrue(ringed.guidance() == FlightComputerBlockEntity.COMPUTER_GUIDANCE,
                "before it reckons, the computer's own");
        helper.runAfterDelay(GUIDED, () -> {
            helper.assertTrue(ringed.guidance() == FlightComputerBlockEntity.PLATFORM_GUIDANCE,
                    "a formed instrument ring's guidance platform guides it");
            helper.succeed();
        });
    }

    /* A map of flat ground at the world's middle but for a cliff down its middle column. */
    private static FarMapPayload cliffMap() {
        final byte[] pixels = new byte[SIZE * SIZE];
        final byte[] heights = new byte[SIZE * SIZE];
        for (int row = 0; row < SIZE; row++) {
            for (int column = 0; column < SIZE; column++) {
                heights[row * SIZE + column] = FarMapPayload.height(column == SIZE / 2 ? GROUND + CLIFF : GROUND);
            }
        }
        return new FarMapPayload(FlightComputerBlockEntity.NEAR, 0, 0, BLOCKS, SIZE, GROUND - 1,
                List.<ResourceLocation>of(Biomes.PLAINS.location()), pixels, heights);
    }

    /* Builds {@code stack} at the arena's foot, every part formed, and hands its plan to its flight computer. */
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
        helper.assertTrue(build.complete(), "the rocket is built whole");
        build.stop(false);
        final FlightComputerBlockEntity computer = (FlightComputerBlockEntity) level.getBlockEntity(at);
        helper.assertTrue(computer != null, "its capsule holds its flight computer");
        final List<Integer> formed = new ArrayList<>();
        for (int section = 0; section < stack.sections().size(); section++) {
            formed.add(section);
        }
        computer.carry(new RocketDesign("GUIDED", stack), foot.subtract(at), formed);
        return computer;
    }

    private static RocketStack.Part part(final String path) {
        return new RocketStack.Part(SkeletonRocketParts.id(path), 1);
    }
}
