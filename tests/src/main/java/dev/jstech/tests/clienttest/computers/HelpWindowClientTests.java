/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.computers;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.client.os.HelpViewerApp;
import dev.jstech.computers.client.os.IDesktopApp;
import dev.jstech.computers.gui.help.HelpForm;
import dev.jstech.computers.gui.help.HelpTarget;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.core.guide.CoreGuide;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * Help on every desktop, in the form its system had it, worked as a player works it: opened, the Technical
 * Reference's tree or its pages followed down to the entry on graphics cards, and each form photographed there.
 */
public final class HelpWindowClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 100;
    private static final int BOOT_WAIT = 600;
    private static final BlockPos COMPUTER = new BlockPos(5, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 2);
    private static final String MANUAL = "Technical Reference";
    /** The book each system's help opens on, the one written for it, ahead of the others. */
    private static final String FRAMES_BOOK = "Frames Resource Kit";
    private static final String LINUX_BOOK = "The Linux Desk Reference";
    private static final String UNIX_BOOK = "The Daemon Handbook";
    private static final String CARDS_TITLE = "Graphics cards";
    private static final String BUILDING_CHAPTER = " Building computers";
    /** The Technical Reference's top page, among the other manuals a book's top page lists. */
    private static final String REFERENCE = HelpTarget.contents(CoreGuide.TECHNICAL_REFERENCE).written();
    /** J's Computers' chapter on building computers, which holds the section on the parts of a computer. */
    private static final String BUILDING = HelpTarget.node(CoreGuide.TECHNICAL_REFERENCE, "jsc:building_computers")
            .written();
    private static final String HARDWARE = HelpTarget.node(CoreGuide.TECHNICAL_REFERENCE, "jsc:hardware").written();
    /** What Find is asked for: every book is searched, so the words are the entry's own title. */
    private static final String SEARCHED = "graphics cards";
    private static final String CARDS = HelpTarget.node(CoreGuide.TECHNICAL_REFERENCE, "jsc:graphics_cards")
            .written();

    private HelpWindowClientTests() {
    }

    /** Frames 95's Help Topics: Find lists the entry, and Display shows it in the topic window. */
    @ClientTest(timeoutTicks = 2400)
    public static void frames95_findsAnEntryAndDisplaysItInTheTopicWindow(final ClientTestContext ctx) {
        onFrames(ctx, "frames_95")
                .then(0, () -> DesktopScreen.requestOpen(HelpViewerApp.KEY))
                .thenWaitUntil(() -> help(ctx) != null && help(ctx).treeRows().contains(MANUAL), SCREEN_WAIT,
                        "Help Topics to list the Technical Reference")
                .thenAssert(0, () -> help(ctx).form() == HelpForm.FRAMES_95, "Frames 95's Help Topics")
                .thenAssert(0, () -> help(ctx).treeRows().getFirst().endsWith(FRAMES_BOOK),
                        "the book written for Frames first")
                .thenScreenshot(2, "help-95-contents")
                .then(SETTLE, () -> ctx.clickDesktop(help(ctx).tabCentre(2)))
                .then(SETTLE, () -> ctx.clickDesktop(help(ctx).searchCentre()))
                .then(1, () -> ctx.type(SEARCHED))
                .thenWaitUntil(() -> row(ctx, CARDS_TITLE) != null, SCREEN_WAIT, "Find to list the entry")
                .then(SETTLE, () -> ctx.clickDesktop(help(ctx).rowCentre(row(ctx, CARDS_TITLE))))
                .then(SETTLE, () -> ctx.clickDesktop(help(ctx).buttonCentre(0)))
                .thenWaitUntil(() -> topic(ctx) != null && words(topic(ctx)).contains(CARDS_TITLE), SCREEN_WAIT,
                        "Display to show the entry in the topic window")
                .thenScreenshot(2, "help-95-topic");
    }

    /** XP's Help and Support Center: its search finds the entry, and the link leads to it. */
    @ClientTest(timeoutTicks = 2400)
    public static void framesXp_searchesAndFollowsTheResult(final ClientTestContext ctx) {
        onFrames(ctx, "frames_xp")
                .then(0, () -> DesktopScreen.requestOpen(HelpViewerApp.KEY))
                .thenWaitUntil(() -> help(ctx) != null && help(ctx).pageLinks().contains(REFERENCE), SCREEN_WAIT,
                        "the Help and Support Center to open on the book written for Frames, the others under it")
                .thenAssert(0, () -> help(ctx).form() == HelpForm.FRAMES_XP, "XP's Help and Support Center")
                .thenAssert(0, () -> words(help(ctx)).startsWith(FRAMES_BOOK), "the book written for Frames first")
                .then(SETTLE, () -> ctx.clickDesktop(help(ctx).searchCentre()))
                .then(1, () -> ctx.type(SEARCHED))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenWaitUntil(() -> help(ctx).pageLinks().contains(CARDS) && help(ctx).linkCentre(CARDS) != null,
                        SCREEN_WAIT, "the search to find the entry")
                .thenScreenshot(2, "help-xp-results")
                .then(SETTLE, () -> ctx.clickDesktop(help(ctx).linkCentre(CARDS)))
                .thenWaitUntil(() -> help(ctx).shownPage().equals(CARDS) && words(help(ctx)).contains(CARDS_TITLE),
                        SCREEN_WAIT, "the result's link to lead to the entry")
                .thenScreenshot(2, "help-xp-entry");
    }

    /** 7's Help and Support, followed from the tree. */
    @ClientTest(timeoutTicks = 2400)
    public static void frames7_followsTheTreeToTheEntry(final ClientTestContext ctx) {
        downTheTree(onFrames(ctx, "frames_7"), ctx, HelpForm.FRAMES_7, FRAMES_BOOK, "help-7");
    }

    /** 11's Get Help, followed from the tree. */
    @ClientTest(timeoutTicks = 2400)
    public static void frames11_followsTheTreeToTheEntry(final ClientTestContext ctx) {
        downTheTree(onFrames(ctx, "frames_11"), ctx, HelpForm.GET_HELP, FRAMES_BOOK, "help-11");
    }

    /** KDE's Help Center, followed from the tree. */
    @ClientTest(timeoutTicks = 2400)
    public static void kde_followsTheTreeToTheEntry(final ClientTestContext ctx) {
        downTheTree(onLinux(ctx, "jsc:kde_plasma"), ctx, HelpForm.KDE, LINUX_BOOK, "help-kde");
    }

    /** CDE's Help Viewer, followed from its topic hierarchy, and Backtrack back up. */
    @ClientTest(timeoutTicks = 3600)
    public static void cde_followsTheHierarchyAndBacktracks(final ClientTestContext ctx) {
        downTheTree(onCde(ctx), ctx, HelpForm.CDE, UNIX_BOOK, "help-cde")
                .then(SETTLE, () -> ctx.clickDesktop(help(ctx).buttonCentre(0)))
                .thenWaitUntil(() -> help(ctx).shownPage().equals(HARDWARE), SCREEN_WAIT,
                        "Backtrack to go back to the section");
    }

    /**
     * GNOME's Help has no tree: it opens on the book written for the system, whose top page lists the other manuals,
     * and its pages lead down from there.
     */
    @ClientTest(timeoutTicks = 2400)
    public static void gnome_followsThePagesDownToTheEntry(final ClientTestContext ctx) {
        onLinux(ctx, "jsc:gnome")
                .then(0, () -> DesktopScreen.requestOpen(HelpViewerApp.KEY))
                .thenWaitUntil(() -> help(ctx) != null && help(ctx).linkCentre(REFERENCE) != null, SCREEN_WAIT,
                        "Help to open on the book written for Ubuntu, the other manuals listed under it")
                .thenAssert(0, () -> help(ctx).form() == HelpForm.YELP, "GNOME's Help")
                .thenAssert(0, () -> words(help(ctx)).contains(LINUX_BOOK), "the book written for Ubuntu first")
                .thenScreenshot(2, "help-gnome-home")
                .then(SETTLE, () -> ctx.clickDesktop(help(ctx).linkCentre(REFERENCE)))
                .thenWaitUntil(() -> help(ctx).linkCentre(BUILDING) != null, SCREEN_WAIT,
                        "the Technical Reference's chapters")
                .then(SETTLE, () -> ctx.clickDesktop(help(ctx).linkCentre(BUILDING)))
                .thenWaitUntil(() -> help(ctx).linkCentre(HARDWARE) != null, SCREEN_WAIT, "the chapter's sections")
                .then(SETTLE, () -> ctx.clickDesktop(help(ctx).linkCentre(HARDWARE)))
                .thenWaitUntil(() -> help(ctx).linkCentre(CARDS) != null, SCREEN_WAIT, "the section's entries")
                .then(SETTLE, () -> ctx.clickDesktop(help(ctx).linkCentre(CARDS)))
                .thenWaitUntil(() -> words(help(ctx)).contains(CARDS_TITLE), SCREEN_WAIT, "the entry")
                .thenScreenshot(2, "help-gnome-entry");
    }

    /**
     * Opens help on the book written for the system, then goes to the Technical Reference and follows its chapter of
     * J's Computers on building computers, the section on the parts and the entry on graphics cards.
     */
    private static ClientTestContext downTheTree(final ClientTestContext chain, final ClientTestContext ctx,
                                                 final HelpForm form, final String book, final String shot) {
        return chain.then(0, () -> DesktopScreen.requestOpen(HelpViewerApp.KEY))
                .thenWaitUntil(() -> help(ctx) != null && reference(ctx) != null, SCREEN_WAIT,
                        "help to open on its books")
                .thenAssert(0, () -> help(ctx).form() == form, "the desktop's own form of help")
                .thenAssert(0, () -> help(ctx).treeRows().getFirst().endsWith(book),
                        "the book written for the system first")
                .thenScreenshot(2, shot + "-home")
                .then(SETTLE, () -> ctx.clickDesktop(reference(ctx)))
                .thenWaitUntil(() -> building(ctx) != null, SCREEN_WAIT, "the Technical Reference's chapters")
                .then(SETTLE, () -> ctx.clickDesktop(building(ctx)))
                .thenWaitUntil(() -> help(ctx).linkCentre(HARDWARE) != null, SCREEN_WAIT,
                        "the chapter's page to list its sections")
                .then(SETTLE, () -> ctx.clickDesktop(help(ctx).linkCentre(HARDWARE)))
                .thenWaitUntil(() -> help(ctx).linkCentre(CARDS) != null, SCREEN_WAIT,
                        "the section's page to list its entries")
                .then(SETTLE, () -> ctx.clickDesktop(help(ctx).linkCentre(CARDS)))
                .thenWaitUntil(() -> help(ctx).shownPage().equals(CARDS) && words(help(ctx)).contains(CARDS_TITLE),
                        SCREEN_WAIT, "the entry on graphics cards")
                .thenAssert(0, () -> help(ctx).treeRows().stream().anyMatch(label -> label.endsWith(CARDS_TITLE)),
                        "the tree opened down to the entry shown")
                .thenScreenshot(2, shot + "-entry");
    }

    /**
     * Where to click for the Technical Reference: its row in the tree when the row is in view, else its link among the
     * other manuals on the book's top page; null while neither is shown.
     */
    @Nullable
    private static int[] reference(final ClientTestContext ctx) {
        final String row = row(ctx, MANUAL);
        return row != null ? help(ctx).rowCentre(row) : help(ctx) == null ? null : help(ctx).linkCentre(REFERENCE);
    }

    /**
     * Where to click for the chapter on building computers: its row in the tree when the row is in view, else its link
     * on the page, as CDE's short topic hierarchy shows only the first chapters; null while neither is shown.
     */
    @Nullable
    private static int[] building(final ClientTestContext ctx) {
        final String row = row(ctx, BUILDING_CHAPTER);
        return row != null ? help(ctx).rowCentre(row) : help(ctx) == null ? null : help(ctx).linkCentre(BUILDING);
    }

    /** A personal computer running that Frames edition, at its desktop. */
    private static ClientTestContext onFrames(final ClientTestContext ctx, final String system) {
        return ctx.thenBuild(0, world -> {
                    world.placeRunningPersonalComputer(COMPUTER,
                            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, system));
                    world.placeMonitor(MONITOR, Direction.EAST);
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .thenWaitUntil(() -> !ctx.screen(DesktopScreen.class).launcherLabels().isEmpty(), SCREEN_WAIT,
                        "the desktop to list its programs");
    }

    /** A personal computer running Ubuntu with that desktop, at its desktop. */
    private static ClientTestContext onLinux(final ClientTestContext ctx, final String desktop) {
        return ctx.thenBuild(0, world -> {
                    world.placeRunningPersonalComputer(COMPUTER,
                            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "ubuntu")).console()
                            .install(desktop);
                    world.placeMonitor(MONITOR, Direction.EAST);
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .thenWaitUntil(() -> !ctx.screen(DesktopScreen.class).launcherLabels().isEmpty(), SCREEN_WAIT,
                        "the desktop to list its programs");
    }

    /** A Mainframe running UNIX with CDE, at its desktop. */
    private static ClientTestContext onCde(final ClientTestContext ctx) {
        return ctx.thenBuild(0, world -> {
                    world.setBlock(COMPUTER, ComputingModule.MAINFRAME.get());
                    final MainframeBlockEntity machine = world.blockEntity(COMPUTER, MainframeBlockEntity.class);
                    TestWorldBuilder.installMainframeParts(machine, StorageTier.SSD, true);
                    machine.installOs(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "unix"));
                    machine.console().install(JsComputers.MODID + ":cde");
                    machine.togglePower();
                    world.placeMonitor(MONITOR, Direction.EAST);
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT);
    }

    /** The first row of the help window's tree or list whose words end with that, or null. */
    @Nullable
    private static String row(final ClientTestContext ctx, final String ending) {
        final HelpViewerApp help = help(ctx);
        if (help == null) {
            return null;
        }
        for (final String label : help.treeRows()) {
            if (label.endsWith(ending) || label.endsWith(ending + " (GPU)")) {
                return help.rowCentre(label) == null ? null : label;
            }
        }
        return null;
    }

    private static String words(final HelpViewerApp help) {
        return String.join(" ", help.pageWords());
    }

    @Nullable
    private static HelpViewerApp help(final ClientTestContext ctx) {
        return app(ctx, HelpViewerApp.KEY);
    }

    @Nullable
    private static HelpViewerApp topic(final ClientTestContext ctx) {
        return app(ctx, HelpViewerApp.TOPIC_KEY);
    }

    @Nullable
    private static HelpViewerApp app(final ClientTestContext ctx, final String key) {
        final DesktopScreen desktop = ctx.screen(DesktopScreen.class);
        final var window = desktop == null ? null : desktop.windowFor(key);
        final IDesktopApp app = window == null ? null : window.app();
        return app instanceof HelpViewerApp help ? help : null;
    }
}
