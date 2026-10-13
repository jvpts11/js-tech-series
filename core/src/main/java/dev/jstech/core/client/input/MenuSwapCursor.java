/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import dev.jstech.core.JsCore;
import dev.jstech.core.menu.CoreMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * Keeps the cursor where it was when the server puts one Core window in place of another.
 *
 * <p>The server puts a new window in front of a player by closing the open one first. The game closes the screen and
 * takes the mouse back for the world, which puts the cursor in the middle of the window, then frees it again for the
 * screen that comes next, still in the middle. A player at a screen that moves on by itself (a system coming up after
 * its start, a restart, the next page of an installer) had the cursor pulled from under the hand every time. So when a
 * Core window closes and another opens within a few ticks, the cursor goes back to where it was.
 */
@EventBusSubscriber(modid = JsCore.MODID, value = Dist.CLIENT)
public final class MenuSwapCursor {

    /** How soon after a Core window closes the next one counts as taking its place, in client ticks. */
    private static final int SWAP_TICKS = 4;

    /* Where the cursor was when the last Core window closed, and when; null when there is nothing to give back. */
    @Nullable
    private static Closed closed;

    private MenuSwapCursor() {
    }

    @SubscribeEvent
    public static void onClosing(final ScreenEvent.Closing event) {
        if (!isCoreWindow(event.getScreen())) {
            return;
        }
        /*
         * Read from the window rather than from the game's mouse handler: the handler only learns where the cursor
         * is when the window is told it moved, and the window itself always knows.
         */
        final Minecraft minecraft = Minecraft.getInstance();
        final double[] x = new double[1];
        final double[] y = new double[1];
        GLFW.glfwGetCursorPos(minecraft.getWindow().getWindow(), x, y);
        closed = new Closed(x[0], y[0], minecraft.gui.getGuiTicks());
    }

    @SubscribeEvent
    public static void onInit(final ScreenEvent.Init.Post event) {
        final Closed last = closed;
        if (last == null) {
            return;
        }
        closed = null;
        final Minecraft minecraft = Minecraft.getInstance();
        if (isCoreWindow(event.getScreen()) && minecraft.gui.getGuiTicks() - last.tick() <= SWAP_TICKS) {
            InputConstants.grabOrReleaseMouse(minecraft.getWindow().getWindow(), GLFW.GLFW_CURSOR_NORMAL,
                    last.x(), last.y());
        }
    }

    private static boolean isCoreWindow(final Screen screen) {
        return screen instanceof MenuAccess<?> access && access.getMenu() instanceof CoreMenu;
    }

    private record Closed(double x, double y, int tick) {
    }
}
