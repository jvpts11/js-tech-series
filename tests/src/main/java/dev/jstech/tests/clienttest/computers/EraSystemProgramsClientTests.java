/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.computers;

import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.client.os.DesktopWindow;
import dev.jstech.computers.client.os.DeviceManagerApp;
import dev.jstech.computers.client.os.HelpViewerApp;
import dev.jstech.computers.client.os.SettingsApp;
import dev.jstech.computers.client.os.TaskManagerApp;
import dev.jstech.computers.hardware.GpuSpec;
import dev.jstech.testkit.client.ClientTestFailure;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.computers.EraKit;
import dev.jstech.tests.testkit.computers.EraNetwork;
import dev.jstech.tests.testkit.computers.EraParts;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.List;
import java.util.Locale;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/**
 * Each era's system programs in a player's hands, at the era's Personal Computer on its own network: the Task Manager
 * opened from the panel's own menu ends the Calculator; the System Monitor opens on the machine; the Device Manager
 * lists the era's graphics card; Settings refuses and then allows programs from other computers, and the machine
 * keeps each answer; and Help follows a link to its page.
 */
public final class EraSystemProgramsClientTests {

    /** How long a step may take to show on the screen. */
    private static final int WAIT = 80;
    private static final String CALCULATOR = "Calculator";
    private static final String TASK_MANAGER = "jsc:task_manager";
    /** The Settings page that holds the sharing and the programs other computers may start. */
    private static final int NETWORK_PAGE = 2;

    private EraSystemProgramsClientTests() {
    }

    @ClientTest(timeoutTicks = 6000)
    public static void legacy_systemProgramsDoTheirWork(final ClientTestContext ctx) {
        system(ctx, EraKit.LEGACY);
    }

    @ClientTest(timeoutTicks = 6000)
    public static void transition_systemProgramsDoTheirWork(final ClientTestContext ctx) {
        system(ctx, EraKit.TRANSITION);
    }

    @ClientTest(timeoutTicks = 6000)
    public static void standard_systemProgramsDoTheirWork(final ClientTestContext ctx) {
        system(ctx, EraKit.STANDARD);
    }

    @ClientTest(timeoutTicks = 6000)
    public static void advanced_systemProgramsDoTheirWork(final ClientTestContext ctx) {
        system(ctx, EraKit.ADVANCED);
    }

    private static void system(final ClientTestContext ctx, final EraKit kit) {
        EraJourney.atTheDesk(ctx, kit)
                .then(EraJourney.SETTLE, () -> {
                    final DesktopScreen desktop = ctx.screen(DesktopScreen.class);
                    for (final String open : List.copyOf(desktop.openWindowLabels())) {
                        close(ctx, open);
                    }
                });
        taskManager(ctx, kit);
        systemMonitor(ctx, kit);
        deviceManager(ctx, kit);
        settings(ctx, kit);
        help(ctx, kit);
    }

    /*
     * The Calculator opened, the Task Manager brought up from the menu the panel's empty stretch opens, the
     * Calculator picked in its list and ended with the End button: its window goes.
     */
    private static void taskManager(final ClientTestContext ctx, final EraKit kit) {
        open(ctx, kit, CALCULATOR)
                .then(EraJourney.SETTLE, () -> {
                    final int[] at = ctx.screen(DesktopScreen.class).emptyPanelPoint();
                    ctx.rightClick(at[0] + 0.5, at[1] + 0.5);
                })
                .thenWaitUntil(() -> ctx.screen(DesktopScreen.class).isPanelMenuOpen(), WAIT,
                        "the right button on the panel's empty stretch to open its menu")
                .then(EraJourney.SETTLE, () -> {
                    final int[] at = ctx.screen(DesktopScreen.class).panelMenuPoint("Task Manager");
                    ctx.click(at[0] + 0.5, at[1] + 0.5);
                })
                .thenWaitUntil(() -> taskManagerApp(ctx) != null && taskManagerApp(ctx).rowPoint(CALCULATOR) != null,
                        WAIT, "the Task Manager to open and list the Calculator")
                .then(EraJourney.SETTLE, () -> ctx.clickDesktop(taskManagerApp(ctx).rowPoint(CALCULATOR)))
                .then(EraJourney.SETTLE, () -> ctx.clickDesktop(taskManagerApp(ctx).endPoint()))
                .thenWaitUntil(() -> ctx.screen(DesktopScreen.class).windowFor(CALCULATOR) == null, WAIT,
                        "the End button to end the Calculator")
                .thenScreenshot(EraJourney.SETTLE, shot(kit, "task-manager"));
        shut(ctx, TASK_MANAGER);
    }

    /* The System Monitor opened on the machine, and closed again. */
    private static void systemMonitor(final ClientTestContext ctx, final EraKit kit) {
        final String label = "System Monitor";
        open(ctx, kit, label).thenScreenshot(EraJourney.SETTLE * 10, shot(kit, "system-monitor"));
        shut(ctx, label);
    }

    /* The Device Manager listing the graphics card the era's computer was built with. */
    private static void deviceManager(final ClientTestContext ctx, final EraKit kit) {
        final String label = "Device Manager";
        final String card = new ItemStack(EraParts.cardFor(kit.computerBoard(), GpuSpec.class)).getHoverName()
                .getString();
        open(ctx, kit, label)
                .thenWaitUntil(() -> app(ctx, label, DeviceManagerApp.class).shownRows().stream()
                        .anyMatch(row -> row.contains(card)), WAIT, "the Device Manager to list " + card)
                .thenScreenshot(EraJourney.SETTLE, shot(kit, "device-manager"));
        shut(ctx, label);
    }

    /*
     * Settings opened on the page of the network, from its home on the editions that open on one: programs from
     * other computers refused, and the machine says no; allowed again, and the machine says yes.
     */
    private static void settings(final ClientTestContext ctx, final EraKit kit) {
        final String label = kit == EraKit.TRANSITION ? "Control Panel" : "Settings";
        open(ctx, kit, label)
                .thenWaitUntil(() -> {
                    final SettingsApp app = app(ctx, label, SettingsApp.class);
                    return app.onHome() ? app.homeEntryCenter(NETWORK_PAGE)[0] > 0 : app.navCenter(NETWORK_PAGE)[0] > 0;
                }, WAIT, "Settings to show its way to the network page")
                .then(EraJourney.SETTLE, () -> {
                    final SettingsApp app = app(ctx, label, SettingsApp.class);
                    ctx.clickDesktop(app.onHome() ? app.homeEntryCenter(NETWORK_PAGE) : app.navCenter(NETWORK_PAGE));
                })
                .thenWaitUntil(() -> app(ctx, label, SettingsApp.class).page() == NETWORK_PAGE
                        && app(ctx, label, SettingsApp.class).remoteRefusedCenter()[0] > 0, WAIT,
                        "the network page with its choice of programs from other computers")
                .then(EraJourney.SETTLE, () -> ctx.clickDesktop(
                        app(ctx, label, SettingsApp.class).remoteRefusedCenter()))
                .thenWaitUntil(() -> !app(ctx, label, SettingsApp.class).remoteAllowedShown(), WAIT,
                        "programs from other computers to be refused");
        ctx.thenWaitUntilServer(level -> !computer(ctx, level).console().settings().remoteAllowed(), WAIT,
                        "the machine to refuse programs from other computers", level -> "still allowed")
                .thenScreenshot(EraJourney.SETTLE, shot(kit, "settings-refused"))
                .then(EraJourney.SETTLE, () -> ctx.clickDesktop(
                        app(ctx, label, SettingsApp.class).remoteAllowedCenter()))
                .thenWaitUntil(() -> app(ctx, label, SettingsApp.class).remoteAllowedShown(), WAIT,
                        "programs from other computers to be allowed again");
        ctx.thenWaitUntilServer(level -> computer(ctx, level).console().settings().remoteAllowed(), WAIT,
                "the machine to allow programs from other computers again", level -> "still refused");
        shut(ctx, label);
    }

    /* Help opened on its first page, and its first link followed to the page it names. */
    private static void help(final ClientTestContext ctx, final EraKit kit) {
        final String label = kit.era().isAtLeast(EraKit.STANDARD.era()) ? "Get Help" : "Help and Support";
        final String[] link = new String[1];
        open(ctx, kit, label)
                .thenWaitUntil(() -> !app(ctx, label, HelpViewerApp.class).pageLinks().isEmpty()
                        && app(ctx, label, HelpViewerApp.class).linkCentre(
                                app(ctx, label, HelpViewerApp.class).pageLinks().getFirst()) != null, WAIT,
                        "Help to open on a page with links")
                .then(EraJourney.SETTLE, () -> {
                    final HelpViewerApp app = app(ctx, label, HelpViewerApp.class);
                    link[0] = app.pageLinks().getFirst();
                    ctx.clickDesktop(app.linkCentre(link[0]));
                })
                .thenWaitUntil(() -> app(ctx, label, HelpViewerApp.class).shownPage().equals(link[0]), WAIT,
                        "the link to lead to its page")
                .thenScreenshot(EraJourney.SETTLE, shot(kit, "help"));
        shut(ctx, label);
    }

    /* Opens the program from Start and waits for its window. */
    private static ClientTestContext open(final ClientTestContext ctx, final EraKit kit, final String label) {
        return EraJourney.launch(ctx, kit, label)
                .thenWaitUntil(() -> ctx.screen(DesktopScreen.class).windowFor(label) != null, WAIT,
                        "the " + label + " window to open from Start");
    }

    /* Closes the program's window with its close box and waits for it to go. */
    private static void shut(final ClientTestContext ctx, final String label) {
        ctx.then(EraJourney.SETTLE, () -> close(ctx, label))
                .thenWaitUntil(() -> ctx.screen(DesktopScreen.class).windowFor(label) == null, WAIT,
                        "the " + label + " window to close from its close box");
    }

    private static void close(final ClientTestContext ctx, final String label) {
        final DesktopWindow window = ctx.screen(DesktopScreen.class).windowFor(label);
        if (window != null) {
            ctx.clickDesktop(window.buttonCentre(DesktopWindow.BUTTON_CLOSE));
        }
    }

    private static TaskManagerApp taskManagerApp(final ClientTestContext ctx) {
        final DesktopWindow window = ctx.screen(DesktopScreen.class).windowFor(TASK_MANAGER);
        return window != null && window.app() instanceof TaskManagerApp manager ? manager : null;
    }

    private static <T> T app(final ClientTestContext ctx, final String label, final Class<T> type) {
        final DesktopWindow window = ctx.screen(DesktopScreen.class).windowFor(label);
        if (window == null || !type.isInstance(window.app())) {
            throw new ClientTestFailure("no " + type.getSimpleName() + " window named " + label);
        }
        return type.cast(window.app());
    }

    private static String shot(final EraKit kit, final String what) {
        return kit.name().toLowerCase(Locale.ROOT) + "-" + what;
    }

    private static PersonalComputerBlockEntity computer(final ClientTestContext ctx, final ServerLevel level) {
        return TestWorldBuilder.at(level, ctx.origin()).blockEntity(EraNetwork.COMPUTER,
                PersonalComputerBlockEntity.class);
    }
}
