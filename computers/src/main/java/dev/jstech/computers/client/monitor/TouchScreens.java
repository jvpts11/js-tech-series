/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.monitor;

import dev.jstech.computers.gui.MonitorGlass;
import dev.jstech.computers.menu.IMonitorMenu;
import dev.jstech.computers.monitor.TouchGesture;
import dev.jstech.core.client.live.LiveGraphics;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

/**
 * The screen the player's game holds on a touch monitor's glass (computers/docs/design/peripherals.md): the one its
 * machine opens for the player who touched it, which the game keeps from coming up over the world and draws on the
 * glass instead, moving it on tick by tick and handing it what the finger does there.
 *
 * <p>The session is the player's at that monitor as any other, so the server sees nothing different: it opened a menu
 * for a player at a monitor, and closes it when the player walks away. A container of the player's own (the inventory,
 * a chest) leaves the monitor first, since it would take the machine's menu's place; the chat, the pause menu and the
 * typing line come up over the world with the screen still held.
 */
public final class TouchScreens {

    /* The screen held, or none. */
    @Nullable
    private static Held held;
    /* The touch monitor a touch was sent for, waiting for the screen its machine opens, and the tick it stops at. */
    @Nullable
    private static BlockPos expected;
    private static long expectedUntil;
    /* Where the last press or drag was, for the next drag's step. */
    private static double lastX;
    private static double lastY;

    /* A held screen is laid out on an area just bigger than the glass, as a face is, the glass at half the margin. */
    private static final int AREA_WIDTH = MonitorGlass.WIDTH + MonitorGlass.BESIDE;
    private static final int AREA_HEIGHT = MonitorGlass.HEIGHT + MonitorGlass.ABOVE_AND_BELOW;
    private static final int GLASS_X = MonitorGlass.BESIDE / 2;
    private static final int GLASS_Y = MonitorGlass.ABOVE_AND_BELOW / 2;
    /* How long a touch waits for its machine's screen, in ticks: a monitor that has nothing to show says so first. */
    private static final int WAIT_TICKS = 40;
    /* Where the finger is when it is on no part of the glass, which nothing lights for. */
    private static final int FAR = -10_000;

    private TouchScreens() {
    }

    /** Hands the game's bus what this answers, once, from the mod's client constructor. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(ScreenEvent.Opening.class, TouchScreens::onOpening);
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, TouchScreens::onTick);
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> drop());
    }

    /** Whether the game holds the screen of the touch monitor whose screen stands at {@code monitor}. */
    public static boolean holds(final BlockPos monitor) {
        return held != null && held.monitor().equals(monitor);
    }

    /** The screen held, or null. */
    @Nullable
    public static Screen held() {
        return held == null ? null : held.screen();
    }

    /** Waits a moment for the screen the machine behind the touch monitor at {@code monitor} opens. */
    static void expect(final BlockPos monitor) {
        final Minecraft minecraft = Minecraft.getInstance();
        expected = monitor.immutable();
        expectedUntil = minecraft.level == null ? 0L : minecraft.level.getGameTime() + WAIT_TICKS;
    }

    /** Whether a touch on the monitor at {@code monitor} is still waiting for its screen. */
    static boolean waiting(final BlockPos monitor) {
        return monitor.equals(expected);
    }

    /**
     * Draws the screen held on the touch monitor whose screen stands at {@code monitor} into its picture, with the finger
     * where the crosshair is; false when it holds none there, and the picture is the machine's as usual.
     */
    public static boolean paint(final GuiGraphics g, final BlockPos monitor, final float partialTick) {
        if (!holds(monitor)) {
            return false;
        }
        if (g instanceof LiveGraphics live) {
            live.shift(-GLASS_X, -GLASS_Y);
        }
        final double[] finger = TouchFinger.pointOn(monitor);
        final int x = finger == null ? FAR : (int) Math.floor(finger[0]) + GLASS_X;
        final int y = finger == null ? FAR : (int) Math.floor(finger[1]) + GLASS_Y;
        held.glass().paintTouched(g, x, y, partialTick);
        return true;
    }

    /**
     * Hands the screen held on the touch monitor whose screen stands at {@code monitor} what the finger did, at points
     * of its picture; after a tap on something that takes typing, the typing line comes up.
     */
    static void deliver(final BlockPos monitor, final List<TouchGesture.Event> events) {
        if (!holds(monitor)) {
            return;
        }
        final Screen screen = held.screen();
        for (final TouchGesture.Event event : events) {
            final double x = event.x() + GLASS_X;
            final double y = event.y() + GLASS_Y;
            switch (event.kind()) {
                case PRESS -> screen.mouseClicked(x, y, event.button());
                case DRAG -> screen.mouseDragged(x, y, event.button(), x - lastX, y - lastY);
                case RELEASE -> screen.mouseReleased(x, y, event.button());
            }
            lastX = x;
            lastY = y;
            if (event.kind() == TouchGesture.Kind.RELEASE && event.button() == TouchGesture.LEFT && held != null
                    && held.glass().takesTyping()) {
                TouchTypingScreen.open();
            }
        }
    }

    /*
     * The machine's screen for a monitor the player touched is held on its glass instead of coming up; any other
     * screen but the typing line leaves the monitor first.
     */
    private static void onOpening(final ScreenEvent.Opening event) {
        final Screen opening = event.getNewScreen();
        if (opening instanceof TouchTypingScreen) {
            return;
        }
        if (opening instanceof AbstractContainerScreen<?> container && container.getMenu() instanceof IMonitorMenu at
                && (at.monitorPos().equals(expected) || holds(at.monitorPos()))
                && opening instanceof ITouchGlass glass) {
            event.setCanceled(true);
            hold(at.monitorPos().immutable(), opening, glass);
            return;
        }
        // A screen of the player's own inventory, or another container, would take the place of the machine's menu.
        if (held != null && opening instanceof AbstractContainerScreen<?>) {
            leave();
        }
    }

    private static void onTick(final ClientTickEvent.Post event) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (expected != null && (minecraft.level == null || minecraft.level.getGameTime() > expectedUntil)) {
            expected = null;
        }
        if (held == null) {
            return;
        }
        // The server closed the session (the player walked away, the machine went down): the glass is the face again.
        if (minecraft.player == null || !(held.screen() instanceof AbstractContainerScreen<?> container)
                || minecraft.player.containerMenu != container.getMenu()) {
            drop();
            return;
        }
        held.screen().tick();
    }

    /* Holds a screen on the glass, laid out there, in place of whatever was held before. */
    private static void hold(final BlockPos monitor, final Screen screen, final ITouchGlass glass) {
        final Held before = held;
        held = new Held(monitor, screen, glass);
        expected = null;
        if (before != null && before.screen() != screen) {
            before.screen().removed();
        }
        glass.holdOnGlass();
        screen.init(Minecraft.getInstance(), AREA_WIDTH, AREA_HEIGHT);
    }

    /* Leaves the monitor as closing its screen would, without a screen to close: the server is told, and it is let go. */
    private static void leave() {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.getConnection() != null
                && held.screen() instanceof AbstractContainerScreen<?> container
                && minecraft.player.containerMenu == container.getMenu()) {
            minecraft.getConnection().send(new ServerboundContainerClosePacket(container.getMenu().containerId));
            minecraft.player.containerMenu = minecraft.player.inventoryMenu;
        }
        drop();
    }

    /* Lets go of the screen held, and of the finger on it. */
    private static void drop() {
        TouchFinger.cancel();
        expected = null;
        if (held != null) {
            final Screen screen = held.screen();
            held = null;
            screen.removed();
        }
    }

    /** A screen held on a touch monitor's glass: the monitor whose screen it is, and the screen. */
    private record Held(BlockPos monitor, Screen screen, ITouchGlass glass) {
    }
}
