/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.cannon.lua.lib.LuaTerminal;
import dev.jstech.computers.gui.layout.UiLayout;
import dev.jstech.computers.operation.payload.UiEventPayload;
import dev.jstech.computers.operation.payload.UiWindowPayload;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * A window a Cannon program opened, drawn by the machine's own system.
 *
 * <p>The program says what it wants (a row, a label, a button, a list) and the desktop draws it the way
 * this system draws everything else, so the same program is a Frames 95 window on 95 and an XP one on XP.
 * Where each widget lands is worked out by {@link UiLayout} from what the letters of this font measure.
 *
 * <p>What the player does goes back to the program as an event. A box being typed in keeps what is typed
 * here until it is sent, so a slow machine never eats a keystroke.
 */
public final class CannonWindowApp implements IDesktopApp {

    /** How tall the things a program can put in a window are, when nobody says otherwise. */
    private static final int LINE_H = 9;
    private static final int BUTTON_H = 16;
    private static final int FIELD_H = 16;
    private static final int BOX_H = 12;
    private static final int BAR_H = 10;
    private static final int ROW_H = 11;
    private static final int LEAST_LIST = 40;
    private static final int LEAST_CANVAS_W = 80;
    private static final int LEAST_CANVAS_H = 60;

    private final BlockPos host;
    private final int program;
    private final long window;

    private UiWindowPayload state;
    private OsSkin skin = OsSkin.fallback();
    private List<UiLayout.Rect> places = List.of();
    private final Map<Long, UiLayout.Rect> where = new HashMap<>();

    /** What is being typed in a box, until the program has it. */
    private final Map<Long, String> typing = new HashMap<>();
    private long focused;
    private long pressed;
    private double pointerX;
    private double pointerY;
    private final Map<Long, Integer> scrolled = new HashMap<>();

    public CannonWindowApp(final BlockPos host, final UiWindowPayload opened) {
        this.host = host;
        this.program = opened.program();
        this.window = opened.window();
        this.state = opened;
    }

    /** The key a desktop lists this window under, which names the program and the window. */
    public static String keyFor(final int program, final long window) {
        return "Cannon\0" + program + "\0" + window;
    }

    public String key() {
        return keyFor(this.program, this.window);
    }

    /** Takes the window as it now stands. */
    public void accept(final UiWindowPayload payload) {
        this.state = payload;
        // A box the program itself changed shows what the program says, not what was half typed into it.
        for (final UiWindowPayload.Widget widget : payload.widgets()) {
            final String typed = this.typing.get(widget.id());
            if (typed != null && !typed.equals(widget.text())) {
                this.typing.remove(widget.id());
            }
        }
    }

    /** The middle of the first widget of that kind, in desktop pixels, or null when it has none; for tests. */
    public int[] pointAt(final String kind) {
        for (final UiWindowPayload.Widget widget : this.state.widgets()) {
            final UiLayout.Rect rect = this.where.get(widget.id());
            if (rect != null && widget.kind().equals(kind)) {
                return new int[] {rect.x() + rect.w() / 2, rect.y() + rect.h() / 2};
            }
        }
        return null;
    }

    /** What the first widget of that kind says, or empty when it has none; for tests. */
    public String said(final String kind) {
        for (final UiWindowPayload.Widget widget : this.state.widgets()) {
            if (widget.kind().equals(kind)) {
                return widget.text();
            }
        }
        return "";
    }

    /** How many rows the first list in the window holds; for tests. */
    public int rows() {
        for (final UiWindowPayload.Widget widget : this.state.widgets()) {
            if ("ListBox".equals(widget.kind())) {
                return widget.rows().size();
            }
        }
        return 0;
    }

    @Override
    public String title() {
        return this.state.title().isEmpty() ? "Window" : this.state.title();
    }

    @Override
    public int defaultWidth() {
        return this.state.width() + 8;
    }

    @Override
    public int defaultHeight() {
        return this.state.height() + DesktopWindow.TITLE_H + 8;
    }

    @Override
    public int minWidth() {
        return Math.min(this.defaultWidth(), 120);
    }

    @Override
    public int minHeight() {
        return Math.min(this.defaultHeight(), 70);
    }

    @Override
    public void applySkin(final OsSkin osSkin) {
        this.skin = osSkin == null ? OsSkin.fallback() : osSkin;
    }

    @Override
    public void onClosed() {
        // The player shutting the window is the program's to hear: its own OnClose runs, and it may end.
        this.send("close", 0L, "", 0, 0);
    }

    @Override
    public void renderContent(final GuiGraphics g, final Font font, final int x, final int y, final int width,
                              final int height, final int mouseX, final int mouseY, final float partialTick) {
        this.pointerX = mouseX;
        this.pointerY = mouseY;
        g.fill(x, y, x + width, y + height, this.skin.windowBg());
        this.places = UiLayout.lay(this.boxes(font), x, y, width, height);
        this.where.clear();
        for (final UiLayout.Rect rect : this.places) {
            this.where.put(rect.id(), rect);
        }
        for (final UiWindowPayload.Widget widget : this.state.widgets()) {
            final UiLayout.Rect rect = this.where.get(widget.id());
            if (rect != null && widget.shows()) {
                this.draw(g, font, widget, rect, mouseX, mouseY);
            }
        }
    }

    /** What each widget is, and how big it is when nobody says otherwise, for the layout to work with. */
    private List<UiLayout.Box> boxes(final Font font) {
        final List<UiLayout.Box> boxes = new ArrayList<>();
        for (final UiWindowPayload.Widget widget : this.state.widgets()) {
            boxes.add(new UiLayout.Box(widget.id(), widget.parent(), widget.kind(), widget.weight(),
                    this.wideOf(font, widget), tallOf(widget), widget.width(), widget.height(),
                    widget.number(0), widget.x(), widget.y()));
        }
        return boxes;
    }

    private int wideOf(final Font font, final UiWindowPayload.Widget widget) {
        return switch (widget.kind()) {
            case "Label" -> font.width(widget.text());
            case "Button" -> font.width(widget.text()) + 12;
            case "TextBox" -> Math.max(60, font.width(this.textOf(widget)) + 10);
            case "CheckBox" -> BOX_H + 4 + font.width(widget.text());
            case "ProgressBar" -> 60;
            case "ListBox" -> 80;
            case "Canvas" -> LEAST_CANVAS_W;
            default -> 0;
        };
    }

    private static int tallOf(final UiWindowPayload.Widget widget) {
        return switch (widget.kind()) {
            case "Label" -> LINE_H;
            case "Button" -> BUTTON_H;
            case "TextBox" -> FIELD_H;
            case "CheckBox" -> BOX_H;
            case "ProgressBar" -> BAR_H;
            case "ListBox" -> LEAST_LIST;
            case "Canvas" -> LEAST_CANVAS_H;
            default -> 0;
        };
    }

    private String textOf(final UiWindowPayload.Widget widget) {
        final String typed = this.typing.get(widget.id());
        return typed != null ? typed : widget.text();
    }

    private void draw(final GuiGraphics g, final Font font, final UiWindowPayload.Widget widget,
                      final UiLayout.Rect rect, final int mouseX, final int mouseY) {
        final boolean over = mouseX >= rect.x() && mouseX < rect.x() + rect.w()
                && mouseY >= rect.y() && mouseY < rect.y() + rect.h();
        final int middle = rect.y() + (rect.h() - 7) / 2;
        switch (widget.kind()) {
            case "Label" -> g.drawString(font, widget.text(), rect.x(), middle,
                    widget.answers() ? this.skin.text() : this.skin.dim(), this.skin.textShadow());
            case "Button" -> this.skin.button(g, font, rect.x(), rect.y(), rect.w(), rect.h(), widget.text(),
                    over && widget.answers(), this.pressed == widget.id(), false);
            case "TextBox" -> {
                this.skin.field(g, rect.x(), rect.y(), rect.w(), rect.h(), this.focused == widget.id());
                final String said = this.textOf(widget);
                final String shown = said.length() > 64 ? said.substring(said.length() - 64) : said;
                g.drawString(font, shown, rect.x() + 4, middle, this.skin.text(), false);
                if (this.focused == widget.id() && (System.currentTimeMillis() / 500) % 2 == 0) {
                    g.fill(rect.x() + 4 + font.width(shown), rect.y() + 3,
                            rect.x() + 5 + font.width(shown), rect.y() + rect.h() - 3, this.skin.text());
                }
            }
            case "CheckBox" -> {
                final int box = Math.min(BOX_H, rect.h());
                this.skin.field(g, rect.x(), rect.y() + (rect.h() - box) / 2, box, box, false);
                if (widget.ticked()) {
                    g.drawString(font, "x", rect.x() + 3, middle, this.skin.text(), false);
                }
                g.drawString(font, widget.text(), rect.x() + box + 4, middle, this.skin.text(),
                        this.skin.textShadow());
            }
            case "ProgressBar" -> this.bar(g, widget, rect);
            case "ListBox" -> this.list(g, font, widget, rect, mouseX, mouseY);
            case "Canvas" -> this.canvas(g, font, widget, rect);
            default -> {
                // A row or a column is not drawn: it is only where its widgets are.
            }
        }
    }

    private void bar(final GuiGraphics g, final UiWindowPayload.Widget widget, final UiLayout.Rect rect) {
        this.skin.panel(g, rect.x(), rect.y(), rect.w(), rect.h());
        final int least = widget.number(2);
        final int most = Math.max(least + 1, widget.number(3));
        final int value = Math.clamp(widget.number(1), least, most);
        final int room = rect.w() - 4;
        final int filled = (int) ((long) room * (value - least) / (most - least));
        if (filled > 0) {
            g.fill(rect.x() + 2, rect.y() + 2, rect.x() + 2 + filled, rect.y() + rect.h() - 2,
                    this.skin.accent());
        }
    }

    private void list(final GuiGraphics g, final Font font, final UiWindowPayload.Widget widget,
                      final UiLayout.Rect rect, final int mouseX, final int mouseY) {
        this.skin.panel(g, rect.x(), rect.y(), rect.w(), rect.h());
        final int rows = Math.max(1, (rect.h() - 4) / ROW_H);
        final int scroll = Math.clamp(this.scrolled.getOrDefault(widget.id(), 0), 0,
                Math.max(0, widget.rows().size() - rows));
        for (int i = 0; i < rows && scroll + i < widget.rows().size(); i++) {
            final int at = scroll + i;
            final int top = rect.y() + 2 + i * ROW_H;
            final boolean over = mouseX >= rect.x() && mouseX < rect.x() + rect.w()
                    && mouseY >= top && mouseY < top + ROW_H;
            final boolean picked = at == widget.number(4) - 1;
            this.skin.listRow(g, rect.x() + 2, top, rect.w() - 4, ROW_H, over, picked);
            final int colour = this.skin.listRowText(picked);
            g.drawString(font, widget.rows().get(at), rect.x() + 4, top + 2, colour, false);
            if (at < widget.details().size() && !widget.details().get(at).isEmpty()) {
                final String right = widget.details().get(at);
                g.drawString(font, right, rect.x() + rect.w() - 4 - font.width(right), top + 2, colour, false);
            }
        }
        if (widget.rows().size() > rows) {
            final int span = rect.h() - 4;
            final int thumb = Math.max(6, span * rows / widget.rows().size());
            final int top = rect.y() + 2 + (span - thumb) * scroll / Math.max(1, widget.rows().size() - rows);
            this.skin.scrollThumb(g, rect.x() + rect.w() - 5, top, 3, thumb);
        }
    }

    /* A canvas is drawn stroke by stroke, in the sixteen colours every screen on these machines has. */
    private void canvas(final GuiGraphics g, final Font font, final UiWindowPayload.Widget widget,
                        final UiLayout.Rect rect) {
        this.skin.panel(g, rect.x(), rect.y(), rect.w(), rect.h());
        for (final UiWindowPayload.Stroke stroke : widget.drawing()) {
            final int colour = paint(stroke.colour());
            switch (stroke.kind()) {
                case "Clear" -> g.fill(rect.x() + 1, rect.y() + 1, rect.x() + rect.w() - 1,
                        rect.y() + rect.h() - 1, colour);
                case "FillRect" -> g.fill(rect.x() + 1 + stroke.x(), rect.y() + 1 + stroke.y(),
                        rect.x() + 1 + stroke.x() + Math.max(0, stroke.x2()),
                        rect.y() + 1 + stroke.y() + Math.max(0, stroke.y2()), colour);
                case "DrawLine" -> line(g, rect.x() + 1 + stroke.x(), rect.y() + 1 + stroke.y(),
                        rect.x() + 1 + stroke.x2(), rect.y() + 1 + stroke.y2(), colour);
                case "DrawText" -> g.drawString(font, stroke.text(), rect.x() + 1 + stroke.x(),
                        rect.y() + 1 + stroke.y(), colour, false);
                case "SetPixel" -> g.fill(rect.x() + 1 + stroke.x(), rect.y() + 1 + stroke.y(),
                        rect.x() + 2 + stroke.x(), rect.y() + 2 + stroke.y(), colour);
                default -> {
                    // A stroke of a kind this version does not draw is left out rather than guessed at.
                }
            }
        }
    }

    private static int paint(final int colour) {
        final int[] palette = LuaTerminal.DEFAULT_PALETTE;
        return 0xFF000000 | palette[Math.floorMod(colour, palette.length)];
    }

    /* A line of single pixels, since the screen has no line of its own to draw with. */
    private static void line(final GuiGraphics g, final int x1, final int y1, final int x2, final int y2,
                             final int colour) {
        final int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        if (steps == 0) {
            g.fill(x1, y1, x1 + 1, y1 + 1, colour);
            return;
        }
        for (int i = 0; i <= steps; i++) {
            final int x = x1 + (x2 - x1) * i / steps;
            final int y = y1 + (y2 - y1) * i / steps;
            g.fill(x, y, x + 1, y + 1, colour);
        }
    }

    // what the player does

    @Override
    public void mouseClicked(final DesktopWindow window, final double mouseX, final double mouseY,
                             final int button) {
        final UiWindowPayload.Widget hit = this.widgetAt(mouseX, mouseY);
        this.focused = 0;
        if (hit == null || !hit.answers()) {
            return;
        }
        switch (hit.kind()) {
            case "Button" -> {
                this.pressed = hit.id();
                this.send("click", hit.id(), "", 0, 0);
            }
            case "TextBox" -> this.focused = hit.id();
            case "CheckBox" -> this.send("toggle", hit.id(), "", hit.ticked() ? 0 : 1, 0);
            case "ListBox" -> {
                final UiLayout.Rect rect = this.where.get(hit.id());
                final int scroll = this.scrolled.getOrDefault(hit.id(), 0);
                final int row = scroll + (int) ((mouseY - rect.y() - 2) / ROW_H);
                if (row >= 0 && row < hit.rows().size()) {
                    this.send("select", hit.id(), "", row + 1, 0);
                }
            }
            case "Canvas" -> {
                final UiLayout.Rect rect = this.where.get(hit.id());
                this.send("click", hit.id(), "", (int) (mouseX - rect.x() - 1), (int) (mouseY - rect.y() - 1));
            }
            default -> {
                // A row or a column is not something to press.
            }
        }
    }

    @Override
    public void mouseReleased(final DesktopWindow window, final double mouseX, final double mouseY,
                              final int button) {
        this.pressed = 0;
    }

    @Override
    public boolean mouseScrolled(final double delta) {
        final UiWindowPayload.Widget hit = this.widgetAt(this.pointerX, this.pointerY);
        if (hit == null || !"ListBox".equals(hit.kind())) {
            return false;
        }
        this.scrolled.merge(hit.id(), delta > 0 ? -1 : 1, Integer::sum);
        this.scrolled.put(hit.id(), Math.max(0, this.scrolled.get(hit.id())));
        return true;
    }

    @Override
    public boolean charTyped(final char c) {
        final UiWindowPayload.Widget box = this.widgetOf(this.focused);
        if (box == null || c < 32 || c == 127) {
            return false;
        }
        final String said = this.textOf(box) + c;
        this.typing.put(box.id(), said.length() > UiEventPayload.MAX_TEXT
                ? said.substring(0, UiEventPayload.MAX_TEXT) : said);
        this.send("text", box.id(), this.typing.get(box.id()), 0, 0);
        return true;
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        final UiWindowPayload.Widget box = this.widgetOf(this.focused);
        if (box == null) {
            return false;
        }
        switch (key) {
            case GLFW.GLFW_KEY_BACKSPACE -> {
                final String said = this.textOf(box);
                if (!said.isEmpty()) {
                    this.typing.put(box.id(), said.substring(0, said.length() - 1));
                    this.send("text", box.id(), this.typing.get(box.id()), 0, 0);
                }
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                this.send("submit", box.id(), this.textOf(box), 0, 0);
                this.typing.remove(box.id());
            }
            case GLFW.GLFW_KEY_TAB -> this.focused = 0;
            default -> {
                return false;
            }
        }
        return true;
    }

    /** A window of a program takes the keyboard whole while a box in it has the cursor. */
    @Override
    public boolean wantsEscape() {
        return false;
    }

    private UiWindowPayload.Widget widgetOf(final long id) {
        if (id == 0) {
            return null;
        }
        for (final UiWindowPayload.Widget widget : this.state.widgets()) {
            if (widget.id() == id) {
                return widget;
            }
        }
        return null;
    }

    /* The widget under a point: the last one drawn there, so a widget inside a row is found before it. */
    private UiWindowPayload.Widget widgetAt(final double mouseX, final double mouseY) {
        UiWindowPayload.Widget found = null;
        for (final UiWindowPayload.Widget widget : this.state.widgets()) {
            final UiLayout.Rect rect = this.where.get(widget.id());
            if (rect == null || !widget.shows() || "Row".equals(widget.kind())
                    || "Column".equals(widget.kind())) {
                continue;
            }
            if (mouseX >= rect.x() && mouseX < rect.x() + rect.w()
                    && mouseY >= rect.y() && mouseY < rect.y() + rect.h()) {
                found = widget;
            }
        }
        return found;
    }

    private void send(final String kind, final long widget, final String said, final int number,
                      final int second) {
        PacketDistributor.sendToServer(new UiEventPayload(this.host, this.program, this.window, widget, kind,
                said, number, second));
    }
}
