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
import dev.jstech.space.platform.LandingPlatform;
import dev.jstech.space.platform.LandingPlatformBlockEntity;
import dev.jstech.space.platform.LandingPlatforms;
import dev.jstech.tests.JsTests;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The landing platform: a slab of landing pads with its terminal in the middle forms at nine blocks and at fifteen,
 * three times the diameter of the stages it takes; formed, it is noted in the world's platforms with its owner, holds
 * one stage at a time and is found where its terminal stands; a pad taken away undoes it and lets it go.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class LandingPlatformGameTests {

    private static final String ARENA = "bench";
    private static final String BATCH = "jsspace_platforms";
    private static final BlockPos MIDDLE = new BlockPos(12, 1, 12);

    private LandingPlatformGameTests() {
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void platform_ofNineTakesStagesOfThreeAndHoldsOneAtATime(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final LandingPlatformBlockEntity terminal = build(helper, 9);
        final UUID id = terminal.id();
        final StructureUuid first = new StructureUuid(UUID.randomUUID());
        final StructureUuid second = new StructureUuid(UUID.randomUUID());
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(LandingPlatforms.get(server, id).isPresent(),
                "it forms and is noted")).thenExecute(() -> {
                    final LandingPlatforms.Entry entry = LandingPlatforms.get(server, id).orElseThrow();
                    helper.assertTrue(entry.side() == 9 && terminal.shownSide() == 9, "nine blocks wide");
                    helper.assertTrue(entry.takes(3) && !entry.takes(5), "it takes the stages of three, not five");
                    helper.assertTrue(entry.free(), "it holds no stage yet");
                    helper.assertTrue(LandingPlatforms.reserve(server, id, first), "a stage keeps it");
                    helper.assertTrue(!LandingPlatforms.reserve(server, id, second), "a second does not");
                    helper.assertTrue(LandingPlatforms.reserve(server, id, first), "the first still has it");
                    LandingPlatforms.release(server, id);
                    helper.assertTrue(LandingPlatforms.reserve(server, id, second), "freed, the second keeps it");
                    final Optional<Vec3> spot = LandingPlatforms.landingSpot(server,
                            LandingPlatforms.get(server, id).orElseThrow());
                    helper.assertTrue(spot.isPresent() && spot.get().equals(Vec3.atBottomCenterOf(helper
                            .absolutePos(MIDDLE.above()))), "a stage comes down on the middle of its top");
                    helper.destroyBlock(MIDDLE.east(4));
                }).thenWaitUntil(() -> helper.assertTrue(LandingPlatforms.get(server, id).isEmpty(),
                "a pad taken away undoes it")).thenExecute(() -> helper.assertTrue(terminal.shownSide() == 0,
                "and its target is no longer painted")).thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void platform_ofFifteenTakesStagesOfFive(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final LandingPlatformBlockEntity terminal = build(helper, 15);
        helper.succeedWhen(() -> {
            final Optional<LandingPlatforms.Entry> formed = LandingPlatforms.get(server, terminal.id());
            helper.assertTrue(formed.isPresent(), "it forms and is noted");
            final LandingPlatforms.Entry entry = formed.get();
            helper.assertTrue(entry.side() == 15, "fifteen blocks wide: " + entry.side());
            helper.assertTrue(entry.takes(5) && entry.takes(3) && !entry.takes(7), "it takes the stages of five");
        });
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void platform_withAPadMissingDoesNotForm(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final LandingPlatformBlockEntity terminal = build(helper, 9);
        helper.destroyBlock(MIDDLE.offset(-4, 0, -4));
        helper.startSequence().thenIdle(60).thenExecute(() -> helper.assertTrue(LandingPlatforms.get(server,
                terminal.id()).isEmpty(), "a corner missing: not formed")).thenExecute(() -> helper.setBlock(
                MIDDLE.offset(-4, 0, -4), LandingPlatform.PAD.get())).thenWaitUntil(() -> helper.assertTrue(
                LandingPlatforms.get(server, terminal.id()).isPresent(), "the corner laid, it forms")).thenSucceed();
    }

    /* Lays a platform {@code side} blocks wide round the middle of the arena, its terminal owned and its team's. */
    private static LandingPlatformBlockEntity build(final GameTestHelper helper, final int side) {
        helper.setBlock(MIDDLE, LandingPlatform.TERMINAL.get());
        final LandingPlatformBlockEntity terminal = helper.getBlockEntity(MIDDLE);
        terminal.own(Ownership.of(UUID.randomUUID()).withAccess(Access.TEAM));
        final int half = side / 2;
        for (int dx = -half; dx <= half; dx++) {
            for (int dz = -half; dz <= half; dz++) {
                if (dx != 0 || dz != 0) {
                    helper.setBlock(MIDDLE.offset(dx, 0, dz), LandingPlatform.PAD.get());
                }
            }
        }
        return terminal;
    }
}
