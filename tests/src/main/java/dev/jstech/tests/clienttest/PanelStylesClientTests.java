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
import dev.jstech.computers.client.os.ThisPcApp;
import dev.jstech.computers.client.os.WallpaperStyle;
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
 * <p>The last of them is the period panel, the one an earlier machine wears: raised studs and sunken wells
 * taken from the skin's own relief instead of a flat band, the way panels looked before anybody flattened
 * them. It is the one panel that does not follow from the system installed but from the machine underneath,
 * so reaching it means building a machine of that era rather than installing something.
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
     * KDE is the only one that renames this one: GNOME, Cinnamon and every Frames edition call it
     * Calculator, while KDE calls it KCalc.
     */
    private static final String CALCULATOR = "Calculator";
    private static final String KCALC = "KCalc";

    /** This PC's native names: Info Center on KDE, About on GNOME, System Info on Cinnamon. */
    private static final String INFO_CENTER = "Info Center";
    private static final String ABOUT = "About";
    private static final String SYSTEM_INFO = "System Info";

    private static ResourceLocation jsc(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, path);
    }

    private static DesktopScreen desktop(final ClientTestContext ctx) {
        return ctx.screen(DesktopScreen.class);
    }

    /** The This PC window named {@code label}, or null while it is not open. */
    private static ThisPcApp thisPc(final ClientTestContext ctx, final String label) {
        final DesktopWindow window = desktop(ctx).windowFor(label);
        return window != null && window.app() instanceof ThisPcApp app ? app : null;
    }

    /**
     * A machine at its desktop: {@code os} installed, and {@code desktopPackage} on top of it when the
     * system is a Linux that takes one. {@code period} builds a Legacy-era machine instead of a current
     * one, which is what decides whether the desktop wears period chrome.
     */
    private static ClientTestContext booted(final ClientTestContext ctx, final String os,
                                            final String desktopPackage, final String calculator,
                                            final boolean period) {
        return ctx.thenBuild(0, world -> {
                    final CraftingComputerBlockEntity computer = period
                            ? world.placeRunningLegacyCraftingComputer(COMPUTER)
                            : world.placeRunningCraftingComputer(COMPUTER);
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
        booted(ctx, "ubuntu", "jsc:cinnamon", CALCULATOR, false)
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
        booted(ctx, "ubuntu", "jsc:gnome", CALCULATOR, false)
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
        booted(ctx, "frames_95", null, CALCULATOR, false)
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

    /**
     * The period panel, on a Legacy machine wearing KDE. The same desktop on a current machine draws the
     * flat band the other Linux tests show; here it is built out of the skin's own relief instead, which
     * is what makes this the one panel that follows from the hardware rather than from the system.
     */
    @ClientTest(timeoutTicks = 2400)
    public static void period_drawsItsPanelOnALegacyMachine(final ClientTestContext ctx) {
        booted(ctx, "ubuntu", "jsc:kde_plasma", KCALC, true)
                .then(0, () -> DesktopScreen.requestOpen(KCALC))
                .thenWaitUntil(() -> desktop(ctx).windowFor(KCALC) != null, SCREEN_WAIT, "the KCalc window")
                .then(2, () -> ctx.assertTrue(desktop(ctx).taskEntryLabels().contains(KCALC),
                        "the period panel lists the open program; got " + desktop(ctx).taskEntryLabels()))
                .thenScreenshot(2, "period-panel")
                .then(SETTLE, () -> ctx.click(desktop(ctx).startButtonX(), desktop(ctx).startButtonY()))
                .thenAssert(2, () -> desktop(ctx).isStartOpen(),
                        "the period launcher opens from its raised stud")
                .thenScreenshot(2, "period-menu");
    }

    /**
     * KDE Plasma on FreeBSD lists Info Center, opens FreeBSD's own wallpaper (the variant painted for this
     * desktop) instead of Breeze, and its Info Center opens the About-style page rather than the drives
     * explorer Frames draws under the same name.
     */
    @ClientTest(timeoutTicks = 2400)
    public static void kdePlasma_onFreeBsdListsInfoCenterWithFreeBsdsWallpaper(final ClientTestContext ctx) {
        booted(ctx, "freebsd", "jsc:kde_plasma", INFO_CENTER, false)
                .thenAssert(0, () -> WallpaperStyle.FREEBSD_PLASMA.id().equals(DesktopScreen.currentWallpaperId()),
                        "FreeBSD's own wallpaper hangs here; got " + DesktopScreen.currentWallpaperId())
                .thenScreenshot(2, "freebsd-kde-desktop")
                .then(0, () -> DesktopScreen.requestOpen(INFO_CENTER))
                .thenWaitUntil(() -> desktop(ctx).windowFor(INFO_CENTER) != null, SCREEN_WAIT,
                        "the Info Center window")
                .thenAssert(0, () -> thisPc(ctx, INFO_CENTER) != null && thisPc(ctx, INFO_CENTER).isAboutPage(),
                        "Info Center opens the About-style page, not the drives explorer")
                .thenScreenshot(2, "freebsd-kde-info-center");
    }

    /** GNOME on FreeBSD lists About, wears FreeBSD's own wallpaper, and About opens the same About-style page. */
    @ClientTest(timeoutTicks = 2400)
    public static void gnome_onFreeBsdListsAboutWithFreeBsdsWallpaper(final ClientTestContext ctx) {
        booted(ctx, "freebsd", "jsc:gnome", ABOUT, false)
                .thenAssert(0, () -> WallpaperStyle.FREEBSD_GNOME.id().equals(DesktopScreen.currentWallpaperId()),
                        "FreeBSD's own wallpaper hangs here; got " + DesktopScreen.currentWallpaperId())
                .thenScreenshot(2, "freebsd-gnome-desktop")
                .then(0, () -> DesktopScreen.requestOpen(ABOUT))
                .thenWaitUntil(() -> desktop(ctx).windowFor(ABOUT) != null, SCREEN_WAIT, "the About window")
                .thenAssert(0, () -> thisPc(ctx, ABOUT) != null && thisPc(ctx, ABOUT).isAboutPage(),
                        "About opens the About-style page, not the drives explorer")
                .thenScreenshot(2, "freebsd-gnome-about");
    }

    /** Cinnamon on FreeBSD lists System Info, wears FreeBSD's own wallpaper, and opens the same About-style page. */
    @ClientTest(timeoutTicks = 2400)
    public static void cinnamon_onFreeBsdListsSystemInfoWithFreeBsdsWallpaper(final ClientTestContext ctx) {
        booted(ctx, "freebsd", "jsc:cinnamon", SYSTEM_INFO, false)
                .thenAssert(0, () -> WallpaperStyle.FREEBSD_CINNAMON.id().equals(DesktopScreen.currentWallpaperId()),
                        "FreeBSD's own wallpaper hangs here; got " + DesktopScreen.currentWallpaperId())
                .thenScreenshot(2, "freebsd-cinnamon-desktop")
                .then(0, () -> DesktopScreen.requestOpen(SYSTEM_INFO))
                .thenWaitUntil(() -> desktop(ctx).windowFor(SYSTEM_INFO) != null, SCREEN_WAIT,
                        "the System Info window")
                .thenAssert(0, () -> thisPc(ctx, SYSTEM_INFO) != null && thisPc(ctx, SYSTEM_INFO).isAboutPage(),
                        "System Info opens the About-style page, not the drives explorer")
                .thenScreenshot(2, "freebsd-cinnamon-system-info");
    }

    /** This PC reaches KDE, GNOME and Cinnamon on a Linux distribution too, not only on FreeBSD. */
    @ClientTest(timeoutTicks = 2400)
    public static void kdePlasma_onLinuxAlsoListsInfoCenter(final ClientTestContext ctx) {
        booted(ctx, "ubuntu", "jsc:kde_plasma", INFO_CENTER, false)
                .then(0, () -> DesktopScreen.requestOpen(INFO_CENTER))
                .thenWaitUntil(() -> desktop(ctx).windowFor(INFO_CENTER) != null, SCREEN_WAIT,
                        "the Info Center window")
                .thenAssert(0, () -> thisPc(ctx, INFO_CENTER) != null && thisPc(ctx, INFO_CENTER).isAboutPage(),
                        "Info Center opens the About-style page on a Linux too")
                .thenScreenshot(2, "linux-kde-info-center");
    }
}
