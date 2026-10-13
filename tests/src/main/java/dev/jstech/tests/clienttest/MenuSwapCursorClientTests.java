/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import com.mojang.blaze3d.platform.InputConstants;
import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.client.SystemBootScreen;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.tests.testkit.TestWorldBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

/**
 * The cursor stays under the player's hand while the server moves a monitor on from one window to the next: a system
 * coming up after its start hands the screen to its desktop, and the desktop opens with the cursor where it was left.
 */
public final class MenuSwapCursorClientTests {

    private static final int SETTLE = 4;
    private static final int BOOT_WAIT = 1_200;
    /** How far the cursor may stand from where it was put, in the window's pixels. */
    private static final double SLACK = 1.0;

    private static final BlockPos MACHINE = new BlockPos(5, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 2);

    private MenuSwapCursorClientTests() {
    }

    @ClientTest(timeoutTicks = 3600)
    public static void systemComingUp_leavesTheCursorWhereThePlayerHadIt(final ClientTestContext ctx) {
        /* A quarter of the way in from the left and a third down: well clear of the middle the game would use. */
        final double[] put = new double[2];
        final boolean[] focused = new boolean[1];
        final int[] settled = new int[1];
        ctx.thenBuild(0, world -> {
                    world.setBlock(MACHINE, ComputingModule.MAINFRAME.get());
                    final MainframeBlockEntity machine = world.blockEntity(MACHINE, MainframeBlockEntity.class);
                    TestWorldBuilder.installMainframeParts(machine, StorageTier.SSD, true);
                    machine.installOs(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "frames_11"));
                    machine.setSystemWelcome(machine.systemWelcome().met());
                    machine.togglePower();
                    world.placeMonitor(MONITOR, Direction.EAST);
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(SystemBootScreen.class, BOOT_WAIT)
                .then(SETTLE, () -> {
                    final long window = ctx.mc().getWindow().getWindow();
                    put[0] = Math.floor(ctx.mc().getWindow().getScreenWidth() / 4.0);
                    put[1] = Math.floor(ctx.mc().getWindow().getScreenHeight() / 3.0);
                    InputConstants.grabOrReleaseMouse(window, GLFW.GLFW_CURSOR_NORMAL, put[0], put[1]);
                    focused[0] = ctx.mc().isWindowActive();
                })
                /*
                 * The window has to hold the focus the whole way, so it is looked at on every tick until the check.
                 * Run beside other clients, one of theirs can take it in between and give it back, and the cursor a
                 * window gets back is not where this checks it was left, so a run that lost it says nothing here.
                 */
                .thenWaitUntil(() -> {
                    focused[0] &= ctx.mc().isWindowActive();
                    return ctx.mc().screen instanceof DesktopScreen;
                }, BOOT_WAIT, "the desktop to open")
                .thenWaitUntil(() -> {
                    focused[0] &= ctx.mc().isWindowActive();
                    return ++settled[0] > SETTLE;
                }, SETTLE * 4, "the desktop to settle")
                .then(0, () -> {
                    if (!focused[0] || !ctx.mc().isWindowActive()) {
                        return;
                    }
                    final double[] x = new double[1];
                    final double[] y = new double[1];
                    GLFW.glfwGetCursorPos(ctx.mc().getWindow().getWindow(), x, y);
                    ctx.assertTrue(Math.abs(x[0] - put[0]) <= SLACK && Math.abs(y[0] - put[1]) <= SLACK,
                            "the desktop opens with the cursor where the player left it, at " + put[0] + ", "
                                    + put[1] + ", but it is at " + x[0] + ", " + y[0]);
                });
    }
}
