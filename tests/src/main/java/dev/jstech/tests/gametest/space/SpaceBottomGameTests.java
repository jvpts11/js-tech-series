/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.StructurePose;
import dev.jstech.core.moving.StructureResult;
import dev.jstech.space.universe.SolarBody;
import dev.jstech.space.universe.SpaceClimbs;
import dev.jstech.space.universe.SphereMap;
import dev.jstech.space.universe.StarSystems;
import dev.jstech.space.universe.SystemLayout;
import dev.jstech.space.universe.SystemSpaces;
import dev.jstech.tests.JsTests;
import dev.jstech.testkit.gametest.GameTestPlayers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A system's space has no bottom: a player or a moving structure that goes far under its lowest block, where no world
 * lies under them, is neither hurt nor taken away, and stays where it went, as high up would be. Where a world lies
 * under it, a structure sent under the floor of the low orbits goes down into that world, over its middle or near the
 * edge of its map.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class SpaceBottomGameTests {

    private static final String ARENA = "empty";
    /* A spot of the Solar System's space over no world, far from the Overworld, the Moon and Mars. */
    private static final double X = 0;
    private static final double Z = 15_000;
    /* How deep under the space's lowest block they go, where the game would take away whatever falls. */
    private static final double DEEP = -500;
    private static final double CLOSE = 1;
    private static final BlockPos CRAFT = new BlockPos(2, 2, 2);
    private static final int WATCHED = 60;
    /* How far under the orbits' floor, over a world, a structure is sent. */
    private static final double UNDER_FLOOR = 20;
    /* Walking a spot out from a world's middle, in blocks, to the longitude near its map's edge, in degrees. */
    private static final double EDGE_STEP = 16;
    private static final int EDGE_STEPS = 100_000;
    private static final double EDGE_LONGITUDE = 175;
    /* How many chunks round the chunk a structure comes down in are made before it is sent. */
    private static final int AROUND = 1;

    private SpaceBottomGameTests() {
    }

    @GameTest(template = ARENA, batch = "jsspace_bottom", timeoutTicks = 400)
    public static void bottom_letsAPlayerGoFarDownInSpaceUnharmed(final GameTestHelper helper) {
        final ServerLevel space = space(helper);
        helper.assertTrue(SystemLayout.over(X, Z) == null, "no world lies under the spot");
        final ServerPlayer player = GameTestPlayers.connected(helper, "deep_diver");
        // Creative: the void kills a player in creative mode too, while space's air and cold leave them alone.
        player.setGameMode(GameType.CREATIVE);
        player.teleportTo(space, X, DEEP, Z, 0, 0);
        helper.startSequence()
                .thenIdle(WATCHED)
                .thenExecute(() -> {
                    helper.assertTrue(player.isAlive() && player.level() == space, "the player is still there, in "
                            + player.level().dimension().location());
                    helper.assertTrue(player.getHealth() == player.getMaxHealth(), "unhurt by the void: "
                            + player.getHealth());
                    helper.assertTrue(Math.abs(player.getY() - DEEP) < CLOSE, "where they went: " + player.getY());
                    GameTestPlayers.leave(player);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = "jsspace_bottom", timeoutTicks = 3000)
    public static void bottom_letsAMovingStructureGoFarDownInSpaceWhole(final GameTestHelper helper) {
        final ServerLevel overworld = helper.getLevel();
        helper.setBlock(CRAFT, Blocks.IRON_BLOCK);
        final StructureResult made = MovingStructures.assemble(overworld, helper.absolutePos(CRAFT),
                helper.absolutePos(CRAFT));
        helper.assertTrue(made.ok(), "the craft is made");
        final MovingStructure craft = made.structure();
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(craft.ready(), "the craft is ready"))
                .thenExecute(() -> helper.assertTrue(MovingStructures.travel(craft, space(helper),
                        new Vec3(X, SystemLayout.ORBIT_Y, Z)).ok(), "the craft is sent into space"))
                .thenWaitUntil(() -> {
                    final MovingStructure there = MovingStructures.byId(space(helper), craft.id());
                    helper.assertTrue(there != null && there.ready(), "the craft is in space");
                })
                .thenExecute(() -> {
                    final MovingStructure there = MovingStructures.byId(space(helper), craft.id());
                    final StructurePose at = there.pose();
                    there.teleport(at.movedTo(at.x(), DEEP, at.z()));
                })
                .thenIdle(WATCHED)
                .thenExecute(() -> {
                    final MovingStructure there = MovingStructures.byId(space(helper), craft.id());
                    helper.assertTrue(there != null && !there.entity().isRemoved() && there.blocks() == 1,
                            "far under the space's lowest block the craft is still there, whole");
                    helper.assertTrue(Math.abs(there.pose().y() - DEEP) < CLOSE, "where it went, not put back up: "
                            + there.pose().y());
                    MovingStructures.takeAway(there);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = "jsspace_bottom", timeoutTicks = 1200)
    public static void bottom_aStructureSentUnderTheFloorOverAWorldArrives(final GameTestHelper helper) {
        final ServerLevel overworld = helper.getLevel();
        helper.setBlock(CRAFT, Blocks.IRON_BLOCK);
        final StructureResult made = MovingStructures.assemble(overworld, helper.absolutePos(CRAFT),
                helper.absolutePos(CRAFT));
        helper.assertTrue(made.ok(), "the craft is made");
        final MovingStructure craft = made.structure();
        final double[] over = SystemLayout.centre(SolarBody.OVERWORLD);
        final Vec3 under = new Vec3(over[0], SpaceClimbs.floor() - UNDER_FLOOR, over[2]);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(craft.ready(), "the craft is ready"))
                .thenExecute(() -> helper.assertTrue(SystemLayout.over(under.x, under.z) == SolarBody.OVERWORLD,
                        "the Overworld lies under the spot"))
                .thenExecute(() -> {
                    kept(helper, under, true);
                    helper.assertTrue(MovingStructures.travel(craft, space(helper), under).ok(),
                            "the craft is sent into space, under the floor");
                })
                .thenWaitUntil(() -> helper.assertTrue(arrived(helper, craft), "under the floor over the Overworld,"
                        + " the craft goes down into it; " + whereItIs(helper, craft)))
                .thenExecute(() -> {
                    kept(helper, under, false);
                    MovingStructures.takeAway(MovingStructures.byId(overworld, craft.id()));
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = "jsspace_bottom", timeoutTicks = 1800)
    public static void bottom_aStructureUnderTheFloorNearAWorldsEdgeArrives(final GameTestHelper helper) {
        final ServerLevel overworld = helper.getLevel();
        helper.setBlock(CRAFT, Blocks.IRON_BLOCK);
        final StructureResult made = MovingStructures.assemble(overworld, helper.absolutePos(CRAFT),
                helper.absolutePos(CRAFT));
        helper.assertTrue(made.ok(), "the craft is made");
        final MovingStructure craft = made.structure();
        final Vec3 under = nearTheEdge();
        final double[] place = SystemLayout.placeUnder(SolarBody.OVERWORLD, under.x, under.z);
        // The land it comes down on is made first: it lies far out, where the game has made none yet, and the test
        // server runs its ticks faster than new land is made, while its wait counts ticks, not time.
        final double width = Math.max(1, overworld.getWorldBorder().getSize());
        final int landX = Mth.floor(SphereMap.x(place[0], width)) >> 4;
        final int landZ = Mth.floor(SphereMap.z(place[1], width)) >> 4;
        for (int dx = -AROUND; dx <= AROUND; dx++) {
            for (int dz = -AROUND; dz <= AROUND; dz++) {
                overworld.getChunk(landX + dx, landZ + dz);
            }
        }
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(craft.ready(), "the craft is ready"))
                .thenExecute(() -> {
                    kept(helper, under, true);
                    helper.assertTrue(MovingStructures.travel(craft, space(helper), under).ok(), "the craft is sent"
                            + " into space, under the floor near the Overworld's edge, over " + place[0] + " east, "
                            + place[1] + " north");
                })
                .thenWaitUntil(() -> helper.assertTrue(arrived(helper, craft), "near the Overworld's edge, the craft"
                        + " goes down into it; " + whereItIs(helper, craft)))
                .thenExecute(() -> {
                    kept(helper, under, false);
                    MovingStructures.takeAway(MovingStructures.byId(overworld, craft.id()));
                })
                .thenSucceed();
    }

    /*
     * Keeps the chunk of space at {@code at} loaded, or lets it go: no one is aboard the craft to keep the place it
     * waits at loaded while the land under it loads, and a structure waiting to go, its chunk let go, waits there
     * unloaded till someone comes.
     */
    private static void kept(final GameTestHelper helper, final Vec3 at, final boolean held) {
        space(helper).setChunkForced(SectionPos.blockToSectionCoord(at.x), SectionPos.blockToSectionCoord(at.z),
                held);
    }

    /* Whether {@code craft}, sent from the Overworld into space, has come down into the Overworld again, ready. */
    private static boolean arrived(final GameTestHelper helper, final MovingStructure craft) {
        final MovingStructure home = MovingStructures.byId(helper.getLevel(), craft.id());
        return home != null && home.ready() && home != craft;
    }

    /* Where {@code craft}, sent from the Overworld into space, is: in space, in the Overworld, or neither. */
    private static String whereItIs(final GameTestHelper helper, final MovingStructure craft) {
        final MovingStructure inSpace = MovingStructures.byId(space(helper), craft.id());
        final MovingStructure home = MovingStructures.byId(helper.getLevel(), craft.id());
        final String space = inSpace == null ? "gone" : "at " + inSpace.pose() + (inSpace.departing()
                ? ", waiting to go" : ", not going");
        final String overworld = home == null ? "it is not" : home == craft ? "it is the one sent, "
                + (craft.departing() ? "waiting to go" : "not going") : home.blocks() + " blocks at " + home.pose()
                + (home.ready() ? ", ready" : ", not ready") + (home.departing() ? ", waiting to go" : "");
        return "in space it is " + space + "; in the Overworld " + overworld;
    }

    /* A spot under the floor over the Overworld, as near the edge of the map of it as its region reaches. */
    private static Vec3 nearTheEdge() {
        final double[] middle = SystemLayout.centre(SolarBody.OVERWORLD);
        double x = middle[0];
        double z = middle[2];
        for (int k = 0; k < EDGE_STEPS; k++) {
            final double nx = x + EDGE_STEP;
            final double nz = z - EDGE_STEP;
            if (SystemLayout.over(nx, nz) != SolarBody.OVERWORLD) {
                break;
            }
            final double[] place = SystemLayout.placeUnder(SolarBody.OVERWORLD, nx, nz);
            if (Math.abs(place[0]) >= EDGE_LONGITUDE) {
                break;
            }
            x = nx;
            z = nz;
        }
        return new Vec3(x, SpaceClimbs.floor() - UNDER_FLOOR, z);
    }

    private static ServerLevel space(final GameTestHelper helper) {
        return SystemSpaces.spaceOf(helper.getLevel().getServer(), StarSystems.SOLAR_SYSTEM);
    }
}
