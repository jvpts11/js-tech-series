/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.layout;

import java.util.ArrayList;
import java.util.List;

/**
 * Where the MCP server's pairing question puts its parts, in the game's confirm look: the heading, who asks, what
 * reading and acting mean beside their names, the note, the three answers in a row (one over another when the screen
 * is too narrow), and when it refuses by itself; all in the middle of the screen. Pure, so a test reads it.
 */
public final class McpPairLayout {

    /** The widest an answer is, the height of one, and the room between them. */
    public static final int BUTTON_WIDTH = 150;
    public static final int BUTTON_HEIGHT = 20;
    public static final int GAP = 4;
    /** A line's height, the widest the words run, and the room at each side. */
    public static final int LINE = 10;
    public static final int MOST_TEXT = 420;
    public static final int SIDE = 20;
    /** How far the explanations stand from the names they explain. */
    public static final int LABEL = 40;
    /** The smallest screen laid out: the smallest the game's scaling of its menus leaves. */
    public static final int MIN_WIDTH = 320;
    public static final int MIN_HEIGHT = 240;
    /** The room between the parts, and the less they are given on a screen too short for it. */
    private static final int ROOM = 8;
    private static final int TIGHT = 4;

    private McpPairLayout() {
    }

    /**
     * The parts on a {@code width} by {@code height} screen, for {@code who} lines of who asks, {@code read} of what
     * reading means and {@code act} of what acting means, each wrapped to {@link Regions#textWidth()}.
     */
    public static Regions of(final int width, final int height, final int who, final int read, final int act) {
        final int w = Math.max(MIN_WIDTH, width);
        final int h = Math.max(MIN_HEIGHT, height);
        final int textWidth = Math.min(MOST_TEXT, w - 2 * SIDE);
        final boolean row = 3 * BUTTON_WIDTH + 2 * GAP <= w - 2 * SIDE;
        final int buttonsHeight = row ? BUTTON_HEIGHT : 3 * BUTTON_HEIGHT + 2 * GAP;
        final int lines = (2 + who + read + act + 1) * LINE + buttonsHeight;
        // Five rooms between the parts, the one over the answers half as large again.
        final int room = lines + 5 * ROOM + ROOM / 2 <= h - 2 * TIGHT ? ROOM : TIGHT;
        final int content = lines + 5 * room + room / 2;
        int y = Math.max(TIGHT, (h - content) / 2);
        final int titleY = y;
        y += LINE + room;
        final int whoY = y;
        y += who * LINE + room;
        final int readY = y;
        y += read * LINE;
        final int actY = y;
        y += act * LINE + room;
        final int noteY = y;
        y += LINE + room + room / 2;
        final List<StudioLayout.Rect> buttons = new ArrayList<>(3);
        if (row) {
            final int left = (w - (3 * BUTTON_WIDTH + 2 * GAP)) / 2;
            for (int i = 0; i < 3; i++) {
                buttons.add(new StudioLayout.Rect(left + i * (BUTTON_WIDTH + GAP), y, BUTTON_WIDTH, BUTTON_HEIGHT));
            }
        } else {
            final int left = (w - BUTTON_WIDTH) / 2;
            for (int i = 0; i < 3; i++) {
                buttons.add(new StudioLayout.Rect(left, y + i * (BUTTON_HEIGHT + GAP), BUTTON_WIDTH, BUTTON_HEIGHT));
            }
        }
        y += buttonsHeight + room;
        final int textX = (w - textWidth) / 2;
        return new Regions(w, h, titleY, whoY, readY, actY, noteY, y, textX, textWidth, List.copyOf(buttons));
    }

    /** The parts as a layout a test checks for overlaps and for staying on the screen. */
    public static GuiLayout layout(final Regions regions, final int who, final int read, final int act) {
        final GuiLayout out = new GuiLayout(regions.width(), regions.height());
        out.textBox("title", regions.textX(), regions.titleY(), regions.textWidth(), LINE);
        out.textBox("who", regions.textX(), regions.whoY(), regions.textWidth(), who * LINE);
        out.textBox("read", regions.textX(), regions.readY(), regions.textWidth(), read * LINE);
        out.textBox("act", regions.textX(), regions.actY(), regions.textWidth(), act * LINE);
        out.textBox("note", regions.textX(), regions.noteY(), regions.textWidth(), LINE);
        final List<String> names = List.of("read_button", "act_button", "refuse_button");
        for (int i = 0; i < names.size(); i++) {
            final StudioLayout.Rect button = regions.buttons().get(i);
            out.box(names.get(i), button.x(), button.y(), button.width(), button.height());
        }
        out.textBox("timeout", regions.textX(), regions.timeoutY(), regions.textWidth(), LINE);
        return out;
    }

    /**
     * The parts.
     *
     * @param width     the screen's width laid out
     * @param height    its height
     * @param titleY    the heading's top
     * @param whoY      who asks, its first line's top
     * @param readY     what reading means
     * @param actY      what acting means
     * @param noteY     the note under them
     * @param timeoutY  when it refuses by itself
     * @param textX     where the words start
     * @param textWidth how wide they run
     * @param buttons   the answers: read, read and act, refuse
     */
    public record Regions(int width, int height, int titleY, int whoY, int readY, int actY, int noteY, int timeoutY,
                          int textX, int textWidth, List<StudioLayout.Rect> buttons) {
    }
}
