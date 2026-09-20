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
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

/**
 * Picking text out of a terminal with the pointer and copying it, the way a player does it: drag across what
 * the machine printed, press Ctrl+C, and find it on the clipboard.
 *
 * <p>A real terminal window on a real desktop, because what is being tested is the pointer reaching the right
 * cells of the glass, which is the half the arithmetic on its own cannot answer for.
 */
public final class TerminalSelectionClientTests {

    private TerminalSelectionClientTests() {
    }

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 80;
    private static final int BOOT_WAIT = 400;

    private static final BlockPos COMPUTER = new BlockPos(5, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 2);
    private static final ResourceLocation FRAMES_XP =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "frames_xp");
    private static final String TERMINAL = "Command Prompt";

    /** Something nobody would have on the clipboard already, so finding it there means the copy did it. */
    private static final String SAID = "picked-out-by-the-pointer";

    private static ShellApp shell(final ClientTestContext ctx) {
        final DesktopScreen desktop = ctx.screen(DesktopScreen.class);
        final DesktopWindow window = desktop == null ? null : desktop.windowFor(TERMINAL);
        final IDesktopApp app = window == null ? null : window.app();
        return app instanceof ShellApp s ? s : null;
    }

    private static void launch(final ClientTestContext ctx, final String label) {
        final DesktopScreen desktop = ctx.screen(DesktopScreen.class);
        ctx.click(desktop.startButtonX(), desktop.startButtonY());
        final int item = desktop.launcherLabels().indexOf(label);
        ctx.click(desktop.startMenuItemX(item), desktop.startMenuItemY(item));
    }

    private static String clipboard() {
        final String text = Minecraft.getInstance().keyboardHandler.getClipboard();
        return text == null ? "" : text;
    }

    /**
     * Dragging across a printed row picks it out, and Ctrl+C puts it on the clipboard. With nothing picked
     * out the same key is still the interrupt it has always been, which is why the drag comes first.
     */
    @ClientTest(timeoutTicks = 2400)
    public static void dragging_picksOutARowAndControlCCopiesIt(final ClientTestContext ctx) {
        ctx.thenBuild(0, world -> {
                    final CraftingComputerBlockEntity computer = world.placeRunningCraftingComputer(COMPUTER);
                    computer.installOs(FRAMES_XP);
                    world.placeMonitor(MONITOR, Direction.EAST);
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .thenWaitUntil(() -> ctx.screen(DesktopScreen.class).launcherLabels().contains(TERMINAL),
                        SCREEN_WAIT, "the terminal to be listed in Start")
                .then(0, () -> launch(ctx, TERMINAL))
                .thenWaitUntil(() -> shell(ctx) != null, SCREEN_WAIT, "the terminal window to open")
                .then(SETTLE, () -> ctx.type("echo " + SAID))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenWaitUntil(() -> shell(ctx).scrollbackText().contains(SAID), SCREEN_WAIT,
                        "the machine to print what is to be picked out")
                .then(SETTLE, () -> Minecraft.getInstance().keyboardHandler.setClipboard(""))
                .then(1, () -> dragAcrossTheEcho(ctx))
                .thenScreenshot(2, "terminal-selection")
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_C, GLFW.GLFW_MOD_CONTROL))
                .thenAssert(1, () -> clipboard().contains(SAID),
                        "what was dragged across is on the clipboard")
                .thenAssert(0, () -> !clipboard().endsWith(" "),
                        "and it is cut at what the row says, not at the width of the glass");
    }

    /**
     * Drags across everything the terminal is showing, from the top of its glass to the last row above the
     * prompt.
     *
     * <p>The whole glass rather than the one row the answer is on: which row that is depends on how the glass
     * scrolled and how the lines wrapped, and none of that is what this is holding to account. What is, is
     * that a drag across printed rows picks them out and that the key copies them.
     */
    private static void dragAcrossTheEcho(final ClientTestContext ctx) {
        final DesktopWindow window = ctx.screen(DesktopScreen.class).windowFor(TERMINAL);
        /*
         * Well inside the glass at both ends: starting on the title bar would move the window instead, and
         * the rows the machine has printed sit at the top of a glass that has not scrolled yet.
         */
        ctx.dragDesktop(new int[] {window.x() + 8, window.y() + 52},
                new int[] {window.x() + window.width() - 12, window.y() + 62});
    }
}
