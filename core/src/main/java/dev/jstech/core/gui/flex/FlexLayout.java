/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.flex;

import java.util.ArrayList;
import java.util.List;

/**
 * Lays a tree of {@link FlexBox}es out in a rectangle: rows and columns that share their room by {@link FlexBox#grow},
 * give it up by {@link FlexBox#shrink} down to their smallest sizes, wrap when asked, and put what is left where
 * {@link Justify} says; grids whose columns are fixed, sized by what they hold or a share of the rest; stacks laid over
 * each other. Every size is a whole pixel, and a share of room is cut by largest remainder, so the parts always add up
 * to the room they share.
 *
 * <p>A box whose contents need more room than they get even at their smallest is placed all the same and marked
 * {@linkplain Placed#overflowing overflowing}, so a test or the developer's inspector says where a screen does not fit.
 */
public final class FlexLayout {

    private FlexLayout() {
    }

    /** Lays {@code root} out at the top-left of a {@code width} by {@code height} rectangle. */
    public static Placed layout(final FlexBox root, final int width, final int height) {
        final int w = root.width() != FlexBox.UNSET ? root.width() : width;
        final int h = root.height() != FlexBox.UNSET ? root.height() : height;
        return place(root, 0, 0, clamp(w, root.minWidth(), root.maxWidth()),
                clamp(h, root.minHeight(), root.maxHeight()));
    }

    /**
     * How big {@code box} wants to be, padding included and its margin not, when it may be at most {@code maxWidth}
     * wide: what a screen asks to know how tall to make itself for the room it has across.
     */
    public static Size preferred(final FlexBox box, final int maxWidth) {
        final int widest = box.width() != FlexBox.UNSET ? box.width() : Math.min(maxWidth, box.maxWidth());
        final int inner = Math.max(0, widest - box.padding().horizontal());
        final Size content = switch (box.flow()) {
            case LEAF -> box.measure().measure(inner);
            case ROW -> rowContent(box, inner);
            case COLUMN -> columnContent(box, inner);
            case GRID -> gridContent(box, inner);
            case STACK -> stackContent(box, inner);
        };
        final int w = box.width() != FlexBox.UNSET ? box.width() : content.width() + box.padding().horizontal();
        final int h = box.height() != FlexBox.UNSET ? box.height() : content.height() + box.padding().vertical();
        return new Size(clamp(w, box.minWidth(), box.maxWidth()), clamp(h, box.minHeight(), box.maxHeight()));
    }

    /**
     * {@code total} cut into parts by {@code weights}, by largest remainder: every part a whole number, and together
     * exactly {@code total}. All the weights zero cut nothing.
     */
    public static int[] share(final int total, final int[] weights) {
        final int[] out = new int[weights.length];
        long sum = 0;
        for (final int weight : weights) {
            sum += Math.max(0, weight);
        }
        if (sum == 0 || total <= 0) {
            return out;
        }
        final long[] remainders = new long[weights.length];
        int given = 0;
        for (int i = 0; i < weights.length; i++) {
            final long exact = (long) total * Math.max(0, weights[i]);
            out[i] = (int) (exact / sum);
            remainders[i] = exact % sum;
            given += out[i];
        }
        for (int left = total - given; left > 0; left--) {
            int best = -1;
            for (int i = 0; i < weights.length; i++) {
                if (weights[i] > 0 && (best < 0 || remainders[i] > remainders[best])) {
                    best = i;
                }
            }
            out[best]++;
            remainders[best] = -1;
        }
        return out;
    }

    private static Placed place(final FlexBox box, final int x, final int y, final int w, final int h) {
        final int ix = x + box.padding().left();
        final int iy = y + box.padding().top();
        final int iw = Math.max(0, w - box.padding().horizontal());
        final int ih = Math.max(0, h - box.padding().vertical());
        final List<Placed> placed = new ArrayList<>();
        final boolean overflowing = switch (box.flow()) {
            case LEAF -> leafOverflows(box, iw, ih);
            case ROW -> box.wrap() ? placeWrappedRow(box, ix, iy, iw, ih, placed)
                    : placeLine(box, box.held(), ix, iy, iw, ih, placed, true);
            case COLUMN -> placeLine(box, box.held(), ix, iy, iw, ih, placed, false);
            case GRID -> placeGrid(box, ix, iy, iw, ih, placed);
            case STACK -> placeStack(box, ix, iy, iw, ih, placed);
        };
        return new Placed(box, x, y, w, h, placed, overflowing);
    }

    /* Whether what a leaf shows is bigger than the room it was given. */
    private static boolean leafOverflows(final FlexBox leaf, final int iw, final int ih) {
        final Size wanted = leaf.measure().measure(iw);
        return wanted.width() > iw || wanted.height() > ih;
    }

    /*
     * Lays boxes out one after the other along a row ({@code across} true) or a column: their sizes along it first,
     * then the room left grown into or given up, then where each stands across it.
     */
    private static boolean placeLine(final FlexBox box, final List<FlexBox> kids, final int x, final int y,
                                     final int iw, final int ih, final List<Placed> out, final boolean across) {
        final int n = kids.size();
        if (n == 0) {
            return false;
        }
        final int main = across ? iw : ih;
        final int[] cross = new int[n];
        final int[] size = new int[n];
        int used = box.gap() * (n - 1);
        for (int i = 0; i < n; i++) {
            final FlexBox kid = kids.get(i);
            cross[i] = across ? 0 : crossWidth(box, kid, iw);
            size[i] = across ? basisWidth(kid, iw) : basisHeight(kid, cross[i]);
            used += size[i] + (across ? kid.margin().horizontal() : kid.margin().vertical());
        }
        int free = main - used;
        boolean overflowing = false;
        if (free > 0) {
            free = grow(kids, size, free, across);
        } else if (free < 0) {
            final int deficit = shrink(kids, size, -free, across);
            overflowing = deficit > 0;
            free = 0;
        }
        int at = (across ? x : y) + lead(box.justify(), free, n);
        final int between = box.gap() + spread(box.justify(), free, n);
        for (int i = 0; i < n; i++) {
            final FlexBox kid = kids.get(i);
            final Insets m = kid.margin();
            if (across) {
                final int ch = crossHeight(box, kid, size[i], ih);
                final int cy = y + m.top() + offset(alignOf(box, kid), ih - m.vertical(), ch);
                out.add(place(kid, at + m.left(), cy, size[i], ch));
                overflowing |= ch + m.vertical() > ih;
                at += size[i] + m.horizontal() + between;
            } else {
                final int cx = x + m.left() + offset(alignOf(box, kid), iw - m.horizontal(), cross[i]);
                out.add(place(kid, cx, at + m.top(), cross[i], size[i]));
                overflowing |= cross[i] + m.horizontal() > iw;
                at += size[i] + m.vertical() + between;
            }
        }
        return overflowing;
    }

    /* A row that runs out of room goes on in a new line under it, each line laid out as a row of its own. */
    private static boolean placeWrappedRow(final FlexBox box, final int x, final int y, final int iw, final int ih,
                                           final List<Placed> out) {
        final List<List<FlexBox>> lines = wrapLines(box, iw);
        boolean overflowing = false;
        int at = y;
        for (int l = 0; l < lines.size(); l++) {
            final List<FlexBox> line = lines.get(l);
            final int tall = lineHeight(line, iw);
            overflowing |= placeLine(box, line, x, at, iw, tall, out, true);
            at += tall + (l + 1 < lines.size() ? box.gap() : 0);
        }
        return overflowing || at - y > ih;
    }

    private static boolean placeGrid(final FlexBox box, final int x, final int y, final int iw, final int ih,
                                     final List<Placed> out) {
        final List<Track> tracks = box.columns();
        final int columns = tracks.size();
        final List<FlexBox> kids = box.held();
        final int[] widths = trackWidths(box, iw, false);
        int fixed = box.gap() * (columns - 1);
        for (final int width : widths) {
            fixed += width;
        }
        boolean overflowing = fixed > iw;
        final List<int[]> cells = cells(kids, columns);
        final int rows = cells.isEmpty() ? 0 : cells.get(cells.size() - 1)[0] + 1;
        final int[] heights = new int[rows];
        for (int i = 0; i < kids.size(); i++) {
            final int[] cell = cells.get(i);
            final FlexBox kid = kids.get(i);
            final int cw = cellWidth(widths, cell[1], cell[2], box.gap());
            heights[cell[0]] = Math.max(heights[cell[0]], outerHeight(kid, cw));
        }
        final int[] rowTop = new int[rows];
        int at = y;
        for (int r = 0; r < rows; r++) {
            rowTop[r] = at;
            at += heights[r] + box.gap();
        }
        overflowing |= rows > 0 && at - box.gap() - y > ih;
        for (int i = 0; i < kids.size(); i++) {
            final int[] cell = cells.get(i);
            final FlexBox kid = kids.get(i);
            final Insets m = kid.margin();
            int cx = x;
            for (int c = 0; c < cell[1]; c++) {
                cx += widths[c] + box.gap();
            }
            final int cw = cellWidth(widths, cell[1], cell[2], box.gap()) - m.horizontal();
            final int chRoom = heights[cell[0]] - m.vertical();
            final Align align = alignOf(box, kid);
            final int kw = align == Align.STRETCH && kid.width() == FlexBox.UNSET
                    ? clamp(cw, kid.minWidth(), kid.maxWidth()) : Math.min(cw, preferred(kid, cw).width());
            final int kh = align == Align.STRETCH && kid.height() == FlexBox.UNSET
                    ? clamp(chRoom, kid.minHeight(), kid.maxHeight()) : preferred(kid, kw).height();
            out.add(place(kid, cx + m.left() + offset(align, cw, kw), rowTop[cell[0]] + m.top()
                    + offset(align, chRoom, kh), kw, kh));
        }
        return overflowing;
    }

    private static boolean placeStack(final FlexBox box, final int x, final int y, final int iw, final int ih,
                                      final List<Placed> out) {
        boolean overflowing = false;
        for (final FlexBox kid : box.held()) {
            final Insets m = kid.margin();
            final Align align = alignOf(box, kid);
            final int roomW = Math.max(0, iw - m.horizontal());
            final int roomH = Math.max(0, ih - m.vertical());
            final Size wanted = preferred(kid, roomW);
            final int kw = align == Align.STRETCH && kid.width() == FlexBox.UNSET
                    ? clamp(roomW, kid.minWidth(), kid.maxWidth()) : wanted.width();
            final int kh = align == Align.STRETCH && kid.height() == FlexBox.UNSET
                    ? clamp(roomH, kid.minHeight(), kid.maxHeight()) : preferred(kid, kw).height();
            overflowing |= kw > roomW || kh > roomH;
            out.add(place(kid, x + m.left() + offset(align, roomW, kw), y + m.top() + offset(align, roomH, kh), kw,
                    kh));
        }
        return overflowing;
    }

    private static Size rowContent(final FlexBox box, final int inner) {
        final List<FlexBox> kids = box.held();
        if (kids.isEmpty()) {
            return Size.ZERO;
        }
        if (box.wrap()) {
            int widest = 0;
            int tall = 0;
            final List<List<FlexBox>> lines = wrapLines(box, inner);
            for (final List<FlexBox> line : lines) {
                widest = Math.max(widest, lineWidth(box, line, inner));
                tall += lineHeight(line, inner);
            }
            return new Size(widest, tall + box.gap() * (lines.size() - 1));
        }
        return new Size(lineWidth(box, kids, inner), lineHeight(kids, inner));
    }

    private static Size columnContent(final FlexBox box, final int inner) {
        int widest = 0;
        int tall = 0;
        for (final FlexBox kid : box.held()) {
            final Size wanted = preferred(kid, Math.max(0, inner - kid.margin().horizontal()));
            widest = Math.max(widest, wanted.width() + kid.margin().horizontal());
            tall += wanted.height() + kid.margin().vertical();
        }
        return new Size(widest, tall + box.gap() * Math.max(0, box.held().size() - 1));
    }

    private static Size gridContent(final FlexBox box, final int inner) {
        final int columns = box.columns().size();
        final int[] widths = trackWidths(box, inner, true);
        int wide = box.gap() * (columns - 1);
        for (final int width : widths) {
            wide += width;
        }
        final List<int[]> cells = cells(box.held(), columns);
        if (cells.isEmpty()) {
            return new Size(wide, 0);
        }
        final int rows = cells.get(cells.size() - 1)[0] + 1;
        final int[] heights = new int[rows];
        for (int i = 0; i < cells.size(); i++) {
            final int[] cell = cells.get(i);
            heights[cell[0]] = Math.max(heights[cell[0]], outerHeight(box.held().get(i),
                    cellWidth(widths, cell[1], cell[2], box.gap())));
        }
        int tall = box.gap() * (rows - 1);
        for (final int height : heights) {
            tall += height;
        }
        return new Size(wide, tall);
    }

    private static Size stackContent(final FlexBox box, final int inner) {
        int widest = 0;
        int tall = 0;
        for (final FlexBox kid : box.held()) {
            final Size wanted = preferred(kid, Math.max(0, inner - kid.margin().horizontal()));
            widest = Math.max(widest, wanted.width() + kid.margin().horizontal());
            tall = Math.max(tall, wanted.height() + kid.margin().vertical());
        }
        return new Size(widest, tall);
    }

    /*
     * The width of each grid column: the fixed ones as given, those sized by what they hold as their widest single
     * cell, and the fractions a share of the rest, or, when only the wanted size is asked ({@code wanted}), as wide as
     * their widest single cell too.
     */
    private static int[] trackWidths(final FlexBox box, final int inner, final boolean wanted) {
        final List<Track> tracks = box.columns();
        final int columns = tracks.size();
        final int[] widths = new int[columns];
        final int[] widestCell = new int[columns];
        final List<int[]> cells = cells(box.held(), columns);
        for (int i = 0; i < cells.size(); i++) {
            final int[] cell = cells.get(i);
            if (cell[2] == 1) {
                final FlexBox kid = box.held().get(i);
                widestCell[cell[1]] = Math.max(widestCell[cell[1]],
                        preferred(kid, Math.max(0, inner - kid.margin().horizontal())).width()
                                + kid.margin().horizontal());
            }
        }
        int taken = box.gap() * (columns - 1);
        final int[] shares = new int[columns];
        for (int c = 0; c < columns; c++) {
            final Track track = tracks.get(c);
            switch (track.kind()) {
                case FIXED -> widths[c] = track.value();
                case AUTO -> widths[c] = widestCell[c];
                case FRACTION -> {
                    shares[c] = track.value();
                    widths[c] = wanted ? widestCell[c] : 0;
                }
            }
            if (track.kind() != Track.Kind.FRACTION) {
                taken += widths[c];
            }
        }
        if (!wanted) {
            final int[] cut = share(Math.max(0, inner - taken), shares);
            for (int c = 0; c < columns; c++) {
                if (tracks.get(c).kind() == Track.Kind.FRACTION) {
                    widths[c] = cut[c];
                }
            }
        }
        return widths;
    }

    /* Where each box of a grid falls: its row, its first column and how many columns it takes. */
    private static List<int[]> cells(final List<FlexBox> kids, final int columns) {
        final List<int[]> out = new ArrayList<>(kids.size());
        int row = 0;
        int column = 0;
        for (final FlexBox kid : kids) {
            final int span = Math.min(columns, kid.span());
            if (column + span > columns) {
                row++;
                column = 0;
            }
            out.add(new int[] {row, column, span});
            column += span;
            if (column >= columns) {
                row++;
                column = 0;
            }
        }
        return out;
    }

    private static int cellWidth(final int[] widths, final int first, final int span, final int gap) {
        int wide = gap * (span - 1);
        for (int c = first; c < first + span && c < widths.length; c++) {
            wide += widths[c];
        }
        return wide;
    }

    /* The lines of a wrapping row: a box goes on the line while it fits, and opens a new one when it does not. */
    private static List<List<FlexBox>> wrapLines(final FlexBox box, final int inner) {
        final List<List<FlexBox>> lines = new ArrayList<>();
        List<FlexBox> line = new ArrayList<>();
        int used = 0;
        for (final FlexBox kid : box.held()) {
            final int wide = basisWidth(kid, inner) + kid.margin().horizontal();
            final int need = line.isEmpty() ? wide : used + box.gap() + wide;
            if (!line.isEmpty() && need > inner) {
                lines.add(line);
                line = new ArrayList<>();
                used = wide;
            } else {
                used = need;
            }
            line.add(kid);
        }
        if (!line.isEmpty()) {
            lines.add(line);
        }
        return lines;
    }

    private static int lineWidth(final FlexBox box, final List<FlexBox> line, final int inner) {
        int wide = box.gap() * Math.max(0, line.size() - 1);
        for (final FlexBox kid : line) {
            wide += basisWidth(kid, inner) + kid.margin().horizontal();
        }
        return wide;
    }

    private static int lineHeight(final List<FlexBox> line, final int inner) {
        int tall = 0;
        for (final FlexBox kid : line) {
            tall = Math.max(tall, outerHeight(kid, basisWidth(kid, inner) + kid.margin().horizontal()));
        }
        return tall;
    }

    /* A box's own width along a row before any growing or shrinking. */
    private static int basisWidth(final FlexBox kid, final int room) {
        if (kid.width() != FlexBox.UNSET) {
            return clamp(kid.width(), kid.minWidth(), kid.maxWidth());
        }
        return preferred(kid, Math.max(0, room - kid.margin().horizontal())).width();
    }

    /* A box's own height down a column, for the width it was given across it. */
    private static int basisHeight(final FlexBox kid, final int width) {
        if (kid.height() != FlexBox.UNSET) {
            return clamp(kid.height(), kid.minHeight(), kid.maxHeight());
        }
        return clamp(preferred(kid, width).height(), kid.minHeight(), kid.maxHeight());
    }

    /* How wide a box stands across a column: stretched over it, or its own width. */
    private static int crossWidth(final FlexBox column, final FlexBox kid, final int iw) {
        final int room = Math.max(0, iw - kid.margin().horizontal());
        if (kid.width() != FlexBox.UNSET) {
            return clamp(kid.width(), kid.minWidth(), kid.maxWidth());
        }
        if (alignOf(column, kid) == Align.STRETCH) {
            return clamp(room, kid.minWidth(), kid.maxWidth());
        }
        return Math.min(room, preferred(kid, room).width());
    }

    /* How tall a box stands across a row: stretched over it, or its own height for the width it got. */
    private static int crossHeight(final FlexBox row, final FlexBox kid, final int width, final int ih) {
        if (kid.height() != FlexBox.UNSET) {
            return clamp(kid.height(), kid.minHeight(), kid.maxHeight());
        }
        if (alignOf(row, kid) == Align.STRETCH) {
            return clamp(Math.max(0, ih - kid.margin().vertical()), kid.minHeight(), kid.maxHeight());
        }
        return clamp(preferred(kid, width).height(), kid.minHeight(), kid.maxHeight());
    }

    private static int outerHeight(final FlexBox kid, final int cellWidth) {
        final int width = Math.max(0, cellWidth - kid.margin().horizontal());
        return basisHeight(kid, width) + kid.margin().vertical();
    }

    /*
     * Hands {@code free} pixels to the boxes that grow, by their shares, none past its largest size; a box that
     * reaches it gives the rest back to the others. Returns what nobody could take.
     */
    private static int grow(final List<FlexBox> kids, final int[] size, final int free, final boolean across) {
        int left = free;
        final boolean[] full = new boolean[kids.size()];
        while (left > 0) {
            final int[] weights = new int[kids.size()];
            boolean any = false;
            for (int i = 0; i < kids.size(); i++) {
                if (!full[i] && kids.get(i).grow() > 0) {
                    weights[i] = kids.get(i).grow();
                    any = true;
                }
            }
            if (!any) {
                break;
            }
            final int[] cut = share(left, weights);
            int given = 0;
            for (int i = 0; i < kids.size(); i++) {
                if (cut[i] == 0) {
                    continue;
                }
                final FlexBox kid = kids.get(i);
                final int most = across ? kid.maxWidth() : kid.maxHeight();
                final int room = Math.max(0, most - size[i]);
                final int take = Math.min(room, cut[i]);
                size[i] += take;
                given += take;
                if (take < cut[i]) {
                    full[i] = true;
                }
            }
            if (given == 0) {
                break;
            }
            left -= given;
        }
        return left;
    }

    /*
     * Takes {@code deficit} pixels back from the boxes that shrink, in proportion to their shares times their sizes,
     * none below its smallest size. Returns what could not be taken back.
     */
    private static int shrink(final List<FlexBox> kids, final int[] size, final int deficit, final boolean across) {
        int left = deficit;
        while (left > 0) {
            final int[] weights = new int[kids.size()];
            boolean any = false;
            for (int i = 0; i < kids.size(); i++) {
                final FlexBox kid = kids.get(i);
                final int least = across ? kid.minWidth() : kid.minHeight();
                if (kid.shrink() > 0 && size[i] > least) {
                    weights[i] = (int) Math.min(Integer.MAX_VALUE, (long) kid.shrink() * size[i]);
                    any = true;
                }
            }
            if (!any) {
                break;
            }
            final int[] cut = share(left, weights);
            int taken = 0;
            for (int i = 0; i < kids.size(); i++) {
                final FlexBox kid = kids.get(i);
                final int least = across ? kid.minWidth() : kid.minHeight();
                final int take = Math.min(cut[i], Math.max(0, size[i] - least));
                size[i] -= take;
                taken += take;
            }
            if (taken == 0) {
                break;
            }
            left -= taken;
        }
        return left;
    }

    private static Align alignOf(final FlexBox parent, final FlexBox kid) {
        return kid.alignSelf() != null ? kid.alignSelf() : parent.align();
    }

    /* Where a box of {@code size} stands in {@code room} for its alignment. */
    private static int offset(final Align align, final int room, final int size) {
        return switch (align) {
            case START, STRETCH -> 0;
            case CENTER -> Math.max(0, (room - size) / 2);
            case END -> Math.max(0, room - size);
        };
    }

    private static int lead(final Justify justify, final int free, final int count) {
        return switch (justify) {
            case START, SPACE_BETWEEN -> 0;
            case CENTER -> free / 2;
            case END -> free;
            case SPACE_AROUND -> count == 0 ? 0 : free / (count * 2);
        };
    }

    private static int spread(final Justify justify, final int free, final int count) {
        return switch (justify) {
            case SPACE_BETWEEN -> count > 1 ? free / (count - 1) : 0;
            case SPACE_AROUND -> count == 0 ? 0 : free / count;
            case START, CENTER, END -> 0;
        };
    }

    private static int clamp(final int value, final int min, final int max) {
        return Math.max(min, Math.min(Math.max(min, max), value));
    }
}
