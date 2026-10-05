/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.core.gui.TextScreen;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * A dialog of the MC-DOS Shell: a box in the middle of the screen with its title in the top rule, lines of words,
 * fields to type in, boxes to tick, a choice of one among several, and the buttons along the bottom, with the shadow a
 * DOS dialog threw. Tab moves between what it holds, Enter presses the first button and Escape the last.
 */
final class DosShellDialog {

    private final Text title;
    private final List<Text> lines;
    private final List<Control> controls;
    private final List<TextKey> buttons;
    /** What pressing each button does, told the dialog: given the index of the button. */
    private final Consumer<Integer> pressed;
    /** What has the keyboard: a control by its index, then the buttons after the controls. */
    private int focus;

    private static final int FIELD_WIDTH = 30;
    private static final int MOST_LETTERS = 120;

    private DosShellDialog(final Text title, final List<Text> lines, final List<Control> controls,
                           final List<TextKey> buttons, final Consumer<Integer> pressed) {
        this.title = title;
        this.lines = List.copyOf(lines);
        this.controls = List.copyOf(controls);
        this.buttons = List.copyOf(buttons);
        this.pressed = pressed;
    }

    /** A message with an OK button. */
    static DosShellDialog message(final Text title, final List<Text> lines, final Runnable ok) {
        return new DosShellDialog(title, lines, List.of(), List.of(DosShellTexts.OK), button -> ok.run());
    }

    /** A question with Yes and No; {@code yes} runs on Yes. */
    static DosShellDialog ask(final Text title, final List<Text> lines, final Runnable yes, final Runnable no) {
        return new DosShellDialog(title, lines, List.of(), List.of(DosShellTexts.YES, DosShellTexts.NO),
                button -> {
                    if (button == 0) {
                        yes.run();
                    } else {
                        no.run();
                    }
                });
    }

    /**
     * Lines and controls with OK and Cancel: {@code ok} is handed the dialog to read what was typed and chosen,
     * {@code cancel} runs on Cancel.
     */
    static DosShellDialog form(final Text title, final List<Text> lines, final List<Control> controls,
                               final Consumer<DosShellDialog> ok, final Runnable cancel) {
        final DosShellDialog[] made = new DosShellDialog[1];
        made[0] = new DosShellDialog(title, lines, controls, List.of(DosShellTexts.OK, DosShellTexts.CANCEL),
                button -> {
                    if (button == 0) {
                        ok.accept(made[0]);
                    } else {
                        cancel.run();
                    }
                });
        return made[0];
    }

    /** A field to type in, with its label and what it starts with. */
    static Control field(final TextKey label, final String value) {
        return new Control(Kind.FIELD, label, new StringBuilder(value), false, List.of(), 0);
    }

    /** A box to tick. */
    static Control check(final TextKey label, final boolean ticked) {
        return new Control(Kind.CHECK, label, new StringBuilder(), ticked, List.of(), 0);
    }

    /** One choice among several. */
    static Control choice(final TextKey label, final List<TextKey> options, final int chosen) {
        return new Control(Kind.CHOICE, label, new StringBuilder(), false, options, chosen);
    }

    /** What the control at {@code index} holds as typed. */
    String typed(final int index) {
        return controls.get(index).text.toString();
    }

    /** Whether the box at {@code index} is ticked. */
    boolean ticked(final int index) {
        return controls.get(index).ticked;
    }

    /** Which option the choice at {@code index} has. */
    int chosen(final int index) {
        return controls.get(index).chosen;
    }

    /** The dialog's title, for a test that reads which dialog is up. */
    Text title() {
        return title;
    }

    /** Types into the field at {@code index} as though the player had, for a test. */
    void type(final int index, final String text) {
        final Control control = controls.get(index);
        control.text.setLength(0);
        control.text.append(text);
    }

    /** A key; true when it meant something. */
    boolean key(final int key, final int modifiers) {
        final int count = controls.size() + buttons.size();
        final boolean shift = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
        @Nullable final Control at = focus < controls.size() ? controls.get(focus) : null;
        switch (key) {
            case GLFW.GLFW_KEY_TAB -> focus = Math.floorMod(focus + (shift ? -1 : 1), count);
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> press(focus < controls.size() ? 0
                    : focus - controls.size());
            case GLFW.GLFW_KEY_ESCAPE -> press(buttons.size() - 1);
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (at != null && at.kind == Kind.FIELD && !at.text.isEmpty()) {
                    at.text.setLength(at.text.length() - 1);
                }
            }
            case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_UP -> {
                if (at != null && at.kind == Kind.CHOICE) {
                    at.chosen = Math.floorMod(at.chosen - 1, at.options.size());
                } else if (at == null) {
                    focus = controls.size() + Math.floorMod(focus - controls.size() - 1, buttons.size());
                }
            }
            case GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_DOWN -> {
                if (at != null && at.kind == Kind.CHOICE) {
                    at.chosen = Math.floorMod(at.chosen + 1, at.options.size());
                } else if (at == null) {
                    focus = controls.size() + Math.floorMod(focus - controls.size() + 1, buttons.size());
                }
            }
            case GLFW.GLFW_KEY_SPACE -> {
                if (at != null && at.kind == Kind.CHECK) {
                    at.ticked = !at.ticked;
                } else if (at != null && at.kind == Kind.FIELD) {
                    return typed(' ');
                }
            }
            default -> {
                return true;
            }
        }
        return true;
    }

    /** A character typed; true when it went into a field. */
    boolean typed(final char c) {
        if (focus < controls.size() && controls.get(focus).kind == Kind.FIELD && c >= ' '
                && controls.get(focus).text.length() < MOST_LETTERS) {
            controls.get(focus).text.append(c);
            return true;
        }
        return false;
    }

    /** A click on the cell ({@code column}, {@code row}) of the screen the dialog was last drawn on. */
    boolean clicked(final TextScreen screen, final int column, final int row) {
        final Box box = box(screen);
        for (int i = 0; i < buttons.size(); i++) {
            final int[] at = buttonAt(box, i);
            if (row == box.buttonRow && column >= at[0] && column < at[0] + at[1]) {
                press(i);
                return true;
            }
        }
        for (int i = 0; i < controls.size(); i++) {
            if (row == box.controlRow + i) {
                focus = i;
                if (controls.get(i).kind == Kind.CHECK) {
                    controls.get(i).ticked = !controls.get(i).ticked;
                }
                return true;
            }
        }
        return true;
    }

    /** Draws the dialog over whatever {@code screen} holds, in the middle of it, with its shadow. */
    void draw(final TextScreen screen) {
        final int ink = TextScreen.cga(TextScreen.BLACK);
        final int ground = TextScreen.cga(TextScreen.GREY);
        final Box box = box(screen);
        screen.box(box.left, box.top, box.width, box.height, ink, ground, false);
        final String heading = " " + GameText.resolve(title) + " ";
        screen.put(box.left + (box.width - heading.length()) / 2, box.top, heading, TextScreen.cga(TextScreen.WHITE),
                TextScreen.cga(TextScreen.BLUE));
        int row = box.top + 2;
        for (final Text line : lines) {
            for (final String part : GameText.resolve(line).split("\n")) {
                screen.put(box.left + 2, row++, part, ink, ground);
            }
        }
        for (int i = 0; i < controls.size(); i++) {
            drawControl(screen, controls.get(i), box.left + 2, box.controlRow + i, focus == i);
        }
        for (int i = 0; i < buttons.size(); i++) {
            final int[] at = buttonAt(box, i);
            final boolean focused = focus == controls.size() + i;
            screen.put(at[0], box.buttonRow, buttonLabel(i), focused ? TextScreen.cga(TextScreen.WHITE) : ink,
                    focused ? TextScreen.cga(TextScreen.BLACK) : ground);
        }
        screen.shadow(box.left, box.top, box.width, box.height, TextScreen.cga(TextScreen.DARK_GREY),
                TextScreen.cga(TextScreen.BLACK));
    }

    private void drawControl(final TextScreen screen, final Control control, final int left, final int row,
                             final boolean focused) {
        final int ink = TextScreen.cga(TextScreen.BLACK);
        final int ground = TextScreen.cga(TextScreen.GREY);
        final String label = GameText.resolve(control.label);
        screen.put(left, row, label, ink, ground);
        final int at = left + label.length() + 1;
        switch (control.kind) {
            case FIELD -> {
                final String text = control.text.toString();
                final String shown = text.length() > FIELD_WIDTH - 1 ? text.substring(text.length() - FIELD_WIDTH + 1)
                        : text;
                screen.put(at, row, " ".repeat(FIELD_WIDTH), ink, TextScreen.cga(TextScreen.CYAN));
                screen.put(at, row, shown + (focused ? "_" : ""), ink, TextScreen.cga(TextScreen.CYAN));
            }
            case CHECK -> screen.put(at, row, control.ticked ? "[X]" : "[ ]",
                    focused ? TextScreen.cga(TextScreen.WHITE) : ink, ground);
            case CHOICE -> {
                int x = at;
                for (int i = 0; i < control.options.size(); i++) {
                    final String option = (i == control.chosen ? "(•) " : "( ) ")
                            + GameText.resolve(control.options.get(i));
                    screen.put(x, row, option, focused && i == control.chosen ? TextScreen.cga(TextScreen.WHITE) : ink,
                            ground);
                    x += option.length() + 2;
                }
            }
        }
    }

    private void press(final int button) {
        pressed.accept(Math.max(0, Math.min(buttons.size() - 1, button)));
    }

    private String buttonLabel(final int index) {
        return "< " + GameText.resolve(buttons.get(index)) + " >";
    }

    /* Where button {@code index} starts along the button row, and how wide it is. */
    private int[] buttonAt(final Box box, final int index) {
        int total = 0;
        for (int i = 0; i < buttons.size(); i++) {
            total += buttonLabel(i).length() + (i > 0 ? 2 : 0);
        }
        int x = box.left + (box.width - total) / 2;
        for (int i = 0; i < index; i++) {
            x += buttonLabel(i).length() + 2;
        }
        return new int[] {x, buttonLabel(index).length()};
    }

    /* The box the dialog takes on {@code screen}: wide enough for its widest line, in the middle. */
    private Box box(final TextScreen screen) {
        int widest = GameText.resolve(title).length() + 4;
        int textRows = 0;
        for (final Text line : lines) {
            for (final String part : GameText.resolve(line).split("\n")) {
                widest = Math.max(widest, part.length());
                textRows++;
            }
        }
        for (final Control control : controls) {
            widest = Math.max(widest, GameText.resolve(control.label).length() + 1 + controlWidth(control));
        }
        int buttonsWide = 0;
        for (int i = 0; i < buttons.size(); i++) {
            buttonsWide += buttonLabel(i).length() + 2;
        }
        widest = Math.max(widest, buttonsWide);
        final int width = Math.min(screen.columns() - 4, widest + 4);
        final int height = Math.min(screen.rows() - 2, textRows + controls.size() + (textRows > 0 ? 1 : 0)
                + (controls.isEmpty() ? 0 : 1) + 4);
        final int left = Math.max(0, (screen.columns() - width) / 2);
        final int top = Math.max(1, (screen.rows() - height) / 2);
        final int controlRow = top + 2 + textRows + (textRows > 0 && !controls.isEmpty() ? 1 : 0);
        return new Box(left, top, width, height, controlRow, top + height - 2);
    }

    private static int controlWidth(final Control control) {
        return switch (control.kind) {
            case FIELD -> FIELD_WIDTH;
            case CHECK -> 3;
            case CHOICE -> {
                int wide = 0;
                for (final TextKey option : control.options) {
                    wide += GameText.resolve(option).length() + 6;
                }
                yield wide;
            }
        };
    }

    /** What sort of control a line of the dialog is. */
    enum Kind {
        FIELD, CHECK, CHOICE
    }

    /** One control of a dialog, and what it holds. */
    static final class Control {

        private final Kind kind;
        private final TextKey label;
        private final StringBuilder text;
        private final List<TextKey> options;
        private boolean ticked;
        private int chosen;

        private Control(final Kind kind, final TextKey label, final StringBuilder text, final boolean ticked,
                        final List<TextKey> options, final int chosen) {
            this.kind = kind;
            this.label = label;
            this.text = text;
            this.ticked = ticked;
            this.options = new ArrayList<>(options);
            this.chosen = chosen;
        }
    }

    /* Where the dialog stands: its box, the row of its first control and the row of its buttons. */
    private record Box(int left, int top, int width, int height, int controlRow, int buttonRow) {
    }
}
