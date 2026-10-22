/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.protection.PlayerLimits;
import dev.jstech.core.protection.ProtectionBalance;
import dev.jstech.core.text.Text;
import dev.jstech.core.world.edit.EditPlan;
import dev.jstech.core.world.edit.RegionSnapshot;
import dev.jstech.core.world.edit.WorldEdit;
import dev.jstech.core.world.edit.WorldEdits;
import dev.jstech.tests.JsTests;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Large edits of the world: planned away from the server's thread from a snapshot of what they read, set a share of
 * each tick until done; stopped where they are; and a planning that fails setting nothing. Each edit works far out in
 * the test world's sky, at a place of its own each run.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class WorldEditGameTests {

    private static final String ARENA = "empty";
    private static final int FAR = 2_000_000;
    private static final int SPREAD = 1_000_000;
    private static final int SKY = 200;
    private static final int WIDE = 47;
    private static final int HIGH = 7;

    private WorldEditGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 600)
    public static void edit_isPlannedAwayAndSetAShareOfEachTick(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos corner = farCorner();
        final BoundingBox box = BoundingBox.fromCorners(corner, corner.offset(WIDE, HIGH, WIDE));
        level.setBlockAndUpdate(corner, Blocks.STONE.defaultBlockState());
        final RegionSnapshot before = RegionSnapshot.of(level, box);
        helper.assertTrue(before.get(corner.getX(), corner.getY(), corner.getZ()).is(Blocks.STONE)
                && before.get(corner.getX() + 1, corner.getY(), corner.getZ()).isAir()
                && before.get(corner.getX() - 1, corner.getY(), corner.getZ()).isAir(),
                "a snapshot holds the box's blocks, and air outside it");
        final WorldEdit edit = WorldEdits.plan(level, box, blocks -> {
            final EditPlan.Builder plan = EditPlan.builder();
            for (final BlockPos at : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(),
                    box.maxY(), box.maxZ())) {
                if (blocks.get(at.getX(), at.getY(), at.getZ()).isAir()) {
                    plan.set(at, Blocks.GLASS.defaultBlockState());
                }
            }
            return plan.build();
        });
        helper.assertTrue(edit.state() == WorldEdit.State.PLANNING, "it is planned away from the server's thread");
        final AtomicBoolean partway = new AtomicBoolean();
        final AtomicInteger heard = new AtomicInteger();
        edit.whenOver(over -> heard.incrementAndGet());
        helper.onEachTick(() -> partway.compareAndSet(false, edit.state() == WorldEdit.State.SETTING
                && edit.progress() > 0 && edit.progress() < 1));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(edit.over(), "the edit is not over yet"))
                .thenExecute(() -> {
                    final int expected = (WIDE + 1) * (HIGH + 1) * (WIDE + 1) - 1;
                    helper.assertTrue(edit.state() == WorldEdit.State.DONE && edit.total() == expected
                            && edit.set() == expected && edit.progress() == 1, "every block planned is set, "
                            + edit.set() + " of " + edit.total());
                    helper.assertTrue(level.getBlockState(corner).is(Blocks.STONE),
                            "what the plan read as filled is left");
                    helper.assertTrue(level.getBlockState(corner.offset(WIDE, HIGH, WIDE)).is(Blocks.GLASS),
                            "and the rest is set");
                    helper.assertTrue(partway.get(), "over several ticks, a share each");
                    helper.assertTrue(heard.get() == 1 && !WorldEdits.underWay().contains(edit),
                            "and it says so once, done");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void edit_stopsWhereItIsAndAFailedPlanSetsNothing(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos corner = farCorner();
        final EditPlan.Builder big = EditPlan.builder();
        for (final BlockPos at : BlockPos.betweenClosed(corner, corner.offset(WIDE, HIGH, WIDE))) {
            big.set(at, Blocks.GLASS.defaultBlockState());
        }
        final WorldEdit stopped = WorldEdits.apply(level, big.build());
        stopped.stop();
        helper.assertTrue(stopped.state() == WorldEdit.State.STOPPED && stopped.over(), "an edit stops at once");
        final BoundingBox box = BoundingBox.fromCorners(corner, corner.offset(1, 1, 1));
        final WorldEdit failing = WorldEdits.plan(level, box, blocks -> {
            throw new IllegalStateException("a planner that fails");
        });
        final WorldEdit empty = WorldEdits.apply(level, EditPlan.builder().build());
        helper.assertTrue(empty.state() == WorldEdit.State.DONE, "an empty plan is done at once");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(failing.over(), "the failing plan has not ended"))
                .thenExecute(() -> {
                    helper.assertTrue(failing.state() == WorldEdit.State.FAILED && failing.set() == 0,
                            "a plan that fails sets nothing");
                    helper.assertTrue(stopped.set() == 0 && level.getBlockState(corner).isAir(),
                            "and the stopped edit set nothing more");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void editsForAPlayer_stopAtTheirLimitAndComeBackAsOneEnds(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final UUID player = UUID.randomUUID();
        final int most = ProtectionBalance.editsPerPlayer();
        ProtectionBalance.setEditsPerPlayer(1);
        try {
            final EditPlan.Builder one = EditPlan.builder();
            one.set(farCorner(), Blocks.GLASS.defaultBlockState());
            final EditPlan plan = one.build();
            final Optional<WorldEdit> first = WorldEdits.applyFor(level, player, plan);
            helper.assertTrue(first.isPresent() && first.get().player() == player
                    && WorldEdits.underWayFor(player) == 1, "the first is under way, counted for them");
            helper.assertTrue(WorldEdits.applyFor(level, player, plan).isEmpty()
                    && WorldEdits.underWayFor(player) == 1, "a second goes past their limit of one");
            helper.assertTrue(PlayerLimits.reached(PlayerLimits.LARGE_EDITS, 1) instanceof Text.Translated said
                    && said.key() == PlayerLimits.REACHED, "and they are told so");
            first.get().stop();
            helper.assertTrue(WorldEdits.underWayFor(player) == 0, "stopped, it no longer counts");
            final Optional<WorldEdit> again = WorldEdits.applyFor(level, player, plan);
            helper.assertTrue(again.isPresent(), "so another may start");
            again.get().stop();
            final WorldEdit nobodys = WorldEdits.apply(level, plan);
            helper.assertTrue(nobodys.player() == null && WorldEdits.underWayFor(player) == 0,
                    "an edit made for nobody counts for nobody");
            nobodys.stop();
        } finally {
            ProtectionBalance.setEditsPerPlayer(most);
        }
        helper.succeed();
    }

    /* A corner far out in the sky, at a place of its own each run, its chunks never touched before. */
    private static BlockPos farCorner() {
        final int offset = Math.floorMod(UUID.randomUUID().hashCode(), SPREAD) & ~15;
        return new BlockPos(FAR + offset, SKY, FAR - offset);
    }
}
