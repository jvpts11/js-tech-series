/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.gui.layout.UiLayout;
import dev.jstech.computers.operation.payload.UiEventPayload;
import dev.jstech.computers.operation.payload.UiWindowPayload;
import dev.jstech.computers.vm.program.UiWidgets;
import dev.jstech.core.gui.TextScreen;
import dev.jstech.core.text.GameText;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * The windows of Σ# programs on a machine that only has its terminal, drawn in letters as the full-screen programs of
 * the age drew their dialogs: the machine's ground in blue, a menu bar along the top line, the window in the middle
 * in a double frame with its shadow, and the status line along the bottom.
 *
 * <p>The window in front takes the keyboard. Tab moves between its widgets, the arrows move inside a list, a group of
 * choices, a slider or a number, Enter or Space presses, Escape shuts what is open and then the window, and F10 opens
 * the menu bar. The mouse does the same where a terminal has one. Only what a screen of letters can show is ever here:
 * a program on such a machine is told it cannot make the rest.
 */
public final class SigmaTextWindows {

    private final BlockPos host;
    /** The windows open, the one in front last, by program and window. */
    private final Map<String, UiWindowPayload> open = new LinkedHashMap<>();
    private final SigmaUiState ui = new SigmaUiState();
    /** Where each widget of the window in front was drawn, in cells, at the last frame. */
    private final Map<Long, UiLayout.Rect> where = new HashMap<>();
    /** The dialog the window in front has up, typed into on the terminal, and what is typed in it. */
    private long dialog;
    private String dialogPath = "";
    private UiLayout.Rect menuRect;

    /** The terminal's sixteen colours, by what each part of a window is drawn in. */
    private static final int DESK = TextScreen.BLUE;
    private static final int FACE = TextScreen.GREY;
    private static final int INK = TextScreen.BLACK;
    private static final int FIELD = TextScreen.BLUE;
    private static final int FIELD_INK = TextScreen.WHITE;
    private static final int PICKED = TextScreen.CYAN;
    private static final int STATUS = TextScreen.CYAN;
    /** What the cursor in a box of letters is drawn as. */
    private static final String CARET = "_";
    /** The widgets the keyboard visits in turn. */
    private static final Set<String> FOCUSABLE = Set.of("Button", "TextBox", "TextArea", "CheckBox", "NumberBox",
            "Slider", "RadioGroup", "ComboBox", "TabView", "ListBox", "Table", "TreeView", "LogView");
    /** The widgets a screen of letters draws along its own edges rather than in the window. */
    private static final Set<String> EDGES = Set.of("MenuBar", "StatusBar");

    /** The machine whose terminal is on the screen, which the windows arriving are for. */
    @Nullable
    private static BlockPos showing;
    @Nullable
    private static SigmaTextWindows current;

    private SigmaTextWindows(final BlockPos host) {
        this.host = host;
    }

    /** Says which machine's terminal is on the screen, or null when none is; its windows start afresh. */
    public static void showing(@Nullable final BlockPos host) {
        // The same terminal laid out again, as a resize does, keeps the windows it has.
        if (host != null && host.equals(showing) && current != null) {
            return;
        }
        showing = host;
        current = host == null ? null : new SigmaTextWindows(host);
    }

    /** Takes a window arriving for the terminal on the screen; false when it is for a desktop instead. */
    public static boolean take(final UiWindowPayload payload) {
        if (current == null || !payload.hostPos().equals(showing)) {
            return false;
        }
        current.accept(payload);
        return true;
    }

    /** The windows of the terminal on the screen, or null when no program there has one open. */
    @Nullable
    public static SigmaTextWindows active(final BlockPos host) {
        return current != null && current.host.equals(host) && !current.open.isEmpty() ? current : null;
    }

    private void accept(final UiWindowPayload payload) {
        final String key = payload.program() + ":" + payload.window();
        if (!payload.open()) {
            this.open.remove(key);
            this.ui.closePopup();
            return;
        }
        final boolean known = this.open.containsKey(key);
        this.open.put(key, payload);
        if (!known) {
            this.ui.focused = 0;
        }
        for (final UiWindowPayload.Widget widget : payload.widgets()) {
            final String typed = this.ui.typing.get(widget.id());
            final String held = "TextArea".equals(widget.kind()) ? widget.joined() : widget.text();
            if (typed != null && !"NumberBox".equals(widget.kind()) && !typed.equals(held)) {
                this.ui.typing.remove(widget.id());
            }
            final boolean fileDialog = "OpenFileDialog".equals(widget.kind())
                    || "SaveFileDialog".equals(widget.kind());
            if (fileDialog && widget.epoch() != this.ui.dialogsShown.getOrDefault(widget.id(), 0)) {
                this.ui.dialogsShown.put(widget.id(), widget.epoch());
                this.dialog = widget.id();
                final String start = widget.row(0);
                final String name = widget.row(2);
                this.dialogPath = name.isEmpty() ? start : start.isEmpty() || start.endsWith("\\")
                        || start.endsWith("/") ? start + name : start + "/" + name;
            }
        }
    }

    /** The window in front. */
    private UiWindowPayload front() {
        UiWindowPayload last = null;
        for (final UiWindowPayload window : this.open.values()) {
            last = window;
        }
        return last;
    }

    /** Paints the windows over the whole of the terminal's glass. */
    public void paint(final TextScreen screen) {
        final UiWindowPayload window = this.front();
        final int ground = TextScreen.cga(DESK);
        screen.fill(0, 0, screen.columns(), screen.rows(), ground, ground);
        this.where.clear();
        this.menuRect = null;
        if (window == null) {
            return;
        }
        final UiWindowPayload.Widget bar = this.first(window, "MenuBar");
        final UiWindowPayload.Widget status = this.first(window, "StatusBar");
        final int top = bar == null ? 0 : 1;
        final int bottom = screen.rows() - (status == null ? 0 : 1);
        final List<UiLayout.Box> boxes = this.boxes(window);
        final UiLayout.Box root = boxes.stream().filter(box -> box.parent() == 0).findFirst().orElse(null);
        final Map<Long, List<UiLayout.Box>> children = new HashMap<>();
        for (final UiLayout.Box box : boxes) {
            if (box.parent() != 0) {
                children.computeIfAbsent(box.parent(), id -> new ArrayList<>()).add(box);
            }
        }
        final int innerW = root == null ? 20 : UiLayout.natural(root, children, true, UiLayout.CELLS);
        final int innerH = root == null ? 3 : UiLayout.natural(root, children, false, UiLayout.CELLS);
        final int w = Math.clamp(innerW + 4, Math.min(24, screen.columns()), screen.columns() - 2);
        final int h = Math.clamp(innerH + 4, Math.min(5, bottom - top), Math.max(5, bottom - top - 1));
        final int x = (screen.columns() - w) / 2;
        final int y = top + Math.max(0, (bottom - top - h) / 2);
        screen.shadow(x, y, w, h, TextScreen.cga(TextScreen.DARK_GREY), TextScreen.cga(TextScreen.BLACK));
        screen.box(x, y, w, h, TextScreen.cga(INK), TextScreen.cga(FACE), true);
        final String title = " " + window.title() + " ";
        screen.put(x + Math.max(1, (w - title.length()) / 2), y, clip(title, w - 2), TextScreen.cga(INK),
                TextScreen.cga(FACE));
        for (final UiLayout.Rect rect : UiLayout.lay(boxes, x + 1, y + 1, w - 2, h - 2, this.ui.scrolled,
                UiLayout.CELLS)) {
            this.where.put(rect.id(), rect);
        }
        this.ensureFocus(window);
        for (final UiWindowPayload.Widget widget : window.widgets()) {
            final UiLayout.Rect rect = this.where.get(widget.id());
            if (rect != null && widget.shows() && this.inView(window, widget, rect)) {
                this.draw(screen, widget, rect);
            }
        }
        if (bar != null) {
            this.menuBar(screen, bar);
        }
        if (status != null) {
            this.statusLine(screen, status);
        }
        this.popup(screen, window);
        if (this.dialog != 0) {
            this.fileDialog(screen, window);
        }
    }

    /* Whether a widget inside a scroll view lies wholly within its frame, which is all a screen of letters shows. */
    private boolean inView(final UiWindowPayload window, final UiWindowPayload.Widget widget,
                           final UiLayout.Rect rect) {
        for (UiWindowPayload.Widget at = this.widget(window, widget.parent()); at != null;
             at = this.widget(window, at.parent())) {
            final UiLayout.Rect view = this.where.get(at.id());
            if ("ScrollView".equals(at.kind()) && view != null
                    && (rect.y() <= view.y() || rect.y() + rect.h() >= view.y() + view.h())) {
                return false;
            }
        }
        return true;
    }

    /** What each widget is in cells when nobody says otherwise; the menu bar and status line live on the edges. */
    private List<UiLayout.Box> boxes(final UiWindowPayload window) {
        final List<UiLayout.Box> boxes = new ArrayList<>();
        for (final UiWindowPayload.Widget widget : window.widgets()) {
            if (EDGES.contains(widget.kind())) {
                continue;
            }
            final int placedX = widget.x() == UiLayout.LAID_OUT ? UiLayout.LAID_OUT : widget.x() / 6;
            final int placedY = widget.y() == UiLayout.LAID_OUT ? UiLayout.LAID_OUT : widget.y() / 10;
            boxes.add(new UiLayout.Box(widget.id(), widget.parent(), widget.kind(), widget.weight(),
                    wideOf(widget), tallOf(widget), widget.width() / 6, widget.height() / 10, widget.number(0),
                    placedX, placedY, widget.number(4)));
        }
        return boxes;
    }

    private static int wideOf(final UiWindowPayload.Widget widget) {
        return switch (widget.kind()) {
            case "Label" -> widget.text().length();
            case "Button" -> widget.text().length() + 4;
            case "TextBox" -> Math.max(12, widget.text().length() + 2);
            case "TextArea", "Table", "LogView" -> 30;
            case "CheckBox" -> widget.text().length() + 4;
            case "ProgressBar" -> 20;
            case "NumberBox" -> 9;
            case "Slider" -> 16;
            case "RadioGroup" -> radiosWide(widget);
            case "ComboBox" -> longest(widget.rows()) + 4;
            case "TabView" -> tabsWide(widget);
            case "GroupBox" -> widget.text().length() + 4;
            case "ScrollView", "ListBox", "TreeView" -> 20;
            default -> 0;
        };
    }

    private static int tallOf(final UiWindowPayload.Widget widget) {
        return switch (widget.kind()) {
            case "TextArea" -> 4;
            case "RadioGroup" -> widget.ticked() ? 1 : Math.max(1, widget.rows().size());
            case "TabView", "GroupBox" -> 3;
            case "ScrollView", "ListBox", "Table", "TreeView" -> 6;
            case "LogView" -> 5;
            default -> 1;
        };
    }

    private static int radiosWide(final UiWindowPayload.Widget widget) {
        if (!widget.ticked()) {
            return longest(widget.rows()) + 4;
        }
        int total = 0;
        for (final String one : widget.rows()) {
            total += one.length() + 6;
        }
        return total;
    }

    private static int tabsWide(final UiWindowPayload.Widget widget) {
        int total = 0;
        for (final String one : widget.rows()) {
            total += one.length() + 4;
        }
        return total;
    }

    private static int longest(final List<String> said) {
        int most = 0;
        for (final String one : said) {
            most = Math.max(most, one.length());
        }
        return most;
    }

    private void draw(final TextScreen s, final UiWindowPayload.Widget widget, final UiLayout.Rect r) {
        final boolean focused = this.ui.focused == widget.id();
        final int ink = TextScreen.cga(widget.answers() ? INK : TextScreen.DARK_GREY);
        final int face = TextScreen.cga(FACE);
        final int field = TextScreen.cga(FIELD);
        final int fieldInk = TextScreen.cga(FIELD_INK);
        switch (widget.kind()) {
            case "Label" -> s.put(r.x(), r.y(), clip(widget.text(), r.w()), ink, face);
            case "Button" -> {
                final String said = clip("< " + widget.text() + " >", r.w());
                s.put(r.x(), r.y(), said, focused ? fieldInk : ink, focused ? field : face);
            }
            case "TextBox" -> {
                final String said = this.ui.textOf(widget) + (focused ? CARET : "");
                s.fill(r.x(), r.y(), r.w(), 1, fieldInk, field);
                s.put(r.x(), r.y(), tail(said, r.w()), fieldInk, field);
            }
            case "TextArea" -> this.area(s, widget, r, focused);
            case "CheckBox" -> s.put(r.x(), r.y(), clip((widget.ticked() ? "[X] " : "[ ] ") + widget.text(), r.w()),
                    focused ? fieldInk : ink, focused ? field : face);
            case "ProgressBar" -> s.put(r.x(), r.y(), bar(widget.number(1), widget.number(2), widget.number(3),
                    r.w()), ink, face);
            case "NumberBox" -> {
                final String typed = this.ui.typing.get(widget.id());
                final String value = typed != null ? typed + CARET : String.valueOf(widget.number(1));
                s.put(r.x(), r.y(), clip("[" + pad(value, Math.max(1, r.w() - 4)) + "]", r.w() - 2),
                        focused ? fieldInk : ink, focused ? field : face);
                s.put(r.x() + r.w() - 2, r.y(), "▲▼", ink, face);
            }
            case "Slider" -> {
                final String track = bar(widget.number(1), widget.number(2), widget.number(3), r.w());
                s.put(r.x(), r.y(), track, focused ? fieldInk : field, face);
            }
            case "RadioGroup" -> this.radios(s, widget, r, focused);
            case "ComboBox" -> {
                final int picked = widget.number(4);
                final String said = picked >= 1 ? widget.row(picked - 1) : "";
                s.fill(r.x(), r.y(), r.w(), 1, fieldInk, field);
                s.put(r.x(), r.y(), "[" + pad(said, Math.max(1, r.w() - 4)) + "▼]", fieldInk, field);
            }
            case "TabView" -> this.tabs(s, widget, r, focused);
            case "GroupBox", "ScrollView" -> {
                s.box(r.x(), r.y(), r.w(), r.h(), ink, face, false);
                if (!widget.text().isEmpty()) {
                    s.put(r.x() + 2, r.y(), clip(" " + widget.text() + " ", r.w() - 4), ink, face);
                }
            }
            case "ListBox", "LogView" -> this.lines(s, widget, r, focused, widget.rows());
            case "Table" -> this.table(s, widget, r, focused);
            case "TreeView" -> this.tree(s, widget, r, focused);
            default -> {
                // A row or a column is only where its widgets are.
            }
        }
    }

    private void area(final TextScreen s, final UiWindowPayload.Widget widget, final UiLayout.Rect r,
                      final boolean focused) {
        final int field = TextScreen.cga(FIELD);
        final int fieldInk = TextScreen.cga(FIELD_INK);
        s.fill(r.x(), r.y(), r.w(), r.h(), fieldInk, field);
        final List<String> lines = wrap(this.ui.textOf(widget) + (focused ? CARET : ""), r.w());
        final int first = Math.max(0, lines.size() - r.h());
        for (int i = 0; i < r.h() && first + i < lines.size(); i++) {
            s.put(r.x(), r.y() + i, lines.get(first + i), fieldInk, field);
        }
    }

    private void radios(final TextScreen s, final UiWindowPayload.Widget widget, final UiLayout.Rect r,
                        final boolean focused) {
        int at = r.x();
        for (int i = 0; i < widget.rows().size(); i++) {
            final String said = (widget.number(4) == i + 1 ? "(•) " : "( ) ") + widget.row(i);
            final boolean lit = focused && Math.max(1, widget.number(4)) == i + 1;
            final int x = widget.ticked() ? at : r.x();
            final int y = widget.ticked() ? r.y() : r.y() + i;
            s.put(x, y, said, TextScreen.cga(lit ? FIELD_INK : INK), TextScreen.cga(lit ? FIELD : FACE));
            at += said.length() + 2;
        }
    }

    private void tabs(final TextScreen s, final UiWindowPayload.Widget widget, final UiLayout.Rect r,
                      final boolean focused) {
        int at = r.x();
        for (int i = 0; i < widget.rows().size(); i++) {
            final boolean picked = widget.number(4) == i + 1;
            final String said = picked ? "[ " + widget.row(i) + " ]" : "  " + widget.row(i) + "  ";
            s.put(at, r.y(), said, TextScreen.cga(picked && focused ? FIELD_INK : INK),
                    TextScreen.cga(picked && focused ? FIELD : FACE));
            at += said.length();
        }
    }

    private void lines(final TextScreen s, final UiWindowPayload.Widget widget, final UiLayout.Rect r,
                       final boolean focused, final List<String> rows) {
        s.box(r.x(), r.y(), r.w(), r.h(), TextScreen.cga(focused ? TextScreen.WHITE : INK), TextScreen.cga(FACE),
                false);
        final int shown = Math.max(1, r.h() - 2);
        final boolean log = "LogView".equals(widget.kind());
        final int first = log ? Math.max(0, rows.size() - shown) : this.scrolledTo(widget, rows.size(), shown);
        for (int i = 0; i < shown && first + i < rows.size(); i++) {
            final boolean picked = !log && first + i == widget.number(4) - 1;
            final String said = pad(rows.get(first + i), r.w() - 2);
            s.put(r.x() + 1, r.y() + 1 + i, said, TextScreen.cga(INK), TextScreen.cga(picked ? PICKED : FACE));
        }
    }

    private void table(final TextScreen s, final UiWindowPayload.Widget widget, final UiLayout.Rect r,
                       final boolean focused) {
        s.box(r.x(), r.y(), r.w(), r.h(), TextScreen.cga(focused ? TextScreen.WHITE : INK), TextScreen.cga(FACE),
                false);
        final int[] widths = cellWidths(widget, r.w() - 2);
        int at = r.x() + 1;
        for (int c = 0; c < widths.length; c++) {
            final String header = c < widget.details().size() ? widget.details().get(c) : "";
            s.put(at, r.y(), clip(header, widths[c] - 1), TextScreen.cga(INK), TextScreen.cga(FACE));
            if (c > 0) {
                s.put(at - 1, r.y(), "┬", TextScreen.cga(INK), TextScreen.cga(FACE));
                for (int row = 1; row < r.h() - 1; row++) {
                    s.put(at - 1, r.y() + row, "│", TextScreen.cga(INK), TextScreen.cga(FACE));
                }
                s.put(at - 1, r.y() + r.h() - 1, "┴", TextScreen.cga(INK), TextScreen.cga(FACE));
            }
            at += widths[c];
        }
        final int shown = Math.max(1, r.h() - 2);
        final int first = this.scrolledTo(widget, widget.rows().size(), shown);
        for (int i = 0; i < shown && first + i < widget.rows().size(); i++) {
            final boolean picked = first + i == widget.number(4) - 1;
            final String[] cells = widget.rows().get(first + i).split("\t", -1);
            int x = r.x() + 1;
            for (int c = 0; c < widths.length; c++) {
                final int ground = TextScreen.cga(picked ? PICKED : FACE);
                s.put(x, r.y() + 1 + i, pad(c < cells.length ? cells[c] : "", widths[c] - (c + 1 < widths.length
                        ? 1 : 0)), TextScreen.cga(INK), ground);
                x += widths[c];
            }
        }
    }

    private static int[] cellWidths(final UiWindowPayload.Widget widget, final int room) {
        final int columns = Math.max(1, widget.details().size());
        final int[] wants = new int[columns];
        int total = 0;
        for (int c = 0; c < columns; c++) {
            wants[c] = (c < widget.details().size() ? widget.details().get(c).length() : 0) + 2;
            for (int r = 0; r < Math.min(widget.rows().size(), 64); r++) {
                final String[] cells = widget.rows().get(r).split("\t", -1);
                wants[c] = Math.max(wants[c], (c < cells.length ? cells[c].length() : 0) + 2);
            }
            total += wants[c];
        }
        final int[] widths = new int[columns];
        int given = 0;
        for (int c = 0; c < columns; c++) {
            widths[c] = c == columns - 1 ? room - given : Math.max(2, wants[c] * room / Math.max(1, total));
            given += widths[c];
        }
        return widths;
    }

    private void tree(final TextScreen s, final UiWindowPayload.Widget widget, final UiLayout.Rect r,
                      final boolean focused) {
        s.box(r.x(), r.y(), r.w(), r.h(), TextScreen.cga(focused ? TextScreen.WHITE : INK), TextScreen.cga(FACE),
                false);
        final List<SigmaTrees.Row> shownRows = SigmaTrees.shown(widget, this.ui);
        final int shown = Math.max(1, r.h() - 2);
        final int first = this.scrolledTo(widget, shownRows.size(), shown);
        for (int i = 0; i < shown && first + i < shownRows.size(); i++) {
            final SigmaTrees.Row row = shownRows.get(first + i);
            final String mark = row.children() ? (this.ui.isFolded(widget.id(), row.node()) ? "+ " : "- ") : "  ";
            final String said = pad("  ".repeat(row.depth()) + mark + widget.row(row.node() - 1), r.w() - 2);
            final boolean picked = row.node() == widget.number(4);
            s.put(r.x() + 1, r.y() + 1 + i, said, TextScreen.cga(INK), TextScreen.cga(picked ? PICKED : FACE));
        }
    }

    /* Keeps the picked row of a list in sight: scrolled as far as the player left it, and no further from it. */
    private int scrolledTo(final UiWindowPayload.Widget widget, final int total, final int shown) {
        int first = Math.clamp(this.ui.scroll(widget.id()), 0, Math.max(0, total - shown));
        final int picked = widget.number(4) - 1;
        if (picked >= 0 && picked < first) {
            first = picked;
        } else if (picked >= first + shown) {
            first = picked - shown + 1;
        }
        this.ui.scrolled.put(widget.id(), first);
        return first;
    }

    private void menuBar(final TextScreen s, final UiWindowPayload.Widget bar) {
        s.fill(0, 0, s.columns(), 1, TextScreen.cga(INK), TextScreen.cga(FACE));
        int at = 1;
        for (final String menu : menus(bar)) {
            final boolean open = this.ui.popup == SigmaUiState.PopupKind.MENU && this.ui.popupMenu.equals(menu);
            s.put(at, 0, " " + menu + " ", TextScreen.cga(open ? FIELD_INK : INK),
                    TextScreen.cga(open ? INK : FACE));
            at += menu.length() + 3;
        }
        this.where.put(bar.id(), new UiLayout.Rect(bar.id(), 0, 0, s.columns(), 1));
    }

    private static List<String> menus(final UiWindowPayload.Widget bar) {
        final List<String> named = new ArrayList<>();
        for (final String menu : bar.details()) {
            if (!named.contains(menu)) {
                named.add(menu);
            }
        }
        return named;
    }

    private void statusLine(final TextScreen s, final UiWindowPayload.Widget status) {
        final int row = s.rows() - 1;
        s.fill(0, row, s.columns(), 1, TextScreen.cga(INK), TextScreen.cga(STATUS));
        s.put(1, row, clip(status.text(), s.columns() - 2), TextScreen.cga(INK), TextScreen.cga(STATUS));
        int right = s.columns() - 1;
        for (int i = status.rows().size() - 1; i >= 0; i--) {
            final String section = status.rows().get(i);
            right -= section.length() + 4;
            s.put(right, row, section, TextScreen.cga(INK), TextScreen.cga(STATUS));
        }
    }

    /* The menu open over the window: a combo box's list, a menu bar's menu or a context menu, in a single frame. */
    private void popup(final TextScreen s, final UiWindowPayload window) {
        if (!this.ui.popupOpen()) {
            return;
        }
        final UiWindowPayload.Widget owner = this.widget(window, this.ui.popupOwner);
        if (owner == null) {
            this.ui.closePopup();
            return;
        }
        final List<Integer> entries = this.entries(owner);
        int x = this.ui.popupX;
        int y = this.ui.popupY;
        int w = 6;
        for (final int entry : entries) {
            w = Math.max(w, owner.row(entry).length() + 4);
        }
        final UiLayout.Rect ownerRect = this.where.get(owner.id());
        if (this.ui.popup == SigmaUiState.PopupKind.COMBO && ownerRect != null) {
            x = ownerRect.x();
            y = ownerRect.y() + 1;
            w = Math.max(w, ownerRect.w());
        } else if (this.ui.popup == SigmaUiState.PopupKind.MENU) {
            x = this.menuColumn(owner);
            y = 1;
        }
        final int h = Math.min(entries.size(), s.rows() - 2) + 2;
        x = Math.clamp(x, 0, Math.max(0, s.columns() - w));
        y = Math.clamp(y, 0, Math.max(0, s.rows() - h));
        s.box(x, y, w, h, TextScreen.cga(INK), TextScreen.cga(FACE), false);
        for (int i = 0; i < h - 2 && i < entries.size(); i++) {
            final String said = owner.row(entries.get(i));
            final boolean lit = i == this.ui.popupScroll;
            if (SigmaMenus.SEPARATOR.equals(said)) {
                s.put(x, y + 1 + i, "├" + "─".repeat(w - 2) + "┤", TextScreen.cga(INK), TextScreen.cga(FACE));
            } else {
                s.put(x + 1, y + 1 + i, pad(" " + said, w - 2), TextScreen.cga(lit ? FIELD_INK : INK),
                        TextScreen.cga(lit ? INK : FACE));
            }
        }
        this.menuRect = new UiLayout.Rect(owner.id(), x, y, w, h);
    }

    private int menuColumn(final UiWindowPayload.Widget bar) {
        int at = 1;
        for (final String menu : menus(bar)) {
            if (menu.equals(this.ui.popupMenu)) {
                return at;
            }
            at += menu.length() + 3;
        }
        return 1;
    }

    /* The entries an open popup lists, as their places in its owner's rows. */
    private List<Integer> entries(final UiWindowPayload.Widget owner) {
        final List<Integer> entries = new ArrayList<>();
        for (int i = 0; i < owner.rows().size(); i++) {
            if (this.ui.popup != SigmaUiState.PopupKind.MENU
                    || i < owner.details().size() && owner.details().get(i).equals(this.ui.popupMenu)) {
                entries.add(i);
            }
        }
        return entries;
    }

    /* A file dialog on a terminal is the line its path is typed on, with its two answers under it. */
    private void fileDialog(final TextScreen s, final UiWindowPayload window) {
        final UiWindowPayload.Widget shown = this.widget(window, this.dialog);
        if (shown == null) {
            this.dialog = 0;
            return;
        }
        final int w = Math.min(s.columns() - 4, 50);
        final int h = 7;
        final int x = (s.columns() - w) / 2;
        final int y = (s.rows() - h) / 2;
        s.shadow(x, y, w, h, TextScreen.cga(TextScreen.DARK_GREY), TextScreen.cga(TextScreen.BLACK));
        s.box(x, y, w, h, TextScreen.cga(INK), TextScreen.cga(FACE), true);
        final String title = " " + (shown.text().isEmpty() ? window.title() : shown.text()) + " ";
        s.put(x + Math.max(1, (w - title.length()) / 2), y, clip(title, w - 2), TextScreen.cga(INK),
                TextScreen.cga(FACE));
        final String label = GameText.resolve(SigmaWindowTexts.FILE_NAME);
        s.put(x + 2, y + 2, label, TextScreen.cga(INK), TextScreen.cga(FACE));
        final int fieldX = x + 3 + label.length();
        final int fieldW = x + w - 2 - fieldX;
        s.fill(fieldX, y + 2, fieldW, 1, TextScreen.cga(FIELD_INK), TextScreen.cga(FIELD));
        s.put(fieldX, y + 2, tail(this.dialogPath + CARET, fieldW), TextScreen.cga(FIELD_INK),
                TextScreen.cga(FIELD));
        final String ok = "< " + GameText.resolve(SigmaWindowTexts.OK) + " >";
        final String cancel = "< " + GameText.resolve(SigmaWindowTexts.CANCEL) + " >";
        s.put(x + w - 4 - ok.length() - cancel.length(), y + 4, ok, TextScreen.cga(FIELD_INK), TextScreen.cga(FIELD));
        s.put(x + w - 2 - cancel.length(), y + 4, cancel, TextScreen.cga(INK), TextScreen.cga(FACE));
    }

    // what the player does

    /** A key pressed at the terminal; true when the windows took it. */
    public boolean keyPressed(final int key, final int modifiers) {
        final UiWindowPayload window = this.front();
        if (window == null) {
            return false;
        }
        if (this.dialog != 0) {
            return this.dialogKey(key);
        }
        if (this.ui.popupOpen()) {
            return this.popupKey(window, key);
        }
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            this.send(window, "close", 0, "", 0);
            return true;
        }
        if (key == GLFW.GLFW_KEY_F10) {
            final UiWindowPayload.Widget bar = this.first(window, "MenuBar");
            final List<String> menus = bar == null ? List.of() : menus(bar);
            if (!menus.isEmpty()) {
                this.openMenu(bar, menus.getFirst());
            }
            return true;
        }
        if (key == GLFW.GLFW_KEY_TAB) {
            this.moveFocus(window, (modifiers & GLFW.GLFW_MOD_SHIFT) != 0 ? -1 : 1);
            return true;
        }
        final UiWindowPayload.Widget focused = this.widget(window, this.ui.focused);
        return focused != null && this.widgetKey(window, focused, key);
    }

    /** A character typed at the terminal; true when the windows took it. */
    public boolean charTyped(final char c) {
        final UiWindowPayload window = this.front();
        if (window == null || c < 32 || c == 127) {
            return false;
        }
        if (this.dialog != 0) {
            if (this.dialogPath.length() < UiWidgets.MOST_TEXT) {
                this.dialogPath += c;
            }
            return true;
        }
        final UiWindowPayload.Widget focused = this.widget(window, this.ui.focused);
        if (focused == null) {
            return true;
        }
        switch (focused.kind()) {
            case "TextBox" -> this.type(window, focused, this.ui.textOf(focused) + c, UiWidgets.MOST_TEXT);
            case "TextArea" -> this.type(window, focused, this.ui.textOf(focused) + c, UiWidgets.MOST_AREA);
            case "NumberBox" -> {
                final String typed = this.ui.typing.getOrDefault(focused.id(), "");
                if (Character.isDigit(c) || c == '-' && typed.isEmpty()) {
                    this.ui.typing.put(focused.id(), typed + c);
                }
            }
            case "Button", "CheckBox", "RadioGroup" -> {
                if (c == ' ') {
                    this.press(window, focused);
                }
            }
            default -> {
                // Nothing else takes letters.
            }
        }
        return true;
    }

    /** A click on a cell of the glass; true when the windows took it. */
    public boolean mouseClicked(final int column, final int row) {
        final UiWindowPayload window = this.front();
        if (window == null) {
            return false;
        }
        if (this.dialog != 0) {
            return true;
        }
        if (this.ui.popupOpen()) {
            this.clickPopup(window, column, row);
            return true;
        }
        UiWindowPayload.Widget hit = null;
        for (final UiWindowPayload.Widget widget : window.widgets()) {
            final UiLayout.Rect rect = this.where.get(widget.id());
            if (rect != null && widget.shows() && column >= rect.x() && column < rect.x() + rect.w()
                    && row >= rect.y() && row < rect.y() + rect.h() && !"Row".equals(widget.kind())
                    && !"Column".equals(widget.kind())) {
                hit = widget;
            }
        }
        if (hit == null || !hit.answers()) {
            return true;
        }
        this.clickWidget(window, hit, this.where.get(hit.id()), column, row);
        return true;
    }

    private void clickWidget(final UiWindowPayload window, final UiWindowPayload.Widget hit, final UiLayout.Rect r,
                             final int column, final int row) {
        if ("MenuBar".equals(hit.kind())) {
            int at = 1;
            for (final String menu : menus(hit)) {
                if (column >= at && column < at + menu.length() + 2) {
                    this.openMenu(hit, menu);
                    return;
                }
                at += menu.length() + 3;
            }
            return;
        }
        if (FOCUSABLE.contains(hit.kind())) {
            this.focusOn(window, hit.id());
        }
        switch (hit.kind()) {
            case "Button", "CheckBox" -> this.press(window, hit);
            case "NumberBox" -> {
                if (column >= r.x() + r.w() - 2) {
                    this.step(window, hit, column == r.x() + r.w() - 2 ? 1 : -1);
                }
            }
            case "Slider" -> this.send(window, "number", hit.id(), "", sliderValue(hit, column - r.x(), r.w()));
            case "RadioGroup" -> {
                int at = r.x();
                for (int i = 0; i < hit.rows().size(); i++) {
                    final int wide = hit.row(i).length() + 4;
                    final boolean on = hit.ticked() ? column >= at && column < at + wide : row == r.y() + i;
                    if (on) {
                        this.send(window, "select", hit.id(), "", i + 1);
                        return;
                    }
                    at += wide + 2;
                }
            }
            case "ComboBox" -> this.openCombo(hit);
            case "TabView" -> {
                int at = r.x();
                for (int i = 0; i < hit.rows().size(); i++) {
                    final int wide = hit.row(i).length() + 4;
                    if (row == r.y() && column >= at && column < at + wide) {
                        this.send(window, "select", hit.id(), "", i + 1);
                        return;
                    }
                    at += wide;
                }
            }
            case "ListBox", "Table" -> {
                final int index = this.ui.scroll(hit.id()) + row - r.y() - 1;
                if (row > r.y() && row < r.y() + r.h() - 1 && index < hit.rows().size()) {
                    this.send(window, "select", hit.id(), "", index + 1);
                }
            }
            case "TreeView" -> {
                final List<SigmaTrees.Row> shown = SigmaTrees.shown(hit, this.ui);
                final int index = this.ui.scroll(hit.id()) + row - r.y() - 1;
                if (row > r.y() && index >= 0 && index < shown.size()) {
                    final SigmaTrees.Row node = shown.get(index);
                    final int mark = r.x() + 1 + node.depth() * 2;
                    if (node.children() && column >= mark && column < mark + 2) {
                        this.ui.toggleFold(hit.id(), node.node());
                    } else {
                        this.send(window, "select", hit.id(), "", node.node());
                    }
                }
            }
            default -> {
                // A box of letters only takes the keyboard.
            }
        }
    }

    private boolean widgetKey(final UiWindowPayload window, final UiWindowPayload.Widget focused, final int key) {
        final boolean enter = key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER;
        final int along = key == GLFW.GLFW_KEY_DOWN || key == GLFW.GLFW_KEY_RIGHT ? 1
                : key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_LEFT ? -1 : 0;
        switch (focused.kind()) {
            case "Button", "CheckBox" -> {
                if (enter) {
                    this.press(window, focused);
                }
            }
            case "TextBox" -> {
                if (enter) {
                    this.send(window, "submit", focused.id(), this.ui.textOf(focused), 0);
                    this.ui.typing.remove(focused.id());
                } else if (key == GLFW.GLFW_KEY_BACKSPACE) {
                    this.type(window, focused, chop(this.ui.textOf(focused)), UiWidgets.MOST_TEXT);
                }
            }
            case "TextArea" -> {
                if (enter) {
                    this.type(window, focused, this.ui.textOf(focused) + "\n", UiWidgets.MOST_AREA);
                } else if (key == GLFW.GLFW_KEY_BACKSPACE) {
                    this.type(window, focused, chop(this.ui.textOf(focused)), UiWidgets.MOST_AREA);
                }
            }
            case "NumberBox" -> {
                if (enter) {
                    this.commit(window, focused);
                } else if (key == GLFW.GLFW_KEY_BACKSPACE) {
                    this.ui.typing.computeIfPresent(focused.id(), (id, typed) -> chop(typed));
                } else if (key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_DOWN) {
                    this.step(window, focused, key == GLFW.GLFW_KEY_UP ? 1 : -1);
                }
            }
            case "Slider" -> {
                if (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_RIGHT) {
                    this.step(window, focused, along);
                }
            }
            case "RadioGroup", "TabView" -> {
                if (along != 0) {
                    final int picked = Math.clamp(focused.number(4) + along, 1, Math.max(1, focused.rows().size()));
                    this.send(window, "select", focused.id(), "", picked);
                }
            }
            case "ComboBox" -> {
                if (enter || key == GLFW.GLFW_KEY_DOWN) {
                    this.openCombo(focused);
                }
            }
            case "ListBox", "Table" -> {
                if (key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_DOWN) {
                    final int picked = Math.clamp(focused.number(4) + along, 1, Math.max(1, focused.rows().size()));
                    this.send(window, "select", focused.id(), "", picked);
                }
            }
            case "TreeView" -> this.treeKey(window, focused, key, along);
            default -> {
                return false;
            }
        }
        return true;
    }

    private void treeKey(final UiWindowPayload window, final UiWindowPayload.Widget tree, final int key,
                         final int along) {
        final List<SigmaTrees.Row> shown = SigmaTrees.shown(tree, this.ui);
        int index = 0;
        for (int i = 0; i < shown.size(); i++) {
            if (shown.get(i).node() == tree.number(4)) {
                index = i;
            }
        }
        if (key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_DOWN) {
            final int next = Math.clamp(index + along, 0, Math.max(0, shown.size() - 1));
            if (!shown.isEmpty()) {
                this.send(window, "select", tree.id(), "", shown.get(next).node());
            }
        } else if ((key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_RIGHT) && !shown.isEmpty()) {
            final SigmaTrees.Row row = shown.get(index);
            final boolean folded = this.ui.isFolded(tree.id(), row.node());
            if (row.children() && folded == (key == GLFW.GLFW_KEY_RIGHT)) {
                this.ui.toggleFold(tree.id(), row.node());
            }
        }
    }

    private boolean popupKey(final UiWindowPayload window, final int key) {
        final UiWindowPayload.Widget owner = this.widget(window, this.ui.popupOwner);
        final int count = owner == null ? 0 : this.entries(owner).size();
        switch (key) {
            case GLFW.GLFW_KEY_ESCAPE -> this.ui.closePopup();
            case GLFW.GLFW_KEY_UP -> this.ui.popupScroll = Math.max(0, this.ui.popupScroll - 1);
            case GLFW.GLFW_KEY_DOWN -> this.ui.popupScroll = Math.min(Math.max(0, count - 1),
                    this.ui.popupScroll + 1);
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> this.choose(window, this.ui.popupScroll);
            case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_RIGHT -> {
                if (this.ui.popup == SigmaUiState.PopupKind.MENU && owner != null) {
                    final List<String> menus = menus(owner);
                    final int at = menus.indexOf(this.ui.popupMenu);
                    final int next = Math.floorMod(at + (key == GLFW.GLFW_KEY_RIGHT ? 1 : -1), menus.size());
                    this.openMenu(owner, menus.get(next));
                }
            }
            default -> {
                return true;
            }
        }
        return true;
    }

    private boolean dialogKey(final int key) {
        final UiWindowPayload window = this.front();
        switch (key) {
            case GLFW.GLFW_KEY_ESCAPE -> {
                this.send(window, "cancel", this.dialog, "", 0);
                this.dialog = 0;
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                if (!this.dialogPath.isEmpty()) {
                    this.send(window, "file", this.dialog, this.dialogPath, 0);
                    this.dialog = 0;
                }
            }
            case GLFW.GLFW_KEY_BACKSPACE -> this.dialogPath = chop(this.dialogPath);
            default -> {
                // The line takes letters through charTyped.
            }
        }
        return true;
    }

    private void clickPopup(final UiWindowPayload window, final int column, final int row) {
        final UiLayout.Rect rect = this.menuRect;
        if (rect == null || column < rect.x() || column >= rect.x() + rect.w() || row <= rect.y()
                || row >= rect.y() + rect.h() - 1) {
            this.ui.closePopup();
            return;
        }
        this.choose(window, row - rect.y() - 1);
    }

    /* Picks the entry of the open popup at that place in what it lists. */
    private void choose(final UiWindowPayload window, final int index) {
        final UiWindowPayload.Widget owner = this.widget(window, this.ui.popupOwner);
        if (owner == null) {
            this.ui.closePopup();
            return;
        }
        final List<Integer> entries = this.entries(owner);
        if (index < 0 || index >= entries.size() || SigmaMenus.SEPARATOR.equals(owner.row(entries.get(index)))) {
            return;
        }
        this.send(window, this.ui.popup == SigmaUiState.PopupKind.COMBO ? "select" : "pick", owner.id(), "",
                entries.get(index) + 1);
        this.ui.closePopup();
    }

    private void openMenu(final UiWindowPayload.Widget bar, final String menu) {
        this.ui.popup = SigmaUiState.PopupKind.MENU;
        this.ui.popupOwner = bar.id();
        this.ui.popupMenu = menu;
        this.ui.popupScroll = 0;
    }

    private void openCombo(final UiWindowPayload.Widget combo) {
        this.ui.popup = SigmaUiState.PopupKind.COMBO;
        this.ui.popupOwner = combo.id();
        this.ui.popupScroll = Math.max(0, combo.number(4) - 1);
    }

    private void press(final UiWindowPayload window, final UiWindowPayload.Widget widget) {
        switch (widget.kind()) {
            case "Button" -> this.send(window, "click", widget.id(), "", 0);
            case "CheckBox" -> this.send(window, "toggle", widget.id(), "", widget.ticked() ? 0 : 1);
            case "RadioGroup" -> this.send(window, "select", widget.id(), "", Math.max(1, widget.number(4)));
            default -> {
                // Nothing else is pressed.
            }
        }
    }

    private void step(final UiWindowPayload window, final UiWindowPayload.Widget widget, final int direction) {
        final int value = Math.clamp((long) widget.number(1) + (long) direction * Math.max(1, widget.number(5)),
                widget.number(2), Math.max(widget.number(2), widget.number(3)));
        this.ui.typing.remove(widget.id());
        this.send(window, "number", widget.id(), "", value);
    }

    private void commit(final UiWindowPayload window, final UiWindowPayload.Widget box) {
        final String typed = this.ui.typing.remove(box.id());
        if (typed == null || typed.isEmpty() || "-".equals(typed)) {
            return;
        }
        try {
            final int value = (int) Math.clamp(Long.parseLong(typed), box.number(2),
                    Math.max(box.number(2), box.number(3)));
            this.send(window, "number", box.id(), "", value);
        } catch (final NumberFormatException unreadable) {
            // Only digits are taken, so a number too long to read is all that lands here, and it is let go.
        }
    }

    private void type(final UiWindowPayload window, final UiWindowPayload.Widget box, final String said,
                      final int most) {
        final String kept = said.length() > most ? said.substring(0, most) : said;
        this.ui.typing.put(box.id(), kept);
        this.send(window, "text", box.id(), kept, 0);
    }

    private static int sliderValue(final UiWindowPayload.Widget slider, final int at, final int width) {
        final int least = slider.number(2);
        final int most = Math.max(least, slider.number(3));
        final double fraction = Math.clamp(at / (double) Math.max(1, width - 1), 0, 1);
        return least + (int) Math.round(fraction * (most - least));
    }

    /* Gives the keyboard to the first widget that takes it when none has it, or the one it had has gone. */
    private void ensureFocus(final UiWindowPayload window) {
        if (this.widget(window, this.ui.focused) == null || !this.where.containsKey(this.ui.focused)) {
            this.ui.focused = 0;
            this.moveFocus(window, 1);
        }
    }

    private void moveFocus(final UiWindowPayload window, final int by) {
        final List<Long> order = new ArrayList<>();
        for (final UiWindowPayload.Widget widget : window.widgets()) {
            if (FOCUSABLE.contains(widget.kind()) && widget.answers() && widget.shows()
                    && this.where.containsKey(widget.id())) {
                order.add(widget.id());
            }
        }
        if (order.isEmpty()) {
            return;
        }
        final int at = order.indexOf(this.ui.focused);
        this.focusOn(window, order.get(Math.floorMod(at < 0 ? (by > 0 ? -1 : 0) + by : at + by, order.size())));
    }

    private void focusOn(final UiWindowPayload window, final long id) {
        final UiWindowPayload.Widget was = this.widget(window, this.ui.focused);
        if (was != null && was.id() != id && "NumberBox".equals(was.kind())) {
            this.commit(window, was);
        }
        this.ui.focused = id;
    }

    @Nullable
    private UiWindowPayload.Widget first(final UiWindowPayload window, final String kind) {
        for (final UiWindowPayload.Widget widget : window.widgets()) {
            if (kind.equals(widget.kind())) {
                return widget;
            }
        }
        return null;
    }

    @Nullable
    private UiWindowPayload.Widget widget(final UiWindowPayload window, final long id) {
        if (id == 0) {
            return null;
        }
        for (final UiWindowPayload.Widget widget : window.widgets()) {
            if (widget.id() == id) {
                return widget;
            }
        }
        return null;
    }

    private void send(final UiWindowPayload window, final String kind, final long widget, final String said,
                      final int number) {
        PacketDistributor.sendToServer(new UiEventPayload(this.host, window.program(), window.window(), widget, kind,
                said, number, 0));
    }

    private static String bar(final int value, final int least, final int mostAsked, final int width) {
        final int most = Math.max(least + 1, mostAsked);
        final int filled = (int) ((long) Math.max(0, width) * (Math.clamp(value, least, most) - least)
                / (most - least));
        return "▓".repeat(Math.max(0, filled)) + "░".repeat(Math.max(0, width - filled));
    }

    private static String clip(final String said, final int width) {
        return said.length() > Math.max(0, width) ? said.substring(0, Math.max(0, width)) : said;
    }

    private static String pad(final String said, final int width) {
        final String cut = clip(said, width);
        return cut + " ".repeat(Math.max(0, width - cut.length()));
    }

    private static String tail(final String said, final int width) {
        return said.length() > width ? said.substring(said.length() - Math.max(0, width)) : said;
    }

    private static String chop(final String said) {
        return said.isEmpty() ? said : said.substring(0, said.length() - 1);
    }

    private static List<String> wrap(final String text, final int width) {
        // A box laid out with no width still takes a letter a line, so the loop always moves on.
        final int step = Math.max(1, width);
        final List<String> lines = new ArrayList<>();
        for (final String paragraph : text.split("\n", -1)) {
            String left = paragraph;
            while (left.length() > step) {
                lines.add(left.substring(0, step));
                left = left.substring(step);
            }
            lines.add(left);
        }
        return lines;
    }
}
