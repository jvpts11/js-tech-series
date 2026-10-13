/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.client.os.DesktopWindow;
import dev.jstech.computers.client.os.SettingsApp;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * The Personalize page's content can grow past its window (every offered wallpaper, plus the flat skin's
 * taskbar and appearance rows): the wheel has to move that content, not just the scroll thumb drawn over it.
 */
public final class SettingsPersonalizeClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 80;
    private static final int BOOT_WAIT = 400;
    private static final int SCROLL_NOTCHES = 8;
    private static final String SETTINGS = "Settings";

    private static final BlockPos COMPUTER = new BlockPos(5, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 2);
    private static final ResourceLocation FRAMES_11 =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "frames_11");

    private SettingsPersonalizeClientTests() {
    }

    @ClientTest(timeoutTicks = 2400)
    public static void personalize_scrollingBringsTheDarkModeButtonIntoTheDefaultWindow(final ClientTestContext ctx) {
        ctx.thenBuild(0, world -> {
                    final CraftingComputerBlockEntity computer = world.placeRunningCraftingComputer(COMPUTER);
                    computer.installOs(FRAMES_11);
                    world.placeMonitor(MONITOR, Direction.EAST);
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .then(SETTLE, () -> DesktopScreen.requestOpen(SETTINGS))
                .thenWaitUntil(() -> settings(ctx) != null && settings(ctx).appearanceDarkCenter()[0] != 0,
                        SCREEN_WAIT, "the Settings window with its Personalize page and Appearance row built")
                .then(SETTLE, () -> pointAtDesktop(ctx, settings(ctx).personalizeScrollCenter()))
                .then(SETTLE, () -> {
                    for (int i = 0; i < SCROLL_NOTCHES; i++) {
                        settings(ctx).mouseScrolled(-1.0);
                    }
                })
                .thenAssert(1, () -> settings(ctx).appearanceDarkFullyShown(),
                        "the dark-mode button lies whole inside the page's visible area once it has scrolled to its"
                                + " last row, not clipped by the page or hidden under the title bar")
                .thenScreenshot(2, "personalize-scrolled");
    }

    @Nullable
    private static SettingsApp settings(final ClientTestContext ctx) {
        final DesktopScreen desktop = ctx.screen(DesktopScreen.class);
        if (desktop == null) {
            return null;
        }
        final DesktopWindow window = desktop.windowFor(SETTINGS);
        return window != null && window.app() instanceof SettingsApp app ? app : null;
    }

    /** Moves the real pointer over a desktop-local point, the way {@link ClientTestContext#clickDesktop} clicks one. */
    private static void pointAtDesktop(final ClientTestContext ctx, final int[] point) {
        final DesktopScreen desktop = ctx.screen(DesktopScreen.class);
        final double scale = desktop.desktopScale();
        ctx.pointAt(desktop.desktopX() + point[0] * scale + 0.5, desktop.desktopY() + point[1] * scale + 0.5);
    }
}
