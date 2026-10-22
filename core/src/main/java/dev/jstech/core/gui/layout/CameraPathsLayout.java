/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.layout;

import java.util.ArrayList;
import java.util.List;

/**
 * Where everything on the studio's page of camera paths sits, in the body the studio gives it: the paths down a
 * column on the left, each a row of its name with its namespace under it, the last a new path written in place; in
 * the middle the keyframes, a table of when, where, which way, field of view, roll and easing, with the four buttons
 * under its rows (two to a row when one row is too narrow for them), the hint under those and the file at the panel's
 * foot; and on the right the path seen from above, fitted to its room. The page draws and places its buttons from
 * these, and a test lays it out on the studio's screens and checks nothing collides.
 */
public final class CameraPathsLayout {

    /** The share of the body the paths take, and the keyframes; the map takes the rest. */
    public static final double LIST_SHARE = 0.23;
    public static final double KEYFRAMES_SHARE = 0.43;
    /** The room inside a panel's edge, and its heading's height. */
    public static final int INSET = 6;
    public static final int HEAD = 16;
    /** A path's row: its name, and its namespace small under it. */
    public static final int PATH_ROW = 18;
    /** A keyframe's row, in the small font. */
    public static final int ROW = 10;
    public static final int BUTTON_HEIGHT = 16;
    /** Between the buttons, and round them. */
    public static final int GAP = 4;
    /** The room a button leaves round its label. */
    public static final int LABEL_ROOM = 8;
    /** A line of the small font, and how many the hint takes. */
    public static final int SMALL_LINE = 8;
    public static final int HINT_LINES = 2;
    /** The room round the map's places, for their numbers, in pixels; and the fewest blocks it spans. */
    public static final int MAP_MARGIN = 12;
    public static final double LEAST_SPAN = 8.0;

    /* Each column's share of the table's width, the last taking the rest: time, where, looking, fov, roll. */
    private static final double[] COLUMNS = {0.13, 0.29, 0.18, 0.09, 0.11};
    private static final int BUTTONS = 4;
    private static final int TWO = 2;

    private CameraPathsLayout() {
    }

    /**
     * The page laid out in {@code body}, with {@code keyframes} keyframes to list and the four buttons' labels
     * {@code labels} pixels wide.
     */
    public static Laid of(final StudioLayout.Rect body, final int keyframes, final List<Integer> labels) {
        if (labels.size() != BUTTONS) {
            throw new IllegalArgumentException("the page has four buttons, not " + labels.size());
        }
        final List<StudioLayout.Rect> columns = StudioLayout.columns(body, LIST_SHARE, KEYFRAMES_SHARE);
        final StudioLayout.Rect middle = columns.get(1);
        final int tableX = middle.x() + INSET;
        final int tableWidth = middle.width() - 2 * INSET;
        final List<Integer> cells = new ArrayList<>();
        double at = tableX;
        cells.add(tableX);
        for (final double share : COLUMNS) {
            at += tableWidth * share;
            cells.add((int) Math.round(at));
        }
        final int headerY = middle.y() + HEAD + 2;
        final int firstRowY = headerY + ROW + 1;
        final int fileY = middle.bottom() - INSET - SMALL_LINE;
        int wanted = (BUTTONS - 1) * GAP;
        for (final int label : labels) {
            wanted += label + LABEL_ROOM;
        }
        final boolean oneRow = wanted <= tableWidth;
        final int buttonRows = oneRow ? 1 : TWO;
        final int below = 2 * GAP + buttonRows * BUTTON_HEIGHT + (buttonRows - 1) * GAP + GAP
                + HINT_LINES * SMALL_LINE + GAP;
        final int room = Math.max(1, (fileY - firstRowY - below) / ROW);
        final int rows = Math.clamp(keyframes, 1, room);
        final int buttonsY = firstRowY + rows * ROW + 2 * GAP;
        final List<StudioLayout.Rect> buttons = new ArrayList<>();
        if (oneRow) {
            int x = tableX;
            for (final int label : labels) {
                buttons.add(new StudioLayout.Rect(x, buttonsY, label + LABEL_ROOM, BUTTON_HEIGHT));
                x += label + LABEL_ROOM + GAP;
            }
        } else {
            final int half = (tableWidth - GAP) / TWO;
            for (int i = 0; i < BUTTONS; i++) {
                buttons.add(new StudioLayout.Rect(tableX + i % TWO * (half + GAP), buttonsY + i / TWO
                        * (BUTTON_HEIGHT + GAP), half, BUTTON_HEIGHT));
            }
        }
        final int hintY = buttons.get(BUTTONS - 1).bottom() + GAP;
        final StudioLayout.Rect map = columns.get(2);
        final StudioLayout.Rect list = columns.get(0);
        return new Laid(list, middle, map, List.copyOf(cells), headerY, firstRowY, rows, List.copyOf(buttons), hintY,
                fileY, new StudioLayout.Rect(map.x() + 2, map.y() + HEAD, map.width() - 4, map.height() - HEAD - 2),
                Math.max(1, (list.height() - HEAD - 2) / PATH_ROW));
    }

    /** The row of the path {@code index} rows down the list. */
    public static StudioLayout.Rect pathRow(final Laid laid, final int index) {
        return new StudioLayout.Rect(laid.list().x() + 2, laid.list().y() + HEAD + 2 + index * PATH_ROW,
                laid.list().width() - 4, PATH_ROW - 1);
    }

    /** The row of the keyframe {@code index} rows down the table, across it. */
    public static StudioLayout.Rect keyframeRow(final Laid laid, final int index) {
        return new StudioLayout.Rect(laid.cells().get(0) - 2, laid.firstRowY() + index * ROW - 1,
                laid.keyframes().width() - 2 * INSET + 4, ROW);
    }

    /** The cell of column {@code column} in the row of the keyframe {@code index} rows down. */
    public static StudioLayout.Rect cell(final Laid laid, final int index, final int column) {
        final int x = laid.cells().get(column);
        final int right = column + 1 < laid.cells().size() ? laid.cells().get(column + 1)
                : laid.keyframes().right() - INSET;
        return new StudioLayout.Rect(x, laid.firstRowY() + index * ROW - 1, right - x, ROW);
    }

    /**
     * The fit of the map: every place of {@code places}, each x and z blocks from the origin, and the origin itself,
     * inside {@code area} with room round them, as large as the room lets them be.
     */
    public static Fit fit(final StudioLayout.Rect area, final List<double[]> places) {
        double minX = 0;
        double maxX = 0;
        double minZ = 0;
        double maxZ = 0;
        for (final double[] place : places) {
            minX = Math.min(minX, place[0]);
            maxX = Math.max(maxX, place[0]);
            minZ = Math.min(minZ, place[1]);
            maxZ = Math.max(maxZ, place[1]);
        }
        final double spanX = Math.max(LEAST_SPAN, maxX - minX);
        final double spanZ = Math.max(LEAST_SPAN, maxZ - minZ);
        final double scale = Math.max(1.0e-3, Math.min((area.width() - 2.0 * MAP_MARGIN) / spanX,
                (area.height() - 2.0 * MAP_MARGIN) / spanZ));
        return new Fit(scale, (minX + maxX) / 2, (minZ + maxZ) / 2, area);
    }

    /** The page's parts, as the overlap check reads them, on a screen {@code width} by {@code height}. */
    public static GuiLayout layout(final int width, final int height, final List<Integer> tabs, final int keyframes,
                                   final List<Integer> labels) {
        final StudioLayout.Rect body = StudioLayout.of(width, height, tabs).body();
        final Laid laid = of(body, keyframes, labels);
        final GuiLayout layout = new GuiLayout(Math.max(StudioLayout.MIN_WIDTH, width), Math.max(
                StudioLayout.MIN_HEIGHT, height));
        for (int i = 0; i < laid.buttons().size(); i++) {
            final StudioLayout.Rect button = laid.buttons().get(i);
            layout.box("button " + i, button.x(), button.y(), button.width(), button.height());
        }
        for (int i = 0; i < laid.rows(); i++) {
            final StudioLayout.Rect row = keyframeRow(laid, i);
            layout.box("keyframe " + i, row.x(), row.y(), row.width(), row.height());
        }
        for (int i = 0; i < laid.pathRows(); i++) {
            final StudioLayout.Rect row = pathRow(laid, i);
            layout.box("path " + i, row.x(), row.y(), row.width(), row.height());
        }
        layout.box("map", laid.mapArea().x(), laid.mapArea().y(), laid.mapArea().width(), laid.mapArea().height());
        layout.box("hint", laid.keyframes().x() + INSET, laid.hintY(), laid.keyframes().width() - 2 * INSET,
                HINT_LINES * SMALL_LINE);
        layout.box("file", laid.keyframes().x() + INSET, laid.fileY(), laid.keyframes().width() - 2 * INSET,
                SMALL_LINE);
        return layout;
    }

    /**
     * The page laid out.
     *
     * @param list      the paths' column
     * @param keyframes the keyframes' panel
     * @param map       the map's panel
     * @param cells     where each column of the table starts, across
     * @param headerY   where the table's heading row stands
     * @param firstRowY where its first keyframe's row stands
     * @param rows      how many keyframes' rows it shows
     * @param buttons   Fly and record, Add here, Play and Save to file
     * @param hintY     where the hint starts
     * @param fileY     where the file is written
     * @param mapArea   the map within its panel
     * @param pathRows  how many paths' rows the list shows
     */
    public record Laid(StudioLayout.Rect list, StudioLayout.Rect keyframes, StudioLayout.Rect map, List<Integer> cells,
                       int headerY, int firstRowY, int rows, List<StudioLayout.Rect> buttons, int hintY, int fileY,
                       StudioLayout.Rect mapArea, int pathRows) {
    }

    /**
     * How the map is fitted: its pixels a block, the place in blocks at its middle, and its room.
     *
     * @param scale   pixels a block
     * @param centreX the place at the map's middle, blocks across from the origin
     * @param centreZ and blocks along, south
     * @param area    the map's room
     */
    public record Fit(double scale, double centreX, double centreZ, StudioLayout.Rect area) {

        /** Where on the screen a place {@code x} and {@code z} blocks from the origin falls: north up, east right. */
        public double[] at(final double x, final double z) {
            return new double[] {this.area.x() + this.area.width() / 2.0 + (x - this.centreX) * this.scale,
                    this.area.y() + this.area.height() / 2.0 + (z - this.centreZ) * this.scale};
        }
    }
}
