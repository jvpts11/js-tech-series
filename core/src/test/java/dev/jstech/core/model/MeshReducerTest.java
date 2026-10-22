/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class MeshReducerTest {

    private static final int GRID = 20;
    private static final int RINGS = 16;
    private static final int SEGMENTS = 32;
    private static final float RADIUS = 10;

    @Test
    void reduce_bringsASphereDownAndKeepsItRound() {
        final ModelMesh sphere = sphere();
        final ModelMesh reduced = MeshReducer.reduce(sphere, 200);
        assertTrue(MeshReducer.triangles(reduced) <= 200, "at most 200 triangles: " + MeshReducer.triangles(reduced));
        assertTrue(MeshReducer.triangles(reduced) > 100, "and not far fewer: " + MeshReducer.triangles(reduced));
        final float[] points = reduced.positions();
        for (int at = 0; at < points.length; at += 3) {
            final double away = Math.sqrt(points[at] * points[at] + points[at + 1] * points[at + 1]
                    + points[at + 2] * points[at + 2]);
            assertEquals(RADIUS, away, 0.01, "a corner moves onto another, so every corner stays on the sphere");
        }
    }

    @Test
    void reduce_keepsAFlatSheetFlatItsAreaAndItsEdges() {
        final ModelMesh sheet = sheet();
        final ModelMesh reduced = MeshReducer.reduce(sheet, 100);
        assertTrue(MeshReducer.triangles(reduced) <= 100, "at most 100: " + MeshReducer.triangles(reduced));
        assertEquals(area(sheet), area(reduced), 1.0e-3, "no triangle turned over, none left a gap");
        for (int at = 1; at < reduced.positions().length; at += 3) {
            assertEquals(0, reduced.positions()[at], 1.0e-6, "flat as it was");
        }
    }

    @Test
    void reduce_leavesAMeshOfFewEnoughAsItIs() {
        final ModelMesh sheet = sheet();
        assertSame(sheet, MeshReducer.reduce(sheet, 10_000));
    }

    /* A sphere of rings and segments, its poles single corners. */
    private static ModelMesh sphere() {
        final FloatList points = new FloatList();
        final IntList triangles = new IntList();
        for (int ring = 0; ring <= RINGS; ring++) {
            final double up = Math.PI * ring / RINGS;
            for (int segment = 0; segment < SEGMENTS; segment++) {
                final double round = 2 * Math.PI * segment / SEGMENTS;
                points.add((float) (RADIUS * Math.sin(up) * Math.cos(round)));
                points.add((float) (RADIUS * Math.cos(up)));
                points.add((float) (RADIUS * Math.sin(up) * Math.sin(round)));
            }
        }
        for (int ring = 0; ring < RINGS; ring++) {
            for (int segment = 0; segment < SEGMENTS; segment++) {
                final int a = ring * SEGMENTS + segment;
                final int b = ring * SEGMENTS + (segment + 1) % SEGMENTS;
                final int c = a + SEGMENTS;
                final int d = b + SEGMENTS;
                if (ring > 0) {
                    triangles.add(a);
                    triangles.add(b);
                    triangles.add(c);
                }
                if (ring < RINGS - 1) {
                    triangles.add(b);
                    triangles.add(d);
                    triangles.add(c);
                }
            }
        }
        return new ModelMesh(points.toArray(), new float[0], new float[0], triangles.toArray(), new int[0],
                new float[0], List.of(), 0);
    }

    /* A flat sheet of squares, two triangles each, lying on Y = 0. */
    private static ModelMesh sheet() {
        final FloatList points = new FloatList();
        final IntList triangles = new IntList();
        for (int z = 0; z <= GRID; z++) {
            for (int x = 0; x <= GRID; x++) {
                points.add(x);
                points.add(0);
                points.add(z);
            }
        }
        for (int z = 0; z < GRID; z++) {
            for (int x = 0; x < GRID; x++) {
                final int a = z * (GRID + 1) + x;
                triangles.add(a);
                triangles.add(a + GRID + 1);
                triangles.add(a + 1);
                triangles.add(a + 1);
                triangles.add(a + GRID + 1);
                triangles.add(a + GRID + 2);
            }
        }
        return new ModelMesh(points.toArray(), new float[0], new float[0], triangles.toArray(), new int[0],
                new float[0], List.of(), 0);
    }

    /* The area of a flat mesh's triangles, each signed by which way it faces, so one turned over takes away. */
    private static double area(final ModelMesh mesh) {
        final float[] p = mesh.positions();
        final int[] t = mesh.triangles();
        double sum = 0;
        for (int i = 0; i < t.length; i += 3) {
            final int a = t[i] * 3;
            final int b = t[i + 1] * 3;
            final int c = t[i + 2] * 3;
            sum += ((p[b + 2] - p[a + 2]) * (p[c] - p[a]) - (p[b] - p[a]) * (p[c + 2] - p[a + 2])) / 2.0;
        }
        return sum;
    }
}
