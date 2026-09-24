/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.os.edit.NanoReplace;
import dev.jstech.computers.os.edit.NanoWords;
import dev.jstech.computers.os.edit.TtyLook;
import dev.jstech.core.client.gui.logic.TextDocument;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import java.util.List;
import org.lwjgl.glfw.GLFW;

/**
 * nano, as far as a keyboard is concerned.
 *
 * <p>It has no modes and no runs of keys: every key types, and everything else is one key held with
 * Control. What it does have is questions. Writing a file, leaving one that has changed, searching and
 * replacing each put a question on the row above the keys, and until it is answered or cancelled the
 * keyboard belongs to the answer.
 *
 * <p>It is the editor a live medium carries, which is why it is here: the files a by-hand install asks to
 * have edited are edited with this.
 */
public final class NanoKeys implements TtyEditor.IKeys {

    private final StringBuilder answer = new StringBuilder();
    private Asking asking = Asking.NOTHING;

    /** Whether the write being named ends with leaving, which it does when leaving is what asked for it. */
    private boolean leaving;

    /** What was last searched for, which a search offers again. */
    private String needle = "";

    /** The pass of replacing that is stopping at each place, while there is one. */
    private NanoReplace replacing;

    /** What the cuts have taken, which is what a paste puts back. */
    private String cut = "";

    /** Whether the last key was a cut, so that the next one adds to what was taken instead of starting over. */
    private boolean cutting;

    /**
     * The letter of the Meta key just handled, or zero.
     *
     * <p>A keyboard reports Alt held with a letter twice over, as the key and as the letter it would have
     * typed, and the second of those is part of the first and not something to put in the file.
     */
    private char metaLetter;

    /** What the row above the keys is waiting to be told. */
    private enum Asking {
        NOTHING, SAVE_ON_EXIT, FILE_NAME, SEARCH, REPLACE_WHAT, REPLACE_WITH, REPLACE_EACH, READ_NAME, HELP
    }

    @Override
    public String status(final TtyEditor editor) {
        return switch (this.asking) {
            case SAVE_ON_EXIT -> GameText.resolve(NanoWords.SAVE_MODIFIED);
            case FILE_NAME -> GameText.resolve(NanoWords.FILE_NAME.with(NanoWords.shown(editor.path())));
            case SEARCH -> GameText.resolve(
                    NanoWords.searching(NanoWords.SEARCH, this.needle, this.answer.toString()));
            case REPLACE_WHAT -> GameText.resolve(NanoWords.searching(NanoWords.SEARCH_TO_REPLACE, this.needle,
                    this.answer.toString()));
            case REPLACE_WITH -> GameText.resolve(NanoWords.REPLACE_WITH.with(this.answer.toString()));
            case REPLACE_EACH -> GameText.resolve(NanoWords.REPLACE_THIS);
            case READ_NAME -> GameText.resolve(NanoWords.FILE_TO_INSERT.with(this.answer.toString()));
            case NOTHING, HELP -> editor.message();
        };
    }

    @Override
    public TtyLook look(final TtyEditor editor) {
        final String file = NanoWords.shown(editor.path());
        final boolean passing = this.asking == Asking.NOTHING || this.asking == Asking.HELP;
        return new TtyLook(NanoWords.VERSION, file.isEmpty() ? NanoWords.NEW_BUFFER.text() : Text.literal(file),
                editor.dirty() ? NanoWords.MODIFIED.text() : Text.EMPTY,
                passing ? TtyLook.Status.BRACKETED : TtyLook.Status.BAR,
                switch (this.asking) {
                    case NOTHING -> NanoWords.EDITING;
                    case HELP -> NanoWords.READING_HELP;
                    case SAVE_ON_EXIT -> NanoWords.YES_OR_NO;
                    case REPLACE_EACH -> NanoWords.EACH_OR_ALL;
                    case FILE_NAME, SEARCH, REPLACE_WHAT, REPLACE_WITH, READ_NAME -> NanoWords.TYPING;
                },
                this.asking == Asking.HELP ? NanoWords.HELP : List.of(), false);
    }

    @Override
    public void opened(final TtyEditor editor, final boolean existed) {
        editor.say(existed ? NanoWords.read(lines(editor.document())) : NanoWords.NEW_FILE.text());
    }

    @Override
    public boolean typed(final TtyEditor editor, final char c) {
        if (c < 32 || c == 127) {
            return false;
        }
        if (this.metaLetter != 0 && Character.toLowerCase(c) == this.metaLetter) {
            this.metaLetter = 0;
            return true;
        }
        switch (this.asking) {
            case NOTHING -> {
                editor.say("");
                this.cutting = false;
                editor.document().insert(c);
                editor.touched();
            }
            case SAVE_ON_EXIT -> saveOnExit(editor, Character.toLowerCase(c));
            case REPLACE_EACH -> replaceEach(editor, Character.toLowerCase(c));
            case SEARCH, REPLACE_WHAT, REPLACE_WITH, READ_NAME -> this.answer.append(c);
            case FILE_NAME, HELP -> {
            }
        }
        return true;
    }

    @Override
    public boolean key(final TtyEditor editor, final int key, final int modifiers) {
        // Holding Control down is on its way to being a keystroke and is not one yet.
        if (key >= GLFW.GLFW_KEY_LEFT_SHIFT && key <= GLFW.GLFW_KEY_RIGHT_SUPER) {
            return true;
        }
        this.metaLetter = 0;
        final boolean control = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        final boolean alt = (modifiers & GLFW.GLFW_MOD_ALT) != 0;
        if (control && alt) {
            // The key that types a keyboard's third symbols reports as both, and what it types comes by itself.
            return true;
        }
        if (this.asking != Asking.NOTHING) {
            answering(editor, key, control);
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            /*
             * With no question standing this editor has no use for Escape, and says so: the terminal is then
             * free to let the player look away, keeping the editor as it is for when they look back.
             */
            return false;
        }
        editor.say("");
        final boolean wasCutting = this.cutting;
        this.cutting = false;
        if (control) {
            held(editor, key, wasCutting);
        } else if (alt) {
            meta(editor, key);
        } else {
            NanoMoves.alone(editor, key);
        }
        return true;
    }

    /** A key held with Control, which is how this editor is told to do anything but type. */
    private void held(final TtyEditor editor, final int key, final boolean wasCutting) {
        final TextDocument doc = editor.document();
        switch (key) {
            case GLFW.GLFW_KEY_G -> {
                this.asking = Asking.HELP;
                editor.toTheTop();
            }
            case GLFW.GLFW_KEY_X -> {
                if (editor.dirty()) {
                    this.asking = Asking.SAVE_ON_EXIT;
                } else {
                    editor.quit();
                }
            }
            case GLFW.GLFW_KEY_O -> ask(Asking.FILE_NAME);
            case GLFW.GLFW_KEY_W -> ask(Asking.SEARCH);
            case GLFW.GLFW_KEY_BACKSLASH -> ask(Asking.REPLACE_WHAT);
            case GLFW.GLFW_KEY_R -> ask(Asking.READ_NAME);
            case GLFW.GLFW_KEY_K -> cutLine(editor, wasCutting);
            case GLFW.GLFW_KEY_U -> {
                if (!this.cut.isEmpty()) {
                    doc.setCursor(doc.cursorLine(), 0);
                    doc.insertText(this.cut);
                    editor.touched();
                }
            }
            case GLFW.GLFW_KEY_C -> editor.say(position(doc));
            default -> NanoMoves.held(doc, key);
        }
    }

    /** A key held with Alt, which this editor calls Meta: undoing and doing again. */
    private void meta(final TtyEditor editor, final int key) {
        final TextDocument doc = editor.document();
        if (key != GLFW.GLFW_KEY_U && key != GLFW.GLFW_KEY_E) {
            return;
        }
        this.metaLetter = key == GLFW.GLFW_KEY_U ? 'u' : 'e';
        if (key == GLFW.GLFW_KEY_U ? doc.undo() : doc.redo()) {
            editor.touched();
        } else {
            editor.say((key == GLFW.GLFW_KEY_U ? NanoWords.NOTHING_TO_UNDO : NanoWords.NOTHING_TO_REDO).text());
        }
    }

    /** A key pressed while a question stands: it answers, takes a character back, or cancels. */
    private void answering(final TtyEditor editor, final int key, final boolean control) {
        if (this.asking == Asking.HELP) {
            if (key == GLFW.GLFW_KEY_ESCAPE || (control && key == GLFW.GLFW_KEY_X)) {
                this.asking = Asking.NOTHING;
            } else if (key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_PAGE_UP) {
                editor.scrolled(1);
            } else if (key == GLFW.GLFW_KEY_DOWN || key == GLFW.GLFW_KEY_PAGE_DOWN) {
                editor.scrolled(-1);
            }
            return;
        }
        // Escape backs out of a question the way Control and C does, which is what anybody tries first.
        if ((control && key == GLFW.GLFW_KEY_C) || key == GLFW.GLFW_KEY_ESCAPE) {
            final boolean midway = this.asking == Asking.REPLACE_EACH && this.replacing != null;
            editor.document().clearSelection();
            editor.say(midway ? NanoWords.replaced(this.replacing.done()) : NanoWords.CANCELLED.text());
            this.replacing = null;
            this.asking = Asking.NOTHING;
        } else if (key == GLFW.GLFW_KEY_BACKSPACE && !this.answer.isEmpty()) {
            this.answer.setLength(this.answer.length() - 1);
        } else if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            answered(editor);
        }
    }

    /** Enter at a question that is answered by typing. */
    private void answered(final TtyEditor editor) {
        final String typed = this.answer.toString();
        final Asking was = this.asking;
        this.asking = Asking.NOTHING;
        switch (was) {
            case FILE_NAME -> {
                editor.save();
                editor.say(NanoWords.wrote(lines(editor.document())));
                if (this.leaving) {
                    editor.quit();
                }
            }
            case SEARCH -> {
                this.needle = typed.isEmpty() ? this.needle : typed;
                search(editor);
            }
            case REPLACE_WHAT -> {
                this.needle = typed.isEmpty() ? this.needle : typed;
                if (this.needle.isEmpty()) {
                    editor.say(NanoWords.CANCELLED.text());
                } else {
                    ask(Asking.REPLACE_WITH);
                }
            }
            case REPLACE_WITH -> {
                this.replacing = new NanoReplace(editor.document(), this.needle, typed);
                stopAtTheNext(editor);
            }
            case READ_NAME -> insertFile(editor, typed);
            // The two that are answered with a letter take no notice of Enter, and go on asking.
            case SAVE_ON_EXIT, REPLACE_EACH -> this.asking = was;
            case NOTHING, HELP -> {
            }
        }
    }

    private void ask(final Asking what) {
        this.asking = what;
        this.leaving = false;
        this.answer.setLength(0);
    }

    /** The letter typed at the question about leaving a file that has changed. */
    private void saveOnExit(final TtyEditor editor, final char letter) {
        if (letter == 'y') {
            ask(Asking.FILE_NAME);
            this.leaving = true;
        } else if (letter == 'n') {
            editor.quit();
        }
    }

    /** The letter typed at a place a replacement could be made. */
    private void replaceEach(final TtyEditor editor, final char letter) {
        if (this.replacing == null || (letter != 'y' && letter != 'n' && letter != 'a')) {
            return;
        }
        if (letter == 'a') {
            this.replacing.all();
            editor.touched();
            finished(editor);
            return;
        }
        if (letter == 'y') {
            this.replacing.replace();
            editor.touched();
        } else {
            this.replacing.skip();
        }
        stopAtTheNext(editor);
    }

    private void stopAtTheNext(final TtyEditor editor) {
        if (this.replacing.next()) {
            this.asking = Asking.REPLACE_EACH;
        } else {
            finished(editor);
        }
    }

    private void finished(final TtyEditor editor) {
        editor.document().clearSelection();
        editor.say(this.replacing.done() == 0 ? NanoWords.notFound(this.needle)
                : NanoWords.replaced(this.replacing.done()));
        this.replacing = null;
        this.asking = Asking.NOTHING;
    }

    /** Looks for the text after the cursor, going round to the top when there is nothing further down. */
    private void search(final TtyEditor editor) {
        final TextDocument doc = editor.document();
        final int fromLine = doc.cursorLine();
        final int fromCol = doc.cursorCol();
        if (!doc.find(this.needle)) {
            editor.say(NanoWords.notFound(this.needle));
            return;
        }
        doc.clearSelection();
        final boolean wentRound = doc.cursorLine() < fromLine
                || (doc.cursorLine() == fromLine && doc.cursorCol() <= fromCol);
        editor.say(wentRound ? NanoWords.SEARCH_WRAPPED.text() : Text.EMPTY);
    }

    /** Takes the cursor's line out of the file and keeps it, together with any taken just before it. */
    private void cutLine(final TtyEditor editor, final boolean wasCutting) {
        final TextDocument doc = editor.document();
        final int at = doc.cursorLine();
        final String line = doc.line(at);
        if (at < doc.lineCount() - 1) {
            doc.select(at, 0, at + 1, 0);
        } else {
            doc.select(at, 0, at, line.length());
        }
        doc.deleteSelection();
        this.cut = (wasCutting ? this.cut : "") + line + "\n";
        this.cutting = true;
        editor.touched();
    }

    /** Pulls another file in where the cursor is, once the machine has said what is in it. */
    private static void insertFile(final TtyEditor editor, final String name) {
        if (name.isBlank()) {
            editor.say(NanoWords.CANCELLED.text());
            return;
        }
        editor.read(name.trim(), (content, existed) -> {
            if (!existed) {
                editor.say(NanoWords.noSuchFile(name.trim()));
                return;
            }
            editor.document().insertText(content);
            editor.touched();
            editor.say(NanoWords.read(content.isEmpty() ? 0 : content.split("\n", -1).length));
        });
    }

    /**
     * How many lines the file has, the way this editor counts them: the empty one after the last line's end
     * is where the file stops and not a line of it.
     */
    private static int lines(final TextDocument doc) {
        final int count = doc.lineCount();
        return count > 0 && doc.line(count - 1).isEmpty() ? count - 1 : count;
    }

    private static Text position(final TextDocument doc) {
        int before = 0;
        int all = 0;
        for (int i = 0; i < doc.lineCount(); i++) {
            final int length = doc.line(i).length() + 1;
            before += i < doc.cursorLine() ? length : 0;
            all += length;
        }
        final int columns = doc.line(doc.cursorLine()).length() + 1;
        return NanoWords.position(doc.cursorLine() + 1, doc.lineCount(), doc.cursorCol() + 1, columns,
                before + doc.cursorCol() + 1, all);
    }
}
