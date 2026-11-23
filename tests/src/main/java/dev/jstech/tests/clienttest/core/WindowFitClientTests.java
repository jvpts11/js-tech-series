/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import com.mojang.blaze3d.platform.Window;
import dev.jstech.core.jstechseries.energy.EnergyConverters;
import dev.jstech.core.jstechseries.energy.client.EnergyConverterScreen;
import dev.jstech.core.jstechseries.probe.NetworkProbe;
import dev.jstech.core.jstechseries.probe.ProbeLayout;
import dev.jstech.core.jstechseries.probe.client.NetworkProbeScreen;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

/**
 * Screens of the series in a small window, the game's own 854 by 480 at interface scale 2, where a screen taller than
 * 240 lost its edges: a container screen (the FE Converter's, 255 tall) and a panel screen (the Network Probe's, 270
 * tall) open whole inside the window, the scale lowered only while they are open and back as each closes.
 */
public final class WindowFitClientTests {

    private static final int SMALL_WIDTH = 854;
    private static final int SMALL_HEIGHT = 480;
    private static final int TEST_WIDTH = 960;
    private static final int TEST_HEIGHT = 640;
    private static final int SCALE = 2;
    private static final double SAME = 1.0E-3;
    private static final int FLOOR = 2;
    private static final BlockPos STAND = new BlockPos(0, FLOOR, -2);
    private static final BlockPos CONVERTER = new BlockPos(0, FLOOR, 0);
    private static final int WAIT = 200;
    private static final int SETTLE = 10;

    private WindowFitClientTests() {
    }

    @ClientTest(timeoutTicks = 800)
    public static void screens_tooBigForASmallWindowAreFittedAndTheScaleComesBack(final ClientTestContext ctx) {
        final int[] chosen = new int[1];
        ctx.then(0, () -> {
            chosen[0] = ctx.mc().options.guiScale().get();
            GLFW.glfwSetWindowSize(ctx.mc().getWindow().getWindow(), SMALL_WIDTH, SMALL_HEIGHT);
        })
                .thenWaitUntil(() -> ctx.mc().getWindow().getWidth() == SMALL_WIDTH
                        && ctx.mc().getWindow().getHeight() == SMALL_HEIGHT, WAIT, "the window to be 854 by 480")
                .then(0, () -> {
                    ctx.mc().options.guiScale().set(SCALE);
                    ctx.mc().resizeDisplay();
                })
                .thenServer(0, level -> level.setBlockAndUpdate(ctx.abs(CONVERTER), EnergyConverters.LOW.get()
                        .defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH)))
                .thenTeleport(0, STAND, Direction.SOUTH)
                .thenAwaitLandDrawn(WAIT)
                .then(SETTLE, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(CONVERTER))))
                .thenRightClick(SETTLE, CONVERTER)
                .thenAwaitScreen(EnergyConverterScreen.class, WAIT)
                .thenAssert(SETTLE, () -> containerWhole(ctx) && lowered(ctx),
                        "the converter's screen opens whole in the window, the scale lowered below 2")
                .thenScreenshot(0, "converter-854x480")
                .then(0, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenAwaitNoScreen(WAIT)
                .thenAssert(SETTLE, () -> back(ctx), "the scale back to 2 once it closes")
                .thenGive(0, NetworkProbe.ITEM.get().getDefaultInstance())
                .then(SETTLE, () -> ctx.mc().setScreen(new NetworkProbeScreen(InteractionHand.MAIN_HAND)))
                .thenAssert(SETTLE, () -> ctx.mc().getWindow().getGuiScaledHeight() >= ProbeLayout.HEIGHT
                        && ctx.mc().getWindow().getGuiScaledWidth() >= ProbeLayout.WIDTH && lowered(ctx),
                        "the probe's panel fits the window, the scale lowered below 2")
                .thenScreenshot(0, "probe-854x480")
                .then(0, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenAwaitNoScreen(WAIT)
                .thenAssert(SETTLE, () -> back(ctx), "the scale back to 2 once the probe closes")
                .then(0, () -> {
                    ctx.mc().options.guiScale().set(chosen[0]);
                    GLFW.glfwSetWindowSize(ctx.mc().getWindow().getWindow(), TEST_WIDTH, TEST_HEIGHT);
                })
                .thenWaitUntil(() -> ctx.mc().getWindow().getWidth() == TEST_WIDTH, WAIT,
                        "the window back to its size")
                .then(0, () -> ctx.mc().resizeDisplay())
                .thenServer(0, level -> level.setBlockAndUpdate(ctx.abs(CONVERTER), Blocks.AIR.defaultBlockState()));
    }

    /* Whether the open container screen lies whole inside the window. */
    private static boolean containerWhole(final ClientTestContext ctx) {
        return ctx.mc().screen instanceof AbstractContainerScreen<?> screen && screen.getGuiLeft() >= 0
                && screen.getGuiTop() >= 0 && screen.getGuiLeft() + screen.getXSize() <= screen.width
                && screen.getGuiTop() + screen.getYSize() <= screen.height;
    }

    private static boolean lowered(final ClientTestContext ctx) {
        return ctx.mc().getWindow().getGuiScale() < SCALE - SAME;
    }

    private static boolean back(final ClientTestContext ctx) {
        final Window window = ctx.mc().getWindow();
        return Math.abs(window.getGuiScale() - SCALE) < SAME;
    }
}
