/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import dev.jstech.core.air.AirMix;
import dev.jstech.core.dimension.RuntimeDimensions;
import dev.jstech.core.room.GasMath;
import dev.jstech.core.room.Room;
import dev.jstech.core.room.SealedRooms;
import dev.jstech.space.geology.DryIceBlock;
import dev.jstech.space.geology.SpaceRocks;
import dev.jstech.space.geology.WaterIceBlock;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestWorldGen;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The ices of Mars, off the Earth: Water Ice stays put in the cold, turns to vapour when warmed in a vacuum, and melts
 * into water only in a sealed room full of air; Dry Ice, warmed even in a room of air, gives its carbon dioxide into
 * the room share by share until it is gone; the Icy Regolith never thaws by itself.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class SpaceIceGameTests {

    private static final String ARENA = "empty";
    private static final String BATCH = "jsspace_ice";
    /* On the test Moon: the ice on the floor of a hut whose shell runs from LOW to HIGH, and a place of its air. */
    private static final BlockPos ICE = new BlockPos(3, 9, 3);
    private static final BlockPos LOW = new BlockPos(2, 8, 2);
    private static final BlockPos HIGH = new BlockPos(5, 11, 5);
    private static final BlockPos INSIDE = new BlockPos(4, 10, 4);
    private static final double ATMOSPHERE = 101.3;
    private static final double CLOSE = 1e-6;

    private SpaceIceGameTests() {
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void waterIce_staysInTheColdAndTurnsToVapourWarmedInAVacuum(final GameTestHelper helper) {
        final ResourceKey<Level> key = key("ice_vacuum");
        final ServerLevel moon = moon(helper, key);
        moon.setBlockAndUpdate(ICE.below(), Blocks.STONE.defaultBlockState());
        moon.setBlockAndUpdate(ICE, SpaceRocks.WATER_ICE.get().defaultBlockState());
        final boolean thawedInTheCold = WaterIceBlock.thaw(moon, ICE);
        moon.setBlockAndUpdate(ICE.above(2), Blocks.GLOWSTONE.defaultBlockState());
        helper.startSequence()
                .thenIdle(10)
                .thenExecute(() -> {
                    final boolean thawed = WaterIceBlock.thaw(moon, ICE);
                    final BlockState left = moon.getBlockState(ICE);
                    RuntimeDimensions.remove(helper.getLevel().getServer(), key);
                    helper.assertFalse(thawedInTheCold, "in the Moon's cold and dark the ice stays");
                    helper.assertTrue(thawed && left.isAir(), "warmed by a light in a vacuum it turns to vapour and is"
                            + " gone; " + left);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 300)
    public static void waterIce_meltsIntoWaterOnlyInARoomFullOfAir(final GameTestHelper helper) {
        final ResourceKey<Level> key = key("ice_room");
        final ServerLevel moon = moon(helper, key);
        hut(moon);
        moon.setBlockAndUpdate(ICE, SpaceRocks.WATER_ICE.get().defaultBlockState());
        SealedRooms.track(moon, INSIDE);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(SealedRooms.roomAt(moon, INSIDE).isPresent(),
                        "the hut is a room"))
                .thenExecute(() -> {
                    breathe(SealedRooms.roomAt(moon, INSIDE).orElseThrow());
                    final boolean thawed = WaterIceBlock.thaw(moon, ICE);
                    final BlockState left = moon.getBlockState(ICE);
                    SealedRooms.untrack(moon, INSIDE);
                    RuntimeDimensions.remove(helper.getLevel().getServer(), key);
                    helper.assertTrue(thawed && left.is(Blocks.WATER), "warmed in a room of air it melts into water; "
                            + left);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 300)
    public static void dryIce_givesItsCarbonDioxideIntoAWarmRoomShareByShare(final GameTestHelper helper) {
        final ResourceKey<Level> key = key("dry_ice_room");
        final ServerLevel moon = moon(helper, key);
        hut(moon);
        moon.setBlockAndUpdate(ICE, SpaceRocks.DRY_ICE.get().defaultBlockState());
        SealedRooms.track(moon, INSIDE);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(SealedRooms.roomAt(moon, INSIDE).isPresent(),
                        "the hut is a room"))
                .thenExecute(() -> {
                    final Room room = SealedRooms.roomAt(moon, INSIDE).orElseThrow();
                    breathe(room);
                    final double before = room.moles(AirMix.CARBON_DIOXIDE);
                    DryIceBlock.sublimate(moon, ICE, moon.getBlockState(ICE));
                    final double oneShare = room.moles(AirMix.CARBON_DIOXIDE) - before;
                    final BlockState afterOne = moon.getBlockState(ICE);
                    for (int share = 1; share < DryIceBlock.SHARES; share++) {
                        DryIceBlock.sublimate(moon, ICE, moon.getBlockState(ICE));
                    }
                    final double given = room.moles(AirMix.CARBON_DIOXIDE) - before;
                    final BlockState left = moon.getBlockState(ICE);
                    SealedRooms.untrack(moon, INSIDE);
                    RuntimeDimensions.remove(helper.getLevel().getServer(), key);
                    helper.assertTrue(Math.abs(oneShare - DryIceBlock.MOLES / DryIceBlock.SHARES) < CLOSE
                            && afterOne.is(SpaceRocks.DRY_ICE.get()), "warmed once, it gives a share of its gas and"
                            + " stays; " + oneShare + " moles, " + afterOne);
                    helper.assertTrue(Math.abs(given - DryIceBlock.MOLES) < CLOSE * DryIceBlock.MOLES,
                            "all its gas went into the room: " + given + " moles");
                    helper.assertTrue(left.isAir(), "and it is gone; " + left);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void icyRegolith_neverThawsByItself(final GameTestHelper helper) {
        helper.assertFalse(SpaceRocks.ICY_REGOLITH.get().defaultBlockState().isRandomlyTicking(),
                "the Icy Regolith gives its water only in a machine");
        helper.assertTrue(SpaceRocks.WATER_ICE.get().defaultBlockState().isRandomlyTicking()
                && SpaceRocks.DRY_ICE.get().defaultBlockState().isRandomlyTicking(), "the ices thaw by themselves");
        helper.succeed();
    }

    /* Fills {@code room} with the Earth's air. */
    private static void breathe(final Room room) {
        room.empty();
        room.add(AirMix.NITROGEN, GasMath.moles(ATMOSPHERE * 0.79, room.gasVolume(), GasMath.TEMPERED));
        room.add(AirMix.OXYGEN, GasMath.moles(ATMOSPHERE * 0.21, room.gasVolume(), GasMath.TEMPERED));
    }

    private static ServerLevel moon(final GameTestHelper helper, final ResourceKey<Level> key) {
        final ServerLevel moon = RuntimeDimensions.getOrCreate(helper.getLevel().getServer(), key,
                TestWorldGen.MOON.id());
        moon.setChunkForced(0, 0, true);
        return moon;
    }

    /* A hut of stone from LOW to HIGH, its inside empty. */
    private static void hut(final ServerLevel moon) {
        for (int x = LOW.getX(); x <= HIGH.getX(); x++) {
            for (int y = LOW.getY(); y <= HIGH.getY(); y++) {
                for (int z = LOW.getZ(); z <= HIGH.getZ(); z++) {
                    final boolean shell = x == LOW.getX() || x == HIGH.getX() || y == LOW.getY() || y == HIGH.getY()
                            || z == LOW.getZ() || z == HIGH.getZ();
                    moon.setBlockAndUpdate(new BlockPos(x, y, z), shell ? Blocks.STONE.defaultBlockState()
                            : Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    private static ResourceKey<Level> key(final String path) {
        return ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(JsTests.MODID,
                path + "_" + UUID.randomUUID().toString().substring(0, 8)));
    }
}
