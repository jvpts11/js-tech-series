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
import dev.jstech.computers.client.os.IDesktopApp;
import dev.jstech.computers.client.os.ShellApp;
import dev.jstech.tests.testkit.TestWorldBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

/**
 * A terminal window on a desktop is narrower than a monitor, and what the machine prints has to fit it.
 *
 * <p>The machine lays its answers out in columns and has no glass of its own to measure, so the window tells
 * it how wide it is with every line. Told nothing, the machine wrote to a monitor's width and the window
 * folded every row of a directory listing in half, leaving a line of leader dots on its own under each name.
 * That is what this watches for, in a real window on a real desktop.
 */
public final class TerminalWidthClientTests {

    private TerminalWidthClientTests() {
    }

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 80;
    private static final int BOOT_WAIT = 400;

    private static final BlockPos COMPUTER = new BlockPos(5, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 2);
    /**
     * The edition this is read on.
     *
     * <p>Any of them would do: the window tells the machine how wide it is whatever desktop it is drawn by,
     * and what broke was the telling. This one is the desktop the rest of these tests drive.
     */
    private static final ResourceLocation FRAMES_XP =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "frames_xp");

    /**
     * What this desktop calls its terminal, asked of the desktop rather than assumed.
     *
     * <p>Each family names it its own way and the newest Frames calls it Megashell, so a test that went
     * looking for a Command Prompt would only ever find one on the older editions.
     */
    private static String terminal(final ClientTestContext ctx) {
        return DesktopScreen.terminalName();
    }

    private static ShellApp shell(final ClientTestContext ctx) {
        final DesktopScreen desktop = ctx.screen(DesktopScreen.class);
        final DesktopWindow window = desktop == null ? null : desktop.windowFor(terminal(ctx));
        final IDesktopApp app = window == null ? null : window.app();
        return app instanceof ShellApp s ? s : null;
    }

    private static void launch(final ClientTestContext ctx) {
        final DesktopScreen desktop = ctx.screen(DesktopScreen.class);
        ctx.click(desktop.startButtonX(), desktop.startButtonY());
        final int item = desktop.launcherLabels().indexOf(terminal(ctx));
        ctx.click(desktop.startMenuItemX(item), desktop.startMenuItemY(item));
    }

    /**
     * A row that is nothing but the tail of the row above it: spaces, leader dots and nothing to read.
     *
     * <p>Which is exactly what a listing written too wide leaves behind, and nothing a listing written to
     * fit the glass ever produces.
     */
    private static boolean foldedRow(final String row) {
        final String bare = row.trim();
        return bare.length() > 2 && bare.chars().allMatch(c -> c == '.' || c == ' ' || c == '-');
    }

    /** Whether one row of the glass holds that text whole, rather than it being split across two. */
    private static boolean oneRowHolds(final ClientTestContext ctx, final String text) {
        for (final String row : shell(ctx).scrollbackText().split("\n")) {
            if (row.contains(text)) {
                return true;
            }
        }
        return false;
    }

    /** A directory listing in a terminal window reads as columns, not as rows folded in half. */
    @ClientTest(timeoutTicks = 2400)
    public static void aListing_fitsTheTerminalWindowItIsReadIn(final ClientTestContext ctx) {
        ctx.thenBuild(0, world -> {
                    final CraftingComputerBlockEntity computer = world.placeRunningCraftingComputer(COMPUTER);
                    TestWorldBuilder.installDesktop(computer, FRAMES_XP);
                    computer.togglePower();
                    computer.togglePower();
                    world.placeMonitor(MONITOR, Direction.EAST);
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .thenWaitUntil(() -> ctx.screen(DesktopScreen.class).launcherLabels().contains(terminal(ctx)),
                        SCREEN_WAIT, "the terminal to be listed in Start")
                .then(0, () -> launch(ctx))
                .thenWaitUntil(() -> shell(ctx) != null, SCREEN_WAIT, "the terminal window to open")
                .then(SETTLE, () -> ctx.type("dir"))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenWaitUntil(() -> shell(ctx).scrollbackText().contains("Frames"), SCREEN_WAIT,
                        "the machine to list what is on the disk")
                .thenScreenshot(2, "terminal-width-dir")
                .then(0, () -> {
                    for (final String row : shell(ctx).scrollbackText().split("\n")) {
                        ctx.assertTrue(!foldedRow(row),
                                "a row folded in half: [" + row + "] in\n" + shell(ctx).scrollbackText());
                    }
                })
                /*
                 * The longest name on a fresh disk, whole and on one row. It is the one that used to break
                 * first, and a name a player cannot read whole is a name they cannot type back.
                 */
                .then(0, () -> ctx.assertTrue(oneRowHolds(ctx, "Program Files (x86)"),
                        "the longest name is on one row; got\n" + shell(ctx).scrollbackText()))
                .then(0, () -> ctx.assertTrue(shell(ctx).scrollbackText().contains("<DIR>"),
                        "and the listing is still a listing; got\n" + shell(ctx).scrollbackText()));
    }
}
