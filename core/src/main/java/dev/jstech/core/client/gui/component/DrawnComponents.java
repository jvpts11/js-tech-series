/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.component;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * What the components drew in the last frame and where each stands on the screen, kept only while something reads it:
 * an agent's screen tools read a screen drawn of components, the programs of a desktop among them, which hold no
 * widget of the game's for them to find. Where a component stands is where it was drawn, the moves and the scale of
 * what drew it counted in.
 */
public final class DrawnComponents {

    /* What this frame has drawn so far. */
    private static final List<Drawn> FRAME = new ArrayList<>();
    /* Whether components are kept as they draw; off, drawing costs nothing more. */
    private static volatile boolean kept;
    /* What the last whole frame drew. */
    private static List<Drawn> last = List.of();

    private DrawnComponents() {
    }

    /** Keeps what is drawn from now on, or stops and forgets it. */
    public static void keep(final boolean on) {
        kept = on;
        if (!on) {
            FRAME.clear();
            last = List.of();
        }
    }

    /** Ends a frame: what it drew becomes what the last frame drew. */
    public static void frameEnded() {
        if (kept) {
            last = List.copyOf(FRAME);
            FRAME.clear();
        }
    }

    /** What the last whole frame drew, in the order it was drawn: the last over the others. */
    public static List<Drawn> lastFrame() {
        return last;
    }

    /* A component drawn now, kept with the box it covers on the screen. */
    static void drawn(final GuiGraphics g, final UiComponent component) {
        if (!kept) {
            return;
        }
        final Matrix4f moved = g.pose().last().pose();
        final Vector3f from = moved.transformPosition(component.x(), component.y(), 0, new Vector3f());
        final Vector3f to = moved.transformPosition(component.x() + component.width(),
                component.y() + component.height(), 0, new Vector3f());
        FRAME.add(new Drawn(component, Math.round(Math.min(from.x, to.x)), Math.round(Math.min(from.y, to.y)),
                Math.round(Math.abs(to.x - from.x)), Math.round(Math.abs(to.y - from.y))));
    }

    /**
     * A component as it was drawn.
     *
     * @param component what was drawn
     * @param x         its box's left on the screen, in the interface's pixels
     * @param y         its box's top
     * @param width     its box's width on the screen
     * @param height    its box's height on the screen
     */
    public record Drawn(UiComponent component, int x, int y, int width, int height) {
    }
}
