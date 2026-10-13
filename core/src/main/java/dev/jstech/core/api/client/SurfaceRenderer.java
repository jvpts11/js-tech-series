/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.api.client;

import org.jetbrains.annotations.ApiStatus;

/**
 * What draws frames onto a surface: a game, a viewer, anything that paints its own picture inside a window.
 *
 * <p>A renderer says how big a surface it wants and of which kind, and is then asked for frames: at most
 * {@link #frameCap()} a second, and only while its window is on a screen someone is looking at, so a renderer nobody
 * sees costs nothing. Between frames it may keep any state it likes; it lives on the client of the player who opened
 * it and on no other. Whoever else looks at the same window, another player at the machine or the monitor's face in
 * the world, sees a placeholder instead, unless the renderer says what it draws is safe to show anywhere.
 *
 * <p>A renderer that throws is not asked again: its window shows the placeholder from then on and the game writes one
 * line about it in the log, so a fault in one mod's drawing never takes the game down.
 *
 * <p>A renderer of 64 by 40 pixels that paints a moving gradient, as a desktop program of J's Computers hands it out
 * from its {@code renderer()}:
 *
 * <pre>{@code
 * private final SurfaceRenderer renderer = new SurfaceRenderer(64, 40, false) {
 *     private int frame;
 *
 *     @Override
 *     public void frame(final ISurface surface, final double seconds) {
 *         final PixelSurface pixels = (PixelSurface) surface;
 *         for (int y = 0; y < pixels.height(); y++) {
 *             for (int x = 0; x < pixels.width(); x++) {
 *                 pixels.set(x, y, 0xFF000000 | ((x + this.frame) * 4 & 0xFF) << 16 | y * 6 << 8);
 *             }
 *         }
 *         this.frame++;
 *     }
 * };
 * }</pre>
 */
@ApiStatus.Experimental
public abstract class SurfaceRenderer {

    /** The most pixels a surface may be wide or tall; a bigger request is a mistake in the renderer (1024 each way). */
    static final int MAX_SIZE = 1024;

    private final int width;
    private final int height;
    private final boolean gpu;

    /**
     * A renderer of a surface of that size, from 1 to 1024 pixels each way; any other size is refused with an
     * {@link IllegalArgumentException}, so a mistake shows when the renderer is built.
     *
     * @param gpu whether it draws on the graphics card ({@link GpuSurface}) rather than into an array of pixels
     *            ({@link PixelSurface})
     */
    protected SurfaceRenderer(final int width, final int height, final boolean gpu) {
        if (width < 1 || height < 1 || width > MAX_SIZE || height > MAX_SIZE) {
            throw new IllegalArgumentException("Surface size must be 1.." + MAX_SIZE + " each way, got "
                + width + "x" + height);
        }
        this.width = width;
        this.height = height;
        this.gpu = gpu;
    }

    /** How wide a surface it draws on. */
    public final int width() {
        return this.width;
    }

    /** How tall a surface it draws on. */
    public final int height() {
        return this.height;
    }

    /** Whether it draws on the graphics card. */
    public final boolean gpu() {
        return this.gpu;
    }

    /** The most frames it is asked for in a second; thirty unless it says otherwise. */
    public int frameCap() {
        return 30;
    }

    /**
     * Whether what it draws may be shown to anyone looking at its window, other players and the monitor in the world
     * among them. Only a renderer whose picture follows from what every client already knows can say yes: one that
     * simulates on its opener's client would show each viewer something different.
     */
    public boolean worldSafe() {
        return false;
    }

    /**
     * Draws a frame.
     *
     * @param surface the surface to draw on: a {@link PixelSurface} or a {@link GpuSurface}, as asked
     * @param seconds how long since the frame before, zero for the first
     */
    public abstract void frame(ISurface surface, double seconds);

    /** A click on the surface, in the surface's own pixels; true when it was taken. */
    public boolean mouseClicked(final double x, final double y, final int button) {
        return false;
    }

    /** A button let go over the surface, in its own pixels. */
    public boolean mouseReleased(final double x, final double y, final int button) {
        return false;
    }

    /** The wheel turned over the surface; above zero is up. */
    public boolean mouseScrolled(final double delta) {
        return false;
    }

    /** A key pressed while its window has the keyboard, with the game's key code. */
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        return false;
    }

    /** A key let go while its window has the keyboard. */
    public boolean keyReleased(final int key, final int scanCode, final int modifiers) {
        return false;
    }

    /** A character typed while its window has the keyboard. */
    public boolean charTyped(final char typed) {
        return false;
    }

    /** Its window has closed; it is never asked for another frame. */
    public void closed() {
    }
}
