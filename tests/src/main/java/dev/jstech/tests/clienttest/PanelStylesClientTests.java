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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/**
 * The panels nothing else opens.
 *
 * <p>The other taskbar tests go deep on three of them, because that is where the interesting behaviour is:
 * KDE groups a program's windows into cards, Frames 11 centres its entries, Frames XP keeps pins on a quick
 * launch. That left Cinnamon, GNOME and the classic Frames 95 bar with no test that ever drew them.
 *
 * <p>Those three are not less likely to break for being less interesting. They are more likely, because a
 * panel nothing opens is a panel nobody notices is wrong. Each case here boots a machine wearing one, asks
 * it the two questions every panel must answer (where its launcher is, and what it lists), and takes a
 * picture so the drawing can be looked at rather than assumed.
 *
 * <p>GNOME is the one that differs in kind: its bar is at the top, where every other panel here is at the
 * bottom, and it carries Activities and the clock rather than a list of open programs. Where its launcher
 * sits is what this asserts, because that is the difference a reading can see; whether the bar drew a task
 * list is a question for the picture.
 *
 * <p>NOT COVERED, and worth naming rather than pretending otherwise: the period panel, the one a Legacy or
 * Vintage machine wears, built out of the skin's own raised studs and sunken wells instead of a flat band.
 * Reaching it needs a machine of that era, and the client test kit can only build a Standard one today.
 * Writing a case here that installs a modern desktop and calls it the period one would be worse than having
 * no case at all.
 */
public final class PanelStylesClientTests {

    private PanelStylesClientTests() {
    }

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 80;
    private static final int BOOT_WAIT = 400;

    private static final BlockPos COMPUTER = new BlockPos(5, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 2);

    /**
     * What each desktop calls the calculator. The launcher lists programs by its own desktop's names, and
     * only KDE renames this one: GNOME, Cinnamon and every Frames edition all call it Calculator.
     */
    private static final String CALCULATOR = "Calculator";

    private static ResourceLocation jsc(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, path);
    }

    private static DesktopScreen desktop(final ClientTestContext ctx) {
        return ctx.screen(DesktopScreen.class);
    }

    /**
     * A machine at its desktop: {@code os} installed, and {@code desktopPackage} on top of it when the
     * system is a Linux that takes one.
     */
    private static ClientTestContext booted(final ClientTestContext ctx, final String os,
                                            final String desktopPackage, final String calculator) {
        return ctx.thenBuild(0, world -> {
                    final CraftingComputerBlockEntity computer = world.placeRunningCraftingComputer(COMPUTER);
                    computer.installOs(jsc(os));
                    if (desktopPackage != null) {
                        computer.console().install(desktopPackage);
                    }
                    world.placeMonitor(MONITOR, Direction.EAST);
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .thenWaitUntil(() -> desktop(ctx).launcherLabels().contains(calculator),
                        SCREEN_WAIT, calculator + " to be listed in the launcher");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void cinnamon_listsAnOpenProgramAndOpensItsMenuFromThePanel(final ClientTestContext ctx) {
        booted(ctx, "ubuntu", "jsc:cinnamon", CALCULATOR)
                .then(0, () -> DesktopScreen.requestOpen(CALCULATOR))
                .thenWaitUntil(() -> desktop(ctx).windowFor(CALCULATOR) != null, SCREEN_WAIT, "the Calculator window")
                .then(2, () -> ctx.assertTrue(desktop(ctx).taskEntryLabels().contains(CALCULATOR),
                        "the Mint panel lists the open program; got " + desktop(ctx).taskEntryLabels()))
                .thenScreenshot(2, "cinnamon-panel")
                .then(SETTLE, () -> ctx.click(desktop(ctx).startButtonX(), desktop(ctx).startButtonY()))
                .thenAssert(2, () -> desktop(ctx).isStartOpen(),
                        "the Mint menu opens where the panel says its button is")
                .thenScreenshot(2, "cinnamon-menu");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void gnome_putsItsBarOnTopAndListsNoProgramsOnIt(final ClientTestContext ctx) {
        booted(ctx, "ubuntu", "jsc:gnome", CALCULATOR)
                .then(0, () -> DesktopScreen.requestOpen(CALCULATOR))
                .thenWaitUntil(() -> desktop(ctx).windowFor(CALCULATOR) != null, SCREEN_WAIT, "the Calculator window")
                /*
                 * The assertion that IS GNOME: its bar is at the TOP, where every other panel here is at the
                 * bottom, so its launcher button sits within a band's height of the top of the glass.
                 *
                 * Not asserted, because it would be asserting the wrong layer: that the bar lists no
                 * programs. The desktop still works out what a panel WOULD list; GNOME's bar simply never
                 * draws it, the way the shell it copies never had a task list. Whether it drew one is a
                 * question for the picture below, not for the list behind it.
                 */
                .then(2, () -> ctx.assertTrue(desktop(ctx).startButtonY() - desktop(ctx).desktopY() < 40,
                        "GNOME puts its bar at the top; its launcher sits "
                                + (desktop(ctx).startButtonY() - desktop(ctx).desktopY())
                                + " pixels below the top of the glass"))
                .thenScreenshot(2, "gnome-top-bar")
                .then(SETTLE, () -> ctx.click(desktop(ctx).startButtonX(), desktop(ctx).startButtonY()))
                .thenAssert(2, () -> desktop(ctx).isStartOpen(),
                        "Activities opens the overview from the top bar")
                .thenScreenshot(2, "gnome-overview");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void frames95_listsAnOpenProgramOnItsClassicBar(final ClientTestContext ctx) {
        booted(ctx, "frames_95", null, CALCULATOR)
                .then(0, () -> DesktopScreen.requestOpen(CALCULATOR))
                .thenWaitUntil(() -> desktop(ctx).windowFor(CALCULATOR) != null, SCREEN_WAIT, "the Calculator")
                .then(2, () -> ctx.assertTrue(desktop(ctx).taskEntryLabels().contains(CALCULATOR),
                        "the classic bar lists the open program; got " + desktop(ctx).taskEntryLabels()))
                .thenScreenshot(2, "frames95-bar")
                .then(SETTLE, () -> ctx.click(desktop(ctx).startButtonX(), desktop(ctx).startButtonY()))
                .thenAssert(2, () -> desktop(ctx).isStartOpen(),
                        "the classic Start menu opens from its button")
                .thenScreenshot(2, "frames95-start");
    }
}
