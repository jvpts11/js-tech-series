/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.gui.layout.LuaScreenLayout;
import dev.jstech.computers.operation.payload.LuaEventPayload;
import dev.jstech.computers.operation.payload.LuaScreenPayload;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * The screen of the Lua program in front of a terminal, drawn cell by cell, and the keyboard and mouse
 * that go to it as ComputerCraft's events.
 *
 * <p>Keys go as {@code key} (with whether it is held down and repeating) and {@code key_up}, with the
 * key numbers ComputerCraft uses, which are the keyboard's own; what they type goes as {@code char}.
 * Ctrl+V pastes, Ctrl+T asks the program to {@code terminate}, and clicks on a cell go as the mouse
 * events, counted in cells from 1. Ctrl+C is not the program's: the terminal takes it to stop it.
 */
final class LuaScreenView {

    private static final int GROUND = 0xFF000000;
    private static final int TAG_GROUND = 0xFF39D6C4;
    private static final int TAG_TEXT = 0xFF0D1117;
    private static final int RAIL = 0xFF2A2F3A;
    private static final int RAIL_THUMB = 0xFF5E6A7D;
    private static final float TAG_SCALE = 0.75f;
    private static final long BLINK_MILLIS = 400;

    private final BlockPos host;
    private final int session;
    /** Whether this is a terminal window, which sizes the screen to fit, rather than an editor's panel. */
    private final boolean window;

    private LuaScreenPayload screen;
    private LuaScreenLayout.Placement placed;
    private final Set<Integer> down = new HashSet<>();
    private int dragButton = -1;
    private int[] dragCell;

    LuaScreenView(final BlockPos host, final int session, final boolean window) {
        this.host = host;
        this.session = session;
        this.window = window;
    }

    void accept(final LuaScreenPayload payload) {
        this.screen = payload;
    }

    LuaScreenPayload screen() {
        return this.screen;
    }

    /** The screen as rows of plain text, the blank rows at the bottom left off: what the scrollback keeps. */
    List<String> rows() {
        final List<String> rows = new ArrayList<>();
        if (this.screen == null) {
            return rows;
        }
        for (final String row : this.screen.text()) {
            rows.add(row.stripTrailing());
        }
        while (!rows.isEmpty() && rows.getLast().isEmpty()) {
            rows.removeLast();
        }
        return rows;
    }

    /** The middle of a cell (counted from 1) where it was last drawn, or null when it is not showing. */
    int[] cellPoint(final int column, final int row) {
        final LuaScreenLayout.Placement at = this.placed;
        if (at == null || row - 1 < at.firstRow() || row - 1 >= at.firstRow() + at.rows()) {
            return null;
        }
        return new int[] {(int) (at.x() + (column - 0.5f) * at.cellWidth()),
                (int) (at.y() + (row - 1 - at.firstRow() + 0.5f) * at.cellHeight())};
    }

    /** How the screen was last placed, or null before it has been drawn. */
    LuaScreenLayout.Placement placed() {
        return this.placed;
    }

    void render(final GuiGraphics g, final Font font, final int x, final int y, final int width, final int height) {
        g.fill(x, y, x + width, y + height, GROUND);
        if (this.screen == null) {
            return;
        }
        final LuaScreenLayout.Placement at = this.window
                ? LuaScreenLayout.inWindow(x, y, width, height, this.screen.cursorY())
                : LuaScreenLayout.inPanel(x, y, width, height, this.screen.cursorY());
        this.placed = at;
        g.pose().pushPose();
        g.pose().translate(at.x(), at.y(), 0);
        g.pose().scale(at.scale(), at.scale(), 1f);
        for (int i = 0; i < at.rows(); i++) {
            final int row = at.firstRow() + i;
            final int top = i * LuaScreenLayout.CELL_H;
            this.drawGround(g, row, top);
            this.drawText(g, font, row, top);
        }
        this.drawCursor(g, font, at);
        g.pose().popPose();
        if (!at.whole()) {
            this.drawWhereAmI(g, font, at, x, y, width, height);
        }
    }

    /* The grounds of a row, a run of one colour at a time. */
    private void drawGround(final GuiGraphics g, final int row, final int top) {
        final String grounds = this.screen.groundColours().get(row);
        int from = 0;
        while (from < LuaScreenLayout.COLUMNS) {
            final int colour = digitAt(grounds, from, 15);
            int to = from + 1;
            while (to < LuaScreenLayout.COLUMNS && digitAt(grounds, to, 15) == colour) {
                to++;
            }
            g.fill(from * LuaScreenLayout.CELL_W, top, to * LuaScreenLayout.CELL_W, top + LuaScreenLayout.CELL_H,
                    this.colour(colour));
            from = to;
        }
    }

    /* The characters of a row, each in its own cell, centred in it since the font is not all one width. */
    private void drawText(final GuiGraphics g, final Font font, final int row, final int top) {
        final String text = this.screen.text().get(row);
        final String colours = this.screen.textColours().get(row);
        for (int column = 0; column < Math.min(text.length(), LuaScreenLayout.COLUMNS); column++) {
            final char c = text.charAt(column);
            if (c == ' ') {
                continue;
            }
            final String glyph = String.valueOf(c);
            final int left = column * LuaScreenLayout.CELL_W + (LuaScreenLayout.CELL_W - font.width(glyph) + 1) / 2;
            g.drawString(font, glyph, left, top + 1, this.colour(digitAt(colours, column, 0)), false);
        }
    }

    private void drawCursor(final GuiGraphics g, final Font font, final LuaScreenLayout.Placement at) {
        final int column = this.screen.cursorX() - 1;
        final int row = this.screen.cursorY() - 1 - at.firstRow();
        if (!this.screen.blink() || (System.currentTimeMillis() / BLINK_MILLIS) % 2 != 0
                || column < 0 || column >= LuaScreenLayout.COLUMNS || row < 0 || row >= at.rows()) {
            return;
        }
        g.drawString(font, "_", column * LuaScreenLayout.CELL_W + 1, row * LuaScreenLayout.CELL_H + 1,
                this.colour(this.screen.textColour()), false);
    }

    /* Which rows are showing when not all of them fit: a tag that says so, and a rail that shows where. */
    private void drawWhereAmI(final GuiGraphics g, final Font font, final LuaScreenLayout.Placement at, final int x,
                              final int y, final int width, final int height) {
        final String tag = LuaScreenLayout.COLUMNS + "x" + LuaScreenLayout.ROWS + " · rows " + (at.firstRow() + 1)
                + " to " + (at.firstRow() + at.rows());
        final int tagW = Math.round(font.width(tag) * TAG_SCALE) + 6;
        final int tagH = Math.round(font.lineHeight * TAG_SCALE) + 2;
        final int tagX = x + width - tagW - 6;
        final int tagY = y + 3;
        g.fill(tagX, tagY, tagX + tagW, tagY + tagH, TAG_GROUND);
        g.pose().pushPose();
        g.pose().translate(tagX + 3, tagY + 1.5f, 0);
        g.pose().scale(TAG_SCALE, TAG_SCALE, 1f);
        g.drawString(font, tag, 0, 0, TAG_TEXT, false);
        g.pose().popPose();
        final int railTop = tagY + tagH + 3;
        final int railBottom = y + height - 3;
        final int railX = x + width - 4;
        if (railBottom - railTop < 6) {
            return;
        }
        g.fill(railX, railTop, railX + 3, railBottom, RAIL);
        final int span = railBottom - railTop;
        final int thumbTop = railTop + span * at.firstRow() / LuaScreenLayout.ROWS;
        final int thumbBottom = railTop + span * (at.firstRow() + at.rows()) / LuaScreenLayout.ROWS;
        g.fill(railX, thumbTop, railX + 3, Math.max(thumbTop + 2, thumbBottom), RAIL_THUMB);
    }

    private int colour(final int index) {
        final int[] palette = this.screen.palette();
        return 0xFF000000 | (index >= 0 && index < palette.length ? palette[index] : 0);
    }

    private static int digitAt(final String digits, final int at, final int fallback) {
        if (at >= digits.length()) {
            return fallback;
        }
        final int value = Character.digit(digits.charAt(at), 16);
        return value < 0 ? fallback : value;
    }

    // the keyboard

    boolean keyPressed(final int key, final int modifiers) {
        final boolean control = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        if (control && key == GLFW.GLFW_KEY_T) {
            this.send("terminate");
            return true;
        }
        if (control && key == GLFW.GLFW_KEY_V) {
            String text = Minecraft.getInstance().keyboardHandler.getClipboard();
            final int end = firstLineEnd(text);
            text = text.substring(0, Math.min(end, LuaEventPayload.MAX_TEXT));
            if (!text.isEmpty()) {
                this.send("paste", text);
            }
            return true;
        }
        final boolean held = !this.down.add(key);
        this.send("key", (long) key, held);
        return true;
    }

    boolean keyReleased(final int key) {
        this.down.remove(key);
        this.send("key_up", (long) key);
        return true;
    }

    boolean charTyped(final char c) {
        if (c < 32 || c == 127) {
            return false;
        }
        this.send("char", String.valueOf(c));
        return true;
    }

    private static int firstLineEnd(final String text) {
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n' || text.charAt(i) == '\r') {
                return i;
            }
        }
        return text.length();
    }

    // the mouse

    boolean mouseClicked(final double mx, final double my, final int button) {
        final int[] cell = this.cellAt(mx, my);
        if (cell == null) {
            return false;
        }
        this.dragButton = button;
        this.dragCell = cell;
        this.send("mouse_click", (long) mouseButton(button), (long) cell[0], (long) cell[1]);
        return true;
    }

    boolean mouseDragged(final double mx, final double my, final int button) {
        final int[] cell = this.cellAt(mx, my);
        if (cell == null || this.dragButton != button || this.dragCell == null
                || (cell[0] == this.dragCell[0] && cell[1] == this.dragCell[1])) {
            return false;
        }
        this.dragCell = cell;
        this.send("mouse_drag", (long) mouseButton(button), (long) cell[0], (long) cell[1]);
        return true;
    }

    boolean mouseReleased(final double mx, final double my, final int button) {
        if (this.dragButton != button) {
            return false;
        }
        final int[] cell = this.cellAt(mx, my);
        final int[] at = cell != null ? cell : this.dragCell;
        this.dragButton = -1;
        this.dragCell = null;
        if (at == null) {
            return false;
        }
        this.send("mouse_up", (long) mouseButton(button), (long) at[0], (long) at[1]);
        return true;
    }

    boolean mouseScrolled(final double mx, final double my, final double delta) {
        final int[] cell = this.cellAt(mx, my);
        if (cell == null || delta == 0) {
            return false;
        }
        // Up the page is negative, as ComputerCraft counts it.
        this.send("mouse_scroll", delta > 0 ? -1L : 1L, (long) cell[0], (long) cell[1]);
        return true;
    }

    private int[] cellAt(final double mx, final double my) {
        return this.placed == null ? null : this.placed.cellAt(mx, my);
    }

    /* ComputerCraft counts the buttons from 1: left, right, middle. */
    private static int mouseButton(final int button) {
        return switch (button) {
            case GLFW.GLFW_MOUSE_BUTTON_RIGHT -> 2;
            case GLFW.GLFW_MOUSE_BUTTON_MIDDLE -> 3;
            default -> 1;
        };
    }

    private void send(final String name, final Object... values) {
        PacketDistributor.sendToServer(new LuaEventPayload(this.host, this.session, name, List.of(values)));
    }
}
