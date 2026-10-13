/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.client.os.AutomationManagerApp;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.client.os.DesktopWindow;
import dev.jstech.computers.program.Programs;
import dev.jstech.tests.testkit.TestWorldBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * The Automation Manager on a Frames XP desktop, which it now reaches back to: the Automation Engine online on the
 * Mainframe, and the form's first kind of job, Restock below, saying it does not count what is on its way.
 */
public final class AutomationManagerClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 160;
    private static final int BOOT_WAIT = 400;

    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 2);

    private static final ResourceLocation FRAMES_XP =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "frames_xp");

    private AutomationManagerClientTests() {
    }

    @ClientTest(timeoutTicks = 1600)
    public static void automationManager_onFramesXpOffersRestockBelow(final ClientTestContext ctx) {
        ctx.thenBuild(0, world -> {
                    final TestWorldBuilder.CraftingNetwork wired = world.buildCraftingNetwork();
                    wired.mainframe().installAutomationEngine();
                    wired.cc().installOs(FRAMES_XP);
                    wired.cc().console().install(Programs.AUTOMATION_MANAGER.toString());
                    world.placeMonitor(MONITOR, Direction.EAST);
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                // The desktop opens a program it has been told is installed; that word comes after the desktop does.
                .thenWaitUntil(() -> ctx.screen(DesktopScreen.class).launcherLabels().contains("Automation Manager"),
                        SCREEN_WAIT, "the desktop to list the Automation Manager")
                .then(SETTLE, () -> DesktopScreen.requestOpen(AutomationManagerApp.TITLE))
                .thenWaitUntil(() -> manager(ctx) != null && manager(ctx).hasState(), SCREEN_WAIT,
                        "the Automation Manager to hear from the Mainframe")
                // The form is laid out when the window draws, and a loaded client can run several ticks between two
                // frames, so the answer can be in before the window has drawn it.
                .thenWaitUntil(() -> manager(ctx).restockNoteShown(), SCREEN_WAIT,
                        "Restock below to say it does not count what is on its way")
                .thenScreenshot(4, "restock-below")
                .then(0, () -> ctx.assertTrue(manager(ctx).engineOnline(), "the Automation Engine shows online"))
                .then(0, () -> ctx.assertTrue(manager(ctx).prophetTipText().contains("KEEP"),
                        "and points whoever wants a level held to Prophet YourIQL's KEEP; got "
                                + manager(ctx).prophetTipText()));
    }

    @Nullable
    private static AutomationManagerApp manager(final ClientTestContext ctx) {
        final DesktopScreen desktop = ctx.screen(DesktopScreen.class);
        if (desktop == null) {
            return null;
        }
        final DesktopWindow window = desktop.windowFor(AutomationManagerApp.TITLE);
        return window != null && window.app() instanceof AutomationManagerApp app ? app : null;
    }
}
