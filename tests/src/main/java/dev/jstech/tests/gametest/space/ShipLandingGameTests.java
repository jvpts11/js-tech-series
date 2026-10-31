/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import dev.jstech.core.team.Ownership;
import dev.jstech.space.landing.RunwayBeaconBlock;
import dev.jstech.space.landing.Runways;
import dev.jstech.space.landing.ShipLandingBlocks;
import dev.jstech.space.landing.ShipLandings;
import dev.jstech.space.landing.ShipPadBlockEntity;
import dev.jstech.space.ship.ShipAutopilots;
import dev.jstech.testkit.gametest.GameTestPlayers;
import dev.jstech.tests.JsTests;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Where a ship sets down: a ship landing pad forms from its octagon of plates round its terminal, the corners cut off
 * it needing nothing, and is noted for its owner's ships, its middle where they set down, and let go as a plate is
 * broken; a runway beacon is noted facing the way its runway runs and let go when broken, and its runway's stretch is
 * clear only while it is long, firm and free.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ShipLandingGameTests {

    private static final String PAD_ARENA = "ship_pad";
    private static final String RUNWAY_ARENA = "runway";
    /* The pad's terminal, in the middle of its arena's floor; the runway's beacon, near one end of its arena's. */
    private static final BlockPos TERMINAL = new BlockPos(11, 2, 11);
    private static final BlockPos BEACON = new BlockPos(7, 2, 2);
    /* How far along the runway a block stands in its way. */
    private static final int IN_THE_WAY = 30;

    private ShipLandingGameTests() {
    }

    @GameTest(template = PAD_ARENA, timeoutTicks = 400)
    public static void shipPad_formsFromItsOctagonOfPlatesAndIsNotedForItsOwnersShips(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos terminal = helper.absolutePos(TERMINAL);
        final ServerPlayer owner = GameTestPlayers.connected(helper, "owner");
        level.setBlockAndUpdate(terminal, ShipLandingBlocks.TERMINAL.get().defaultBlockState());
        final ShipPadBlockEntity pad = (ShipPadBlockEntity) level.getBlockEntity(terminal);
        pad.own(Ownership.of(owner.getUUID()));
        int plates = 0;
        for (int x = -ShipLandingBlocks.PAD_REACH; x <= ShipLandingBlocks.PAD_REACH; x++) {
            for (int z = -ShipLandingBlocks.PAD_REACH; z <= ShipLandingBlocks.PAD_REACH; z++) {
                if ((x != 0 || z != 0) && ShipLandingBlocks.onPad(x, z)) {
                    level.setBlockAndUpdate(terminal.offset(x, 0, z), ShipLandingBlocks.PLATE.get()
                            .defaultBlockState());
                    plates++;
                }
            }
        }
        helper.assertTrue(plates == 356, "an octagon of 356 plates round its terminal: " + plates);
        final BlockPos edge = terminal.offset(ShipLandingBlocks.PAD_REACH, 0, 0);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(pad.shownFormed(), "the pad forms, its corners cut off"))
                .thenWaitUntil(() -> helper.assertTrue(ShipLandings.pad(level.getServer(), pad.id()).isPresent(),
                        "and is noted"))
                .thenExecute(() -> {
                    final Vec3 spot = ShipLandings.padSpot(level, ShipLandings.pad(level.getServer(), pad.id())
                            .orElseThrow());
                    helper.assertTrue(spot.equals(Vec3.atBottomCenterOf(terminal.above())), "its middle over its"
                            + " terminal, where a ship sets down: " + spot);
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
