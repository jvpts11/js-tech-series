/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.air.AirMix;
import dev.jstech.core.dimension.RuntimeDimensions;
import dev.jstech.core.heat.Thawing;
import dev.jstech.core.room.GasMath;
import dev.jstech.core.room.Room;
import dev.jstech.core.room.SealedRooms;
import dev.jstech.testkit.gametest.GameTestPlayers;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Ice by the air it meets: the game's ice broken on the Earth melts into water as before; broken in the Moon's vacuum
 * it turns to vapour and is gone, as it does in the Nether's heat; in a sealed room on the Moon, which keeps its
 * warmth, it melts into water again once the room is full of air. A bright light warms ice anywhere.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ThawingGameTests {

    private static final String ARENA = "empty";
    private static final String BATCH = "jscore_thawing";
    private static final BlockPos GROUND = new BlockPos(1, 1, 1);
    private static final BlockPos ICE = new BlockPos(1, 2, 1);
    /* On the test Moon: the ice on the floor of a hut whose shell runs from LOW to HIGH, and a place of its air. */
    private static final BlockPos MOON_ICE = new BlockPos(3, 9, 3);
    private static final BlockPos LOW = new BlockPos(2, 8, 2);
    private static final BlockPos HIGH = new BlockPos(5, 11, 5);
    private static final BlockPos INSIDE = new BlockPos(4, 10, 4);
    private static final double ATMOSPHERE = 101.3;

    private ThawingGameTests() {
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void ice_brokenOnTheEarthMeltsIntoWater(final GameTestHelper helper) {
        helper.setBlock(GROUND, Blocks.STONE);
        helper.setBlock(ICE, Blocks.ICE);
        final ServerPlayer player = GameTestPlayers.connected(helper, "earth_breaker");
        broken(helper.getLevel(), player, helper.absolutePos(ICE));
        GameTestPlayers.leave(player);
        helper.assertBlockPresent(Blocks.WATER, ICE);
        helper.assertTrue(Thawing.meltsToWater(helper.getLevel(), helper.absolutePos(ICE)),
                "the Earth's air keeps water liquid");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void ice_brokenOnTheMoonTurnsToVapour(final GameTestHelper helper) {
        final ResourceKey<Level> key = key("thawing_moon");
        final ServerLevel moon = moon(helper, key);
        moon.setBlockAndUpdate(MOON_ICE.below(), Blocks.STONE.defaultBlockState());
        moon.setBlockAndUpdate(MOON_ICE, Blocks.ICE.defaultBlockState());
        final boolean liquid = Thawing.meltsToWater(moon, MOON_ICE);
        final ServerPlayer player = GameTestPlayers.join(moon, UUID.randomUUID(), "moon_breaker");
        broken(moon, player, MOON_ICE);
        GameTestPlayers.leave(player);
        final BlockState left = moon.getBlockState(MOON_ICE);
        RuntimeDimensions.remove(helper.getLevel().getServer(), key);
        helper.assertFalse(liquid, "the Moon's vacuum keeps no water liquid");
        helper.assertTrue(left.isAir(), "the ice turned to vapour and is gone; " + left);
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 300)
    public static void ice_inARoomOfAirOnTheMoonIsWarmedAndMeltsIntoWater(final GameTestHelper helper) {
        final ResourceKey<Level> key = key("thawing_room");
        final ServerLevel moon = moon(helper, key);
        hut(moon);
        moon.setBlockAndUpdate(MOON_ICE, Blocks.ICE.defaultBlockState());
        SealedRooms.track(moon, INSIDE);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(SealedRooms.roomAt(moon, INSIDE).isPresent(),
                        "the hut is a room"))
                .thenExecute(() -> {
                    helper.assertTrue(Thawing.warmed(moon, MOON_ICE) && !Thawing.meltsToWater(moon, MOON_ICE),
                            "the hut keeps its warmth, but in its vacuum no water stays liquid");
                    final Room room = SealedRooms.roomAt(moon, INSIDE).orElseThrow();
                    room.empty();
                    room.add(AirMix.NITROGEN, GasMath.moles(ATMOSPHERE * 0.79, room.gasVolume(), GasMath.TEMPERED));
                    room.add(AirMix.OXYGEN, GasMath.moles(ATMOSPHERE * 0.21, room.gasVolume(), GasMath.TEMPERED));
                    final boolean warmed = Thawing.warmed(moon, MOON_ICE);
                    final boolean liquid = Thawing.meltsToWater(moon, MOON_ICE);
                    final ServerPlayer player = GameTestPlayers.join(moon, UUID.randomUUID(), "room_breaker");
                    broken(moon, player, MOON_ICE);
                    GameTestPlayers.leave(player);
                    final BlockState left = moon.getBlockState(MOON_ICE);
                    SealedRooms.untrack(moon, INSIDE);
                    RuntimeDimensions.remove(helper.getLevel().getServer(), key);
                    helper.assertTrue(warmed && liquid, "filled with air, the room warms the ice and keeps water");
                    helper.assertTrue(left.is(Blocks.WATER), "the ice melted into water; " + left);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void ice_isWarmedByABrightLightInTheMoonsCold(final GameTestHelper helper) {
        final ResourceKey<Level> key = key("thawing_light");
        final ServerLevel moon = moon(helper, key);
        moon.setBlockAndUpdate(MOON_ICE.below(), Blocks.STONE.defaultBlockState());
        moon.setBlockAndUpdate(MOON_ICE, Blocks.ICE.defaultBlockState());
        final boolean darkWarmed = Thawing.warmed(moon, MOON_ICE);
        moon.setBlockAndUpdate(MOON_ICE.above(2), Blocks.GLOWSTONE.defaultBlockState());
        helper.startSequence()
                .thenIdle(10)
                .thenExecute(() -> {
                    final boolean litWarmed = Thawing.warmed(moon, MOON_ICE);
                    RuntimeDimensions.remove(helper.getLevel().getServer(), key);
                    helper.assertFalse(darkWarmed, "in the dark of the Moon's cold the ice is not warmed");
                    helper.assertTrue(litWarmed, "a bright light beside it warms it");
                })
                .thenSucceed();
    }

    /* Breaks the ice at {@code pos} as {@code player} would with a bare hand, after the game took the block. */
    private static void broken(final ServerLevel level, final ServerPlayer player, final BlockPos pos) {
        final BlockState ice = level.getBlockState(pos);
        ice.getBlock().playerDestroy(level, player, pos, ice, null, ItemStack.EMPTY);
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
