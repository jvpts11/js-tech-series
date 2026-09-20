/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.os.edit.CodeRuns;
import dev.jstech.computers.os.edit.InkPalette;
import dev.jstech.computers.os.edit.TtyLook;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.logic.TextDocument;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * An editor that has taken over a terminal.
 *
 * <p>It draws no window and owns no chrome: it is handed the glass a terminal was using and paints
 * rows of text on it, which is what a program that runs at a prompt gets. That is the whole reason it
 * exists, because it means a machine with no desktop, or one reached over ssh, can still be programmed.
 *
 * <p>What the keys mean is somebody else's answer. This holds the text, the view and the message line,
 * and the editors that use it differ only in how they read a keyboard and in what they keep round the text.
 */
public final class TtyEditor {

    /** How far apart the rows are unless the terminal that was taken says otherwise. */
    private static final int LINE_H = 9;
    private static final int PAD = 3;

    /** How an editor reads the keyboard. */
    public interface IKeys {

        /** A key was pressed; true if it meant something. */
        boolean key(TtyEditor editor, int key, int modifiers);

        /**
         * A cell of the glass was clicked, counted from the top left of what the editor is showing.
         *
         * <p>True when it meant something to this editor. One that says nothing gets the ordinary answer:
         * the caret goes where the pointer is, which is what an editor with a mouse has always done.
         */
        default boolean clicked(final TtyEditor editor, final int row, final int column) {
            return false;
        }

        /** A character was typed; true if it went into the text. */
        boolean typed(TtyEditor editor, char c);

        /** What the line at the bottom says: the mode, the file, whatever the editor wants there. */
        String status(TtyEditor editor);

        /** What this editor keeps round the text; a line at the bottom and nothing else unless it says so. */
        default TtyLook look(final TtyEditor editor) {
            return TtyLook.PLAIN;
        }

        /** The file has just been opened, which is when an editor says whether there was one to open. */
        default void opened(final TtyEditor editor, final boolean existed) {
            if (!existed) {
                editor.say("\"" + editor.name() + "\" [New]");
            }
        }
    }

    /** What an editor asked the terminal to do for it. */
    public interface IHost {

        /** Put the text back on the disk under that name. */
        void save(String path, String text);

        /** Fetch another file of the same machine, for an editor that can pull one into the one it has open. */
        void read(String path, IFound then);

        /** Give the terminal back; the editor is finished with it. */
        void quit();
    }

    /** Whoever asked for another file, told what was in it, or that it is not there. */
    public interface IFound {

        void found(String content, boolean existed);
    }

    private final String path;
    private final TextDocument doc = new TextDocument();
    private final IKeys keys;
    /** The terminal it has taken, which changes when the player looks away and the next look is another screen. */
    private IHost host;

    /**
     * How far apart the rows are.
     *
     * <p>A terminal that draws its text smaller says so, and gives a pitch that comes out a whole number of
     * pixels at that size: rows that land between two pixels are what makes small text look smeared.
     */
    private int lineH = LINE_H;

    private int scroll;
    /** How far the rows are slid to the left, in pixels, to keep the caret on a long line in view. */
    private int shift;
    private boolean dirty;
    private String message = "";

    /** Whatever the flavour of editor wants to remember between keys, such as a pending command. */
    private String pending = "";

    /**
     * A second buffer under the first, with its own name.
     *
     * <p>Splitting the glass is what one of these editors does and the other does not, so it lives
     * here as something a flavour may fill rather than as a thing every editor has. Empty means the
     * file has the whole screen.
     */
    private List<String> lower = List.of();
    private String lowerName = "";

    /** Puts a second buffer under the file, named the way a mode line names one. */
    public void showLower(final String name, final List<String> lines) {
        this.lowerName = name == null ? "" : name;
        this.lower = lines == null ? List.of() : List.copyOf(lines);
    }

    /** Takes the second buffer away, giving the file the whole glass again. */
    public void hideLower() {
        this.lower = List.of();
        this.lowerName = "";
    }

    /** Whether a second buffer is showing. */
    public boolean split() {
        return !this.lower.isEmpty();
    }

    /** What the second buffer shows, one line after another; empty when there is none. */
    public String lowerText() {
        return String.join("\n", this.lower);
    }

    /** The colouring of the open file, read again only when the text changes. */
    private final TtyInk ink = new TtyInk();

    public TtyEditor(final String path, final String text, final IKeys keys, final IHost host) {
        this.path = path;
        this.keys = keys;
        this.host = host;
        this.doc.setText(text);
    }

    /* What it is holding */

    /** The file it is editing. */
    public String path() {
        return this.path;
    }

    /** Just the name, which is what a status line shows. */
    public String name() {
        final int slash = this.path.lastIndexOf('/');
        return slash >= 0 && slash < this.path.length() - 1 ? this.path.substring(slash + 1) : this.path;
    }

    /** The text as it stands. */
    public String text() {
        return this.doc.text();
    }

    /** The document, for a flavour that moves the caret its own way. */
    public TextDocument document() {
        return this.doc;
    }

    /** Whether it has been changed since it was last written. */
    public boolean dirty() {
        return this.dirty;
    }

    /** Says the text changed, which is what makes a colouring stale and a quit refusable. */
    public void touched() {
        this.dirty = true;
    }

    /** What the editor wants said at the bottom, beside whatever the flavour puts there. */
    public String message() {
        return this.message;
    }

    /** Says something at the bottom. */
    public void say(final String text) {
        this.message = text == null ? "" : text;
    }

    /** Whatever the flavour is in the middle of, such as a half-typed command. */
    public String pending() {
        return this.pending;
    }

    /** Remembers what the flavour is in the middle of. */
    public void setPending(final String value) {
        this.pending = value == null ? "" : value;
    }

    /* What it can be asked to do */

    /** Puts the text back on the disk. */
    public void save() {
        this.host.save(this.path, this.doc.text());
        this.dirty = false;
        say("\"" + name() + "\" written");
    }

    /** Gives the terminal back. */
    public void quit() {
        this.host.quit();
    }

    /** Sets how far apart the rows are, for a terminal whose rows are not the usual distance apart. */
    public void setRowPitch(final int pitch) {
        this.lineH = Math.max(LINE_H, pitch);
    }

    /** Puts the editor on another terminal of the same machine, as it was, for a player who looked away and back. */
    public void handTo(final IHost terminal) {
        this.host = terminal;
    }

    /** Tells the flavour the file is open, so it can say what an editor of its kind says then. */
    public void opened(final boolean existed) {
        this.keys.opened(this, existed);
    }

    /** Fetches another file, named the way somebody at this editor would name it. */
    public void read(final String typed, final IFound then) {
        this.host.read(beside(typed), then);
    }

    /**
     * The whole name of a file typed at this editor.
     *
     * <p>It belongs to the same set of files as the one that is open, so whatever says which set that is
     * carries over. A name from the root is taken as it is; any other is a neighbour of the open file.
     */
    String beside(final String typed) {
        final int colon = this.path.indexOf(':');
        final int firstSlash = this.path.indexOf('/');
        final boolean schemed = colon >= 0 && (firstSlash < 0 || colon < firstSlash);
        final String scheme = schemed ? this.path.substring(0, colon + 1) : "";
        final String open = this.path.substring(scheme.length());
        final int slash = open.lastIndexOf('/');
        final boolean fromTheRoot = typed.startsWith("/");
        return scheme + (fromTheRoot || slash < 0 ? typed : open.substring(0, slash + 1) + typed);
    }

    /* Drawing */

    /**
     * Paints the editor over the terminal's glass.
     *
     * <p>The bottom row is the status line, the way every editor that runs at a prompt reserves one, and
     * everything above it is the file.
     */
    public void render(final GuiGraphics g, final Font font, final int x, final int y,
                       final int width, final int height, final InkPalette palette) {
        g.fill(x, y, x + width, y + height, palette.ground());
        final TtyLook look = this.keys.look(this);
        /* A title row across the top and rows of keys along the bottom, for an editor that keeps them. */
        final int head = look.titled() ? this.lineH : 0;
        final int keysH = look.keys().size() * this.lineH;
        if (look.titled()) {
            TtyChrome.title(g, font, x, y, width, this.lineH, look, palette);
        }
        TtyChrome.keys(g, font, x, y + height - keysH, width, this.lineH, look.keys(), palette);
        /*
         * With a second buffer showing, the file gets the upper half and keeps its own mode line, and
         * what is under it gets the rest. The status line under them belongs to the editor either way,
         * which is where the echo area is on a real one.
         */
        final int lowerH = this.lower.isEmpty() ? 0
                : Math.min(height / 2, (this.lower.size() + 1) * this.lineH + PAD);
        final int upperH = height - lowerH - keysH;
        if (lowerH > 0) {
            TtyChrome.lower(g, font, x, y + upperH, width, lowerH, this.lineH, this.lowerName, this.lower, palette);
        }
        final int rows = Math.max(1, (upperH - head - PAD - this.lineH) / this.lineH);
        drawStatus(g, font, x, y + height - keysH - this.lineH, width, look, palette);
        if (!look.page().isEmpty()) {
            Draw.pushScissor(g, x, y + head, x + width, y + height - keysH - this.lineH);
            drawPage(g, font, look.page(), x + PAD, y + head + PAD, rows, palette);
            Draw.popScissor(g);
            return;
        }
        followCaret(rows);
        followCaretAcross(font, width - 2 * PAD);

        final List<List<CodeRuns.Run>> runs = this.ink.of(this.path, this.doc);
        Draw.pushScissor(g, x, y + head, x + width, y + height - keysH - this.lineH);
        final int startX = x + PAD - this.shift;
        int ry = y + head + PAD;
        for (int i = this.scroll; i < this.doc.lineCount() && i - this.scroll < rows; i++) {
            drawLine(g, font, this.doc.line(i), i < runs.size() ? runs.get(i) : List.of(),
                    startX, ry, palette);
            if (i == this.doc.cursorLine()) {
                final String line = this.doc.line(i);
                final int col = Math.min(this.doc.cursorCol(), line.length());
                final int cx = startX + font.width(line.substring(0, col));
                g.fill(cx, ry - 1, cx + font.width("m"), ry + LINE_H - 1, 0x66CDD6E2);
            }
            ry += this.lineH;
        }
        /*
         * The rows past the end of the file are marked, the way a terminal editor does, so the end of a
         * short file is not mistaken for a screen of blank lines that are really there.
         */
        for (int i = this.doc.lineCount() - this.scroll; look.marksTheEnd() && i < rows; i++) {
            Draw.text(g, font, "~", x + PAD, y + head + PAD + i * this.lineH, palette.gutterText(), palette.ground());
        }
        Draw.popScissor(g);
    }

    /** Lines shown in place of the file, from wherever the wheel has left them. */
    private void drawPage(final GuiGraphics g, final Font font, final List<String> page, final int x,
                          final int y, final int rows, final InkPalette palette) {
        this.scroll = Math.max(0, Math.min(Math.max(0, page.size() - rows), this.scroll));
        for (int i = this.scroll; i < page.size() && i - this.scroll < rows; i++) {
            Draw.text(g, font, page.get(i), x, y + (i - this.scroll) * this.lineH, palette.plain(), palette.ground());
        }
    }

    private void drawStatus(final GuiGraphics g, final Font font, final int x, final int y,
                            final int width, final TtyLook look, final InkPalette palette) {
        final String left = this.keys.status(this);
        if (look.status() == TtyLook.Status.BRACKETED) {
            TtyChrome.bracketed(g, font, x, y, width, this.lineH, left, palette);
            return;
        }
        if (look.status() == TtyLook.Status.BAR) {
            TtyChrome.bar(g, font, x, y, width, this.lineH, left, palette);
            return;
        }
        g.fill(x, y, x + width, y + this.lineH, palette.gutter());
        final String where = (this.doc.cursorLine() + 1) + "," + (this.doc.cursorCol() + 1);
        // The message has the whole line but the corner where the position sits, so a question reads whole.
        Draw.text(g, font, font.plainSubstrByWidth(left, width - 2 * PAD - font.width(where) - 6), x + PAD, y,
                palette.plain(), palette.gutter());
        Draw.text(g, font, where, x + width - font.width(where) - PAD, y, palette.gutterText(), palette.gutter());
    }

    private void drawLine(final GuiGraphics g, final Font font, final String line,
                          final List<CodeRuns.Run> runs, final int startX, final int textY,
                          final InkPalette palette) {
        if (runs.isEmpty()) {
            Draw.text(g, font, line, startX, textY, palette.plain(), palette.ground());
            return;
        }
        int rx = startX;
        for (final CodeRuns.Run run : runs) {
            final int from = Math.min(run.start(), line.length());
            final int to = Math.min(run.start() + run.length(), line.length());
            if (to <= from) {
                continue;
            }
            final String piece = line.substring(from, to);
            Draw.text(g, font, piece, rx, textY, palette.of(run.ink()), palette.ground());
            rx += font.width(piece);
        }
    }

    /**
     * Slides the text sideways so the caret stays on the glass, the way a terminal editor shows a long
     * line: the rows all move together, and nothing wraps.
     */
    private void followCaretAcross(final Font font, final int room) {
        final String line = this.doc.line(this.doc.cursorLine());
        final int col = Math.min(this.doc.cursorCol(), line.length());
        final int caretX = font.width(line.substring(0, col));
        final int caretW = font.width("m");
        if (caretX - this.shift < 0) {
            this.shift = caretX;
        } else if (caretX + caretW - this.shift > room) {
            this.shift = caretX + caretW - room;
        }
        this.shift = Math.max(0, this.shift);
    }

    private void followCaret(final int rows) {
        if (this.doc.cursorLine() < this.scroll) {
            this.scroll = this.doc.cursorLine();
        } else if (this.doc.cursorLine() >= this.scroll + rows) {
            this.scroll = this.doc.cursorLine() - rows + 1;
        }
        this.scroll = Math.max(0, Math.min(Math.max(0, this.doc.lineCount() - rows), this.scroll));
    }

    /* Input */

    /** Hands a key to whichever flavour of editor this is. */
    public boolean keyPressed(final int key, final int modifiers) {
        return this.keys.key(this, key, modifiers);
    }

    /** Hands a character to it. */
    public boolean charTyped(final char c) {
        return this.keys.typed(this, c);
    }

    /** Shows whatever is on the glass from its first line, for a flavour that has just put a page there. */
    public void toTheTop() {
        this.scroll = 0;
    }

    /**
     * A cell of the glass was clicked.
     *
     * <p>The row is counted from the top of what is being shown, not of the file, since that is all a screen
     * can know: which line of the file that is depends on how far the view has scrolled, which is this
     * editor's own business.
     */
    public boolean clicked(final int row, final int column) {
        if (this.keys.clicked(this, row, column)) {
            return true;
        }
        final int line = Math.max(0, Math.min(this.doc.lineCount() - 1, this.scroll + Math.max(0, row)));
        this.doc.setCursor(line, Math.max(0, column));
        return true;
    }

    /** Moves the view without moving the caret, which is what a wheel does. */
    public boolean scrolled(final double delta) {
        this.scroll = Math.max(0, this.scroll - (int) Math.signum(delta) * 3);
        return true;
    }
}
