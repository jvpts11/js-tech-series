/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.core.gui.TextScreen;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.util.List;

/**
 * Draws PACE on a text screen, in the grey and white of the monochrome terminals it ran on: the name and the maker
 * across the top, the frames in cascade with their numbered title bars in reverse, the one in front bright and the
 * others dimmed, the {@code -->} command line, the line saying what the picked item is, and the eight function-key
 * labels along the bottom.
 */
final class PacePainter {

    /** The most items a frame shows at once; one with more scrolls. */
    static final int FRAME_PAGE = 10;

    private static final int GROUND = TextScreen.cga(TextScreen.BLACK);
    private static final int INK = TextScreen.cga(TextScreen.GREY);
    private static final int BRIGHT = TextScreen.cga(TextScreen.WHITE);
    private static final int DIM = TextScreen.cga(TextScreen.DARK_GREY);
    /** How far each frame is set in and down from the one opened before it. */
    private static final int STEP_ACROSS = 8;
    private static final int STEP_DOWN = 3;
    private static final int FIRST_COLUMN = 2;
    private static final int FIRST_ROW = 2;
    private static final int LEAST_WIDTH = 30;
    /** How wide each function-key label is, and how far apart they stand. */
    private static final int KEY_WIDTH = 8;
    private static final int KEY_STEP = 10;
    private static final int KEYS = 8;
    private static final String PROMPT = "-->";

    private PacePainter() {
    }

    /** PACE's screen as it stands, at that many cells. */
    static TextScreen paint(final PaceKeys s, final int columns, final int rows) {
        final TextScreen screen = new TextScreen(columns, rows, INK, GROUND);
        final TaskPrompt task = s.task();
        if (task != null) {
            task.drawInto(screen, 0);
            return screen;
        }
        screen.put(1, 0, GameText.resolve(PaceTexts.NAME), BRIGHT, GROUND);
        final String maker = GameText.resolve(PaceTexts.MAKER);
        screen.put(columns - maker.length() - 1, 0, maker, INK, GROUND);
        final List<PaceKeys.Frame> frames = s.frames();
        for (final PaceKeys.Frame frame : s.drawOrder()) {
            final int index = frames.indexOf(frame);
            frame(screen, frame, index, index == s.active(), columns, rows);
        }
        final String typing = s.commandLine();
        screen.put(0, rows - 3, PROMPT + (typing == null ? "" : " " + typing + "_"), BRIGHT, GROUND);
        screen.put(0, rows - 2, clip(GameText.resolve(s.says()), columns), INK, GROUND);
        keys(screen, rows - 1);
        return screen;
    }

    /** Where frame number {@code index} (from 0) stands: its left, top, width and height. */
    static int[] box(final PaceKeys.Frame frame, final int index, final int columns, final int rows) {
        // The title bar holds the frame's number and its name with a space round them, inside the corners.
        int widest = titleBar(frame, index).length() + 4;
        int labels = 0;
        int details = 0;
        for (final PaceKeys.Item item : frame.items()) {
            labels = Math.max(labels, GameText.resolve(item.label()).length());
            details = Math.max(details, GameText.resolve(item.detail()).length());
        }
        widest = Math.max(widest, labels + (details > 0 ? details + 4 : 0) + 4);
        final int width = Math.max(LEAST_WIDTH, Math.min(columns - 2, widest));
        final int height = Math.min(FRAME_PAGE, Math.max(1, frame.items().size())) + 2;
        final int left = Math.max(0, Math.min(columns - width - 1, FIRST_COLUMN + STEP_ACROSS * index));
        final int top = Math.max(1, Math.min(rows - 4 - height, FIRST_ROW + STEP_DOWN * index + index / 2));
        return new int[] {left, top, width, height};
    }

    private static void frame(final TextScreen screen, final PaceKeys.Frame frame, final int index,
                              final boolean front, final int columns, final int rows) {
        final int[] box = box(frame, index, columns, rows);
        final int border = front ? BRIGHT : DIM;
        screen.box(box[0], box[1], box[2], box[3], border, GROUND, false);
        // The title bar in reverse: the frame's number and its name.
        final int bar = front ? INK : DIM;
        screen.fill(box[0] + 1, box[1], box[2] - 2, 1, GROUND, bar);
        screen.put(box[0] + 2, box[1], clip(titleBar(frame, index), box[2] - 3), GROUND, bar);
        int labels = 0;
        for (final PaceKeys.Item item : frame.items()) {
            labels = Math.max(labels, GameText.resolve(item.label()).length());
        }
        final int detailAt = box[0] + 2 + labels + 4;
        for (int i = 0; i < box[3] - 2; i++) {
            final int at = frame.top() + i;
            if (at >= frame.items().size()) {
                break;
            }
            final PaceKeys.Item item = frame.items().get(at);
            final int row = box[1] + 1 + i;
            final boolean picked = front && at == frame.selected();
            final int ink = picked ? GROUND : !front || !item.enabled() ? DIM : INK;
            final int ground = picked ? INK : GROUND;
            if (picked) {
                screen.fill(box[0] + 1, row, box[2] - 2, 1, ink, ground);
            }
            screen.put(box[0] + 2, row, clip(GameText.resolve(item.label()), box[2] - 4), ink, ground);
            final String detail = GameText.resolve(item.detail());
            if (!detail.isEmpty() && detailAt < box[0] + box[2] - 2) {
                screen.put(detailAt, row, clip(detail, box[0] + box[2] - 2 - detailAt), ink, ground);
            }
        }
    }

    private static void keys(final TextScreen screen, final int row) {
        final List<TextKey> labels = List.of(PaceTexts.KEY_HELP, PaceTexts.KEY_ENTER, PaceTexts.KEY_PREV,
                PaceTexts.KEY_NEXT, PaceTexts.KEY_CANCEL, PaceTexts.KEY_COMMANDS);
        for (int i = 0; i < KEYS; i++) {
            final int x = i * KEY_STEP;
            if (i >= labels.size()) {
                continue;
            }
            final String label = clip(GameText.resolve(labels.get(i)), KEY_WIDTH);
            screen.fill(x, row, KEY_WIDTH, 1, GROUND, INK);
            screen.put(x + (KEY_WIDTH - label.length()) / 2, row, label, GROUND, INK);
        }
    }

    /* What a frame's title bar says: its number, from 1, and its name. */
    private static String titleBar(final PaceKeys.Frame frame, final int index) {
        return " " + (index + 1) + "  " + GameText.resolve(frame.title()) + " ";
    }

    private static String clip(final String text, final int width) {
        return width <= 0 ? "" : text.length() <= width ? text : text.substring(0, width);
    }
}
