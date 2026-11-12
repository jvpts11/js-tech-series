/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.computers;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.client.os.DesktopWindow;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The disk tools as a player sees them (computers/docs/design/storage-devices.md), each on a computer with a second
 * disk beside its own: Frames 11's and Frames XP's Disk Management, the KDE Partition Manager and GNOME's Disks, each
 * listing both disks with their partitions, shot as it opens and maximized for the side-by-side with their mocks.
 */
public final class DiskToolsClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 120;
    private static final int BOOT_WAIT = 1_200;
    /* Long enough for the tool to ask the machine for its disks and draw what came back. */
    private static final int REPORT_WAIT = 40;
    private static final BlockPos COMPUTER = new BlockPos(5, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 2);
    private static final String DISK_MANAGEMENT = "jsc:disk_management";
    private static final String DISKS = "jsc:disks";

    private DiskToolsClientTests() {
    }

    @ClientTest(timeoutTicks = 3000)
    public static void diskManagement_framesEleven_listsBothDisksAndTheirPartitions(final ClientTestContext ctx) {
        shot(ctx, opened(ctx, "frames_11", null, DISK_MANAGEMENT), DISK_MANAGEMENT, "disk-management-frames-11");
    }

    @ClientTest(timeoutTicks = 3000)
    public static void diskManagement_framesXp_listsBothDisksAndTheirPartitions(final ClientTestContext ctx) {
        shot(ctx, opened(ctx, "frames_xp", null, DISK_MANAGEMENT), DISK_MANAGEMENT, "disk-management-frames-xp");
    }

    @ClientTest(timeoutTicks = 3000)
    public static void partitionManager_kde_listsBothDisksAndTheirPartitions(final ClientTestContext ctx) {
        shot(ctx, opened(ctx, "ubuntu", "jsc:kde_plasma", DISKS), DISKS, "kde-partition-manager");
    }

    @ClientTest(timeoutTicks = 3000)
    public static void disks_gnome_listsBothDisksAndTheirPartitions(final ClientTestContext ctx) {
        shot(ctx, opened(ctx, "ubuntu", "jsc:gnome", DISKS), DISKS, "gnome-disks");
    }

    /* The tool shot as it opens, and again maximized, as its mocks show it. */
    private static void shot(final ClientTestContext ctx, final ClientTestContext opened, final String tool,
                             final String label) {
        opened.thenScreenshot(REPORT_WAIT, label)
                .then(SETTLE, () -> ctx.clickDesktop(window(ctx, tool).buttonCentre(DesktopWindow.BUTTON_MAXIMIZE)))
                .thenScreenshot(REPORT_WAIT, label + "-maximized");
    }

    /*
     * A running computer of {@code system}, with {@code desktop} installed over it when one is named, a monitor and a
     * second disk, its desktop up and the tool {@code tool} opened on it.
     */
    private static ClientTestContext opened(final ClientTestContext ctx, final String system,
                                            @Nullable final String desktop, final String tool) {
        return ctx.thenBuild(0, world -> {
                    final PersonalComputerBlockEntity pc = world.placeRunningPersonalComputer(COMPUTER,
                            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, system));
                    if (desktop != null) {
                        pc.console().install(desktop);
                    }
                    pc.getHardware().setStackInSlot(PersonalComputerBlockEntity.DISK_SLOTS_START + 1,
                            new ItemStack(ComputingModule.disk(StorageTier.SSD, DiskSize.GB_500)));
                    world.placeMonitor(MONITOR, Direction.EAST);
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .thenWaitUntil(() -> !ctx.screen(DesktopScreen.class).launcherLabels().isEmpty(), SCREEN_WAIT,
                        "the desktop to list its programs")
                .then(SETTLE, () -> DesktopScreen.requestOpen(tool))
                .thenWaitUntil(() -> window(ctx, tool) != null, SCREEN_WAIT, "the disk tool to open in a window");
    }

    @Nullable
    private static DesktopWindow window(final ClientTestContext ctx, final String tool) {
        final DesktopScreen desktop = ctx.openScreen(DesktopScreen.class);
        return desktop == null ? null : desktop.windowFor(tool);
    }
}
