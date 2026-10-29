/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import dev.jstech.core.team.Access;
import dev.jstech.core.team.Ownership;
import dev.jstech.core.uuid.StructureUuid;
import dev.jstech.space.api.RocketPart;
import dev.jstech.space.api.RocketParts;
import dev.jstech.space.platform.LandingPlatform;
import dev.jstech.space.platform.LandingPlatformBlockEntity;
import dev.jstech.space.platform.LandingPlatforms;
import dev.jstech.space.rocket.RocketStack;
import dev.jstech.space.rocket.RocketStages;
import dev.jstech.space.rocket.Rockets;
import dev.jstech.space.rocket.SkeletonRocketParts;
import dev.jstech.space.rocket.StageFate;
import dev.jstech.space.rocket.StageFates;
import dev.jstech.testkit.gametest.GameTestPlayers;
import dev.jstech.tests.JsTests;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Where a stage goes as it comes loose: onto the platform it was set to, when it has legs and an engine to come back
 * and the platform is free and wide enough, the platform then kept for it; into its fall zone; or anywhere in the
 * world already loaded, never where new land would be made.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class StageFateGameTests {

    private static final String ARENA = "bench";
    private static final String BATCH = "jsspace_stage_fates";
    private static final BlockPos MIDDLE = new BlockPos(12, 1, 12);

    private StageFateGameTests() {
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void stage_withLegsLandsOnItsFreePlatformAndASecondFalls(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final Map<ResourceLocation, RocketPart> parts = RocketParts.all(level);
        final LandingPlatformBlockEntity platform = platform(helper);
        final Vec3 over = Vec3.atCenterOf(helper.absolutePos(MIDDLE.above(6)));
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(LandingPlatforms.get(level.getServer(),
                platform.id()).isPresent(), "formed")).thenExecute(() -> {
            final Rockets.Rocket returning = rocket(legged(), new StageFate.Platform(platform.id()));
            helper.assertTrue(RocketStages.canComeBack(parts, returning.stack(), 1), "legs and an engine");
            final StructureUuid body = new StructureUuid(UUID.randomUUID());
            final StageFates.Outcome lands = StageFates.decide(level, parts, returning, 1, body, over, 1L);
            helper.assertTrue(lands instanceof StageFates.Outcome.Lands, "it lands: " + lands);
            helper.assertTrue(((StageFates.Outcome.Lands) lands).spot().equals(Vec3.atBottomCenterOf(helper
                    .absolutePos(MIDDLE.above()))), "on the middle of the platform");
            helper.assertTrue(LandingPlatforms.get(level.getServer(), platform.id()).orElseThrow().holding()
                    .orElseThrow().equals(body.value()), "which is kept for it");
            final StageFates.Outcome second = StageFates.decide(level, parts, returning, 1,
                    new StructureUuid(UUID.randomUUID()), over, 2L);
            helper.assertTrue(second instanceof StageFates.Outcome.Falls(var dimension, var column, var inZone)
                    && !inZone, "a second stage finds it taken and falls: " + second);
            LandingPlatforms.release(level.getServer(), platform.id());
        }).thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void stage_withoutLegsOrTooWideDoesNotLand(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final Map<ResourceLocation, RocketPart> parts = RocketParts.all(level);
        final LandingPlatformBlockEntity platform = platform(helper);
        final Vec3 over = Vec3.atCenterOf(helper.absolutePos(MIDDLE.above(6)));
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(LandingPlatforms.get(level.getServer(),
                platform.id()).isPresent(), "formed")).thenExecute(() -> {
            final RocketStack bare =RocketStack.of(part("sea_level_engine_3", 1), part("tank_3", 10),
                    part("separation_ring_3", 1), part("capsule_3", 1));
            helper.assertTrue(!RocketStages.canComeBack(parts, bare, 1), "no legs");
            helper.assertTrue(StageFates.decide(level, parts, rocket(bare, new StageFate.Platform(platform.id())),
                    1, new StructureUuid(UUID.randomUUID()), over, 3L) instanceof StageFates.Outcome.Falls,
                    "with no legs it falls");
            final RocketStack wide = RocketStack.of(part("sea_level_engine_5", 1), part("landing_legs_5", 1),
                    part("tank_5", 8), part("separation_ring_5", 1), part("capsule_5", 1));
            helper.assertTrue(RocketStages.canComeBack(parts, wide, 1) && RocketStages.diameter(parts, wide, 1) == 5,
                    "a stage of five with its legs");
            helper.assertTrue(StageFates.decide(level, parts, rocket(wide, new StageFate.Platform(platform.id())),
                    1, new StructureUuid(UUID.randomUUID()), over, 4L) instanceof StageFates.Outcome.Falls,
                    "too wide for a platform of nine, it falls");
            helper.assertTrue(LandingPlatforms.get(level.getServer(), platform.id()).orElseThrow().free(),
                    "and the platform is still free");
        }).thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void stage_setToAFallZoneFallsInIt(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final Map<ResourceLocation, RocketPart> parts = RocketParts.all(level);
        final BlockPos centre = helper.absolutePos(MIDDLE).offset(400, 0, -300);
        final StageFate.FallZone zone = new StageFate.FallZone(level.dimension(), centre, 60);
        for (long seed = 0; seed < 40; seed++) {
            final StageFates.Outcome outcome = StageFates.decide(level, parts, rocket(legged(), zone), 1,
                    new StructureUuid(UUID.randomUUID()), Vec3.ZERO, seed);
            helper.assertTrue(outcome instanceof StageFates.Outcome.Falls(var dimension, var column, var inZone)
                    && inZone && Math.hypot(column.getX() + 0.5 - centre.getX() - 0.5,
                    column.getZ() + 0.5 - centre.getZ() - 0.5) <= 61, "it falls within the zone: " + outcome);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void stage_withNothingSetFallsWhereTheWorldIsLoaded(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final Map<ResourceLocation, RocketPart> parts = RocketParts.all(level);
        final Vec3 over = Vec3.atCenterOf(helper.absolutePos(MIDDLE.above(6)));
        final ServerPlayer watcher = GameTestPlayers.join(level, UUID.randomUUID(), "stage_watcher");
        try {
            watcher.setPos(over.x, over.y, over.z);
            for (long seed = 0; seed < 20; seed++) {
                final StageFates.Outcome outcome = StageFates.decide(level, parts, rocket(legged(), null), 1,
                        new StructureUuid(UUID.randomUUID()), over, seed);
                helper.assertTrue(outcome instanceof StageFates.Outcome.Falls(var dimension, var column, var inZone)
                        && !inZone && level.getChunkSource().getChunkNow(column.getX() >> 4, column.getZ() >> 4)
                        != null, "it falls on land already made: " + outcome);
            }
        } finally {
            GameTestPlayers.leave(watcher);
        }
        helper.succeed();
    }

    /* A rocket of {@code stack} whose first stage goes as {@code fate} says, or nowhere set. */
    private static Rockets.Rocket rocket(final RocketStack stack, final StageFate fate) {
        return new Rockets.Rocket(stack, Ownership.of(UUID.randomUUID()), Map.of(),
                fate == null ? Map.of() : Map.of(1, fate), 0L);
    }

    /* A rocket of three whose first stage has legs. */
    private static RocketStack legged() {
        return RocketStack.of(part("sea_level_engine_3", 1), part("landing_legs_3", 1), part("tank_3", 10),
                part("separation_ring_3", 1), part("vacuum_engine_3", 1), part("tank_3", 4), part("capsule_3", 1));
    }

    private static RocketStack.Part part(final String path, final int layers) {
        return new RocketStack.Part(SkeletonRocketParts.id(path), layers);
    }

    /* A platform of nine in the middle of the arena, its team's. */
    private static LandingPlatformBlockEntity platform(final GameTestHelper helper) {
        helper.setBlock(MIDDLE, LandingPlatform.TERMINAL.get());
        final LandingPlatformBlockEntity terminal = helper.getBlockEntity(MIDDLE);
        terminal.own(Ownership.of(UUID.randomUUID()).withAccess(Access.TEAM));
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                if (dx != 0 || dz != 0) {
                    helper.setBlock(MIDDLE.offset(dx, 0, dz), LandingPlatform.PAD.get());
                }
            }
        }
        return terminal;
    }
}
