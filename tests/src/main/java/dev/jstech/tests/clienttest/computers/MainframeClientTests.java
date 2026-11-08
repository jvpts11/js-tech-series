/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.computers;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.CoolingItems;
import dev.jstech.computers.HardwareItems;
import dev.jstech.computers.block.MainframeBlock;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * The Mainframes of every age side by side, each built on a board of its own age and switched on: every cabinet
 * stands with its lamps and light bars lit, the Transition's blue slit down the mullion and the Advanced's white
 * lines up its sides among them; each is seen again with the service panel off, its parts in the bay, each drawn by
 * its own model: the board standing on the back of the card bay facing the front, the four processors with their
 * coolers and the memory on it, the supply in its niche and the disks in their sleds, a lamp lit on the sled of the
 * one that carries a system.
 */
public final class MainframeClientTests {

    private static final int SETTLE = 6;
    /** The cabinets face south in a row, five blocks apart, each seen from five blocks in front. */
    private static final int SPACING = 5;
    private static final int ROW_Z = 4;
    private static final int AWAY = 5;
    /** How far from an open cabinet the camera stands to see the parts in its bays. */
    private static final int NEAR = 3;
    /** How long the machines may take to be known running once switched on. */
    private static final int RUNS_WITHIN = 100;
    /**
     * The five cabinets, each with a build of its own age: the board, a processor and the cooler over it on every
     * socket, a module in every memory slot, the supply and four disks.
     */
    private static final List<Cabinet> CABINETS = List.of(
            new Cabinet("vintage", ComputingModule.VINTAGE_MAINFRAME, HardwareItems.MOTHERBOARD_MTX_VINTAGE,
                    HardwareItems.CPU_INTEGRA_PENTIX_PRO_200, CoolingItems.VINTAGE_STOCK_COOLER,
                    HardwareItems.RAM_SIMM_4, HardwareItems.PSU_300),
            new Cabinet("legacy", ComputingModule.LEGACY_MAINFRAME, HardwareItems.MOTHERBOARD_MTX_LEGACY,
                    HardwareItems.CPU_VELOCION_OPTERA_244, CoolingItems.LEGACY_STOCK_COOLER, HardwareItems.RAM_DDR_1024,
                    HardwareItems.PSU_500B),
            new Cabinet("transition", ComputingModule.TRANSITION_MAINFRAME, HardwareItems.MOTHERBOARD_MTX_T,
                    HardwareItems.CPU_VELOCION_OPTERA_8356, CoolingItems.TRANSITION_STOCK_COOLER,
                    HardwareItems.RAM_DDR2_4096_RDIMM, HardwareItems.PSU_2000P),
            new Cabinet("standard", ComputingModule.MAINFRAME, ComputingModule.MOTHERBOARD_MTX_S_2011,
                    ComputingModule.CPU_SERVO_2620, CoolingItems.STANDARD_TOWER_COOLER, ComputingModule.RAM_DDR3_8192,
                    HardwareItems.PSU_2000P),
            new Cabinet("advanced", ComputingModule.ADVANCED_MAINFRAME, HardwareItems.MOTHERBOARD_MTX_A_SP3,
                    HardwareItems.CPU_VELOCION_EPIC_7302, CoolingItems.ADVANCED_TOWER_COOLER,
                    HardwareItems.RAM_DDR4_65536_RDIMM, HardwareItems.PSU_2000P));

    private MainframeClientTests() {
    }

    @ClientTest(timeoutTicks = 900)
    public static void mainframes_ofEveryAgeStandRunningWithTheirLights(final ClientTestContext ctx) {
        ctx.thenBuild(0, world -> {
            for (int i = 0; i < CABINETS.size(); i++) {
                final Cabinet cabinet = CABINETS.get(i);
                final BlockPos at = origin(i);
                world.setBlock(at, cabinet.block().get().defaultBlockState()
                        .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
                final MainframeBlockEntity mainframe = world.blockEntity(at, MainframeBlockEntity.class);
                cabinet.install(mainframe.getHardware());
                mainframe.togglePower();
            }
        }).thenWaitUntil(() -> IntStream.range(0, CABINETS.size()).allMatch(i -> runs(ctx, origin(i))),
                RUNS_WITHIN, "every Mainframe to run, as the player sees it");
        for (int i = 0; i < CABINETS.size(); i++) {
            ctx.thenTeleport(SETTLE, origin(i).south(AWAY), Direction.NORTH)
                    .thenScreenshot(SETTLE, CABINETS.get(i).name() + "-running");
        }
        for (int i = 0; i < CABINETS.size(); i++) {
            final BlockPos at = origin(i);
            ctx.thenBuild(0, world -> world.blockEntity(at, MainframeBlockEntity.class).toggleServicePanel())
                    .thenTeleport(SETTLE, at.south(AWAY), Direction.NORTH)
                    .thenScreenshot(SETTLE, CABINETS.get(i).name() + "-open")
                    .thenTeleport(SETTLE, at.south(NEAR), Direction.NORTH)
                    .thenScreenshot(SETTLE, CABINETS.get(i).name() + "-open-near");
        }
    }

    private static BlockPos origin(final int index) {
        return new BlockPos(index * SPACING, 2, ROW_Z);
    }

    /** Whether the player's game knows the Mainframe there runs on a build that makes a computer. */
    private static boolean runs(final ClientTestContext ctx, final BlockPos relative) {
        return ctx.mc().level != null && ctx.mc().level.getBlockEntity(ctx.abs(relative))
                instanceof MainframeBlockEntity mainframe && mainframe.visualRunning() && mainframe.visualBuildValid();
    }

    /** A cabinet and the parts of its age it is built with. */
    private record Cabinet(String name, BlockEntry<MainframeBlock> block, Supplier<? extends Item> board,
                           Supplier<? extends Item> cpu, Supplier<? extends Item> cooler, Supplier<? extends Item> ram,
                           Supplier<? extends Item> psu) {

        void install(final ItemStackHandler slots) {
            slots.setStackInSlot(MainframeBlockEntity.MOTHERBOARD_SLOT, new ItemStack(board.get()));
            for (int k = 0; k < MainframeBlockEntity.CPU_SLOTS; k++) {
                slots.setStackInSlot(MainframeBlockEntity.CPU_SLOTS_START + k, new ItemStack(cpu.get()));
                slots.setStackInSlot(MainframeBlockEntity.COOLER_SLOTS_START + k, new ItemStack(cooler.get()));
            }
            for (int k = 0; k < MainframeBlockEntity.RAM_SLOTS; k++) {
                slots.setStackInSlot(MainframeBlockEntity.RAM_SLOTS_START + k, new ItemStack(ram.get()));
            }
            slots.setStackInSlot(MainframeBlockEntity.PSU_SLOT, new ItemStack(psu.get()));
            for (int k = 0; k < MainframeBlockEntity.DISK_SLOTS; k++) {
                slots.setStackInSlot(MainframeBlockEntity.DISK_SLOTS_START + k,
                        new ItemStack(ComputingModule.disk(StorageTier.HDD, DiskSize.GB_500)));
            }
        }
    }
}
