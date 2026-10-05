/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.gui.help.HelpViews;
import dev.jstech.computers.gui.help.InfoHelp;
import dev.jstech.core.gui.TextScreen;
import dev.jstech.core.text.GameText;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * info on a terminal it has taken whole: the keys read into what they mean, and the glass {@link InfoHelp} draws.
 *
 * <p>The letters info has always answered to (n, p, u, t, d, l, m, s, h and q), Space and Backspace for a screen, the
 * arrows for a line, Tab for the next link and Enter to follow it; Escape or Ctrl+G stops a question in the echo area.
 */
public final class InfoKeys implements TtyEditor.IKeys {

    @Nullable
    private InfoHelp info;
    @Nullable
    private HelpSource source;

    @Override
    public void opened(final TtyEditor editor, final boolean existed) {
        this.start(editor);
    }

    @Override
    public String status(final TtyEditor editor) {
        return "";
    }

    @Override
    public TextScreen screen(final TtyEditor editor, final int columns, final int rows) {
        return this.start(editor).paint(columns, rows);
    }

    /** info as it stands, for a test to read. */
    @Nullable
    public InfoHelp info() {
        return this.info;
    }

    @Override
    public boolean key(final TtyEditor editor, final int key, final int modifiers) {
        final InfoHelp shown = this.start(editor);
        final boolean shift = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
        final boolean control = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        if (shown.asking()) {
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                shown.submit();
            } else if (key == GLFW.GLFW_KEY_ESCAPE || (control && key == GLFW.GLFW_KEY_G)) {
                shown.cancel();
            } else if (key == GLFW.GLFW_KEY_BACKSPACE) {
                shown.erase();
            }
            return true;
        }
        switch (key) {
            case GLFW.GLFW_KEY_TAB -> shown.focusNext(shift);
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> shown.follow();
            case GLFW.GLFW_KEY_UP -> shown.scroll(-1);
            case GLFW.GLFW_KEY_DOWN -> shown.scroll(1);
            case GLFW.GLFW_KEY_PAGE_UP, GLFW.GLFW_KEY_BACKSPACE -> shown.page(false);
            case GLFW.GLFW_KEY_PAGE_DOWN -> shown.page(true);
            default -> {
            }
        }
        return true;
    }

    @Override
    public boolean typed(final TtyEditor editor, final char c) {
        final InfoHelp shown = this.start(editor);
        if (shown.asking()) {
            shown.typed(c);
            return true;
        }
        switch (c) {
            case 'n' -> shown.next();
            case 'p' -> shown.previous();
            case 'u' -> shown.up();
            case 't' -> shown.top();
            case 'd' -> shown.directory();
            case 'l' -> shown.lastNode();
            case 'h' -> shown.showKeys();
            case 'm' -> shown.askMenuItem();
            case 's', '/' -> shown.askSearch();
            case ' ' -> shown.page(true);
            case 'q' -> {
                shown.quit();
                this.leave(editor);
            }
            default -> {
            }
        }
        return true;
    }

    @Override
    public boolean clicked(final TtyEditor editor, final int row, final int column) {
        this.start(editor).clicked(row, column);
        return true;
    }

    private InfoHelp start(final TtyEditor editor) {
        if (this.info == null) {
            final String view = editor.path();
            final HelpSource made = new HelpSource(editor.machine(), this::answered);
            this.source = made;
            this.info = new InfoHelp(HelpViews.namesInfo(view) ? HelpViews.infoTopic(view) : "", made,
                    GameText.LOADED);
        }
        return this.info;
    }

    private void answered() {
        if (this.info != null) {
            this.info.answered();
        }
    }

    private void leave(final TtyEditor editor) {
        if (this.source != null) {
            this.source.close();
            this.source = null;
        }
        editor.quit();
    }
}
