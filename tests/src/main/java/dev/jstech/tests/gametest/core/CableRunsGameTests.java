/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import com.mojang.authlib.GameProfile;
import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.cable.CableRuns;
import dev.jstech.core.jstechseries.lines.SharedPipes;
import dev.jstech.tests.JsTests;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A run of cable dragged at once: it is laid along its way, each block joined to the one before; it stops at the first
 * block something is in the way of, or where no cable is left in hand; and a run that does not start within the
 * player's reach is not laid at all.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class CableRunsGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos FROM = new BlockPos(1, 2, 1);
    private static final BlockPos TO = new BlockPos(4, 2, 3);
    private static final int STACK = 64;

    private CableRunsGameTests() {
    }

    @GameTest(template = ARENA)
    public static void run_laysEveryBlockAlongItsWay(final GameTestHelper helper) {
        final FakePlayer player = player(helper, "run_whole", STACK);
        final int laid = CableRuns.lay(player, helper.getLevel(), helper.absolutePos(FROM), helper.absolutePos(TO),
                InteractionHand.MAIN_HAND);
        final List<BlockPos> path = CableRuns.path(FROM, TO, false);
        helper.assertTrue(laid == path.size() && path.size() == 6, "the whole run is laid: " + laid);
        for (final BlockPos at : path) {
            helper.assertTrue(helper.getBlockEntity(at) instanceof CableBlockEntity cable
                    && cable.holds(SharedPipes.COPPER_PIPE.get()), "a pipe at " + at);
        }
        helper.assertTrue(player.getMainHandItem().getCount() == STACK - path.size(), "a pipe spent on each block");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void run_stopsAtWhatIsInTheWay(final GameTestHelper helper) {
        helper.setBlock(new BlockPos(3, 2, 1), Blocks.STONE);
        final FakePlayer player = player(helper, "run_blocked", STACK);
        final int laid = CableRuns.lay(player, helper.getLevel(), helper.absolutePos(FROM), helper.absolutePos(TO),
                InteractionHand.MAIN_HAND);
        helper.assertTrue(laid == 2, "the run stops before the stone: " + laid);
        helper.assertTrue(helper.getBlockState(new BlockPos(3, 2, 1)).is(Blocks.STONE), "which stays");
        helper.assertTrue(!(helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(4, 2, 1)))
                instanceof CableBlockEntity), "and nothing is laid past it");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void run_stopsWhenNoCableIsLeft(final GameTestHelper helper) {
        final FakePlayer player = player(helper, "run_short", 3);
        final int laid = CableRuns.lay(player, helper.getLevel(), helper.absolutePos(FROM), helper.absolutePos(TO),
                InteractionHand.MAIN_HAND);
        helper.assertTrue(laid == 3 && player.getMainHandItem().isEmpty(), "three pipes lay three blocks: " + laid);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void run_isNotLaidOutOfReach(final GameTestHelper helper) {
        final FakePlayer player = player(helper, "run_far", STACK);
        final BlockPos far = helper.absolutePos(FROM).offset(0, 0, 30);
        player.moveTo(far.getX() + 0.5, far.getY(), far.getZ() + 0.5);
        final int laid = CableRuns.lay(player, helper.getLevel(), helper.absolutePos(FROM), helper.absolutePos(TO),
                InteractionHand.MAIN_HAND);
        helper.assertTrue(laid == 0, "a run out of reach is not laid: " + laid);
        helper.succeed();
    }

    /* A player standing by the run's start, holding {@code count} copper pipes. */
    private static FakePlayer player(final GameTestHelper helper, final String name, final int count) {
        final FakePlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.nameUUIDFromBytes(
                name.getBytes(StandardCharsets.UTF_8)), name));
        final BlockPos start = helper.absolutePos(FROM);
        player.moveTo(start.getX() + 0.5, start.getY(), start.getZ() - 1.5);
        player.setItemInHand(InteractionHand.MAIN_HAND, SharedPipes.COPPER_PIPE.stack(count));
        return player;
    }
}
