/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.gui.layout.CdeFrontPanelLayout;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * CDE as a player meets it on a UNIX machine: the Front Panel where the others have a bar, windows in Motif
 * frames, and the programs found through the panel's own subpanel.
 */
public final class CdeClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 80;
    private static final int BOOT_WAIT = 1_200;

    private static final BlockPos MACHINE = new BlockPos(5, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 2);

    private CdeClientTests() {
    }

    /**
     * The machine comes up at CDE, a control on the Front Panel opens its program in a Motif window, and the
     * Applications control raises the subpanel that lists every program and starts the one that is chosen.
     */
    @ClientTest(timeoutTicks = 3600)
    public static void frontPanel_opensProgramsFromItsControlsAndItsSubpanel(final ClientTestContext ctx) {
        ctx.thenBuild(0, world -> {
                    world.setBlock(MACHINE, ComputingModule.MAINFRAME.get());
                    final MainframeBlockEntity machine = world.blockEntity(MACHINE, MainframeBlockEntity.class);
                    final ItemStackHandler inv = machine.getInventory();
                    inv.setStackInSlot(MainframeBlockEntity.MOTHERBOARD_SLOT,
                            new ItemStack(ComputingModule.MOTHERBOARD_MTX_P.get()));
                    inv.setStackInSlot(MainframeBlockEntity.CPU_SLOTS_START,
                            new ItemStack(ComputingModule.CPU_SERVO_2620.get()));
                    inv.setStackInSlot(MainframeBlockEntity.RAM_SLOTS_START,
                            new ItemStack(ComputingModule.RAM_DDR3_8192.get()));
                    inv.setStackInSlot(MainframeBlockEntity.PSU_SLOT, new ItemStack(ComputingModule.PSU_650G.get()));
                    // A graphics card gives the machine peripheral ports, which is what the monitor links to.
                    inv.setStackInSlot(MainframeBlockEntity.GPU_SLOTS_START,
                            new ItemStack(ComputingModule.GPU_HD_7970.get()));
                    inv.setStackInSlot(MainframeBlockEntity.DISK_SLOTS_START,
                            new ItemStack(ComputingModule.disk(StorageTier.SSD, DiskSize.GB_500)));
                    machine.installOs(jsc("unix"));
                    machine.console().install(jsc("cde").toString());
                    machine.togglePower();
                    world.placeMonitor(MONITOR, Direction.EAST);
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .thenScreenshot(SETTLE * 2, "cde-desktop")
                .then(SETTLE, () -> press(ctx, CdeFrontPanelLayout.Control.FILES))
                .thenWaitUntil(() -> desktop(ctx).openWindowLabels().contains("File Manager"), SCREEN_WAIT,
                        "the Files control to open the File Manager")
                .thenScreenshot(SETTLE, "cde-file-manager")
                .then(SETTLE, () -> press(ctx, CdeFrontPanelLayout.Control.APPLICATIONS))
                .thenWaitUntil(() -> desktop(ctx).isStartOpen(), SCREEN_WAIT, "the Applications subpanel to rise")
                .thenScreenshot(SETTLE, "cde-applications");
    }

    /** Clicks a control of the Front Panel where it is drawn on the screen. */
    private static void press(final ClientTestContext ctx, final CdeFrontPanelLayout.Control control) {
        final int[] at = desktop(ctx).frontPanelPoint(control);
        ctx.click(at[0] + 0.5, at[1] + 0.5);
    }

    private static DesktopScreen desktop(final ClientTestContext ctx) {
        return ctx.screen(DesktopScreen.class);
    }

    private static ResourceLocation jsc(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, path);
    }
}
