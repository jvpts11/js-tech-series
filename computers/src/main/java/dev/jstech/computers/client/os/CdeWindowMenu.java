/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.gui.CdePalette;
import dev.jstech.computers.os.WorkspaceSet;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

/**
 * The menu behind the button at the left of a Motif title bar, which is where CDE keeps everything that can be
 * done to a window: bring it back, put it away, grow it, send it behind the others, say which workspaces it is
 * on, close it. A double click on the button closes the window without the menu, as it always did.
 *
 * <p>The menu lists only what works here. Moving and sizing by keyboard are left out, and so are the keys for
 * Lower and Close: the first belongs to the game's own debug screen and the second closes the game itself.
 */
final class CdeWindowMenu {

    private final DesktopScreen desktop;
    private final MotifMenu menu = new MotifMenu();

    /** The window the open menu speaks for. */
    private DesktopWindow target;

    /** The window whose button took the last press and when, which is what tells a double click. */
    private DesktopWindow lastPressed;
    private long lastPressedAt;

    /** The window whose own button put its menu away on the way down, so the release does not raise it again. */
    private DesktopWindow putAwayBy;

    private static final long DOUBLE_CLICK_MS = 300L;

    CdeWindowMenu(final DesktopScreen desktop) {
        this.desktop = desktop;
    }

    boolean isOpen() {
        return this.menu.isOpen();
    }

    void close() {
        this.menu.close();
    }

    /** What the open menu lists, for a test to read. */
    List<String> labels() {
        final List<String> out = new ArrayList<>();
        if (this.menu.isOpen()) {
            for (final MotifMenu.Entry entry : this.menu.entries()) {
                if (!entry.isLine()) {
                    out.add(entry.label());
                }
            }
        }
        return out;
    }

    /** The middle of the entry so labelled, in desktop pixels, or null when the menu does not list it. */
    int[] entryCentre(final String label) {
        for (int i = 0; this.menu.isOpen() && i < this.menu.entries().size(); i++) {
            if (this.menu.entries().get(i).label().equals(label)) {
                return this.menu.entryCentre(i);
            }
        }
        return null;
    }

    /** The menu button of {@code w} was pressed and let go: the second time in a moment closes the window. */
    void pressed(final DesktopWindow w) {
        final long now = System.currentTimeMillis();
        final boolean twice = w == this.lastPressed && now - this.lastPressedAt < DOUBLE_CLICK_MS;
        final boolean putAway = w == this.putAwayBy;
        this.putAwayBy = null;
        if (twice) {
            this.lastPressed = null;
            this.menu.close();
            this.desktop.closeOne(w);
            return;
        }
        this.lastPressed = w;
        this.lastPressedAt = now;
        if (!putAway) {
            openFor(w, w.x(), w.y() + DesktopWindow.TITLE_H);
        }
    }

    /** Raises the menu of {@code w} with its top left at that point, as the icon of a window put away asks. */
    void openFor(final DesktopWindow w, final int atX, final int atY) {
        this.target = w;
        this.menu.open(entriesFor(w), atX, atY, this.desktop.textFont(), this.desktop.workAreaWidth(),
                this.desktop.workAreaBottom());
    }

    /**
     * A click while the menu is up. It runs the entry it lands on and puts the menu away wherever it lands.
     *
     * @return false when the click is on the very button the menu hangs from, which goes on to the button so
     *         that a double click there still closes the window
     */
    boolean clicked(final double mx, final double my) {
        final DesktopWindow owner = this.target;
        final boolean onOwnButton = owner != null && !owner.minimized() && !this.menu.holds(mx, my)
                && owner.buttonAt(mx, my) == DesktopWindow.BUTTON_CLOSE;
        this.menu.clicked(mx, my);
        if (onOwnButton) {
            this.putAwayBy = owner;
            return false;
        }
        return true;
    }

    /** The keys Motif gave the menu's entries, which work while the front window has the keyboard. */
    boolean keyPressed(final int key, final int modifiers, final DesktopWindow front) {
        if (this.menu.isOpen() && key == GLFW.GLFW_KEY_ESCAPE) {
            this.menu.close();
            return true;
        }
        if (front == null || front.dialog() || (modifiers & GLFW.GLFW_MOD_ALT) == 0) {
            return false;
        }
        switch (key) {
            case GLFW.GLFW_KEY_F5 -> restore(front);
            case GLFW.GLFW_KEY_F9 -> front.setMinimized(true);
            case GLFW.GLFW_KEY_F10 -> maximize(front);
            default -> {
                return false;
            }
        }
        this.menu.close();
        return true;
    }

    void render(final GuiGraphics g, final int mx, final int my, final CdePalette palette) {
        this.menu.render(g, this.desktop.textFont(), mx, my, palette);
    }

    /** Only what can be done to that window now; a dialog is a question, and all it can be is closed. */
    private List<MotifMenu.Entry> entriesFor(final DesktopWindow w) {
        final List<MotifMenu.Entry> out = new ArrayList<>();
        if (!w.dialog()) {
            final boolean changed = w.minimized() || w.maximized();
            out.add(new MotifMenu.Entry(words(CdeTexts.RESTORE), "Alt+F5", changed, () -> restore(w)));
            out.add(new MotifMenu.Entry(words(CdeTexts.MINIMIZE), "Alt+F9", !w.minimized(),
                    () -> w.setMinimized(true)));
            out.add(new MotifMenu.Entry(words(CdeTexts.MAXIMIZE), "Alt+F10", !w.maximized(), () -> maximize(w)));
            out.add(new MotifMenu.Entry(words(CdeTexts.LOWER), "", !w.minimized(), () -> this.desktop.lowerOne(w)));
            out.add(MotifMenu.Entry.line());
            out.add(new MotifMenu.Entry(words(CdeTexts.OCCUPY_WORKSPACE), "", true, () -> askWorkspaces(w)));
            out.add(new MotifMenu.Entry(words(CdeTexts.OCCUPY_ALL), "", w.workspaces() != WorkspaceSet.EVERY,
                    () -> this.desktop.occupy(w, WorkspaceSet.EVERY)));
            out.add(MotifMenu.Entry.line());
        }
        out.add(new MotifMenu.Entry(words(CdeTexts.CLOSE), "", true, () -> this.desktop.closeOne(w)));
        return out;
    }

    /**
     * One step back: a window that was put away comes back as it was, grown or not, and one that is up and grown
     * goes back to the size and place it floats at.
     */
    private void restore(final DesktopWindow w) {
        if (!w.minimized() && w.maximized()) {
            w.toggleMaximize();
        }
        this.desktop.focusOne(w);
    }

    private void maximize(final DesktopWindow w) {
        this.desktop.focusOne(w);
        if (!w.maximized()) {
            w.toggleMaximize();
        }
    }

    private void askWorkspaces(final DesktopWindow w) {
        this.desktop.focusOne(w);
        DesktopScreen.openDialogFor(w.app(), new OccupyWorkspaceDialog(w.appKey(), w.workspaces(),
                chosen -> this.desktop.occupy(w, chosen)));
    }

    private static String words(final TextKey key) {
        return GameText.resolve(key);
    }
}
