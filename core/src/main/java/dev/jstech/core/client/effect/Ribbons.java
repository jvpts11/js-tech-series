/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.effect;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.world.phys.Vec3;

/**
 * A ribbon of colour along a line of points, turned to face the eye wherever it bends: a trail behind a particle, a
 * beam, an arc. A trail fades from nothing at its tail to its colour at its head.
 */
final class Ribbons {

    private static final int CHANNEL = 255;
    private static final int ALPHA_SHIFT = 24;
    private static final double LEAST = 1e-9;

    private Ribbons() {
    }

    /**
     * Lays the ribbon along {@code points} (x y z, the world's), {@code width} blocks wide, in {@code argb};
     * {@code fades} from nothing at its first point to its colour at its last.
     */
    static void strip(final VertexConsumer consumer, final Vec3 eye, final List<double[]> points, final float width,
                      final int argb, final boolean fades) {
        if (points.size() < 2) {
            return;
        }
        final int r = argb >> 16 & CHANNEL;
        final int g = argb >> 8 & CHANNEL;
        final int b = argb & CHANNEL;
        final int a = argb >>> ALPHA_SHIFT;
        final int last = points.size() - 1;
        for (int i = 0; i < last; i++) {
            final double[] from = points.get(i);
            final double[] to = points.get(i + 1);
            final double[] side = side(from, to, eye, width / 2);
            final int alphaFrom = fades ? a * i / last : a;
            final int alphaTo = fades ? a * (i + 1) / last : a;
            vertex(consumer, eye, from, side, -1, r, g, b, alphaFrom);
            vertex(consumer, eye, from, side, 1, r, g, b, alphaFrom);
            vertex(consumer, eye, to, side, 1, r, g, b, alphaTo);
            vertex(consumer, eye, to, side, -1, r, g, b, alphaTo);
        }
    }

    /* Half the ribbon's width across the piece from {@code from} to {@code to}, square to it and to the eye. */
    private static double[] side(final double[] from, final double[] to, final Vec3 eye, final double half) {
        final double ax = to[0] - from[0];
        final double ay = to[1] - from[1];
        final double az = to[2] - from[2];
        final double ex = eye.x - from[0];
        final double ey = eye.y - from[1];
        final double ez = eye.z - from[2];
        final double sx = ay * ez - az * ey;
        final double sy = az * ex - ax * ez;
        final double sz = ax * ey - ay * ex;
        final double length = Math.max(LEAST, Math.sqrt(sx * sx + sy * sy + sz * sz));
        return new double[] {sx / length * half, sy / length * half, sz / length * half};
    }

    private static void vertex(final VertexConsumer consumer, final Vec3 eye, final double[] at, final double[] side,
                               final int sign, final int r, final int g, final int b, final int a) {
        consumer.addVertex((float) (at[0] + side[0] * sign - eye.x), (float) (at[1] + side[1] * sign - eye.y),
                (float) (at[2] + side[2] * sign - eye.z)).setColor(r, g, b, a);
    }
}
