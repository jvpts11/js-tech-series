/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.developer;

import java.util.ArrayList;
import java.util.List;

/**
 * The guides that line the chosen component up with the others, as a drawing program's do: where its left, middle or
 * right falls on another's left, middle or right a line runs down through both, and where its top, middle or bottom
 * falls on another's a line runs across; and how far a component being dragged is pulled so the nearest of them within
 * reach lines up. Boxes are {@code {x, y, width, height}} on the game's screen. Pure.
 */
final class AlignmentGuides {

    private AlignmentGuides() {
    }

    /**
     * A guide: a line down the screen at {@code at} across, from {@code from} to {@code to} down, or, not
     * {@code down}, a line across it at {@code at} down, from {@code from} to {@code to} across.
     *
     * @param down whether it runs down the screen
     * @param at   where it runs
     * @param from where it starts along its run
     * @param to   where it ends
     */
    record Guide(boolean down, int at, int from, int to) {
    }

    /** Every guide along which {@code box} lines up with one of {@code others}. */
    static List<Guide> of(final int[] box, final List<int[]> others) {
        final List<Guide> out = new ArrayList<>();
        for (final int[] other : others) {
            for (final int mine : across(box)) {
                for (final int theirs : across(other)) {
                    if (mine == theirs) {
                        out.add(new Guide(true, mine, Math.min(box[1], other[1]),
                                Math.max(box[1] + box[3], other[1] + other[3])));
                    }
                }
            }
            for (final int mine : down(box)) {
                for (final int theirs : down(other)) {
                    if (mine == theirs) {
                        out.add(new Guide(false, mine, Math.min(box[0], other[0]),
                                Math.max(box[0] + box[2], other[0] + other[2])));
                    }
                }
            }
        }
        return out;
    }

    /**
     * How far, across and down, to move {@code box} so that of its edges and middles the one nearest to one of
     * {@code others}' lines up with it, each way apart; nothing that way when none is within {@code reach}.
     */
    static int[] pull(final int[] box, final List<int[]> others, final int reach) {
        int bestX = reach + 1;
        int bestY = reach + 1;
        for (final int[] other : others) {
            for (final int mine : across(box)) {
                for (final int theirs : across(other)) {
                    if (Math.abs(theirs - mine) < Math.abs(bestX)) {
                        bestX = theirs - mine;
                    }
                }
            }
            for (final int mine : down(box)) {
                for (final int theirs : down(other)) {
                    if (Math.abs(theirs - mine) < Math.abs(bestY)) {
                        bestY = theirs - mine;
                    }
                }
            }
        }
        return new int[] {Math.abs(bestX) <= reach ? bestX : 0, Math.abs(bestY) <= reach ? bestY : 0};
    }

    /* A box's left, middle and right. */
    private static int[] across(final int[] box) {
        return new int[] {box[0], box[0] + box[2] / 2, box[0] + box[2]};
    }

    /* A box's top, middle and bottom. */
    private static int[] down(final int[] box) {
        return new int[] {box[1], box[1] + box[3] / 2, box[1] + box[3]};
    }
}
