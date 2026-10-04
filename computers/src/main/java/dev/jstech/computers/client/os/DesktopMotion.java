/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.os.DesktopEffects;
import dev.jstech.computers.os.OsMotions;
import dev.jstech.core.client.motion.MotionClock;
import dev.jstech.core.motion.Motion;
import dev.jstech.core.motion.MotionProfile;
import dev.jstech.core.motion.MotionSpec;
import dev.jstech.core.motion.MotionStyles;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import net.minecraft.client.gui.GuiGraphics;

/**
 * How this desktop moves: its system's motion profile, as the resource packs left it, less whatever the owner
 * switched off on the system's own settings page and at the pace they set there; and the drawing of a window under
 * way, grown, shrunk or carried to its button.
 *
 * <p>Nothing moves on a monitor's face in the world, nor for a player who reduced motion: each motion then starts
 * already over, so what it draws is where it ends.
 */
@PaletteHolder
final class DesktopMotion {

    private final DesktopState desktop;

    /**
     * The colours of motion itself, {@code jsc:desktop/motion}: the outline a window manager of the oldest kind
     * carried to a window's button, light so it reads over any wallpaper.
     */
    private static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "desktop/motion",
            new Colours(0xFFE8E8E8));

    DesktopMotion(final DesktopState desktop) {
        this.desktop = desktop;
    }

    /** The profile this desktop moves by: its system's, as the packs left it. */
    MotionProfile profile() {
        return OsMotions.of(desktop.panelStyle(), desktop.periodPanel()).get();
    }

    /**
     * How that kind of thing moves here: nothing when any of the boxes over it is switched off, and otherwise at the
     * pace the owner set, unless the motion keeps its own whatever the pace.
     */
    MotionSpec spec(final String kind) {
        final MotionSpec spec = profile().spec(kind);
        final DesktopEffects effects = desktop.prefs().effects();
        for (final String group : spec.groups()) {
            if (effects.isOff(group)) {
                return MotionSpec.NONE;
            }
        }
        return spec.param(OsMotions.STEADY, 0.0) > 0.0 ? spec : spec.slowed(effects.speed());
    }

    /** A motion of that kind starting now; one already over where nothing moves. */
    Motion start(final String kind) {
        return desktop.surface().moves() ? MotionClock.start(spec(kind)) : Motion.FINISHED;
    }

    /** Whether one of the system's own effects is on, which is every effect its owner did not switch off. */
    boolean on(final String effect) {
        return !desktop.prefs().effects().isOff(effect);
    }

    /** The time of this frame, on the clock every motion is read against. */
    static double now() {
        return MotionClock.now();
    }

    /**
     * Moves the pose to where a thing standing at ({@code x}, {@code y}), {@code w} by {@code h}, is drawn this frame
     * by its motion: grown or shrunk about its point, or on its way to the place that stands for it, the rectangle
     * {@code tx}, {@code ty}, {@code tw} by {@code th}. A motion that is over, or of a style that does not move the
     * thing itself, leaves the pose as it is.
     */
    static void pose(final GuiGraphics g, final Motion motion, final double now, final int x, final int y,
                     final int w, final int h, final int tx, final int ty, final int tw, final int th) {
        if (motion.done(now) || w <= 0 || h <= 0) {
            return;
        }
        if (motion.is(MotionStyles.SCALE)) {
            final float px = (float) (x + w * motion.spec().param("pivot_x", 0.5));
            final float py = (float) (y + h * motion.spec().param("pivot_y", 0.5));
            g.pose().translate(px, py, 0);
            g.pose().scale((float) motion.scale(now), (float) motion.scaleY(now), 1);
            g.pose().translate(-px, -py, 0);
        } else if (motion.is(MotionStyles.ZOOM)) {
            final double t = motion.toward(now);
            final double cx = x + (tx - x) * t;
            final double cy = y + (ty - y) * t;
            final double cw = w + (tw - w) * t;
            final double ch = h + (th - h) * t;
            g.pose().translate((float) cx, (float) cy, 0);
            g.pose().scale((float) (cw / w), (float) (ch / h), 1);
            g.pose().translate(-x, -y, 0);
        }
    }

    /**
     * Draws the outline an {@link MotionStyles#OUTLINE} motion carries between a thing and the place that stands for
     * it, with the outlines trailing behind it.
     */
    static void outline(final GuiGraphics g, final Motion motion, final double now, final int x, final int y,
                        final int w, final int h, final int tx, final int ty, final int tw, final int th) {
        final int colour = PALETTE.get().outline();
        final int trail = (int) Math.max(0, motion.spec().param("trail", 0.0));
        final double lead = motion.toward(now);
        for (int i = 0; i <= trail; i++) {
            // Each trailing outline sits a little further back along the way the leading one came.
            final double t = Math.max(0.0, Math.min(1.0, lead + (motion.outward() ? -0.08 : 0.08) * i));
            final int cx = (int) Math.round(x + (tx - x) * t);
            final int cy = (int) Math.round(y + (ty - y) * t);
            final int cw = Math.max(2, (int) Math.round(w + (tw - w) * t));
            final int ch = Math.max(2, (int) Math.round(h + (th - h) * t));
            g.fill(cx, cy, cx + cw, cy + 1, colour);
            g.fill(cx, cy + ch - 1, cx + cw, cy + ch, colour);
            g.fill(cx, cy, cx + 1, cy + ch, colour);
            g.fill(cx + cw - 1, cy, cx + cw, cy + ch, colour);
        }
    }

    /** The colours of motion, as the palette above names them. */
    private record Colours(int outline) {
    }
}
