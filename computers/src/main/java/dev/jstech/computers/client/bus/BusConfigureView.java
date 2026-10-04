/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.bus;

import com.mojang.blaze3d.platform.InputConstants;
import dev.jstech.computers.block.part.AbstractBusPart;
import dev.jstech.computers.bus.BusAbilities;
import dev.jstech.computers.bus.BusActivity;
import dev.jstech.computers.bus.BusCondition;
import dev.jstech.computers.bus.BusScript;
import dev.jstech.computers.bus.BusSettings;
import dev.jstech.computers.gui.layout.BusLayout;
import dev.jstech.computers.menu.AbstractBusMenu;
import dev.jstech.computers.operation.payload.BusEditPayload;
import dev.jstech.computers.operation.payload.BusStatePayload;
import dev.jstech.core.client.gui.theme.JsTechTheme;
import dev.jstech.core.id.StableIds;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The Configure tab of a bus's window: the rows of what the bus's era can be set to, one under the other, scrolling
 * above the inventory when they do not all fit. A change goes to the server as it is made, and the row shows it when
 * the bus's new state comes back. A setting a program set carries the program's mark until a hand changes it.
 */
final class BusConfigureView {

    private final AbstractBusMenu menu;
    private final Font font;
    private final Consumer<BusEditPayload> send;
    private final EditBox tagField;
    private final EditBox conditionField;
    private List<BusLayout.Row> rows = List.of();
    private int contentHeight;
    private int scroll;
    private boolean editingTag;
    private boolean editingCondition;
    private BusCondition.Kind editKind = BusCondition.Kind.STOCK;
    private long editBelow = DEFAULT_BELOW;
    private int editFrom = DEFAULT_FROM;
    private int editTo = DEFAULT_TO;
    private boolean shift;

    private static final int STEP = 1;
    private static final int SHIFT_STEP = 16;
    private static final int BELOW_SHIFT_STEP = 64;
    private static final long DEFAULT_BELOW = 64L;
    private static final int DEFAULT_FROM = 18;
    private static final int DEFAULT_TO = 6;
    private static final int HOURS = 24;
    private static final int SCROLL_STEP = BusLayout.ROW;

    BusConfigureView(final AbstractBusMenu menu, final Font font, final Consumer<BusEditPayload> send) {
        this.menu = menu;
        this.font = font;
        this.send = send;
        this.tagField = field(BusTexts.TAG_FIELD, BusEditPayload.MAX_TEXT);
        this.conditionField = field(BusTexts.STOCK_FIELD, BusEditPayload.MAX_TEXT);
    }

    /** The text fields this tab types into, which the screen adds as its widgets. */
    List<EditBox> widgets() {
        return List.of(tagField, conditionField);
    }

    /** Whether one of its fields has the keyboard. */
    boolean typing() {
        return tagField.isFocused() && tagField.isVisible() || conditionField.isFocused() && conditionField.isVisible();
    }

    /** Hides its fields, when another tab is shown. */
    void hideFields() {
        tagField.setVisible(false);
        conditionField.setVisible(false);
    }

    void render(final GuiGraphics g, final int left, final int top, final int mx, final int my, final boolean held) {
        this.shift = held;
        layOut();
        final int viewTop = top + BusLayout.CONFIGURE_VIEW_Y;
        final int viewH = viewHeight();
        scroll = Math.max(0, Math.min(scroll, contentHeight - viewH));
        final boolean pointing = BusDraw.inside(mx, my, left + BusLayout.LABEL_X, viewTop, BusLayout.ROW_W, viewH);
        g.enableScissor(left + BusLayout.LABEL_X, viewTop, left + BusLayout.RIGHT, viewTop + viewH);
        for (final BusLayout.Row row : rows) {
            final int y = viewTop + row.y() - scroll;
            if (y + row.height() > viewTop && y < viewTop + viewH) {
                drawRow(g, row, left, y, pointing ? mx - left : -1, pointing ? my - y : -1);
            }
        }
        g.disableScissor();
        BusDraw.scrollbar(g, left + BusLayout.SCROLL_X, viewTop, viewH, scroll, contentHeight);
        placeFields(left, viewTop, viewH);
    }

    boolean click(final double mx, final double my, final int left, final int top) {
        final int viewTop = top + BusLayout.CONFIGURE_VIEW_Y;
        if (!BusDraw.inside(mx, my, left + BusLayout.LABEL_X, viewTop, BusLayout.ROW_W, viewHeight())) {
            return false;
        }
        final int at = (int) my - viewTop + scroll;
        for (final BusLayout.Row row : rows) {
            if (at >= row.y() && at < row.y() + row.height()) {
                return clickRow(row, (int) mx - left, at - row.y());
            }
        }
        return false;
    }

    boolean scrolled(final double mx, final double my, final double delta, final int left, final int top) {
        final int viewTop = top + BusLayout.CONFIGURE_VIEW_Y;
        if (!BusDraw.inside(mx, my, left + BusLayout.LABEL_X, viewTop, BusLayout.ROW_W + 6, viewHeight())
                || contentHeight <= viewHeight()) {
            return false;
        }
        scroll = Math.max(0, Math.min(contentHeight - viewHeight(), scroll - (int) Math.signum(delta) * SCROLL_STEP));
        return true;
    }

    /** Enter adds what is typed, escape gives up on it; whether the key was the field's. */
    boolean keyPressed(final int key) {
        if (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER) {
            if (tagField.isFocused() && editingTag) {
                addTag();
                return true;
            }
            if (conditionField.isFocused() && editingCondition) {
                addCondition();
                return true;
            }
        }
        if (key == InputConstants.KEY_ESCAPE && typing()) {
            editingTag = false;
            editingCondition = false;
            tagField.setFocused(false);
            conditionField.setFocused(false);
            return true;
        }
        return false;
    }

    /** What the pointer is over says, or nothing. */
    List<Component> tooltip(final int mx, final int my, final int left, final int top) {
        final int viewTop = top + BusLayout.CONFIGURE_VIEW_Y;
        if (!BusDraw.inside(mx, my, left + BusLayout.LABEL_X, viewTop, BusLayout.ROW_W, viewHeight())) {
            return List.of();
        }
        final int at = my - viewTop + scroll;
        for (final BusLayout.Row row : rows) {
            if (at >= row.y() && at < row.y() + row.height()) {
                return rowTooltip(row, mx - left, at - row.y());
            }
        }
        return List.of();
    }

    /** How tall the rows' area is: fixed for the bus, so the inventory under it stays put. */
    int viewHeight() {
        return BusLayout.configureView(abilities(), menu.window());
    }

    /* The rows as the bus is set now, and how tall they are together. */
    private void layOut() {
        final BusSettings s = menu.settings();
        final BusLayout.Shape shape = new BusLayout.Shape(abilities(), menu.window(),
                lines(introText()), itemRows(), tagLines(s), editingTag, lines(GameText.resolve(BusTexts.FUZZY_NOTE)),
                Math.min(BusLayout.MOST_CONDITIONS, s.conditions().size()), editingCondition, lines(noteText()));
        rows = BusLayout.rows(shape);
        contentHeight = BusLayout.contentHeight(shape);
    }

    private void drawRow(final GuiGraphics g, final BusLayout.Row row, final int left, final int y, final int px,
                         final int py) {
        final BusSettings s = menu.settings();
        final int x = left;
        switch (row.kind()) {
            case INTRO -> paragraph(g, introText(), x, y);
            case FUZZY_NOTE -> paragraph(g, GameText.resolve(BusTexts.FUZZY_NOTE), x, y);
            case NOTE -> paragraph(g, noteText(), x, y);
            case NOW -> drawNow(g, x, y);
            case HOLDS -> drawHolds(g, x, y);
            case ACCESS -> {
                label(g, BusTexts.ACCESS, x, y + 2);
                drawToggle(g, toggle(row.kind()), x, y, px, py);
                final Toggle access = toggle(row.kind());
                final int last = access.xs().length - 1;
                if (!s.setByOf(BusSettings.ACCESS).isEmpty()) {
                    final int end = access.xs()[last] + access.ws()[last] + 4;
                    BusDraw.mark(g, font, s.setByOf(BusSettings.ACCESS), x + end, y + access.lines()[last]
                            * BusLayout.ROW + 2, BusLayout.RIGHT - end);
                }
            }
            case FILTER -> {
                label(g, menu.window() == BusLayout.Window.EXTERNAL ? BusTexts.SEES : BusTexts.FILTER, x, y + 7);
                for (int i = 0; i < BusAbilities.FILTER_SLOTS; i++) {
                    BusDraw.cell(g, x + BusLayout.CONTROL_X + i * BusLayout.CELL, y + 1, menu.filterStack(i), true);
                }
                markAt(g, s, BusSettings.FILTER, x, BusLayout.CONTROL_X + BusAbilities.FILTER_SLOTS * BusLayout.CELL
                        + 4, y + 7);
            }
            case ITEMS_LABEL -> {
                final int w = BusDraw.width(font, GameText.resolve(BusTexts.ITEMS_LABEL));
                heading(g, BusTexts.ITEMS_LABEL, x, y);
                markAt(g, s, BusSettings.KEEP, x, BusLayout.LABEL_X + w + 4, y);
            }
            case ITEM -> drawItem(g, row.index(), x, y, px, py);
            case ADD_ITEM -> addBar(g, BusTexts.ADD_ITEM, x, y, over(px, py, BusLayout.LABEL_X, 0, BusLayout.ROW_W,
                    BusLayout.CONTROL_H + 1));
            case TAGS -> drawTags(g, s, x, y, px, py);
            case TAG_EDITOR -> {
                BusDraw.well(g, x + BusLayout.CONTROL_X, y, BusLayout.TAG_FIELD_W, BusLayout.CONTROL_H);
                BusDraw.button(g, font, GameText.resolve(BusTexts.ADD), x + BusLayout.TAG_ADD_X, y,
                        BusLayout.TAG_ADD_W, BusLayout.CONTROL_H, over(px, py, BusLayout.TAG_ADD_X, 0,
                                BusLayout.TAG_ADD_W, BusLayout.CONTROL_H));
            }
            case MATCH -> toggleRow(g, BusTexts.MATCH, toggle(row.kind()), x, y, px, py, s, BusSettings.MATCH);
            case FILTER_MODE -> toggleRow(g, null, toggle(row.kind()), x, y, px, py, s, BusSettings.FILTER);
            case KEEP -> stepper(g, BusTexts.KEEP, String.valueOf(s.keep()), keepNote(), x, y, px, py, s,
                    BusSettings.KEEP);
            case MAX -> stepper(g, BusTexts.MAX, maxText(s.max()), GameText.resolve(BusTexts.MAX_NOTE), x, y, px, py,
                    s, BusSettings.MAX);
            case MODE -> toggleRow(g, BusTexts.MODE, toggle(row.kind()), x, y, px, py, s, BusSettings.MODE);
            case POWER -> toggleRow(g, BusTexts.POWER, toggle(row.kind()), x, y, px, py, s, BusSettings.POWER);
            case PRIORITY -> stepper(g, BusTexts.PRIORITY, String.valueOf(s.priority()),
                    menu.window() == BusLayout.Window.EXTERNAL ? GameText.resolve(BusTexts.FILL_FIRST) : "", x, y, px,
                    py, s, BusSettings.PRIORITY);
            case CONDITIONS_LABEL -> {
                final int w = BusDraw.width(font, GameText.resolve(BusTexts.CONDITIONS));
                heading(g, BusTexts.CONDITIONS, x, y);
                markAt(g, s, BusSettings.CONDITIONS, x, BusLayout.LABEL_X + w + 4, y);
            }
            case CONDITION -> drawCondition(g, s.conditions().get(row.index()), x, y, px, py);
            case ADD_CONDITION -> addBar(g, BusTexts.ADD_CONDITION, x, y, over(px, py, BusLayout.LABEL_X, 0,
                    BusLayout.ROW_W, BusLayout.CONTROL_H + 1));
            case CONDITION_EDITOR -> drawEditor(g, x, y, px, py);
            case SPEED -> drawSpeed(g, x, y);
        }
    }

    private void drawNow(final GuiGraphics g, final int x, final int y) {
        final boolean export = exports();
        label(g, export ? BusTexts.SENDS : BusTexts.NOW, x, y + 7);
        final BusActivity.Entry moved = newestMove();
        final ItemStack shown = export ? menu.filterStack(0)
                : moved == null ? ItemStack.EMPTY : BusKeys.icon(moved.what());
        BusDraw.cell(g, x + BusLayout.CONTROL_X, y + 1, shown, export);
        final int textX = x + BusLayout.CONTROL_X + BusLayout.CELL + 3;
        final int room = BusLayout.RIGHT - BusLayout.CONTROL_X - BusLayout.CELL - 3;
        if (shown.isEmpty()) {
            BusDraw.small(g, font, GameText.resolve(BusTexts.NOTHING_YET), textX, y + 7, JsTechTheme.dim());
            return;
        }
        BusDraw.small(g, font, BusDraw.clip(font, shown.getHoverName().getString(), room), textX, y + 3,
                JsTechTheme.text());
        if (moved != null && (!export || BusKeys.icon(moved.what()).is(shown.getItem()))) {
            BusDraw.small(g, font, GameText.resolve(BusTexts.MOVED.with(moved.amount())), textX, y + 11,
                    JsTechTheme.dim());
        }
    }

    private void drawItem(final GuiGraphics g, final int slot, final int x, final int y, final int px, final int py) {
        final BusSettings s = menu.settings();
        BusDraw.bar(g, x + BusLayout.LABEL_X, y, BusLayout.ROW_W, BusLayout.ITEM_H, false);
        final ItemStack stack = menu.filterStack(slot);
        BusDraw.cell(g, x + BusLayout.ITEM_CELL_X, y + 1, stack, true);
        final String name = stack.isEmpty() ? GameText.resolve(BusTexts.NOTHING_YET) : stack.getHoverName().getString();
        BusDraw.small(g, font, BusDraw.clip(font, name, BusLayout.RIGHT - BusLayout.ITEM_NAME_X - 2),
                x + BusLayout.ITEM_NAME_X, y + 2, stack.isEmpty() ? JsTechTheme.dim() : JsTechTheme.text());
        BusDraw.small(g, font, GameText.resolve(BusTexts.KEEP), x + BusLayout.ITEM_KEEP_X, y + 12, JsTechTheme.dim());
        smallStepper(g, x, y + 11, BusLayout.ITEM_KEEP_MINUS, BusLayout.ITEM_KEEP_VALUE, BusLayout.ITEM_KEEP_PLUS,
                String.valueOf(s.itemKeep().get(slot)), px, py - 11);
        BusDraw.small(g, font, GameText.resolve(BusTexts.MAX), x + BusLayout.ITEM_MAX_X, y + 12, JsTechTheme.dim());
        smallStepper(g, x, y + 11, BusLayout.ITEM_MAX_MINUS, BusLayout.ITEM_MAX_VALUE, BusLayout.ITEM_MAX_PLUS,
                maxText(s.itemMax().get(slot)), px, py - 11);
    }

    private void smallStepper(final GuiGraphics g, final int x, final int y, final int minus, final int value,
                              final int plus, final String text, final int px, final int py) {
        final int step = BusLayout.ITEM_STEP;
        BusDraw.button(g, font, "-", x + minus, y, step, step, over(px, py, minus, 0, step, step));
        BusDraw.well(g, x + value, y, BusLayout.ITEM_VALUE_W, step);
        BusDraw.smallCentered(g, font, BusDraw.clip(font, text, BusLayout.ITEM_VALUE_W - 2),
                x + value + BusLayout.ITEM_VALUE_W / 2, y + 1, JsTechTheme.text());
        BusDraw.button(g, font, "+", x + plus, y, step, step, over(px, py, plus, 0, step, step));
    }

    private void drawTags(final GuiGraphics g, final BusSettings s, final int x, final int y, final int px,
                          final int py) {
        label(g, BusTexts.TAGS, x, y + 2);
        final List<String> words = chipWords(s);
        final List<int[]> at = BusLayout.chips(chipWidths(words));
        for (int i = 0; i < words.size(); i++) {
            final int cx = at.get(i)[0];
            final int cy = at.get(i)[1] * BusLayout.ROW;
            final int w = BusDraw.optionWidth(font, words.get(i));
            final boolean add = i == s.tags().size();
            BusDraw.option(g, font, words.get(i), x + cx, y + cy, w, !add,
                    over(px, py, cx, cy, w, BusLayout.CONTROL_H));
        }
    }

    private void drawCondition(final GuiGraphics g, final BusCondition condition, final int x, final int y,
                               final int px, final int py) {
        final int barW = BusLayout.REMOVE_X - 1 - BusLayout.LABEL_X;
        BusDraw.bar(g, x + BusLayout.LABEL_X, y, barW, BusLayout.CONTROL_H + 1, false);
        final Component text = describe(condition);
        BusDraw.small(g, font, clipped(text, barW - 6), x + BusLayout.LABEL_X + 3, y + 2, JsTechTheme.text());
        final boolean hovered = over(px, py, BusLayout.REMOVE_X, 1, BusLayout.REMOVE_W, BusLayout.REMOVE_W);
        BusDraw.bar(g, x + BusLayout.REMOVE_X, y + 1, BusLayout.REMOVE_W, BusLayout.REMOVE_W, hovered);
        BusDraw.smallCentered(g, font, "x", x + BusLayout.REMOVE_X + BusLayout.REMOVE_W / 2, y + 2,
                JsTechTheme.red());
    }

    private void drawEditor(final GuiGraphics g, final int x, final int y, final int px, final int py) {
        final Toggle kinds = toggle(BusLayout.Kind.CONDITION_EDITOR);
        drawToggle(g, kinds, x, y, px, py);
        final int[] buttons = editorButtons();
        final int by = 2 * BusLayout.ROW;
        BusDraw.button(g, font, GameText.resolve(BusTexts.ADD), x + buttons[0], y + by, buttons[1],
                BusLayout.CONTROL_H, over(px, py, buttons[0], by, buttons[1], BusLayout.CONTROL_H));
        BusDraw.button(g, font, GameText.resolve(BusTexts.CANCEL), x + buttons[2], y + by, buttons[3],
                BusLayout.CONTROL_H, over(px, py, buttons[2], by, buttons[3], BusLayout.CONTROL_H));
        final int below = y + BusLayout.ROW;
        final int bpy = py - BusLayout.ROW;
        switch (editKind) {
            case STOCK -> {
                BusDraw.well(g, x + BusLayout.LABEL_X, below, BusLayout.EDIT_FIELD_W, BusLayout.CONTROL_H);
                BusDraw.smallCentered(g, font, "<", x + (BusLayout.LABEL_X + BusLayout.EDIT_FIELD_W
                        + BusLayout.EDIT_MINUS_X) / 2, below + 2, JsTechTheme.dim());
                plainStepper(g, x, below, BusLayout.EDIT_MINUS_X, BusLayout.EDIT_VALUE_X, BusLayout.VALUE_W,
                        BusLayout.EDIT_PLUS_X, String.valueOf(editBelow), px, bpy);
            }
            case HOURS -> {
                BusDraw.small(g, font, GameText.resolve(BusTexts.FROM), x + BusLayout.LABEL_X, below + 2,
                        JsTechTheme.dim());
                plainStepper(g, x, below, BusLayout.HOURS_FROM_MINUS, BusLayout.HOURS_FROM_VALUE,
                        BusLayout.HOURS_VALUE_W, BusLayout.HOURS_FROM_PLUS, BusScript.hour(editFrom), px, bpy);
                BusDraw.small(g, font, GameText.resolve(BusTexts.TO), x + BusLayout.HOURS_TO_X, below + 2,
                        JsTechTheme.dim());
                plainStepper(g, x, below, BusLayout.HOURS_TO_MINUS, BusLayout.HOURS_TO_VALUE, BusLayout.HOURS_VALUE_W,
                        BusLayout.HOURS_TO_PLUS, BusScript.hour(editTo), px, bpy);
            }
            case AFTER -> BusDraw.well(g, x + BusLayout.LABEL_X, below, BusLayout.ROW_W, BusLayout.CONTROL_H);
        }
    }

    private void plainStepper(final GuiGraphics g, final int x, final int y, final int minus, final int value,
                              final int valueW, final int plus, final String text, final int px, final int py) {
        final int h = BusLayout.CONTROL_H;
        BusDraw.button(g, font, "-", x + minus, y, BusLayout.STEP_W, h, over(px, py, minus, 0, BusLayout.STEP_W, h));
        BusDraw.well(g, x + value, y, valueW, h);
        BusDraw.smallCentered(g, font, BusDraw.clip(font, text, valueW - 2), x + value + valueW / 2, y + 2,
                JsTechTheme.text());
        BusDraw.button(g, font, "+", x + plus, y, BusLayout.STEP_W, h, over(px, py, plus, 0, BusLayout.STEP_W, h));
    }

    private void stepper(final GuiGraphics g, final TextKey word, final String value, final String note, final int x,
                         final int y, final int px, final int py, final BusSettings s, final String setting) {
        label(g, word, x, y + 2);
        plainStepper(g, x, y, BusLayout.MINUS_X, BusLayout.VALUE_X, BusLayout.VALUE_W, BusLayout.PLUS_X, value, px,
                py);
        if (!s.setByOf(setting).isEmpty()) {
            BusDraw.mark(g, font, s.setByOf(setting), x + BusLayout.NOTE_X, y + 2, BusLayout.RIGHT - BusLayout.NOTE_X);
        } else if (!note.isEmpty()) {
            BusDraw.small(g, font, BusDraw.clip(font, note, BusLayout.RIGHT - BusLayout.NOTE_X), x + BusLayout.NOTE_X,
                    y + 2, JsTechTheme.dim());
        }
    }

    private void toggleRow(final GuiGraphics g, @Nullable final TextKey word, final Toggle toggle, final int x,
                           final int y, final int px, final int py, final BusSettings s, final String setting) {
        if (word != null) {
            label(g, word, x, y + 2);
        }
        drawToggle(g, toggle, x, y, px, py);
        final int end = toggle.xs()[toggle.xs().length - 1] + toggle.ws()[toggle.ws().length - 1] + 4;
        if (!s.setByOf(setting).isEmpty() && end < BusLayout.RIGHT - 10) {
            BusDraw.mark(g, font, s.setByOf(setting), x + end, y + 2, BusLayout.RIGHT - end);
        }
    }

    private void drawToggle(final GuiGraphics g, final Toggle toggle, final int x, final int y, final int px,
                            final int py) {
        for (int i = 0; i < toggle.words().size(); i++) {
            final int line = toggle.lines()[i] * BusLayout.ROW;
            BusDraw.option(g, font, toggle.words().get(i), x + toggle.xs()[i], y + line, toggle.ws()[i],
                    i == toggle.chosen(), over(px, py, toggle.xs()[i], line, toggle.ws()[i], BusLayout.CONTROL_H));
        }
    }

    private void drawSpeed(final GuiGraphics g, final int x, final int y) {
        label(g, BusTexts.SPEED, x, y + 2);
        final BusStatePayload state = menu.state();
        final long speed = state == null ? 0L : state.speed();
        final String value = GameText.resolve(BusTexts.SPEED_VALUE.with(speed));
        BusDraw.small(g, font, value, x + BusLayout.CONTROL_X, y + 2, JsTechTheme.text());
        final String note = state == null || state.carries() <= 0L ? GameText.resolve(BusTexts.NO_CABLE)
                : GameText.resolve(BusTexts.CABLE_CARRIES.with(state.carries()));
        final int noteX = BusLayout.CONTROL_X + BusDraw.width(font, value) + 5;
        BusDraw.small(g, font, BusDraw.clip(font, note, BusLayout.RIGHT - noteX), x + noteX, y + 2,
                JsTechTheme.dim());
    }

    private void addBar(final GuiGraphics g, final TextKey word, final int x, final int y, final boolean hovered) {
        BusDraw.bar(g, x + BusLayout.LABEL_X, y, BusLayout.ROW_W, BusLayout.CONTROL_H + 1, hovered);
        BusDraw.smallCentered(g, font, GameText.resolve(word), x + BusLayout.LABEL_X + BusLayout.ROW_W / 2, y + 2,
                JsTechTheme.accent());
    }

    private void paragraph(final GuiGraphics g, final String text, final int x, final int y) {
        final List<String> lines = BusDraw.lines(font, text, BusLayout.ROW_W);
        for (int i = 0; i < lines.size(); i++) {
            BusDraw.small(g, font, lines.get(i), x + BusLayout.LABEL_X, y + 1 + i * BusLayout.LINE, JsTechTheme.dim());
        }
    }

    /* A row's word in the column before its controls, smaller when the language's word is longer than the column. */
    private void label(final GuiGraphics g, final TextKey word, final int x, final int y) {
        BusDraw.fitted(g, font, GameText.resolve(word), x + BusLayout.LABEL_X, y, JsTechTheme.dim(),
                BusLayout.CONTROL_X - BusLayout.LABEL_X - 4);
    }

    /* A word over the whole row, cut where the row ends. */
    private void heading(final GuiGraphics g, final TextKey word, final int x, final int y) {
        BusDraw.small(g, font, BusDraw.clip(font, GameText.resolve(word), BusLayout.ROW_W), x + BusLayout.LABEL_X, y,
                JsTechTheme.dim());
    }

    /* The mark of a program's setting at {@code at} across the window, when there is room for it there. */
    private void markAt(final GuiGraphics g, final BusSettings s, final String setting, final int left, final int at,
                        final int y) {
        if (!s.setByOf(setting).isEmpty() && at < BusLayout.RIGHT - 10) {
            BusDraw.mark(g, font, s.setByOf(setting), left + at, y, BusLayout.RIGHT - at);
        }
    }

    private boolean clickRow(final BusLayout.Row row, final int x, final int y) {
        final BusSettings s = menu.settings();
        final int step = shift ? SHIFT_STEP : STEP;
        switch (row.kind()) {
            case NOW -> {
                if (exports() && over(x, y, BusLayout.CONTROL_X, 1, BusLayout.CELL, BusLayout.CELL)) {
                    edit(BusEditPayload.FILTER_SLOT, 0, 0L);
                    return true;
                }
            }
            case FILTER -> {
                for (int i = 0; i < BusAbilities.FILTER_SLOTS; i++) {
                    if (over(x, y, BusLayout.CONTROL_X + i * BusLayout.CELL, 1, BusLayout.CELL, BusLayout.CELL)) {
                        edit(BusEditPayload.FILTER_SLOT, i, 0L);
                        return true;
                    }
                }
            }
            case ITEM -> {
                return clickItem(row.index(), x, y, step);
            }
            case ADD_ITEM -> {
                edit(BusEditPayload.ADD_ITEM, 0, 0L);
                return true;
            }
            case TAGS -> {
                return clickTags(s, x, y);
            }
            case TAG_EDITOR -> {
                if (over(x, y, BusLayout.TAG_ADD_X, 0, BusLayout.TAG_ADD_W, BusLayout.CONTROL_H)) {
                    addTag();
                    return true;
                }
            }
            case MATCH, FILTER_MODE, MODE, POWER, ACCESS -> {
                return clickToggle(row.kind(), x, y);
            }
            case KEEP -> {
                return clickStepper(x, y, BusEditPayload.KEEP, step);
            }
            case MAX -> {
                return clickStepper(x, y, BusEditPayload.MAX, step);
            }
            case PRIORITY -> {
                return clickStepper(x, y, BusEditPayload.PRIORITY, STEP);
            }
            case CONDITION -> {
                if (over(x, y, BusLayout.REMOVE_X, 1, BusLayout.REMOVE_W, BusLayout.REMOVE_W)) {
                    edit(BusEditPayload.REMOVE_CONDITION, row.index(), 0L);
                    return true;
                }
            }
            case ADD_CONDITION -> {
                editingCondition = true;
                editKind = BusCondition.Kind.STOCK;
                editBelow = DEFAULT_BELOW;
                conditionField.setValue("");
                return true;
            }
            case CONDITION_EDITOR -> {
                return clickEditor(x, y);
            }
            default -> {
                return false;
            }
        }
        return false;
    }

    private boolean clickItem(final int slot, final int x, final int y, final int step) {
        if (over(x, y, BusLayout.ITEM_CELL_X, 1, BusLayout.CELL, BusLayout.CELL)) {
            edit(BusEditPayload.FILTER_SLOT, slot, 0L);
            return true;
        }
        final int sy = y - 11;
        final int w = BusLayout.ITEM_STEP;
        final int[][] buttons = {{BusLayout.ITEM_KEEP_MINUS, BusEditPayload.ITEM_KEEP, -step},
                {BusLayout.ITEM_KEEP_PLUS, BusEditPayload.ITEM_KEEP, step},
                {BusLayout.ITEM_MAX_MINUS, BusEditPayload.ITEM_MAX, -step},
                {BusLayout.ITEM_MAX_PLUS, BusEditPayload.ITEM_MAX, step}};
        for (final int[] button : buttons) {
            if (over(x, sy, button[0], 0, w, w)) {
                edit(button[1], slot, button[2]);
                return true;
            }
        }
        return false;
    }

    private boolean clickTags(final BusSettings s, final int x, final int y) {
        final List<String> words = chipWords(s);
        final List<int[]> at = BusLayout.chips(chipWidths(words));
        for (int i = 0; i < words.size(); i++) {
            if (over(x, y, at.get(i)[0], at.get(i)[1] * BusLayout.ROW, BusDraw.optionWidth(font, words.get(i)),
                    BusLayout.CONTROL_H)) {
                if (i < s.tags().size()) {
                    edit(BusEditPayload.REMOVE_TAG, i, 0L);
                } else {
                    editingTag = true;
                    tagField.setValue("");
                }
                return true;
            }
        }
        return false;
    }

    private boolean clickToggle(final BusLayout.Kind kind, final int x, final int y) {
        final Toggle toggle = toggle(kind);
        for (int i = 0; i < toggle.words().size(); i++) {
            if (over(x, y, toggle.xs()[i], toggle.lines()[i] * BusLayout.ROW, toggle.ws()[i], BusLayout.CONTROL_H)) {
                switch (kind) {
                    case MATCH -> edit(BusEditPayload.MATCH, 0, i);
                    case FILTER_MODE -> edit(BusEditPayload.EXCLUDE, 0, i);
                    case MODE -> edit(BusEditPayload.MODE, 0, i);
                    case POWER -> edit(BusEditPayload.POWER, 0, i == 0 ? 1L : 0L);
                    case ACCESS -> edit(BusEditPayload.ACCESS, 0, i);
                    default -> {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    private boolean clickStepper(final int x, final int y, final int op, final int step) {
        if (over(x, y, BusLayout.MINUS_X, 0, BusLayout.STEP_W, BusLayout.CONTROL_H)) {
            edit(op, 0, -step);
            return true;
        }
        if (over(x, y, BusLayout.PLUS_X, 0, BusLayout.STEP_W, BusLayout.CONTROL_H)) {
            edit(op, 0, step);
            return true;
        }
        return false;
    }

    private boolean clickEditor(final int x, final int y) {
        final Toggle kinds = toggle(BusLayout.Kind.CONDITION_EDITOR);
        for (int i = 0; i < kinds.words().size(); i++) {
            if (over(x, y, kinds.xs()[i], 0, kinds.ws()[i], BusLayout.CONTROL_H)) {
                // The kinds are offered in the order of their ids: stock, hours, after.
                final BusCondition.Kind kind = StableIds.of(BusCondition.Kind.class).find(i);
                editKind = kind == null ? editKind : kind;
                conditionField.setValue("");
                return true;
            }
        }
        final int[] buttons = editorButtons();
        if (over(x, y, buttons[0], 2 * BusLayout.ROW, buttons[1], BusLayout.CONTROL_H)) {
            addCondition();
            return true;
        }
        if (over(x, y, buttons[2], 2 * BusLayout.ROW, buttons[3], BusLayout.CONTROL_H)) {
            editingCondition = false;
            return true;
        }
        final int by = y - BusLayout.ROW;
        final int belowStep = shift ? BELOW_SHIFT_STEP : STEP;
        switch (editKind) {
            case STOCK -> {
                if (over(x, by, BusLayout.LABEL_X, 0, BusLayout.EDIT_FIELD_W, BusLayout.CONTROL_H)
                        && !menu.getCarried().isEmpty()) {
                    conditionField.setValue(BuiltInRegistries.ITEM.getKey(menu.getCarried().getItem()).toString());
                    return true;
                }
                if (over(x, by, BusLayout.EDIT_MINUS_X, 0, BusLayout.STEP_W, BusLayout.CONTROL_H)) {
                    editBelow = Math.max(1L, editBelow - belowStep);
                    return true;
                }
                if (over(x, by, BusLayout.EDIT_PLUS_X, 0, BusLayout.STEP_W, BusLayout.CONTROL_H)) {
                    editBelow = editBelow + belowStep;
                    return true;
                }
            }
            case HOURS -> {
                final int[][] hours = {{BusLayout.HOURS_FROM_MINUS, 0, -1}, {BusLayout.HOURS_FROM_PLUS, 0, 1},
                        {BusLayout.HOURS_TO_MINUS, 1, -1}, {BusLayout.HOURS_TO_PLUS, 1, 1}};
                for (final int[] button : hours) {
                    if (over(x, by, button[0], 0, BusLayout.STEP_W, BusLayout.CONTROL_H)) {
                        if (button[1] == 0) {
                            editFrom = Math.floorMod(editFrom + button[2], HOURS);
                        } else {
                            editTo = Math.floorMod(editTo + button[2], HOURS);
                        }
                        return true;
                    }
                }
            }
            case AFTER -> {
                return false;
            }
        }
        return false;
    }

    private List<Component> rowTooltip(final BusLayout.Row row, final int x, final int y) {
        final BusSettings s = menu.settings();
        return switch (row.kind()) {
            case FILTER -> {
                for (int i = 0; i < BusAbilities.FILTER_SLOTS; i++) {
                    if (over(x, y, BusLayout.CONTROL_X + i * BusLayout.CELL, 1, BusLayout.CELL, BusLayout.CELL)) {
                        yield cellTooltip(menu.filterStack(i));
                    }
                }
                yield marked(s, BusSettings.FILTER, x, y);
            }
            case NOW -> exports() && over(x, y, BusLayout.CONTROL_X, 1, BusLayout.CELL, BusLayout.CELL)
                    ? cellTooltip(menu.filterStack(0)) : List.of();
            case ITEM -> over(x, y, BusLayout.ITEM_CELL_X, 1, BusLayout.CELL, BusLayout.CELL)
                    ? cellTooltip(menu.filterStack(row.index())) : List.of(GameText.component(BusTexts.STEP_HINT));
            case ADD_ITEM -> List.of(GameText.component(BusTexts.ADD_ITEM_HINT));
            case TAGS -> x >= BusLayout.CONTROL_X ? List.of(GameText.component(BusTexts.REMOVE_HINT)) : List.of();
            case KEEP, MAX -> marked(s, row.kind() == BusLayout.Kind.KEEP ? BusSettings.KEEP : BusSettings.MAX, x, y,
                    GameText.component(BusTexts.STEP_HINT));
            case PRIORITY -> marked(s, BusSettings.PRIORITY, x, y, GameText.component(BusTexts.PRIORITY_HINT));
            case MODE -> marked(s, BusSettings.MODE, x, y, GameText.component(BusTexts.ON_DEMAND_HINT));
            case POWER -> marked(s, BusSettings.POWER, x, y);
            case ACCESS -> marked(s, BusSettings.ACCESS, x, y);
            case MATCH -> marked(s, BusSettings.MATCH, x, y);
            case FILTER_MODE -> marked(s, BusSettings.FILTER, x, y);
            case CONDITIONS_LABEL -> marked(s, BusSettings.CONDITIONS, x, y);
            case CONDITION -> over(x, y, BusLayout.REMOVE_X, 1, BusLayout.REMOVE_W, BusLayout.REMOVE_W)
                    ? List.of(GameText.component(BusTexts.REMOVE_HINT)) : List.of(describe(
                    s.conditions().get(row.index())));
            case CONDITION_EDITOR -> editKind == BusCondition.Kind.STOCK && y >= BusLayout.ROW
                    ? List.of(GameText.component(BusTexts.STOCK_HINT)) : List.of();
            default -> List.of();
        };
    }

    /* What a program's mark says, when the setting has one, with what the row says anyway. */
    private List<Component> marked(final BusSettings s, final String setting, final int x, final int y,
                                   final Component... always) {
        final List<Component> lines = new ArrayList<>(List.of(always));
        if (!s.setByOf(setting).isEmpty()) {
            lines.add(GameText.component(BusTexts.SET_BY.with(s.setByOf(setting))));
        }
        return lines;
    }

    private List<Component> cellTooltip(final ItemStack stack) {
        final List<Component> lines = new ArrayList<>();
        if (!stack.isEmpty()) {
            lines.add(stack.getHoverName());
        }
        lines.add(GameText.component(BusTexts.CELL_HINT));
        return lines;
    }

    private void addTag() {
        final String typed = tagField.getValue().trim();
        if (!typed.isEmpty()) {
            send.accept(new BusEditPayload(BusEditPayload.ADD_TAG, 0, 0L, typed));
        }
        editingTag = false;
        tagField.setFocused(false);
    }

    private void addCondition() {
        final String typed = conditionField.getValue().trim();
        switch (editKind) {
            case STOCK -> send.accept(new BusEditPayload(BusEditPayload.ADD_CONDITION, editKind.id(), editBelow,
                    typed));
            case HOURS -> edit(BusEditPayload.ADD_CONDITION, editKind.id(), (long) editFrom * HOURS + editTo);
            case AFTER -> send.accept(new BusEditPayload(BusEditPayload.ADD_CONDITION, editKind.id(), 0L, typed));
        }
        editingCondition = false;
        conditionField.setFocused(false);
    }

    /* Where ADD and CANCEL go at the right of the condition being written, as wide as their words: x, w, x, w. */
    private int[] editorButtons() {
        final int cancelW = BusDraw.optionWidth(font, GameText.resolve(BusTexts.CANCEL)) + 4;
        final int addW = BusDraw.optionWidth(font, GameText.resolve(BusTexts.ADD)) + 4;
        final int cancelX = BusLayout.RIGHT - cancelW;
        return new int[] {cancelX - 3 - addW, addW, cancelX, cancelW};
    }

    private void edit(final int op, final int slot, final long value) {
        send.accept(BusEditPayload.of(op, slot, value));
    }

    /* The fields go where their rows are, and only while those rows are wholly in view. */
    private void placeFields(final int left, final int viewTop, final int viewH) {
        tagField.setVisible(false);
        conditionField.setVisible(false);
        for (final BusLayout.Row row : rows) {
            final int y = viewTop + row.y() - scroll;
            if (row.kind() == BusLayout.Kind.TAG_EDITOR) {
                show(tagField, left + BusLayout.CONTROL_X + 3, y + 2, BusLayout.TAG_FIELD_W - 6, viewTop, viewH);
            } else if (row.kind() == BusLayout.Kind.CONDITION_EDITOR && editKind != BusCondition.Kind.HOURS) {
                final int w = editKind == BusCondition.Kind.STOCK ? BusLayout.EDIT_FIELD_W : BusLayout.ROW_W;
                show(conditionField, left + BusLayout.LABEL_X + 3, y + BusLayout.ROW + 2, w - 6, viewTop, viewH);
                conditionField.setHint(GameText.component(editKind == BusCondition.Kind.STOCK ? BusTexts.STOCK_FIELD
                        : BusTexts.AFTER_FIELD));
            }
        }
    }

    private static void show(final EditBox field, final int x, final int y, final int w, final int viewTop,
                             final int viewH) {
        if (y - 2 >= viewTop && y + BusLayout.CONTROL_H - 2 <= viewTop + viewH) {
            field.setX(x);
            field.setY(y);
            field.setWidth(w);
            field.setVisible(true);
        }
    }

    private EditBox field(final TextKey hint, final int most) {
        final EditBox box = new EditBox(font, 0, 0, BusLayout.TAG_FIELD_W, BusLayout.CONTROL_H - 2,
                GameText.component(hint));
        box.setBordered(false);
        box.setTextShadow(false);
        box.setMaxLength(most);
        box.setTextColor(JsTechTheme.text());
        box.setHint(GameText.component(hint));
        box.setVisible(false);
        return box;
    }

    /** A toggle's words, where each option goes, on which of its lines, how wide it is, and which is chosen. */
    private record Toggle(List<String> words, int[] xs, int[] ws, int chosen, int[] lines) {
    }

    private Toggle toggle(final BusLayout.Kind kind) {
        final BusSettings s = menu.settings();
        return switch (kind) {
            case MATCH -> options(BusLayout.CONTROL_X, s.fuzzy() ? 1 : 0, BusTexts.EXACT, BusTexts.FUZZY);
            case ACCESS -> wrapped(s.access(), BusTexts.READ_WRITE, BusTexts.READ_ONLY, BusTexts.WRITE_ONLY);
            case FILTER_MODE -> {
                final Toggle there = options(BusLayout.CONTROL_X, s.exclude() ? 1 : 0, BusTexts.ONLY_THESE,
                        BusTexts.ALL_BUT);
                // Too wide for its column in a longer language: it has no word, so it starts at the row's edge.
                yield there.xs()[1] + there.ws()[1] <= BusLayout.RIGHT ? there
                        : options(BusLayout.LABEL_X, s.exclude() ? 1 : 0, BusTexts.ONLY_THESE, BusTexts.ALL_BUT);
            }
            case MODE -> options(BusLayout.CONTROL_X, s.onDemand() ? 1 : 0, BusTexts.CONTINUOUS, BusTexts.ON_DEMAND);
            case POWER -> options(BusLayout.CONTROL_X, s.powered() ? 0 : 1, BusTexts.ON, BusTexts.OFF);
            default -> options(BusLayout.LABEL_X, editKind.id(), BusTexts.KIND_STOCK, BusTexts.KIND_HOURS,
                    BusTexts.KIND_AFTER);
        };
    }

    private Toggle options(final int x, final int chosen, final TextKey... words) {
        final List<String> resolved = new ArrayList<>();
        final int[] widths = new int[words.length];
        for (int i = 0; i < words.length; i++) {
            resolved.add(GameText.resolve(words[i]));
            widths[i] = BusDraw.optionWidth(font, resolved.get(i));
        }
        return new Toggle(resolved, BusLayout.options(x, widths), widths, chosen, new int[words.length]);
    }

    /* A toggle whose options run on to a second line when they do not fit beside its word on one. */
    private Toggle wrapped(final int chosen, final TextKey... words) {
        final List<String> resolved = new ArrayList<>();
        final List<Integer> widths = new ArrayList<>();
        for (final TextKey word : words) {
            resolved.add(GameText.resolve(word));
            widths.add(BusDraw.optionWidth(font, resolved.get(resolved.size() - 1)));
        }
        final List<int[]> at = BusLayout.chips(widths);
        final int[] xs = new int[words.length];
        final int[] ws = new int[words.length];
        final int[] lines = new int[words.length];
        for (int i = 0; i < words.length; i++) {
            xs[i] = at.get(i)[0];
            ws[i] = widths.get(i);
            lines[i] = at.get(i)[1];
        }
        return new Toggle(resolved, xs, ws, chosen, lines);
    }

    /* The words of the tag chips: each tag, then the chip that adds one while there is room and none is typed. */
    private List<String> chipWords(final BusSettings s) {
        final List<String> words = new ArrayList<>();
        s.tags().forEach(tag -> words.add("#" + tag));
        if (s.tags().size() < AbstractBusPart.MAX_TAGS && !editingTag) {
            words.add(GameText.resolve(BusTexts.ADD_TAG));
        }
        return words;
    }

    private List<Integer> chipWidths(final List<String> words) {
        return words.stream().map(word -> BusDraw.optionWidth(font, word)).toList();
    }

    private int tagLines(final BusSettings s) {
        final List<int[]> at = BusLayout.chips(chipWidths(chipWords(s)));
        return at.isEmpty() ? 1 : at.get(at.size() - 1)[1] + 1;
    }

    /* The Transition rows: one per slot up to the last that lists something. */
    private int itemRows() {
        int last = -1;
        for (int i = 0; i < BusAbilities.FILTER_SLOTS; i++) {
            if (!menu.settings().filter().get(i).isEmpty()) {
                last = i;
            }
        }
        return last + 1;
    }

    private int lines(final String text) {
        return Math.max(1, BusDraw.lines(font, text, BusLayout.ROW_W).size());
    }

    private Component describe(final BusCondition condition) {
        final MutableComponent text = switch (condition.kind()) {
            case STOCK -> sentence(BusTexts.COND_STOCK, Component.literal(String.valueOf(condition.below())),
                    BusKeys.itemName(condition.subject()));
            case HOURS -> sentence(BusTexts.COND_HOURS, Component.literal(BusScript.hour(condition.fromHour())),
                    Component.literal(BusScript.hour(condition.toHour())));
            case AFTER -> sentence(BusTexts.COND_AFTER, Component.literal(condition.subject()));
        };
        return text;
    }

    /* A sentence with what it says put in in the accent. */
    private static MutableComponent sentence(final TextKey key, final Component... args) {
        final Object[] coloured = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            coloured[i] = args[i].copy().withColor(JsTechTheme.accent() & 0xFFFFFF);
        }
        return Component.translatableWithFallback(key.key(), key.english(), coloured);
    }

    private Component clipped(final Component text, final int room) {
        return BusDraw.width(font, text) <= room ? text
                : Component.literal(BusDraw.clip(font, text.getString(), room));
    }

    private String introText() {
        if (menu.window() == BusLayout.Window.EXTERNAL) {
            return GameText.resolve(BusTexts.INTRO_EXTERNAL);
        }
        return GameText.resolve(exports() ? BusTexts.INTRO_EXPORT : BusTexts.INTRO_IMPORT);
    }

    /* The note at the end of the rows: what a crafting bus carries, or that an external inventory is slower. */
    private String noteText() {
        return GameText.resolve(menu.window() == BusLayout.Window.EXTERNAL ? BusTexts.EXTERNAL_NOTE
                : BusTexts.CRAFTING_NOTE);
    }

    /* What an External Storage Bus's inventory holds, as the network is shown it. */
    private void drawHolds(final GuiGraphics g, final int x, final int y) {
        label(g, BusTexts.HOLDS, x, y + 2);
        final BusStatePayload state = menu.state();
        final int places = state == null ? 0 : state.places();
        final String value = places <= 0 ? GameText.resolve(BusTexts.NOTHING_FACED)
                : GameText.resolve(BusTexts.HOLDS_VALUE.with(places, state.placesUsed()));
        BusDraw.small(g, font, BusDraw.clip(font, value, BusLayout.RIGHT - BusLayout.CONTROL_X), x
                + BusLayout.CONTROL_X, y + 2, places <= 0 ? JsTechTheme.dim() : JsTechTheme.text());
        final int noteX = BusLayout.CONTROL_X + BusDraw.width(font, value) + 5;
        if (places > 0 && noteX < BusLayout.RIGHT - 10) {
            BusDraw.small(g, font, BusDraw.clip(font, GameText.resolve(BusTexts.HOLDS_NOTE), BusLayout.RIGHT - noteX),
                    x + noteX, y + 2, JsTechTheme.dim());
        }
    }

    private String keepNote() {
        return GameText.resolve(exports() ? BusTexts.KEEP_NOTE_EXPORT : BusTexts.KEEP_NOTE_IMPORT);
    }

    private String maxText(final int max) {
        return max <= 0 ? GameText.resolve(BusTexts.ANY) : String.valueOf(max);
    }

    private BusAbilities abilities() {
        return menu.abilities();
    }

    /* Whether the bus sends out of the network rather than bringing in. */
    private boolean exports() {
        return menu.exports();
    }

    @Nullable
    private BusActivity.Entry newestMove() {
        final BusStatePayload state = menu.state();
        if (state == null) {
            return null;
        }
        return state.activity().stream().filter(e -> e.reason() == BusActivity.MOVED).findFirst().orElse(null);
    }

    /* Whether the point, inside the row, is over a box at (x, y) of w by h. */
    private static boolean over(final int px, final int py, final int x, final int y, final int w, final int h) {
        return px >= x && px < x + w && py >= y && py < y + h;
    }
}
