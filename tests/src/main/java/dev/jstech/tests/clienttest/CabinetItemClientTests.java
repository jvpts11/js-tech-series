/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.tests.testkit.TestWorldBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.AABB;

/**
 * The items of the machines their block entities draw, a Mainframe and a Server Rack, shown in frames beside a
 * running Mainframe full of hardware and a rack with a server in it. The items share the models the world draws, and
 * each shows its machine as it comes, empty and switched off, whatever the machines beside it hold.
 */
public final class CabinetItemClientTests {

    private static final int SETTLE = 4;
    /** Long enough for the Mainframe to come on and light its lamps. */
    private static final int RUNNING = 60;

    private static final BlockPos MAINFRAME = new BlockPos(4, 2, 3);
    private static final BlockPos RACK = new BlockPos(9, 2, 3);
    private static final BlockPos MAINFRAME_FRAME_WALL = new BlockPos(6, 3, 4);
    private static final BlockPos RACK_FRAME_WALL = new BlockPos(7, 3, 4);
    private static final BlockPos PLAYER = new BlockPos(6, 2, 9);
    private static final BlockPos CLOSE = new BlockPos(6, 2, 7);

    private CabinetItemClientTests() {
    }

    @ClientTest(timeoutTicks = 600)
    public static void cabinetItems_showTheMachineAsItComes(final ClientTestContext ctx) {
        ctx.thenBuild(0, world -> {
                    world.setBlock(MAINFRAME, ComputingModule.MAINFRAME.get().defaultBlockState()
                            .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
                    final MainframeBlockEntity mainframe = world.blockEntity(MAINFRAME, MainframeBlockEntity.class);
                    TestWorldBuilder.installMainframeBuild(mainframe);
                    mainframe.togglePower();
                    world.placeSeededRack(RACK, Direction.SOUTH);
                    world.setBlock(MAINFRAME_FRAME_WALL, Blocks.STONE);
                    world.setBlock(RACK_FRAME_WALL, Blocks.STONE);
                })
                .thenServer(SETTLE, level -> {
                    hang(ctx, level, MAINFRAME_FRAME_WALL, ComputingModule.MAINFRAME.get());
                    hang(ctx, level, RACK_FRAME_WALL, ComputingModule.SERVER_RACK.get());
                })
                .thenTeleport(SETTLE, PLAYER, Direction.NORTH)
                .thenScreenshot(RUNNING, "beside-the-machines")
                .thenTeleport(0, CLOSE, Direction.NORTH)
                .thenScreenshot(SETTLE * 2, "the-items-close-up")
                .thenServer(0, level -> {
                    unhang(ctx, level, MAINFRAME_FRAME_WALL);
                    unhang(ctx, level, RACK_FRAME_WALL);
                });
    }

    /* Hangs a frame holding that item on the south face of the wall block. */
    private static void hang(final ClientTestContext ctx, final ServerLevel level, final BlockPos wall,
                             final ItemLike item) {
        final ItemFrame frame = new ItemFrame(level, ctx.abs(wall.south()), Direction.SOUTH);
        frame.setItem(new ItemStack(item));
        level.addFreshEntity(frame);
    }

    private static void unhang(final ClientTestContext ctx, final ServerLevel level, final BlockPos wall) {
        level.getEntitiesOfClass(ItemFrame.class, new AABB(ctx.abs(wall.south()))).forEach(ItemFrame::discard);
    }
}
