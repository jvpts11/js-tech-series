/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.client.SystemBootScreen;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.client.os.DesktopWindow;
import dev.jstech.computers.client.os.ShellApp;
import dev.jstech.computers.gui.layout.CdeExitLayout;
import dev.jstech.computers.gui.layout.CdeFrontPanelLayout;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * CDE as a player meets it on a UNIX machine: the Front Panel where the others have a bar, windows in Motif
 * frames, and the programs found through the panel's own subpanel.
 */
public final class CdeClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 80;
    private static final int BOOT_WAIT = 1_200;
    /** Ticks enough for a first click to have gone stale, so the two after it are a double click of their own. */
    private static final int DOUBLE_CLICK_GONE = 10;

    private static final String MANAGER = "Application Manager";
    private static final String MANAGER_APPS = "Application Manager - Desktop_Apps";

    private static final BlockPos MACHINE = new BlockPos(5, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 2);

    private CdeClientTests() {
    }

    /**
     * The machine comes up at CDE, a control on the Front Panel opens its program in a Motif window, and the
     * Applications control raises the subpanel that lists every program and starts the one that is chosen.
     */
    @ClientTest(timeoutTicks = 3600)
    public static void frontPanel_opensProgramsFromItsControlsAndItsSubpanel(final ClientTestContext ctx) {
        atCde(ctx)
                .thenScreenshot(SETTLE * 2, "cde-desktop")
                .then(SETTLE, () -> press(ctx, CdeFrontPanelLayout.Control.FILES))
                .thenWaitUntil(() -> desktop(ctx).openWindowLabels().contains("File Manager"), SCREEN_WAIT,
                        "the Files control to open the File Manager")
                .thenScreenshot(SETTLE, "cde-file-manager")
                .then(SETTLE, () -> clickAt(ctx,
                        desktop(ctx).frontPanelArrowPoint(CdeFrontPanelLayout.Control.APPLICATIONS)))
                // A subpanel lists only what the machine has, and nothing on UNIX stands behind a third line yet.
                .thenWaitUntil(() -> desktop(ctx).subpanelLabels().equals(List.of("Application Manager",
                                "Performance Meter")), SCREEN_WAIT,
                        "the arrow over Applications to raise its subpanel")
                .thenScreenshot(SETTLE, "cde-applications");
    }

    /**
     * A subpanel is a place to keep things and not a menu: what is chosen on it starts, the subpanel stays up,
     * and only the arrow it came up by puts it away again.
     */
    @ClientTest(timeoutTicks = 3600)
    public static void subpanel_startsWhatIsChosenAndStaysUpUntilItsArrowIsPressed(final ClientTestContext ctx) {
        atCde(ctx)
                .then(SETTLE, () -> clickAt(ctx,
                        desktop(ctx).frontPanelArrowPoint(CdeFrontPanelLayout.Control.EDITOR)))
                .thenWaitUntil(() -> desktop(ctx).subpanelLabels().equals(List.of("Text Editor", "Terminal",
                        "Calculator")), SCREEN_WAIT, "the arrow over the Text Editor to raise Personal Applications")
                .then(SETTLE, () -> clickAt(ctx, desktop(ctx).subpanelPoint("Calculator")))
                .thenWaitUntil(() -> desktop(ctx).shownWindowLabels().contains("Calculator"), SCREEN_WAIT,
                        "the Calculator to start from the subpanel")
                .thenAssert(1, () -> !desktop(ctx).subpanelLabels().isEmpty(), "and the subpanel is still up")
                .thenScreenshot(SETTLE, "cde-personal-applications")
                .then(SETTLE, () -> clickAt(ctx,
                        desktop(ctx).frontPanelArrowPoint(CdeFrontPanelLayout.Control.FILES)))
                .thenWaitUntil(() -> desktop(ctx).subpanelLabels().equals(List.of("Home", "Desktop")), SCREEN_WAIT,
                        "the arrow over Files to raise its own subpanel in place of the other")
                .then(SETTLE, () -> clickAt(ctx,
                        desktop(ctx).frontPanelArrowPoint(CdeFrontPanelLayout.Control.FILES)))
                .thenWaitUntil(() -> desktop(ctx).subpanelLabels().isEmpty(), SCREEN_WAIT,
                        "the same arrow pressed again to put the subpanel away");
    }

    /**
     * The Applications control opens the Application Manager: the groups that hold something as folders, a
     * group in a window of its own on a double click, and a program started by a double click on it there.
     */
    @ClientTest(timeoutTicks = 3600)
    public static void applicationManager_findsAProgramByItsGroupAndStartsIt(final ClientTestContext ctx) {
        atCde(ctx)
                .then(SETTLE, () -> press(ctx, CdeFrontPanelLayout.Control.APPLICATIONS))
                .thenWaitUntil(() -> desktop(ctx).applicationManagerNames(MANAGER).contains("Desktop_Apps"),
                        SCREEN_WAIT, "the Applications control to open the Application Manager")
                .thenAssert(1, () -> !desktop(ctx).applicationManagerNames(MANAGER).contains("Games"),
                        "a group with nothing in it is not drawn")
                .thenScreenshot(SETTLE, "cde-application-manager")
                .then(DOUBLE_CLICK_GONE, () -> doubleClickAt(ctx,
                        desktop(ctx).applicationManagerPoint(MANAGER, "Desktop_Apps")))
                .thenWaitUntil(() -> desktop(ctx).applicationManagerNames(MANAGER_APPS).contains("Calculator"),
                        SCREEN_WAIT, "a double click on Desktop_Apps to open the group in its own window")
                .thenScreenshot(SETTLE, "cde-application-manager-group")
                .then(DOUBLE_CLICK_GONE, () -> doubleClickAt(ctx,
                        desktop(ctx).applicationManagerPoint(MANAGER_APPS, "Calculator")))
                .thenWaitUntil(() -> desktop(ctx).shownWindowLabels().contains("Calculator"), SCREEN_WAIT,
                        "a double click on the Calculator to start it");
    }

    private static void doubleClickAt(final ClientTestContext ctx, final int[] at) {
        clickAt(ctx, at);
        clickAt(ctx, at);
    }

    /**
     * Between the console's lines and the desktop, CDE puts up its own plate over the backdrop it is about to
     * stand on, and then the desktop comes up behind it.
     */
    @ClientTest(timeoutTicks = 3600)
    public static void comingUp_showsCdesOwnPlateBeforeTheDesktop(final ClientTestContext ctx) {
        switchedOn(ctx)
                .thenAwaitScreen(SystemBootScreen.class, BOOT_WAIT)
                .thenWaitUntil(() -> ctx.screen(SystemBootScreen.class) != null
                        && ctx.screen(SystemBootScreen.class).desktopSplashUp(), BOOT_WAIT,
                        "CDE's plate to take the glass from the system's lines")
                .thenScreenshot(2, "cde-coming-up")
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT);
    }

    /** A UNIX machine with CDE installed, brought up, with the player at its monitor and the desktop open. */
    private static ClientTestContext atCde(final ClientTestContext ctx) {
        return switchedOn(ctx).thenAwaitScreen(DesktopScreen.class, BOOT_WAIT);
    }

    /** The same machine just switched on, with the player at its monitor while it is still coming up. */
    private static ClientTestContext switchedOn(final ClientTestContext ctx) {
        return ctx.thenBuild(0, world -> {
                    world.setBlock(MACHINE, ComputingModule.MAINFRAME.get());
                    final MainframeBlockEntity machine = world.blockEntity(MACHINE, MainframeBlockEntity.class);
                    final ItemStackHandler inv = machine.getInventory();
                    inv.setStackInSlot(MainframeBlockEntity.MOTHERBOARD_SLOT,
                            new ItemStack(ComputingModule.MOTHERBOARD_MTX_P.get()));
                    inv.setStackInSlot(MainframeBlockEntity.CPU_SLOTS_START,
                            new ItemStack(ComputingModule.CPU_SERVO_2620.get()));
                    inv.setStackInSlot(MainframeBlockEntity.RAM_SLOTS_START,
                            new ItemStack(ComputingModule.RAM_DDR3_8192.get()));
                    inv.setStackInSlot(MainframeBlockEntity.PSU_SLOT, new ItemStack(ComputingModule.PSU_650G.get()));
                    // A graphics card gives the machine peripheral ports, which is what the monitor links to.
                    inv.setStackInSlot(MainframeBlockEntity.GPU_SLOTS_START,
                            new ItemStack(ComputingModule.GPU_HD_7970.get()));
                    inv.setStackInSlot(MainframeBlockEntity.DISK_SLOTS_START,
                            new ItemStack(ComputingModule.disk(StorageTier.SSD, DiskSize.GB_500)));
                    machine.installOs(jsc("unix"));
                    machine.console().install(jsc("cde").toString());
                    machine.togglePower();
                    world.placeMonitor(MONITOR, Direction.EAST);
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR);
    }

    /**
     * The workspaces are real: a window stays on the one it was opened on, another workspace shows a desktop
     * without it, and what is opened there belongs there.
     */
    @ClientTest(timeoutTicks = 3600)
    public static void workspaces_keepTheirOwnWindows(final ClientTestContext ctx) {
        atCde(ctx)
                .then(SETTLE, () -> press(ctx, CdeFrontPanelLayout.Control.FILES))
                .thenWaitUntil(() -> desktop(ctx).shownWindowLabels().contains("File Manager"), SCREEN_WAIT,
                        "the File Manager to open on workspace One")
                .then(SETTLE, () -> pressWorkspace(ctx, 1))
                .thenWaitUntil(() -> desktop(ctx).shownWorkspace() == 1, SCREEN_WAIT, "workspace Two to come up")
                .thenAssert(1, () -> desktop(ctx).shownWindowLabels().isEmpty(), "workspace Two shows no window")
                .thenAssert(1, () -> desktop(ctx).openWindowLabels().contains("File Manager"),
                        "and the File Manager is still open, on its own workspace")
                .then(SETTLE, () -> press(ctx, CdeFrontPanelLayout.Control.EDITOR))
                .thenWaitUntil(() -> desktop(ctx).shownWindowLabels().contains("Text Editor"), SCREEN_WAIT,
                        "the Text Editor to open on workspace Two")
                .thenScreenshot(SETTLE, "cde-workspace-two")
                .then(SETTLE, () -> pressWorkspace(ctx, 0))
                .thenWaitUntil(() -> desktop(ctx).shownWorkspace() == 0, SCREEN_WAIT, "workspace One to come back")
                .thenAssert(1, () -> desktop(ctx).shownWindowLabels().equals(List.of("File Manager")),
                        "workspace One shows the File Manager alone");
    }

    /**
     * CDE lists no open windows, so a window that is put away has to be found some other way: it stands at the
     * top left of its workspace as an icon, clear of what the player keeps on the workspace, and a double click
     * on the icon brings it back where one click does not.
     */
    @ClientTest(timeoutTicks = 3600)
    public static void aWindowPutAway_standsAsAnIconThatBringsItBack(final ClientTestContext ctx) {
        atCde(ctx)
                .then(SETTLE, () -> press(ctx, CdeFrontPanelLayout.Control.FILES))
                .thenWaitUntil(() -> desktop(ctx).shownWindowLabels().contains("File Manager"), SCREEN_WAIT,
                        "the File Manager to open")
                .then(SETTLE, () -> clickAt(ctx, desktop(ctx).windowButtonPoint("File Manager", 1)))
                .thenWaitUntil(() -> desktop(ctx).shownWindowLabels().isEmpty(), SCREEN_WAIT,
                        "the minimise button to put the window away")
                .thenAssert(1, () -> desktop(ctx).openWindowLabels().contains("File Manager"),
                        "put away, the window is still open")
                .thenScreenshot(SETTLE, "cde-window-icon")
                .then(SETTLE, () -> clickAt(ctx, desktop(ctx).putAwayIconPoint(0)))
                .thenAssert(DOUBLE_CLICK_GONE, () -> desktop(ctx).shownWindowLabels().isEmpty(),
                        "one click on the icon leaves the window put away")
                .then(1, () -> {
                    clickAt(ctx, desktop(ctx).putAwayIconPoint(0));
                    clickAt(ctx, desktop(ctx).putAwayIconPoint(0));
                })
                .thenWaitUntil(() -> desktop(ctx).shownWindowLabels().contains("File Manager"), SCREEN_WAIT,
                        "a double click on its icon to bring the window back");
    }

    /**
     * The button at the left of a Motif title bar raises the window's menu, and Occupy Workspace on it is how a
     * window comes to be on another workspace as well: ticked on Three, the File Manager shows there too, and
     * stays on One.
     */
    @ClientTest(timeoutTicks = 3600)
    public static void windowMenu_putsAWindowOnAnotherWorkspaceAsWell(final ClientTestContext ctx) {
        atCde(ctx)
                .then(SETTLE, () -> press(ctx, CdeFrontPanelLayout.Control.FILES))
                .thenWaitUntil(() -> desktop(ctx).shownWindowLabels().contains("File Manager"), SCREEN_WAIT,
                        "the File Manager to open")
                .then(SETTLE, () -> clickAt(ctx, desktop(ctx).windowButtonPoint("File Manager", 3)))
                .thenWaitUntil(() -> !desktop(ctx).windowMenuLabels().isEmpty(), SCREEN_WAIT,
                        "the menu button to raise the window's menu")
                .thenAssert(1, () -> desktop(ctx).windowMenuLabels().equals(List.of("Restore", "Minimize",
                                "Maximize", "Lower", "Occupy Workspace...", "Occupy All Workspaces", "Close")),
                        "the menu lists what can be done to a window, and nothing else")
                .thenAssert(1, () -> desktop(ctx).shownWindowLabels().contains("File Manager"),
                        "and one click on the button does not close the window")
                .thenScreenshot(SETTLE, "cde-window-menu")
                .then(DOUBLE_CLICK_GONE, () -> clickAt(ctx, desktop(ctx).windowMenuPoint("Occupy Workspace...")))
                .thenWaitUntil(() -> desktop(ctx).occupyBoxPoint(2) != null, SCREEN_WAIT,
                        "Occupy Workspace to ask which workspaces")
                .thenScreenshot(SETTLE, "cde-occupy-workspace")
                .then(SETTLE, () -> clickAt(ctx, desktop(ctx).occupyBoxPoint(2)))
                .then(SETTLE, () -> clickAt(ctx, desktop(ctx).occupyOkPoint()))
                .thenWaitUntil(() -> desktop(ctx).workspacesOf("File Manager").equals(List.of(0, 2)), SCREEN_WAIT,
                        "OK to put the File Manager on One and Three")
                .then(SETTLE, () -> pressWorkspace(ctx, 2))
                .thenWaitUntil(() -> desktop(ctx).shownWorkspace() == 2, SCREEN_WAIT, "workspace Three to come up")
                .thenAssert(1, () -> desktop(ctx).shownWindowLabels().contains("File Manager"),
                        "the File Manager shows on Three")
                .then(SETTLE, () -> pressWorkspace(ctx, 1))
                .thenWaitUntil(() -> desktop(ctx).shownWorkspace() == 1, SCREEN_WAIT, "workspace Two to come up")
                .thenAssert(1, () -> desktop(ctx).shownWindowLabels().isEmpty(), "and not on Two");
    }

    /**
     * EXIT on the Front Panel asks before anything goes down, and Cancel on it means what it says: the dialog
     * goes, the desktop stays, and the program that was open is still open.
     */
    @ClientTest(timeoutTicks = 3600)
    public static void exit_asksAndCancelLeavesTheDesktopAsItWas(final ClientTestContext ctx) {
        atCde(ctx)
                .then(SETTLE, () -> press(ctx, CdeFrontPanelLayout.Control.FILES))
                .thenWaitUntil(() -> desktop(ctx).shownWindowLabels().contains("File Manager"), SCREEN_WAIT,
                        "the File Manager to open")
                .then(SETTLE, () -> clickAt(ctx, desktop(ctx).exitPoint()))
                .thenWaitUntil(() -> desktop(ctx).powerDialogOpen(), SCREEN_WAIT, "EXIT to ask")
                .thenScreenshot(SETTLE, "cde-exit")
                .then(SETTLE, () -> clickAt(ctx, aboveTheExitButtons(ctx)))
                .thenAssert(SETTLE, () -> desktop(ctx).powerDialogOpen(),
                        "a click beside the buttons does not answer the question")
                .then(SETTLE, () -> clickAt(ctx, desktop(ctx).exitDialogPoint(CdeExitLayout.CANCEL)))
                .thenWaitUntil(() -> !desktop(ctx).powerDialogOpen(), SCREEN_WAIT, "Cancel to put the dialog away")
                .thenAssert(1, () -> desktop(ctx).shownWindowLabels().contains("File Manager"),
                        "and the File Manager is still open");
    }

    /**
     * The Terminal of a UNIX desktop is a UNIX terminal: it opens on the shell's own prompt and greets nobody,
     * and nothing in it speaks the way a Frames command prompt does.
     */
    @ClientTest(timeoutTicks = 3600)
    public static void terminal_opensOnTheShellPromptAndNotAsAFramesPrompt(final ClientTestContext ctx) {
        atCde(ctx)
                .then(SETTLE, () -> clickAt(ctx,
                        desktop(ctx).frontPanelArrowPoint(CdeFrontPanelLayout.Control.EDITOR)))
                .thenWaitUntil(() -> desktop(ctx).subpanelLabels().contains("Terminal"), SCREEN_WAIT,
                        "Personal Applications to come up")
                .then(SETTLE, () -> clickAt(ctx, desktop(ctx).subpanelPoint("Terminal")))
                .thenWaitUntil(() -> terminal(ctx) != null && terminal(ctx).prompt().endsWith("$"), SCREEN_WAIT * 2,
                        "the Terminal to come up on the shell's prompt")
                .thenAssert(1, () -> !terminal(ctx).scrollbackText().contains("Midsoft")
                                && !terminal(ctx).scrollbackText().contains("HELP")
                                && !terminal(ctx).scrollbackText().contains("CDE"),
                        "a UNIX terminal says nothing of the desktop, of Midsoft or of HELP when it opens")
                .then(SETTLE, () -> terminal(ctx).typeLines(List.of("uname -sr", "ls /", "nosuch")))
                .thenWaitUntil(() -> terminal(ctx).scrollbackText().contains("UNIX 3.2")
                        && terminal(ctx).scrollbackText().contains("not found: nosuch"), SCREEN_WAIT * 3,
                        "the shell to answer on the paper of the terminal window")
                .thenScreenshot(SETTLE, "cde-terminal");
    }

    private static ShellApp terminal(final ClientTestContext ctx) {
        final DesktopWindow window = desktop(ctx).windowFor("Terminal");
        return window != null && window.app() instanceof ShellApp shell ? shell : null;
    }

    /** A double click on the menu button closes the window, as it always did on a Motif title bar. */
    @ClientTest(timeoutTicks = 3600)
    public static void menuButton_closesTheWindowOnADoubleClick(final ClientTestContext ctx) {
        atCde(ctx)
                .then(SETTLE, () -> press(ctx, CdeFrontPanelLayout.Control.FILES))
                .thenWaitUntil(() -> desktop(ctx).shownWindowLabels().contains("File Manager"), SCREEN_WAIT,
                        "the File Manager to open")
                .then(SETTLE, () -> {
                    final int[] at = desktop(ctx).windowButtonPoint("File Manager", 3);
                    clickAt(ctx, at);
                    clickAt(ctx, at);
                })
                .thenWaitUntil(() -> !desktop(ctx).openWindowLabels().contains("File Manager"), SCREEN_WAIT,
                        "the double click to close the File Manager");
    }

    /** A point on the Exit dialog's words, over the middle button: inside the dialog and on none of its buttons. */
    private static int[] aboveTheExitButtons(final ClientTestContext ctx) {
        final int[] restart = desktop(ctx).exitDialogPoint(CdeExitLayout.RESTART);
        return new int[] {restart[0], restart[1] - 30};
    }

    private static void clickAt(final ClientTestContext ctx, final int[] at) {
        ctx.click(at[0] + 0.5, at[1] + 0.5);
    }

    private static void pressWorkspace(final ClientTestContext ctx, final int index) {
        final int[] at = desktop(ctx).workspacePoint(index);
        ctx.click(at[0] + 0.5, at[1] + 0.5);
    }

    /** Clicks a control of the Front Panel where it is drawn on the screen. */
    private static void press(final ClientTestContext ctx, final CdeFrontPanelLayout.Control control) {
        final int[] at = desktop(ctx).frontPanelPoint(control);
        ctx.click(at[0] + 0.5, at[1] + 0.5);
    }

    private static DesktopScreen desktop(final ClientTestContext ctx) {
        return ctx.screen(DesktopScreen.class);
    }

    private static ResourceLocation jsc(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, path);
    }
}
