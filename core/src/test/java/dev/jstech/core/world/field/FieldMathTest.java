/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.world.field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FieldMathTest {

    private static final float CLOSE = 1e-4F;

    @Test
    void pack_roundTripsNegativeAndPositiveChunks() {
        for (final int[] chunk : new int[][] {{0, 0}, {-1, 1}, {123_456, -654_321}, {Integer.MIN_VALUE, -1}}) {
            final long packed = FieldMath.pack(chunk[0], chunk[1]);

            assertEquals(chunk[0], FieldMath.x(packed));
            assertEquals(chunk[1], FieldMath.z(packed));
        }
    }

    @Test
    void step_handsTheSpreadEvenlyToTheFourNeighbours() {
        final Map<Long, Float> next = step(new long[] {FieldMath.pack(0, 0)}, new float[] {100F}, 0.2, 0);

        assertEquals(80F, next.get(FieldMath.pack(0, 0)), CLOSE);
        assertEquals(5F, next.get(FieldMath.pack(1, 0)), CLOSE);
        assertEquals(5F, next.get(FieldMath.pack(-1, 0)), CLOSE);
        assertEquals(5F, next.get(FieldMath.pack(0, 1)), CLOSE);
        assertEquals(5F, next.get(FieldMath.pack(0, -1)), CLOSE);
        assertFalse(next.containsKey(FieldMath.pack(1, 1)));
    }

    @Test
    void step_keepsTheTotalWithoutDecayAndFadesItWithDecay() {
        final long[] chunks = {FieldMath.pack(3, -2), FieldMath.pack(4, -2)};
        final float[] values = {40F, 60F};

        assertEquals(100, total(step(chunks, values, 0.5, 0)), CLOSE);
        assertEquals(90, total(step(chunks, values, 0.5, 0.1)), CLOSE);
    }

    @Test
    void step_addsNeighbouringSharesTogetherAndKeepsWithoutSpread() {
        final Map<Long, Float> spread = step(new long[] {FieldMath.pack(0, 0), FieldMath.pack(2, 0)},
                new float[] {40F, 40F}, 0.4, 0);
        final Map<Long, Float> still = step(new long[] {FieldMath.pack(7, 7)}, new float[] {10F}, 0, 0.5);

        assertEquals(8F, spread.get(FieldMath.pack(1, 0)), CLOSE);
        assertEquals(1, still.size());
        assertEquals(5F, still.get(FieldMath.pack(7, 7)), CLOSE);
        assertTrue(step(new long[0], new float[0], 0.4, 0.1).isEmpty());
    }

    private static Map<Long, Float> step(final long[] chunks, final float[] values, final double spread,
                                         final double decay) {
        final Map<Long, Float> next = new HashMap<>();
        FieldMath.step(chunks, values, spread, decay, (chunk, amount) -> next.merge(chunk, amount, Float::sum));
        return next;
    }

    private static double total(final Map<Long, Float> values) {
        double sum = 0;
        for (final float value : values.values()) {
            sum += value;
        }
        return sum;
    }
}
