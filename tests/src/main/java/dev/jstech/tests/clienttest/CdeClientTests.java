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
import java.util.List;
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
    /** Ticks enough for a first click to have gone stale, so the two after it are a double click of their own. */
    private static final int DOUBLE_CLICK_GONE = 10;

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
        atCde(ctx)
                .thenScreenshot(SETTLE * 2, "cde-desktop")
                .then(SETTLE, () -> press(ctx, CdeFrontPanelLayout.Control.FILES))
                .thenWaitUntil(() -> desktop(ctx).openWindowLabels().contains("File Manager"), SCREEN_WAIT,
                        "the Files control to open the File Manager")
                .thenScreenshot(SETTLE, "cde-file-manager")
                .then(SETTLE, () -> press(ctx, CdeFrontPanelLayout.Control.APPLICATIONS))
                .thenWaitUntil(() -> desktop(ctx).isStartOpen(), SCREEN_WAIT, "the Applications subpanel to rise")
                .thenScreenshot(SETTLE, "cde-applications");
    }

    /** A UNIX machine with CDE installed, brought up, with the player at its monitor and the desktop open. */
    private static ClientTestContext atCde(final ClientTestContext ctx) {
        return ctx.thenBuild(0, world -> {
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
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT);
    }

    /**
     * The workspaces are real: a window stays on the one it was opened on, another workspace shows a desktop
     * without it, and what is opened there belongs there.
     */
    @ClientTest(timeoutTicks = 3600)
    public static void workspaces_keepTheirOwnWindows(final ClientTestContext ctx) {
        atCde(ctx)
                .then(SETTLE, () -> press(ctx, CdeFrontPanelLayout.Control.FILES))
                .thenWaitUntil(() -> desktop(ctx).shownWindowLabels().contains("File Manager"), SCREEN_WAIT,
                        "the File Manager to open on workspace One")
                .then(SETTLE, () -> pressWorkspace(ctx, 1))
                .thenWaitUntil(() -> desktop(ctx).shownWorkspace() == 1, SCREEN_WAIT, "workspace Two to come up")
                .thenAssert(1, () -> desktop(ctx).shownWindowLabels().isEmpty(), "workspace Two shows no window")
                .thenAssert(1, () -> desktop(ctx).openWindowLabels().contains("File Manager"),
                        "and the File Manager is still open, on its own workspace")
                .then(SETTLE, () -> press(ctx, CdeFrontPanelLayout.Control.EDITOR))
                .thenWaitUntil(() -> desktop(ctx).shownWindowLabels().contains("Text Editor"), SCREEN_WAIT,
                        "the Text Editor to open on workspace Two")
                .thenScreenshot(SETTLE, "cde-workspace-two")
                .then(SETTLE, () -> pressWorkspace(ctx, 0))
                .thenWaitUntil(() -> desktop(ctx).shownWorkspace() == 0, SCREEN_WAIT, "workspace One to come back")
                .thenAssert(1, () -> desktop(ctx).shownWindowLabels().equals(List.of("File Manager")),
                        "workspace One shows the File Manager alone");
    }

    /**
     * CDE lists no open windows, so a window that is put away has to be found some other way: it stands at the
     * top left of its workspace as an icon, clear of what the player keeps on the workspace, and a double click
     * on the icon brings it back where one click does not.
     */
    @ClientTest(timeoutTicks = 3600)
    public static void aWindowPutAway_standsAsAnIconThatBringsItBack(final ClientTestContext ctx) {
        atCde(ctx)
                .then(SETTLE, () -> press(ctx, CdeFrontPanelLayout.Control.FILES))
                .thenWaitUntil(() -> desktop(ctx).shownWindowLabels().contains("File Manager"), SCREEN_WAIT,
                        "the File Manager to open")
                .then(SETTLE, () -> clickAt(ctx, desktop(ctx).windowButtonPoint("File Manager", 1)))
                .thenWaitUntil(() -> desktop(ctx).shownWindowLabels().isEmpty(), SCREEN_WAIT,
                        "the minimise button to put the window away")
                .thenAssert(1, () -> desktop(ctx).openWindowLabels().contains("File Manager"),
                        "put away, the window is still open")
                .thenScreenshot(SETTLE, "cde-window-icon")
                .then(SETTLE, () -> clickAt(ctx, desktop(ctx).putAwayIconPoint(0)))
                .thenAssert(DOUBLE_CLICK_GONE, () -> desktop(ctx).shownWindowLabels().isEmpty(),
                        "one click on the icon leaves the window put away")
                .then(1, () -> {
                    clickAt(ctx, desktop(ctx).putAwayIconPoint(0));
                    clickAt(ctx, desktop(ctx).putAwayIconPoint(0));
                })
                .thenWaitUntil(() -> desktop(ctx).shownWindowLabels().contains("File Manager"), SCREEN_WAIT,
                        "a double click on its icon to bring the window back");
    }

    private static void clickAt(final ClientTestContext ctx, final int[] at) {
        ctx.click(at[0] + 0.5, at[1] + 0.5);
    }

    private static void pressWorkspace(final ClientTestContext ctx, final int index) {
        final int[] at = desktop(ctx).workspacePoint(index);
        ctx.click(at[0] + 0.5, at[1] + 0.5);
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
