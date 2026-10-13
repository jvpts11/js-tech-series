/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.os.edit.TtyLook;
import dev.jstech.core.text.GameText;
import java.util.Locale;
import org.lwjgl.glfw.GLFW;

/**
 * Reading a long file a page at a time: the pager, on a terminal it has taken whole.
 *
 * <p>The same glass an editor takes, and for the same reason: text longer than the screen is read by moving
 * through it, not by watching it scroll past. Nothing here writes, which is the one thing that makes a pager a
 * pager: every key either moves, searches, or leaves.
 *
 * <p>A page at a time with the space bar, a line at a time with the arrows, {@code /} to look for something,
 * {@code n} for the next one of those, {@code g} and {@code G} for the two ends, and {@code q} to give the
 * terminal back. Those are the keys that pager has answered to for forty years.
 */
public final class LessKeys implements TtyEditor.IKeys {

    /** What is being typed after a slash, while one is. */
    private final StringBuilder asking = new StringBuilder();

    /** Whether a search is being typed. */
    private boolean searching;

    /** What was last looked for, so the next one can be found without typing it again. */
    private String needle = "";

    /** How many rows a page moves by, which is a screen less the line that says where you are. */
    private static final int PAGE = 18;

    @Override
    public TtyLook look(final TtyEditor editor) {
        return TtyLook.PLAIN;
    }

    @Override
    public String status(final TtyEditor editor) {
        if (this.searching) {
            return "/" + this.asking;
        }
        final int line = editor.document().cursorLine() + 1;
        final int of = Math.max(1, editor.document().lineCount());
        return GameText.resolve(TtyTexts.LESS_STATUS.with(editor.name(), line, of, line * 100 / of));
    }

    /** A file opened for reading says nothing about whether it was there; the command already did. */
    @Override
    public void opened(final TtyEditor editor, final boolean existed) {
        if (!existed) {
            editor.say(TtyTexts.NO_SUCH_FILE.with(editor.name()));
        }
    }

    @Override
    public boolean typed(final TtyEditor editor, final char c) {
        if (this.searching) {
            this.asking.append(c);
            return true;
        }
        switch (Character.toLowerCase(c)) {
            case ' ' -> page(editor, PAGE);
            case 'b' -> page(editor, -PAGE);
            case 'j' -> editor.document().down();
            case 'k' -> editor.document().up();
            case '/' -> {
                this.searching = true;
                this.asking.setLength(0);
            }
            case 'n' -> find(editor, this.needle);
            case 'q' -> editor.quit();
            case 'g' -> {
                if (Character.isUpperCase(c)) {
                    page(editor, editor.document().lineCount());
                } else {
                    page(editor, -editor.document().lineCount());
                }
            }
            default -> {
                return true;
            }
        }
        return true;
    }

    @Override
    public boolean key(final TtyEditor editor, final int key, final int modifiers) {
        if (this.searching) {
            return typing(editor, key);
        }
        switch (key) {
            case GLFW.GLFW_KEY_DOWN -> editor.document().down();
            case GLFW.GLFW_KEY_UP -> editor.document().up();
            case GLFW.GLFW_KEY_PAGE_DOWN -> page(editor, PAGE);
            case GLFW.GLFW_KEY_PAGE_UP -> page(editor, -PAGE);
            case GLFW.GLFW_KEY_HOME -> page(editor, -editor.document().lineCount());
            case GLFW.GLFW_KEY_END -> page(editor, editor.document().lineCount());
            case GLFW.GLFW_KEY_ESCAPE -> editor.quit();
            default -> {
                return true;
            }
        }
        return true;
    }

    /** The keys of a search being typed: it is run on Enter and dropped on Escape. */
    private boolean typing(final TtyEditor editor, final int key) {
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            this.searching = false;
            this.needle = this.asking.toString();
            find(editor, this.needle);
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            this.searching = false;
            this.asking.setLength(0);
            return true;
        }
        if (key == GLFW.GLFW_KEY_BACKSPACE && !this.asking.isEmpty()) {
            this.asking.setLength(this.asking.length() - 1);
        }
        return true;
    }

    /** Moves that many lines, stopping at either end of the file. */
    private static void page(final TtyEditor editor, final int lines) {
        for (int i = 0; i < Math.abs(lines); i++) {
            if (lines > 0) {
                editor.document().down();
            } else {
                editor.document().up();
            }
        }
    }

    /** Puts the caret on the next line holding that text, or says there is none below. */
    private static void find(final TtyEditor editor, final String text) {
        if (text.isEmpty()) {
            return;
        }
        final String[] lines = editor.text().split("\n", -1);
        final String wanted = text.toLowerCase(Locale.ROOT);
        for (int i = editor.document().cursorLine() + 1; i < lines.length; i++) {
            if (lines[i].toLowerCase(Locale.ROOT).contains(wanted)) {
                page(editor, i - editor.document().cursorLine());
                editor.say("");
                return;
            }
        }
        editor.say(TtyTexts.PATTERN_NOT_FOUND.with(text));
    }
}
