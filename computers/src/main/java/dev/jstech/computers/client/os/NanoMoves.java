/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.core.client.gui.logic.TextDocument;
import org.lwjgl.glfw.GLFW;

/**
 * The keys of nano that ask nothing: moving about the file, and changing it without typing a character.
 *
 * <p>Every one of them is reachable two ways, as the key a keyboard has for it and as a letter held with
 * Control, which is what a terminal with no such keys left people with and what their hands still do.
 */
final class NanoMoves {

    private static final int TAB_STOP = 8;
    private static final int SCREENFUL = 10;

    private NanoMoves() {
    }

    /** A key by itself: the arrows and their neighbours, and Enter, Backspace, Delete and Tab. */
    static void alone(final TtyEditor editor, final int key) {
        final TextDocument doc = editor.document();
        switch (key) {
            case GLFW.GLFW_KEY_LEFT -> doc.left();
            case GLFW.GLFW_KEY_RIGHT -> doc.right();
            case GLFW.GLFW_KEY_UP -> doc.up();
            case GLFW.GLFW_KEY_DOWN -> doc.down();
            case GLFW.GLFW_KEY_HOME -> doc.setCursor(doc.cursorLine(), 0);
            case GLFW.GLFW_KEY_END -> doc.setCursor(doc.cursorLine(), doc.line(doc.cursorLine()).length());
            case GLFW.GLFW_KEY_PAGE_UP -> up(doc);
            case GLFW.GLFW_KEY_PAGE_DOWN -> down(doc);
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                doc.newline();
                editor.touched();
            }
            case GLFW.GLFW_KEY_BACKSPACE -> {
                doc.backspace();
                editor.touched();
            }
            case GLFW.GLFW_KEY_DELETE -> {
                doc.delete();
                editor.touched();
            }
            case GLFW.GLFW_KEY_TAB -> {
                // As far as the next stop, which is what a tab is on a glass that has no tab of its own.
                final int to = TAB_STOP - doc.cursorCol() % TAB_STOP;
                for (int i = 0; i < to; i++) {
                    doc.insert(' ');
                }
                editor.touched();
            }
            default -> {
            }
        }
    }

    /**
     * A letter held with Control that moves the cursor.
     *
     * @return false when that letter moves nothing, so it is somebody else's to answer
     */
    static boolean held(final TextDocument doc, final int key) {
        switch (key) {
            case GLFW.GLFW_KEY_A -> doc.setCursor(doc.cursorLine(), 0);
            case GLFW.GLFW_KEY_E -> doc.setCursor(doc.cursorLine(), doc.line(doc.cursorLine()).length());
            case GLFW.GLFW_KEY_Y -> up(doc);
            case GLFW.GLFW_KEY_V -> down(doc);
            case GLFW.GLFW_KEY_P -> doc.up();
            case GLFW.GLFW_KEY_N -> doc.down();
            case GLFW.GLFW_KEY_B -> doc.left();
            case GLFW.GLFW_KEY_F -> doc.right();
            default -> {
                return false;
            }
        }
        return true;
    }

    private static void up(final TextDocument doc) {
        doc.setCursor(Math.max(0, doc.cursorLine() - SCREENFUL), 0);
    }

    private static void down(final TextDocument doc) {
        doc.setCursor(Math.min(doc.lineCount() - 1, doc.cursorLine() + SCREENFUL), 0);
    }
}
