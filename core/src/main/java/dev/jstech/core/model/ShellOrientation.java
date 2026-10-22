/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.joml.Vector3d;

/**
 * Which faces of a shell are to be turned over so every one faces the same way, out of a closed solid: two faces
 * that share an edge pass along it opposite ways when both face out, so a face most of whose neighbours pass their
 * shared edges its own way faces in, and is turned. A closed shell all of whose faces then face in, as its volume by
 * the triangles says, is turned over whole; an open one keeps the way most of its faces came. What a file or a reading
 * of it got wrong for one face is set right by the faces round it. Pure.
 */
final class ShellOrientation {

    private ShellOrientation() {
    }

    /**
     * Whether each face is to be turned over.
     *
     * @param faces  each face's triangles, three corners each, counter-clockwise seen from the side it faces
     * @param closed whether the faces close round a solid
     */
    static boolean[] flips(final List<List<Vector3d[]>> faces, final boolean closed) {
        final int count = faces.size();
        final Map<Edge, List<int[]>> uses = new HashMap<>();
        for (int f = 0; f < count; f++) {
            for (final Vector3d[] triangle : faces.get(f)) {
                for (int k = 0; k < 3; k++) {
                    final Vector3d a = triangle[k];
                    final Vector3d b = triangle[(k + 1) % 3];
                    final boolean forward = Edge.forward(a, b);
                    uses.computeIfAbsent(Edge.of(a, b), key -> new ArrayList<>()).add(new int[] {f, forward ? 1 : 0});
                }
            }
        }
        // Between two faces, how many shared edges they pass opposite ways, less those they pass alike.
        final List<Map<Integer, Integer>> agree = new ArrayList<>(count);
        for (int f = 0; f < count; f++) {
            agree.add(new HashMap<>());
        }
        for (final List<int[]> use : uses.values()) {
            if (use.size() != 2 || use.get(0)[0] == use.get(1)[0]) {
                continue;
            }
            final int a = use.get(0)[0];
            final int b = use.get(1)[0];
            final int vote = use.get(0)[1] != use.get(1)[1] ? 1 : -1;
            agree.get(a).merge(b, vote, Integer::sum);
            agree.get(b).merge(a, vote, Integer::sum);
        }
        final boolean[] flip = new boolean[count];
        final boolean[] seen = new boolean[count];
        final List<List<Integer>> parts = new ArrayList<>();
        for (int seed = 0; seed < count; seed++) {
            if (seen[seed]) {
                continue;
            }
            final List<Integer> part = new ArrayList<>();
            final ArrayDeque<Integer> open = new ArrayDeque<>();
            open.add(seed);
            seen[seed] = true;
            while (!open.isEmpty()) {
                final int face = open.poll();
                part.add(face);
                for (final Map.Entry<Integer, Integer> neighbour : agree.get(face).entrySet()) {
                    final int other = neighbour.getKey();
                    if (seen[other] || neighbour.getValue() == 0) {
                        continue;
                    }
                    seen[other] = true;
                    flip[other] = neighbour.getValue() > 0 ? flip[face] : !flip[face];
                    open.add(other);
                }
            }
            parts.add(part);
        }
        for (final List<Integer> part : parts) {
            if (closed ? volume(faces, part, flip) < 0 : turnedMost(part, flip)) {
                for (final int face : part) {
                    flip[face] = !flip[face];
                }
            }
        }
        return flip;
    }

    /* Six times the volume the faces enclose, as they face once turned; below 0 for faces facing in. */
    private static double volume(final List<List<Vector3d[]>> faces, final List<Integer> part, final boolean[] flip) {
        double sum = 0;
        for (final int face : part) {
            for (final Vector3d[] triangle : faces.get(face)) {
                final double six = triangle[0].dot(new Vector3d(triangle[1]).cross(triangle[2]));
                sum += flip[face] ? -six : six;
            }
        }
        return sum;
    }

    /* Whether more of the faces would be turned than kept. */
    private static boolean turnedMost(final List<Integer> part, final boolean[] flip) {
        int turned = 0;
        for (final int face : part) {
            if (flip[face]) {
                turned++;
            }
        }
        return turned * 2 > part.size();
    }

    /* An edge between two places, whichever way it is passed. */
    private record Edge(Vector3d low, Vector3d high) {

        static Edge of(final Vector3d a, final Vector3d b) {
            return forward(a, b) ? new Edge(a, b) : new Edge(b, a);
        }

        /* Whether {@code a} comes before {@code b}, by X, then Y, then Z. */
        static boolean forward(final Vector3d a, final Vector3d b) {
            if (a.x != b.x) {
                return a.x < b.x;
            }
            if (a.y != b.y) {
                return a.y < b.y;
            }
            return a.z < b.z;
        }
    }
}
