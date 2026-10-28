/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import dev.jstech.core.dimension.DimensionRespawns;
import dev.jstech.core.dimension.RuntimeDimensions;
import dev.jstech.space.api.SpaceWorlds;
import dev.jstech.space.universe.LandingCabins;
import dev.jstech.testkit.gametest.GameTestPlayers;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestSpaceWorlds;
import dev.jstech.tests.TestWorldGen;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Where a player who dies on another world comes back, by J's Space's rule over J's Core's: a bed out in the open
 * there counts for nothing; the cabin they landed in, still whole and with air, takes them back; with neither, they go
 * home to the Overworld. A death in the Overworld is left to the game. The test mod's world with air is made for the
 * tests, as a copy of a land the Overworld's rules hold, and taken away.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class SpaceRespawnGameTests {

    private static final String ARENA = "empty";
    /* High in the copied land, which is 128 blocks tall, above its ground. */
    private static final BlockPos CABIN = new BlockPos(0, 120, 0);
    private static final Vec3 BED_IN_THE_OPEN = new Vec3(40.5, 120, 40.5);

    private SpaceRespawnGameTests() {
    }

    @GameTest(template = ARENA)
    public static void death_onAnotherWorldGoesToTheCabinWholeAndWithAirElseHome(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final ResourceKey<Level> key = SpaceWorlds.get(helper.getLevel(), TestSpaceWorlds.HAVEN).orElseThrow()
                .dimension();
        final ServerLevel haven = RuntimeDimensions.getOrCreate(server, key, TestWorldGen.TEST_LAND.id());
        final ServerPlayer player = GameTestPlayers.connected(helper, "spacefarer");
        try {
            player.teleportTo(haven, CABIN.getX() + 0.5, CABIN.getY() + 1, CABIN.getZ() + 0.5, 0, 0);
            haven.setBlockAndUpdate(CABIN, Blocks.IRON_BLOCK.defaultBlockState());
            LandingCabins.mark(player, haven, CABIN);
            final DimensionTransition bedOutside = new DimensionTransition(haven, BED_IN_THE_OPEN, Vec3.ZERO, 0, 0,
                    DimensionTransition.DO_NOTHING);
            final DimensionTransition back = DimensionRespawns.respawn(player, bedOutside);
            helper.assertTrue(back.newLevel() == haven && BlockPos.containing(back.pos()).equals(CABIN.above()),
                    "a bed in the open counts for nothing, and the cabin landed in takes the player back");
            haven.setBlockAndUpdate(CABIN, Blocks.AIR.defaultBlockState());
            final DimensionTransition home = DimensionRespawns.respawn(player, bedOutside);
            helper.assertTrue(home.newLevel() == server.overworld(),
                    "with the cabin broken, the player goes home to the Overworld");
        } finally {
            GameTestPlayers.leave(player);
            RuntimeDimensions.remove(server, key);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void death_inTheOverworldIsLeftToTheGame(final GameTestHelper helper) {
        final ServerPlayer player = GameTestPlayers.connected(helper, "homebody");
        try {
            final DimensionTransition planned = new DimensionTransition(helper.getLevel(), Vec3.ZERO, Vec3.ZERO, 0, 0,
                    DimensionTransition.DO_NOTHING);
            helper.assertTrue(DimensionRespawns.respawn(player, planned) == planned,
                    "where the game sends a player who died at home is the game's to say");
        } finally {
            GameTestPlayers.leave(player);
        }
        helper.succeed();
    }
}
