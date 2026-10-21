/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.dimension.RuntimeDimensions;
import dev.jstech.core.dimension.Wind;
import dev.jstech.core.dimension.Winds;
import dev.jstech.core.projectile.Projectiles;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestEntities;
import dev.jstech.tests.TestWorldGen;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Core's projectiles by real ballistics, through the test mod's slug: the air slows it more than the game's own
 * drag slows a bolt; it gets through glass and hits what is behind, but stops in a plank; the wind carries it; and it
 * falls by the pull of the dimension it flies in, a sixth as hard on the test moon.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ProjectileGameTests {

    private static final String ARENA = "empty";
    private static final String LONG = "bench";
    private static final String BATCH = "jscore_projectiles";
    private static final AtomicBoolean WIND_SOURCED = new AtomicBoolean();
    /*
     * The moon the slug falls on: a test server's flat world leaves the datapacks' dimensions out, so a copy of the
     * declared moon made while the game runs, the one the dimension tests stand on.
     */
    private static final ResourceKey<Level> MOON = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "test_moon_here"));

    /* The box of the world the test's wind blows in, east at twenty metres a second; none while no test blows. */
    private static volatile AABB windy;

    private ProjectileGameTests() {
    }

    @GameTest(template = LONG, batch = BATCH, timeoutTicks = 100)
    public static void slug_isSlowedByTheAirMoreThanABolt(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final Vec3 ahead = new Vec3(0, 0, 1);
        final TestEntities.Bolt bolt = Projectiles.fire(level, null, TestEntities.BOLT.get(),
                helper.absoluteVec(new Vec3(10.5, 5, 2)), ahead, 2.0F);
        final TestEntities.Slug slug = Projectiles.fire(level, null, TestEntities.SLUG.get(),
                helper.absoluteVec(new Vec3(30.5, 5, 2)), ahead, 2.0F);
        helper.runAfterDelay(8, () -> {
            final double boltSpeed = bolt.getDeltaMovement().length();
            final double slugSpeed = slug.getDeltaMovement().horizontalDistance();
            helper.assertTrue(slugSpeed < boltSpeed * 0.8, "the air holds the slug back: it goes " + slugSpeed
                    + " blocks a tick against the bolt's " + boltSpeed);
            bolt.discard();
            slug.discard();
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void slug_getsThroughGlassButNotAPlank(final GameTestHelper helper) {
        helper.setBlock(new BlockPos(2, 2, 4), Blocks.GLASS);
        helper.setBlock(new BlockPos(6, 2, 4), Blocks.OAK_PLANKS);
        final Pig behindGlass = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(2, 2, 6));
        final Pig behindPlank = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(6, 2, 6));
        final float health = behindGlass.getHealth();
        final ServerLevel level = helper.getLevel();
        // At a pig's height, for the slug to meet the pig after the glass.
        final TestEntities.Slug throughGlass = Projectiles.fire(level, null, TestEntities.SLUG.get(),
                helper.absoluteVec(new Vec3(2.5, 2.6, 1.5)), new Vec3(0, 0, 1), 1.5F);
        final TestEntities.Slug atPlank = Projectiles.fire(level, null, TestEntities.SLUG.get(),
                helper.absoluteVec(new Vec3(6.5, 2.6, 1.5)), new Vec3(0, 0, 1), 1.5F);
        helper.succeedWhen(() -> {
            helper.assertTrue(behindGlass.getHealth() < health, "the slug through the glass hits the pig behind it");
            helper.assertTrue(atPlank.isRemoved() && behindPlank.getHealth() >= health,
                    "the one at the plank stops in it");
            helper.assertBlockPresent(Blocks.GLASS, new BlockPos(2, 2, 4));
            behindGlass.discard();
            behindPlank.discard();
            throughGlass.discard();
        });
    }

    @GameTest(template = LONG, batch = BATCH, timeoutTicks = 100)
    public static void slug_isCarriedByTheWind(final GameTestHelper helper) {
        windIn(new AABB(helper.absoluteVec(new Vec3(0, 0, 0)), helper.absoluteVec(new Vec3(48, 8, 48))));
        final Vec3 from = helper.absoluteVec(new Vec3(10.5, 6.5, 2));
        final TestEntities.Slug slug = Projectiles.fire(helper.getLevel(), null, TestEntities.SLUG.get(), from,
                new Vec3(0, 0, 1), 1.0F);
        helper.runAfterDelay(10, () -> {
            final double drift = slug.getX() - from.x;
            windy = null;
            helper.assertTrue(drift > 0.3, "a wind from the west carries it east: " + drift + " blocks");
            slug.discard();
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void slug_fallsByThePullOfItsDimension(final GameTestHelper helper) {
        final ServerLevel moon = RuntimeDimensions.getOrCreate(helper.getLevel().getServer(), MOON,
                TestWorldGen.MOON.id());
        final TestEntities.Slug here = TestEntities.SLUG.get().create(helper.getLevel());
        final TestEntities.Slug there = TestEntities.SLUG.get().create(moon);
        here.setPos(helper.absoluteVec(new Vec3(4, 4, 4)));
        there.setPos(new Vec3(0, 100, 0));
        helper.assertTrue(Math.abs(here.getGravity() - TestEntities.SLUG_SPEC.gravity()) < 1.0e-9,
                "in the overworld it falls as its spec says: " + here.getGravity());
        helper.assertTrue(Math.abs(there.getGravity() - TestEntities.SLUG_SPEC.gravity() * 0.16) < 1.0e-9,
                "on the moon, by its pull of 0.16: " + there.getGravity());
        here.discard();
        there.discard();
        helper.succeed();
    }

    /* The test's wind blows east within {@code box}, a source of it put in once for every test. */
    private static void windIn(final AABB box) {
        if (WIND_SOURCED.compareAndSet(false, true)) {
            Winds.source((level, at) -> {
                final AABB blowing = windy;
                return blowing != null && blowing.contains(at) ? Optional.of(new Wind(20.0, 0.0))
                        : Optional.empty();
            });
        }
        windy = box;
    }
}
