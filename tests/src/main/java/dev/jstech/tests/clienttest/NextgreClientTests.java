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
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.client.os.DesktopWindow;
import dev.jstech.computers.client.os.NextgreStudioApp;
import dev.jstech.computers.engine.NetworkEngines;
import dev.jstech.computers.engine.nextgre.NextgrePlanView;
import dev.jstech.tests.testkit.CraftFiles;
import dev.jstech.tests.testkit.TestPlanner;
import dev.jstech.tests.testkit.TestWorldBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * The Nextgre Planner Studio as a player uses it: a craft's plan explained, then explained and run, the boxes filling
 * in with what each step took; the hints marked on the plan; the statistics, another mod's among them; a rule of the
 * planner switched off; and the plans kept in the history.
 */
public final class NextgreClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 120;
    private static final int WORK_WAIT = 600;
    private static final int BOOT_WAIT = 1_200;
    private static final BlockPos COMPUTER = new BlockPos(5, 2, 3);
    private static final BlockPos COMPUTER_CABLE = new BlockPos(4, 2, 3);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 3);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 3);
    private static final String STUDIO = "jsc:nextgre_studio";
    private static final String CRAFT_PLANKS = "CRAFT 8 oak_planks";
    private static final ResourceLocation FRAMES_11 = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID,
            "frames_11");

    private NextgreClientTests() {
    }

    /** A plan explained, then run and measured; the statistics, a rule switched off, and the history. */
    @ClientTest(timeoutTicks = 3000)
    public static void studio_explainsRunsAndKeepsAPlan(final ClientTestContext ctx) {
        openStudio(ctx)
                .then(2, () -> {
                    studio(ctx).typeStatement(CRAFT_PLANKS);
                    ctx.clickDesktop(studio(ctx).buttonCenter(false));
                })
                .thenWaitUntil(() -> !studio(ctx).asking() && !studio(ctx).nodeTitles().isEmpty(), SCREEN_WAIT,
                        "the plan to come back")
                .then(0, () -> expect(ctx, studio(ctx).nodeTitles().get(0).startsWith("Craft Oak Planks x8")
                                && studio(ctx).planState() == NextgrePlanView.PLANNED,
                        "the craft at the top, planned and not run; got " + studio(ctx).nodeTitles()))
                .then(0, () -> expect(ctx, studio(ctx).alternativeLines().stream().anyMatch(line -> line.contains(
                                "chosen")), "the chosen plan is marked; got " + studio(ctx).alternativeLines()))
                .thenScreenshot(2, "nextgre-explain")
                .then(2, () -> ctx.clickDesktop(studio(ctx).buttonCenter(true)))
                .thenWaitUntil(() -> studio(ctx).planState() == NextgrePlanView.DONE && studio(ctx).planMeasured(),
                        WORK_WAIT, "the craft to run and its plan to fill in")
                .thenScreenshot(2, "nextgre-analyze")
                .then(2, () -> ctx.clickDesktop(studio(ctx).tabCenter(NextgreStudioApp.TAB_STATISTICS)))
                .then(2, () -> expect(ctx, studio(ctx).tab() == NextgreStudioApp.TAB_STATISTICS
                                && studio(ctx).statisticLines().stream().anyMatch(line -> line.contains(
                                TestPlanner.STATISTIC_VALUE)),
                        "the statistics, another mod's among them; got " + studio(ctx).statisticLines()))
                .thenScreenshot(1, "nextgre-statistics")
                .then(2, () -> ctx.clickDesktop(studio(ctx).tabCenter(NextgreStudioApp.TAB_RULES)))
                .then(2, () -> studio(ctx).toggleRule("nextgre:weigh_machines"))
                .thenWaitUntil(() -> studio(ctx).ruleLines().contains("nextgre:weigh_machines=off"), SCREEN_WAIT,
                        "the rule to be switched off")
                .thenScreenshot(2, "nextgre-rules")
                .then(2, () -> ctx.clickDesktop(studio(ctx).tabCenter(NextgreStudioApp.TAB_HISTORY)))
                .then(2, () -> expect(ctx, studio(ctx).historyLines().size() >= 2,
                        "both plans are kept; got " + studio(ctx).historyLines()))
                .thenScreenshot(1, "nextgre-history")
                .then(SETTLE, () -> leave(ctx))
                .thenAwaitNoScreen(SCREEN_WAIT);
    }

    /** A hint is marked on the box it changed. */
    @ClientTest(timeoutTicks = 2400)
    public static void studio_marksTheHintsWhereTheyChangedThePlan(final ClientTestContext ctx) {
        openStudio(ctx)
                .then(2, () -> {
                    studio(ctx).typeStatement(CRAFT_PLANKS + " MAX PARALLEL 1");
                    ctx.key(GLFW.GLFW_KEY_F7);
                })
                .thenWaitUntil(() -> !studio(ctx).asking() && !studio(ctx).nodeTitles().isEmpty(), SCREEN_WAIT,
                        "the plan to come back")
                .then(0, () -> expect(ctx, studio(ctx).nodeHints().contains("MAX PARALLEL 1"),
                        "the hint on the box it changed; got " + studio(ctx).nodeHints()))
                .thenScreenshot(2, "nextgre-hint")
                .then(SETTLE, () -> leave(ctx))
                .thenAwaitNoScreen(SCREEN_WAIT);
    }

    private static ClientTestContext openStudio(final ClientTestContext ctx) {
        return ctx.thenBuild(0, world -> {
                    final TestWorldBuilder.CraftingNetwork net = world.buildCraftingNetwork();
                    world.setBlock(COMPUTER_CABLE, ComputingModule.ETHERNET_CABLE);
                    final PersonalComputerBlockEntity pc = world.placeRunningPersonalComputer(COMPUTER, FRAMES_11);
                    pc.console().install(STUDIO);
                    world.placeMonitor(MONITOR, Direction.EAST);
                    net.mainframe().installEngine(NetworkEngines.NEXTGRE_IQL.program());
                    net.mainframe().activateEngine(NetworkEngines.NEXTGRE_IQL.program());
                    net.cc().loadPattern(CraftFiles.oakPlanks());
                    net.seed(Items.OAK_LOG, 8);
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .thenWaitUntil(() -> !ctx.screen(DesktopScreen.class).launcherLabels().isEmpty(), SCREEN_WAIT,
                        "the desktop to list its programs")
                .then(SETTLE, () -> DesktopScreen.requestOpen(STUDIO))
                .thenWaitUntil(() -> studio(ctx) != null && !studio(ctx).statisticLines().isEmpty(), SCREEN_WAIT,
                        "the studio with the network's NextgreIQL");
    }

    /* An assertion read when its step runs, so its words may say what the window shows then. */
    private static void expect(final ClientTestContext ctx, final boolean condition, final String message) {
        ctx.assertTrue(condition, message);
    }

    private static void leave(final ClientTestContext ctx) {
        ctx.key(GLFW.GLFW_KEY_ESCAPE);
        if (ctx.mc().screen != null) {
            ctx.key(GLFW.GLFW_KEY_ESCAPE);
        }
        if (ctx.mc().screen != null) {
            ctx.key(GLFW.GLFW_KEY_ESCAPE);
        }
    }

    @Nullable
    private static NextgreStudioApp studio(final ClientTestContext ctx) {
        if (!(ctx.mc().screen instanceof DesktopScreen desktop)) {
            return null;
        }
        final DesktopWindow window = desktop.windowFor(STUDIO);
        return window != null && window.app() instanceof NextgreStudioApp app ? app : null;
    }
}
