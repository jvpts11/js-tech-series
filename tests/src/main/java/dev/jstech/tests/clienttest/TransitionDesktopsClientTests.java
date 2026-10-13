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
import dev.jstech.computers.client.os.SoundfoundryApp;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/**
 * KDE and GNOME as they looked on Transition hardware: KDE 4 with its Plasma panel, Kickoff, the pager, the Folder View
 * and the cashew, Oxygen windows; GNOME 2 with its two Clearlooks panels, its three menus along the top and its
 * switcher along the foot. Each test boots the desktop on a Transition machine, uses what it has and takes a picture.
 */
public final class TransitionDesktopsClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 80;
    /** A Transition machine with a Linux and its desktop takes longer to come up than a current one. */
    private static final int BOOT_WAIT = 1_200;

    private static final BlockPos COMPUTER = new BlockPos(5, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 2);

    /** What each desktop calls the calculator: GNOME by what it is, KDE by its own program's name. */
    private static final String CALCULATOR = "Calculator";
    private static final String KCALC = "KCalc";
    private static final String SOUNDFOUNDRY = "Soundfoundry";

    private TransitionDesktopsClientTests() {
    }

    @ClientTest(timeoutTicks = 3600)
    public static void kde4_kickoffOpensFromTheOrbAndItsTabsChangeTheList(final ClientTestContext ctx) {
        booted(ctx, "jsc:kde_plasma")
                .then(2, () -> ctx.assertTrue("OXYGEN".equals(desktop(ctx).formShown()),
                        "KDE on a Transition machine wears Oxygen; got " + desktop(ctx).formShown()))
                .thenScreenshot(2, "kde4-desktop")
                .then(SETTLE, () -> ctx.click(desktop(ctx).startButtonX(), desktop(ctx).startButtonY()))
                .thenAssert(2, () -> desktop(ctx).isStartOpen() && "FAVORITES".equals(desktop(ctx).kickoffTab()),
                        "Kickoff opens from the orb on Favorites")
                .thenScreenshot(2, "kde4-kickoff")
                .then(SETTLE, () -> click(ctx, desktop(ctx).kickoffTabPoint("APPLICATIONS")))
                .then(2, () -> ctx.assertTrue("APPLICATIONS".equals(desktop(ctx).kickoffTab())
                                && desktop(ctx).kickoffRows().contains(KCALC),
                        "Applications lists every program; got " + desktop(ctx).kickoffRows()))
                .then(SETTLE, () -> click(ctx, desktop(ctx).kickoffTabPoint("LEAVE")))
                .thenAssert(2, () -> desktop(ctx).kickoffRows().size() == 2,
                        "Leave offers shutting down and restarting")
                .thenScreenshot(2, "kde4-kickoff-leave");
    }

    @ClientTest(timeoutTicks = 3600)
    public static void kde4_pagerSwitchesTheWorkspaceAndTheCashewOffersTheDesktop(final ClientTestContext ctx) {
        booted(ctx, "jsc:kde_plasma")
                .then(SETTLE, () -> click(ctx, desktop(ctx).workspacePoint(1)))
                .thenAssert(2, () -> desktop(ctx).workspaceShown() == 1, "the pager puts the second workspace up")
                .then(SETTLE, () -> click(ctx, desktop(ctx).cashewPoint()))
                .then(2, () -> ctx.assertTrue(desktop(ctx).taskMenuLabels().contains("Desktop Settings"),
                        "the cashew offers the desktop's settings; got " + desktop(ctx).taskMenuLabels()))
                .thenScreenshot(2, "kde4-cashew");
    }

    @ClientTest(timeoutTicks = 3600)
    public static void kde4_soundfoundryIsTheLegacyPlayerOnTheTransition(final ClientTestContext ctx) {
        booted(ctx, "jsc:kde_plasma", "jsc:soundfoundry")
                .then(0, () -> DesktopScreen.requestOpen(SOUNDFOUNDRY))
                .thenWaitUntil(() -> desktop(ctx).windowFor(SOUNDFOUNDRY) != null, SCREEN_WAIT, "Soundfoundry")
                .thenAssert(2, () -> desktop(ctx).windowFor(SOUNDFOUNDRY).app() instanceof SoundfoundryApp,
                        "a Transition desktop runs the player of its time");
    }

    @ClientTest(timeoutTicks = 3600)
    public static void gnome2_menusAlongTheTopAndTheSwitcherAlongTheFoot(final ClientTestContext ctx) {
        booted(ctx, "jsc:gnome")
                .then(2, () -> ctx.assertTrue("CLEARLOOKS".equals(desktop(ctx).formShown()),
                        "GNOME on a Transition machine wears Clearlooks; got " + desktop(ctx).formShown()))
                .thenScreenshot(2, "gnome2-desktop")
                .then(SETTLE, () -> click(ctx, desktop(ctx).gnome2MenuPoint(0)))
                .then(2, () -> ctx.assertTrue(desktop(ctx).taskMenuLabels().contains("Accessories"),
                        "Applications groups the programs by what they are for; got "
                                + desktop(ctx).taskMenuLabels()))
                .thenScreenshot(2, "gnome2-applications")
                // A click away from an open menu puts it away, as it did; the next one opens System.
                .then(SETTLE, () -> click(ctx, desktop(ctx).gnome2MenuPoint(2)))
                .thenAssert(2, () -> !desktop(ctx).isTaskMenuOpen(), "a click away puts the menu away")
                .then(SETTLE, () -> click(ctx, desktop(ctx).gnome2MenuPoint(2)))
                .then(2, () -> ctx.assertTrue(desktop(ctx).taskMenuLabels().contains("Preferences"),
                        "System holds the preferences; got " + desktop(ctx).taskMenuLabels()))
                .then(SETTLE, () -> click(ctx, desktop(ctx).workspacePoint(2)))
                .then(SETTLE, () -> click(ctx, desktop(ctx).workspacePoint(2)))
                .thenAssert(2, () -> desktop(ctx).workspaceShown() == 2, "the switcher puts the third workspace up")
                .thenScreenshot(2, "gnome2-workspace");
    }

    /**
     * A Transition machine running Ubuntu with {@code desktopPackage} on it and the other {@code packages}, at its
     * desktop, the player in front.
     */
    private static ClientTestContext booted(final ClientTestContext ctx, final String desktopPackage,
                                            final String... packages) {
        return ctx.thenBuild(0, world -> {
                    final CraftingComputerBlockEntity computer = world.placeRunningTransitionCraftingComputer(COMPUTER,
                            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "ubuntu"));
                    computer.console().install(desktopPackage);
                    for (final String each : packages) {
                        computer.console().install(each);
                    }
                    world.placeMonitor(MONITOR, Direction.EAST);
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .thenWaitUntil(() -> desktop(ctx).launcherLabels().contains(CALCULATOR)
                        || desktop(ctx).launcherLabels().contains(KCALC), SCREEN_WAIT, "the calculator to be listed");
    }

    private static DesktopScreen desktop(final ClientTestContext ctx) {
        return ctx.screen(DesktopScreen.class);
    }

    private static void click(final ClientTestContext ctx, final int[] point) {
        ctx.assertTrue(point != null, "the point to click is on the screen");
        ctx.click(point[0], point[1]);
    }
}
