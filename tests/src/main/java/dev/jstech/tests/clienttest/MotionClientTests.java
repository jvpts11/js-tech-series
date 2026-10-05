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
import dev.jstech.computers.client.os.ActiveDesktop;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.client.os.DesktopWindow;
import dev.jstech.computers.client.os.SceneHandoff;
import dev.jstech.computers.client.os.SettingsApp;
import dev.jstech.computers.gui.layout.EffectsPageLayout;
import dev.jstech.computers.os.DesktopEffects;
import dev.jstech.computers.os.OsMotions;
import dev.jstech.core.client.motion.MotionClock;
import dev.jstech.core.motion.DeclaredMotion;
import dev.jstech.core.motion.MotionKinds;
import dev.jstech.core.motion.MotionProfile;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.jetbrains.annotations.Nullable;

/**
 * The desktops move: a window grows in and a closed one shrinks away, a window goes down to its button and comes
 * back, a launcher slides out of its panel, and a boot picture melts into the desktop. Every motion is slowed to
 * eight times its own length here, which is what a player's speed setting does, so a test sees it under way.
 *
 * <p>Each system's own effects page switches its motions off one by one, the machine keeps the choice, and the
 * desktop stops moving at once. A resource pack's profile replaces the system's own, and the files the mod ships are
 * the ones its code declares.
 */
public final class MotionClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 80;
    private static final int BOOT_WAIT = 400;
    /** Long enough for any motion at eight times its length to be over, the slowest being four tenths of a second. */
    private static final int MOTION_WAIT = 100;
    private static final String CALCULATOR = "Calculator";
    /** The Settings program by its key, which every desktop knows whatever it calls the program. */
    private static final String SETTINGS = "jsc:settings";
    /** Every motion at eight times its own length. */
    private static final DesktopEffects SLOW = new DesktopEffects(List.of(), 800);

    private static final BlockPos COMPUTER = new BlockPos(5, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 2);

    private MotionClientTests() {
    }

    @ClientTest(timeoutTicks = 2400)
    public static void window_growsInAndAClosedOneShrinksAwayOnFrames11(final ClientTestContext ctx) {
        booted(ctx, "frames_11", null, false)
                .then(0, () -> moving(true))
                .then(SETTLE, () -> DesktopScreen.requestOpen(CALCULATOR))
                .thenWaitUntil(() -> desktop(ctx).windowFor(CALCULATOR) != null, SCREEN_WAIT, "the Calculator window")
                .thenAssert(0, () -> desktop(ctx).windowMoving(CALCULATOR),
                        "the window grows in from a little smaller than itself")
                .thenScreenshot(2, "motion-window-growing")
                .thenWaitUntil(() -> !desktop(ctx).windowMoving(CALCULATOR), MOTION_WAIT, "the window to arrive")
                .then(SETTLE, () -> clickScreen(ctx, desktop(ctx).windowButtonPoint(CALCULATOR, 3)))
                .thenAssert(1, () -> desktop(ctx).windowFor(CALCULATOR) == null
                                && desktop(ctx).windowsGoingAway() == 1,
                        "a closed window leaves the desktop at once and is drawn shrinking away")
                .thenScreenshot(1, "motion-window-closing")
                .thenWaitUntil(() -> desktop(ctx).windowsGoingAway() == 0, MOTION_WAIT,
                        "the closed window to be gone from the glass");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void window_goesDownToItsButtonAndComesBackOnFramesXp(final ClientTestContext ctx) {
        booted(ctx, "frames_xp", null, false)
                .then(0, () -> moving(true))
                .then(SETTLE, () -> DesktopScreen.requestOpen(CALCULATOR))
                .thenWaitUntil(() -> desktop(ctx).windowFor(CALCULATOR) != null, SCREEN_WAIT, "the Calculator window")
                .thenAssert(0, () -> !desktop(ctx).windowMoving(CALCULATOR),
                        "Frames XP opens a window at once, as that system did")
                .then(SETTLE, () -> clickScreen(ctx, desktop(ctx).windowButtonPoint(CALCULATOR, 1)))
                .thenAssert(1, () -> desktop(ctx).windowMoving(CALCULATOR),
                        "minimized, the window is drawn going down to its button")
                .thenScreenshot(2, "motion-window-minimizing")
                .thenWaitUntil(() -> !desktop(ctx).windowMoving(CALCULATOR), MOTION_WAIT,
                        "the window to reach its button")
                .then(SETTLE, () -> clickScreen(ctx, desktop(ctx).taskEntryPoint(CALCULATOR)))
                .thenAssert(1, () -> desktop(ctx).windowMoving(CALCULATOR),
                        "restored, it comes back up from its button")
                .thenScreenshot(2, "motion-window-restoring")
                .thenWaitUntil(() -> !desktop(ctx).windowMoving(CALCULATOR), MOTION_WAIT, "the window to be back");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void launcher_fadesInOnFramesXp(final ClientTestContext ctx) {
        booted(ctx, "frames_xp", null, false)
                .then(0, () -> moving(true))
                .then(SETTLE, () -> ctx.click(desktop(ctx).startButtonX(), desktop(ctx).startButtonY()))
                .thenAssert(1, () -> desktop(ctx).isStartOpen() && desktop(ctx).launcherMoving(),
                        "the Start menu fades in over the taskbar, the setting's own default")
                .thenScreenshot(2, "motion-start-fading")
                .thenWaitUntil(() -> !desktop(ctx).launcherMoving(), MOTION_WAIT, "the Start menu to arrive");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void reducedMotion_drawsEveryWindowWhereItEnds(final ClientTestContext ctx) {
        booted(ctx, "frames_11", null, false)
                .then(0, () -> ActiveDesktop.applyLiveEffects(SLOW))
                .then(SETTLE, () -> DesktopScreen.requestOpen(CALCULATOR))
                .thenWaitUntil(() -> desktop(ctx).windowFor(CALCULATOR) != null, SCREEN_WAIT, "the Calculator window")
                .thenAssert(0, () -> !desktop(ctx).windowMoving(CALCULATOR),
                        "with motion reduced a window is drawn where it ends from its first frame")
                .then(SETTLE, () -> clickScreen(ctx, desktop(ctx).windowButtonPoint(CALCULATOR, 3)))
                .thenAssert(1, () -> desktop(ctx).windowFor(CALCULATOR) == null
                                && desktop(ctx).windowsGoingAway() == 0,
                        "and a closed one is simply gone");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void bootPicture_meltsIntoTheDesktop(final ClientTestContext ctx) {
        ctx.thenBuild(0, world -> {
                    final CraftingComputerBlockEntity computer = world.placeRunningCraftingComputer(COMPUTER);
                    computer.installOs(jsc("frames_11"));
                    world.placeMonitor(MONITOR, Direction.EAST);
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .then(SETTLE, () -> {
                    MotionClock.setReduced(false);
                    // What a boot screen leaves as it hands the glass over: its picture's colour.
                    SceneHandoff.leave(ctx.abs(MONITOR), 0xFF10121C);
                    ctx.rightClick(MONITOR);
                })
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .thenAssert(0, () -> desktop(ctx).veiled(),
                        "the desktop comes up under the boot picture's colour, which gives way to it")
                .thenWaitUntil(() -> !desktop(ctx).veiled(), MOTION_WAIT, "the colour to have given way");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void pack_replacesASystemsMotionAndTheShippedFilesAreTheDeclaredOnes(final ClientTestContext ctx) {
        booted(ctx, "frames_11", null, false)
                .then(0, () -> moving(true))
                .then(0, () -> {
                    for (final DeclaredMotion declared : List.of(OsMotions.FRAMES_95, OsMotions.FRAMES_XP,
                            OsMotions.FRAMES_11, OsMotions.KDE_CLASSIC, OsMotions.PLASMA, OsMotions.GNOME_CLASSIC,
                            OsMotions.GNOME, OsMotions.CINNAMON, OsMotions.CDE)) {
                        final MotionProfile shipped = shipped(ctx, declared.file());
                        ctx.assertTrue(declared.declared().equals(shipped),
                                declared.name() + " ships the profile its code declares; got " + shipped);
                    }
                })
                .thenAssert(0, () -> "frames_11".equals(desktop(ctx).motionProfile())
                                && desktop(ctx).kindMoves(MotionKinds.WINDOW_OPEN),
                        "Frames 11 moves by its own profile, where a window grows in")
                .then(0, () -> OsMotions.FRAMES_11.load(MotionProfile.STILL))
                .thenAssert(0, () -> !desktop(ctx).kindMoves(MotionKinds.WINDOW_OPEN),
                        "a pack's still profile in its place keeps every window still")
                .then(0, OsMotions.FRAMES_11::reset)
                .thenAssert(0, () -> desktop(ctx).kindMoves(MotionKinds.WINDOW_OPEN),
                        "and without the pack the system moves again");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void frames95_displayEffectsSwitchesMinimizingAndCancelPutsItBack(final ClientTestContext ctx) {
        effectsPage(booted(ctx, "frames_95", null, false), ctx, SettingsApp.PAGE_DISPLAY)
                .then(SETTLE, () -> ctx.clickDesktop(settings(ctx).effectsControlCenter(1)))
                .thenWaitUntil(() -> off(ctx, OsMotions.MINIMIZE), SCREEN_WAIT, "minimizing switched off")
                .thenAssert(SETTLE, () -> !desktop(ctx).kindMoves(MotionKinds.WINDOW_MINIMIZE),
                        "with the box clear a window goes down to its button at once")
                .thenScreenshot(2, "effects-frames95")
                .then(SETTLE, () -> ctx.clickDesktop(settings(ctx).effectsFooterCenter(1)))
                .thenWaitUntil(() -> !off(ctx, OsMotions.MINIMIZE) && !settings(ctx).effectsOpen(), SCREEN_WAIT,
                        "Cancel to put the box back and leave the page");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void framesXp_bestPerformanceSwitchesEveryEffectOff(final ClientTestContext ctx) {
        effectsPage(booted(ctx, "frames_xp", null, false), ctx, 1)
                .then(SETTLE, () -> {
                    final int[] first = settings(ctx).effectsControlCenter(2);
                    ctx.clickDesktop(new int[] {first[0], first[1] + EffectsPageLayout.presetY(2)});
                })
                .thenWaitUntil(() -> off(ctx, OsMotions.MINIMIZE) && off(ctx, OsMotions.MENUS)
                        && off(ctx, OsMotions.ICON_SHADOWS), SCREEN_WAIT, "every effect switched off")
                .thenAssert(SETTLE, () -> !desktop(ctx).kindMoves(MotionKinds.MENU_SHOW)
                                && !desktop(ctx).kindMoves(MotionKinds.WINDOW_MINIMIZE),
                        "best performance keeps the menus and the windows still")
                .thenScreenshot(2, "effects-frames-xp")
                .then(SETTLE, () -> ctx.clickDesktop(settings(ctx).effectsFooterCenter(0)))
                .thenWaitUntil(() -> !settings(ctx).effectsOpen() && settings(ctx).page() == 1, SCREEN_WAIT,
                        "OK to go back to the System page");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void frames11_animationEffectsSwitchesEveryMotionOff(final ClientTestContext ctx) {
        effectsPage(booted(ctx, "frames_11", null, false), ctx, SettingsApp.PAGE_PERSONALIZE)
                .then(SETTLE, () -> ctx.clickDesktop(settings(ctx).effectsControlCenter(0)))
                .thenWaitUntil(() -> off(ctx, OsMotions.ANIMATIONS), SCREEN_WAIT, "Animation effects switched off")
                .thenAssert(SETTLE, () -> !desktop(ctx).kindMoves(MotionKinds.WINDOW_OPEN)
                                && !desktop(ctx).kindMoves(MotionKinds.MENU_SHOW),
                        "with Animation effects off no window grows and no flyout slides")
                .thenScreenshot(2, "effects-frames11")
                .then(SETTLE, () -> ctx.clickDesktop(settings(ctx).effectsBackCenter()))
                .thenWaitUntil(() -> settings(ctx).page() == SettingsApp.PAGE_PERSONALIZE, SCREEN_WAIT,
                        "Back to go to Personalize");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void plasma_animationSpeedAtInstantStopsEveryMotion(final ClientTestContext ctx) {
        effectsPage(booted(ctx, "ubuntu", "jsc:kde_plasma", false), ctx, SettingsApp.PAGE_PERSONALIZE)
                .then(SETTLE, () -> {
                    final int[] box = settings(ctx).effectsControlBox(0);
                    ctx.clickDesktop(new int[] {box[0] + box[2] - 4, box[1] + EffectsPageLayout.LINE_H + 6});
                })
                .thenWaitUntil(() -> settings(ctx).effectsShown() != null
                        && settings(ctx).effectsShown().speed() == 0, SCREEN_WAIT, "the speed set to Instant")
                .thenAssert(SETTLE, () -> !desktop(ctx).kindMoves(MotionKinds.WINDOW_OPEN),
                        "at Instant nothing moves")
                .thenScreenshot(2, "effects-plasma");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void kdeClassic_menuEffectDisableStopsTheMenusRolling(final ClientTestContext ctx) {
        effectsPage(booted(ctx, "ubuntu", "jsc:kde_plasma", true), ctx, SettingsApp.PAGE_PERSONALIZE)
                .thenAssert(0, () -> "kde_classic".equals(desktop(ctx).motionProfile()),
                        "a Legacy machine's KDE moves as KDE 3 did")
                .then(SETTLE, () -> ctx.clickDesktop(settings(ctx).effectsControlCenter(4)))
                .thenWaitUntil(() -> off(ctx, OsMotions.MENUS), SCREEN_WAIT, "the menu effect disabled")
                .thenAssert(SETTLE, () -> !desktop(ctx).kindMoves(MotionKinds.MENU_SHOW)
                                && desktop(ctx).kindMoves(MotionKinds.WINDOW_MINIMIZE),
                        "the menus stop rolling and the window outline still travels")
                .thenScreenshot(2, "effects-kde-classic");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void gnomeClassic_wireframeOffStopsTheOutline(final ClientTestContext ctx) {
        effectsPage(booted(ctx, "ubuntu", "jsc:gnome", true), ctx, SettingsApp.PAGE_PERSONALIZE)
                .then(SETTLE, () -> ctx.clickDesktop(settings(ctx).effectsControlCenter(0)))
                .thenWaitUntil(() -> off(ctx, OsMotions.WIREFRAME), SCREEN_WAIT, "the wireframe switched off")
                .thenAssert(SETTLE, () -> !desktop(ctx).kindMoves(MotionKinds.WINDOW_MINIMIZE),
                        "no outline travels to the button")
                .thenScreenshot(2, "effects-gnome-classic");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void gnome_reduceAnimationKeepsEveryWindowStill(final ClientTestContext ctx) {
        effectsPage(booted(ctx, "ubuntu", "jsc:gnome", false), ctx, SettingsApp.PAGE_PERSONALIZE)
                .then(SETTLE, () -> ctx.clickDesktop(settings(ctx).effectsControlCenter(0)))
                .thenWaitUntil(() -> off(ctx, OsMotions.ANIMATIONS), SCREEN_WAIT, "Reduce Animation switched on")
                .thenAssert(SETTLE, () -> !desktop(ctx).kindMoves(MotionKinds.WINDOW_OPEN)
                                && !desktop(ctx).kindMoves(MotionKinds.MENU_SHOW),
                        "windows and the overview appear at once")
                .thenScreenshot(2, "effects-gnome");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void cinnamon_openingWindowsNoneAndWindowEffectsOff(final ClientTestContext ctx) {
        effectsPage(booted(ctx, "ubuntu", "jsc:cinnamon", false), ctx, SettingsApp.PAGE_PERSONALIZE)
                .then(SETTLE, () -> ctx.clickDesktop(settings(ctx).effectsControlCenter(6)))
                .thenWaitUntil(() -> off(ctx, OsMotions.MAP), SCREEN_WAIT, "opening windows set to none")
                .thenAssert(SETTLE, () -> !desktop(ctx).kindMoves(MotionKinds.WINDOW_OPEN)
                                && desktop(ctx).kindMoves(MotionKinds.WINDOW_CLOSE),
                        "a window opens at once and still shrinks away when closed")
                .then(SETTLE, () -> ctx.clickDesktop(settings(ctx).effectsControlCenter(1)))
                .thenWaitUntil(() -> off(ctx, OsMotions.EFFECTS), SCREEN_WAIT, "window effects switched off")
                .thenAssert(SETTLE, () -> !desktop(ctx).kindMoves(MotionKinds.WINDOW_CLOSE)
                                && !desktop(ctx).kindMoves(MotionKinds.WINDOW_MINIMIZE),
                        "with window effects off no window moves at all")
                .thenScreenshot(2, "effects-cinnamon");
    }

    /**
     * A machine at its desktop: {@code os} installed, and {@code desktopPackage} on top of it when the system is a
     * Linux that takes one; {@code period} builds a Legacy-era machine, whose desktop wears its period look.
     */
    private static ClientTestContext booted(final ClientTestContext ctx, final String os,
                                            @Nullable final String desktopPackage, final boolean period) {
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
                .thenWaitUntil(() -> !desktop(ctx).launcherLabels().isEmpty(), SCREEN_WAIT,
                        "the desktop's programs to be listed");
    }

    /** Opens Settings on the page {@code parent} and from there the system's effects page. */
    private static ClientTestContext effectsPage(final ClientTestContext chain, final ClientTestContext ctx,
                                                 final int parent) {
        return chain.then(SETTLE, () -> DesktopScreen.requestOpen(SETTINGS))
                .thenWaitUntil(() -> settings(ctx) != null && settings(ctx).effectsShown() != null, SCREEN_WAIT,
                        "the Settings window with the machine's settings")
                .then(SETTLE, () -> ctx.clickDesktop(settings(ctx).navCenter(parent)))
                .thenWaitUntil(() -> settings(ctx).effectsEntryCenter()[0] != 0, SCREEN_WAIT,
                        "the button to the system's effects page")
                .then(SETTLE, () -> ctx.clickDesktop(settings(ctx).effectsEntryCenter()))
                .thenWaitUntil(() -> settings(ctx).effectsOpen() && (settings(ctx).effectsBackCenter()[0] != 0
                                || settings(ctx).effectsFooterCenter(0)[0] != 0),
                        SCREEN_WAIT, "the system's effects page built");
    }

    /** Lets every motion move again, slowed to eight times its length, or keeps it reduced. */
    private static void moving(final boolean move) {
        MotionClock.setReduced(!move);
        ActiveDesktop.applyLiveEffects(SLOW);
    }

    private static boolean off(final ClientTestContext ctx, final String effect) {
        final SettingsApp app = settings(ctx);
        return app != null && app.effectsShown() != null && app.effectsShown().isOff(effect);
    }

    private static MotionProfile shipped(final ClientTestContext ctx, final ResourceLocation file) {
        final Optional<Resource> resource = ctx.mc().getResourceManager().getResource(file);
        if (resource.isEmpty()) {
            throw new ClientTestFailure("the mod ships no " + file);
        }
        try (InputStream in = resource.get().open()) {
            return MotionProfile.read(in.readAllBytes());
        } catch (final IOException unreadable) {
            throw new ClientTestFailure("could not read " + file, unreadable);
        }
    }

    private static void clickScreen(final ClientTestContext ctx, final int[] point) {
        ctx.click(point[0], point[1]);
    }

    private static ResourceLocation jsc(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, path);
    }

    private static DesktopScreen desktop(final ClientTestContext ctx) {
        return ctx.screen(DesktopScreen.class);
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
}
