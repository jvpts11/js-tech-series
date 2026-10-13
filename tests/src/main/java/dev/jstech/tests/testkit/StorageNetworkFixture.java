/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.testkit;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.storage.ExternalDataPort;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * A running Mainframe cabled to one Server Rack on slow drives, and the small handler helpers the Operation tests
 * around it share, so no test class has to borrow another one's setup.
 */
public final class StorageNetworkFixture {

    private static final BlockPos MAINFRAME = new BlockPos(1, 2, 2);
    private static final BlockPos RACK = new BlockPos(3, 2, 2);

    private StorageNetworkFixture() {
    }

    /**
     * A running single-queue Mainframe cabled to one Server Rack whose server sits on HDD drives. The HDD
     * seek latency (ten ticks) keeps an Operation visibly in flight for a while, so a test can look at who
     * holds the queue mid-way; on NVMe a thirty-item pull is over the tick after it is granted.
     */
    public static MainframeBlockEntity storageNetwork(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final MainframeBlockEntity mainframe = world.placeRunningMainframe(MAINFRAME);
        world.setBlock(new BlockPos(2, 2, 2), ComputingModule.HBW_CABLE);
        world.setBlock(RACK, ComputingModule.SERVER_RACK.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST)); // cables attach through the rear
        final ServerRackBlockEntity rack = world.blockEntity(RACK, ServerRackBlockEntity.class);
        rack.getServers().setStackInSlot(0, ServerStacks.defaultServer());
        rack.insertDrive(0, new ItemStack(ComputingModule.disk(StorageTier.HDD, DiskSize.GB_500)));
        rack.insertDrive(0, new ItemStack(ComputingModule.disk(StorageTier.HDD, DiskSize.GB_500)));
        return mainframe;
    }

    /** The rack {@link #storageNetwork} placed. */
    public static ServerRackBlockEntity rack(final GameTestHelper helper) {
        if (helper.getBlockEntity(RACK) instanceof ServerRackBlockEntity rack) {
            return rack;
        }
        throw new IllegalStateException("no rack at " + RACK);
    }

    public static ExternalDataPort port(final ItemStackHandler handler) {
        return new ExternalDataPort(handler, null);
    }

    /** A one-slot handler holding a full stack of sticks, so a push into it has nowhere to go. */
    public static ItemStackHandler fullHandler() {
        final ItemStackHandler handler = new ItemStackHandler(1);
        handler.setStackInSlot(0, new ItemStack(Items.STICK, 64));
        return handler;
    }

    /** How many items the handler holds across all its slots. */
    public static int count(final ItemStackHandler handler) {
        int total = 0;
        for (int i = 0; i < handler.getSlots(); i++) {
            total += handler.getStackInSlot(i).getCount();
        }
        return total;
    }
}
