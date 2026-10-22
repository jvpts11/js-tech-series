/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.motion;

import dev.jstech.core.motion.Motion;
import dev.jstech.core.motion.MotionStyles;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Draws a thing as its motion has it this frame: grown or shrunk about its point, slid in from its side or out to it,
 * and faded in or out as one picture, the three together where the motion says so. A motion that is over draws the
 * thing where it ends, at no cost.
 */
public final class MotionPainter {

    private MotionPainter() {
    }

    /**
     * Draws what {@code paint} draws as {@code motion} has it at {@code now}, the thing standing at {@code x},
     * {@code y}, {@code width} by {@code height} where it rests.
     */
    public static void draw(final GuiGraphics g, final Motion motion, final double now, final int x, final int y,
                            final int width, final int height, final Runnable paint) {
        if (motion.done(now) || width <= 0 || height <= 0) {
            paint.run();
            return;
        }
        g.pose().pushPose();
        if (motion.is(MotionStyles.SCALE)) {
            final float px = (float) (x + width * motion.spec().param("pivot_x", 0.5));
            final float py = (float) (y + height * motion.spec().param("pivot_y", 0.5));
            g.pose().translate(px, py, 0);
            g.pose().scale((float) motion.scale(now), (float) motion.scaleY(now), 1);
            g.pose().translate(-px, -py, 0);
        } else if (motion.is(MotionStyles.SLIDE)) {
            g.pose().translate(0, (float) (motion.offset(now) * height * motion.spec().param("from_y", 1.0)), 0);
        }
        if (motion.fades()) {
            FadeLayer.draw(g, (float) Math.clamp(motion.opacity(now), 0.0, 1.0), paint);
        } else {
            paint.run();
        }
        g.pose().popPose();
    }
}
