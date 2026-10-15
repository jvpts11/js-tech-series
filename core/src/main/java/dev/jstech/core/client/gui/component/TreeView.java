/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.component;

import dev.jstech.core.client.gui.logic.TreeModel;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/**
 * A tree: its nodes in rows, each child under its parent with the dotted guides joining them, a node that holds others
 * opened and closed by the arrow beside it, its item's picture and its count. A click chooses a node; the wheel
 * scrolls.
 */
public final class TreeView extends UiComponent {

    private final TreeModel model;
    private final Function<String, ItemStack> icon;
    private final Consumer<String> onSelect;
    private int scroll;

    /** How tall a row is, its item's picture included; how far a level steps in. */
    public static final int ROW = 19;
    public static final int INDENT = 12;
    /** How far the arrow of a node that holds others takes. */
    public static final int ARROW = 9;
    private static final int ICON = 19;
    private static final int GUIDE_STEP = 2;

    /**
     * @param model    the tree
     * @param icon     the item drawn beside a node, by its id, or an empty stack for none
     * @param onSelect what is done when a node is chosen
     */
    public TreeView(final TreeModel model, final Function<String, ItemStack> icon, final Consumer<String> onSelect) {
        this.model = Objects.requireNonNull(model, "model");
        this.icon = Objects.requireNonNull(icon, "icon");
        this.onSelect = Objects.requireNonNull(onSelect, "onSelect");
    }

    @Override
    public boolean focusable() {
        return true;
    }

    @Override
    public void render(final GuiGraphics g, final UiContext ctx) {
        ctx.skin().panel(g, x(), y(), width(), height());
        final List<TreeModel.Row> rows = model.rows();
        scroll = Math.max(0, Math.min(scroll, Math.max(0, rows.size() - rowsShown())));
        Draw.pushScissor(g, x() + 1, y() + 1, right() - 1, bottom() - 1);
        try {
            for (int i = scroll; i < rows.size() && i < scroll + rowsShown() + 1; i++) {
                drawRow(g, ctx, rows.get(i), y() + 2 + (i - scroll) * ROW);
            }
        } finally {
            Draw.popScissor(g);
        }
    }

    @Override
    public boolean mouseClicked(final double mx, final double my, final int button) {
        final int index = scroll + (int) ((my - y() - 2) / ROW);
        final List<TreeModel.Row> rows = model.rows();
        if (button != 0 || index < 0 || index >= rows.size()) {
            return false;
        }
        final TreeModel.Row row = rows.get(index);
        final int arrowX = x() + 4 + row.depth() * INDENT;
        if (!row.node().children().isEmpty() && mx >= arrowX && mx < arrowX + ARROW) {
            model.toggle(row.node().id());
        } else {
            model.select(row.node().id());
            onSelect.accept(row.node().id());
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(final double mx, final double my, final double delta) {
        scroll = Math.max(0, scroll - (int) Math.signum(delta));
        return true;
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        final List<TreeModel.Row> rows = model.rows();
        int at = -1;
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).node().id().equals(model.selected())) {
                at = i;
            }
        }
        final int next = switch (key) {
            case GLFW.GLFW_KEY_DOWN -> Math.min(rows.size() - 1, at + 1);
            case GLFW.GLFW_KEY_UP -> Math.max(0, at - 1);
            default -> -1;
        };
        if (next >= 0 && next < rows.size()) {
            model.select(rows.get(next).node().id());
            onSelect.accept(rows.get(next).node().id());
            return true;
        }
        if ((key == GLFW.GLFW_KEY_RIGHT || key == GLFW.GLFW_KEY_LEFT) && at >= 0) {
            final String id = rows.get(at).node().id();
            if (model.isOpen(id) == (key == GLFW.GLFW_KEY_LEFT)) {
                model.toggle(id);
            }
            return true;
        }
        return false;
    }

    private int rowsShown() {
        return Math.max(1, (height() - 4) / ROW);
    }

    private void drawRow(final GuiGraphics g, final UiContext ctx, final TreeModel.Row row, final int y) {
        final boolean selected = row.node().id().equals(model.selected());
        ctx.skin().listRow(g, x() + 2, y - 1, width() - 4, ROW, ctx.over(x(), y - 1, width(), ROW), selected);
        final int x = x() + 4 + row.depth() * INDENT;
        drawGuides(g, ctx, row, x, y);
        int tx = x;
        if (!row.node().children().isEmpty()) {
            drawArrow(g, ctx, x, y, model.isOpen(row.node().id()));
            tx += ARROW;
        }
        final ItemStack stack = icon.apply(row.node().id());
        if (!stack.isEmpty()) {
            g.renderItem(stack, tx, y);
            tx += ICON;
        }
        ctx.skin().drawText(g, ctx.font(), row.node().label(), tx, y + 5, ctx.skin().listRowText(selected));
        if (row.node().count() >= 0) {
            final String count = String.valueOf(row.node().count());
            ctx.skin().drawText(g, ctx.font(), count, right() - 4 - ctx.skin().textWidth(ctx.font(), count), y + 5,
                    ctx.skin().dim());
        }
    }

    /* The dotted guides: down beside each level that runs on, and across into this row. */
    private void drawGuides(final GuiGraphics g, final UiContext ctx, final TreeModel.Row row, final int x,
                            final int y) {
        if (row.depth() == 0) {
            return;
        }
        final int guide = ctx.skin().dim();
        for (int level = 1; level < row.runOn().size(); level++) {
            if (row.runOn().get(level)) {
                final int gx = x() + 4 + level * INDENT - INDENT + 3;
                dotsDown(g, gx, y - 1, y + ROW - 1, guide);
            }
        }
        final int gx = x - INDENT + 3;
        dotsDown(g, gx, y - 1, row.last() ? y + 9 : y + ROW - 1, guide);
        for (int dx = gx; dx < x - 1; dx += GUIDE_STEP) {
            g.fill(dx, y + 8, dx + 1, y + 9, guide);
        }
    }

    private static void dotsDown(final GuiGraphics g, final int x, final int from, final int to, final int colour) {
        for (int dy = from; dy < to; dy += GUIDE_STEP) {
            g.fill(x, dy, x + 1, dy + 1, colour);
        }
    }

    private static void drawArrow(final GuiGraphics g, final UiContext ctx, final int x, final int y,
                                  final boolean open) {
        final int ink = ctx.skin().text();
        for (int k = 0; k < 3; k++) {
            if (open) {
                g.fill(x + k, y + 7 + k, x + 6 - k, y + 8 + k, ink);
            } else {
                g.fill(x + 1 + k, y + 5 + k, x + 2 + k, y + 12 - k, ink);
            }
        }
    }
}
