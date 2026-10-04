/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.client.os.DesktopWindow;
import dev.jstech.computers.client.os.TaskManagerApp;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/**
 * The graphics card on screen: the Task Manager of each system shows its part (Frames XP's video memory meter and
 * history, Frames 11's GPU page with what holds the card's memory, the Plasma monitor's GPU card), and a graphics
 * program does not open when the card has no memory left for it, and says why.
 */
public final class GpuClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 160;
    private static final int BOOT_WAIT = 1_200;
    private static final BlockPos COMPUTER = new BlockPos(5, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 2);
    private static final String TASK_MANAGER = "jsc:task_manager";
    /* A wall of forty-eight Standard monitors, its screens facing south, the computer behind its corner. */
    private static final BlockPos WALL = new BlockPos(2, 2, 2);
    private static final BlockPos WALL_COMPUTER = new BlockPos(2, 2, 1);
    /* Within reach of the wall's bottom-left monitor, which is where the player clicks it. */
    private static final BlockPos AT_WALL = new BlockPos(3, 2, 4);

    private GpuClientTests() {
    }

    @ClientTest(timeoutTicks = 2400)
    public static void taskManager_framesXpShowsTheVideoMemoryOnItsPerformancePage(final ClientTestContext ctx) {
        openTaskManager(ctx, "frames_xp")
                .then(SETTLE, () -> ctx.clickDesktop(app(ctx).pageCentre(2)))
                .thenWaitUntil(() -> app(ctx).page() == 2 && app(ctx).showsGraphics(), SCREEN_WAIT,
                        "the Performance page with the card's video memory")
                .thenScreenshot(30, "xp-performance-video-memory");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void taskManager_frames11HasAGpuPageWithWhatHoldsItsMemory(final ClientTestContext ctx) {
        openTaskManager(ctx, "frames_11")
                .then(SETTLE, () -> ctx.clickDesktop(app(ctx).pageCentre(5)))
                .thenWaitUntil(() -> app(ctx).page() == 5 && app(ctx).showsGraphics(), SCREEN_WAIT,
                        "the GPU page with the card")
                .thenScreenshot(30, "11-gpu-page");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void systemMonitor_plasmaShowsTheGpuBesideMemoryAndProcessor(final ClientTestContext ctx) {
        ctx.thenBuild(0, world -> {
                    final PersonalComputerBlockEntity pc = world.placeRunningPersonalComputer(COMPUTER,
                            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "ubuntu"));
                    pc.console().install("jsc:kde_plasma");
                    world.placeMonitor(MONITOR, Direction.EAST);
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .then(SETTLE, () -> DesktopScreen.requestOpen(TASK_MANAGER))
                .thenWaitUntil(() -> app(ctx) != null && app(ctx).showsGraphics(), SCREEN_WAIT,
                        "the System Monitor to show the card on its overview")
                .thenScreenshot(30, "plasma-overview-gpu");
    }

    @ClientTest(timeoutTicks = 3000)
    public static void graphicsProgram_doesNotOpenWithoutVideoMemory(final ClientTestContext ctx) {
        ctx.thenBuild(0, world -> {
                    final PersonalComputerBlockEntity pc = world.placeRunningPersonalComputer(WALL_COMPUTER);
                    pc.console().install("jsc:paint");
                    // Forty-eight Standard monitors hold the whole three gigabytes of its card.
                    for (int v = 0; v < 6; v++) {
                        for (int u = 0; u < 8; u++) {
                            world.placeMonitor(WALL.east(u).above(v), Direction.NORTH);
                        }
                    }
                })
                .thenTeleport(SETTLE, AT_WALL, Direction.NORTH)
                .thenRightClick(SETTLE, WALL)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .thenWaitUntil(() -> !ctx.screen(DesktopScreen.class).launcherLabels().isEmpty(), SCREEN_WAIT,
                        "the desktop to list its programs")
                .then(SETTLE, () -> DesktopScreen.requestOpen("jsc:paint"))
                .thenWaitUntil(() -> !ctx.screen(DesktopScreen.class).balloonTitle().isEmpty(), SCREEN_WAIT,
                        "the notification area to say there is no video memory for it")
                .thenScreenshot(2, "paint-refused")
                .thenAssert(0, () -> !ctx.screen(DesktopScreen.class).openWindowLabels().contains("Paint"),
                        "Paint did not open");
    }

    /* A running computer with that system on it and a monitor, its desktop up and its Task Manager open. */
    private static ClientTestContext openTaskManager(final ClientTestContext ctx, final String system) {
        return ctx.thenBuild(0, world -> {
                    world.placeRunningPersonalComputer(COMPUTER,
                            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, system));
                    world.placeMonitor(MONITOR, Direction.EAST);
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .thenWaitUntil(() -> !ctx.screen(DesktopScreen.class).launcherLabels().isEmpty(), SCREEN_WAIT,
                        "the desktop to list its programs")
                .then(SETTLE, () -> DesktopScreen.requestOpen(TASK_MANAGER))
                .thenWaitUntil(() -> app(ctx) != null && app(ctx).pageCentre(0) != null, SCREEN_WAIT,
                        "the Task Manager to open");
    }

    private static TaskManagerApp app(final ClientTestContext ctx) {
        final DesktopScreen desktop = ctx.screen(DesktopScreen.class);
        final DesktopWindow window = desktop == null ? null : desktop.windowFor(TASK_MANAGER);
        return window != null && window.app() instanceof TaskManagerApp manager ? manager : null;
    }
}
