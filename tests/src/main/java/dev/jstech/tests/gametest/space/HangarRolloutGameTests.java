/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import dev.jstech.core.multiblock.Orientation;
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.multiblock.plan.PlannedBuild;
import dev.jstech.core.text.Text;
import dev.jstech.space.crawler.CrawlerEngineBlockEntity;
import dev.jstech.space.crawler.CrawlerLayout;
import dev.jstech.space.hangar.HangarBay;
import dev.jstech.space.hangar.HangarPieceBlock;
import dev.jstech.space.hangar.HangarTerminalBlockEntity;
import dev.jstech.space.hangar.RocketPlans;
import dev.jstech.space.launcher.LauncherBlocks;
import dev.jstech.space.launcher.LauncherLayout;
import dev.jstech.space.rocket.RocketStack;
import dev.jstech.space.rocket.SkeletonRocketParts;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.space.TestHangars;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The tallest and widest rocket a Rocket Hangar builds, 96 blocks tall with a booster each side, rolled out of the
 * hangar on its mobile launcher by the crawler-transporter: through the stepped door, the lower door for the crawler
 * and the launcher's deck, the slot over it for the rocket and the tower, the door opening for it and shutting behind
 * it, the whole of it set down outside on the crawlerway's far end.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class HangarRolloutGameTests {

    private static final String ARENA = "rocket_hangar";
    /* The hangar's terminal in the arena's middle, the hangar standing with its door to the north. */
    private static final BlockPos TERMINAL = new BlockPos(40, 2, 42);
    /* The tallest rocket's tank, in layers, and its boosters'. */
    private static final int TALLEST_TANK = 89;
    private static final int BOOSTER_TANK = 20;
    /* How far into the deck's length, from the rocket's axis toward the tower, the crawler stands under its middle. */
    private static final int UNDER_DECK = 2;
    /* How high the crawler's engine stands over the bay's floor, its tracks on the floor's gravel. */
    private static final int ENGINE_UP = 2;
    /*
     * The crawlerway's lanes outside, as the floor's run, each side of the middle line; and where they end, the
     * crawler's cabs, past its tracks, still in the arena when it stops.
     */
    private static final int LANE_IN = 3;
    private static final int LANE_OUT = 6;
    private static final int LANE_END_Z = 3;
    /* The door's place on the bay's middle line, as the hangar's pattern draws it. */
    private static final int[] DOOR = {0, 0, -19};
    private static final int ROLLOUT_TIME = 4000;

    private HangarRolloutGameTests() {
    }

    @GameTest(template = ARENA, batch = "jsspace_hangar_rollout", timeoutTicks = ROLLOUT_TIME)
    public static void crawler_rollsTheTallestRocketOutThroughTheDoor(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final Orientation turn = Orientation.NORTH;
        final BlockPos at = TestHangars.build(level, helper.absolutePos(TERMINAL), turn);
        final BlockPos door = TestHangars.at(at, turn, DOOR[0], DOOR[1], DOOR[2]);
        final PlannedBuild[] laid = new PlannedBuild[1];
        final BlockPos[] engine = new BlockPos[1];
        final int[] steps = new int[1];
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(terminal(level, at).formed(), "the hangar forms"))
                .thenExecute(() -> {
                    final HangarBay bay = terminal(level, at).bay().orElseThrow();
                    final Direction tower = RocketPlans.launcherSide(level, tallest(), bay).orElseThrow();
                    helper.assertTrue(tower == Direction.SOUTH, "the tower stands toward the far wall");
                    // The rocket and its launcher laid whole by hand, every part forming with its last block.
                    final PlannedBuild plan = new PlannedBuild(RocketPlans.of(level, tallest(), tower),
                            bay.pad().above(LauncherLayout.RAISE));
                    plan.start(level, at, () -> { });
                    for (final PlanSpot spot : plan.plan().spots()) {
                        level.setBlockAndUpdate(plan.where(spot), BuiltInRegistries.BLOCK.get(spot.block())
                                .defaultBlockState());
                    }
                    helper.assertTrue(plan.complete(), "the tallest rocket and its launcher stand whole");
                    laid[0] = plan;
                    // The crawler under the deck's middle; its crawlerway out of the door to the arena's edge.
                    engine[0] = bay.pad().relative(tower, UNDER_DECK).above(ENGINE_UP);
                    for (final CrawlerLayout.Spot spot : CrawlerLayout.spots(engine[0], Direction.SOUTH)) {
                        level.setBlockAndUpdate(spot.at(), spot.kind().block().defaultBlockState());
                    }
                    final BlockPos edge = helper.absolutePos(new BlockPos(0, 1, LANE_END_Z));
                    for (int z = edge.getZ(); z < door.getZ(); z++) {
                        for (int off = LANE_IN; off <= LANE_OUT; off++) {
                            for (final int side : new int[] {-1, 1}) {
                                final BlockPos lane = new BlockPos(door.getX() + side * off, edge.getY(), z);
                                level.setBlockAndUpdate(lane.below(), Blocks.STONE.defaultBlockState());
                                level.setBlockAndUpdate(lane, Blocks.GRAVEL.defaultBlockState());
                            }
                        }
                    }
                    steps[0] = engine[0].getZ() - CrawlerLayout.HALF_LENGTH - edge.getZ();
                })
                .thenWaitUntil(() -> helper.assertTrue(crawler(level, engine[0]).along().isPresent(),
                        "the crawler forms under the launcher"))
                .thenExecute(() -> {
                    final Optional<Text> why = crawler(level, engine[0]).start();
                    helper.assertTrue(why.isEmpty(), "with the launcher over it, it sets off: " + why);
                })
                .thenWaitUntil(() -> helper.assertTrue(open(level, door), "the bay door opens for it"))
                .thenWaitUntil(() -> helper.assertTrue(level.getBlockState(top(laid[0]).relative(Direction.NORTH,
                        steps[0])).getBlock() != Blocks.AIR && level.getBlockState(deck(laid[0]).relative(
                        Direction.NORTH, steps[0])).is(LauncherBlocks.LAUNCHER_DECK.get()), "the tallest rocket is set"
                        + " down outside on its launcher, its top block with it"))
                .thenWaitUntil(() -> helper.assertTrue(!open(level, door), "and the door shuts behind it"))
                .thenExecute(() -> helper.assertTrue(level.getBlockState(top(laid[0])).isAir(),
                        "nothing of it is left in the bay"))
                .thenSucceed();
    }

    /* A booster each side of a five-block engine, a tank on it and a capsule: 96 blocks tall, 15 across. */
    private static RocketStack tallest() {
        final List<RocketStack.Part> booster = List.of(part("sea_level_engine_5", 1), part("tank_5", BOOSTER_TANK));
        return new RocketStack(List.of(new RocketStack.Section(part("sea_level_engine_5", 1), List.of(booster,
                booster)), new RocketStack.Section(part("tank_5", TALLEST_TANK), List.of()),
                new RocketStack.Section(part("capsule_5", 1), List.of())));
    }

    private static RocketStack.Part part(final String path, final int layers) {
        return new RocketStack.Part(SkeletonRocketParts.id(path), layers);
    }

    /* The rocket's top block, as laid in the bay. */
    private static BlockPos top(final PlannedBuild plan) {
        return plan.where(plan.plan().spots().stream().max(Comparator.comparingInt(spot -> spot.at().getY()))
                .orElseThrow());
    }

    /* A block of the launcher's deck, as laid in the bay. */
    private static BlockPos deck(final PlannedBuild plan) {
        return plan.where(plan.plan().spots().stream().filter(spot -> spot.block().equals(LauncherBlocks
                .LAUNCHER_DECK.getId())).findFirst().orElseThrow());
    }

    private static boolean open(final ServerLevel level, final BlockPos door) {
        return level.getBlockState(door).getOptionalValue(HangarPieceBlock.OPEN).orElse(false);
    }

    private static HangarTerminalBlockEntity terminal(final ServerLevel level, final BlockPos at) {
        return (HangarTerminalBlockEntity) level.getBlockEntity(at);
    }

    private static CrawlerEngineBlockEntity crawler(final ServerLevel level, final BlockPos at) {
        if (level.getBlockEntity(at) instanceof CrawlerEngineBlockEntity found) {
            return found;
        }
        throw new IllegalStateException("No crawler engine at " + at);
    }
}
