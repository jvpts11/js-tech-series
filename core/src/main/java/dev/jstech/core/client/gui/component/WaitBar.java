/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.component;

import dev.jstech.core.api.client.ISkin;
import dev.jstech.core.client.motion.MotionClock;
import dev.jstech.core.motion.MotionKinds;
import dev.jstech.core.motion.MotionScope;
import dev.jstech.core.motion.MotionSpec;
import dev.jstech.core.motion.MotionStyles;
import dev.jstech.core.motion.Rhythm;
import net.minecraft.client.gui.GuiGraphics;

/**
 * A bar for a wait with no known end: looking for printers, getting through to a machine, waiting on the network.
 * It moves the way the system it is drawn on moves such a bar ({@link MotionKinds#PROGRESS_WAIT}): Frames XP's three
 * blocks crossing it, a segment sliding across, one that grows and shrinks on its way, or a block going from end to
 * end. A system that had no such bar shows the empty trough, and with motion reduced the bar stands in its first
 * place.
 */
public final class WaitBar extends UiComponent {

    /* What the bar runs by where the system says nothing of its own: a segment a third of it long. */
    private static final double SEGMENT_LENGTH = 0.3;
    private static final double BOUNCE_LENGTH = 0.25;
    private static final int BLOCKS = 3;
    private static final int BLOCK_GAP = 2;

    @Override
    public void render(final GuiGraphics g, final UiContext ctx) {
        paint(g, ctx.skin(), x(), y(), width(), height());
    }

    /** Draws the bar in that rectangle, in that look, moving as the system being drawn moves its bars. */
    public static void paint(final GuiGraphics g, final ISkin skin, final int x, final int y, final int w,
                             final int h) {
        skin.panel(g, x, y, w, h);
        final int left = x + 1;
        final int right = x + w - 1;
        final int top = y + 1;
        final int bottom = y + h - 1;
        final int inner = right - left;
        if (inner <= 0 || bottom <= top) {
            return;
        }
        final MotionSpec spec = MotionScope.spec(MotionKinds.PROGRESS_WAIT);
        if (MotionStyles.NONE.equals(spec.style())) {
            return;
        }
        final double elapsed = MotionClock.loopMs();
        final int fill = skin.progressFill();
        switch (spec.style()) {
            case MotionStyles.BLOCKS -> {
                final int count = Math.max(1, (int) spec.param("blocks", BLOCKS));
                final int block = Math.max(2, bottom - top);
                final int group = count * block + (count - 1) * BLOCK_GAP;
                final int start = left - group + (int) Math.round(Rhythm.pass(spec, elapsed) * (inner + group));
                for (int i = 0; i < count; i++) {
                    final int bx = start + i * (block + BLOCK_GAP);
                    span(g, bx, bx + block, left, right, top, bottom, fill);
                }
            }
            case MotionStyles.GROW -> {
                final double along = Rhythm.pass(spec, elapsed);
                final int length = (int) Math.round(inner * (0.1 + 0.4 * Math.sin(Math.PI * along)));
                final int start = left - length + (int) Math.round(along * (inner + length));
                span(g, start, start + length, left, right, top, bottom, fill);
            }
            case MotionStyles.BOUNCE -> {
                final int length = Math.max(2, (int) Math.round(inner * spec.param("length", BOUNCE_LENGTH)));
                final int start = left + (int) Math.round(Rhythm.bounce(spec, elapsed) * (inner - length));
                span(g, start, start + length, left, right, top, bottom, fill);
            }
            default -> {
                // A segment crossing the bar, which is also what a style this bar does not know draws.
                final int length = Math.max(2, (int) Math.round(inner * spec.param("length", SEGMENT_LENGTH)));
                final int start = left - length + (int) Math.round(Rhythm.pass(spec, elapsed) * (inner + length));
                span(g, start, start + length, left, right, top, bottom, fill);
            }
        }
    }

    /* The part of the run from {@code from} to {@code to} that lies inside the trough. */
    private static void span(final GuiGraphics g, final int from, final int to, final int left, final int right,
                             final int top, final int bottom, final int colour) {
        final int a = Math.max(left, from);
        final int b = Math.min(right, to);
        if (b > a) {
            g.fill(a, top, b, bottom, colour);
        }
    }
}
