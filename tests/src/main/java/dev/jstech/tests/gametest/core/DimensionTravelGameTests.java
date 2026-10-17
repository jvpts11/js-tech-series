/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.dimension.DimensionTravel;
import dev.jstech.core.dimension.RuntimeDimensions;
import dev.jstech.core.dimension.SafeLanding;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestWorldGen;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Travelling between dimensions: a pig in a minecart goes with the minecart and arrives in it, at a safe place near
 * where it was sent; a safe place steps aside from lava and up out of a wall, and over no ground at all a platform is
 * laid to stand on.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class DimensionTravelGameTests {

    private static final String ARENA = "empty";
    /* A traveller's height, a player's. */
    private static final double TALL = 1.8;

    private DimensionTravelGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void travel_takesWhatItRidesAndWhoRidesIt(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(
                JsTests.MODID, "travel_moon_" + UUID.randomUUID().toString().substring(0, 8)));
        final ServerLevel moon = RuntimeDimensions.getOrCreate(server, key, TestWorldGen.MOON.id());
        final AbstractMinecart cart = helper.spawn(EntityType.MINECART, new BlockPos(4, 2, 4));
        final Pig pig = helper.spawn(EntityType.PIG, new BlockPos(4, 2, 4));
        pig.startRiding(cart, true);
        final Entity arrived = DimensionTravel.travel(pig, moon, new BlockPos(0, 20, 0), null);
        try {
            helper.assertTrue(arrived instanceof Pig && arrived.level() == moon, "the pig arrives on the Moon");
            helper.assertTrue(arrived.getVehicle() instanceof AbstractMinecart minecart && minecart.level() == moon,
                    "still in its minecart, which went with it");
            final Entity vehicle = arrived.getVehicle();
            helper.assertTrue(SafeLanding.safe(moon, vehicle.blockPosition(), 2),
                    "on firm ground, with room above; " + vehicle.blockPosition());
            helper.assertTrue(vehicle.getY() < 20, "on the ground below where it was sent, not in the air above it");
            helper.assertTrue(pig.isRemoved() && cart.isRemoved(), "and gone from where it left");
        } finally {
            if (arrived != null) {
                if (arrived.getVehicle() != null) {
                    arrived.getVehicle().discard();
                }
                arrived.discard();
            }
            RuntimeDimensions.remove(server, key);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void safeLanding_stepsAsideFromLavaAndUpOutOfAWall(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        helper.setBlock(new BlockPos(4, 2, 4), Blocks.LAVA);
        final BlockPos besideLava = SafeLanding.find(level, helper.absolutePos(new BlockPos(4, 2, 4)), TALL);
        helper.assertTrue(!besideLava.equals(helper.absolutePos(new BlockPos(4, 2, 4)))
                && SafeLanding.safe(level, besideLava, 2), "not in the lava, but beside it; " + besideLava);
        helper.setBlock(new BlockPos(2, 2, 2), Blocks.STONE);
        helper.setBlock(new BlockPos(2, 3, 2), Blocks.STONE);
        final BlockPos onTheWall = SafeLanding.find(level, helper.absolutePos(new BlockPos(2, 2, 2)), TALL);
        helper.assertTrue(onTheWall.equals(helper.absolutePos(new BlockPos(2, 4, 2))),
                "out of the stone, standing on it; " + helper.relativePos(onTheWall));
        helper.setBlock(new BlockPos(4, 2, 4), Blocks.AIR);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void safeLanding_laysAPlatformWhereThereIsNoGround(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos high = helper.absolutePos(new BlockPos(4, 140, 4));
        final BlockPos feet = SafeLanding.find(level, high, TALL);
        try {
            helper.assertTrue(feet.equals(high), "where there is no ground, right where it was asked; " + feet);
            helper.assertTrue(level.getBlockState(feet.below()).is(Blocks.STONE)
                    && level.getBlockState(feet.below().east()).is(Blocks.STONE),
                    "on a platform laid under the traveller");
            helper.assertTrue(SafeLanding.safe(level, feet, 2), "which is safe to stand on");
        } finally {
            for (final BlockPos at : BlockPos.betweenClosed(feet.below().offset(-1, 0, -1),
                    feet.below().offset(1, 0, 1))) {
                level.setBlockAndUpdate(at, Blocks.AIR.defaultBlockState());
            }
        }
        helper.succeed();
    }
}
