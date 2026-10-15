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
import java.util.Objects;
import org.jetbrains.annotations.Nullable;

/**
 * One box of a screen's layout: what it holds and how, the room around it, and how big it may be. Written as the
 * box's kind followed by what it changes, the way the layout reads:
 *
 * <pre>{@code
 * FlexBox root = FlexBox.column("root").padding(Insets.of(6)).gap(4)
 *         .add(FlexBox.leaf("title", text).role(Role.TEXT))
 *         .add(FlexBox.row("slots").gap(2).add(slot("in")).add(FlexBox.leaf("arrow", arrow).grow(1)).add(slot("out")));
 * Placed placed = FlexLayout.layout(root, 176, 90);
 * }</pre>
 *
 * <p>Sizes are in pixels. A box with no size of its own is as big as what it holds; {@link #grow} hands it a share of
 * the room its row or column has left; the smallest and largest sizes hold it either way. Pure, with no game types,
 * so a layout is checked in a unit test exactly as the game lays it out.
 */
public final class FlexBox {

    private final String name;
    private final Flow flow;
    private final List<FlexBox> children = new ArrayList<>();
    private IMeasure measure = IMeasure.NOTHING;
    private Role role = Role.NONE;
    private Insets padding = Insets.NONE;
    private Insets margin = Insets.NONE;
    private int gap;
    private int width = UNSET;
    private int height = UNSET;
    private int minWidth;
    private int minHeight;
    private int maxWidth = UNBOUNDED;
    private int maxHeight = UNBOUNDED;
    private int grow;
    private int shrink = 1;
    private Align align = Align.STRETCH;
    @Nullable
    private Align alignSelf;
    private Justify justify = Justify.START;
    private boolean wrap;
    private List<Track> columns = List.of();
    private int span = 1;

    /** A size that is not given: the box is as big as what it holds. */
    public static final int UNSET = -1;
    /** No largest size. */
    public static final int UNBOUNDED = Integer.MAX_VALUE / 4;

    private FlexBox(final String name, final Flow flow) {
        this.name = Objects.requireNonNull(name, "name");
        this.flow = Objects.requireNonNull(flow, "flow");
    }

    /** A box with nothing inside, as big as {@code measure} says what it shows wants to be. */
    public static FlexBox leaf(final String name, final IMeasure measure) {
        return new FlexBox(name, Flow.LEAF).measure(measure);
    }

    /** A box laying out what it holds left to right. */
    public static FlexBox row(final String name) {
        return new FlexBox(name, Flow.ROW);
    }

    /** A box laying out what it holds top to bottom. */
    public static FlexBox column(final String name) {
        return new FlexBox(name, Flow.COLUMN);
    }

    /** A box laying out what it holds in the cells of a grid of these columns, row after row. */
    public static FlexBox grid(final String name, final List<Track> columns) {
        if (columns.isEmpty()) {
            throw new IllegalArgumentException("the grid '" + name + "' has no columns");
        }
        final FlexBox grid = new FlexBox(name, Flow.GRID);
        grid.columns = List.copyOf(columns);
        return grid;
    }

    /** A box laying what it holds over each other, the first at the back. */
    public static FlexBox stack(final String name) {
        return new FlexBox(name, Flow.STACK);
    }

    /** Adds a box inside this one, after those already there. */
    public FlexBox add(final FlexBox child) {
        if (flow == Flow.LEAF) {
            throw new IllegalStateException("'" + name + "' is a leaf and holds nothing; '" + child.name
                    + "' cannot go inside it");
        }
        children.add(Objects.requireNonNull(child, "child"));
        return this;
    }

    /** How big what this leaf shows wants to be. */
    public FlexBox measure(final IMeasure value) {
        measure = Objects.requireNonNull(value, "measure");
        return this;
    }

    /** What the box is to the overlap check. */
    public FlexBox role(final Role value) {
        role = Objects.requireNonNull(value, "role");
        return this;
    }

    /** The room kept inside the box, around what it holds. */
    public FlexBox padding(final Insets value) {
        padding = Objects.requireNonNull(value, "padding");
        return this;
    }

    /** The room kept outside the box, between it and its neighbours. */
    public FlexBox margin(final Insets value) {
        margin = Objects.requireNonNull(value, "margin");
        return this;
    }

    /** The room between two of what the box holds; in a grid, between its rows and its columns. */
    public FlexBox gap(final int pixels) {
        gap = Math.max(0, pixels);
        return this;
    }

    /** The box's own width, padding included. */
    public FlexBox width(final int pixels) {
        width = pixels < 0 ? UNSET : pixels;
        return this;
    }

    /** The box's own height, padding included. */
    public FlexBox height(final int pixels) {
        height = pixels < 0 ? UNSET : pixels;
        return this;
    }

    /** The box's own width and height. */
    public FlexBox size(final int w, final int h) {
        return width(w).height(h);
    }

    /** The narrowest the box may be. */
    public FlexBox minWidth(final int pixels) {
        minWidth = Math.max(0, pixels);
        return this;
    }

    /** The shortest the box may be. */
    public FlexBox minHeight(final int pixels) {
        minHeight = Math.max(0, pixels);
        return this;
    }

    /** The widest the box may be. */
    public FlexBox maxWidth(final int pixels) {
        maxWidth = Math.max(0, pixels);
        return this;
    }

    /** The tallest the box may be. */
    public FlexBox maxHeight(final int pixels) {
        maxHeight = Math.max(0, pixels);
        return this;
    }

    /** The share of its row's or column's room left over that the box takes; 0 takes none. */
    public FlexBox grow(final int share) {
        grow = Math.max(0, share);
        return this;
    }

    /** How readily the box gives up room when its row or column is too narrow; 0 never does. */
    public FlexBox shrink(final int share) {
        shrink = Math.max(0, share);
        return this;
    }

    /** Where what the box holds stands across its rows or columns, unless one says otherwise. */
    public FlexBox align(final Align value) {
        align = Objects.requireNonNull(value, "align");
        return this;
    }

    /** Where this box stands across its parent's row or column, whatever the parent says. */
    public FlexBox alignSelf(@Nullable final Align value) {
        alignSelf = value;
        return this;
    }

    /** Where the room nobody grows into goes. */
    public FlexBox justify(final Justify value) {
        justify = Objects.requireNonNull(value, "justify");
        return this;
    }

    /** Whether a row that runs out of room goes on in a new line under it. */
    public FlexBox wrap(final boolean value) {
        wrap = value;
        return this;
    }

    /** How many grid columns the box takes. */
    public FlexBox span(final int columnsTaken) {
        span = Math.max(1, columnsTaken);
        return this;
    }

    public String name() {
        return name;
    }

    public Flow flow() {
        return flow;
    }

    public List<FlexBox> children() {
        return List.copyOf(children);
    }

    public IMeasure measure() {
        return measure;
    }

    public Role role() {
        return role;
    }

    public Insets padding() {
        return padding;
    }

    public Insets margin() {
        return margin;
    }

    public int gap() {
        return gap;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int minWidth() {
        return minWidth;
    }

    public int minHeight() {
        return minHeight;
    }

    public int maxWidth() {
        return maxWidth;
    }

    public int maxHeight() {
        return maxHeight;
    }

    public int grow() {
        return grow;
    }

    public int shrink() {
        return shrink;
    }

    public Align align() {
        return align;
    }

    @Nullable
    public Align alignSelf() {
        return alignSelf;
    }

    public Justify justify() {
        return justify;
    }

    public boolean wrap() {
        return wrap;
    }

    public List<Track> columns() {
        return columns;
    }

    public int span() {
        return span;
    }

    @Override
    public String toString() {
        return name + " (" + flow + ")";
    }

    /* The children as they stand, for the layout to read without a copy each pass. */
    List<FlexBox> held() {
        return children;
    }
}
