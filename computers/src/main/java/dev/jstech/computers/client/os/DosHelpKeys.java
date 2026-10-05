/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.gui.help.DosHelp;
import dev.jstech.computers.gui.help.HelpViews;
import dev.jstech.computers.os.Platform;
import dev.jstech.core.gui.TextScreen;
import dev.jstech.core.text.GameText;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * MC-DOS's and MC-NET's HELP on a terminal it has taken whole: the keys a person presses read into what they mean, and
 * the screen {@link DosHelp} draws.
 *
 * <p>Tab and Shift+Tab pick the buttons and links, Enter follows, the arrows and Page Up and Down move the topic, Alt
 * with C, N, B or I does what the buttons do, Alt with F or S puts a menu down, F3 finds again, and Escape leaves.
 */
public final class DosHelpKeys implements TtyEditor.IKeys {

    @Nullable
    private DosHelp help;
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

    /** The help being shown, for a test to read. */
    @Nullable
    public DosHelp help() {
        return this.help;
    }

    @Override
    public boolean key(final TtyEditor editor, final int key, final int modifiers) {
        final DosHelp shown = this.start(editor);
        final boolean alt = (modifiers & GLFW.GLFW_MOD_ALT) != 0;
        final boolean shift = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
        if (shown.finding()) {
            switch (key) {
                case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> shown.submitFind();
                case GLFW.GLFW_KEY_ESCAPE -> shown.cancelFind();
                case GLFW.GLFW_KEY_BACKSPACE -> shown.erase();
                default -> {
                }
            }
            return true;
        }
        if (shown.menuOpen()) {
            switch (key) {
                case GLFW.GLFW_KEY_ESCAPE, GLFW.GLFW_KEY_F10 -> shown.closeMenu();
                case GLFW.GLFW_KEY_UP -> shown.menuMove(-1);
                case GLFW.GLFW_KEY_DOWN -> shown.menuMove(1);
                case GLFW.GLFW_KEY_LEFT -> shown.menuAcross(-1);
                case GLFW.GLFW_KEY_RIGHT -> shown.menuAcross(1);
                case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> shown.pickMenuItem();
                default -> {
                }
            }
            this.leaveIfAsked(editor, shown);
            return true;
        }
        if (alt) {
            switch (key) {
                case GLFW.GLFW_KEY_C -> shown.contents();
                case GLFW.GLFW_KEY_N -> shown.next();
                case GLFW.GLFW_KEY_B -> shown.back();
                case GLFW.GLFW_KEY_I -> shown.index();
                case GLFW.GLFW_KEY_F -> shown.openMenu(0);
                case GLFW.GLFW_KEY_S -> shown.openMenu(1);
                case GLFW.GLFW_KEY_F4 -> this.leave(editor);
                default -> {
                }
            }
            return true;
        }
        switch (key) {
            case GLFW.GLFW_KEY_TAB -> shown.focusNext(shift);
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> shown.follow();
            case GLFW.GLFW_KEY_UP -> shown.scroll(-1);
            case GLFW.GLFW_KEY_DOWN -> shown.scroll(1);
            case GLFW.GLFW_KEY_PAGE_UP -> shown.page(false);
            case GLFW.GLFW_KEY_PAGE_DOWN -> shown.page(true);
            case GLFW.GLFW_KEY_HOME -> shown.scroll(-Integer.MAX_VALUE / 2);
            case GLFW.GLFW_KEY_END -> shown.scroll(Integer.MAX_VALUE / 2);
            case GLFW.GLFW_KEY_F10 -> shown.openMenu(0);
            case GLFW.GLFW_KEY_F3 -> shown.findAgain();
            case GLFW.GLFW_KEY_ESCAPE -> this.leave(editor);
            default -> {
            }
        }
        return true;
    }

    @Override
    public boolean typed(final TtyEditor editor, final char c) {
        final DosHelp shown = this.start(editor);
        if (shown.finding()) {
            shown.typed(c);
        }
        return true;
    }

    @Override
    public boolean clicked(final TtyEditor editor, final int row, final int column) {
        final DosHelp shown = this.start(editor);
        shown.clicked(row, column);
        this.leaveIfAsked(editor, shown);
        return true;
    }

    /** The help, made the first time it is needed, on the topic the machine opened it on. */
    private DosHelp start(final TtyEditor editor) {
        if (this.help == null) {
            final String view = editor.path();
            final Platform system = Platform.byName(HelpViews.namesDos(view) ? HelpViews.dosSystem(view) : "");
            final HelpSource made = new HelpSource(editor.machine(), this::answered);
            this.source = made;
            this.help = new DosHelp((system == null ? Platform.MC_DOS : system).label(),
                    HelpViews.namesDos(view) ? HelpViews.dosTopic(view) : "", made, GameText.LOADED);
        }
        return this.help;
    }

    private void answered() {
        if (this.help != null) {
            this.help.answered();
        }
    }

    private void leaveIfAsked(final TtyEditor editor, final DosHelp shown) {
        if (shown.leaving()) {
            this.leave(editor);
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
