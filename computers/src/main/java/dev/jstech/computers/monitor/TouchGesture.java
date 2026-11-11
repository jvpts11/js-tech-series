/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.monitor;

import java.util.List;

/**
 * One finger on a touch monitor's glass, from the moment the use key goes down to the moment it comes up, read as the
 * mouse a screen understands (computers/docs/design/peripherals.md): let go where it went down, a tap, the left button
 * pressed and let go there; moved while down, a drag, the left button held from where it went down to where it is let
 * go; held still for a second, the right button, pressed and let go where it is held.
 *
 * <p>A tap is known only when the finger lifts, since until then it could still become a drag or a hold; so its press
 * and its release are handed over together. Points are in the picture's pixels; nothing here knows the game.
 */
public final class TouchGesture {

    private Phase phase = Phase.UP;
    private double startX;
    private double startY;
    private double lastX;
    private double lastY;
    private int heldTicks;

    /** The left button, which taps and drags are. */
    public static final int LEFT = 0;
    /** The right button, which a touch held still is. */
    public static final int RIGHT = 1;
    /** How long a touch held still takes to be the right button: a second of game ticks. */
    public static final int HOLD_TICKS = 20;
    /** How far a finger may wander and still be holding still, in the picture's pixels. */
    public static final double SLOP = 3.0;

    /** Whether a finger is on the glass. */
    public boolean down() {
        return phase != Phase.UP;
    }

    /** The finger goes down at a point; nothing happens yet. */
    public List<Event> press(final double x, final double y) {
        phase = Phase.PRESSED;
        startX = x;
        startY = y;
        lastX = x;
        lastY = y;
        heldTicks = 0;
        return List.of();
    }

    /** A tick with the finger still down, at a point: a drag once it wanders, the right button once it has held. */
    public List<Event> held(final double x, final double y) {
        switch (phase) {
            case PRESSED -> {
                heldTicks++;
                if (Math.hypot(x - startX, y - startY) > SLOP) {
                    phase = Phase.DRAGGING;
                    lastX = x;
                    lastY = y;
                    return List.of(new Event(Kind.PRESS, startX, startY, LEFT), new Event(Kind.DRAG, x, y, LEFT));
                }
                if (heldTicks >= HOLD_TICKS) {
                    phase = Phase.HELD;
                    return List.of(new Event(Kind.PRESS, startX, startY, RIGHT),
                            new Event(Kind.RELEASE, startX, startY, RIGHT));
                }
                return List.of();
            }
            case DRAGGING -> {
                if (x == lastX && y == lastY) {
                    return List.of();
                }
                lastX = x;
                lastY = y;
                return List.of(new Event(Kind.DRAG, x, y, LEFT));
            }
            default -> {
                return List.of();
            }
        }
    }

    /** The finger lifts: a tap where it went down, the end of a drag where it is, or nothing after a hold. */
    public List<Event> release() {
        final Phase was = phase;
        phase = Phase.UP;
        return switch (was) {
            case PRESSED -> List.of(new Event(Kind.PRESS, startX, startY, LEFT),
                    new Event(Kind.RELEASE, startX, startY, LEFT));
            case DRAGGING -> List.of(new Event(Kind.RELEASE, lastX, lastY, LEFT));
            default -> List.of();
        };
    }

    /** Lets go of the finger without telling anything, for a glass that went away under it. */
    public void cancel() {
        phase = Phase.UP;
    }

    /** What the mouse does. */
    public enum Kind {
        PRESS,
        DRAG,
        RELEASE
    }

    /**
     * One thing the mouse does on the glass.
     *
     * @param kind   pressed, moved with the button held, or let go
     * @param x      where across the picture, in its pixels
     * @param y      where down it
     * @param button {@link #LEFT} or {@link #RIGHT}
     */
    public record Event(Kind kind, double x, double y, int button) {
    }

    private enum Phase {
        UP,
        PRESSED,
        DRAGGING,
        HELD
    }
}
