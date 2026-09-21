/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.client.gui.component.Label;
import dev.jstech.core.client.gui.component.Panel;
import dev.jstech.core.client.gui.component.TextField;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.client.gui.logic.TextDocument;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.function.Supplier;

/**
 * The strip along the bottom of the editor that finds text, replaces it, and goes to a line.
 *
 * <p>Its own class because it is a small machine with a state of its own: two fields, three buttons, a
 * count of what it found, and two different jobs behind one strip. Left inside the editor it would have
 * doubled that window's size for something the editor only has to show and hide.
 */
public final class EditorFindBar {

    /** How tall the strip is when it is up. */
    public static final int HEIGHT = 16;

    /** Which of the two things the strip is doing. */
    public enum Mode { FIND, GO_TO_LINE }

    private final Panel root = new Panel();
    private final Label findCaption;
    private final TextField needle = new TextField(64);
    private final Label replaceCaption;
    private final TextField replacement = new TextField(64);
    private final Button next;
    private final Button replaceAll;
    private final Label hits;

    private final Supplier<TextDocument> document;

    private Mode mode = Mode.FIND;
    private boolean open;
    private String found = "";

    public EditorFindBar(final Supplier<TextDocument> document) {
        this.document = document;
        findCaption = root.add(new Label(() -> mode == Mode.FIND ? "Find" : "Line", Label.Tone.DIM));
        root.add(needle);
        replaceCaption = root.add(new Label("Replace", Label.Tone.DIM));
        root.add(replacement);
        next = root.add(new Button(() -> mode == Mode.FIND ? "Next" : "Go", this::go));
        replaceAll = root.add(new Button("Replace all", this::replaceAll));
        hits = root.add(new Label(() -> found, Label.Tone.DIM));
    }

    public boolean isOpen() {
        return open;
    }

    public Mode mode() {
        return mode;
    }

    /** Opens the strip for one of its two jobs, with the caret already in its field. */
    public void open(final Mode what) {
        this.mode = what;
        this.open = true;
        this.found = "";
        final boolean find = what == Mode.FIND;
        replaceCaption.setVisible(find);
        replacement.setVisible(find);
        replaceAll.setVisible(find);
        if (!find) {
            needle.set("");
        }
        root.focus(needle);
    }

    public void close() {
        this.open = false;
        this.found = "";
    }

    /** Lays the strip out along the bottom of the window and draws it. */
    public void render(final GuiGraphics g, final UiContext ctx, final Font font,
                       final int x, final int y, final int width) {
        if (!open) {
            return;
        }
        final boolean find = mode == Mode.FIND;
        int bx = x + 3;
        final int capW = font.width(find ? "Find" : "Line") + 2;
        findCaption.setBounds(bx, y + 4, capW, 8);
        bx += capW + 2;
        final int fieldW = find ? Math.max(40, (width - 170) / 2) : 46;
        needle.setBounds(bx, y + 2, fieldW, 12);
        bx += fieldW + 4;
        if (find) {
            final int repW = font.width("Replace") + 2;
            replaceCaption.setBounds(bx, y + 4, repW, 8);
            bx += repW + 2;
            replacement.setBounds(bx, y + 2, fieldW, 12);
            bx += fieldW + 4;
        }
        final int nextW = font.width(find ? "Next" : "Go") + 8;
        next.setBounds(bx, y + 2, nextW, 12);
        bx += nextW + 2;
        if (find) {
            final int allW = font.width("Replace all") + 8;
            replaceAll.setBounds(bx, y + 2, allW, 12);
            bx += allW + 4;
        }
        hits.setBounds(bx, y + 4, Math.max(0, x + width - 3 - bx), 8);
        root.render(g, ctx);
    }

    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        return open && root.mouseClicked(mouseX, mouseY, button);
    }

    public boolean mouseReleased(final double mouseX, final double mouseY, final int button) {
        return open && root.mouseReleased(mouseX, mouseY, button);
    }

    public boolean charTyped(final char c) {
        return open && root.charTyped(c);
    }

    /** Whether one of the strip's own fields has the keyboard, which decides who a key belongs to. */
    public boolean hasFocus() {
        return open && (needle.isFocused() || replacement.isFocused());
    }

    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        return open && root.keyPressed(key, scanCode, modifiers);
    }

    /** Does the strip's job: the next match, or the line asked for. */
    public void go() {
        final TextDocument doc = document.get();
        if (doc == null) {
            return;
        }
        if (mode == Mode.GO_TO_LINE) {
            goToLine(doc);
            return;
        }
        final String what = needle.edit();
        if (what.isEmpty()) {
            this.found = "";
            return;
        }
        /*
         * The document's own search carries on from the caret and wraps, so the count beside the field is
         * how many there are in all rather than how many are left, which is the number a player wants.
         */
        this.found = doc.find(what) ? count(doc, what) : "none";
    }

    private void goToLine(final TextDocument doc) {
        final int line;
        try {
            line = Integer.parseInt(needle.edit().trim());
        } catch (final NumberFormatException notANumber) {
            this.found = "?";
            return;
        }
        final int target = Math.max(1, Math.min(doc.lineCount(), line));
        doc.setCursor(target - 1, 0);
        this.found = "line " + target;
    }

    private void replaceAll() {
        final TextDocument doc = document.get();
        final String what = needle.edit();
        if (doc == null || what.isEmpty()) {
            return;
        }
        final String before = doc.text();
        final String after = before.replace(what, replacement.edit());
        if (after.equals(before)) {
            this.found = "none";
            return;
        }
        /*
         * Counted before the text is replaced, because afterwards there is nothing left to count: a
         * replacement that contains what it replaced would otherwise report a number that keeps growing.
         */
        int changed = 0;
        int at = before.indexOf(what);
        while (at >= 0) {
            changed++;
            at = before.indexOf(what, at + what.length());
        }
        final int line = doc.cursorLine();
        doc.setText(after);
        doc.setCursor(Math.min(line, Math.max(0, doc.lineCount() - 1)), 0);
        this.found = changed + " replaced";
    }

    private static String count(final TextDocument doc, final String what) {
        final String text = doc.text();
        int total = 0;
        int at = text.indexOf(what);
        while (at >= 0) {
            total++;
            at = text.indexOf(what, at + what.length());
        }
        return total + (total == 1 ? " match" : " matches");
    }
}
