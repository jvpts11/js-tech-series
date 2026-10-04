/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.component;

import dev.jstech.core.client.font.GridPainter;
import dev.jstech.core.client.gui.logic.TextEditState;
import dev.jstech.core.font.CellFont;
import dev.jstech.core.font.GridSpan;
import dev.jstech.core.gui.LineHistory;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * One line of a terminal: what is being typed after a prompt, submitted with Enter and recalled with the
 * arrow keys through the lines typed before. The caret moves within the line the way it does at any prompt:
 * Left and Right by a character, Home and End to the ends, Ctrl with Left or Right by a word. While it has
 * the keyboard and nothing is typed yet, it can show the last line the command produced instead of an empty
 * prompt. It looks like a terminal on every desktop: light text on a dark strip, not the skin's field.
 */
public final class CommandLine extends UiComponent {

    /** How many lines of a paste a terminal will run: enough for a handful of commands, and no more. */
    private static final int MOST_PASTED_LINES = 16;

    private final int maxLength;
    private final Consumer<String> onSubmit;
    private final TextEditState input;
    private final LineHistory history = new LineHistory();
    private Supplier<String> prompt = () -> ">";
    private Supplier<String> idleText = () -> "";
    private IntSupplier idleColor = () -> ComponentPalette.get().consolePrompt();
    private int background = ComponentPalette.get().consoleGround();
    private int textColor = ComponentPalette.get().consolePrompt();
    private BooleanSupplier unseen = () -> false;
    private BooleanSupplier takesNothing = () -> false;
    private @Nullable GridPainter<Integer> grid;

    public CommandLine(final int maxLength, final Consumer<String> onSubmit) {
        this.maxLength = Math.max(1, maxLength);
        this.onSubmit = onSubmit;
        this.input = new TextEditState(this.maxLength);
    }

    /**
     * Whether what is typed is kept off the line, the way a password is: taken, and never drawn, not even as
     * dots, and never kept for the arrow keys to bring back.
     */
    public CommandLine setUnseen(final BooleanSupplier value) {
        unseen = value;
        return this;
    }

    /**
     * Whether Enter on an empty line is an answer in its own right.
     *
     * <p>At a shell it is not, and nothing is sent. At a question it usually is: it takes the default, which
     * is how most of a partition editor's questions are meant to be answered.
     */
    public CommandLine setTakesNothing(final BooleanSupplier value) {
        takesNothing = value;
        return this;
    }

    /** The line shown instead of an empty prompt, with its colour; empty text shows the prompt. */
    public CommandLine setIdle(final Supplier<String> text, final IntSupplier color) {
        idleText = text;
        idleColor = color;
        return this;
    }

    /** The prompt in front of what is typed: a shell's current directory, a bare {@code >} by default. */
    public CommandLine setPrompt(final Supplier<String> value) {
        prompt = value;
        return this;
    }

    /** The strip's colours, for a terminal that tints its console to the desktop it runs on. */
    public CommandLine setStyle(final int backgroundArgb, final int textArgb) {
        background = backgroundArgb;
        textColor = textArgb;
        return this;
    }

    /**
     * Writes the line in a font of cells, one character to each, as the terminal above it is written, rather than
     * in the game's font.
     */
    public CommandLine setCellFont(final CellFont font) {
        grid = new GridPainter<>(font);
        return this;
    }

    /** What is typed so far. */
    public String input() {
        return input.edit();
    }

    /** Where the caret is in what is typed, counted in characters from the start. */
    public int caret() {
        return input.caret();
    }

    @Override
    public boolean focusable() {
        return true;
    }

    @Override
    public void render(final GuiGraphics g, final UiContext ctx) {
        Grounds.fill(g, x(), y(), right(), bottom(), background);
        final Font font = ctx.font();
        final String idle = idleText.get();
        if (isFocused() && input.edit().isEmpty() && !idle.isEmpty()) {
            write(g, font, trim(font, idle, width() - 6), x() + 3, idleColor.getAsInt());
            return;
        }
        /*
         * The caret takes the room of one character so the line can be scrolled to keep it in view even
         * when it sits at the very end, which is where it is most of the time.
         */
        final boolean hidden = unseen.getAsBoolean();
        final String full = prompt.get() + " " + (hidden ? "" : input.edit()) + " ";
        final int caretAt = prompt.get().length() + 1 + (hidden ? 0 : input.caret());
        final String shown = tail(font, full, width() - 6);
        final int dropped = full.length() - shown.length();
        if (isFocused() && !hidden && input.hasSelection()) {
            // Under the letters, on a wash of the ink, so what Shift and the arrows picked out reads as picked out.
            final int from = Math.max(0, prompt.get().length() + 1 + input.selectionStart() - dropped);
            final int to = Math.max(from, Math.min(shown.length(),
                    prompt.get().length() + 1 + input.selectionEnd() - dropped));
            final int left = x() + 3 + width(font, shown.substring(0, Math.min(from, shown.length())));
            g.fill(left, y() + 1, left + width(font, shown.substring(Math.min(from, shown.length()), to)),
                    y() + 11, ComponentPalette.get().consolePicked());
        }
        write(g, font, shown, x() + 3, textColor);
        if (isFocused()) {
            final int visibleCaret = Math.max(0, Math.min(shown.length(), caretAt - dropped));
            write(g, font, "_", x() + 3 + width(font, shown.substring(0, visibleCaret)), textColor);
        }
    }

    @Override
    public boolean mouseClicked(final double mx, final double my, final int button) {
        return true;
    }

    @Override
    public boolean charTyped(final char c) {
        if (!isFocused() || c < 32 || c == 127) {
            return false;
        }
        input.type(c);
        return true;
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        if (!isFocused()) {
            return false;
        }
        final boolean control = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        final boolean shift = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
        if (control && key == GLFW.GLFW_KEY_C) {
            return copy();
        }
        if (control && key == GLFW.GLFW_KEY_V) {
            paste();
            return true;
        }
        switch (key) {
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> submit();
            case GLFW.GLFW_KEY_BACKSPACE -> input.backspace();
            case GLFW.GLFW_KEY_DELETE -> input.delete();
            case GLFW.GLFW_KEY_LEFT -> {
                if (control) {
                    input.wordLeft();
                } else {
                    input.left(shift);
                }
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                if (control) {
                    input.wordRight();
                } else {
                    input.right(shift);
                }
            }
            case GLFW.GLFW_KEY_HOME -> input.home(shift);
            case GLFW.GLFW_KEY_END -> input.end(shift);
            case GLFW.GLFW_KEY_A -> {
                if (!control) {
                    return false;
                }
                input.selectAll();
            }
            case GLFW.GLFW_KEY_UP -> recall(-1);
            case GLFW.GLFW_KEY_DOWN -> recall(1);
            default -> {
                return false;
            }
        }
        return true;
    }

    /** Puts what is picked out of the line on the clipboard; with nothing picked out this key is not ours. */
    private boolean copy() {
        final String picked = input.selectedText();
        if (picked.isEmpty()) {
            return false;
        }
        Minecraft.getInstance().keyboardHandler.setClipboard(picked);
        return true;
    }

    /**
     * Types what is on the clipboard.
     *
     * <p>Several lines run one after another, which is what a terminal does with a paste; the last stays on
     * the line unrun, since a paste that did not end in a newline is a line somebody is still writing. A paste
     * longer than a terminal has any business running is cut short.
     */
    private void paste() {
        final String text = Minecraft.getInstance().keyboardHandler.getClipboard();
        if (text == null || text.isEmpty()) {
            return;
        }
        final String[] lines = text.split("\r?\n", -1);
        for (int i = 0; i < lines.length && i < MOST_PASTED_LINES; i++) {
            for (final char ch : lines[i].toCharArray()) {
                if (ch >= 32 && ch != 127) {
                    input.type(ch);
                }
            }
            if (i < lines.length - 1 && i < MOST_PASTED_LINES - 1) {
                submit();
            }
        }
    }

    private void submit() {
        final String line = input.edit().trim();
        input.sync("");
        history.rest();
        if (line.isEmpty() && !takesNothing.getAsBoolean()) {
            return;
        }
        // Neither an empty answer nor one that was not for showing is something to bring back later.
        if (!unseen.getAsBoolean()) {
            history.add(line);
        }
        onSubmit.accept(line);
    }

    private void recall(final int direction) {
        history.recall(direction).ifPresent(input::sync);
    }

    /** Writes text from {@code x} along the strip, in the line's font. */
    private void write(final GuiGraphics g, final Font font, final String text, final int x, final int colour) {
        if (grid == null) {
            Draw.text(g, font, text, x, y() + 2, colour, background);
        } else {
            grid.drawOnce(g, font, List.of(new GridSpan<>(text, colour)), x, y() + (height() - grid.cellHeight()) / 2,
                    grid.cellHeight(), Integer::intValue, background);
        }
    }

    /** How wide text comes out in the line's font: in whole cells when it has one. */
    private int width(final Font font, final String text) {
        return grid == null ? font.width(text) : text.length() * grid.cellWidth();
    }

    /** The end of a text that fits that room, which is the part a line scrolled to its caret shows. */
    private String tail(final Font font, final String text, final int room) {
        if (grid == null) {
            return Texts.tail(font, text, room);
        }
        final int fits = Math.max(1, room / grid.cellWidth());
        return text.length() <= fits ? text : text.substring(text.length() - fits);
    }

    /** The start of a text that fits that room. */
    private String trim(final Font font, final String text, final int room) {
        if (grid == null) {
            return Texts.trim(font, text, room);
        }
        final int fits = Math.max(1, room / grid.cellWidth());
        return text.length() <= fits ? text : text.substring(0, fits);
    }
}
