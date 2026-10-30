/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import dev.jstech.space.hangar.HangarBay;
import dev.jstech.space.hangar.HangarBlocks;
import dev.jstech.space.hangar.HangarPieceBlock;
import dev.jstech.space.hangar.HangarTerminalBlockEntity;
import dev.jstech.space.hangar.HangarTexts;
import dev.jstech.tests.JsTests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Rocket Hangar, its walls and roof of plain stone: from its terminal it finds its bay, seven by seven and twelve
 * high, and forms once its crane rails run along the tops of two facing walls, its bridge spans them, its hoist hangs
 * under the bridge and a bay door stands on the floor; it says what is missing until then, the crane's pieces taking
 * their own shapes as it forms, their rails looking into the bay and their bridge along its span, and giving them back
 * as a piece of it goes or its wall is opened.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class RocketHangarGameTests {

    private static final String ARENA = "tall";
    private static final String BATCH = "jsspace_hangar";
    /* The bay inside the walls: from (2, 2, 2) to (8, 13, 8); the walls round it, the floor at 1, the roof at 14. */
    private static final int LOW = 2;
    private static final int HIGH_XZ = 8;
    private static final int TOP = 13;
    private static final BlockPos TERMINAL = new BlockPos(5, 3, 9);
    private static final BlockPos DOOR = new BlockPos(1, 2, 5);
    private static final BlockPos HOIST = new BlockPos(5, 12, 5);
    private static final BlockPos BRIDGE = new BlockPos(5, TOP, 5);
    private static final BlockPos RAIL = new BlockPos(3, TOP - 1, LOW);
    private static final BlockPos WALL = new BlockPos(9, 6, 5);

    private RocketHangarGameTests() {
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void hangar_formsWithItsCraneAndDoorAndSaysWhatIsMissing(final GameTestHelper helper) {
        build(helper);
        final HangarTerminalBlockEntity terminal = helper.getBlockEntity(TERMINAL);
        terminal.look();
        helper.assertTrue(terminal.bay().isEmpty() && says(terminal, HangarTexts.NO_DOOR),
                "without a door it is not formed: " + GameText.resolve(terminal.reason()));
        helper.setBlock(DOOR, HangarBlocks.BAY_DOOR.get());
        terminal.look();
        final HangarBay bay = terminal.bay().orElse(null);
        helper.assertTrue(bay != null && bay.width() == 7 && bay.depth() == 7 && bay.height() == 12,
                "with its door it forms, a bay seven by seven and twelve high: " + bay + ", "
                        + GameText.resolve(terminal.reason()));
        final BlockState rail = helper.getBlockState(RAIL);
        helper.assertTrue(rail.getValue(HangarPieceBlock.FORMED)
                && rail.getValue(HorizontalDirectionalBlock.FACING) == Direction.SOUTH,
                "its north rail takes its shape, looking into the bay: " + rail);
        final BlockState bridge = helper.getBlockState(BRIDGE);
        helper.assertTrue(bridge.getValue(HangarPieceBlock.FORMED)
                && bridge.getValue(HorizontalDirectionalBlock.FACING).getAxis() == Direction.Axis.Z,
                "its bridge, along its span from rail to rail: " + bridge);
        helper.assertTrue(helper.getBlockState(HOIST).getValue(HangarPieceBlock.FORMED), "its hoist too");
        helper.setBlock(HOIST, Blocks.AIR);
        terminal.look();
        helper.assertTrue(terminal.bay().isEmpty() && says(terminal, HangarTexts.NO_HOIST)
                && !helper.getBlockState(RAIL).getValue(HangarPieceBlock.FORMED),
                "its hoist gone, it is undone and its pieces are the kit's again: "
                        + GameText.resolve(terminal.reason()));
        helper.setBlock(HOIST, HangarBlocks.CRANE_HOIST.get());
        helper.setBlock(WALL, Blocks.AIR);
        terminal.look();
        helper.assertTrue(terminal.bay().isEmpty() && says(terminal, HangarTexts.OPEN),
                "a wall opened, it is not formed: " + GameText.resolve(terminal.reason()));
        helper.setBlock(WALL, Blocks.STONE);
        terminal.look();
        helper.assertTrue(terminal.bay().isPresent(), "the wall closed, it forms again");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void hangar_withoutItsCraneSaysSo(final GameTestHelper helper) {
        build(helper);
        for (int x = LOW; x <= HIGH_XZ; x++) {
            helper.setBlock(new BlockPos(x, TOP - 1, LOW), Blocks.AIR);
        }
        final HangarTerminalBlockEntity terminal = helper.getBlockEntity(TERMINAL);
        terminal.look();
        helper.assertTrue(terminal.bay().isEmpty() && says(terminal, HangarTexts.NO_RAILS),
                "one wall's rail gone, no crane: " + GameText.resolve(terminal.reason()));
        helper.setBlock(TERMINAL, Blocks.STONE);
        helper.setBlock(new BlockPos(5, 3, 1), HangarBlocks.TERMINAL.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        final HangarTerminalBlockEntity other = helper.getBlockEntity(new BlockPos(5, 3, 1));
        other.look();
        helper.assertTrue(other.bay().isEmpty() && says(other, HangarTexts.NO_RAILS),
                "a terminal in the far wall finds the same bay, and the same rail missing: "
                        + GameText.resolve(other.reason()));
        helper.succeed();
    }

    /*
     * The hangar of plain stone round the bay, its terminal in the south wall looking north, the rails along the
     * north and south walls at the top, the bridge across from one to the other, the hoist under it; no door yet.
     */
    private static void build(final GameTestHelper helper) {
        for (int x = LOW - 1; x <= HIGH_XZ + 1; x++) {
            for (int z = LOW - 1; z <= HIGH_XZ + 1; z++) {
                for (int y = LOW - 1; y <= TOP + 1; y++) {
                    final boolean shell = x < LOW || x > HIGH_XZ || z < LOW || z > HIGH_XZ || y < LOW || y > TOP;
                    helper.setBlock(new BlockPos(x, y, z), shell ? Blocks.STONE : Blocks.AIR);
                }
            }
        }
        helper.setBlock(TERMINAL, HangarBlocks.TERMINAL.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        for (int x = LOW; x <= HIGH_XZ; x++) {
            helper.setBlock(new BlockPos(x, TOP - 1, LOW), HangarBlocks.CRANE_RAIL.get());
            helper.setBlock(new BlockPos(x, TOP - 1, HIGH_XZ), HangarBlocks.CRANE_RAIL.get());
        }
        for (int z = LOW; z <= HIGH_XZ; z++) {
            helper.setBlock(new BlockPos(5, TOP, z), HangarBlocks.CRANE_BRIDGE.get());
        }
        helper.setBlock(HOIST, HangarBlocks.CRANE_HOIST.get());
    }

    /* Whether the terminal says the sentence {@code key}, whatever is put into it. */
    private static boolean says(final HangarTerminalBlockEntity terminal, final TextKey key) {
        return terminal.reason() instanceof Text.Translated said && said.key().equals(key);
    }
}
