/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.client.CommandPromptScreen;
import dev.jstech.computers.client.CrashScreen;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.OpenWindow;
import dev.jstech.tests.testkit.EraKit;
import dev.jstech.tests.testkit.EraNetwork;
import dev.jstech.tests.testkit.ServerStacks;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import org.lwjgl.glfw.GLFW;

/**
 * A server in a rack following the rule of its system, at the rack's own console. The player at the monitor beside
 * the rack watches a Linux server's kernel print its lines as it ends what holds the most, then types kill -9 1 and
 * watches that server go down on its panic, and come back to its prompt once the page has stood its time.
 */
public final class RackCrashClientTests {

    private static final ResourceLocation DEBIAN = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "debian");
    private static final BlockPos RACK_MONITOR = new BlockPos(6, 2, 6);
    private static final BlockPos AT_THE_RACK_MONITOR = new BlockPos(6, 2, 8);
    private static final String CALCULATOR_KEY = "jsc:calculator";

    private RackCrashClientTests() {
    }

    @ClientTest(timeoutTicks = 9000)
    public static void linuxServer_endsWhatHoldsTheMostThenPanicsOnItsInitAtTheConsole(final ClientTestContext ctx) {
        EraJourney.lay(ctx, EraKit.STANDARD)
                .thenBuild(0, world -> {
                    world.placeMonitor(RACK_MONITOR, Direction.SOUTH, EraKit.STANDARD.monitor());
                    // A server with one 8 GB stick, which a desktop's worth of Calculators runs out of.
                    world.blockEntity(EraNetwork.RACK, ServerRackBlockEntity.class).getServers()
                            .setStackInSlot(0, ServerStacks.defaultServer());
                })
                .thenServer(EraJourney.SETTLE, level -> server(ctx, level).installOs(DEBIAN))
                .thenTeleport(EraJourney.SETTLE, AT_THE_RACK_MONITOR, Direction.NORTH)
                .thenRightClick(EraJourney.SETTLE, RACK_MONITOR)
                .thenAwaitScreen(CommandPromptScreen.class, EraJourney.BOOT_WAIT)
                .thenServer(EraJourney.SETTLE * 4, level -> server(ctx, level).setOpenWindows(
                        calculators(OpenWindow.MAX)))
                .thenWaitUntil(() -> said(ctx, "Out of memory: Killed process"), EraJourney.SCREEN_WAIT,
                        "the kernel's line at the server's console")
                .thenServer(0, level -> {
                    final IOsHost server = server(ctx, level);
                    ctx.assertTrue(server.crashPage() == null && server.isRunning()
                                    && server.openWindows().size() < OpenWindow.MAX,
                            "the server should end windows and stay up; it kept " + server.openWindows().size());
                })
                .thenScreenshot(EraJourney.SETTLE, "server-out-of-memory")
                .then(EraJourney.SETTLE, () -> ctx.type("kill -9 1"))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenAwaitScreen(CrashScreen.class, EraJourney.SCREEN_WAIT)
                .thenAssert(0, () -> ctx.screen(CrashScreen.class).page().lines().stream()
                                .anyMatch(line -> line.contains("Attempted to kill init!")),
                        "the page should be the kernel's panic on its init killed")
                .thenScreenshot(EraJourney.SETTLE, "server-kernel-panic")
                .thenAwaitScreen(CommandPromptScreen.class, EraJourney.BOOT_WAIT)
                .thenServer(0, level -> ctx.assertTrue(server(ctx, level).memoryLog().isEmpty(),
                        "a server that started over begins its kernel's log again"));
    }

    /* That many Calculator windows, as a desktop leaves them open on the machine. */
    private static List<OpenWindow> calculators(final int count) {
        final List<OpenWindow> windows = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            windows.add(new OpenWindow(CALCULATOR_KEY, 10 + i, 10 + i, 120, 140, false, false, "", 1));
        }
        return windows;
    }

    private static boolean said(final ClientTestContext ctx, final String what) {
        return ctx.mc().screen instanceof CommandPromptScreen<?> screen
                && screen.scrollbackText().stream().anyMatch(line -> line.contains(what));
    }

    private static IOsHost server(final ClientTestContext ctx, final ServerLevel level) {
        return TestWorldBuilder.at(level, ctx.origin()).blockEntity(EraNetwork.RACK, ServerRackBlockEntity.class)
                .unitHost(0);
    }
}
