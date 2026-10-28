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
import dev.jstech.core.moving.StructureResult;
import dev.jstech.space.network.FarMapPayload;
import dev.jstech.space.universe.FarMaps;
import dev.jstech.space.universe.FarRings;
import dev.jstech.space.universe.SolarBody;
import dev.jstech.space.universe.SpaceClimbs;
import dev.jstech.space.universe.SphereMap;
import dev.jstech.space.universe.StarSystems;
import dev.jstech.space.universe.SystemLayout;
import dev.jstech.space.universe.SystemSpaces;
import dev.jstech.tests.JsTests;
import dev.jstech.testkit.gametest.GameTestPlayers;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The way up to space and back: a player who climbs a few hundred blocks over the Overworld's build limit passes into
 * the Solar System's space over the spot of the Overworld's low orbit their place is laid under, and going down
 * through the floor of the low orbits passes back to the very place, high over it; a moving structure that climbs
 * there goes up whole; and the far map seen on the way is the ground as the world's generator makes it.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class SpaceClimbGameTests {

    private static final String ARENA = "empty";
    private static final double CLOSE = 1e-3;
    /* How far over the ceiling the climbers are put. */
    private static final int OVER = 5;
    private static final BlockPos CRAFT = new BlockPos(2, 2, 2);

    private SpaceClimbGameTests() {
    }

    @GameTest(template = ARENA, batch = "jsspace_climb", timeoutTicks = 2000)
    public static void climb_takesAPlayerUpOverTheirPlaceAndBackDownToIt(final GameTestHelper helper) {
        final ServerLevel overworld = helper.getLevel();
        final ServerPlayer player = GameTestPlayers.connected(helper, "climber");
        final BlockPos base = helper.absolutePos(BlockPos.ZERO);
        final double x = base.getX() + 0.5;
        final double z = base.getZ() + 0.5;
        final double width = overworld.getWorldBorder().getSize();
        final double[] spot = SystemLayout.orbitSpot(SolarBody.OVERWORLD, SphereMap.longitude(x, width),
                SphereMap.latitude(z, width));
        player.teleportTo(overworld, x, SpaceClimbs.ceiling(overworld) + OVER, z, 0, 0);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(player.level() == space(helper),
                        "over the ceiling the player passes into the Solar System's space; in "
                                + player.level().dimension().location()))
                .thenExecute(() -> {
                    helper.assertTrue(Math.abs(player.getX() - spot[0]) < CLOSE
                            && Math.abs(player.getZ() - spot[1]) < CLOSE && player.getY() >= SystemLayout.ORBIT_Y,
                            "over the spot their place is laid under: " + player.position() + " for "
                                    + spot[0] + ", " + spot[1]);
                    player.teleportTo(space(helper), player.getX(), SpaceClimbs.floor() - OVER, player.getZ(), 0,
                            0);
                })
                .thenWaitUntil(() -> helper.assertTrue(player.level() == overworld,
                        "under the floor of the low orbits the player passes back into the Overworld"))
                .thenExecute(() -> {
                    helper.assertTrue(Math.abs(player.getX() - x) < CLOSE && Math.abs(player.getZ() - z) < CLOSE
                            && player.getY() < SpaceClimbs.ceiling(overworld)
                            && player.getY() > overworld.getMaxBuildHeight(),
                            "over the very place they left, high over the ground: " + player.position());
                    GameTestPlayers.leave(player);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = "jsspace_climb", timeoutTicks = 6000)
    public static void climb_takesAMovingStructureUpWhole(final GameTestHelper helper) {
        final ServerLevel overworld = helper.getLevel();
        helper.setBlock(CRAFT, Blocks.IRON_BLOCK);
        final StructureResult made = MovingStructures.assemble(overworld, helper.absolutePos(CRAFT),
                helper.absolutePos(CRAFT));
        helper.assertTrue(made.ok(), "the craft is made");
        final MovingStructure craft = made.structure();
        final BlockPos base = helper.absolutePos(CRAFT);
        final double x = base.getX() + 0.5;
        final double z = base.getZ() + 0.5;
        final double width = overworld.getWorldBorder().getSize();
        final double[] spot = SystemLayout.orbitSpot(SolarBody.OVERWORLD, SphereMap.longitude(x, width),
                SphereMap.latitude(z, width));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(craft.ready(), "the craft is ready"))
                .thenExecute(() -> helper.assertTrue(MovingStructures.travel(craft, overworld,
                        new Vec3(x, SpaceClimbs.ceiling(overworld) + OVER, z)).ok(), "the craft climbs"))
                .thenWaitUntil(() -> {
                    final MovingStructure arrived = MovingStructures.byId(space(helper), craft.id());
                    helper.assertTrue(arrived != null && arrived.ready(),
                            "over the ceiling the craft passes into the Solar System's space");
                })
                .thenExecute(() -> {
                    final MovingStructure arrived = MovingStructures.byId(space(helper), craft.id());
                    helper.assertTrue(arrived.blocks() == 1 && Math.abs(arrived.pose().x() - spot[0]) < 1
                            && Math.abs(arrived.pose().z() - spot[1]) < 1,
                            "whole, over the spot its place is laid under: " + arrived.pose());
                    MovingStructures.takeAway(arrived);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = "jsspace_climb")
    public static void farMap_isTheGroundAsTheGeneratorMakesIt(final GameTestHelper helper) {
        final BlockPos base = helper.absolutePos(BlockPos.ZERO);
        // The test level is the game's flat land, all plains, its ground the top of its layers.
        final FlatLevelSource flat = (FlatLevelSource) helper.getLevel().getChunkSource().getGenerator();
        final int ground = helper.getLevel().getMinBuildHeight() + flat.settings().getLayers().size() - 1;
        for (int ring = 0; ring < FarRings.BLOCKS.size(); ring++) {
            final int blocks = FarRings.BLOCKS.get(ring);
            final FarMapPayload map = FarMaps.draw(helper.getLevel(), ring, base.getX(), base.getZ());
            helper.assertTrue(map.ring() == ring && map.size() == FarRings.SIZE && map.blocks() == blocks
                    && map.sea() == flat.getSeaLevel()
                    && map.pixels().length == FarRings.SIZE * FarRings.SIZE
                    && map.heights().length == FarRings.SIZE * FarRings.SIZE, "ring " + ring + " of its size");
            helper.assertTrue(Math.abs(map.centreX() - base.getX()) < blocks
                    && Math.abs(map.centreZ() - base.getZ()) < blocks, "round where it was asked");
            boolean known = true;
            boolean level = true;
            for (int i = 0; i < map.pixels().length; i++) {
                known &= (map.pixels()[i] & 0xFF) < map.biomes().size();
                level &= Math.abs(FarMapPayload.blocks(map.heights()[i]) - ground) <= FarMapPayload.HEIGHT_STEP;
            }
            helper.assertTrue(known && map.biomes().contains(Biomes.PLAINS.location()),
                    "every pixel one of its biomes, the plains: " + map.biomes());
            helper.assertTrue(level, "the ground at its height, " + ground + ", everywhere");
        }
        helper.succeed();
    }

    private static ServerLevel space(final GameTestHelper helper) {
        return SystemSpaces.spaceOf(helper.getLevel().getServer(), StarSystems.SOLAR_SYSTEM);
    }
}
