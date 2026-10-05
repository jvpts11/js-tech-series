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
import dev.jstech.computers.client.os.FilesApp;
import dev.jstech.computers.client.os.SettingsApp;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/**
 * Frames 7 and Frames 10 at their desktops: what each one's bar, Start, settings and explorer answer when they are
 * used, and a picture of each so the drawing is looked at rather than assumed.
 *
 * <p>Frames 7 keeps its programs on a superbar of icons and opens a Start of two columns; its settings are the Control
 * Panel, which opens on its categories, and its explorer carries a command bar under the address. Frames 10 has the
 * Action Center and Task View on its taskbar, tiles on its Start, settings that open on a grid of their pages and an
 * explorer with the ribbon folded over the address.
 */
public final class FramesSevenTenClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 80;
    private static final int BOOT_WAIT = 400;

    private static final BlockPos COMPUTER = new BlockPos(5, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 2);

    private static final String CALCULATOR = "Calculator";
    private static final String CONTROL_PANEL = "Control Panel";
    private static final String SETTINGS = "Settings";
    private static final String FILES = "Files";
    private static final String FILE_EXPLORER = "File Explorer";

    private FramesSevenTenClientTests() {
    }

    @ClientTest(timeoutTicks = 2400)
    public static void frames7_superbarListsAProgramAndTheOrbOpensStart(final ClientTestContext ctx) {
        booted(ctx, "frames_7")
                .then(0, () -> DesktopScreen.requestOpen(CALCULATOR))
                .thenWaitUntil(() -> desktop(ctx).windowFor(CALCULATOR) != null, SCREEN_WAIT, "the Calculator window")
                .then(2, () -> ctx.assertTrue(desktop(ctx).taskEntryLabels().contains(CALCULATOR),
                        "the superbar lists the open program; got " + desktop(ctx).taskEntryLabels()))
                .thenScreenshot(2, "frames7-superbar")
                .then(SETTLE, () -> ctx.click(desktop(ctx).startButtonX(), desktop(ctx).startButtonY()))
                .thenAssert(2, () -> desktop(ctx).isStartOpen(), "Start opens from the orb on the superbar")
                .thenScreenshot(2, "frames7-start");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void frames7_controlPanelOpensOnItsCategoriesAndACategoryOpensItsPage(final ClientTestContext ctx) {
        booted(ctx, "frames_7")
                .then(0, () -> DesktopScreen.requestOpen(CONTROL_PANEL))
                .thenWaitUntil(() -> settings(ctx, CONTROL_PANEL) != null, SCREEN_WAIT, "the Control Panel window")
                .thenAssert(2, () -> settings(ctx, CONTROL_PANEL).onHome(),
                        "the Control Panel opens on its categories")
                .thenScreenshot(2, "frames7-control-panel")
                .then(SETTLE, () -> ctx.clickDesktop(settings(ctx, CONTROL_PANEL)
                        .homeEntryCenter(SettingsApp.PAGE_PERSONALIZE)))
                .thenAssert(2, () -> !settings(ctx, CONTROL_PANEL).onHome(),
                        "Appearance and Personalization opens its page");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void frames7_explorerCarriesACommandBarWhoseOrganizeOpensAMenu(final ClientTestContext ctx) {
        booted(ctx, "frames_7")
                .then(0, () -> DesktopScreen.requestOpen(FILES))
                .thenWaitUntil(() -> files(ctx, FILES) != null, SCREEN_WAIT, "the Files window")
                .thenAssert(2, () -> files(ctx, FILES).commandBarShown(), "Frames 7's explorer has its command bar")
                .then(SETTLE, () -> ctx.clickDesktop(files(ctx, FILES).commandPoint("Organize")))
                .thenAssert(2, () -> files(ctx, FILES).contextOpen(), "Organize opens the folder's menu")
                .thenScreenshot(2, "frames7-explorer");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void frames7_onTheProcessorsGraphicsRunsBasicAndSaysSo(final ClientTestContext ctx) {
        ctx.thenBuild(0, world -> {
                    final CraftingComputerBlockEntity computer = world.placeRunningCraftingComputer(COMPUTER);
                    // Without its card the machine draws on the graphics of its processor, which Aero never ran on.
                    computer.getHardware().setStackInSlot(CraftingComputerBlockEntity.PCIE_SLOTS_START + 1,
                            ItemStack.EMPTY);
                    computer.installOs(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "frames_7"));
                    world.placeMonitor(MONITOR, Direction.EAST);
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .thenWaitUntil(() -> desktop(ctx).basicLook(), SCREEN_WAIT, "the desktop to drop to its basic look")
                .then(2, () -> ctx.assertTrue(desktop(ctx).balloonTitle().equals("Frames 7 Basic"),
                        "the notification area says the scheme changed; got " + desktop(ctx).balloonTitle()))
                .thenScreenshot(2, "frames7-basic")
                .then(0, () -> DesktopScreen.requestOpen(CONTROL_PANEL))
                .thenWaitUntil(() -> settings(ctx, CONTROL_PANEL) != null, SCREEN_WAIT, "the Control Panel window")
                .then(0, () -> settings(ctx, CONTROL_PANEL).showPage(SettingsApp.PAGE_SYSTEM))
                .thenScreenshot(4, "frames7-basic-rating")
                .then(SETTLE, () -> ctx.clickDesktop(settings(ctx, CONTROL_PANEL).effectsEntryCenter()))
                .thenAssert(2, () -> settings(ctx, CONTROL_PANEL).effectsOpen(), "Performance Options opens")
                .thenScreenshot(2, "frames7-basic-effects");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void frames10_getStartedShowsItsBandAndTheTilesOfItsDoors(final ClientTestContext ctx) {
        booted(ctx, "frames_10")
                .thenWaitUntil(() -> desktop(ctx).shownWindowLabels().contains("Get started"), SCREEN_WAIT,
                        "Get started to come up on the first desktop")
                .thenScreenshot(4, "frames10-get-started");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void frames10_actionCenterAndTaskViewOpenFromTheTaskbar(final ClientTestContext ctx) {
        booted(ctx, "frames_10")
                .then(SETTLE, () -> click(ctx, desktop(ctx).tenBarPoint("action")))
                .thenAssert(2, () -> desktop(ctx).actionCenterOpen(), "the Action Center slides out")
                .thenScreenshot(2, "frames10-action-center")
                .then(SETTLE, () -> click(ctx, desktop(ctx).tenBarPoint("action")))
                .thenAssert(2, () -> !desktop(ctx).actionCenterOpen(), "and goes away on a second click")
                .then(SETTLE, () -> click(ctx, desktop(ctx).tenBarPoint("view")))
                .thenAssert(2, () -> desktop(ctx).taskViewOpen(), "Task View opens from its button")
                .thenScreenshot(2, "frames10-task-view");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void frames10_startShowsTilesAndATileStartsItsProgram(final ClientTestContext ctx) {
        booted(ctx, "frames_10")
                .then(SETTLE, () -> ctx.click(desktop(ctx).startButtonX(), desktop(ctx).startButtonY()))
                .thenAssert(2, () -> desktop(ctx).isStartOpen(), "Start opens from its corner")
                .then(2, () -> ctx.assertTrue(desktop(ctx).startTiles().contains("calculator:m"),
                        "a fresh machine's Start carries the Calculator's tile; got " + desktop(ctx).startTiles()))
                .thenScreenshot(2, "frames10-start")
                .then(SETTLE, () -> click(ctx, desktop(ctx).tileCenter("calculator")))
                .thenWaitUntil(() -> desktop(ctx).windowFor(CALCULATOR) != null, SCREEN_WAIT,
                        "the tile to start the Calculator");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void frames10_settingsOpenOnTheirGridAndTheRibbonDropsDown(final ClientTestContext ctx) {
        booted(ctx, "frames_10")
                .then(0, () -> DesktopScreen.requestOpen(SETTINGS))
                .thenWaitUntil(() -> settings(ctx, SETTINGS) != null, SCREEN_WAIT, "the Settings window")
                .thenAssert(2, () -> settings(ctx, SETTINGS).onHome(), "Frames 10's settings open on their grid")
                .thenScreenshot(2, "frames10-settings")
                .then(0, () -> DesktopScreen.requestOpen(FILE_EXPLORER))
                .thenWaitUntil(() -> files(ctx, FILE_EXPLORER) != null, SCREEN_WAIT, "the File Explorer window")
                .then(SETTLE, () -> ctx.clickDesktop(files(ctx, FILE_EXPLORER).ribbonTabPoint("Home")))
                .then(2, () -> ctx.assertTrue(files(ctx, FILE_EXPLORER).ribbonLabels().contains("Copy"),
                        "Home drops down with the clipboard; got " + files(ctx, FILE_EXPLORER).ribbonLabels()))
                .thenScreenshot(2, "frames10-ribbon")
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenAssert(2, () -> files(ctx, FILE_EXPLORER).ribbonLabels().isEmpty(),
                        "Escape folds the ribbon back up");
    }

    /** A machine at its desktop with {@code os} installed and a monitor beside it, the player in front of it. */
    private static ClientTestContext booted(final ClientTestContext ctx, final String os) {
        return ctx.thenBuild(0, world -> {
                    final CraftingComputerBlockEntity computer = world.placeRunningCraftingComputer(COMPUTER);
                    computer.installOs(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, os));
                    world.placeMonitor(MONITOR, Direction.EAST);
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .thenWaitUntil(() -> desktop(ctx).launcherLabels().contains(CALCULATOR), SCREEN_WAIT,
                        "the Calculator to be listed in Start");
    }

    private static DesktopScreen desktop(final ClientTestContext ctx) {
        return ctx.screen(DesktopScreen.class);
    }

    private static void click(final ClientTestContext ctx, final int[] point) {
        ctx.assertTrue(point != null, "the point to click is on the screen");
        ctx.click(point[0], point[1]);
    }

    /** The settings window named {@code label}, or null while it is not open. */
    private static SettingsApp settings(final ClientTestContext ctx, final String label) {
        final DesktopWindow window = desktop(ctx).windowFor(label);
        return window != null && window.app() instanceof SettingsApp app ? app : null;
    }

    /** The explorer window named {@code label}, or null while it is not open. */
    private static FilesApp files(final ClientTestContext ctx, final String label) {
        final DesktopWindow window = desktop(ctx).windowFor(label);
        return window != null && window.app() instanceof FilesApp app ? app : null;
    }
}
