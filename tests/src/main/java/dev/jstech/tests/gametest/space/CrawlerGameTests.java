/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import dev.jstech.core.text.Text;
import dev.jstech.space.crawler.CrawlerEngineBlockEntity;
import dev.jstech.space.crawler.CrawlerLayout;
import dev.jstech.space.crawler.CrawlerPieceBlock;
import dev.jstech.space.crawler.CrawlerTexts;
import dev.jstech.space.hangar.HangarBlocks;
import dev.jstech.space.hangar.HangarPieceBlock;
import dev.jstech.space.launcher.LauncherBlocks;
import dev.jstech.space.launcher.LauncherDeckBlockEntity;
import dev.jstech.tests.JsTests;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The crawler-transporter: built whole round its engine it forms, and falls apart when a piece goes; a cab used, it
 * goes empty to the other end of its crawlerway, the bay doors in its way opening and closing behind it, and sets
 * itself down there; with a launcher centred over it, it lifts it, carries it to the far end, sets it down on the grid
 * there and comes back alone; and it will not set off where the way is blocked.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class CrawlerGameTests {

    private static final String WAY = "crawlerway";
    private static final String BATCH = "jsspace_crawler";
    /* The engine, the crawler's length along x; its lanes of gravel from x 1 to x 40, so it goes 23 blocks east. */
    private static final BlockPos ENGINE = new BlockPos(10, 4, 7);
    private static final int LANE_FROM = 1;
    private static final int LANE_TO = 40;
    private static final int STEPS = 23;
    /* A wall of bay doors across the way, and a stone in the hull's way (it would pass over one between its trucks). */
    private static final int DOORS_X = 21;
    private static final BlockPos STONE = new BlockPos(30, 4, 7);
    /* The test launcher's deck over the crawler, its pedestals each side, and the hold-down arm measuring it. */
    private static final int DECK_FROM_X = 2;
    private static final int DECK_TO_X = 18;
    private static final int DECK_HALF = 6;
    private static final BlockPos ARM = new BlockPos(8, 8, 5);

    private CrawlerGameTests() {
    }

    @GameTest(template = WAY, batch = BATCH, timeoutTicks = 200)
    public static void crawler_formsWhenWholeAndFallsApartWithoutAPiece(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos engine = helper.absolutePos(ENGINE);
        build(helper, engine);
        final BlockPos plate = engine.relative(Direction.EAST, 3);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(engine(level, engine).along().equals(Optional.of(
                        Direction.EAST)), "the crawler round its engine forms along its length"))
                .thenExecute(() -> {
                    for (final CrawlerLayout.Spot spot : CrawlerLayout.spots(engine, Direction.EAST)) {
                        helper.assertTrue(level.getBlockState(spot.at()).getValue(CrawlerPieceBlock.FORMED),
                                "every piece is formed: " + spot);
                    }
                    level.setBlockAndUpdate(plate, Blocks.AIR.defaultBlockState());
                })
                .thenWaitUntil(() -> helper.assertTrue(engine(level, engine).along().isEmpty()
                        && !level.getBlockState(CrawlerLayout.tracks(engine, Direction.EAST).get(0))
                        .getValue(CrawlerPieceBlock.FORMED), "a deck plate gone, the crawler falls apart"))
                .thenExecute(() -> helper.assertTrue(engine(level, engine).start().equals(Optional.of(
                        CrawlerTexts.NOT_WHOLE.text())), "and its cab will not start it"))
                .thenSucceed();
    }

    @GameTest(template = WAY, batch = BATCH, timeoutTicks = 1200)
    public static void crawler_goesEmptyToTheOtherEndThroughTheBayDoors(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos engine = helper.absolutePos(ENGINE);
        build(helper, engine);
        lanes(helper);
        for (int z = 1; z <= 13; z++) {
            for (int y = 2; y <= 5; y++) {
                helper.setBlock(new BlockPos(DOORS_X, y, z), HangarBlocks.BAY_DOOR.get());
            }
        }
        final BlockPos end = engine.relative(Direction.EAST, STEPS);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(engine(level, engine).along().isPresent(), "the crawler forms"))
                .thenExecute(() -> {
                    final Optional<Text> why = engine(level, engine).start();
                    helper.assertTrue(why.isEmpty(), "a cab used, it sets off: " + why);
                })
                .thenWaitUntil(() -> {
                    final BlockState door = level.getBlockState(helper.absolutePos(new BlockPos(DOORS_X, 3, 7)));
                    helper.assertTrue(door.getValue(HangarPieceBlock.OPEN), "the bay doors in its way open");
                })
                .thenWaitUntil(() -> helper.assertTrue(level.getBlockEntity(end) instanceof CrawlerEngineBlockEntity
                        at && at.phase() == CrawlerEngineBlockEntity.Phase.IDLE, "it stands at the far end"))
                .thenExecute(() -> {
                    helper.assertTrue(level.getBlockState(engine).isAir(), "and has left where it was");
                    for (final CrawlerLayout.Spot spot : CrawlerLayout.spots(end, Direction.EAST)) {
                        final BlockState there = level.getBlockState(spot.at());
                        helper.assertTrue(there.is(spot.kind().block()) && there.getValue(CrawlerPieceBlock.FORMED),
                                "set down whole on the grid, formed: " + spot);
                    }
                    final BlockState door = level.getBlockState(helper.absolutePos(new BlockPos(DOORS_X, 3, 7)));
                    helper.assertTrue(door.is(HangarBlocks.BAY_DOOR.get()) && !door.getValue(HangarPieceBlock.OPEN),
                            "the doors close behind it");
                })
                .thenSucceed();
    }

    @GameTest(template = WAY, batch = BATCH, timeoutTicks = 2600)
    public static void crawler_carriesTheLauncherToTheEndAndComesBack(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos engine = helper.absolutePos(ENGINE);
        build(helper, engine);
        lanes(helper);
        launcher(helper);
        final BlockPos deck = helper.absolutePos(new BlockPos(DECK_FROM_X, 6, 7));
        final BlockPos pedestal = helper.absolutePos(new BlockPos(DECK_FROM_X + 1, 3, 1));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(engine(level, engine).along().isPresent(), "the crawler forms"))
                .thenExecute(() -> {
                    final Optional<Text> why = engine(level, engine).start();
                    helper.assertTrue(why.isEmpty(), "with the launcher over it, it sets off: " + why);
                })
                .thenWaitUntil(() -> helper.assertTrue(level.getBlockState(deck.relative(Direction.EAST, STEPS))
                        .is(LauncherBlocks.LAUNCHER_DECK.get()), "the launcher is set down at the far end"))
                .thenWaitUntil(() -> helper.assertTrue(level.getBlockEntity(engine) instanceof CrawlerEngineBlockEntity
                        at && at.phase() == CrawlerEngineBlockEntity.Phase.IDLE, "and the crawler comes back alone"))
                .thenExecute(() -> {
                    helper.assertTrue(level.getBlockState(deck).isAir(), "the launcher has left its pedestals");
                    helper.assertTrue(level.getBlockState(pedestal).is(LauncherBlocks.PAD_PEDESTAL.get()),
                            "which stay where they were");
                    final BlockPos arm = helper.absolutePos(ARM).relative(Direction.EAST, STEPS);
                    helper.assertTrue(level.getBlockEntity(arm) instanceof LauncherDeckBlockEntity measuring
                            && measuring.deck().isPresent(), "its hold-down arm still measures its deck there");
                })
                .thenSucceed();
    }

    @GameTest(template = WAY, batch = BATCH, timeoutTicks = 200)
    public static void crawler_staysWhereTheWayIsBlocked(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos engine = helper.absolutePos(ENGINE);
        build(helper, engine);
        lanes(helper);
        helper.setBlock(STONE, Blocks.STONE);
        final BlockPos stone = helper.absolutePos(STONE);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(engine(level, engine).along().isPresent(), "the crawler forms"))
                .thenExecute(() -> {
                    final Optional<Text> why = engine(level, engine).start();
                    helper.assertTrue(why.equals(Optional.of(CrawlerTexts.BLOCKED.with(stone.getX(), stone.getY(),
                            stone.getZ()))), "a stone in its way, it says where and stays: " + why);
                    helper.assertTrue(engine(level, engine).phase() == CrawlerEngineBlockEntity.Phase.IDLE,
                            "on the grid");
                })
                .thenSucceed();
    }

    /* Builds the crawler round the engine at {@code engine}, its length along x. */
    private static void build(final GameTestHelper helper, final BlockPos engine) {
        for (final CrawlerLayout.Spot spot : CrawlerLayout.spots(engine, Direction.EAST)) {
            helper.getLevel().setBlockAndUpdate(spot.at(), spot.kind().block().defaultBlockState());
        }
    }

    /* Lays its crawlerway, a lane of gravel under each side's trucks, from x 1 to x 40. */
    private static void lanes(final GameTestHelper helper) {
        for (int x = LANE_FROM; x <= LANE_TO; x++) {
            for (final int z : new int[] {2, 3, 11, 12}) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.GRAVEL);
            }
        }
    }

    /*
     * A launcher's deck over the crawler, three blocks thick and as wide as a hangar lays it, on two rows of pedestals
     * the crawler stands between, its hold-down arm measuring it centred over the engine.
     */
    private static void launcher(final GameTestHelper helper) {
        for (int x = DECK_FROM_X; x <= DECK_TO_X; x++) {
            for (int z = ENGINE.getZ() - DECK_HALF; z <= ENGINE.getZ() + DECK_HALF; z++) {
                for (int y = 5; y <= 7; y++) {
                    helper.setBlock(new BlockPos(x, y, z), LauncherBlocks.LAUNCHER_DECK.get());
                }
            }
        }
        for (final int x : new int[] {DECK_FROM_X + 1, ENGINE.getX(), DECK_TO_X - 1}) {
            for (final int z : new int[] {ENGINE.getZ() - DECK_HALF, ENGINE.getZ() + DECK_HALF}) {
                for (int y = 2; y <= 4; y++) {
                    helper.setBlock(new BlockPos(x, y, z), LauncherBlocks.PAD_PEDESTAL.get());
                }
            }
        }
        helper.setBlock(ARM, LauncherBlocks.HOLD_DOWN_ARM.get());
        if (helper.getLevel().getBlockEntity(helper.absolutePos(ARM)) instanceof LauncherDeckBlockEntity arm) {
            final BlockPos axis = ENGINE.atY(ARM.getY()).subtract(ARM);
            arm.deck(new LauncherDeckBlockEntity.Deck(Direction.EAST, 1, axis, ENGINE.getX() - DECK_FROM_X,
                    DECK_TO_X - ENGINE.getX(), DECK_HALF));
        }
    }

    private static CrawlerEngineBlockEntity engine(final ServerLevel level, final BlockPos at) {
        if (level.getBlockEntity(at) instanceof CrawlerEngineBlockEntity engine) {
            return engine;
        }
        throw new IllegalStateException("No crawler engine at " + at);
    }
}
