/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.api.ComponentKind;
import dev.jstech.computers.api.client.ComponentRenderers;
import dev.jstech.computers.api.client.IComponentRenderer;
import dev.jstech.computers.config.ComputersClientConfig;
import dev.jstech.computers.gui.layout.UiLayout;
import dev.jstech.computers.operation.payload.UiWindowPayload;
import dev.jstech.computers.os.fs.PixImage;
import dev.jstech.computers.os.ComponentKinds;
import dev.jstech.computers.vm.program.ComponentValues;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Draws each widget of a program's window in the look of the machine's system.
 *
 * <p>Every frame, from what the program last sent and what the screen keeps of its own (what is being typed, how far a
 * list is scrolled, which tree nodes are folded). The skin's primitives are the system's own controls, and the small
 * marks that tell systems apart come from {@link SigmaGlyphs}; nothing here picks a colour of its own but the few a
 * log, a chart and a missing component share on every system.
 */
final class SigmaPainter {

    private final SigmaUiState ui;
    private final BlockPos host;
    /** What each generic component's value reads as, kept until it changes, so it is read once and not every frame. */
    private final Map<Long, Decoded> decoded = new HashMap<>();
    /** The generic components whose renderer failed, which show the placeholder from then on. */
    private final Set<Long> failed = new HashSet<>();
    /** The stacks each item id shows as, made once. */
    private final Map<String, ItemStack> stacks = new HashMap<>();

    /** How tall a row of a list, a table, a tree, a log or a menu is, and a table's header. */
    static final int ROW_H = 11;
    static final int HEADER_H = 12;
    /** How big a slot of an item is. */
    static final int SLOT = 18;
    /** How tall an item picker's search line is. */
    static final int SEARCH_H = 14;

    SigmaPainter(final SigmaUiState ui, final BlockPos host) {
        this.ui = ui;
        this.host = host;
    }

    /** The item an id names, or an empty stack when the game has none by that name. */
    ItemStack stackOf(final String id) {
        return this.stacks.computeIfAbsent(id, key -> {
            final ResourceLocation name = ResourceLocation.tryParse(key);
            return name == null || !BuiltInRegistries.ITEM.containsKey(name) ? ItemStack.EMPTY
                    : new ItemStack(BuiltInRegistries.ITEM.get(name));
        });
    }

    /** What a generic component's value reads as, or null when it holds none or what came is unreadable. */
    Object dataOf(final UiWindowPayload.Widget widget) {
        final String joined = widget.joined();
        final Decoded had = this.decoded.get(widget.id());
        if (had != null && had.text().equals(joined)) {
            return had.value();
        }
        Object value;
        try {
            value = joined.isEmpty() ? null : ComponentValues.decode(joined);
        } catch (final IllegalArgumentException | IndexOutOfBoundsException unreadable) {
            value = null;
        }
        this.decoded.put(widget.id(), new Decoded(joined, value));
        return value;
    }

    /** The renderer that draws a generic component on this game, or null when it shows a placeholder instead. */
    IComponentRenderer rendererOf(final UiWindowPayload.Widget widget) {
        final IComponentRenderer renderer = ComponentRenderers.get(widget.text());
        final ComponentKind kind = ComponentKinds.find(widget.text());
        if (renderer == null || this.failed.contains(widget.id())
                || kind != null && kind.reachesOutside() && !ComputersClientConfig.outsideComponents()) {
            return null;
        }
        return renderer;
    }

    /** A renderer failed: its component shows the placeholder from now on, and the log is told once. */
    void fail(final UiWindowPayload.Widget widget, final RuntimeException fault) {
        if (this.failed.add(widget.id())) {
            JsComputers.LOGGER.warn("The component kind {} failed to draw and shows a placeholder from now on",
                    widget.text(), fault);
        }
    }

    /** Draws one widget into the place the layout gave it. */
    void draw(final GuiGraphics g, final Font font, final OsSkin skin, final UiWindowPayload.Widget widget,
              final UiLayout.Rect rect, final int mouseX, final int mouseY) {
        final boolean over = mouseX >= rect.x() && mouseX < rect.x() + rect.w()
                && mouseY >= rect.y() && mouseY < rect.y() + rect.h();
        final int middle = rect.y() + (rect.h() - 7) / 2;
        switch (widget.kind()) {
            case "Label" -> Draw.text(g, font, widget.text(), rect.x(), middle,
                    widget.answers() ? skin.text() : skin.dim());
            case "Button" -> skin.button(g, font, rect.x(), rect.y(), rect.w(), rect.h(), widget.text(),
                    over && widget.answers(), this.ui.pressed == widget.id(), false);
            case "TextBox" -> this.textBox(g, font, skin, widget, rect, middle);
            case "TextArea" -> this.textArea(g, font, skin, widget, rect);
            case "CheckBox" -> {
                SigmaGlyphs.check(g, skin, rect.x(), rect.y() + (rect.h() - SigmaGlyphs.MARK) / 2, widget.ticked());
                Draw.text(g, font, widget.text(), rect.x() + SigmaGlyphs.MARK + 4, middle, skin.text());
            }
            case "ProgressBar" -> this.bar(g, skin, rect, widget.number(1), widget.number(2), widget.number(3));
            case "NumberBox" -> this.numberBox(g, font, skin, widget, rect, middle);
            case "Slider" -> {
                final int least = widget.number(2);
                final int most = Math.max(least + 1, widget.number(3));
                SigmaGlyphs.slider(g, font, skin, rect.x(), rect.y(), rect.w(), rect.h(),
                        (widget.number(1) - least) / (double) (most - least), true);
            }
            case "RadioGroup" -> this.radios(g, font, skin, widget, rect);
            case "ComboBox" -> this.combo(g, font, skin, widget, rect, middle);
            case "TabView" -> this.tabs(g, font, skin, widget, rect);
            case "GroupBox" -> this.group(g, font, skin, widget, rect);
            case "ScrollView" -> this.scrollView(g, skin, widget, rect);
            case "ListBox" -> this.list(g, font, skin, widget, rect, mouseX, mouseY);
            case "Table" -> this.table(g, font, skin, widget, rect, mouseX, mouseY);
            case "TreeView" -> this.tree(g, font, skin, widget, rect, mouseX, mouseY);
            case "MenuBar" -> this.menuBar(g, font, skin, widget, rect, mouseX, mouseY);
            case "StatusBar" -> this.statusBar(g, font, skin, widget, rect, middle);
            case "Image" -> this.image(g, font, skin, widget, rect);
            case "Chart" -> this.chart(g, font, skin, widget, rect);
            case "LogView" -> this.log(g, font, skin, widget, rect);
            case "ItemSlot" -> this.slot(g, font, skin, rect.x(), rect.y(), widget.text(), widget.number(1),
                    over && widget.answers());
            case "ItemPicker" -> this.picker(g, font, skin, widget, rect, mouseX, mouseY);
            case "OperationView" -> this.operation(g, font, skin, widget, rect);
            case "GenericComponent" -> this.generic(g, font, skin, widget, rect, mouseX, mouseY);
            case "Canvas" -> SigmaCanvas.draw(g, font, skin, widget, rect);
            default -> {
                // A row or a column is not drawn: it is only where its widgets are.
            }
        }
        if (widget.cut() && rect.h() >= ROW_H * 2) {
            Draw.text(g, font, GameText.resolve(SigmaWindowTexts.CUT), rect.x() + 3, rect.y() + rect.h() - 10,
                    skin.dim());
        }
    }

    private void textBox(final GuiGraphics g, final Font font, final OsSkin skin, final UiWindowPayload.Widget widget,
                         final UiLayout.Rect rect, final int middle) {
        skin.field(g, rect.x(), rect.y(), rect.w(), rect.h(), this.ui.focused == widget.id());
        final String said = this.ui.textOf(widget);
        final String shown = tail(font, said, rect.w() - 8);
        Draw.text(g, font, shown, rect.x() + 4, middle, skin.text());
        if (this.ui.focused == widget.id() && blink()) {
            final int at = rect.x() + 4 + font.width(shown);
            g.fill(at, rect.y() + 3, at + 1, rect.y() + rect.h() - 3, skin.text());
        }
    }

    /* A text area: its lines wrapped to its width, the last of them in sight while it is being typed in. */
    private void textArea(final GuiGraphics g, final Font font, final OsSkin skin,
                          final UiWindowPayload.Widget widget, final UiLayout.Rect rect) {
        skin.field(g, rect.x(), rect.y(), rect.w(), rect.h(), this.ui.focused == widget.id());
        final List<String> lines = wrap(font, this.ui.textOf(widget), rect.w() - 12);
        final int rows = Math.max(1, (rect.h() - 4) / (font.lineHeight + 1));
        final int most = Math.max(0, lines.size() - rows);
        final int top = this.ui.focused == widget.id() ? most : Math.min(this.ui.scroll(widget.id()), most);
        for (int i = 0; i < rows && top + i < lines.size(); i++) {
            Draw.text(g, font, lines.get(top + i), rect.x() + 4, rect.y() + 3 + i * (font.lineHeight + 1),
                    skin.text());
        }
        if (this.ui.focused == widget.id() && blink() && !lines.isEmpty()) {
            final int row = Math.min(rows - 1, lines.size() - 1 - top);
            final int at = rect.x() + 4 + font.width(lines.getLast());
            final int y = rect.y() + 3 + row * (font.lineHeight + 1);
            g.fill(at, y - 1, at + 1, y + font.lineHeight - 1, skin.text());
        }
        if (lines.size() > rows) {
            this.thumb(g, skin, rect, top, rows, lines.size());
        }
    }

    private void bar(final GuiGraphics g, final OsSkin skin, final UiLayout.Rect rect, final int value,
                     final int least, final int mostAsked) {
        skin.panel(g, rect.x(), rect.y(), rect.w(), rect.h());
        final int most = Math.max(least + 1, mostAsked);
        final int room = rect.w() - 4;
        final int filled = (int) ((long) room * (Math.clamp(value, least, most) - least) / (most - least));
        if (filled > 0) {
            g.fill(rect.x() + 2, rect.y() + 2, rect.x() + 2 + filled, rect.y() + rect.h() - 2, skin.progressFill());
        }
    }

    private void numberBox(final GuiGraphics g, final Font font, final OsSkin skin,
                           final UiWindowPayload.Widget widget, final UiLayout.Rect rect, final int middle) {
        final int fieldW = rect.w() - SigmaGlyphs.ARROW_W;
        skin.field(g, rect.x(), rect.y(), fieldW, rect.h(), this.ui.focused == widget.id());
        final String typed = this.ui.typing.get(widget.id());
        final String shown = typed != null ? typed : String.valueOf(widget.number(1));
        Draw.text(g, font, shown, rect.x() + 4, middle, skin.text());
        if (this.ui.focused == widget.id() && blink()) {
            final int at = rect.x() + 4 + font.width(shown);
            g.fill(at, rect.y() + 3, at + 1, rect.y() + rect.h() - 3, skin.text());
        }
        final int half = rect.h() / 2;
        SigmaGlyphs.arrowButton(g, font, skin, rect.x() + fieldW, rect.y(), SigmaGlyphs.ARROW_W, half, false, false);
        SigmaGlyphs.arrowButton(g, font, skin, rect.x() + fieldW, rect.y() + half, SigmaGlyphs.ARROW_W,
                rect.h() - half, true, false);
    }

    private void radios(final GuiGraphics g, final Font font, final OsSkin skin, final UiWindowPayload.Widget widget,
                        final UiLayout.Rect rect) {
        final List<UiLayout.Rect> places = radioPlaces(font, widget, rect);
        for (int i = 0; i < places.size(); i++) {
            final UiLayout.Rect place = places.get(i);
            SigmaGlyphs.radio(g, skin, place.x(), place.y() + (place.h() - SigmaGlyphs.MARK) / 2,
                    widget.number(4) == i + 1);
            Draw.text(g, font, widget.rows().get(i), place.x() + SigmaGlyphs.MARK + 3,
                    place.y() + (place.h() - 7) / 2, skin.text());
        }
    }

    /** Where each choice of a radio group is: one after another across, or one under another down. */
    static List<UiLayout.Rect> radioPlaces(final Font font, final UiWindowPayload.Widget widget,
                                           final UiLayout.Rect rect) {
        final List<UiLayout.Rect> places = new ArrayList<>();
        int at = rect.x();
        for (int i = 0; i < widget.rows().size(); i++) {
            final int wide = SigmaGlyphs.MARK + 3 + font.width(widget.rows().get(i));
            if (widget.ticked()) {
                places.add(new UiLayout.Rect(i, at, rect.y(), wide, rect.h()));
                at += wide + 10;
            } else {
                places.add(new UiLayout.Rect(i, rect.x(), rect.y() + i * 12, wide, 12));
            }
        }
        return places;
    }

    private void combo(final GuiGraphics g, final Font font, final OsSkin skin, final UiWindowPayload.Widget widget,
                       final UiLayout.Rect rect, final int middle) {
        skin.field(g, rect.x(), rect.y(), rect.w(), rect.h(), this.ui.popupOwner == widget.id());
        final int picked = widget.number(4);
        if (picked >= 1 && picked <= widget.rows().size()) {
            Draw.text(g, font, font.plainSubstrByWidth(widget.row(picked - 1), rect.w() - SigmaGlyphs.ARROW_W - 6),
                    rect.x() + 4, middle, skin.text());
        }
        SigmaGlyphs.arrowButton(g, font, skin, rect.x() + rect.w() - SigmaGlyphs.ARROW_W - 1, rect.y() + 1,
                SigmaGlyphs.ARROW_W, rect.h() - 2, true, this.ui.popupOwner == widget.id());
    }

    private void tabs(final GuiGraphics g, final Font font, final OsSkin skin, final UiWindowPayload.Widget widget,
                      final UiLayout.Rect rect) {
        skin.panel(g, rect.x(), rect.y() + UiLayout.TAB_H, rect.w(), rect.h() - UiLayout.TAB_H);
        final List<UiLayout.Rect> places = tabPlaces(font, widget, rect);
        for (int i = 0; i < places.size(); i++) {
            final UiLayout.Rect place = places.get(i);
            skin.tab(g, font, place.x(), place.y(), place.w(), place.h(), widget.row(i), widget.number(4) == i + 1);
        }
    }

    /** Where each tab of a tab view is along its strip. */
    static List<UiLayout.Rect> tabPlaces(final Font font, final UiWindowPayload.Widget widget,
                                         final UiLayout.Rect rect) {
        final List<UiLayout.Rect> places = new ArrayList<>();
        int at = rect.x();
        for (int i = 0; i < widget.rows().size(); i++) {
            final int wide = font.width(widget.row(i)) + 14;
            places.add(new UiLayout.Rect(i, at, rect.y(), wide, UiLayout.TAB_H));
            at += wide;
        }
        return places;
    }

    /* A group box: a frame round its widget with the caption set into its top edge. */
    private void group(final GuiGraphics g, final Font font, final OsSkin skin, final UiWindowPayload.Widget widget,
                       final UiLayout.Rect rect) {
        final int top = rect.y() + UiLayout.CAPTION_H / 2;
        Draw.outline(g, rect.x(), top, rect.w(), rect.h() - (top - rect.y()), skin.edge());
        if (!widget.text().isEmpty()) {
            final int wide = font.width(widget.text());
            g.fill(rect.x() + 5, rect.y(), rect.x() + 9 + wide, rect.y() + UiLayout.CAPTION_H, skin.windowBg());
            Draw.text(g, font, widget.text(), rect.x() + 7, rect.y() + 1,
                    skin.form() == OsSkin.Form.LUNA ? skin.accent() : skin.text());
        }
    }

    private void scrollView(final GuiGraphics g, final OsSkin skin, final UiWindowPayload.Widget widget,
                            final UiLayout.Rect rect) {
        skin.panel(g, rect.x(), rect.y(), rect.w(), rect.h());
        final int barX = rect.x() + rect.w() - UiLayout.SCROLLBAR;
        g.fill(barX, rect.y() + 1, barX + UiLayout.SCROLLBAR - 1, rect.y() + rect.h() - 1, skin.fieldBg());
        final int room = Math.max(1, this.ui.rooms.getOrDefault(widget.id(), 0));
        final int span = rect.h() - 2;
        final int thumb = Math.max(8, span * span / (span + room));
        final int top = rect.y() + 1 + (span - thumb) * Math.min(this.ui.scroll(widget.id()), room) / room;
        skin.scrollThumb(g, barX + 1, top, UiLayout.SCROLLBAR - 3, thumb);
    }

    private void list(final GuiGraphics g, final Font font, final OsSkin skin, final UiWindowPayload.Widget widget,
                      final UiLayout.Rect rect, final int mouseX, final int mouseY) {
        skin.panel(g, rect.x(), rect.y(), rect.w(), rect.h());
        final int rows = Math.max(1, (rect.h() - 4) / ROW_H);
        final int scroll = this.clampScroll(widget.id(), widget.rows().size(), rows);
        for (int i = 0; i < rows && scroll + i < widget.rows().size(); i++) {
            final int at = scroll + i;
            final int top = rect.y() + 2 + i * ROW_H;
            final boolean picked = at == widget.number(4) - 1;
            this.row(g, skin, rect, top, mouseX, mouseY, picked);
            final int colour = skin.listRowText(picked);
            Draw.text(g, font, widget.rows().get(at), rect.x() + 4, top + 2, colour);
            if (at < widget.details().size() && !widget.details().get(at).isEmpty()) {
                final String right = widget.details().get(at);
                Draw.text(g, font, right, rect.x() + rect.w() - 4 - font.width(right), top + 2, colour);
            }
        }
        if (widget.rows().size() > rows) {
            this.thumb(g, skin, rect, scroll, rows, widget.rows().size());
        }
    }

    /* A table: its headers in the system's header cells, its rows under them, each column as wide as it needs. */
    private void table(final GuiGraphics g, final Font font, final OsSkin skin, final UiWindowPayload.Widget widget,
                       final UiLayout.Rect rect, final int mouseX, final int mouseY) {
        skin.panel(g, rect.x(), rect.y(), rect.w(), rect.h());
        final int[] widths = columnWidths(font, widget, rect.w() - 4);
        int at = rect.x() + 2;
        for (int c = 0; c < widths.length; c++) {
            final String header = c < widget.details().size() ? widget.details().get(c) : "";
            if (skin.form() == OsSkin.Form.FLAT) {
                Draw.text(g, font, font.plainSubstrByWidth(header, widths[c] - 6), at + 3, rect.y() + 4, skin.dim());
            } else {
                skin.button(g, font, at, rect.y() + 1, widths[c], HEADER_H, "", false, false, false);
                Draw.text(g, font, font.plainSubstrByWidth(header, widths[c] - 6), at + 3, rect.y() + 3, skin.text());
            }
            at += widths[c];
        }
        g.fill(rect.x() + 2, rect.y() + HEADER_H + 1, rect.x() + rect.w() - 2, rect.y() + HEADER_H + 2, skin.edge());
        final int rows = Math.max(1, (rect.h() - HEADER_H - 6) / ROW_H);
        final int scroll = this.clampScroll(widget.id(), widget.rows().size(), rows);
        for (int i = 0; i < rows && scroll + i < widget.rows().size(); i++) {
            final int index = scroll + i;
            final int top = rect.y() + HEADER_H + 3 + i * ROW_H;
            final boolean picked = index == widget.number(4) - 1;
            this.row(g, skin, rect, top, mouseX, mouseY, picked);
            final String[] cells = widget.rows().get(index).split("\t", -1);
            int x = rect.x() + 2;
            for (int c = 0; c < widths.length; c++) {
                if (c < cells.length) {
                    Draw.text(g, font, font.plainSubstrByWidth(cells[c], widths[c] - 6), x + 3, top + 2,
                            skin.listRowText(picked));
                }
                x += widths[c];
            }
        }
        if (widget.rows().size() > rows) {
            this.thumb(g, skin, new UiLayout.Rect(widget.id(), rect.x(), rect.y() + HEADER_H + 2, rect.w(),
                    rect.h() - HEADER_H - 2), scroll, rows, widget.rows().size());
        }
    }

    /** How wide each column of a table is: as wide as its header and its widest cells need, shared to fit. */
    static int[] columnWidths(final Font font, final UiWindowPayload.Widget widget, final int room) {
        final int columns = Math.max(1, widget.details().size());
        final int[] wants = new int[columns];
        for (int c = 0; c < columns; c++) {
            wants[c] = font.width(c < widget.details().size() ? widget.details().get(c) : "") + 10;
        }
        for (int r = 0; r < Math.min(widget.rows().size(), 64); r++) {
            final String[] cells = widget.rows().get(r).split("\t", -1);
            for (int c = 0; c < Math.min(columns, cells.length); c++) {
                wants[c] = Math.max(wants[c], font.width(cells[c]) + 8);
            }
        }
        int total = 0;
        for (final int want : wants) {
            total += want;
        }
        final int[] widths = new int[columns];
        int given = 0;
        for (int c = 0; c < columns; c++) {
            widths[c] = c == columns - 1 ? room - given : wants[c] * room / Math.max(1, total);
            given += widths[c];
        }
        return widths;
    }

    /* A tree: each node under the one it hangs from, a fold mark before a node with any under it. */
    private void tree(final GuiGraphics g, final Font font, final OsSkin skin, final UiWindowPayload.Widget widget,
                      final UiLayout.Rect rect, final int mouseX, final int mouseY) {
        skin.panel(g, rect.x(), rect.y(), rect.w(), rect.h());
        final List<SigmaTrees.Row> shown = SigmaTrees.shown(widget, this.ui);
        final int rows = Math.max(1, (rect.h() - 4) / ROW_H);
        final int scroll = this.clampScroll(widget.id(), shown.size(), rows);
        for (int i = 0; i < rows && scroll + i < shown.size(); i++) {
            final int node = shown.get(scroll + i).node();
            final int top = rect.y() + 2 + i * ROW_H;
            final int x = rect.x() + 3 + shown.get(scroll + i).depth() * 10;
            if (shown.get(scroll + i).children()) {
                SigmaGlyphs.expander(g, skin, x, top + 1, !this.ui.isFolded(widget.id(), node));
            }
            final String said = widget.row(node - 1);
            final boolean picked = node == widget.number(4);
            final int textX = x + SigmaGlyphs.MARK + 3;
            if (picked) {
                g.fill(textX - 1, top, textX + font.width(said) + 2, top + ROW_H - 1,
                        this.ui.focused == widget.id() || skin.form() != OsSkin.Form.FLAT ? skin.listSelect()
                                : skin.listHover());
            }
            Draw.text(g, font, said, textX, top + 2, skin.listRowText(picked));
        }
        if (shown.size() > rows) {
            this.thumb(g, skin, rect, scroll, rows, shown.size());
        }
    }

    private void menuBar(final GuiGraphics g, final Font font, final OsSkin skin, final UiWindowPayload.Widget widget,
                         final UiLayout.Rect rect, final int mouseX, final int mouseY) {
        for (final SigmaMenus.Title title : SigmaMenus.titles(font, widget, rect)) {
            final boolean open = this.ui.popup == SigmaUiState.PopupKind.MENU
                    && this.ui.popupOwner == widget.id() && this.ui.popupMenu.equals(title.name());
            final boolean over = mouseX >= title.x() && mouseX < title.x() + title.w()
                    && mouseY >= rect.y() && mouseY < rect.y() + rect.h();
            if (open || over && skin.form() == OsSkin.Form.FLAT) {
                g.fill(title.x(), rect.y(), title.x() + title.w(), rect.y() + rect.h(),
                        open ? skin.listSelect() : skin.listHover());
            }
            Draw.text(g, font, title.name(), title.x() + 5, rect.y() + (rect.h() - 7) / 2, skin.listRowText(open));
        }
    }

    private void statusBar(final GuiGraphics g, final Font font, final OsSkin skin,
                           final UiWindowPayload.Widget widget, final UiLayout.Rect rect, final int middle) {
        skin.statusBar(g, rect.x(), rect.y(), rect.w(), rect.h());
        Draw.text(g, font, widget.text(), rect.x() + 4, middle, skin.text());
        int right = rect.x() + rect.w() - 2;
        for (int i = widget.rows().size() - 1; i >= 0; i--) {
            final String section = widget.rows().get(i);
            final int wide = font.width(section) + 10;
            if (skin.form() != OsSkin.Form.FLAT) {
                skin.field(g, right - wide, rect.y() + 1, wide, rect.h() - 2, false);
            }
            Draw.text(g, font, section, right - wide + 5, middle, skin.text());
            right -= wide + 2;
        }
    }

    private void image(final GuiGraphics g, final Font font, final OsSkin skin, final UiWindowPayload.Widget widget,
                       final UiLayout.Rect rect) {
        skin.field(g, rect.x(), rect.y(), rect.w(), rect.h(), false);
        final Optional<PixImage> picture = SigmaImages.get(this.host, widget.text());
        if (picture.isPresent()) {
            Draw.pushScissor(g, rect.x() + 1, rect.y() + 1, rect.x() + rect.w() - 1, rect.y() + rect.h() - 1);
            SigmaImages.paint(g, picture.get(), rect.x() + 1, rect.y() + 1, rect.w() - 2, rect.h() - 2);
            Draw.popScissor(g);
        } else {
            Draw.textCentered(g, font, GameText.resolve(SigmaWindowTexts.NO_PICTURE), rect.x() + rect.w() / 2,
                    rect.y() + (rect.h() - 7) / 2, skin.dim());
        }
    }

    /* A chart: faint lines across, its numbers joined by a line in the accent, its caption and newest number under. */
    private void chart(final GuiGraphics g, final Font font, final OsSkin skin, final UiWindowPayload.Widget widget,
                       final UiLayout.Rect rect) {
        skin.field(g, rect.x(), rect.y(), rect.w(), rect.h(), false);
        final int plotTop = rect.y() + 4;
        final int plotBottom = rect.y() + rect.h() - 14;
        final int grid = SigmaWidgetPalette.get().chartGrid();
        for (int i = 1; i <= 3; i++) {
            final int y = plotTop + (plotBottom - plotTop) * i / 4;
            g.fill(rect.x() + 3, y, rect.x() + rect.w() - 3, y + 1, grid);
        }
        final double least = widget.number(2);
        final double most = Math.max(least + 1, widget.number(3));
        final List<String> points = widget.rows();
        int lastX = -1;
        int lastY = -1;
        for (int i = 0; i < points.size(); i++) {
            final double value = parse(points.get(i));
            final int x = rect.x() + 4 + (points.size() == 1 ? 0 : (rect.w() - 9) * i / (points.size() - 1));
            final int y = plotBottom - (int) Math.round((plotBottom - plotTop) * Math.clamp((value - least)
                    / (most - least), 0, 1));
            if (lastX >= 0) {
                SigmaCanvas.line(g, lastX, lastY, x, y, skin.accent());
            }
            lastX = x;
            lastY = y;
        }
        Draw.text(g, font, font.plainSubstrByWidth(widget.text(), rect.w() - 40), rect.x() + 4, plotBottom + 3,
                skin.text());
        if (!points.isEmpty()) {
            final String newest = points.getLast();
            Draw.text(g, font, newest, rect.x() + rect.w() - 4 - font.width(newest), plotBottom + 3, skin.text());
        }
    }

    /* A log: its newest lines in sight unless the player scrolled up, the time and the level of each picked out. */
    private void log(final GuiGraphics g, final Font font, final OsSkin skin, final UiWindowPayload.Widget widget,
                     final UiLayout.Rect rect) {
        skin.field(g, rect.x(), rect.y(), rect.w(), rect.h(), false);
        final int rows = Math.max(1, (rect.h() - 4) / (font.lineHeight + 1));
        final int most = Math.max(0, widget.rows().size() - rows);
        final int top = this.ui.logsHeld.contains(widget.id()) ? Math.min(this.ui.scroll(widget.id()), most) : most;
        // Where it shows from is written back, so the wheel moves from there; scrolled back down, it follows again.
        this.ui.scrolled.put(widget.id(), top);
        if (top >= most) {
            this.ui.logsHeld.remove(widget.id());
        }
        final SigmaWidgetPalette.Colours colours = SigmaWidgetPalette.get();
        for (int i = 0; i < rows && top + i < widget.rows().size(); i++) {
            final String line = widget.rows().get(top + i);
            final int y = rect.y() + 3 + i * (font.lineHeight + 1);
            int x = rect.x() + 4;
            final String[] words = line.split(" ", 3);
            int from = 0;
            if (words.length > 1 && words[0].matches("\\d{1,2}:\\d\\d(:\\d\\d)?")) {
                Draw.text(g, font, words[0], x, y, colours.logTime());
                x += font.width(words[0] + " ");
                from = 1;
            }
            String rest = line.substring(Math.min(line.length(), from == 0 ? 0 : words[0].length() + 1));
            final String level = from < words.length ? words[from] : "";
            final int levelColour = switch (level) {
                case "INFO", "DEBUG" -> colours.logTime();
                case "WARN", "WARNING" -> colours.logWarn();
                case "ERROR", "FATAL" -> colours.logError();
                default -> 0;
            };
            if (levelColour != 0) {
                Draw.text(g, font, level, x, y, levelColour);
                x += font.width(level + " ");
                rest = rest.substring(Math.min(rest.length(), level.length() + 1));
            }
            Draw.text(g, font, font.plainSubstrByWidth(rest, Math.max(0, rect.x() + rect.w() - 4 - x)), x, y,
                    skin.text());
        }
        if (widget.rows().size() > rows) {
            this.thumb(g, skin, rect, top, rows, widget.rows().size());
        }
    }

    /** One slot of an item, drawn as the game draws items, with how many when that is more than one. */
    void slot(final GuiGraphics g, final Font font, final OsSkin skin, final int x, final int y, final String item,
              final int amount, final boolean over) {
        skin.field(g, x, y, SLOT, SLOT, over);
        final ItemStack stack = this.stackOf(item);
        if (!stack.isEmpty()) {
            g.renderItem(stack, x + 1, y + 1);
            g.renderItemDecorations(font, stack, x + 1, y + 1, amount > 1 ? String.valueOf(amount) : null);
        }
    }

    private void picker(final GuiGraphics g, final Font font, final OsSkin skin, final UiWindowPayload.Widget widget,
                        final UiLayout.Rect rect, final int mouseX, final int mouseY) {
        final boolean typing = this.ui.focused == widget.id();
        skin.field(g, rect.x(), rect.y(), rect.w(), SEARCH_H, typing);
        final String filter = this.ui.filters.getOrDefault(widget.id(), "");
        if (filter.isEmpty() && !typing) {
            Draw.text(g, font, GameText.resolve(SigmaWindowTexts.SEARCH), rect.x() + 4, rect.y() + 3, skin.dim());
        } else {
            Draw.text(g, font, filter, rect.x() + 4, rect.y() + 3, skin.text());
            if (typing && blink()) {
                final int at = rect.x() + 4 + font.width(filter);
                g.fill(at, rect.y() + 2, at + 1, rect.y() + SEARCH_H - 2, skin.text());
            }
        }
        final List<Integer> shown = SigmaPickers.shown(widget, filter, this);
        final int across = Math.max(1, rect.w() / SLOT);
        final int down = Math.max(1, (rect.h() - SEARCH_H - 2) / SLOT);
        final int scroll = this.clampScroll(widget.id(), (shown.size() + across - 1) / across, down);
        for (int i = 0; i < across * down && scroll * across + i < shown.size(); i++) {
            final int index = shown.get(scroll * across + i);
            final int x = rect.x() + i % across * SLOT;
            final int y = rect.y() + SEARCH_H + 2 + i / across * SLOT;
            final boolean over = mouseX >= x && mouseX < x + SLOT && mouseY >= y && mouseY < y + SLOT;
            this.slot(g, font, skin, x, y, widget.row(index), 1, over);
            if (index + 1 == widget.number(4)) {
                Draw.outline(g, x, y, SLOT, SLOT, skin.accent());
            }
        }
    }

    /* An operation: what was asked, its first word in the accent; how it stands, how far, where; and a bar. */
    private void operation(final GuiGraphics g, final Font font, final OsSkin skin,
                           final UiWindowPayload.Widget widget, final UiLayout.Rect rect) {
        skin.panel(g, rect.x(), rect.y(), rect.w(), rect.h());
        final String statement = widget.text();
        final int space = statement.indexOf(' ');
        final String verb = space < 0 ? statement : statement.substring(0, space);
        Draw.text(g, font, verb, rect.x() + 5, rect.y() + 4, skin.accent());
        Draw.text(g, font, font.plainSubstrByWidth(space < 0 ? "" : statement.substring(space),
                rect.w() - 12 - font.width(verb)), rect.x() + 5 + font.width(verb), rect.y() + 4, skin.text());
        final int line = rect.y() + 15;
        final String state = widget.row(0);
        final String done = GameText.resolve(SigmaWindowTexts.DONE_OF.with(widget.number(1), widget.number(3)));
        final String where = widget.row(1);
        Draw.text(g, font, state, rect.x() + 5, line, skin.accent());
        // How far it has gone sits in the middle when the three fit, and takes where it runs's place when they do not.
        final boolean all = font.width(state) + font.width(done) + font.width(where) + 24 <= rect.w();
        if (all) {
            Draw.textCentered(g, font, done, rect.x() + rect.w() / 2, line, skin.text());
            Draw.text(g, font, where, rect.x() + rect.w() - 5 - font.width(where), line, skin.text());
        } else if (font.width(state) + font.width(done) + 16 <= rect.w()) {
            Draw.text(g, font, done, rect.x() + rect.w() - 5 - font.width(done), line, skin.text());
        }
        this.bar(g, skin, new UiLayout.Rect(widget.id(), rect.x() + 5, line + 11, rect.w() - 10, 7),
                widget.number(1), 0, Math.max(1, widget.number(3)));
    }

    private void generic(final GuiGraphics g, final Font font, final OsSkin skin,
                         final UiWindowPayload.Widget widget, final UiLayout.Rect rect, final int mouseX,
                         final int mouseY) {
        final IComponentRenderer renderer = this.rendererOf(widget);
        if (renderer != null) {
            try {
                renderer.draw(g, font, skin, rect.x(), rect.y(), rect.w(), rect.h(), this.dataOf(widget), mouseX,
                        mouseY);
                return;
            } catch (final RuntimeException fault) {
                this.fail(widget, fault);
            }
        }
        final TextKey why;
        final ComponentKind kind = ComponentKinds.find(widget.text());
        if (this.failed.contains(widget.id())) {
            why = SigmaWindowTexts.COULD_NOT_DRAW;
        } else if (kind != null && kind.reachesOutside() && ComponentRenderers.get(widget.text()) != null) {
            why = SigmaWindowTexts.OFF_HERE;
        } else {
            why = SigmaWindowTexts.NEEDS;
        }
        placeholder(g, font, rect, GameText.resolve(why.with(widget.text())));
    }

    /** The mark a component nothing on this game draws shows: hatched, naming why. */
    static void placeholder(final GuiGraphics g, final Font font, final UiLayout.Rect rect, final String said) {
        final SigmaWidgetPalette.Colours colours = SigmaWidgetPalette.get();
        g.fill(rect.x(), rect.y(), rect.x() + rect.w(), rect.y() + rect.h(), colours.placeholder());
        Draw.pushScissor(g, rect.x(), rect.y(), rect.x() + rect.w(), rect.y() + rect.h());
        for (int d = -rect.h(); d < rect.w(); d += 6) {
            SigmaCanvas.line(g, rect.x() + d, rect.y() + rect.h(), rect.x() + d + rect.h(), rect.y(),
                    colours.placeholderHatch());
        }
        Draw.popScissor(g);
        Draw.outline(g, rect.x(), rect.y(), rect.w(), rect.h(), colours.placeholderHatch());
        Draw.textCentered(g, font, font.plainSubstrByWidth(said, rect.w() - 6), rect.x() + rect.w() / 2,
                rect.y() + (rect.h() - 7) / 2, colours.placeholderText());
    }

    /* The highlight under a row of a list, a table or a tree: picked, or under the pointer. */
    private void row(final GuiGraphics g, final OsSkin skin, final UiLayout.Rect rect, final int top,
                     final int mouseX, final int mouseY, final boolean picked) {
        final boolean over = mouseX >= rect.x() && mouseX < rect.x() + rect.w() && mouseY >= top
                && mouseY < top + ROW_H;
        skin.listRow(g, rect.x() + 2, top, rect.w() - 4, ROW_H, over, picked);
    }

    private void thumb(final GuiGraphics g, final OsSkin skin, final UiLayout.Rect rect, final int scroll,
                       final int rows, final int total) {
        final int span = rect.h() - 4;
        final int thumb = Math.max(6, span * rows / total);
        final int top = rect.y() + 2 + (span - thumb) * scroll / Math.max(1, total - rows);
        skin.scrollThumb(g, rect.x() + rect.w() - 5, top, 3, thumb);
    }

    /* How far a list is scrolled, kept within its rows, and written back so the wheel works from what is shown. */
    private int clampScroll(final long id, final int total, final int rows) {
        final int scroll = Math.clamp(this.ui.scroll(id), 0, Math.max(0, total - rows));
        this.ui.scrolled.put(id, scroll);
        return scroll;
    }

    /** A text's end that fits in that width, which is what a box being typed in shows. */
    static String tail(final Font font, final String said, final int width) {
        String shown = said;
        while (!shown.isEmpty() && font.width(shown) > width) {
            shown = shown.substring(1);
        }
        return shown;
    }

    /** A text broken into lines no wider than that width, at its own line breaks and between words where it can. */
    static List<String> wrap(final Font font, final String text, final int width) {
        final List<String> lines = new ArrayList<>();
        for (final String paragraph : text.split("\n", -1)) {
            String left = paragraph;
            while (font.width(left) > width && left.length() > 1) {
                int cut = font.plainSubstrByWidth(left, width).length();
                final int space = left.lastIndexOf(' ', cut);
                cut = space > 0 ? space + 1 : Math.max(1, cut);
                lines.add(left.substring(0, cut));
                left = left.substring(cut);
            }
            lines.add(left);
        }
        return lines;
    }

    private static boolean blink() {
        return System.currentTimeMillis() / 500 % 2 == 0;
    }

    private static double parse(final String number) {
        try {
            return Double.parseDouble(number);
        } catch (final NumberFormatException unreadable) {
            return 0;
        }
    }

    /** A component's value as it was last read, and the letters it was read from. */
    private record Decoded(String text, Object value) {
    }
}
