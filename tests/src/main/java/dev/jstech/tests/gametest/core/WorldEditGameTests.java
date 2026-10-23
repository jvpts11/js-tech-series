/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
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
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Large edits of the world: planned away from the server's thread from a snapshot of what they read, set a share of
 * each tick until done; stopped where they are; a planning that fails setting nothing; an undoable edit undone, a
 * chest's contents and all; a box saved as a structure file and set again elsewhere, turned; and the same from the
 * chat, through {@code /jstech edit}. Each edit works far out in the test world's sky, at a place of its own each run,
 * but the chat's, which works in its test's own place.
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
    private static final String COMMAND_FILE = JsTests.MODID + ":edit_command_test";

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

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void undo_putsBackWhatTheEditReplacedAChestAndAll(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos chest = farCorner();
        final BlockPos stone = chest.east();
        level.setBlockAndUpdate(chest, Blocks.CHEST.defaultBlockState());
        ((ChestBlockEntity) level.getBlockEntity(chest)).setItem(0, new ItemStack(Items.DIAMOND, 3));
        level.setBlockAndUpdate(stone, Blocks.STONE.defaultBlockState());
        final UUID player = UUID.randomUUID();
        final WorldEdit edit = WorldEdits.applyFor(level, player, EditPlan.builder()
                .set(chest, Blocks.GLASS.defaultBlockState()).set(stone, Blocks.GLASS.defaultBlockState())
                .set(stone, Blocks.DIRT.defaultBlockState()).undoable().build()).orElseThrow();
        helper.assertTrue(edit.undo().isEmpty(), "an edit under way cannot be undone yet");
        final WorldEdit plain = WorldEdits.apply(level, EditPlan.builder().set(chest.above(5),
                Blocks.GLASS.defaultBlockState()).build());
        final AtomicReference<WorldEdit> undoing = new AtomicReference<>();
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(edit.over() && plain.over(), "the edits are not over"))
                .thenExecute(() -> {
                    helper.assertTrue(level.getBlockState(chest).is(Blocks.GLASS)
                            && level.getBlockState(stone).is(Blocks.DIRT), "the edit set its blocks, the later wins");
                    helper.assertTrue(plain.undo().isEmpty(), "an edit not made undoable keeps nothing to undo");
                    helper.assertTrue(WorldEdits.lastFor(player).orElseThrow() == edit, "it is their last edit");
                    undoing.set(WorldEdits.applyFor(level, player, edit.undo().orElseThrow()).orElseThrow());
                })
                .thenWaitUntil(() -> helper.assertTrue(undoing.get().over(), "the undo is not over"))
                .thenExecute(() -> {
                    helper.assertTrue(level.getBlockState(stone).is(Blocks.STONE), "the stone is back");
                    helper.assertTrue(level.getBlockEntity(chest) instanceof ChestBlockEntity held
                            && held.getItem(0).is(Items.DIAMOND) && held.getItem(0).getCount() == 3,
                            "and the chest with its diamonds");
                    helper.assertTrue(WorldEdits.lastFor(player).orElseThrow() == undoing.get()
                            && undoing.get().undo().isPresent(), "the undo is their last edit, undone in turn");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void exportThenImport_setsTheBlocksTurnedWithWhatTheyHeld(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos from = farCorner();
        level.setBlockAndUpdate(from, Blocks.CHEST.defaultBlockState());
        ((ChestBlockEntity) level.getBlockEntity(from)).setItem(0, new ItemStack(Items.EMERALD));
        level.setBlockAndUpdate(from.east(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(from.above(), Blocks.OAK_STAIRS.defaultBlockState()
                .setValue(StairBlock.FACING, Direction.NORTH));
        final ResourceLocation id = ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "edit_export_test");
        helper.assertTrue(WorldEdits.export(level, BoundingBox.fromCorners(from, from.offset(1, 1, 0)), id),
                "the box is saved as a structure file");
        helper.assertTrue(WorldEdits.planFrom(level, ResourceLocation.fromNamespaceAndPath(JsTests.MODID,
                "no_such_file"), from, Rotation.NONE).isEmpty(), "a file that is not there plans nothing");
        final BlockPos to = from.offset(0, 0, 8);
        final EditPlan plan = WorldEdits.planFrom(level, id, to, Rotation.CLOCKWISE_90).orElseThrow().build();
        helper.assertTrue(plan.size() == 4, "its four blocks, the air among them: " + plan.size());
        final WorldEdit edit = WorldEdits.apply(level, plan);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(edit.over(), "the import is not over"))
                .thenExecute(() -> {
                    helper.assertTrue(level.getBlockState(to.south()).is(Blocks.STONE),
                            "turned a quarter clockwise, what lay east lies south");
                    helper.assertTrue(level.getBlockState(to.above()).getValue(StairBlock.FACING) == Direction.EAST,
                            "and the stair that faced north faces east");
                    helper.assertTrue(level.getBlockEntity(to) instanceof ChestBlockEntity held
                            && held.getItem(0).is(Items.EMERALD), "the chest holds its emerald");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void editCommands_exportImportAndUndoFromTheChat(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos from = helper.absolutePos(new BlockPos(1, 2, 1));
        final BlockPos to = helper.absolutePos(new BlockPos(1, 2, 3));
        level.setBlockAndUpdate(from, Blocks.CHEST.defaultBlockState());
        ((ChestBlockEntity) level.getBlockEntity(from)).setItem(0, new ItemStack(Items.EMERALD));
        level.setBlockAndUpdate(from.east(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(to, Blocks.DIRT.defaultBlockState());
        helper.assertTrue(edit(helper, "export " + COMMAND_FILE + " " + at(from) + " " + at(from.east())) == 1,
                "the box is saved as a structure file from the chat");
        helper.assertTrue(edit(helper, "import " + JsTests.MODID + ":no_such_file " + at(to)) == 0,
                "a file that is not there is not set");
        helper.assertTrue(edit(helper, "import " + COMMAND_FILE + " " + at(to)) == 1, "the file is set where it says");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(console().over(), "the import is not over"))
                .thenExecute(() -> {
                    helper.assertTrue(level.getBlockEntity(to) instanceof ChestBlockEntity held
                            && held.getItem(0).is(Items.EMERALD) && level.getBlockState(to.east()).is(Blocks.STONE),
                            "the chest with its emerald and the stone are set there");
                    helper.assertTrue(edit(helper, "undo") == 1, "and the import is undone");
                })
                .thenWaitUntil(() -> helper.assertTrue(console().over(), "the undo is not over"))
                .thenExecute(() -> helper.assertTrue(level.getBlockState(to).is(Blocks.DIRT)
                        && level.getBlockState(to.east()).isAir(), "what the import replaced is back"))
                .thenSucceed();
    }

    /* Runs {@code /jstech edit} with {@code arguments} as the console does; what it gives back. */
    private static int edit(final GameTestHelper helper, final String arguments) {
        final MinecraftServer server = helper.getLevel().getServer();
        final CommandSourceStack console = server.createCommandSourceStack().withLevel(helper.getLevel())
                .withSuppressedOutput();
        try {
            return server.getCommands().getDispatcher().execute("jstech edit " + arguments, console);
        } catch (final CommandSyntaxException wrong) {
            throw new GameTestAssertException("/jstech edit " + arguments + " does not read: " + wrong.getMessage());
        }
    }

    /* The console's last edit: the command's edits are the console's when the console gives them. */
    private static WorldEdit console() {
        return WorldEdits.lastFor(Util.NIL_UUID).orElseThrow();
    }

    private static String at(final BlockPos pos) {
        return pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }

    /* A corner far out in the sky, at a place of its own each run, its chunks never touched before. */
    private static BlockPos farCorner() {
        final int offset = Math.floorMod(UUID.randomUUID().hashCode(), SPREAD) & ~15;
        return new BlockPos(FAR + offset, SKY, FAR - offset);
    }
}
