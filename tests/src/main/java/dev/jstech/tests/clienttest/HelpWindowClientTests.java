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
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.client.os.HelpViewerApp;
import dev.jstech.computers.client.os.IDesktopApp;
import dev.jstech.computers.gui.help.HelpForm;
import dev.jstech.computers.gui.help.HelpTarget;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.core.guide.CoreGuide;
import dev.jstech.tests.testkit.TestWorldBuilder;
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
    private static final String CARDS_TITLE = "Graphics cards";
    private static final String COMPUTERS_CHAPTER = " J's Computers";
    private static final String CHAPTER = HelpTarget.node(CoreGuide.TECHNICAL_REFERENCE, "jsc").written();
    private static final String HARDWARE = HelpTarget.node(CoreGuide.TECHNICAL_REFERENCE, "jsc:hardware").written();
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
                .thenScreenshot(2, "help-95-contents")
                .then(SETTLE, () -> ctx.clickDesktop(help(ctx).tabCentre(2)))
                .then(SETTLE, () -> ctx.clickDesktop(help(ctx).searchCentre()))
                .then(1, () -> ctx.type("graphics"))
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
                .thenWaitUntil(() -> help(ctx) != null && help(ctx).pageLinks().contains(CHAPTER), SCREEN_WAIT,
                        "the Help and Support Center to open on the Technical Reference")
                .thenAssert(0, () -> help(ctx).form() == HelpForm.FRAMES_XP, "XP's Help and Support Center")
                .then(SETTLE, () -> ctx.clickDesktop(help(ctx).searchCentre()))
                .then(1, () -> ctx.type("graphics"))
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
        downTheTree(onFrames(ctx, "frames_7"), ctx, HelpForm.FRAMES_7, "help-7");
    }

    /** 11's Get Help, followed from the tree. */
    @ClientTest(timeoutTicks = 2400)
    public static void frames11_followsTheTreeToTheEntry(final ClientTestContext ctx) {
        downTheTree(onFrames(ctx, "frames_11"), ctx, HelpForm.GET_HELP, "help-11");
    }

    /** KDE's Help Center, followed from the tree. */
    @ClientTest(timeoutTicks = 2400)
    public static void kde_followsTheTreeToTheEntry(final ClientTestContext ctx) {
        downTheTree(onLinux(ctx, "jsc:kde_plasma"), ctx, HelpForm.KDE, "help-kde");
    }

    /** CDE's Help Viewer, followed from its topic hierarchy, and Backtrack back up. */
    @ClientTest(timeoutTicks = 3600)
    public static void cde_followsTheHierarchyAndBacktracks(final ClientTestContext ctx) {
        downTheTree(onCde(ctx), ctx, HelpForm.CDE, "help-cde")
                .then(SETTLE, () -> ctx.clickDesktop(help(ctx).buttonCentre(0)))
                .thenWaitUntil(() -> help(ctx).shownPage().equals(HARDWARE), SCREEN_WAIT,
                        "Backtrack to go back to the section");
    }

    /** GNOME's Help has no tree: its pages lead down, and the trail says where the entry is. */
    @ClientTest(timeoutTicks = 2400)
    public static void gnome_followsThePagesDownToTheEntry(final ClientTestContext ctx) {
        onLinux(ctx, "jsc:gnome")
                .then(0, () -> DesktopScreen.requestOpen(HelpViewerApp.KEY))
                .thenWaitUntil(() -> help(ctx) != null && help(ctx).linkCentre(CHAPTER) != null, SCREEN_WAIT,
                        "Help to open on the Technical Reference's chapters")
                .thenAssert(0, () -> help(ctx).form() == HelpForm.YELP, "GNOME's Help")
                .then(SETTLE, () -> ctx.clickDesktop(help(ctx).linkCentre(CHAPTER)))
                .thenWaitUntil(() -> help(ctx).linkCentre(HARDWARE) != null, SCREEN_WAIT, "the chapter's sections")
                .then(SETTLE, () -> ctx.clickDesktop(help(ctx).linkCentre(HARDWARE)))
                .thenWaitUntil(() -> help(ctx).linkCentre(CARDS) != null, SCREEN_WAIT, "the section's entries")
                .then(SETTLE, () -> ctx.clickDesktop(help(ctx).linkCentre(CARDS)))
                .thenWaitUntil(() -> words(help(ctx)).contains(CARDS_TITLE), SCREEN_WAIT, "the entry")
                .thenScreenshot(2, "help-gnome-entry");
    }

    /** Opens help, then follows the tree's Computers chapter, its Hardware section and the entry on graphics cards. */
    private static ClientTestContext downTheTree(final ClientTestContext chain, final ClientTestContext ctx,
                                                 final HelpForm form, final String shot) {
        return chain.then(0, () -> DesktopScreen.requestOpen(HelpViewerApp.KEY))
                .thenWaitUntil(() -> help(ctx) != null && row(ctx, COMPUTERS_CHAPTER) != null, SCREEN_WAIT,
                        "help to list the Technical Reference's chapters")
                .thenAssert(0, () -> help(ctx).form() == form, "the desktop's own form of help")
                .thenScreenshot(2, shot + "-home")
                .then(SETTLE, () -> ctx.clickDesktop(help(ctx).rowCentre(row(ctx, COMPUTERS_CHAPTER))))
                .thenWaitUntil(() -> help(ctx).linkCentre(HARDWARE) != null, SCREEN_WAIT,
                        "the chapter's page to list its sections")
                .then(SETTLE, () -> ctx.clickDesktop(help(ctx).linkCentre(HARDWARE)))
                .thenWaitUntil(() -> help(ctx).linkCentre(CARDS) != null, SCREEN_WAIT,
                        "the section's page to list its entries")
                .then(SETTLE, () -> ctx.clickDesktop(help(ctx).linkCentre(CARDS)))
                .thenWaitUntil(() -> help(ctx).shownPage().equals(CARDS) && words(help(ctx)).contains(CARDS_TITLE),
                        SCREEN_WAIT, "the entry on graphics cards")
                .thenAssert(0, () -> help(ctx).treeRows().stream().anyMatch(label -> label.endsWith(CARDS_TITLE
                        + " (GPU)")), "the tree opened down to the entry shown")
                .thenScreenshot(2, shot + "-entry");
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
