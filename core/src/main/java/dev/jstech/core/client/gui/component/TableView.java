/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.component;

import dev.jstech.core.client.gui.logic.TableModel;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

/**
 * A table, as the game's statistics are shown: a head of column titles, a click on one sorting by it and a second the
 * other way round, the way it sorts marked; then a row for each thing, its item's picture first, the rows told apart
 * by their shade. The wheel scrolls; a click chooses a row.
 *
 * @param <R> a row
 */
public final class TableView<R> extends UiComponent {

    private final TableModel<R> model;
    private final Function<R, ItemStack> icon;
    private final Consumer<R> onSelect;
    private int scroll;

    /** How tall the head is, and a row. */
    public static final int HEAD = 14;
    public static final int ROW = 16;
    private static final int ICON = 20;

    /**
     * @param model    the table
     * @param icon     the item drawn at a row's start, or an empty stack for none
     * @param onSelect what is done when a row is chosen
     */
    public TableView(final TableModel<R> model, final Function<R, ItemStack> icon, final Consumer<R> onSelect) {
        this.model = Objects.requireNonNull(model, "model");
        this.icon = Objects.requireNonNull(icon, "icon");
        this.onSelect = Objects.requireNonNull(onSelect, "onSelect");
    }

    @Override
    public void render(final GuiGraphics g, final UiContext ctx) {
        ctx.skin().panel(g, x(), y(), width(), height());
        drawHead(g, ctx);
        final List<R> rows = model.shown();
        scroll = Math.max(0, Math.min(scroll, Math.max(0, rows.size() - rowsShown())));
        Draw.pushScissor(g, x() + 1, y() + HEAD, right() - 1, bottom() - 1);
        try {
            for (int i = scroll; i < rows.size() && i < scroll + rowsShown() + 1; i++) {
                drawRow(g, ctx, rows.get(i), y() + HEAD + 2 + (i - scroll) * ROW, (i - scroll) % 2 == 0);
            }
        } finally {
            Draw.popScissor(g);
        }
    }

    @Override
    public boolean mouseClicked(final double mx, final double my, final int button) {
        if (button != 0) {
            return false;
        }
        if (my < y() + HEAD) {
            model.sortBy(columnAt(mx));
            return true;
        }
        final List<R> rows = model.shown();
        final int index = scroll + (int) ((my - y() - HEAD - 2) / ROW);
        if (index >= 0 && index < rows.size()) {
            onSelect.accept(rows.get(index));
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(final double mx, final double my, final double delta) {
        scroll = Math.max(0, scroll - (int) Math.signum(delta));
        return true;
    }

    private int rowsShown() {
        return Math.max(1, (height() - HEAD - 2) / ROW);
    }

    /* The column whose head holds the pointer at {@code mx}, or -1. */
    private int columnAt(final double mx) {
        int left = x() + 4;
        final List<TableModel.Column<R>> columns = model.columns();
        for (int i = 0; i < columns.size(); i++) {
            if (mx >= left && mx < left + columns.get(i).width()) {
                return i;
            }
            left += columns.get(i).width();
        }
        return -1;
    }

    private void drawHead(final GuiGraphics g, final UiContext ctx) {
        int left = x() + 4;
        final List<TableModel.Column<R>> columns = model.columns();
        for (int i = 0; i < columns.size(); i++) {
            final TableModel.Column<R> column = columns.get(i);
            final int tw = ctx.skin().textWidth(ctx.font(), column.title());
            final int tx = column.right() ? left + column.width() - tw - 2 : left;
            ctx.skin().drawText(g, ctx.font(), column.title(), tx, y() + 4, ctx.skin().text());
            if (model.sortColumn() == i) {
                caret(g, column.right() ? tx - 8 : tx + tw + 3, y() + 6, model.descending(), ctx.skin().accent());
            }
            left += column.width();
        }
        ctx.skin().separator(g, x() + 2, y() + HEAD - 2, width() - 4);
    }

    private void drawRow(final GuiGraphics g, final UiContext ctx, final R row, final int y, final boolean even) {
        final int ink = even ? ctx.skin().text() : ctx.skin().dim();
        int left = x() + 4;
        final ItemStack stack = icon.apply(row);
        final List<TableModel.Column<R>> columns = model.columns();
        for (int i = 0; i < columns.size(); i++) {
            final TableModel.Column<R> column = columns.get(i);
            int cellX = left;
            if (i == 0 && !stack.isEmpty()) {
                g.renderItem(stack, left, y);
                cellX += ICON;
            }
            final String text = column.text().apply(row);
            final int tw = ctx.skin().textWidth(ctx.font(), text);
            final int tx = column.right() ? left + column.width() - tw - 2 : cellX;
            ctx.skin().drawText(g, ctx.font(), text, tx, y + 4, ink);
            left += column.width();
        }
    }

    /* The mark of the way a column sorts: pointing down from the largest, up from the smallest. */
    private static void caret(final GuiGraphics g, final int x, final int y, final boolean down, final int colour) {
        for (int i = 0; i < 3; i++) {
            final int row = down ? y + i : y + 2 - i;
            g.fill(x + i, row, x + 5 - i, row + 1, colour);
        }
    }
}
