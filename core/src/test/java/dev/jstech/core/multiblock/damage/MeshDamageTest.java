/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock.damage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class MeshDamageTest {

    /* The Core's impact and collision, as their stamps are declared. */
    private static final DamageStamp IMPACT = new DamageStamp(0.55, 0.35, DamageStamp.Push.PETALS_IN, 0.55, true,
            DamageStamp.Edge.TORN);
    private static final DamageStamp COLLISION = new DamageStamp(0, 0, DamageStamp.Push.FOLDS, 0.6, false,
            DamageStamp.Edge.SCUFFED);
    /* The least an impact's rim can be, a share of its size: its hole, less the roughest its waves can make it. */
    private static final double SMALLEST_RIM = 0.3;
    private static final double CLOSE = 1.0e-6;

    @Test
    void apply_cutsAHoleWithinTheRim() {
        final MeshDamage.Damaged damaged = MeshDamage.apply(plate(0), 3, null, List.of(impact()));

        for (int t = 0; t < damaged.triangles(); t++) {
            final double[] middle = middle(damaged.corners(), 3, t);
            assertTrue(Math.hypot(middle[0], middle[1]) > SMALLEST_RIM * 0.8, "no face is left in the hole");
        }
    }

    @Test
    void apply_reachesAWideFaceStruckInItsMiddle() {
        // One face four blocks across, two triangles, none of their corners anywhere near the strike.
        final float[] wide = {-2, -2, 0, 2, -2, 0, 2, 2, 0, -2, -2, 0, 2, 2, 0, -2, 2, 0};

        final MeshDamage.Damaged damaged = MeshDamage.apply(wide, 3, null, List.of(impact()));

        assertTrue(damaged.triangles() > 2, "the face is cut finer round the strike");
        for (int t = 0; t < damaged.triangles(); t++) {
            final double[] middle = middle(damaged.corners(), 3, t);
            assertTrue(Math.hypot(middle[0], middle[1]) > SMALLEST_RIM * 0.8, "and holed in its middle");
        }
    }

    @Test
    void apply_losesEveryFaceOfALostBlockAndTearsTheHoleRoundIt() {
        // A wall two sixteenths thick, its outer face at 0.5; the block from -0.5 to 0.5 every way is lost.
        final float[] wall = join(plate(0.5F), plate(0.375F));
        final Strike lost = new Strike(IMPACT, 0, 0, 0, 0, 0, 1, 0.8, 5, true);

        final MeshDamage.Damaged damaged = MeshDamage.apply(wall, 3, null, List.of(lost));

        boolean bent = false;
        for (int t = 0; t < damaged.triangles(); t++) {
            final double[] middle = middle(damaged.rest(), 3, t);
            assertTrue(Math.abs(middle[0]) > 0.5 || Math.abs(middle[1]) > 0.5, "no face of the lost block is left");
            bent |= damaged.corners()[t * 9 + 2] < 0.375 - 0.05;
        }
        assertTrue(bent, "and the edges round where it was are bent in");
    }

    @Test
    void reaches_aFaceAcrossTheStrikeThoughItsCornersAreFar() {
        final float[] wide = {-5, -5, 0, 5, -5, 0, 0, 5, 0};
        final float[] far = {-5, -5, 3, 5, -5, 3, 0, 5, 3};

        assertTrue(MeshDamage.reaches(impact(), wide, 3, 0));
        assertFalse(MeshDamage.reaches(impact(), far, 3, 0));
    }

    @Test
    void apply_pushesThePetalsIn() {
        final MeshDamage.Damaged damaged = MeshDamage.apply(plate(0), 3, null, List.of(impact()));

        boolean pushed = false;
        for (int c = 0; c < damaged.corners().length / 3; c++) {
            final double dist = Math.hypot(damaged.rest()[c * 3], damaged.rest()[c * 3 + 1]);
            if (dist > 0.7 && dist < 0.9) {
                assertTrue(damaged.corners()[c * 3 + 2] < 0, "a petal is bent in, against the way it faced");
                pushed = true;
            }
        }
        assertTrue(pushed);
    }

    @Test
    void apply_leavesFacesPastItsReachWhereTheyWere() {
        final MeshDamage.Damaged damaged = MeshDamage.apply(plate(0), 3, null, List.of(impact()));

        for (int c = 0; c < damaged.corners().length / 3; c++) {
            final double dist = Math.hypot(damaged.rest()[c * 3], damaged.rest()[c * 3 + 1]);
            if (dist > Strike.REACH) {
                for (int axis = 0; axis < 3; axis++) {
                    assertEquals(damaged.rest()[c * 3 + axis], damaged.corners()[c * 3 + axis], CLOSE);
                }
            }
        }
    }

    @Test
    void apply_isTheSameEveryTimeForTheSameStrike() {
        final MeshDamage.Damaged once = MeshDamage.apply(plate(0), 3, null, List.of(impact()));
        final MeshDamage.Damaged again = MeshDamage.apply(plate(0), 3, null, List.of(impact()));

        assertArrayEquals(once.corners(), again.corners());
    }

    @Test
    void apply_strikesTheInnerSkinButNotTheWallBehind() {
        // The hull's outer skin, its inner skin a little under it, and a wall well behind them both.
        final int each = plate(0).length / 9;
        final float[] all = join(join(plate(0), plate(-0.1F)), plate(-0.6F));
        final int[] tags = tags(each, 0, each, 1, each, 2);

        final MeshDamage.Damaged damaged = MeshDamage.apply(all, 3, tags, List.of(impact()));

        boolean wallInTheMiddle = false;
        for (int t = 0; t < damaged.triangles(); t++) {
            final double[] middle = middle(damaged.corners(), 3, t);
            final double dist = Math.hypot(middle[0], middle[1]);
            if (damaged.tags()[t] == 1) {
                assertTrue(dist > SMALLEST_RIM * 0.8, "the inner skin is holed with the outer");
            }
            if (damaged.tags()[t] == 2 && dist < 0.2) {
                wallInTheMiddle = true;
                assertEquals(-0.6, middle[2], CLOSE, "the wall behind is never moved");
            }
        }
        assertTrue(wallInTheMiddle, "the wall behind keeps its faces");
    }

    @Test
    void apply_cutsNothingInACollision() {
        final float[] plate = plate(0);
        final Strike collision = new Strike(COLLISION, 0, 0, 0, 0, 0, 1, 1, 3);

        final MeshDamage.Damaged damaged = MeshDamage.apply(plate, 3, null, List.of(collision));

        assertTrue(damaged.triangles() >= plate.length / 9, "a dent cuts no face away");
        double deepest = 0;
        for (int c = 0; c < damaged.corners().length / 3; c++) {
            deepest = Math.min(deepest, damaged.corners()[c * 3 + 2]);
        }
        assertTrue(deepest < -0.4, "the dent goes in, more than " + deepest);
    }

    @Test
    void apply_keepsEachTrianglesTag() {
        final float[] left = plate(0, -2, 0);
        final float[] right = plate(0, 0, 2);
        final int[] tags = tags(left.length / 9, 4, right.length / 9, 7);

        final MeshDamage.Damaged damaged = MeshDamage.apply(join(left, right), 3, tags, List.of(impact()));

        for (int t = 0; t < damaged.triangles(); t++) {
            final double x = damaged.rest()[t * 9] + damaged.rest()[t * 9 + 3] + damaged.rest()[t * 9 + 6];
            assertEquals(x < 0 ? 4 : 7, damaged.tags()[t]);
        }
    }

    @Test
    void apply_tellsWhichStrikesATriangleLiesUnder() {
        final MeshDamage.Damaged damaged = MeshDamage.apply(plate(0), 3, null, List.of(impact()));

        for (int t = 0; t < damaged.triangles(); t++) {
            final double[] middle = middle(damaged.rest(), 3, t);
            final double dist = Math.hypot(middle[0], middle[1]);
            if (dist < 1) {
                assertEquals(1L, damaged.struck()[t]);
            }
        }
    }

    @Test
    void apply_sharesTheRestOfACornersNumbersOutAsItCuts() {
        // Each corner's fourth number is its x, so a corner cut out between two keeps it.
        final float[] plate = plate(0);
        final float[] withX = new float[plate.length / 3 * 4];
        for (int c = 0; c < plate.length / 3; c++) {
            System.arraycopy(plate, c * 3, withX, c * 4, 3);
            withX[c * 4 + 3] = plate[c * 3];
        }

        final MeshDamage.Damaged damaged = MeshDamage.apply(withX, 4, null, List.of(impact()));

        for (int c = 0; c < damaged.corners().length / 4; c++) {
            assertEquals(damaged.rest()[c * 3], damaged.corners()[c * 4 + 3], 1.0e-5);
        }
    }

    @Test
    void apply_refusesCornersThatDoNotComeThreeToATriangle() {
        assertThrows(IllegalArgumentException.class, () -> MeshDamage.apply(new float[] {0, 0, 0, 1, 1, 1}, 3, null,
                List.of(impact())));
    }

    @Test
    void strike_refusesOneOfNoWay() {
        assertThrows(IllegalArgumentException.class, () -> new Strike(IMPACT, 0, 0, 0, 0, 0, 0, 1, 1));
    }

    @Test
    void stamp_refusesAHoleWiderThanItsSize() {
        assertThrows(IllegalArgumentException.class, () -> new DamageStamp(1.5, 0, DamageStamp.Push.DENT, 0.1, false,
                DamageStamp.Edge.TORN));
    }

    private static Strike impact() {
        return new Strike(IMPACT, 0, 0, 0, 0, 0, 1, 1, 11);
    }

    /* A plate four blocks square at {@code z}, facing out along z, of two triangles to each quarter block. */
    private static float[] plate(final float z) {
        return plate(z, -2, 2);
    }

    /* A plate from {@code fromX} to {@code toX} and two blocks either way along y, at {@code z}, facing out along z. */
    private static float[] plate(final float z, final float fromX, final float toX) {
        final float step = 0.25F;
        final int across = Math.round((toX - fromX) / step);
        final int up = Math.round(4 / step);
        final float[] out = new float[across * up * 18];
        int at = 0;
        for (int i = 0; i < across; i++) {
            for (int j = 0; j < up; j++) {
                final float x0 = fromX + i * step;
                final float y0 = -2 + j * step;
                final float x1 = x0 + step;
                final float y1 = y0 + step;
                final float[] two = {x0, y0, z, x1, y0, z, x1, y1, z, x0, y0, z, x1, y1, z, x0, y1, z};
                System.arraycopy(two, 0, out, at, two.length);
                at += two.length;
            }
        }
        return out;
    }

    private static float[] join(final float[] first, final float[] second) {
        final float[] out = new float[first.length + second.length];
        System.arraycopy(first, 0, out, 0, first.length);
        System.arraycopy(second, 0, out, first.length, second.length);
        return out;
    }

    /* Tags given as pairs: how many triangles, then their tag. */
    private static int[] tags(final int... counts) {
        int total = 0;
        for (int i = 0; i < counts.length; i += 2) {
            total += counts[i];
        }
        final int[] out = new int[total];
        int at = 0;
        for (int i = 0; i < counts.length; i += 2) {
            for (int n = 0; n < counts[i]; n++) {
                out[at++] = counts[i + 1];
            }
        }
        return out;
    }

    private static double[] middle(final float[] corners, final int stride, final int t) {
        final double[] out = new double[3];
        for (int c = 0; c < 3; c++) {
            for (int axis = 0; axis < 3; axis++) {
                out[axis] += corners[(t * 3 + c) * stride + axis] / 3.0;
            }
        }
        return out;
    }
}
