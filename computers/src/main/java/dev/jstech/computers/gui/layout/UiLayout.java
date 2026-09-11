/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Where the widgets of a program's window land: rows lay their widgets across, columns lay them down, and
 * a weight takes a share of whatever room is left over.
 *
 * <p>This is only the arithmetic. What a widget is as wide as when nobody says (a label is as wide as its
 * words) is worked out where the letters are, and handed in; what a widget looks like is the machine's
 * own system's. Keeping the arithmetic here means it can be checked without a screen.
 */
public final class UiLayout {

    /** What a window keeps round what it holds, and what a row or a column leaves between two widgets. */
    public static final int PAD = 6;
    public static final int GAP = 4;

    private UiLayout() {
    }

    /**
     * One widget to place.
     *
     * @param parent   the widget that holds it, or zero for the one the window shows
     * @param weight   its share of the room left over, or zero for none
     * @param wide     how wide it is when nobody says otherwise
     * @param tall     how tall it is when nobody says otherwise
     * @param askedW   the width the program asked for, or zero for none
     * @param spacing  what it leaves between its own widgets, when it holds any
     * @param placedX  where the program put it, or {@link #LAID_OUT} to let its holder place it
     */
    public record Box(long id, long parent, String kind, int weight, int wide, int tall, int askedW, int askedH,
                      int spacing, int placedX, int placedY) {
    }

    /** Where a widget landed. */
    public record Rect(long id, int x, int y, int w, int h) {
    }

    /** What a widget the program did not place has instead of a place of its own. */
    public static final int LAID_OUT = -1;

    /**
     * Lays out a window's widgets inside the room it has, giving back where each of them landed.
     *
     * <p>Every widget in the list is placed exactly once, holders before the widgets they hold, so
     * drawing them in this order draws a row before what is in it.
     */
    public static List<Rect> lay(final List<Box> boxes, final int x, final int y, final int width,
                                 final int height) {
        final Map<Long, List<Box>> children = new HashMap<>();
        final List<Box> roots = new ArrayList<>();
        for (final Box box : boxes) {
            if (box.parent() == 0) {
                roots.add(box);
            } else {
                children.computeIfAbsent(box.parent(), key -> new ArrayList<>()).add(box);
            }
        }
        final List<Rect> out = new ArrayList<>();
        for (final Box root : roots) {
            if (root.placedX() == LAID_OUT) {
                place(root, children, x + PAD, y + PAD, width - PAD * 2, height - PAD * 2, out);
            } else {
                place(root, children, x + root.placedX(), y + root.placedY(),
                        root.askedW() > 0 ? root.askedW() : natural(root, children, true),
                        root.askedH() > 0 ? root.askedH() : natural(root, children, false), out);
            }
        }
        return out;
    }

    /** How wide (or tall) a widget is when nobody says otherwise, counting what it holds. */
    public static int natural(final Box box, final Map<Long, List<Box>> children, final boolean across) {
        final int asked = across ? box.askedW() : box.askedH();
        if (asked > 0) {
            return asked;
        }
        final List<Box> held = children.getOrDefault(box.id(), List.of());
        if (held.isEmpty()) {
            return across ? box.wide() : box.tall();
        }
        final boolean lengthwise = isRow(box) == across;
        int total = 0;
        int most = 0;
        for (final Box child : held) {
            final int size = natural(child, children, across);
            total += size;
            most = Math.max(most, size);
        }
        return lengthwise ? total + gap(box) * Math.max(0, held.size() - 1) : most;
    }

    private static void place(final Box box, final Map<Long, List<Box>> children, final int x, final int y,
                              final int width, final int height, final List<Rect> out) {
        out.add(new Rect(box.id(), x, y, Math.max(0, width), Math.max(0, height)));
        final List<Box> held = children.getOrDefault(box.id(), List.of());
        if (held.isEmpty()) {
            return;
        }
        final boolean row = isRow(box);
        final int gap = gap(box);
        final int along = (row ? width : height) - gap * Math.max(0, held.size() - 1);
        final int[] sizes = share(held, children, row, along);
        int at = row ? x : y;
        for (int i = 0; i < held.size(); i++) {
            final Box child = held.get(i);
            final int size = sizes[i];
            if (row) {
                final int tall = child.askedH() > 0 ? Math.min(child.askedH(), height) : height;
                place(child, children, at, y + (height - tall) / 2, size, tall, out);
            } else {
                final int wide = child.askedW() > 0 ? Math.min(child.askedW(), width) : width;
                place(child, children, x + (width - wide) / 2, at, wide, size, out);
            }
            at += size + gap;
        }
    }

    /*
     * What each widget takes along the row or the column: the size it asks for, and then a share of what
     * is left over for each weight. The last weighted one takes the remainder, so the widths add up to
     * the room there is rather than a pixel less.
     */
    private static int[] share(final List<Box> held, final Map<Long, List<Box>> children, final boolean row,
                               final int along) {
        final int[] sizes = new int[held.size()];
        int fixed = 0;
        int weights = 0;
        for (int i = 0; i < held.size(); i++) {
            final Box child = held.get(i);
            sizes[i] = natural(child, children, row);
            if (child.weight() > 0) {
                weights += child.weight();
                sizes[i] = 0;
            }
            fixed += sizes[i];
        }
        if (weights == 0) {
            return sizes;
        }
        final int left = Math.max(0, along - fixed);
        int given = 0;
        int last = -1;
        for (int i = 0; i < held.size(); i++) {
            if (held.get(i).weight() <= 0) {
                continue;
            }
            last = i;
            sizes[i] = left * held.get(i).weight() / weights;
            given += sizes[i];
        }
        if (last >= 0) {
            sizes[last] += left - given;
        }
        return sizes;
    }

    private static boolean isRow(final Box box) {
        return "Row".equals(box.kind());
    }

    private static int gap(final Box box) {
        return box.spacing() > 0 ? box.spacing() : GAP;
    }
}
