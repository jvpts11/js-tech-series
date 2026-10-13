/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.os.edit.TtyLook;
import dev.jstech.computers.program.cli.interac.InteracScreen;
import dev.jstech.computers.program.cli.interac.InteracState;
import dev.jstech.computers.program.cli.interac.InteracView;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import java.util.List;
import org.lwjgl.glfw.GLFW;

/**
 * Working the network from a terminal that the view has taken whole: the keyboard half of it.
 *
 * <p>Nothing of the network is held here. A key changes where the view stands, the machine is asked for that
 * view, and what comes back is what is shown: the same screen whether it is being read at a monitor, in a
 * window on a desktop, or over a session opened on a machine on the other side of the world.
 *
 * <p>The keys are the ones the file managers of that age had: arrows move, Tab goes to the next heading,
 * letters look for something, and the numbered keys along the foot do the ten things there are to do.
 */
public final class InteracTuiKeys implements TtyEditor.IKeys {

    /** What is being typed into the question at the foot, while one is being answered. */
    private final StringBuilder typing = new StringBuilder();

    /** Where the view stands, which is the whole of what this side of it remembers. */
    private InteracState state = InteracState.OPENING;

    /** What is being asked for a number, or empty when nothing is. */
    private String asked = "";

    /** Whether the help page is up, which stands in place of the view until a key puts it away. */
    private boolean helping;

    /** How many rows the list had when it was last drawn, read off the screen itself. */
    private int rows;

    /** A plain glass with one line that talks, which is the whole of what this view draws round itself. */
    private static final TtyLook LOOK =
            new TtyLook("", Text.EMPTY, Text.EMPTY, TtyLook.Status.LINE, List.of(), List.of(), false);

    /** The same with the line that talks drawn as a question, for while one is being answered. */
    private static final TtyLook ASKING =
            new TtyLook("", Text.EMPTY, Text.EMPTY, TtyLook.Status.BAR, List.of(), List.of(), false);

    /* How wide the help page's columns start, before a longer word in the player's language widens one. */
    private static final int KEY_COLUMN = 12;
    private static final int DOES_COLUMN = 19;

    /** What the numbered keys along the foot do, in their order. */
    private static final String[] VERBS = {"help", "get", "put", "craft", "lock", "unlock", "fav", "stop",
            "find", "quit"};

    /** The ones of those that ask for a number before anything happens. */
    private static final List<String> COUNTED = List.of("get", "put", "craft", "lock");

    @Override
    public TtyLook look(final TtyEditor editor) {
        if (this.helping) {
            // The help page, which stands in place of the view rather than beside it.
            return new TtyLook("", Text.EMPTY, Text.EMPTY, TtyLook.Status.LINE, List.of(), help(), false);
        }
        return this.asked.isEmpty() ? LOOK : ASKING;
    }

    @Override
    public String status(final TtyEditor editor) {
        if (this.helping) {
            return GameText.resolve(InteracTuiTexts.PUTS_THIS_AWAY);
        }
        if (!this.asked.isEmpty()) {
            return GameText.resolve(InteracTuiTexts.ANSWER_A_NUMBER);
        }
        return GameText.resolve(InteracTuiTexts.STATUS.with("interac", this.state.tabName(), "Tab", "F10"));
    }

    /** The view is up; nothing is said about it, because the screen itself says everything. */
    @Override
    public void opened(final TtyEditor editor, final boolean existed) {
        if (!existed) {
            editor.say(InteracTuiTexts.NO_NETWORK.with(Text.literal("interac")));
            return;
        }
        caretOnThePickedRow(editor);
    }

    /**
     * The window was made another size, so the screen is asked for again at the width it now has.
     *
     * <p>The machine draws the screen, so a glass of another width is another screen and not the same one
     * scrolled sideways: the columns have to be laid out again, and only the machine can do that.
     */
    @Override
    public void resized(final TtyEditor editor, final int columns, final int rows) {
        show(editor, this.state.on(columns, rows));
    }

    @Override
    public boolean typed(final TtyEditor editor, final char c) {
        if (this.helping) {
            this.helping = false;
            return true;
        }
        if (!this.asked.isEmpty()) {
            if (Character.isDigit(c)) {
                this.typing.append(c);
            }
            return true;
        }
        /*
         * A slash would be read as a folder by whatever works out which file is being asked for, and no item
         * is called anything with one in it, so it is not a letter of a search.
         */
        if (c >= ' ' && c != '/') {
            show(editor, this.state.searchingFor(this.state.search() + c));
        }
        return true;
    }

    @Override
    public boolean key(final TtyEditor editor, final int key, final int modifiers) {
        if (this.helping) {
            this.helping = false;
            return true;
        }
        if (!this.asked.isEmpty()) {
            return answering(editor, key);
        }
        final int numbered = key - GLFW.GLFW_KEY_F1;
        if (numbered >= 0 && numbered < VERBS.length) {
            pressed(editor, numbered);
            return true;
        }
        switch (key) {
            case GLFW.GLFW_KEY_TAB -> show(editor, this.state.onTab(this.state.tab()
                    + ((modifiers & GLFW.GLFW_MOD_SHIFT) != 0 ? -1 : 1)));
            case GLFW.GLFW_KEY_RIGHT -> show(editor, this.state.onTab(this.state.tab() + 1));
            case GLFW.GLFW_KEY_LEFT -> show(editor, this.state.onTab(this.state.tab() - 1));
            case GLFW.GLFW_KEY_DOWN -> show(editor, this.state.picking(this.state.selected() + 1));
            case GLFW.GLFW_KEY_UP -> show(editor, this.state.picking(this.state.selected() - 1));
            case GLFW.GLFW_KEY_PAGE_DOWN -> show(editor,
                    this.state.picking(this.state.selected() + InteracScreen.bodyRows(this.state)));
            case GLFW.GLFW_KEY_PAGE_UP -> show(editor,
                    this.state.picking(this.state.selected() - InteracScreen.bodyRows(this.state)));
            case GLFW.GLFW_KEY_HOME -> show(editor, this.state.picking(0));
            case GLFW.GLFW_KEY_END -> show(editor, this.state.picking(this.rows - 1));
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> ask(editor, "get");
            case GLFW.GLFW_KEY_BACKSPACE -> backspace(editor);
            case GLFW.GLFW_KEY_ESCAPE -> editor.quit();
            default -> {
                return true;
            }
        }
        return true;
    }

    /**
     * A click on the glass: a heading is gone to, a row is picked, and a key along the foot is pressed.
     *
     * <p>The row is counted from the top of what is being shown, and the view is always shown from its first
     * line, so the two are the same count.
     */
    @Override
    public boolean clicked(final TtyEditor editor, final int row, final int column) {
        if (this.helping) {
            this.helping = false;
            return true;
        }
        if (row == InteracScreen.TAB_ROW) {
            final int tab = InteracScreen.tabAt(column);
            if (tab >= 0) {
                show(editor, this.state.onTab(tab));
            }
            return true;
        }
        if (row == InteracScreen.screenRows(this.state) - 1) {
            final int key = InteracScreen.keyAt(column);
            if (key >= 0) {
                pressed(editor, key);
            }
            return true;
        }
        if (row >= InteracScreen.HEAD_ROWS
                && row < InteracScreen.HEAD_ROWS + InteracScreen.bodyRows(this.state)) {
            show(editor, this.state.picking(InteracScreen.firstShown(this.state)
                    + row - InteracScreen.HEAD_ROWS));
        }
        return true;
    }

    /** One of the ten keys along the foot, whether it was pressed or clicked. */
    private void pressed(final TtyEditor editor, final int which) {
        final String verb = VERBS[which];
        switch (verb) {
            case "help" -> this.helping = true;
            case "find" -> show(editor, this.state.searchingFor(""));
            case "quit" -> editor.quit();
            default -> {
                if (COUNTED.contains(verb)) {
                    ask(editor, verb);
                } else {
                    act(editor, verb, 0L);
                }
            }
        }
    }

    /** Puts a question at the foot and waits for a number. */
    private void ask(final TtyEditor editor, final String verb) {
        this.asked = verb;
        this.typing.setLength(0);
        show(editor, this.state.asking(verb, 0L));
    }

    /** The keys of a question being answered: it is carried out on Enter and dropped on Escape. */
    private boolean answering(final TtyEditor editor, final int key) {
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            final String verb = this.asked;
            final long many = number();
            this.asked = "";
            act(editor, verb, many);
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            this.asked = "";
            show(editor, this.state.done());
            return true;
        }
        if (key == GLFW.GLFW_KEY_BACKSPACE && !this.typing.isEmpty()) {
            this.typing.setLength(this.typing.length() - 1);
        }
        show(editor, this.state.asking(this.asked, number()));
        return true;
    }

    /**
     * Asks the machine to carry the thing out, and holds the view without it.
     *
     * <p>What is asked for is marked as a thing to do only in the one view asked for here: the state kept on
     * this side keeps the row and the search and nothing else, so nothing is done twice when the next key
     * asks for the view again.
     */
    private void act(final TtyEditor editor, final String verb, final long many) {
        this.typing.setLength(0);
        fetch(editor, this.state.asking(verb + InteracView.NOW, many));
        this.state = this.state.done();
    }

    /** Takes a letter off the search, and leaves a search that is already empty alone. */
    private void backspace(final TtyEditor editor) {
        final String search = this.state.search();
        if (!search.isEmpty()) {
            show(editor, this.state.searchingFor(search.substring(0, search.length() - 1)));
        }
    }

    /**
     * Moves the view to that state and asks the machine to draw it.
     *
     * <p>A row past the end of the list is brought back to the last one there is, so holding an arrow down at
     * the foot of a list leaves the view where it can be read rather than somewhere below it.
     */
    private void show(final TtyEditor editor, final InteracState wanted) {
        this.state = this.rows > 0 && wanted.action().isEmpty() && wanted.selected() >= this.rows
                ? wanted.picking(this.rows - 1) : wanted;
        fetch(editor, this.state);
    }

    /**
     * Asks the machine for that view.
     *
     * <p>It is asked for the way a file is, by name, because to this terminal it is one: the state names the
     * screen wanted and what comes back is the screen. The name is given without what says which kind of
     * thing it is, since the view that is open already says that.
     */
    private void fetch(final TtyEditor editor, final InteracState wanted) {
        editor.read(wanted.path().substring(InteracState.SCHEME.length()), (content, existed) -> {
            editor.document().setText(content);
            editor.toTheTop();
            this.rows = InteracScreen.rowsSaid(List.of(content.split("\n", -1)));
            caretOnThePickedRow(editor);
        });
    }

    /** Puts the caret on the row that is picked, so a terminal that draws one draws it in the right place. */
    private void caretOnThePickedRow(final TtyEditor editor) {
        final int within = this.state.selected() - InteracScreen.firstShown(this.state);
        editor.document().setCursor(InteracScreen.HEAD_ROWS + Math.max(0, within), 0);
    }

    /** What is being typed into a question, as a number. */
    private long number() {
        try {
            return this.typing.isEmpty() ? 0L : Long.parseLong(this.typing.toString());
        } catch (final NumberFormatException tooBig) {
            return Long.MAX_VALUE;
        }
    }

    /**
     * The help page, which says what the keys do and nothing a player could read off the screen itself. It is
     * laid out in the player's language, each column as wide as its longest word needs.
     */
    private static List<Text> help() {
        return List.of(
                row(InteracTuiTexts.ARROWS, InteracTuiTexts.ARROWS_DO),
                row(InteracTuiTexts.TAB, InteracTuiTexts.TAB_DOES),
                row(InteracTuiTexts.LETTERS, InteracTuiTexts.LETTERS_DO),
                row(InteracTuiTexts.ENTER, InteracTuiTexts.ENTER_DOES),
                Text.EMPTY,
                twoKeys("F1", InteracTuiTexts.HELP, InteracTuiTexts.HELP_DOES,
                        "F6", InteracTuiTexts.FREE, InteracTuiTexts.FREE_DOES),
                twoKeys("F2", InteracTuiTexts.GET, InteracTuiTexts.GET_DOES,
                        "F7", InteracTuiTexts.FAV, InteracTuiTexts.FAV_DOES),
                twoKeys("F3", InteracTuiTexts.PUT, InteracTuiTexts.PUT_DOES,
                        "F8", InteracTuiTexts.STOP, InteracTuiTexts.STOP_DOES),
                twoKeys("F4", InteracTuiTexts.CRAFT, InteracTuiTexts.CRAFT_DOES,
                        "F9", InteracTuiTexts.FIND, InteracTuiTexts.FIND_DOES),
                twoKeys("F5", InteracTuiTexts.LOCK, InteracTuiTexts.LOCK_DOES,
                        "F10", InteracTuiTexts.QUIT, InteracTuiTexts.QUIT_DOES),
                Text.EMPTY,
                InteracTuiTexts.PUTS_THE_PAGE_AWAY.text());
    }

    /* One key and what it does, the words starting where the key column ends. */
    private static Text row(final TextKey key, final TextKey does) {
        return Text.literal(column(GameText.resolve(key), KEY_COLUMN) + GameText.resolve(does));
    }

    /* Two numbered keys side by side, each followed by what it does. */
    private static Text twoKeys(final String first, final TextKey firstName, final TextKey firstDoes,
                                final String second, final TextKey secondName, final TextKey secondDoes) {
        return Text.literal(column(first + " " + GameText.resolve(firstName), KEY_COLUMN)
                + column(GameText.resolve(firstDoes), DOES_COLUMN)
                + column(second + " " + GameText.resolve(secondName), KEY_COLUMN)
                + GameText.resolve(secondDoes));
    }

    /* Words padded out to a column, and never run into the next one: a longer word keeps a space after it. */
    private static String column(final String words, final int width) {
        return words + " ".repeat(Math.max(1, width - words.length()));
    }
}
