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
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.client.os.DeviceManagerApp;
import dev.jstech.computers.os.devices.DeviceRows;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/**
 * The Device Manager of the Frames editions, worked as a player works it: opened from Start, the CD drive disabled
 * from the right-click on Frames XP and from the Disable button on Frames 95, each crossing it out and the machine
 * really disabling it, and the views changed from the View menu.
 */
public final class DeviceManagerClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 80;
    private static final int BOOT_WAIT = 400;
    private static final BlockPos COMPUTER = new BlockPos(5, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos DRIVE = new BlockPos(5, 2, 3);
    private static final BlockPos SPEAKER = new BlockPos(5, 2, 1);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 2);
    private static final String CD_ROW = "USB 1: CD Drive";

    private DeviceManagerClientTests() {
    }

    @ClientTest(timeoutTicks = 1600)
    public static void framesXp_disablesADeviceFromTheRightClickAndChangesView(final ClientTestContext ctx) {
        final PersonalComputerBlockEntity[] pc = new PersonalComputerBlockEntity[1];
        openOn(ctx, "frames_xp", pc)
                .then(0, () -> launch(ctx, "Device Manager"))
                .thenWaitUntil(() -> app(ctx, "Device Manager") != null
                                && app(ctx, "Device Manager").shownRows().contains(CD_ROW), SCREEN_WAIT,
                        "the Device Manager to list the CD drive on its port")
                .thenScreenshot(2, "xp-by-port")
                .then(SETTLE, () -> ctx.rightClickDesktop(app(ctx, "Device Manager").rowCentre(CD_ROW)))
                .thenWaitUntil(() -> app(ctx, "Device Manager").contextMenu().isOpen(), SCREEN_WAIT,
                        "the right-click to open its menu")
                .thenScreenshot(2, "xp-context-menu")
                .then(SETTLE, () -> ctx.clickDesktop(app(ctx, "Device Manager").contextMenu().itemCenter(1)))
                .thenWaitUntil(() -> app(ctx, "Device Manager").rowDisabled(CD_ROW), SCREEN_WAIT,
                        "the CD drive to be crossed out")
                .thenWaitUntilServer(level -> pc[0].isDisabled(ctx.abs(DRIVE).asLong()), SCREEN_WAIT,
                        "the machine to disable the drive", level -> "still enabled")
                .thenScreenshot(2, "xp-disabled")
                .then(SETTLE, () -> ctx.clickDesktop(app(ctx, "Device Manager").menuBar().titleCenter(2)))
                .then(SETTLE, () -> ctx.clickDesktop(app(ctx, "Device Manager").menuBar().menu().itemCenter(0)))
                .thenWaitUntil(() -> app(ctx, "Device Manager").view() == DeviceRows.View.BY_TYPE
                                && app(ctx, "Device Manager").shownRows().contains("Display adapters"), SCREEN_WAIT,
                        "View, Devices by type to list the parts by type")
                .thenScreenshot(2, "xp-by-type");
    }

    @ClientTest(timeoutTicks = 1600)
    public static void frames95_disablesADeviceFromItsButtonInSystemProperties(final ClientTestContext ctx) {
        final PersonalComputerBlockEntity[] pc = new PersonalComputerBlockEntity[1];
        openOn(ctx, "frames_95", pc)
                .then(0, () -> launch(ctx, "System"))
                .thenWaitUntil(() -> app(ctx, "System") != null && app(ctx, "System").shownRows().contains(CD_ROW),
                        SCREEN_WAIT, "System Properties to list the CD drive on its port")
                .thenScreenshot(2, "95-by-port")
                // System Properties' tree is short, and the board's own graphics stand above the ports in it.
                .thenWaitUntil(() -> inView(ctx, "System", CD_ROW), SCREEN_WAIT,
                        "the CD drive's row to be scrolled into view")
                .then(SETTLE, () -> ctx.clickDesktop(app(ctx, "System").rowCentre(CD_ROW)))
                .then(SETTLE, () -> ctx.clickDesktop(app(ctx, "System").buttonCentre(2)))
                .thenWaitUntil(() -> app(ctx, "System").rowDisabled(CD_ROW), SCREEN_WAIT,
                        "the Disable button to cross the drive out")
                .thenWaitUntilServer(level -> pc[0].isDisabled(ctx.abs(DRIVE).asLong()), SCREEN_WAIT,
                        "the machine to disable the drive", level -> "still enabled")
                .thenScreenshot(2, "95-disabled")
                .then(SETTLE, () -> ctx.clickDesktop(app(ctx, "System").buttonCentre(0)))
                .thenWaitUntil(() -> app(ctx, "System").propertiesOpen(), SCREEN_WAIT, "Properties to open")
                .thenScreenshot(2, "95-properties");
    }

    @ClientTest(timeoutTicks = 1600)
    public static void frames11_listsThePortsInItsOwnWindow(final ClientTestContext ctx) {
        final PersonalComputerBlockEntity[] pc = new PersonalComputerBlockEntity[1];
        // Frames 11's Start is a grid of pinned programs rather than a list, so the program opens by name here.
        openOn(ctx, "frames_11", pc)
                .then(0, () -> DesktopScreen.requestOpen("Device Manager"))
                .thenWaitUntil(() -> app(ctx, "Device Manager") != null
                                && app(ctx, "Device Manager").shownRows().contains(CD_ROW), SCREEN_WAIT,
                        "the Device Manager to list the CD drive on its port")
                .thenScreenshot(2, "11-by-port")
                .then(SETTLE, () -> {
                    for (int i = 0; i < 20; i++) {
                        app(ctx, "Device Manager").mouseScrolled(-1);
                    }
                })
                .thenWaitUntil(() -> {
                    final DeviceManagerApp manager = app(ctx, "Device Manager");
                    return manager.rowCentre(manager.shownRows().getLast()) != null;
                }, SCREEN_WAIT, "the count at the tree's foot to be in view")
                .thenScreenshot(2, "11-scrolled");
    }

    /* A running computer with the system on it, a monitor, a CD drive and a speaker, and the desktop up. */
    private static ClientTestContext openOn(final ClientTestContext ctx, final String system,
                                            final PersonalComputerBlockEntity[] pc) {
        return ctx.thenBuild(0, world -> {
                    pc[0] = world.placeRunningPersonalComputer(COMPUTER,
                            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, system));
                    world.placeMonitor(MONITOR, Direction.EAST);
                    world.setBlock(DRIVE, ComputingModule.CD_DRIVE.get());
                    world.setBlock(SPEAKER, ComputingModule.SPEAKER.get());
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .thenWaitUntil(() -> !ctx.screen(DesktopScreen.class).launcherLabels().isEmpty(), SCREEN_WAIT,
                        "the desktop to list its programs");
    }

    private static void launch(final ClientTestContext ctx, final String label) {
        DesktopSteps.launch(ctx, label);
    }

    /* Whether that row is in view in that window's list, scrolling it one notch further down when it is not. */
    private static boolean inView(final ClientTestContext ctx, final String label, final String row) {
        final DeviceManagerApp manager = app(ctx, label);
        if (manager == null) {
            return false;
        }
        if (manager.rowCentre(row) != null) {
            return true;
        }
        manager.mouseScrolled(-1);
        return false;
    }

    private static DeviceManagerApp app(final ClientTestContext ctx, final String label) {
        return DesktopSteps.app(ctx, label, DeviceManagerApp.class);
    }
}
