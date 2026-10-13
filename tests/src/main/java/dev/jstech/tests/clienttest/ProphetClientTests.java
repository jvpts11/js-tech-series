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
import dev.jstech.computers.client.os.ProphetConsoleApp;
import dev.jstech.computers.engine.NetworkEngines;
import dev.jstech.tests.testkit.CraftFiles;
import dev.jstech.tests.testkit.TestWorldBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * The Prophet Reactive Console as a player uses it: a state declared at the prompt and applied, made up and held, with
 * its graph; a watch set; what the engine did; and its settings changed.
 */
public final class ProphetClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 120;
    private static final int WORK_WAIT = 800;
    private static final int BOOT_WAIT = 1_200;
    private static final BlockPos COMPUTER = new BlockPos(5, 2, 3);
    private static final BlockPos COMPUTER_CABLE = new BlockPos(4, 2, 3);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 3);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 3);
    private static final String CONSOLE = "jsc:prophet_console";
    private static final ResourceLocation FRAMES_11 = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID,
            "frames_11");

    private ProphetClientTests() {
    }

    /** A state declared and held, a watch set, what was done, and the settings changed. */
    @ClientTest(timeoutTicks = 3200)
    public static void console_declaresAStateAndShowsItHeld(final ClientTestContext ctx) {
        openConsole(ctx)
                .then(2, () -> {
                    console(ctx).typeStatement("KEEP oak_planks >= 16");
                    ctx.clickDesktop(console(ctx).buttonCenter(false));
                })
                .thenWaitUntil(() -> !console(ctx).asking() && !console(ctx).stateLines().isEmpty(), SCREEN_WAIT,
                        "the state to be listed")
                .thenWaitUntil(() -> console(ctx).stateLines().get(0).contains("| Holding |"), WORK_WAIT,
                        "the state to be made up and held")
                .then(0, () -> expect(ctx, console(ctx).graphPoints() > 0,
                        "its graph draws the levels seen; got " + console(ctx).graphPoints()))
                .thenScreenshot(2, "prophet-states")
                .then(2, () -> {
                    console(ctx).typeStatement("WATCH oak_log < 2 DO CRAFT oak_planks TO 32");
                    ctx.clickDesktop(console(ctx).buttonCenter(false));
                })
                .then(2, () -> ctx.clickDesktop(console(ctx).tabCenter(ProphetConsoleApp.TAB_SUBSCRIPTIONS)))
                .thenWaitUntil(() -> console(ctx).watchLines().size() == 1, SCREEN_WAIT, "the watch to be listed")
                .thenScreenshot(2, "prophet-subscriptions")
                .then(2, () -> ctx.clickDesktop(console(ctx).tabCenter(ProphetConsoleApp.TAB_REACTIONS)))
                .then(2, () -> expect(ctx, console(ctx).reactionLines().stream().anyMatch(line -> line.startsWith(
                        "asked for")), "what it did is listed; got " + console(ctx).reactionLines()))
                .thenScreenshot(1, "prophet-reactions")
                .then(2, () -> ctx.clickDesktop(console(ctx).tabCenter(ProphetConsoleApp.TAB_SETTINGS)))
                .then(2, () -> console(ctx).configure(100, 256L, true))
                .thenWaitUntil(() -> console(ctx).settings() != null && console(ctx).settings().interval() == 100,
                        SCREEN_WAIT, "the settings to be set")
                .thenScreenshot(2, "prophet-settings")
                .then(SETTLE, () -> leave(ctx))
                .thenAwaitNoScreen(SCREEN_WAIT);
    }

    private static ClientTestContext openConsole(final ClientTestContext ctx) {
        return ctx.thenBuild(0, world -> {
                    final TestWorldBuilder.CraftingNetwork net = world.buildCraftingNetwork();
                    world.setBlock(COMPUTER_CABLE, ComputingModule.ETHERNET_CABLE);
                    final PersonalComputerBlockEntity pc = world.placeRunningPersonalComputer(COMPUTER, FRAMES_11);
                    pc.console().install(CONSOLE);
                    world.placeMonitor(MONITOR, Direction.EAST);
                    net.mainframe().installEngine(NetworkEngines.PROPHET_YOURIQL.program());
                    net.mainframe().activateEngine(NetworkEngines.PROPHET_YOURIQL.program());
                    net.cc().loadPattern(CraftFiles.oakPlanks());
                    net.seed(Items.OAK_LOG, 16);
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .thenWaitUntil(() -> !ctx.screen(DesktopScreen.class).launcherLabels().isEmpty(), SCREEN_WAIT,
                        "the desktop to list its programs")
                .then(SETTLE, () -> DesktopScreen.requestOpen(CONSOLE))
                .thenWaitUntil(() -> console(ctx) != null && console(ctx).settings() != null
                        && console(ctx).settings().interval() > 0, SCREEN_WAIT,
                        "the console with the network's Prophet YourIQL");
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
    private static ProphetConsoleApp console(final ClientTestContext ctx) {
        if (!(ctx.mc().screen instanceof DesktopScreen desktop)) {
            return null;
        }
        final DesktopWindow window = desktop.windowFor(CONSOLE);
        return window != null && window.app() instanceof ProphetConsoleApp app ? app : null;
    }
}
