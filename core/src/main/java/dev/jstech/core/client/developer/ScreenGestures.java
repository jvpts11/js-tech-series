/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.developer;

import net.minecraft.client.gui.screens.Screen;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * The mouse's and the keyboard's gestures on a screen as the game hands them to it: a click, a drag (the button
 * pressed, moved in steps and let go), a turn of the wheel, a character typed and a key pressed. The agent's tools,
 * the studio's recorded tests and the test kit all make them here, so a gesture one of them makes is the gesture the
 * others replay.
 *
 * <p>Each goes through the screen's events before and after the screen hears it, as the game's own mouse and
 * keyboard handlers send them, so what a mod does round a screen (a button drawn beside a monitor's frame, a key it
 * listens for) answers a gesture made here as it answers the player's hand. While one is being made, {@link #making}
 * says so, and what records the player's own hand leaves it alone.
 */
public final class ScreenGestures {

    /* How many moves a drag is made of between where it starts and where it ends. */
    private static final int DRAG_MOVES = 8;

    /* Whether a gesture made here is being handed to a screen now. */
    private static boolean making;

    private ScreenGestures() {
    }

    /** Whether a gesture made here, not by the player's hand, is being handed to a screen now. */
    public static boolean making() {
        return making;
    }

    /** Presses {@code button} at the screen's point ({@code x}, {@code y}) and lets it go there, as a click does. */
    public static void click(final Screen screen, final double x, final double y, final int button) {
        make(() -> {
            press(screen, x, y, button);
            release(screen, x, y, button);
        });
    }

    /**
     * Presses {@code button} at the screen's point ({@code x}, {@code y}), moves it in steps to ({@code toX},
     * {@code toY}) and lets it go there, as a hand dragging the mouse does.
     */
    public static void drag(final Screen screen, final double x, final double y, final double toX, final double toY,
                            final int button) {
        make(() -> {
            press(screen, x, y, button);
            double lastX = x;
            double lastY = y;
            for (int move = 1; move <= DRAG_MOVES; move++) {
                final double atX = x + (toX - x) * move / DRAG_MOVES;
                final double atY = y + (toY - y) * move / DRAG_MOVES;
                final double dx = atX - lastX;
                final double dy = atY - lastY;
                if (!ClientHooks.onScreenMouseDragPre(screen, atX, atY, button, dx, dy)) {
                    screen.mouseDragged(atX, atY, button, dx, dy);
                    ClientHooks.onScreenMouseDragPost(screen, atX, atY, button, dx, dy);
                }
                lastX = atX;
                lastY = atY;
            }
            release(screen, toX, toY, button);
        });
    }

    /** Turns the wheel {@code notches} at the screen's point ({@code x}, {@code y}): up when more than nought. */
    public static void scroll(final Screen screen, final double x, final double y, final double notches) {
        make(() -> {
            // The game's own hook reads the pointer from the window; this gesture's point is its own.
            if (!NeoForge.EVENT_BUS.post(new ScreenEvent.MouseScrolled.Pre(screen, x, y, 0.0, notches))
                    .isCanceled()) {
                screen.mouseScrolled(x, y, 0.0, notches);
                NeoForge.EVENT_BUS.post(new ScreenEvent.MouseScrolled.Post(screen, x, y, 0.0, notches));
            }
        });
    }

    /** Types {@code character} into the screen, as a key that writes one does. */
    public static void type(final Screen screen, final char character) {
        make(() -> {
            if (!ClientHooks.onScreenCharTypedPre(screen, character, 0)) {
                screen.charTyped(character, 0);
                ClientHooks.onScreenCharTypedPost(screen, character, 0);
            }
        });
    }

    /** Presses the key {@code key} on the screen, as the keyboard does. */
    public static void key(final Screen screen, final int key) {
        make(() -> {
            if (!ClientHooks.onScreenKeyPressedPre(screen, key, 0, 0) && !screen.keyPressed(key, 0, 0)) {
                ClientHooks.onScreenKeyPressedPost(screen, key, 0, 0);
            }
        });
    }

    /** Lets the key {@code key} go on the screen, as the keyboard does after a press. */
    public static void keyUp(final Screen screen, final int key) {
        make(() -> {
            if (!ClientHooks.onScreenKeyReleasedPre(screen, key, 0, 0) && !screen.keyReleased(key, 0, 0)) {
                ClientHooks.onScreenKeyReleasedPost(screen, key, 0, 0);
            }
        });
    }

    /* The press of a click or a drag: the screen hears it unless a listener took it, then the listeners after. */
    private static void press(final Screen screen, final double x, final double y, final int button) {
        if (!ClientHooks.onScreenMouseClickedPre(screen, x, y, button)) {
            ClientHooks.onScreenMouseClickedPost(screen, x, y, button, screen.mouseClicked(x, y, button));
        }
    }

    private static void release(final Screen screen, final double x, final double y, final int button) {
        if (!ClientHooks.onScreenMouseReleasedPre(screen, x, y, button)) {
            ClientHooks.onScreenMouseReleasedPost(screen, x, y, button, screen.mouseReleased(x, y, button));
        }
    }

    /* Makes a gesture with {@link #making} saying so, put back however it ends. */
    private static void make(final Runnable gesture) {
        final boolean was = making;
        making = true;
        try {
            gesture.run();
        } finally {
            making = was;
        }
    }
}
