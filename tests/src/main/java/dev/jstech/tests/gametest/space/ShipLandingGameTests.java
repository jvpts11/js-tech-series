/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import dev.jstech.core.multiblock.Orientation;
import dev.jstech.core.team.Ownership;
import dev.jstech.space.landing.RunwayBeaconBlock;
import dev.jstech.space.landing.Runways;
import dev.jstech.space.landing.ShipLandingBlocks;
import dev.jstech.space.landing.ShipLandings;
import dev.jstech.space.landing.ShipPadBlockEntity;
import dev.jstech.space.ship.ShipAutopilots;
import dev.jstech.testkit.gametest.GameTestPlayers;
import dev.jstech.tests.JsTests;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Where a ship sets down: a ship landing pad forms from its round deck of plates, its truss, its stair and its masts
 * round its terminal on the deck, and its blocks merge into the pad drawn whole; it is noted for its owner's ships, its
 * deck's middle where they set down, whichever way it was built, and let go as a plate is broken; a runway beacon is
 * noted facing the way its runway runs and let go when broken, and its runway's stretch is clear only while it is long,
 * firm and free.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ShipLandingGameTests {

    private static final String PAD_ARENA = "ship_pad";
    private static final String RUNWAY_ARENA = "runway";
    /* The block under the pad's deck's middle, in the middle of its arena's floor; the runway's beacon, by one end. */
    private static final BlockPos MIDDLE = new BlockPos(15, 2, 15);
    private static final BlockPos BEACON = new BlockPos(7, 2, 2);
    /* The pattern's bounds round its middle, as it faces north: across, up, along. */
    private static final int REACH = ShipLandingBlocks.DECK_RADIUS;
    private static final int TOP_LAYER = 8;
    private static final int SOUTH = 15;
    /* How many blocks of each kind the pad is: plates, truss, steps and masts. */
    private static final Map<Character, Integer> KINDS = Map.of('P', 341, 'T', 90, 'S', 15, 'M', 24);
    /* How far along the runway a block stands in its way. */
    private static final int IN_THE_WAY = 30;

    private ShipLandingGameTests() {
    }

    @GameTest(template = PAD_ARENA, timeoutTicks = 400)
    public static void shipPad_formsRaisedAndIsNotedForItsOwnersShipsOverItsDecksMiddle(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos middle = helper.absolutePos(MIDDLE);
        final ServerPlayer owner = GameTestPlayers.connected(helper, "owner");
        final Map<Character, Integer> laid = build(level, middle, Orientation.NORTH);
        helper.assertTrue(laid.equals(KINDS), "its deck, truss, stair and masts: " + laid);
        final BlockPos terminal = middle.offset(ShipLandingBlocks.TERMINAL_X, ShipLandingBlocks.TERMINAL_Y,
                ShipLandingBlocks.TERMINAL_Z);
        final ShipPadBlockEntity pad = (ShipPadBlockEntity) level.getBlockEntity(terminal);
        pad.own(Ownership.of(owner.getUUID()));
        final BlockPos deck = middle.above(ShipLandingBlocks.DECK);
        final BlockPos edge = middle.offset(REACH, ShipLandingBlocks.DECK, 0);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(pad.shownFormed() && pad.merged(), "the pad forms, its blocks"
                        + " merged into it"))
                .thenWaitUntil(() -> helper.assertTrue(ShipLandings.pad(level.getServer(), pad.id()).isPresent(),
                        "and is noted"))
                .thenExecute(() -> {
                    final Vec3 spot = ShipLandings.padSpot(level, ShipLandings.pad(level.getServer(), pad.id())
                            .orElseThrow());
                    helper.assertTrue(pad.middle().equals(deck) && spot.equals(Vec3.atBottomCenterOf(deck.above())),
                            "its ships set down on its deck's middle, three blocks up: " + spot + " for " + deck);
                    helper.assertTrue(ShipLandings.padsUsableBy(owner, level).stream().anyMatch(each ->
                            each.terminal().equals(terminal)), "its owner's ships may use it");
                    helper.assertFalse(ShipLandings.taken(level, spot, null), "no ship stands on it");
                    level.destroyBlock(edge, false);
                })
                .thenWaitUntil(() -> helper.assertTrue(!pad.shownFormed()
                        && ShipLandings.pad(level.getServer(), pad.id()).isEmpty(), "a plate broken, it is undone and"
                        + " let go"))
                .thenExecute(() -> GameTestPlayers.leave(owner))
                .thenSucceed();
    }

    @GameTest(template = PAD_ARENA, timeoutTicks = 400)
    public static void shipPad_builtTurnedSetsItsShipsDownOnItsDecksMiddleStill(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos middle = helper.absolutePos(MIDDLE);
        build(level, middle, Orientation.EAST);
        final int[] at = Orientation.EAST.apply(ShipLandingBlocks.TERMINAL_X, ShipLandingBlocks.TERMINAL_Y,
                ShipLandingBlocks.TERMINAL_Z);
        final ShipPadBlockEntity pad = (ShipPadBlockEntity) level.getBlockEntity(middle.offset(at[0], at[1], at[2]));
        pad.own(Ownership.of(UUID.randomUUID()));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(pad.shownFormed(), "built turned east, the pad forms"))
                .thenExecute(() -> helper.assertTrue(pad.middle().equals(middle.above(ShipLandingBlocks.DECK)),
                        "its deck's middle found from its terminal, turned as it stands: " + pad.middle() + " for "
                                + middle.above(ShipLandingBlocks.DECK)))
                .thenSucceed();
    }

    /*
     * Builds the pad round {@code middle}, the block under its deck's middle on the ground, its pattern turned as
     * {@code turned} says; how many blocks of each kind it laid, its terminal aside.
     */
    private static Map<Character, Integer> build(final ServerLevel level, final BlockPos middle,
                                                 final Orientation turned) {
        final Map<Character, Integer> laid = new HashMap<>();
        for (int x = -REACH; x <= REACH; x++) {
            for (int y = 0; y <= TOP_LAYER; y++) {
                for (int z = -REACH; z <= SOUTH; z++) {
                    final char letter = ShipLandingBlocks.letterAt(x, y, z);
                    final Block block = switch (letter) {
                        case '#' -> ShipLandingBlocks.TERMINAL.get();
                        case 'P' -> ShipLandingBlocks.PLATE.get();
                        case 'T' -> ShipLandingBlocks.TRUSS.get();
                        case 'S' -> ShipLandingBlocks.STEP.get();
                        case 'M' -> ShipLandingBlocks.MAST_BLOCK.get();
                        default -> null;
                    };
                    if (block == null) {
                        continue;
                    }
                    final int[] at = turned.apply(x, y, z);
                    level.setBlockAndUpdate(middle.offset(at[0], at[1], at[2]), block.defaultBlockState());
                    if (letter != '#') {
                        laid.merge(letter, 1, Integer::sum);
                    }
                }
            }
        }
        return laid;
    }

    @GameTest(template = RUNWAY_ARENA, timeoutTicks = 200)
    public static void runwayBeacon_isNotedFacingItsRunwayWhoseStretchIsClearOnlyLongFirmAndFree(
            final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos at = helper.absolutePos(BEACON);
        level.setBlockAndUpdate(at, ShipLandingBlocks.BEACON.get().defaultBlockState()
                .setValue(RunwayBeaconBlock.FACING, Direction.SOUTH));
        final Optional<ShipLandings.Beacon> noted = ShipLandings.beacons(level).stream()
                .filter(each -> each.at().equals(at)).findFirst();
        helper.assertTrue(noted.isPresent() && noted.get().way() == Direction.SOUTH && noted.get().heading() == 180,
                "put down, it is noted with the way its runway runs: " + noted);
        final ShipLandings.Beacon beacon = noted.orElseThrow();
        helper.assertTrue(Runways.clear(level, beacon, null).equals(Optional.of(true)), "its stretch is clear: "
                + Runways.clear(level, beacon, null) + ", in the way " + inTheWay(level, beacon));
        final BlockPos wall = at.relative(Direction.SOUTH, IN_THE_WAY);
        level.setBlockAndUpdate(wall, Blocks.STONE.defaultBlockState());
        helper.assertTrue(Runways.clear(level, beacon, null).equals(Optional.of(false)), "a block in the way, it is"
                + " not");
        level.removeBlock(wall, false);
        final BlockPos hole = wall.below();
        level.setBlockAndUpdate(hole, Blocks.AIR.defaultBlockState());
        helper.assertTrue(Runways.clear(level, beacon, null).equals(Optional.of(false)), "a hole in it, it is not");
        level.setBlockAndUpdate(hole, Blocks.BARRIER.defaultBlockState());
        helper.assertTrue(Runways.clear(level, beacon, null).equals(Optional.of(true)), "mended, it is again");
        level.destroyBlock(at, false);
        helper.assertTrue(ShipLandings.beacons(level).stream().noneMatch(each -> each.at().equals(at)), "broken, the"
                + " beacon is let go");
        helper.succeed();
    }

    /* What stands in the way along the stretch of {@code beacon}'s runway, or where its land is not firm: the first. */
    private static String inTheWay(final ServerLevel level, final ShipLandings.Beacon beacon) {
        final Direction right = beacon.way().getClockWise();
        for (int along = 1; along <= ShipAutopilots.RUNWAY_LENGTH; along++) {
            for (int across = -Runways.HALF_WIDTH; across <= Runways.HALF_WIDTH; across++) {
                final BlockPos foot = beacon.at().relative(beacon.way(), along).relative(right, across);
                if (!level.getBlockState(foot.below()).isFaceSturdy(level, foot.below(), Direction.UP)) {
                    return "no firm land under " + foot + ": " + level.getBlockState(foot.below());
                }
                for (int up = 0; up < Runways.HEADROOM; up++) {
                    if (!level.getBlockState(foot.above(up)).getCollisionShape(level, foot.above(up)).isEmpty()) {
                        return level.getBlockState(foot.above(up)) + " at " + foot.above(up);
                    }
                }
            }
        }
        return "nothing on the land; a moving structure?";
    }
}
