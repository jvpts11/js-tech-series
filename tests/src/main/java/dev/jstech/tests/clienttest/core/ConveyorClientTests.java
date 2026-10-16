/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import dev.jstech.core.conveyor.BeltSlope;
import dev.jstech.core.conveyor.ConveyorBlock;
import dev.jstech.core.conveyor.ConveyorBlockEntity;
import dev.jstech.core.conveyor.ElevatorBlock;
import dev.jstech.tests.TestConveyors;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Conveyors as a player meets them, to set beside what they should look like: a line of belts rising a block and
 * falling again, its items lying flat on the flat belts and tipped up the slopes, moving between two pictures, with a
 * press holding cobblestone still under it; and a chute, a bucket elevator, a splitter, a join and a diverter, each
 * with its items on it.
 */
public final class ConveyorClientTests {

    private static final BlockPos STAND = new BlockPos(0, 3, -3);
    private static final int ROW = 1;
    private static final int WAIT = 200;
    private static final int MOVED = 6;

    private ConveyorClientTests() {
    }

    @ClientTest(timeoutTicks = 600)
    public static void belts_drawTheirItemsAlongFlatsAndSlopes(final ClientTestContext ctx) {
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> {
                    clear(ctx, level);
                    belt(ctx, level, new BlockPos(-3, 2, ROW), BeltSlope.FLAT);
                    belt(ctx, level, new BlockPos(-2, 2, ROW), BeltSlope.RISING);
                    belt(ctx, level, new BlockPos(-1, 3, ROW), BeltSlope.FLAT);
                    belt(ctx, level, new BlockPos(0, 2, ROW), BeltSlope.FALLING);
                    belt(ctx, level, new BlockPos(1, 2, ROW), BeltSlope.FLAT);
                    belt(ctx, level, new BlockPos(2, 2, ROW), BeltSlope.FLAT);
                    level.setBlockAndUpdate(ctx.abs(new BlockPos(1, 3, ROW)),
                            TestConveyors.BELT_PRESS.get().defaultBlockState());
                    put(ctx, level, new BlockPos(-3, 2, ROW), Items.IRON_INGOT, Items.GOLD_INGOT);
                    put(ctx, level, new BlockPos(-2, 2, ROW), Items.DIAMOND, Items.EMERALD);
                    put(ctx, level, new BlockPos(-1, 3, ROW), Items.REDSTONE, Items.APPLE);
                    put(ctx, level, new BlockPos(0, 2, ROW), Items.COAL, Items.BREAD);
                    put(ctx, level, new BlockPos(1, 2, ROW), Items.COBBLESTONE, Items.STONE);
                })
                .thenWaitUntil(() -> ctx.mc().level != null && ctx.mc().level.getBlockEntity(ctx.abs(
                        new BlockPos(2, 2, ROW))) instanceof ConveyorBlockEntity, WAIT, "the belts to reach the player")
                .then(2, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(new BlockPos(-1, 2, ROW)))))
                .thenScreenshot(4, "belts-and-slopes")
                .thenScreenshot(MOVED, "belts-and-slopes-moved")
                .thenServer(0, level -> clear(ctx, level));
    }

    @ClientTest(timeoutTicks = 600)
    public static void chutesElevatorsAndSorters_drawTheirItems(final ClientTestContext ctx) {
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> {
                    clear(ctx, level);
                    for (int y = 2; y <= 4; y++) {
                        level.setBlockAndUpdate(ctx.abs(new BlockPos(-3, y + 1, ROW)),
                                TestConveyors.CHUTE.get().defaultBlockState());
                        level.setBlockAndUpdate(ctx.abs(new BlockPos(-2, y, ROW)), TestConveyors.ELEVATOR.get()
                                .defaultBlockState().setValue(ElevatorBlock.FACING, Direction.NORTH));
                    }
                    level.setBlockAndUpdate(ctx.abs(new BlockPos(-3, 2, ROW)), Blocks.CHEST.defaultBlockState());
                    put(ctx, level, new BlockPos(-3, 5, ROW), Items.GOLD_INGOT, Items.IRON_INGOT);
                    put(ctx, level, new BlockPos(-2, 2, ROW), Items.DIAMOND, Items.EMERALD);
                    piece(ctx, level, new BlockPos(0, 2, ROW), TestConveyors.SPLITTER.get(), Items.STONE);
                    piece(ctx, level, new BlockPos(1, 2, ROW), TestConveyors.JOIN.get(), Items.DIRT);
                    piece(ctx, level, new BlockPos(2, 2, ROW), TestConveyors.DIVERTER.get(), Items.SAND);
                })
                .thenWaitUntil(() -> ctx.mc().level != null && ctx.mc().level.getBlockEntity(ctx.abs(
                        new BlockPos(2, 2, ROW))) instanceof ConveyorBlockEntity, WAIT, "the pieces to reach the eye")
                .then(2, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(new BlockPos(0, 3, ROW)))))
                .thenScreenshot(4, "chutes-elevators-sorters")
                .thenServer(0, level -> clear(ctx, level));
    }

    private static void belt(final ClientTestContext ctx, final ServerLevel level, final BlockPos at,
                             final BeltSlope slope) {
        level.setBlockAndUpdate(ctx.abs(at), TestConveyors.BELT.get().defaultBlockState()
                .setValue(ConveyorBlock.FACING, Direction.EAST).setValue(ConveyorBlock.SLOPE, slope));
    }

    /* A splitter, join or diverter facing west, with an item on it. */
    private static void piece(final ClientTestContext ctx, final ServerLevel level, final BlockPos at,
                              final Block block, final Item item) {
        final BlockState state = block.defaultBlockState().setValue(ConveyorBlock.FACING, Direction.WEST);
        level.setBlockAndUpdate(ctx.abs(at), state);
        put(ctx, level, at, item);
    }

    /* Items put on the piece at {@code at}, from its start, a little apart. */
    private static void put(final ClientTestContext ctx, final ServerLevel level, final BlockPos at,
                            final Item... items) {
        if (level.getBlockEntity(ctx.abs(at)) instanceof ConveyorBlockEntity piece) {
            for (int k = 0; k < items.length; k++) {
                piece.put(new ItemStack(items[k]), 0.1 + k * 0.4);
            }
        }
    }

    /* Everything the tests place, back to air. */
    private static void clear(final ClientTestContext ctx, final ServerLevel level) {
        for (int x = -4; x <= 3; x++) {
            for (int y = 2; y <= 5; y++) {
                for (int z = 0; z <= 2; z++) {
                    level.setBlockAndUpdate(ctx.abs(new BlockPos(x, y, z)), Blocks.AIR.defaultBlockState());
                }
            }
        }
    }
}
