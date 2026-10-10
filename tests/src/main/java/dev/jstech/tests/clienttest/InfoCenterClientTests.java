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
import dev.jstech.computers.client.os.DesktopWindow;
import dev.jstech.computers.client.os.ThisPcApp;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/**
 * KDE's Info Center, worked as a player works it: its "Devices by port" page lists the ports beside what is plugged
 * into each, and the button under them disables the device selected, which the machine then really disables.
 */
public final class InfoCenterClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 80;
    private static final int BOOT_WAIT = 400;
    private static final BlockPos COMPUTER = new BlockPos(5, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos DRIVE = new BlockPos(5, 2, 3);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 2);
    private static final String INFO_CENTER = "Info Center";
    private static final String CD_ROW = "USB 1: CD Drive";

    private InfoCenterClientTests() {
    }

    @ClientTest(timeoutTicks = 2400)
    public static void kde_devicesByPortDisablesTheSelectedDevice(final ClientTestContext ctx) {
        final PersonalComputerBlockEntity[] pc = new PersonalComputerBlockEntity[1];
        ctx.thenBuild(0, world -> {
                    pc[0] = world.placeRunningPersonalComputer(COMPUTER,
                            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "ubuntu"));
                    pc[0].console().install("jsc:kde_plasma");
                    world.placeMonitor(MONITOR, Direction.EAST);
                    world.setBlock(DRIVE, ComputingModule.CD_DRIVE.get());
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .thenWaitUntil(() -> ctx.screen(DesktopScreen.class).launcherLabels().contains(INFO_CENTER),
                        SCREEN_WAIT, "the Info Center to be listed")
                .then(0, () -> DesktopScreen.requestOpen(INFO_CENTER))
                .thenWaitUntil(() -> app(ctx) != null && app(ctx).isAboutPage(), SCREEN_WAIT,
                        "the Info Center to open on its About page")
                .thenScreenshot(2, "kde-about")
                .then(SETTLE, () -> ctx.clickDesktop(point(ctx, app(ctx).infoCenterTabCenter(1))))
                .thenWaitUntil(() -> app(ctx).deviceRows().contains(CD_ROW), SCREEN_WAIT,
                        "Devices by port to list the drive on its port")
                .thenScreenshot(2, "kde-devices")
                .then(SETTLE, () -> ctx.clickDesktop(point(ctx, app(ctx).deviceRowCenter(CD_ROW))))
                .then(SETTLE, () -> ctx.clickDesktop(point(ctx, app(ctx).deviceButtonCenter())))
                .thenWaitUntilServer(level -> pc[0].isDisabled(ctx.abs(DRIVE).asLong()), SCREEN_WAIT,
                        "the machine to disable the drive", level -> "still enabled")
                .thenWaitUntil(() -> app(ctx).deviceRowDisabled(CD_ROW), SCREEN_WAIT,
                        "the page to cross the drive out")
                .thenScreenshot(2, "kde-devices-disabled")
                .then(SETTLE, () -> ctx.clickDesktop(point(ctx, app(ctx).infoCenterTabCenter(0))))
                .thenWaitUntil(() -> app(ctx).deviceRows().isEmpty(), SCREEN_WAIT,
                        "the About tab to put its page back up");
    }

    private static ThisPcApp app(final ClientTestContext ctx) {
        final DesktopScreen desktop = ctx.screen(DesktopScreen.class);
        final DesktopWindow window = desktop == null ? null : desktop.windowFor(INFO_CENTER);
        return window != null && window.app() instanceof ThisPcApp thisPc ? thisPc : null;
    }

    /* A content-local point of the Info Center as a point on the desktop: inside its frame, under its title. */
    private static int[] point(final ClientTestContext ctx, final int[] local) {
        final DesktopWindow window = ctx.screen(DesktopScreen.class).windowFor(INFO_CENTER);
        if (window == null || local == null) {
            throw new ClientTestFailure("the Info Center or the place in it is gone");
        }
        return DesktopSteps.contentPoint(window, local);
    }
}
