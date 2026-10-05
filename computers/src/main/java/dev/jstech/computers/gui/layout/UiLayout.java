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
import java.util.Set;

/**
 * Where the widgets of a program's window land: rows lay their widgets across, columns lay them down, and
 * a weight takes a share of whatever room is left over.
 *
 * <p>This is only the arithmetic. What a widget is as wide as when nobody says (a label is as wide as its
 * words) is worked out where the letters are, and handed in; what a widget looks like is the machine's
 * own system's. Keeping the arithmetic here means it can be checked without a screen.
 *
 * <p>Three widgets hold another in a way of their own: a tab view shows only the page whose tab is picked, under its
 * strip of tabs; a group box shows its one widget inside its frame, under its caption; and a scroll view gives its
 * widget all the height it asks for and shows a window of it, moved by how far it is scrolled. A context menu and a
 * file dialog take no place at all, since one waits for its click and the other is a window of its own.
 *
 * <p>The same arithmetic lays out a window drawn in pixels on a desktop and one drawn in letters on a terminal; only
 * the {@link Metrics} differ, a cell of letters being one unit across and one down.
 */
public final class UiLayout {

    /** What a window keeps round what it holds, and what a row or a column leaves between two widgets. */
    public static final int PAD = 6;
    public static final int GAP = 4;
    /** How tall a tab view's strip of tabs is, and a group box's caption. */
    public static final int TAB_H = 14;
    public static final int CAPTION_H = 10;
    /** How wide a scroll view's bar is. */
    public static final int SCROLLBAR = 8;

    /** What a widget the program did not place has instead of a place of its own. */
    public static final int LAID_OUT = -1;

    /** The measures of a desktop's pixels. */
    public static final Metrics PIXELS = new Metrics(PAD, GAP, TAB_H, CAPTION_H, SCROLLBAR, 1);
    /**
     * The measures of a terminal's cells: one cell round a window and between widgets, one row for a tab strip or a
     * caption, one column for a scroll bar; a spacing a program asked for in pixels counts four of them to a cell.
     */
    public static final Metrics CELLS = new Metrics(1, 1, 1, 1, 1, 4);

    private static final Set<String> FLOATING = Set.of("ContextMenu", "OpenFileDialog", "SaveFileDialog");

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
     * @param picked   which of its pages a tab view shows, counted from one
     */
    public record Box(long id, long parent, String kind, int weight, int wide, int tall, int askedW, int askedH,
                      int spacing, int placedX, int placedY, int picked) {

        /** A widget that picks nothing, which is every widget but a tab view. */
        public Box(final long id, final long parent, final String kind, final int weight, final int wide,
                   final int tall, final int askedW, final int askedH, final int spacing, final int placedX,
                   final int placedY) {
            this(id, parent, kind, weight, wide, tall, askedW, askedH, spacing, placedX, placedY, 0);
        }
    }

    /** Where a widget landed. */
    public record Rect(long id, int x, int y, int w, int h) {
    }

    /**
     * The measures a layout is worked out in.
     *
     * @param pad       what a window keeps round what it holds
     * @param gap       what a row or a column leaves between two widgets when the program asked for nothing
     * @param tab       how tall a tab view's strip is
     * @param caption   how tall a group box's caption is
     * @param scrollbar how wide a scroll view's bar is
     * @param perUnit   how many of a program's pixels of spacing make one unit of these measures
     */
    public record Metrics(int pad, int gap, int tab, int caption, int scrollbar, int perUnit) {
    }

    /** Whether a widget of that kind takes no place in the layout: a menu waiting for its click, or a dialog. */
    public static boolean floating(final String kind) {
        return FLOATING.contains(kind);
    }

    /** Lays out a window's widgets with nothing scrolled. */
    public static List<Rect> lay(final List<Box> boxes, final int x, final int y, final int width,
                                 final int height) {
        return lay(boxes, x, y, width, height, Map.of(), PIXELS);
    }

    /** Lays out a window's widgets in a desktop's pixels. */
    public static List<Rect> lay(final List<Box> boxes, final int x, final int y, final int width,
                                 final int height, final Map<Long, Integer> scrolled) {
        return lay(boxes, x, y, width, height, scrolled, PIXELS);
    }

    /**
     * Lays out a window's widgets inside the room it has, giving back where each of them landed.
     *
     * <p>Every widget in the list is placed exactly once, holders before the widgets they hold, so drawing them in
     * this order draws a row before what is in it. A page of a tab view that is not picked, and everything inside it,
     * is not placed at all.
     *
     * @param scrolled how far down each scroll view is scrolled, by its id
     */
    public static List<Rect> lay(final List<Box> boxes, final int x, final int y, final int width,
                                 final int height, final Map<Long, Integer> scrolled, final Metrics metrics) {
        final Map<Long, List<Box>> children = childrenOf(boxes);
        final List<Box> roots = new ArrayList<>();
        for (final Box box : boxes) {
            if (!floating(box.kind()) && box.parent() == 0) {
                roots.add(box);
            }
        }
        final Placing placing = new Placing(children, scrolled, metrics);
        for (final Box root : roots) {
            if (root.placedX() == LAID_OUT) {
                placing.place(root, x + metrics.pad(), y + metrics.pad(), width - metrics.pad() * 2,
                        height - metrics.pad() * 2);
            } else {
                placing.place(root, x + root.placedX(), y + root.placedY(),
                        root.askedW() > 0 ? root.askedW() : natural(root, children, true, metrics),
                        root.askedH() > 0 ? root.askedH() : natural(root, children, false, metrics));
            }
        }
        return placing.out;
    }

    /** How wide (or tall) a widget is in pixels when nobody says otherwise, counting what it holds. */
    public static int natural(final Box box, final Map<Long, List<Box>> children, final boolean across) {
        return natural(box, children, across, PIXELS);
    }

    /** How wide (or tall) a widget is when nobody says otherwise, counting what it holds, in those measures. */
    public static int natural(final Box box, final Map<Long, List<Box>> children, final boolean across,
                              final Metrics metrics) {
        final int asked = across ? box.askedW() : box.askedH();
        if (asked > 0) {
            return asked;
        }
        final List<Box> held = children.getOrDefault(box.id(), List.of());
        if (held.isEmpty()) {
            return across ? box.wide() : box.tall();
        }
        switch (box.kind()) {
            case "TabView" -> {
                int most = across ? box.wide() : 0;
                for (final Box page : held) {
                    most = Math.max(most, natural(page, children, across, metrics));
                }
                return most + (across ? 4 / metrics.perUnit() : metrics.tab() + 4 / metrics.perUnit());
            }
            case "GroupBox" -> {
                final int inner = natural(held.getFirst(), children, across, metrics);
                return Math.max(across ? box.wide() : 0, inner + (across ? metrics.pad() * 2
                        : metrics.caption() + metrics.pad() + 2 / metrics.perUnit()));
            }
            case "ScrollView" -> {
                // Its widget, its bar, and a line of frame on either side.
                return across ? natural(held.getFirst(), children, true, metrics) + metrics.scrollbar() + 2
                        : box.tall();
            }
            default -> {
                // A row or a column, below.
            }
        }
        final boolean lengthwise = isRow(box) == across;
        int total = 0;
        int most = 0;
        for (final Box child : held) {
            final int size = natural(child, children, across, metrics);
            total += size;
            most = Math.max(most, size);
        }
        return lengthwise ? total + gap(box, metrics) * Math.max(0, held.size() - 1) : most;
    }

    /** How far a scroll view can be scrolled: how much taller its widget is than the room the view shows. */
    public static int scrollRoom(final Box view, final List<Box> boxes, final int viewHeight) {
        return scrollRoom(view, boxes, viewHeight, PIXELS);
    }

    /** How far a scroll view can be scrolled, in those measures. */
    public static int scrollRoom(final Box view, final List<Box> boxes, final int viewHeight,
                                 final Metrics metrics) {
        final Map<Long, List<Box>> children = childrenOf(boxes);
        final List<Box> held = children.getOrDefault(view.id(), List.of());
        return held.isEmpty() ? 0 : Math.max(0, natural(held.getFirst(), children, false, metrics)
                - (viewHeight - 2));
    }

    private static Map<Long, List<Box>> childrenOf(final List<Box> boxes) {
        final Map<Long, List<Box>> children = new HashMap<>();
        for (final Box box : boxes) {
            if (!floating(box.kind()) && box.parent() != 0) {
                children.computeIfAbsent(box.parent(), key -> new ArrayList<>()).add(box);
            }
        }
        return children;
    }

    /* Places widget after widget, holders first, into the list of where each landed. */
    private static final class Placing {

        private final Map<Long, List<Box>> children;
        private final Map<Long, Integer> scrolled;
        private final Metrics metrics;
        private final List<Rect> out = new ArrayList<>();

        Placing(final Map<Long, List<Box>> children, final Map<Long, Integer> scrolled, final Metrics metrics) {
            this.children = children;
            this.scrolled = scrolled;
            this.metrics = metrics;
        }

        void place(final Box box, final int x, final int y, final int width, final int height) {
            this.out.add(new Rect(box.id(), x, y, Math.max(0, width), Math.max(0, height)));
            final List<Box> held = this.children.getOrDefault(box.id(), List.of());
            if (held.isEmpty()) {
                return;
            }
            final int edge = 2 / this.metrics.perUnit();
            switch (box.kind()) {
                case "TabView" -> {
                    final int page = Math.clamp(box.picked(), 1, held.size()) - 1;
                    this.place(held.get(page), x + edge, y + this.metrics.tab() + edge, width - edge * 2,
                            height - this.metrics.tab() - edge * 2);
                }
                case "GroupBox" -> this.place(held.getFirst(), x + this.metrics.pad(),
                        y + this.metrics.caption() + edge, width - this.metrics.pad() * 2,
                        height - this.metrics.caption() - this.metrics.pad() - edge);
                case "ScrollView" -> {
                    final Box inside = held.getFirst();
                    final int frame = 1;
                    final int tall = Math.max(height - frame * 2, natural(inside, this.children, false,
                            this.metrics));
                    final int most = Math.max(0, tall - (height - frame * 2));
                    final int down = Math.clamp(this.scrolled.getOrDefault(box.id(), 0), 0, most);
                    this.place(inside, x + frame, y + frame - down, width - this.metrics.scrollbar() - frame * 2,
                            tall);
                }
                default -> this.lay(box, held, x, y, width, height);
            }
        }

        private void lay(final Box box, final List<Box> held, final int x, final int y, final int width,
                         final int height) {
            final boolean row = isRow(box);
            final int gap = gap(box, this.metrics);
            final int along = (row ? width : height) - gap * Math.max(0, held.size() - 1);
            final int[] sizes = share(held, this.children, row, along, this.metrics);
            int at = row ? x : y;
            for (int i = 0; i < held.size(); i++) {
                final Box child = held.get(i);
                final int size = sizes[i];
                if (row) {
                    final int tall = child.askedH() > 0 ? Math.min(child.askedH(), height) : height;
                    this.place(child, at, y + (height - tall) / 2, size, tall);
                } else {
                    final int wide = child.askedW() > 0 ? Math.min(child.askedW(), width) : width;
                    this.place(child, x + (width - wide) / 2, at, wide, size);
                }
                at += size + gap;
            }
        }
    }

    /*
     * What each widget takes along the row or the column: the size it asks for, and then a share of what
     * is left over for each weight. The last weighted one takes the remainder, so the widths add up to
     * the room there is rather than a pixel less.
     */
    private static int[] share(final List<Box> held, final Map<Long, List<Box>> children, final boolean row,
                               final int along, final Metrics metrics) {
        final int[] sizes = new int[held.size()];
        int fixed = 0;
        int weights = 0;
        for (int i = 0; i < held.size(); i++) {
            final Box child = held.get(i);
            sizes[i] = natural(child, children, row, metrics);
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

    private static int gap(final Box box, final Metrics metrics) {
        return box.spacing() > 0 ? Math.max(0, box.spacing() / metrics.perUnit()) : metrics.gap();
    }
}
