/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.flex;

import dev.jstech.core.gui.layout.GuiLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A box where the layout put it: its rectangle in the screen's space and those of what it holds. {@link #guiLayout}
 * hands the result to the overlap check, so a test checks the layout the game draws, not one written beside it.
 *
 * @param box         the box
 * @param x           its left edge
 * @param y           its top edge
 * @param width       its width, padding included
 * @param height      its height, padding included
 * @param children    what it holds, placed
 * @param overflowing whether what it holds needs more room than it was given, even at the smallest they may be
 */
public record Placed(FlexBox box, int x, int y, int width, int height, List<Placed> children, boolean overflowing) {

    public Placed {
        children = List.copyOf(children);
    }

    /** The box's name. */
    public String name() {
        return box.name();
    }

    /** The first past its right edge. */
    public int right() {
        return x + width;
    }

    /** The first past its bottom edge. */
    public int bottom() {
        return y + height;
    }

    /** The box of that name, this one or any inside it, the first met going down. */
    public Optional<Placed> find(final String name) {
        if (box.name().equals(name)) {
            return Optional.of(this);
        }
        for (final Placed child : children) {
            final Optional<Placed> found = child.find(name);
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    /** This box and every box inside it, parents before their children. */
    public List<Placed> all() {
        final List<Placed> out = new ArrayList<>();
        collect(this, out);
        return out;
    }

    /** The names of the boxes whose contents needed more room than they had. */
    public List<String> overflows() {
        final List<String> out = new ArrayList<>();
        for (final Placed placed : all()) {
            if (placed.overflowing()) {
                out.add(placed.name());
            }
        }
        return out;
    }

    /**
     * The laid-out screen as the overlap check reads it, on a panel {@code panelWidth} by {@code panelHeight}: every
     * solid box as a solid element and every text as a line, the boxes that only hold others left out.
     */
    public GuiLayout guiLayout(final int panelWidth, final int panelHeight) {
        final GuiLayout layout = new GuiLayout(panelWidth, panelHeight);
        for (final Placed placed : all()) {
            switch (placed.box().role()) {
                case SOLID -> layout.box(placed.name(), placed.x(), placed.y(), placed.width(), placed.height());
                case TEXT -> layout.textBox(placed.name(), placed.x(), placed.y(), placed.width(), placed.height());
                case NONE -> {
                }
            }
        }
        return layout;
    }

    private static void collect(final Placed placed, final List<Placed> out) {
        out.add(placed);
        for (final Placed child : placed.children()) {
            collect(child, out);
        }
    }
}
