/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.api.client.IComponentActions;
import dev.jstech.computers.api.client.IComponentRenderer;
import dev.jstech.computers.gui.layout.UiLayout;
import dev.jstech.computers.operation.payload.UiWindowPayload;
import dev.jstech.computers.vm.program.ComponentValues;
import dev.jstech.computers.vm.program.UiWidgets;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import org.lwjgl.glfw.GLFW;

/**
 * What a player's mouse and keyboard do to the widgets of a program's window.
 *
 * <p>What the program is to hear goes to it as an event and comes back as the window it sends; what is only the
 * screen's (a list scrolled, a tree folded, a menu opened, a search typed in a picker) stays here. A click outside an
 * open menu only shuts the menu, as on any desktop.
 */
final class SigmaInput {

    private final SigmaWindowApp app;
    private final SigmaUiState ui;
    private double pointerX;
    private double pointerY;

    /** What a click on the inside of these does nothing, since they only hold other widgets. */
    private static final Set<String> HOLDERS = Set.of("Row", "Column");
    /** The longest a component's action may be on its way to the program, its name and value together. */
    private static final int MOST_ACTION = UiWidgets.MOST_AREA;

    SigmaInput(final SigmaWindowApp app) {
        this.app = app;
        this.ui = app.ui();
    }

    /** Where the pointer was at the last frame, which the wheel is turned over. */
    void pointer(final int x, final int y) {
        this.pointerX = x;
        this.pointerY = y;
    }

    void mouseClicked(final double x, final double y, final int button) {
        final Font font = Minecraft.getInstance().font;
        if (this.ui.popupOpen()) {
            final SigmaMenus.Open menu = this.app.openMenu(font);
            if (menu != null && SigmaMenus.contains(menu, x, y)) {
                this.pickFrom(menu, x, y);
                return;
            }
            final boolean onBar = this.ui.popup == SigmaUiState.PopupKind.MENU;
            this.ui.closePopup();
            final UiWindowPayload.Widget hit = this.widgetAt(x, y);
            if (!onBar || hit == null || !"MenuBar".equals(hit.kind())) {
                return;
            }
        }
        final UiWindowPayload.Widget hit = this.widgetAt(x, y);
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            this.openContextMenu(hit, x, y);
            return;
        }
        this.focus(0);
        if (hit == null || !hit.answers()) {
            return;
        }
        final UiLayout.Rect rect = this.app.rectOf(hit.id());
        switch (hit.kind()) {
            case "Button" -> {
                this.ui.pressed = hit.id();
                this.app.send("click", hit.id(), "", 0, 0);
            }
            case "ItemSlot" -> this.app.send("click", hit.id(), "", 0, 0);
            case "TextBox", "TextArea" -> this.focus(hit.id());
            case "CheckBox" -> this.app.send("toggle", hit.id(), "", hit.ticked() ? 0 : 1, 0);
            case "NumberBox" -> this.numberBoxClicked(hit, rect, x, y);
            case "Slider" -> {
                this.ui.dragging = hit.id();
                this.ui.dragSent = Integer.MIN_VALUE;
                this.slide(hit, rect, x);
            }
            case "RadioGroup" -> this.radioClicked(font, hit, rect, x, y);
            case "ComboBox" -> {
                this.ui.popup = SigmaUiState.PopupKind.COMBO;
                this.ui.popupOwner = hit.id();
            }
            case "TabView" -> this.tabClicked(font, hit, rect, x, y);
            case "ScrollView" -> this.scrollBarClicked(hit, rect, x, y);
            case "ListBox" -> this.rowClicked(hit, (int) ((y - rect.y() - 2) / SigmaPainter.ROW_H),
                    hit.rows().size());
            case "Table" -> this.rowClicked(hit, (int) Math.floor((y - rect.y() - SigmaPainter.HEADER_H - 3)
                    / SigmaPainter.ROW_H), hit.rows().size());
            case "TreeView" -> this.treeClicked(hit, rect, x, y);
            case "MenuBar" -> this.menuBarClicked(font, hit, rect, x);
            case "ItemPicker" -> this.pickerClicked(hit, rect, x, y);
            case "Canvas" -> this.app.send("click", hit.id(), "", (int) (x - rect.x() - 1), (int) (y - rect.y() - 1));
            case "GenericComponent" -> this.genericClicked(hit, x, y, button);
            default -> {
                // A label, a bar, a picture, a chart or a frame is not something to press.
            }
        }
    }

    void mouseDragged(final double x, final double y) {
        final UiWindowPayload.Widget slider = this.app.widgetOf(this.ui.dragging);
        final UiLayout.Rect rect = slider == null ? null : this.app.rectOf(slider.id());
        if (rect != null) {
            this.slide(slider, rect, x);
        }
    }

    void mouseReleased() {
        this.ui.pressed = 0;
        this.ui.dragging = 0;
    }

    boolean mouseScrolled(final double delta) {
        final int step = delta > 0 ? -1 : 1;
        if (this.ui.popup == SigmaUiState.PopupKind.COMBO) {
            this.ui.popupScroll = Math.max(0, this.ui.popupScroll + step);
            return true;
        }
        final UiWindowPayload.Widget hit = this.widgetAt(this.pointerX, this.pointerY);
        if (hit == null) {
            return false;
        }
        switch (hit.kind()) {
            case "ListBox", "Table", "TreeView", "TextArea", "ItemPicker" -> {
                this.ui.scrollBy(hit.id(), step);
                return true;
            }
            case "LogView" -> {
                if (step < 0) {
                    this.ui.logsHeld.add(hit.id());
                }
                this.ui.scrollBy(hit.id(), step);
                return true;
            }
            default -> {
                return this.scrollView(hit, step * 12);
            }
        }
    }

    boolean charTyped(final char c) {
        final UiWindowPayload.Widget focused = this.app.widgetOf(this.ui.focused);
        if (focused == null || c < 32 || c == 127) {
            return false;
        }
        switch (focused.kind()) {
            case "TextBox" -> this.type(focused, this.ui.textOf(focused) + c, UiWidgets.MOST_TEXT);
            case "TextArea" -> this.type(focused, this.ui.textOf(focused) + c, UiWidgets.MOST_AREA);
            case "NumberBox" -> {
                final String typed = this.ui.typing.getOrDefault(focused.id(), "");
                if (Character.isDigit(c) || c == '-' && typed.isEmpty()) {
                    this.ui.typing.put(focused.id(), typed + c);
                }
            }
            case "ItemPicker" -> this.ui.filters.merge(focused.id(), String.valueOf(c), String::concat);
            case "GenericComponent" -> {
                final IComponentRenderer renderer = this.app.painter().rendererOf(focused);
                return renderer != null && this.guard(focused, () -> renderer.charTyped(c,
                        this.app.painter().dataOf(focused), this.actions(focused)));
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        if (key == GLFW.GLFW_KEY_ESCAPE && this.ui.popupOpen()) {
            this.ui.closePopup();
            return true;
        }
        final UiWindowPayload.Widget focused = this.app.widgetOf(this.ui.focused);
        if (focused == null) {
            return false;
        }
        if (key == GLFW.GLFW_KEY_TAB) {
            this.focus(0);
            return true;
        }
        final boolean enter = key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER;
        switch (focused.kind()) {
            case "TextBox" -> {
                if (enter) {
                    this.app.send("submit", focused.id(), this.ui.textOf(focused), 0, 0);
                    this.ui.typing.remove(focused.id());
                } else if (key == GLFW.GLFW_KEY_BACKSPACE) {
                    this.type(focused, chop(this.ui.textOf(focused)), UiWidgets.MOST_TEXT);
                } else {
                    return false;
                }
            }
            case "TextArea" -> {
                if (enter) {
                    this.type(focused, this.ui.textOf(focused) + "\n", UiWidgets.MOST_AREA);
                } else if (key == GLFW.GLFW_KEY_BACKSPACE) {
                    this.type(focused, chop(this.ui.textOf(focused)), UiWidgets.MOST_AREA);
                } else {
                    return false;
                }
            }
            case "NumberBox" -> {
                return this.numberBoxKey(focused, key, enter);
            }
            case "ItemPicker" -> {
                if (key != GLFW.GLFW_KEY_BACKSPACE) {
                    return false;
                }
                this.ui.filters.computeIfPresent(focused.id(), (id, filter) -> chop(filter));
            }
            case "GenericComponent" -> {
                final IComponentRenderer renderer = this.app.painter().rendererOf(focused);
                return renderer != null && this.guard(focused, () -> renderer.keyPressed(key, scanCode, modifiers,
                        this.app.painter().dataOf(focused), this.actions(focused)));
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    /* Gives the keyboard to that widget, or to none; a number half typed into a box is sent as it goes. */
    private void focus(final long id) {
        final UiWindowPayload.Widget was = this.app.widgetOf(this.ui.focused);
        if (was != null && was.id() != id && "NumberBox".equals(was.kind())) {
            this.commitNumber(was);
        }
        this.ui.focused = id;
    }

    private void type(final UiWindowPayload.Widget box, final String said, final int most) {
        final String kept = said.length() > most ? said.substring(0, most) : said;
        this.ui.typing.put(box.id(), kept);
        this.app.send("text", box.id(), kept, 0, 0);
    }

    private void numberBoxClicked(final UiWindowPayload.Widget box, final UiLayout.Rect rect, final double x,
                                  final double y) {
        if (x >= rect.x() + rect.w() - SigmaGlyphs.ARROW_W) {
            this.stepNumber(box, y < rect.y() + rect.h() / 2.0 ? 1 : -1);
            return;
        }
        this.focus(box.id());
        this.ui.typing.put(box.id(), String.valueOf(box.number(1)));
    }

    private boolean numberBoxKey(final UiWindowPayload.Widget box, final int key, final boolean enter) {
        if (enter) {
            this.commitNumber(box);
        } else if (key == GLFW.GLFW_KEY_BACKSPACE) {
            this.ui.typing.computeIfPresent(box.id(), (id, typed) -> chop(typed));
        } else if (key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_DOWN) {
            this.ui.typing.remove(box.id());
            this.stepNumber(box, key == GLFW.GLFW_KEY_UP ? 1 : -1);
        } else {
            return false;
        }
        return true;
    }

    /* A number typed into a box goes to the program kept between the box's least and most. */
    private void commitNumber(final UiWindowPayload.Widget box) {
        final String typed = this.ui.typing.remove(box.id());
        if (typed == null || typed.isEmpty() || "-".equals(typed)) {
            return;
        }
        try {
            final long asked = Long.parseLong(typed);
            final int value = (int) Math.clamp(asked, box.number(2), Math.max(box.number(2), box.number(3)));
            this.app.send("number", box.id(), "", value, 0);
        } catch (final NumberFormatException unreadable) {
            // Only digits are taken, so a number too long to read is all that lands here, and it is let go.
        }
    }

    private void stepNumber(final UiWindowPayload.Widget box, final int direction) {
        final int step = Math.max(1, box.number(5));
        final int value = Math.clamp((long) box.number(1) + (long) direction * step, box.number(2),
                Math.max(box.number(2), box.number(3)));
        this.app.send("number", box.id(), "", value, 0);
    }

    /* A slider's value from where the pointer is along it, sent only when it moves onto another step. */
    private void slide(final UiWindowPayload.Widget slider, final UiLayout.Rect rect, final double x) {
        final int least = slider.number(2);
        final int most = Math.max(least, slider.number(3));
        final int step = Math.max(1, slider.number(5));
        final double fraction = Math.clamp((x - rect.x()) / Math.max(1, rect.w()), 0, 1);
        int value = least + (int) Math.round(fraction * (most - least) / step) * step;
        value = Math.clamp(value, least, most);
        if (value != this.ui.dragSent) {
            this.ui.dragSent = value;
            this.app.send("number", slider.id(), "", value, 0);
        }
    }

    private void radioClicked(final Font font, final UiWindowPayload.Widget group, final UiLayout.Rect rect,
                              final double x, final double y) {
        final List<UiLayout.Rect> places = SigmaPainter.radioPlaces(font, group, rect);
        for (int i = 0; i < places.size(); i++) {
            if (inside(places.get(i), x, y)) {
                this.app.send("select", group.id(), "", i + 1, 0);
                return;
            }
        }
    }

    private void tabClicked(final Font font, final UiWindowPayload.Widget tabs, final UiLayout.Rect rect,
                            final double x, final double y) {
        final List<UiLayout.Rect> places = SigmaPainter.tabPlaces(font, tabs, rect);
        for (int i = 0; i < places.size(); i++) {
            if (inside(places.get(i), x, y)) {
                this.app.send("select", tabs.id(), "", i + 1, 0);
                return;
            }
        }
    }

    private void scrollBarClicked(final UiWindowPayload.Widget view, final UiLayout.Rect rect, final double x,
                                  final double y) {
        if (x < rect.x() + rect.w() - UiLayout.SCROLLBAR) {
            return;
        }
        final int room = this.ui.rooms.getOrDefault(view.id(), 0);
        this.ui.scrolled.put(view.id(), (int) Math.round(room * Math.clamp((y - rect.y()) / rect.h(), 0, 1)));
    }

    /* A row of a list or a table, counted from the one shown at the top. */
    private void rowClicked(final UiWindowPayload.Widget list, final int shownRow, final int rows) {
        final int row = this.ui.scroll(list.id()) + shownRow;
        if (shownRow >= 0 && row < rows) {
            this.app.send("select", list.id(), "", row + 1, 0);
        }
    }

    /* A tree: the fold mark folds or opens the node, anywhere else on its row picks it. */
    private void treeClicked(final UiWindowPayload.Widget tree, final UiLayout.Rect rect, final double x,
                             final double y) {
        final List<SigmaTrees.Row> shown = SigmaTrees.shown(tree, this.ui);
        final int index = this.ui.scroll(tree.id()) + (int) ((y - rect.y() - 2) / SigmaPainter.ROW_H);
        if (index < 0 || index >= shown.size()) {
            return;
        }
        final SigmaTrees.Row row = shown.get(index);
        final int markX = rect.x() + 3 + row.depth() * 10;
        if (row.children() && x >= markX && x < markX + SigmaGlyphs.MARK) {
            this.ui.toggleFold(tree.id(), row.node());
        } else {
            this.app.send("select", tree.id(), "", row.node(), 0);
        }
    }

    private void menuBarClicked(final Font font, final UiWindowPayload.Widget bar, final UiLayout.Rect rect,
                                final double x) {
        for (final SigmaMenus.Title title : SigmaMenus.titles(font, bar, rect)) {
            if (x >= title.x() && x < title.x() + title.w()) {
                this.ui.popup = SigmaUiState.PopupKind.MENU;
                this.ui.popupOwner = bar.id();
                this.ui.popupMenu = title.name();
                return;
            }
        }
    }

    private void pickerClicked(final UiWindowPayload.Widget picker, final UiLayout.Rect rect, final double x,
                               final double y) {
        if (y < rect.y() + SigmaPainter.SEARCH_H) {
            this.focus(picker.id());
            return;
        }
        final List<Integer> shown = SigmaPickers.shown(picker, this.ui.filters.getOrDefault(picker.id(), ""),
                this.app.painter());
        final int across = Math.max(1, rect.w() / SigmaPainter.SLOT);
        final int column = (int) ((x - rect.x()) / SigmaPainter.SLOT);
        final int row = (int) ((y - rect.y() - SigmaPainter.SEARCH_H - 2) / SigmaPainter.SLOT);
        final int at = (this.ui.scroll(picker.id()) + row) * across + column;
        if (column >= 0 && column < across && row >= 0 && at < shown.size()) {
            this.app.send("select", picker.id(), "", shown.get(at) + 1, 0);
        }
    }

    private void genericClicked(final UiWindowPayload.Widget component, final double x, final double y,
                                final int button) {
        final IComponentRenderer renderer = this.app.painter().rendererOf(component);
        if (renderer != null && this.guard(component, () -> renderer.mouseClicked(x, y, button,
                this.app.painter().dataOf(component), this.actions(component)))) {
            this.focus(component.id());
        }
    }

    /* What a component's renderer sends goes to the program as an action: its name, a nul, and the value's letters. */
    private IComponentActions actions(final UiWindowPayload.Widget component) {
        return (name, value) -> {
            if (name == null || name.isEmpty() || name.length() > 64) {
                return;
            }
            final String said = name + "\0" + ComponentValues.encodePlain(value);
            if (said.length() <= MOST_ACTION) {
                this.app.send("action", component.id(), said, 0, 0);
            }
        };
    }

    /* A call into a renderer, which a fault stops for good; the component then shows its placeholder. */
    private boolean guard(final UiWindowPayload.Widget component, final IRendererCall call) {
        try {
            return call.run();
        } catch (final RuntimeException fault) {
            this.app.painter().fail(component, fault);
            return false;
        }
    }

    /* The other button on a widget with a context menu of its own opens it where the pointer is. */
    private void openContextMenu(final UiWindowPayload.Widget hit, final double x, final double y) {
        for (UiWindowPayload.Widget at = hit; at != null; at = this.app.widgetOf(at.parent())) {
            for (final UiWindowPayload.Widget menu : this.app.state().widgets()) {
                if ("ContextMenu".equals(menu.kind()) && menu.parent() == at.id() && menu.answers()
                        && !menu.rows().isEmpty()) {
                    this.ui.popup = SigmaUiState.PopupKind.CONTEXT;
                    this.ui.popupOwner = menu.id();
                    this.ui.popupX = (int) x;
                    this.ui.popupY = (int) y;
                    return;
                }
            }
        }
    }

    private void pickFrom(final SigmaMenus.Open menu, final double x, final double y) {
        final UiWindowPayload.Widget owner = this.app.widgetOf(this.ui.popupOwner);
        final int entry = owner == null ? -1 : SigmaMenus.entryAt(owner, menu, this.ui.popupScroll, x, y);
        if (entry < 0) {
            return;
        }
        this.app.send(this.ui.popup == SigmaUiState.PopupKind.COMBO ? "select" : "pick", owner.id(), "", entry + 1,
                0);
        this.ui.closePopup();
    }

    /* Scrolls the scroll view a widget is inside, or the view itself, by that many pixels. */
    private boolean scrollView(final UiWindowPayload.Widget hit, final int by) {
        for (UiWindowPayload.Widget at = hit; at != null; at = this.app.widgetOf(at.parent())) {
            if ("ScrollView".equals(at.kind())) {
                final int room = this.ui.rooms.getOrDefault(at.id(), 0);
                this.ui.scrolled.put(at.id(), Math.clamp(this.ui.scroll(at.id()) + by, 0, room));
                return true;
            }
        }
        return false;
    }

    /* The widget under a point: the last one drawn there, so a widget inside a row is found before it. */
    private UiWindowPayload.Widget widgetAt(final double x, final double y) {
        UiWindowPayload.Widget found = null;
        for (final UiWindowPayload.Widget widget : this.app.state().widgets()) {
            final UiLayout.Rect rect = this.app.rectOf(widget.id());
            final UiLayout.Rect clip = this.app.clipOf(widget.id());
            if (rect == null || HOLDERS.contains(widget.kind()) || !inside(rect, x, y)
                    || clip != null && !inside(clip, x, y) || !this.app.visible(widget)) {
                continue;
            }
            found = widget;
        }
        return found;
    }

    private static boolean inside(final UiLayout.Rect rect, final double x, final double y) {
        return x >= rect.x() && x < rect.x() + rect.w() && y >= rect.y() && y < rect.y() + rect.h();
    }

    private static String chop(final String said) {
        return said.isEmpty() ? said : said.substring(0, said.length() - 1);
    }

    /** One call into a component's renderer. */
    @FunctionalInterface
    private interface IRendererCall {
        boolean run();
    }
}
