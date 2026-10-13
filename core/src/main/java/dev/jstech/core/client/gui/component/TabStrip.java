/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.component;

import net.minecraft.client.gui.GuiGraphics;

import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import java.util.function.Supplier;

/**
 * A row of tabs, one of them selected, with the skin's separator under the row. The tabs share the width
 * equally, or each takes what its label needs when {@link #fitToLabels} is set.
 */
public final class TabStrip extends UiComponent {

    private final Supplier<List<String>> labels;
    private int selected;
    private IntConsumer onSelect = i -> { };
    private boolean underline = true;
    private int labelPadding = -1;
    /** The widths of the tabs as last drawn; equal shares until the first frame measures the labels. */
    private int[] widths = new int[0];
    /* Which tabs cannot be chosen now, and the word drawn small beside their label to say why. */
    private IntPredicate disabled = i -> false;
    private Supplier<String> disabledNote = () -> "";

    /** A strip whose tabs never change: the sections of a screen. */
    public TabStrip(final List<String> labels) {
        final List<String> fixed = List.copyOf(labels);
        this.labels = () -> fixed;
    }

    /**
     * A strip whose tabs come and go: the files an editor has open. The labels are read as they are
     * drawn, so opening or closing one needs nothing said here.
     */
    public TabStrip(final Supplier<List<String>> labels) {
        this.labels = labels;
    }

    private List<String> labels() {
        final List<String> current = this.labels.get();
        return current == null ? List.of() : current;
    }

    /** Gives each tab the width of its label plus {@code padding}, instead of an equal share of the strip. */
    public TabStrip fitToLabels(final int padding) {
        labelPadding = Math.max(0, padding);
        return this;
    }

    public TabStrip setOnSelect(final IntConsumer action) {
        onSelect = action;
        return this;
    }

    /** Whether to draw the separator line along the bottom of the strip. */
    public TabStrip setUnderline(final boolean value) {
        underline = value;
        return this;
    }

    /**
     * Makes the tabs {@code rule} names unavailable: they draw dim, with {@code note} after their label (what is
     * missing), and a click on one selects nothing.
     */
    public TabStrip setDisabled(final IntPredicate rule, final Supplier<String> note) {
        disabled = rule;
        disabledNote = note;
        return this;
    }

    /** Room for the close mark at the right of a tab that can be closed. */
    private static final int CLOSE_W = 9;
    /** The least padding a fitted tab keeps when the labels are squeezed to fit the strip. */
    private static final int MIN_FITTED_PADDING = 4;
    private IntConsumer onClose;

    /**
     * Gives every tab a close mark at its right, and says what closing one does.
     *
     * <p>The mark takes the click that lands on it, and so does the middle button anywhere on the tab,
     * the way an editor's tabs close; neither selects the tab first.
     */
    public TabStrip setCloseable(final IntConsumer action) {
        onClose = action;
        return this;
    }

    public int selected() {
        return selected;
    }

    /** Selects a tab without firing the callback, as a state refresh from outside does. */
    public TabStrip setSelected(final int index) {
        selected = Math.max(0, Math.min(labels().size() - 1, index));
        return this;
    }

    public int count() {
        return labels().size();
    }

    /** The width of tab {@code index} as laid out now. */
    private int tabWidth(final int index) {
        if (labelPadding >= 0 && index < widths.length) {
            return widths[index];
        }
        return Math.max(1, width() / Math.max(1, labels().size()));
    }

    /** The first tab laid out from the strip's right end; every tab from it on sits there, in order. */
    private int trailingFrom = Integer.MAX_VALUE;

    /**
     * Lays the tabs from {@code index} on at the right end of the strip instead of after the others, the
     * way a favourites tab sits apart from a screen's sections. Tabs before it stay at the left.
     */
    public TabStrip setTrailing(final int index) {
        trailingFrom = index;
        return this;
    }

    /** The left edge of tab {@code index} as laid out now. */
    private int tabX(final int index) {
        final int count = labels().size();
        if (index >= trailingFrom && trailingFrom < count) {
            int tx = x() + width();
            for (int i = count - 1; i >= index; i--) {
                tx -= tabWidth(i);
            }
            return tx;
        }
        int tx = x();
        for (int i = 0; i < index; i++) {
            tx += tabWidth(i);
        }
        return tx;
    }

    /** The centre of tab {@code index}, where a test clicks it. */
    public int[] tabCenter(final int index) {
        return new int[] {tabX(index) + tabWidth(index) / 2, y() + height() / 2};
    }

    @Override
    public void render(final GuiGraphics g, final UiContext ctx) {
        final List<String> current = labels();
        final int close = onClose == null ? 0 : CLOSE_W;
        if (labelPadding >= 0) {
            final int[] measured = new int[current.size()];
            int words = 0;
            for (int i = 0; i < current.size(); i++) {
                measured[i] = ctx.font().width(current.get(i)) + noteWidth(ctx, i);
                words += measured[i] + close;
            }
            /*
             * Labels that would run past the strip with their padding give up padding first, evenly, so a long
             * language or a narrow window still shows every tab rather than pushing the last ones off the end.
             */
            final int padding = current.isEmpty() || words + labelPadding * current.size() <= width()
                    ? labelPadding
                    : Math.max(MIN_FITTED_PADDING, (width() - words) / current.size());
            for (int i = 0; i < current.size(); i++) {
                measured[i] += padding + close;
            }
            widths = measured;
        }
        for (int i = 0; i < current.size(); i++) {
            final int tx = tabX(i);
            final int tw = tabWidth(i);
            if (disabled.test(i)) {
                renderDisabled(g, ctx, tx, tw - close, current.get(i));
                continue;
            }
            ctx.skin().tab(g, ctx.font(), tx, y(), tw - close, height(), current.get(i), i == selected);
            if (close > 0) {
                // The mark is part of the tab, drawn on the same ground, lit when the mouse is over it.
                final boolean over = ctx.over(tx + tw - close, y(), close, height());
                Draw.text(g, ctx.font(), "x", tx + tw - close + 2, y() + (height() - 7) / 2,
                        over ? ctx.skin().text() : ctx.skin().dim());
            }
        }
        if (underline) {
            g.fill(x(), y() + height() - 1, x() + width(), y() + height(), ctx.skin().edge());
        }
    }

    @Override
    public boolean mouseClicked(final double mx, final double my, final int button) {
        final List<String> current = labels();
        if (current.isEmpty()) {
            // A strip whose tabs come and go can be empty, and an empty strip has nothing to select.
            return true;
        }
        int index = -1;
        for (int i = 0; i < current.size(); i++) {
            final int tx = tabX(i);
            if (mx >= tx && mx < tx + tabWidth(i)) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            /*
             * Past the last tab (or between the leading tabs and a trailing one) is the strip's empty end, and
             * a click there is a click on nothing: it used to count as the last tab's close mark, which shut
             * the tabs one by one.
             */
            return true;
        }
        if (onClose != null) {
            final boolean onMark = mx >= tabX(index) + tabWidth(index) - CLOSE_W;
            if (button == 2 || (button == 0 && onMark)) {
                onClose.accept(index);
                return true;
            }
        }
        if (disabled.test(index)) {
            return true;
        }
        if (index != selected) {
            selected = index;
            onSelect.accept(index);
        }
        return true;
    }

    /* The room the note after a disabled tab's label takes, with the space before it. */
    private int noteWidth(final UiContext ctx, final int index) {
        final String note = disabledNote.get();
        return disabled.test(index) && note != null && !note.isEmpty() ? ctx.font().width(" " + note) : 0;
    }

    /* A tab that cannot be chosen: its ground unselected, its label dim and the note after it. */
    private void renderDisabled(final GuiGraphics g, final UiContext ctx, final int tx, final int tw,
                                final String label) {
        ctx.skin().tab(g, ctx.font(), tx, y(), tw, height(), "", false);
        final String note = disabledNote.get() == null ? "" : disabledNote.get();
        final String shown = note.isEmpty() ? label : label + " " + note;
        final int textX = tx + Math.max(2, (tw - ctx.font().width(shown)) / 2);
        Draw.text(g, ctx.font(), shown, textX, y() + (height() - 7) / 2, ctx.skin().dim());
    }
}
