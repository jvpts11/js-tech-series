/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.block.ServerRackBlock;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.ItemEntry;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

/**
 * The Server Racks of every age side by side, each holding 1U servers of its own age and the ages before it, every
 * server in the shape of its own age (a silver Transition server in a black Transition rack, every age's in the
 * Advanced one); and a rack's roof seen from above twice a few ticks apart, its fans' rotors turned and their
 * housings still.
 */
public final class RackClientTests {

    private static final int SETTLE = 6;
    /** The racks face south in a row, four blocks apart, each seen from five blocks in front. */
    private static final int SPACING = 4;
    private static final int ROW_Z = 4;
    private static final int AWAY = 5;
    /** The rack whose roof is watched, and where the player hovers in front of it to look down at it. */
    private static final BlockPos ROOF_RACK = new BlockPos(2, 2, 14);
    private static final BlockPos ABOVE = new BlockPos(2, 6, 17);
    private static final float LOOK_DOWN = 35.0F;
    private static final int FAN_GAP = 3;

    /** Each age's rack and the servers it holds, from the top row down. */
    private static final List<Cabinet> CABINETS = List.of(
            new Cabinet("vintage", ComputingModule.VINTAGE_SERVER_RACK,
                    List.of(ComputingModule.VINTAGE_SERVER, ComputingModule.VINTAGE_SERVER)),
            new Cabinet("legacy", ComputingModule.LEGACY_SERVER_RACK,
                    List.of(ComputingModule.LEGACY_SERVER, ComputingModule.LEGACY_SERVER,
                            ComputingModule.VINTAGE_SERVER)),
            new Cabinet("transition", ComputingModule.TRANSITION_SERVER_RACK,
                    List.of(ComputingModule.TRANSITION_SERVER, ComputingModule.TRANSITION_SERVER,
                            ComputingModule.TRANSITION_SERVER, ComputingModule.LEGACY_SERVER)),
            new Cabinet("standard", ComputingModule.SERVER_RACK,
                    List.of(ComputingModule.SERVER, ComputingModule.SERVER, ComputingModule.TRANSITION_SERVER)),
            new Cabinet("advanced", ComputingModule.ADVANCED_SERVER_RACK,
                    List.of(ComputingModule.ADVANCED_SERVER, ComputingModule.ADVANCED_SERVER,
                            ComputingModule.SERVER, ComputingModule.TRANSITION_SERVER, ComputingModule.LEGACY_SERVER,
                            ComputingModule.VINTAGE_SERVER)));

    private RackClientTests() {
    }

    @ClientTest(timeoutTicks = 900)
    public static void serverRacks_ofEveryAgeHoldTheirServers(final ClientTestContext ctx) {
        ctx.thenBuild(0, world -> {
            for (int i = 0; i < CABINETS.size(); i++) {
                final BlockPos at = rack(i);
                world.setBlock(at, CABINETS.get(i).block().get().defaultBlockState()
                        .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
                final ServerRackBlockEntity rack = world.blockEntity(at, ServerRackBlockEntity.class);
                final List<ItemEntry<?>> servers = CABINETS.get(i).servers();
                for (int row = 0; row < servers.size(); row++) {
                    rack.getServers().setStackInSlot(row, new ItemStack(servers.get(row).get()));
                }
            }
        });
        for (int i = 0; i < CABINETS.size(); i++) {
            ctx.thenTeleport(SETTLE, rack(i).south(AWAY), Direction.NORTH)
                    .thenScreenshot(SETTLE, CABINETS.get(i).name());
        }
    }

    @ClientTest(timeoutTicks = 600)
    public static void roofFans_turnTheirRotorsInStillHousings(final ClientTestContext ctx) {
        ctx.thenBuild(0, world -> {
            world.setBlock(ROOF_RACK, ComputingModule.ADVANCED_SERVER_RACK.get().defaultBlockState()
                    .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
            world.blockEntity(ROOF_RACK, ServerRackBlockEntity.class).getServers()
                    .setStackInSlot(0, new ItemStack(ComputingModule.ADVANCED_SERVER.get()));
            // An unseen block to stand on, so the camera holds still above the roof between the two looks.
            world.setBlock(ABOVE.below(), Blocks.BARRIER);
        })
                .thenTeleport(SETTLE, ABOVE, Direction.NORTH)
                .then(SETTLE, () -> ctx.player().setXRot(LOOK_DOWN))
                .thenScreenshot(SETTLE, "roof")
                .thenScreenshot(FAN_GAP, "roof-turned-further");
    }

    private static BlockPos rack(final int index) {
        return new BlockPos(index * SPACING, 2, ROW_Z);
    }

    /** An age's rack and the servers seated in it, from the top row down. */
    private record Cabinet(String name, BlockEntry<ServerRackBlock> block, List<ItemEntry<?>> servers) {
    }
}
