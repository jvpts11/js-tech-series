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
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.StructurePose;
import dev.jstech.core.moving.StructureResult;
import dev.jstech.core.moving.StructureYard;
import dev.jstech.core.moving.YardLayout;
import dev.jstech.core.room.SealedRooms;
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
import net.minecraft.world.phys.AABB;
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
    private static final int AIRLESS_TIME = 200;
    /* How far east the landed module is moved after its landing, in blocks. */
    private static final int MOVED = 12;

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
            // In the open, whatever an earlier run left there: a cabin is a block, its room the world's air.
            unbuilt(haven, CABIN);
            player.teleportTo(haven, CABIN.getX() + 0.5, CABIN.getY() + 1, CABIN.getZ() + 0.5, 0, 0);
            haven.setBlockAndUpdate(CABIN, Blocks.IRON_BLOCK.defaultBlockState());
            LandingCabins.mark(player, haven, CABIN);
            final DimensionTransition bedOutside = new DimensionTransition(haven, BED_IN_THE_OPEN, Vec3.ZERO, 0, 0,
                    DimensionTransition.DO_NOTHING);
            final DimensionTransition back = DimensionRespawns.respawn(player, bedOutside);
            helper.assertTrue(back.newLevel() == haven && BlockPos.containing(back.pos()).equals(CABIN.above()),
                    "a bed in the open counts for nothing, and the cabin landed in takes the player back; sent to "
                            + back.newLevel().dimension().location() + " at " + back.pos() + ", the cabin "
                            + LandingCabins.of(player, haven).map(cabin -> cabin.pos() + " whole "
                            + cabin.whole(haven)).orElse("unknown") + ", air over it "
                            + SealedRooms.breathableAt(haven, CABIN.above()) + " (a room there "
                            + SealedRooms.roomAt(haven, CABIN.above()).isPresent() + ", the place's own "
                            + SealedRooms.placeClimate(haven, Vec3.atCenterOf(CABIN.above())) + ")");
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

    /*
     * The cabin landed in, still whole but its room emptied of air, takes no one back: the player goes home. In a batch
     * of its own, as the world with air is made and taken away by the test before it too.
     */
    @GameTest(template = ARENA, batch = "jsspace_respawn_airless", timeoutTicks = AIRLESS_TIME)
    public static void death_besideACabinWithNoAirGoesHome(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final ResourceKey<Level> key = SpaceWorlds.get(helper.getLevel(), TestSpaceWorlds.HAVEN).orElseThrow()
                .dimension();
        final ServerLevel haven = RuntimeDimensions.getOrCreate(server, key, TestWorldGen.TEST_LAND.id());
        final ServerPlayer player = GameTestPlayers.connected(helper, "airless");
        final BlockPos inside = CABIN.above();
        hut(haven, CABIN);
        player.teleportTo(haven, inside.getX() + 0.5, inside.getY(), inside.getZ() + 0.5, 0, 0);
        LandingCabins.mark(player, haven, CABIN);
        SealedRooms.track(haven, inside);
        // The world keeps its land and its rooms when it is taken away and made again, so the hut and its emptied room
        // go first, or the next test that makes the world would find them round its own cabin.
        final Runnable away = () -> {
            SealedRooms.untrack(haven, inside);
            unbuilt(haven, CABIN);
            GameTestPlayers.leave(player);
            RuntimeDimensions.remove(server, key);
        };
        // Should the room never form, what the test made goes just before it runs out.
        helper.runAfterDelay(AIRLESS_TIME - 5, away);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(SealedRooms.roomAt(haven, inside).isPresent(),
                        "the cabin is a sealed room"))
                .thenExecute(() -> {
                    SealedRooms.roomAt(haven, inside).orElseThrow().empty();
                    final boolean airless = !SealedRooms.breathableAt(haven, inside);
                    final DimensionTransition bedOutside = new DimensionTransition(haven, BED_IN_THE_OPEN, Vec3.ZERO,
                            0, 0, DimensionTransition.DO_NOTHING);
                    final DimensionTransition home = DimensionRespawns.respawn(player, bedOutside);
                    away.run();
                    helper.assertTrue(airless, "emptied, the cabin holds no air to breathe");
                    helper.assertTrue(home.newLevel() == server.overworld(),
                            "a cabin whole but with no air takes no one back: home to the Overworld");
                })
                .thenSucceed();
    }

    /*
     * A module landed stays a structure standing on the ground: its landing marks the cabin its crew stands in, and a
     * player who dies on that world comes back into it, wherever the module stands then. In a batch of its own, as
     * the world is made and taken away by the tests before it too.
     */
    @GameTest(template = ARENA, batch = "jsspace_respawn_module", timeoutTicks = AIRLESS_TIME)
    public static void death_onAnotherWorldGoesBackIntoTheModuleLandedWhereverItStands(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final ResourceKey<Level> key = SpaceWorlds.get(helper.getLevel(), TestSpaceWorlds.HAVEN).orElseThrow()
                .dimension();
        final ServerLevel haven = RuntimeDimensions.getOrCreate(server, key, TestWorldGen.TEST_LAND.id());
        final ServerPlayer player = GameTestPlayers.connected(helper, "lander");
        final BlockPos inside = CABIN.above();
        unbuilt(haven, CABIN);
        hut(haven, CABIN);
        warm(haven);
        final StructureResult made = MovingStructures.assemble(haven, CABIN.offset(-1, 0, -1), CABIN.offset(1, 3, 1));
        helper.assertTrue(made.ok() && made.structure() != null, "the hut is made a module: "
                + (made.refusal() == null ? "done" : made.refusal().english()));
        final MovingStructure module = made.structure();
        final BlockPos aboard = BlockPos.containing(MovingStructures.toYard(module, Vec3.atCenterOf(inside)));
        final Runnable away = () -> {
            SealedRooms.untrack(haven, aboard);
            MovingStructures.takeAway(module);
            GameTestPlayers.leave(player);
            RuntimeDimensions.remove(server, key);
        };
        helper.runAfterDelay(AIRLESS_TIME - 5, away);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(module.ready(), "its blocks come into the module"))
                .thenExecute(() -> {
                    player.teleportTo(haven, inside.getX() + 0.5, inside.getY(), inside.getZ() + 0.5, 0, 0);
                    // Its landing marks the cabin, as a module setting down on a world does for its crew.
                    LandingCabins.markAboard(player, haven, module);
                    SealedRooms.track(haven, aboard);
                })
                .thenWaitUntil(() -> helper.assertTrue(SealedRooms.breathableAt(haven, aboard),
                        "its cabin is a room with air"))
                .thenExecute(() -> {
                    final StructurePose now = module.pose();
                    module.teleport(now.movedTo(now.x() + MOVED, now.y(), now.z()));
                    final DimensionTransition bedOutside = new DimensionTransition(haven, BED_IN_THE_OPEN, Vec3.ZERO,
                            0, 0, DimensionTransition.DO_NOTHING);
                    final DimensionTransition back = DimensionRespawns.respawn(player, bedOutside);
                    final BlockPos expected = inside.offset(MOVED, 0, 0);
                    away.run();
                    helper.assertTrue(back.newLevel() == haven && BlockPos.containing(back.pos()).equals(expected),
                            "the player comes back into the module's cabin, where it stands now: sent to "
                                    + back.newLevel().dimension().location() + " at " + back.pos() + " for "
                                    + expected);
                })
                .thenSucceed();
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

    /* A closed box of iron round the two places over {@code cabin}, the cabin the middle of its floor. */
    private static void hut(final ServerLevel level, final BlockPos cabin) {
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 3; y++) {
                for (int z = -1; z <= 1; z++) {
                    final boolean room = x == 0 && z == 0 && (y == 1 || y == 2);
                    level.setBlockAndUpdate(cabin.offset(x, y, z), room ? Blocks.AIR.defaultBlockState()
                            : Blocks.IRON_BLOCK.defaultBlockState());
                }
            }
        }
    }

    /* Loads the chunks round the yard's next plot, so a structure made comes into it at once. */
    private static void warm(final ServerLevel level) {
        final int plot = StructureYard.nextPlot(level);
        final int cx = YardLayout.centreX(plot) >> 4;
        final int cz = YardLayout.centreZ(plot) >> 4;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                level.getChunk(cx + dx, cz + dz);
            }
        }
    }

    /*
     * Takes the hut round {@code cabin} away, its cabin and any room found in it with them, back to the open air it was
     * built in.
     */
    private static void unbuilt(final ServerLevel level, final BlockPos cabin) {
        SealedRooms.forget(level, new AABB(cabin).inflate(2));
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 3; y++) {
                for (int z = -1; z <= 1; z++) {
                    level.setBlockAndUpdate(cabin.offset(x, y, z), Blocks.AIR.defaultBlockState());
                }
            }
        }
    }
}
