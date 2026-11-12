/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.computers;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/**
 * The Disk Defragmenter of each Frames edition in its own look (computers/docs/design/storage.md), opened on a running
 * machine and shot for the side-by-side with its mocks.
 */
public final class DefragClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 120;
    private static final int BOOT_WAIT = 1_200;
    /* Long enough for the tool to ask the machine for its drives and draw what came back. */
    private static final int REPORT_WAIT = 40;
    private static final BlockPos COMPUTER = new BlockPos(5, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 2);
    private static final String DEFRAG = "jsc:defrag";

    private DefragClientTests() {
    }

    @ClientTest(timeoutTicks = 2400)
    public static void defrag_frames95(final ClientTestContext ctx) {
        opened(ctx, "frames_95");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void defrag_framesXp(final ClientTestContext ctx) {
        opened(ctx, "frames_xp");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void defrag_frames7(final ClientTestContext ctx) {
        opened(ctx, "frames_7");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void defrag_frames10(final ClientTestContext ctx) {
        opened(ctx, "frames_10");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void defrag_frames11(final ClientTestContext ctx) {
        opened(ctx, "frames_11");
    }

    /* A running computer of {@code system} with a monitor, its desktop up, its Disk Defragmenter opened and shot. */
    private static void opened(final ClientTestContext ctx, final String system) {
        ctx.thenBuild(0, world -> {
                    world.placeRunningPersonalComputer(COMPUTER,
                            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, system));
                    world.placeMonitor(MONITOR, Direction.EAST);
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .thenWaitUntil(() -> !ctx.screen(DesktopScreen.class).launcherLabels().isEmpty(), SCREEN_WAIT,
                        "the desktop to list its programs")
                .then(SETTLE, () -> DesktopScreen.requestOpen(DEFRAG))
                .thenWaitUntil(() -> ctx.screen(DesktopScreen.class).windowFor(DEFRAG) != null, SCREEN_WAIT,
                        "the Disk Defragmenter to open in a window")
                .thenScreenshot(REPORT_WAIT, "defrag-" + system.replace('_', '-'));
    }
}
