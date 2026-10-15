/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.cable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The blocks a run of cable dragged from one block to another goes through: along the axis it covers most first, then
 * the next, then the last, each block one step from the one before, as a player lays a run by hand; for a cable that
 * runs only straight, along that first axis alone. Pure.
 */
public final class RunPath {

    /** The most blocks a run dragged at once lays. */
    public static final int MOST = 64;

    private RunPath() {
    }

    /**
     * The blocks from {@code from} to {@code to}, both included, at most {@link #MOST}; along one axis only when
     * {@code straight}, which stops the run level with {@code to} on that axis.
     */
    public static List<Step> between(final Step from, final Step to, final boolean straight) {
        final List<Step> path = new ArrayList<>();
        path.add(from);
        final int[] delta = {to.x() - from.x(), to.y() - from.y(), to.z() - from.z()};
        final List<Integer> axes = new ArrayList<>(List.of(0, 1, 2));
        axes.sort(Comparator.comparingInt((Integer axis) -> -Math.abs(delta[axis])));
        int x = from.x();
        int y = from.y();
        int z = from.z();
        for (int k = 0; k < (straight ? 1 : axes.size()); k++) {
            final int axis = axes.get(k);
            final int step = Integer.signum(delta[axis]);
            for (int moved = 0; moved < Math.abs(delta[axis]) && path.size() < MOST; moved++) {
                switch (axis) {
                    case 0 -> x += step;
                    case 1 -> y += step;
                    default -> z += step;
                }
                path.add(new Step(x, y, z));
            }
        }
        return path;
    }

    /**
     * One block of a run, by its coordinates.
     *
     * @param x its x
     * @param y its y
     * @param z its z
     */
    public record Step(int x, int y, int z) {
    }
}
