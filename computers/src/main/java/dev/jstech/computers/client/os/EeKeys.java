/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.os.edit.EePosition;
import dev.jstech.computers.os.edit.EeWords;
import dev.jstech.computers.os.edit.TtyLineCount;
import dev.jstech.computers.os.edit.TtyLook;
import dev.jstech.core.client.gui.logic.TextDocument;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.glfw.GLFW;

/**
 * FreeBSD's {@code ee}, as far as a keyboard is concerned.
 *
 * <p>It has no modes: every key types, and everything it can do beyond that is a key held with Control,
 * written where anybody can read it, on the glass itself. That is the whole idea behind an editor built
 * for a newcomer: nothing to remember, because nothing is hidden.
 *
 * <p>Escape opens its menu instead of typing anything, since {@code ee} has no colon commands of Vim's
 * kind; leaving through it always asks whether to keep what was typed. Control-C opens a plain command
 * line for the few who want one, which the menu does not have to know about.
 */
public final class EeKeys implements TtyEditor.IKeys {

    /** What a prompt at the bottom is waiting to be told. */
    private enum Asking { NONE, SEARCH, ASCII, COMMAND, READ_NAME }

    private Asking asking = Asking.NONE;
    private final StringBuilder answer = new StringBuilder();
    private String needle = "";
    private String lastDeletedLine = "";
    private String lastDeletedWord = "";
    private char lastDeletedChar;
    private boolean menuUp;
    private boolean leaveMenuUp;
    private boolean fileOpsUp;
    private boolean settingsUp;
    private boolean helpUp;
    private int menuSelected;
    private int leaveSelected;
    private int fileOpsSelected;
    private int settingsSelected;

    /** Whether the five rows of shortcuts show, the one of ee's own settings this editor can really honour. */
    private boolean infoWindowOn = true;

    @Override
    public TtyLook look(final TtyEditor editor) {
        final TextDocument doc = editor.document();
        final EePosition at = EePosition.of(doc.cursorLine(), doc.cursorCol(), editor.scroll());
        final Text position = EeWords.position(at.line(), at.column(), at.fromTop());
        final List<List<TtyLook.Key>> keys = this.infoWindowOn ? EeWords.KEYS : List.of();
        return new TtyLook("", Text.EMPTY, Text.EMPTY, TtyLook.Status.MESSAGE, keys, helpPage(), false,
                true, true, position, true, menu());
    }

    @Override
    public String status(final TtyEditor editor) {
        if (this.helpUp) {
            // Shown on the row that never scrolls, so it reads even once the page itself runs past the glass.
            return GameText.resolve(EeWords.HELP_CONTINUE.text());
        }
        return switch (this.asking) {
            case SEARCH -> GameText.resolve(EeWords.asking(EeWords.SEARCH_PROMPT_LABEL, this.answer.toString()));
            case ASCII -> GameText.resolve(EeWords.asking(EeWords.ASCII_PROMPT_LABEL, this.answer.toString()));
            case COMMAND -> GameText.resolve(EeWords.asking(EeWords.COMMAND_PROMPT_LABEL, this.answer.toString()));
            case READ_NAME -> GameText.resolve(EeWords.asking(EeWords.FILE_TO_READ_LABEL, this.answer.toString()));
            case NONE -> editor.message();
        };
    }

    /** A file opened for editing says what it is, whether it was there before or not, the way the real one does. */
    @Override
    public void opened(final TtyEditor editor, final boolean existed) {
        editor.say(existed
                ? EeWords.opened(editor.name(), TtyLineCount.of(editor.document()), editor.text().length())
                : EeWords.newFile(editor.name()));
    }

    @Override
    public boolean typed(final TtyEditor editor, final char c) {
        if (this.helpUp) {
            this.helpUp = false;
            return true;
        }
        if (this.leaveMenuUp) {
            leaveMenuLetter(editor, c);
            return true;
        }
        if (this.fileOpsUp) {
            fileOpsLetter(editor, c);
            return true;
        }
        if (this.settingsUp) {
            settingsLetter(editor, c);
            return true;
        }
        if (this.menuUp) {
            menuLetter(editor, c);
            return true;
        }
        if (this.asking == Asking.ASCII) {
            if (Character.isDigit(c)) {
                this.answer.append(c);
            }
            return true;
        }
        if (this.asking != Asking.NONE) {
            this.answer.append(c);
            return true;
        }
        if (c < 32 || c == 127) {
            return false;
        }
        editor.document().insert(c);
        editor.touched();
        return true;
    }

    @Override
    public boolean key(final TtyEditor editor, final int key, final int modifiers) {
        if (this.helpUp) {
            return helpKey(key, modifiers);
        }
        if (this.leaveMenuUp) {
            return leaveMenuKey(editor, key);
        }
        if (this.fileOpsUp) {
            return fileOpsKey(editor, key);
        }
        if (this.settingsUp) {
            return settingsKey(editor, key);
        }
        if (this.menuUp) {
            return menuKey(editor, key);
        }
        if (this.asking != Asking.NONE) {
            return answeringKey(editor, key);
        }
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            this.menuUp = true;
            this.menuSelected = 0;
            return true;
        }
        if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
            return controlKey(editor, key);
        }
        return movementKey(editor, key);
    }

    /* The page "help" opens */

    /**
     * Closes on any key, the way the real editor ends this page: "press any key to continue". A bare modifier
     * is swallowed without closing it, since holding Shift or Control types nothing on its own. A key that types
     * a character is swallowed too, rather than closing the page here: the paired {@link #typed} call is what
     * closes it and eats the character, so it never reaches the file. Control held with it types nothing (a
     * chord such as ^c), unless Alt is held as well, which is AltGr typing a character on many layouts.
     */
    private boolean helpKey(final int key, final int modifiers) {
        if (key >= GLFW.GLFW_KEY_LEFT_SHIFT && key <= GLFW.GLFW_KEY_RIGHT_SUPER) {
            return true;
        }
        final boolean control = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        final boolean altGr = control && (modifiers & GLFW.GLFW_MOD_ALT) != 0;
        final boolean printable = key >= GLFW.GLFW_KEY_SPACE && key <= GLFW.GLFW_KEY_WORLD_2
                || key >= GLFW.GLFW_KEY_KP_0 && key <= GLFW.GLFW_KEY_KP_EQUAL && key != GLFW.GLFW_KEY_KP_ENTER;
        if (!printable || control && !altGr) {
            this.helpUp = false;
        }
        return true;
    }

    /* The menu Escape opens */

    private void menuLetter(final TtyEditor editor, final char c) {
        final int index = Character.toLowerCase(c) - 'a';
        if (index >= 0 && index < EeWords.MENU_ITEMS.size()) {
            chooseMenuItem(editor, index);
        }
    }

    private boolean menuKey(final TtyEditor editor, final int key) {
        final int count = EeWords.MENU_ITEMS.size();
        switch (key) {
            case GLFW.GLFW_KEY_ESCAPE -> this.menuUp = false;
            case GLFW.GLFW_KEY_UP -> this.menuSelected = (this.menuSelected + count - 1) % count;
            case GLFW.GLFW_KEY_DOWN -> this.menuSelected = (this.menuSelected + 1) % count;
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> chooseMenuItem(editor, this.menuSelected);
            default -> {
                return false;
            }
        }
        return true;
    }

    /**
     * The letter chosen. "leave editor" opens the screen asking whether to keep what was typed, "help" opens
     * the page of keys, "file operations" opens the choice between reading a file in and saving the one
     * open, "settings" opens the one of ee's own settings this editor can really honour, and "search" opens
     * the same prompt Control and Y does. Redrawing the screen has nothing more to do than close the menu,
     * since this glass is always drawn whole and closing the menu already puts the file back on it.
     */
    private void chooseMenuItem(final TtyEditor editor, final int index) {
        this.menuUp = false;
        switch (index) {
            case 0 -> {
                this.leaveMenuUp = true;
                this.leaveSelected = 0;
            }
            case 1 -> {
                this.helpUp = true;
                editor.toTheTop();
            }
            case 2 -> {
                this.fileOpsUp = true;
                this.fileOpsSelected = 0;
            }
            case 4 -> {
                this.settingsUp = true;
                this.settingsSelected = this.infoWindowOn ? 0 : 1;
            }
            case 5 -> ask(Asking.SEARCH);
            default -> { }
        }
    }

    /* The question "leave editor" asks */

    private void leaveMenuLetter(final TtyEditor editor, final char c) {
        final int index = Character.toLowerCase(c) - 'a';
        if (index == 0 || index == 1) {
            chooseLeaveItem(editor, index);
        }
    }

    private boolean leaveMenuKey(final TtyEditor editor, final int key) {
        switch (key) {
            case GLFW.GLFW_KEY_ESCAPE -> this.leaveMenuUp = false;
            case GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_DOWN -> this.leaveSelected = 1 - this.leaveSelected;
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> chooseLeaveItem(editor, this.leaveSelected);
            default -> {
                return false;
            }
        }
        return true;
    }

    private void chooseLeaveItem(final TtyEditor editor, final int index) {
        this.leaveMenuUp = false;
        if (index == 0) {
            editor.save();
        }
        editor.quit();
    }

    /* What "file operations" offers: read a file in at the caret, or save the one open */

    private void fileOpsLetter(final TtyEditor editor, final char c) {
        final int index = Character.toLowerCase(c) - 'a';
        if (index == 0 || index == 1) {
            chooseFileOp(editor, index);
        }
    }

    private boolean fileOpsKey(final TtyEditor editor, final int key) {
        switch (key) {
            case GLFW.GLFW_KEY_ESCAPE -> this.fileOpsUp = false;
            case GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_DOWN -> this.fileOpsSelected = 1 - this.fileOpsSelected;
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> chooseFileOp(editor, this.fileOpsSelected);
            default -> {
                return false;
            }
        }
        return true;
    }

    private void chooseFileOp(final TtyEditor editor, final int index) {
        this.fileOpsUp = false;
        if (index == 0) {
            ask(Asking.READ_NAME);
        } else {
            writeFile(editor);
        }
    }

    /* What "settings" offers: whether the five rows of shortcuts show */

    private void settingsLetter(final TtyEditor editor, final char c) {
        final int index = Character.toLowerCase(c) - 'a';
        if (index == 0 || index == 1) {
            chooseSettingsItem(editor, index);
        }
    }

    private boolean settingsKey(final TtyEditor editor, final int key) {
        switch (key) {
            case GLFW.GLFW_KEY_ESCAPE -> this.settingsUp = false;
            case GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_DOWN -> this.settingsSelected = 1 - this.settingsSelected;
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> chooseSettingsItem(editor, this.settingsSelected);
            default -> {
                return false;
            }
        }
        return true;
    }

    private void chooseSettingsItem(final TtyEditor editor, final int index) {
        this.settingsUp = false;
        this.infoWindowOn = index == 0;
    }

    /** Saves the file and says so the way opening one does, which is what "save" and {@code :write} both do. */
    private void writeFile(final TtyEditor editor) {
        editor.save();
        editor.say(EeWords.opened(editor.name(), TtyLineCount.of(editor.document()), editor.text().length()));
    }

    /** Pulls another file in at the caret, once the machine has said what is in it. */
    private static void readFile(final TtyEditor editor, final String typed) {
        final String name = typed.trim();
        if (name.isEmpty()) {
            return;
        }
        editor.read(name, (content, existed) -> {
            if (!existed) {
                editor.say(EeWords.cannotRead(name));
                return;
            }
            editor.document().insertText(content);
            editor.touched();
        });
    }

    /* A prompt at the bottom being answered */

    private boolean answeringKey(final TtyEditor editor, final int key) {
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            this.asking = Asking.NONE;
            editor.say("");
            return true;
        }
        if (key == GLFW.GLFW_KEY_BACKSPACE) {
            if (!this.answer.isEmpty()) {
                this.answer.setLength(this.answer.length() - 1);
            }
            return true;
        }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            answered(editor);
            return true;
        }
        return true;
    }

    private void answered(final TtyEditor editor) {
        final Asking was = this.asking;
        final String typed = this.answer.toString();
        this.asking = Asking.NONE;
        switch (was) {
            case SEARCH -> {
                this.needle = typed.isEmpty() ? this.needle : typed;
                search(editor);
            }
            case ASCII -> insertAscii(editor, typed);
            case COMMAND -> runCommand(editor, typed);
            case READ_NAME -> readFile(editor, typed);
            case NONE -> { }
        }
    }

    private static void insertAscii(final TtyEditor editor, final String typed) {
        try {
            final int code = Integer.parseInt(typed.trim());
            if (code >= 0 && code <= 255) {
                editor.document().insert((char) code);
                editor.touched();
            }
        } catch (final NumberFormatException notANumber) {
            // The real editor leaves the text untouched over a prompt answered with nothing it understood.
        }
    }

    /** Ee's own three command words: {@code write}, {@code quit} and {@code exit}, which is write then quit. */
    private void runCommand(final TtyEditor editor, final String typed) {
        switch (typed.trim()) {
            case "write" -> writeFile(editor);
            case "quit" -> editor.quit();
            case "exit" -> {
                writeFile(editor);
                editor.quit();
            }
            default -> editor.say(EeWords.unknownCommand(typed.trim()));
        }
    }

    /* Keys read straight off the five rows this editor writes on the glass */

    private void ask(final Asking what) {
        this.asking = what;
        this.answer.setLength(0);
    }

    private boolean controlKey(final TtyEditor editor, final int key) {
        final TextDocument doc = editor.document();
        switch (key) {
            case GLFW.GLFW_KEY_Y -> ask(Asking.SEARCH);
            case GLFW.GLFW_KEY_K -> deleteLine(editor);
            case GLFW.GLFW_KEY_P -> doc.up();
            case GLFW.GLFW_KEY_G -> page(editor, -editor.rows());
            case GLFW.GLFW_KEY_O -> ask(Asking.ASCII);
            case GLFW.GLFW_KEY_X -> search(editor);
            case GLFW.GLFW_KEY_L -> undeleteLine(editor);
            case GLFW.GLFW_KEY_N -> doc.down();
            case GLFW.GLFW_KEY_V -> page(editor, editor.rows());
            case GLFW.GLFW_KEY_U -> doc.setCursor(doc.lineCount() - 1, 0);
            case GLFW.GLFW_KEY_A -> doc.setCursor(doc.cursorLine(), 0);
            case GLFW.GLFW_KEY_W -> deleteWord(editor);
            case GLFW.GLFW_KEY_B -> doc.left();
            case GLFW.GLFW_KEY_Z -> wordForward(doc);
            case GLFW.GLFW_KEY_T -> doc.setCursor(0, 0);
            case GLFW.GLFW_KEY_E -> doc.setCursor(doc.cursorLine(), doc.line(doc.cursorLine()).length());
            case GLFW.GLFW_KEY_R -> restoreWord(editor);
            case GLFW.GLFW_KEY_F -> doc.right();
            case GLFW.GLFW_KEY_C -> ask(Asking.COMMAND);
            case GLFW.GLFW_KEY_D -> deleteChar(editor);
            case GLFW.GLFW_KEY_J -> undeleteChar(editor);
            default -> {
                return false;
            }
        }
        return true;
    }

    private static boolean movementKey(final TtyEditor editor, final int key) {
        final TextDocument doc = editor.document();
        switch (key) {
            case GLFW.GLFW_KEY_LEFT -> doc.left();
            case GLFW.GLFW_KEY_RIGHT -> doc.right();
            case GLFW.GLFW_KEY_UP -> doc.up();
            case GLFW.GLFW_KEY_DOWN -> doc.down();
            case GLFW.GLFW_KEY_HOME -> doc.setCursor(doc.cursorLine(), 0);
            case GLFW.GLFW_KEY_END -> doc.setCursor(doc.cursorLine(), doc.line(doc.cursorLine()).length());
            case GLFW.GLFW_KEY_PAGE_UP -> page(editor, -editor.rows());
            case GLFW.GLFW_KEY_PAGE_DOWN -> page(editor, editor.rows());
            case GLFW.GLFW_KEY_BACKSPACE -> {
                doc.backspace();
                editor.touched();
            }
            case GLFW.GLFW_KEY_DELETE -> {
                doc.delete();
                editor.touched();
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                doc.newline();
                editor.touched();
            }
            case GLFW.GLFW_KEY_TAB -> {
                for (int i = 0; i < 4; i++) {
                    doc.insert(' ');
                }
                editor.touched();
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    private static void page(final TtyEditor editor, final int lines) {
        for (int i = 0; i < Math.abs(lines); i++) {
            if (lines > 0) {
                editor.document().down();
            } else {
                editor.document().up();
            }
        }
    }

    private void search(final TtyEditor editor) {
        if (this.needle.isEmpty()) {
            return;
        }
        if (editor.document().find(this.needle)) {
            editor.say("");
        } else {
            editor.say(EeWords.notFound(this.needle));
        }
    }

    private void deleteLine(final TtyEditor editor) {
        final TextDocument doc = editor.document();
        final int line = doc.cursorLine();
        this.lastDeletedLine = doc.line(line);
        if (doc.lineCount() == 1) {
            doc.select(0, 0, 0, doc.line(0).length());
        } else if (line + 1 < doc.lineCount()) {
            doc.select(line, 0, line + 1, 0);
        } else {
            doc.select(line - 1, doc.line(line - 1).length(), line, doc.line(line).length());
        }
        doc.deleteSelection();
        doc.setCursor(Math.min(line, doc.lineCount() - 1), 0);
        editor.touched();
    }

    private void undeleteLine(final TtyEditor editor) {
        if (this.lastDeletedLine.isEmpty()) {
            return;
        }
        final TextDocument doc = editor.document();
        doc.setCursor(doc.cursorLine(), doc.line(doc.cursorLine()).length());
        doc.newline();
        doc.insertText(this.lastDeletedLine);
        editor.touched();
    }

    private void deleteWord(final TtyEditor editor) {
        final TextDocument doc = editor.document();
        final int line = doc.cursorLine();
        final int from = doc.cursorCol();
        wordForward(doc);
        if (doc.cursorLine() != line) {
            doc.setCursor(line, doc.line(line).length());
        }
        doc.select(line, from, line, doc.cursorCol());
        this.lastDeletedWord = doc.selectedText();
        doc.deleteSelection();
        editor.touched();
    }

    private void restoreWord(final TtyEditor editor) {
        if (this.lastDeletedWord.isEmpty()) {
            return;
        }
        editor.document().insertText(this.lastDeletedWord);
        editor.touched();
    }

    private void deleteChar(final TtyEditor editor) {
        final TextDocument doc = editor.document();
        if (doc.cursorCol() < doc.line(doc.cursorLine()).length()) {
            this.lastDeletedChar = doc.charAfter();
            doc.delete();
            editor.touched();
        }
    }

    private void undeleteChar(final TtyEditor editor) {
        if (this.lastDeletedChar == 0) {
            return;
        }
        editor.document().insert(this.lastDeletedChar);
        editor.touched();
    }

    /** The start of the next word, going on to the next line when this one has none left. */
    private static void wordForward(final TextDocument doc) {
        int line = doc.cursorLine();
        int col = doc.cursorCol();
        String text = doc.line(line);
        final boolean onWord = col < text.length() && isWordChar(text.charAt(col));
        while (col < text.length() && isWordChar(text.charAt(col)) == onWord
                && !Character.isWhitespace(text.charAt(col))) {
            col++;
        }
        while (true) {
            while (col < text.length() && Character.isWhitespace(text.charAt(col))) {
                col++;
            }
            if (col < text.length() || line + 1 >= doc.lineCount()) {
                break;
            }
            line++;
            col = 0;
            text = doc.line(line);
        }
        doc.setCursor(line, Math.min(col, text.length()));
    }

    private static boolean isWordChar(final char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    /** The page "help" shows, or none while it is not up. */
    private List<Text> helpPage() {
        return this.helpUp ? EeWords.HELP_PAGE : List.of();
    }

    /** The box drawn over the text: the main menu, the leave question, the file operations or settings choice. */
    private TtyLook.Menu menu() {
        if (this.leaveMenuUp) {
            return new TtyLook.Menu(EeWords.MENU_ITEMS.get(0).text(), textsOf(EeWords.LEAVE_ITEMS),
                    this.leaveSelected);
        }
        if (this.fileOpsUp) {
            return new TtyLook.Menu(EeWords.MENU_ITEMS.get(2).text(), textsOf(EeWords.FILE_OP_ITEMS),
                    this.fileOpsSelected);
        }
        if (this.settingsUp) {
            return new TtyLook.Menu(EeWords.MENU_ITEMS.get(4).text(), textsOf(EeWords.SETTINGS_ITEMS),
                    this.settingsSelected);
        }
        if (this.menuUp) {
            return new TtyLook.Menu(EeWords.MAIN_MENU_TITLE.text(), textsOf(EeWords.MENU_ITEMS), this.menuSelected);
        }
        return TtyLook.Menu.NONE;
    }

    private static List<Text> textsOf(final List<TextKey> keys) {
        final List<Text> out = new ArrayList<>(keys.size());
        for (final TextKey key : keys) {
            out.add(key.text());
        }
        return out;
    }
}
