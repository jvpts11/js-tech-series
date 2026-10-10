/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.client.term.TermFace;
import dev.jstech.computers.client.term.TermPainter;
import dev.jstech.computers.client.term.TermText;
import dev.jstech.computers.client.term.TextScreenPainter;
import dev.jstech.computers.gui.term.TermGrid;
import dev.jstech.computers.os.edit.CodeRuns;
import dev.jstech.computers.os.edit.InkPalette;
import dev.jstech.computers.os.edit.TtyLook;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.logic.TextDocument;
import dev.jstech.core.gui.TextScreen;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

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
@PaletteHolder
public final class TtyEditor {

    /** How far apart the rows are unless the terminal that was taken says otherwise: the terminal font's height. */
    private static final int LINE_H = TermPainter.ROW;
    private static final int PAD = 3;

    /** This editor's own colours, {@code jsc:editor/tty}: the caret block over the line. */
    private static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "editor/tty",
            new Colours(0x66CDD6E2));

    /** How wide the glass is taken to be until one has been drawn, which is what a terminal held. */
    private static final int DEFAULT_COLUMNS = 80;

    /** How many rows it is taken to hold for the same reason. */
    private static final int DEFAULT_ROWS = 24;

    /** The narrowest a glass is ever said to be, so a window squeezed to nothing still asks for something. */
    private static final int LEAST_COLUMNS = 20;

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
                editor.say(TtyTexts.NEW_FILE.with(editor.name()));
            }
        }

        /**
         * The glass is another size than it was, in columns and in rows.
         *
         * <p>An editor showing a file has nothing to do about it, since a file is as wide and as long as it
         * is. One whose text is drawn to fit the glass, such as a view built of columns, asks for it again
         * at the new size.
         */
        default void resized(final TtyEditor editor, final int columns, final int rows) {
        }

        /**
         * The whole glass, cell by cell, for a program that draws its own screen in colours (a menu shell) rather
         * than showing a file; null for an editor, which keeps its text and what it draws round it.
         *
         * <p>Asked every frame with the cells the glass holds now, so the program lays its screen out to them. A
         * click on such a screen is handed over as the cell it landed on, counted from the screen's top left.
         */
        @Nullable
        default TextScreen screen(final TtyEditor editor, final int columns, final int rows) {
            return null;
        }

        /**
         * Another program this one started that has the glass now, drawn in its place, or null. Keys go to whatever
         * this one decides; the drawing and the wheel go to the program it names.
         */
        @Nullable
        default TtyEditor inner(final TtyEditor editor) {
            return null;
        }
    }

    /** What an editor asked the terminal to do for it. */
    public interface IHost {

        /** Put the text back on the disk under that name; false, with nothing sent, when it is too long to save. */
        boolean save(String path, String text);

        /** Fetch another file of the same machine, for an editor that can pull one into the one it has open. */
        void read(String path, IFound then);

        /** Give the terminal back; the editor is finished with it. */
        void quit();

        /** Where the machine is, for a program that talks to it on its own; null when the terminal does not say. */
        @Nullable
        default BlockPos machine() {
            return null;
        }
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

    /** The size of the terminal font the editor writes in: the size of the terminal it has taken. */
    private TermFace face = TermFace.SMALL;

    private int scroll;
    /*
     * A page shown in place of the file (a help page) scrolls on its own, so opening and closing one leaves the
     * file's view, and the line count read from it, exactly where they were.
     */
    private int pageScroll;
    /** How far the rows are slid to the left, in pixels, to keep the caret on a long line in view. */
    private int shift;

    /** How many columns of text the glass holds, as the last drawing of it worked out. */
    private int columns = DEFAULT_COLUMNS;

    /** How many rows of text it holds, worked out the same way. */
    private int rows = DEFAULT_ROWS;
    private boolean dirty;
    /** Whether the last drawing was a program's own screen of cells rather than a file. */
    private boolean drewScreen;
    /** The screen of cells last drawn, or null when the glass last showed a file. */
    @Nullable
    private TextScreen lastScreen;
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

    /** How many columns of text the glass holds, as the last drawing of it worked out. */
    public int columns() {
        return this.columns;
    }

    /** How many rows of text it holds, worked out the same way. */
    public int rows() {
        return this.rows;
    }

    /** How far the view has scrolled from the file's first line, for a flavour that names where it stands. */
    public int scroll() {
        return this.scroll;
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

    /** What the flavour's own status line says, for whoever must read it without drawing it. */
    public String status() {
        return this.keys.status(this);
    }

    /** What the flavour's own row naming the caret's place says, or empty for one that keeps none. */
    public String position() {
        return GameText.resolve(this.keys.look(this).positionLine());
    }

    /** Whether the flavour writes its keys above the text rather than below it. */
    public boolean keysOnTop() {
        return this.keys.look(this).keysOnTop();
    }

    /** Says something at the bottom. */
    public void say(final String text) {
        this.message = text == null ? "" : text;
    }

    /** Says a sentence at the bottom, in the player's language. */
    public void say(final Text text) {
        say(GameText.resolve(text));
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
        final String text = this.doc.text();
        if (!this.host.save(this.path, text)) {
            say(FileSaves.tooLong(text));
            return;
        }
        this.dirty = false;
        say(TtyTexts.WRITTEN.with(name()));
    }

    /** Gives the terminal back. */
    public void quit() {
        this.host.quit();
    }

    /** Sets how far apart the rows are, for a terminal whose rows are not the usual distance apart. */
    public void setRowPitch(final int pitch) {
        this.lineH = Math.max(this.face.height(), pitch);
    }

    /** Writes in that size of the terminal font, the size the terminal it has taken draws in. */
    public void setFace(final TermFace size) {
        this.face = size;
        this.lineH = Math.max(this.lineH, size.height());
    }

    /** Puts the editor on another terminal of the same machine, as it was, for a player who looked away and back. */
    public void handTo(final IHost terminal) {
        this.host = terminal;
    }

    /** Where the machine this editor runs on is, as its terminal says, or null when it does not. */
    @Nullable
    public BlockPos machine() {
        return this.host.machine();
    }

    /**
     * Whether the glass shows a program's own screen of cells, so a click on it is to be handed over as the cell it
     * landed on, counted from the screen's top left.
     */
    public boolean drawsScreen() {
        return this.drewScreen;
    }

    /** The screen of cells last drawn, for a test that reads what a program showed; null after a file. */
    @Nullable
    public TextScreen lastScreen() {
        return this.lastScreen;
    }

    /** How this editor reads the keyboard: the flavour that took the glass. */
    public IKeys keys() {
        return this.keys;
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
        // A program this one started has the glass: drawn in its place, at the same size.
        final TtyEditor inner = this.keys.inner(this);
        if (inner != null) {
            this.drewScreen = false;
            inner.setFace(this.face);
            inner.setRowPitch(this.lineH);
            inner.render(g, font, x, y, width, height, palette);
            return;
        }
        final int cellColumns = Math.max(LEAST_COLUMNS, width / this.face.width());
        final int cellRows = Math.max(1, height / this.lineH);
        final TextScreen screen = this.keys.screen(this, cellColumns, cellRows);
        this.drewScreen = screen != null;
        this.lastScreen = screen;
        if (screen != null) {
            // What the cells do not reach is the colour the screen's body stands on, not the editor's glass.
            g.fill(x, y, x + width, y + height, screen.ground(screen.columns() - 1, Math.max(0, screen.rows() - 2)));
            TextScreenPainter.paint(g, font, this.face, screen, x, y, this.lineH);
            return;
        }
        g.fill(x, y, x + width, y + height, palette.ground());
        final TtyLook look = this.keys.look(this);
        /*
         * A title row across the top, rows of keys either above the text or below it, and a row naming
         * where the caret stands right under whichever keys sit on top: what is above the text is the
         * head, and only what sits below it is taken out of the room the text itself gets.
         */
        final int titleH = look.titled() ? this.lineH : 0;
        final int keysH = look.keys().size() * this.lineH;
        final int topKeysH = look.keysOnTop() ? keysH : 0;
        final int bottomKeysH = look.keysOnTop() ? 0 : keysH;
        final int positionH = GameText.resolve(look.positionLine()).isEmpty() ? 0 : this.lineH;
        final int head = titleH + topKeysH + positionH;
        if (look.titled()) {
            TtyChrome.title(face, g, font, x, y, width, this.lineH, look, palette);
        }
        TtyChrome.keys(face, g, font, x, look.keysOnTop() ? y + titleH : y + height - bottomKeysH, width, this.lineH,
                look.keys(), palette, look.bareKeys());
        if (positionH > 0) {
            TtyChrome.message(face, g, font, x, y + titleH + topKeysH, width, paddedPosition(look, width), palette);
        }
        /*
         * With a second buffer showing, the file gets the upper half and keeps its own mode line, and
         * what is under it gets the rest. The status line under them belongs to the editor either way,
         * which is where the echo area is on a real one.
         */
        final int lowerH = this.lower.isEmpty() ? 0
                : Math.min(height / 2, (this.lower.size() + 1) * this.lineH + PAD);
        final int upperH = height - lowerH - bottomKeysH;
        if (lowerH > 0) {
            TtyChrome.lower(face, g, font, x, y + upperH, width, lowerH, this.lineH, this.lowerName, this.lower,
                    palette);
        }
        final int rows = Math.max(1, (upperH - head - PAD - this.lineH) / this.lineH);
        measure(width, rows);
        drawStatus(g, font, x, y + height - bottomKeysH - this.lineH, width, look, palette);
        final int textBottom = y + height - bottomKeysH - this.lineH;
        if (!look.page().isEmpty()) {
            Draw.pushScissor(g, x, y + head, x + width, textBottom);
            drawPage(g, font, pageLines(look.page(), width - 2 * PAD), x + PAD, y + head + PAD, rows, palette);
            Draw.popScissor(g);
            if (look.menu().up()) {
                TtyChrome.menu(face, g, font, x, y + head, width, textBottom - y - head, this.lineH, look.menu(),
                        palette);
            }
            return;
        }
        followCaret(rows);
        followCaretAcross(width - 2 * PAD);

        final List<List<CodeRuns.Run>> runs = look.plainInk() ? List.of() : this.ink.of(this.path, this.doc);
        Draw.pushScissor(g, x, y + head, x + width, textBottom);
        final int startX = x + PAD - this.shift;
        int ry = y + head + PAD;
        for (int i = this.scroll; i < this.doc.lineCount() && i - this.scroll < rows; i++) {
            drawLine(g, font, this.doc.line(i), i < runs.size() ? runs.get(i) : List.of(),
                    startX, ry, palette);
            if (i == this.doc.cursorLine()) {
                final String line = this.doc.line(i);
                final int col = Math.min(this.doc.cursorCol(), line.length());
                final int cx = startX + TermText.width(face, line.substring(0, col));
                g.fill(cx, ry, cx + face.width(), ry + face.height(), PALETTE.get().caret());
            }
            ry += this.lineH;
        }
        /*
         * The rows past the end of the file are marked, the way a terminal editor does, so the end of a
         * short file is not mistaken for a screen of blank lines that are really there.
         */
        for (int i = this.doc.lineCount() - this.scroll; look.marksTheEnd() && i < rows; i++) {
            TermText.draw(face, g, font, "~", x + PAD, y + head + PAD + i * this.lineH, palette.gutterText(),
                    palette.ground());
        }
        Draw.popScissor(g);
        /*
         * Drawn after the scissor closes and over the file already painted, rather than in place of it: a
         * box open here is a menu asking a question, not a page replacing what is being edited.
         */
        if (look.menu().up()) {
            TtyChrome.menu(face, g, font, x, y + head, width, textBottom - y - head, this.lineH, look.menu(), palette);
        }
    }

    /**
     * Works out how many columns of text this glass holds, and tells the editor when the answer changes.
     *
     * <p>A window is resized by dragging its corner, so the answer changes while the editor is up rather
     * than only when it opens, and whatever is drawn to fit the glass has to hear about it.
     */
    private void measure(final int width, final int tall) {
        final int held = Math.max(LEAST_COLUMNS, (width - 2 * PAD) / face.width());
        if (held == this.columns && tall == this.rows) {
            return;
        }
        this.columns = held;
        this.rows = tall;
        this.keys.resized(this, held, tall);
    }

    /**
     * A page's lines in the player's language, each wrapped to the glass. A wrapped line keeps the indent it
     * started with, so a paragraph set in from the edge stays set in on every row it takes.
     */
    private List<String> pageLines(final List<Text> page, final int width) {
        final List<String> out = new ArrayList<>(page.size());
        for (final Text line : page) {
            final String words = GameText.resolve(line);
            int lead = 0;
            while (lead < words.length() && words.charAt(lead) == ' ') {
                lead++;
            }
            final String indent = words.substring(0, lead);
            final int room = Math.max(1, width / face.width() - lead);
            for (final String row : TermGrid.wrap(words.substring(lead), room)) {
                out.add(indent + row);
            }
        }
        return out;
    }

    /** Lines shown in place of the file, from wherever the wheel has left them. */
    private void drawPage(final GuiGraphics g, final Font font, final List<String> page, final int x,
                          final int y, final int rows, final InkPalette palette) {
        this.pageScroll = Math.max(0, Math.min(Math.max(0, page.size() - rows), this.pageScroll));
        for (int i = this.pageScroll; i < page.size() && i - this.pageScroll < rows; i++) {
            TermText.draw(face, g, font, page.get(i), x, y + (i - this.pageScroll) * this.lineH, palette.plain(),
                    palette.ground());
        }
    }

    /**
     * The row naming where the caret stands, its words read in the player's language and then padded with
     * {@code =} out to the glass's width, the way the real editor that draws one fills the rest of the row
     * with them: the padding is not a word of any language, so it is added after the words are, not before.
     *
     * <p>Padded to the cells the row has, since the terminal font gives every character one; whatever still runs
     * over once the words are long is trimmed where the row is drawn.
     */
    private String paddedPosition(final TtyLook look, final int width) {
        final String words = GameText.resolve(look.positionLine());
        final int marks = (width - 2 * PAD) / face.width() - TermGrid.cells(words);
        return marks > 0 ? words + "=".repeat(marks) : words;
    }

    private void drawStatus(final GuiGraphics g, final Font font, final int x, final int y,
                            final int width, final TtyLook look, final InkPalette palette) {
        final String left = this.keys.status(this);
        if (look.status() == TtyLook.Status.BRACKETED) {
            TtyChrome.bracketed(face, g, font, x, y, width, this.lineH, left, palette);
            return;
        }
        if (look.status() == TtyLook.Status.BAR) {
            TtyChrome.bar(face, g, font, x, y, width, this.lineH, left, palette);
            return;
        }
        if (look.status() == TtyLook.Status.MESSAGE) {
            TtyChrome.message(face, g, font, x, y, width, left, palette);
            return;
        }
        g.fill(x, y, x + width, y + this.lineH, palette.gutter());
        final String where = (this.doc.cursorLine() + 1) + "," + (this.doc.cursorCol() + 1);
        // The message has the whole line but the corner where the position sits, so a question reads whole.
        TermText.draw(face, g, font, TermText.first(face, left, width - 2 * PAD - TermText.width(face, where) - 6),
                x + PAD, y, palette.plain(), palette.gutter());
        TermText.draw(face, g, font, where, x + width - TermText.width(face, where) - PAD, y, palette.gutterText(),
                palette.gutter());
    }

    private void drawLine(final GuiGraphics g, final Font font, final String line,
                          final List<CodeRuns.Run> runs, final int startX, final int textY,
                          final InkPalette palette) {
        if (runs.isEmpty()) {
            TermText.draw(face, g, font, line, startX, textY, palette.plain(), palette.ground());
            return;
        }
        for (final CodeRuns.Run run : runs) {
            final int from = Math.min(run.start(), line.length());
            final int to = Math.min(run.start() + run.length(), line.length());
            if (to > from) {
                // Each run starts at its own cell, so a stretch no run covers never pulls the rest out of line.
                TermText.draw(face, g, font, line.substring(from, to),
                        startX + TermText.width(face, line.substring(0, from)), textY, palette.of(run.ink()),
                        palette.ground());
            }
        }
    }

    /**
     * Slides the text sideways so the caret stays on the glass, the way a terminal editor shows a long
     * line: the rows all move together, and nothing wraps.
     */
    private void followCaretAcross(final int room) {
        final String line = this.doc.line(this.doc.cursorLine());
        final int col = Math.min(this.doc.cursorCol(), line.length());
        final int caretX = TermText.width(face, line.substring(0, col));
        final int caretW = face.width();
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

    /** Shows a page from its first line, for a flavour that has just put one on the glass. */
    public void toTheTop() {
        this.pageScroll = 0;
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

    /**
     * Moves the view without moving the caret, which is what a wheel does: the page's when one is up. On a program's
     * own screen the wheel is the arrow keys, which is what such a program scrolled its lists with.
     */
    public boolean scrolled(final double delta) {
        final TtyEditor inner = this.keys.inner(this);
        if (inner != null) {
            return inner.scrolled(delta);
        }
        if (this.drewScreen) {
            return this.keys.key(this, delta > 0 ? GLFW.GLFW_KEY_UP : GLFW.GLFW_KEY_DOWN, 0);
        }
        final int by = (int) Math.signum(delta) * 3;
        if (!this.keys.look(this).page().isEmpty()) {
            this.pageScroll = Math.max(0, this.pageScroll - by);
        } else {
            this.scroll = Math.max(0, this.scroll - by);
        }
        return true;
    }

    /** This editor's colours: the caret block drawn over the line. */
    private record Colours(int caret) {
    }
}
