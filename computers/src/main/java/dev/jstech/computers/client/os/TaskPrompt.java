/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.client.term.TermPalette;
import dev.jstech.computers.gui.term.TermBuffer;
import dev.jstech.computers.gui.term.TermRow;
import dev.jstech.computers.operation.payload.DesktopShellOutputPayload;
import dev.jstech.computers.operation.payload.DesktopShellRunPayload;
import dev.jstech.computers.operation.payload.RequestFileContentPayload;
import dev.jstech.computers.operation.payload.TerminalKeyboard;
import dev.jstech.computers.operation.payload.WireLine;
import dev.jstech.computers.operation.payload.program.DesktopShellPayloads;
import dev.jstech.computers.operation.payload.program.TerminalTools;
import dev.jstech.computers.os.OsMotions;
import dev.jstech.computers.os.Platform;
import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.cli.CliRun;
import dev.jstech.computers.program.cli.CliStyle;
import dev.jstech.core.client.motion.MotionClock;
import dev.jstech.core.gui.TextScreen;
import dev.jstech.core.motion.MotionKinds;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * A prompt a text-mode shell started as one of its tasks: a shell session of its own on the machine, with its own
 * folder, its own scrollback and the line being typed, drawn over the whole glass while it is the task in front.
 *
 * <p>It may have been started to run one program, in which case it says, once the program is done, that a key brings
 * the shell back, the way a task swapper of that age did. A command that hands the terminal to an editor gives the
 * glass to that editor, inside the task, until the editor is done. EXIT at the prompt ends the task.
 */
final class TaskPrompt implements ShellViews.IListener {

    private final BlockPos host;
    private final String name;
    private final Platform platform;
    private final int session = ShellViews.newSession();
    private final TermBuffer scrollback = new TermBuffer(MOST_ROWS, TermBuffer.MONITOR_COLUMNS, GameText.LOADED);
    private final List<String> history = new ArrayList<>();
    /** The words said once the one program it was started for is done, or null for a prompt of its own. */
    @Nullable
    private final Text closing;
    private final StringBuilder typed = new StringBuilder();
    private int caret;
    private int recalled = -1;
    private String prompt;
    private boolean busy;
    private boolean started;
    /** How many lines sent to the machine have not been answered yet. */
    private int pending;
    private boolean toolSpoke;
    private boolean waitingForKey;
    private boolean ended;
    private int columns = TermBuffer.MONITOR_COLUMNS;
    private TerminalKeyboard keyboard = TerminalKeyboard.PROMPT;
    /** The editor a command gave the glass to, inside this task, or null. */
    @Nullable
    private TtyEditor editor;
    /** How the editor being opened reads a keyboard, set just before its file is asked for. */
    @Nullable
    private TtyEditor.IKeys flavour;
    /** Whether the one program it was started for handed the glass to an editor, which ends the task with it. */
    private boolean editorWasTheProgram;
    /** Whoever is waiting for the file an editor was asked to open. */
    private final CodeFileReplies.IReader opening = new CodeFileReplies.IReader() {
        @Override
        public void onContent(final String path, final String content, final boolean exists) {
            if (TaskPrompt.this.flavour != null && !TaskPrompt.this.ended) {
                TaskPrompt.this.editor = new TtyEditor(path, content, TaskPrompt.this.flavour,
                        new TtyEditorWire(() -> TaskPrompt.this.host, TaskPrompt.this::editorDone));
                TaskPrompt.this.editor.opened(exists);
            }
        }
    };

    /** The word that ends a task's prompt, a word of the shell and not of any language. */
    private static final String EXIT = "exit";
    private static final int MOST_ROWS = 256;
    private static final int MOST_HISTORY = 32;

    /**
     * A prompt of its own on the machine at {@code host}, named {@code name} in the shell's list of tasks; when
     * {@code program} is given, the task runs it at once and says {@code closing} when it is done.
     */
    TaskPrompt(final BlockPos host, final String name, final Platform platform, @Nullable final String program,
               @Nullable final Text closing) {
        this.host = host;
        this.name = name;
        this.platform = platform;
        this.closing = program == null ? null : closing;
        this.prompt = platform.unixLike() ? "$" : "C:\\>";
        ShellViews.register(this);
        // The real prompt first, so the task opens in the folder its shell is in, then the program it is for.
        this.pending++;
        PacketDistributor.sendToServer(new DesktopShellRunPayload(host, "", this.session));
        if (program != null && !program.isBlank()) {
            submit(program);
        }
    }

    /** The name the shell's list of tasks gives it. */
    String name() {
        return this.name;
    }

    @Override
    public int session() {
        return this.session;
    }

    /** Whether the task is over: EXIT was typed, or its program is done and a key was pressed. */
    boolean ended() {
        return this.ended;
    }

    /** The editor that has the glass inside this task, or null. */
    @Nullable
    TtyEditor editor() {
        return this.editor;
    }

    /** Everything the task's prompt has printed, as one piece of text, for a test that reads it. */
    String scrollbackText() {
        final StringBuilder text = new StringBuilder();
        for (final CliLine line : this.scrollback.lines()) {
            text.append(line.text(GameText.LOADED)).append('\n');
        }
        return text.toString();
    }

    /** Stops listening to the machine, for a task that is over or a shell that is gone. */
    void release() {
        ShellViews.forget(this);
        CodeFileReplies.forget(this.opening);
        this.ended = true;
    }

    @Override
    public void accept(final DesktopShellOutputPayload payload) {
        if (this.ended) {
            return;
        }
        if (payload.clear()) {
            this.scrollback.clear();
        }
        boolean over = payload.replaceLast();
        for (final WireLine line : payload.lines()) {
            if (over || (line.over() && this.toolSpoke)) {
                this.scrollback.replaceLast(line.toLine());
                over = false;
            } else {
                this.scrollback.push(line.toLine());
            }
            this.toolSpoke = this.toolSpoke || payload.keyboard().busy();
        }
        if (payload.informational()) {
            return;
        }
        if (payload.session() == this.session && this.pending > 0) {
            this.pending--;
        }
        if (!payload.prompt().isEmpty()) {
            this.prompt = payload.prompt();
        }
        this.keyboard = payload.keyboard();
        if (!this.keyboard.busy()) {
            this.toolSpoke = false;
        }
        this.busy = payload.busy() || this.keyboard.busy();
        if (payload.handsOver()) {
            final TtyEditor.IKeys asked = TtyEditors.flavourOf(payload.editor());
            if (asked != null) {
                this.editorWasTheProgram = this.closing != null;
                openEditor(payload.editorPath(), asked);
                return;
            }
        }
        // The one program it was started for is done: a key brings the shell back.
        if (this.started && this.pending == 0 && !this.busy && this.closing != null && this.editor == null
                && !this.waitingForKey) {
            this.waitingForKey = true;
            this.scrollback.push(new CliLine(this.closing, CliStyle.PLAIN));
        }
    }

    /** A key was pressed while this task is in front; true when it meant something. */
    boolean key(final int key, final int modifiers) {
        if (this.editor != null) {
            this.editor.keyPressed(key, modifiers);
            return true;
        }
        if (this.waitingForKey) {
            end();
            return true;
        }
        final boolean control = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        if (control && key == GLFW.GLFW_KEY_C && this.busy) {
            PacketDistributor.sendToServer(new DesktopShellRunPayload(this.host, DesktopShellPayloads.INTERRUPT,
                    this.session));
            return true;
        }
        switch (key) {
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                final String line = this.typed.toString();
                this.typed.setLength(0);
                this.caret = 0;
                submit(line);
            }
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (this.caret > 0) {
                    this.typed.deleteCharAt(--this.caret);
                }
            }
            case GLFW.GLFW_KEY_DELETE -> {
                if (this.caret < this.typed.length()) {
                    this.typed.deleteCharAt(this.caret);
                }
            }
            case GLFW.GLFW_KEY_LEFT -> this.caret = Math.max(0, this.caret - 1);
            case GLFW.GLFW_KEY_RIGHT -> this.caret = Math.min(this.typed.length(), this.caret + 1);
            case GLFW.GLFW_KEY_HOME -> this.caret = 0;
            case GLFW.GLFW_KEY_END -> this.caret = this.typed.length();
            case GLFW.GLFW_KEY_UP -> recall(1);
            case GLFW.GLFW_KEY_DOWN -> recall(-1);
            case GLFW.GLFW_KEY_ESCAPE -> {
                this.typed.setLength(0);
                this.caret = 0;
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    /** A character was typed while this task is in front. */
    boolean typed(final char c) {
        if (this.editor != null) {
            return this.editor.charTyped(c);
        }
        if (this.waitingForKey) {
            end();
            return true;
        }
        if (c >= ' ' && this.typed.length() < DesktopShellRunPayload.MAX_LEN - 1) {
            this.typed.insert(this.caret++, c);
        }
        return true;
    }

    /**
     * Draws the task on {@code screen}, from row {@code top} down: the newest rows of what it printed, then the
     * prompt and the line being typed with the cursor blinking at the system's own beat.
     */
    void drawInto(final TextScreen screen, final int top) {
        final int ground = TextScreen.cga(TextScreen.BLACK);
        screen.fill(0, top, screen.columns(), screen.rows() - top, TermPalette.colorOf(CliStyle.PLAIN), ground);
        if (this.columns != screen.columns()) {
            this.columns = screen.columns();
            this.scrollback.setColumns(this.columns);
        }
        final List<TermRow> rows = this.scrollback.rows();
        final boolean typing = !this.waitingForKey && !(this.busy && !this.keyboard.asking());
        final String standing = this.keyboard.asking() ? this.keyboard.standing().text().stripTrailing()
                : this.prompt;
        final String line = typing ? standing + this.typed : "";
        final int room = screen.rows() - top - (typing ? 1 : 0);
        final int first = Math.max(0, rows.size() - room);
        int row = top;
        for (int i = first; i < rows.size(); i++) {
            int column = 0;
            for (final CliRun run : rows.get(i).runs()) {
                screen.put(column, row, run.text(), TermPalette.colorOf(run.style()), ground);
                column += run.text().length();
            }
            row++;
        }
        if (!typing) {
            return;
        }
        final String shown = this.keyboard.asking() && this.keyboard.unseen() ? standing : line;
        screen.put(0, row, shown, TermPalette.colorOf(CliStyle.PROMPT), ground);
        if (MotionClock.blinkOn(OsMotions.console(this.platform).get().spec(MotionKinds.CARET_BLINK))) {
            final int at = (this.keyboard.asking() && this.keyboard.unseen() ? standing.length()
                    : standing.length() + this.caret);
            screen.put(Math.min(screen.columns() - 1, at), row, "_", TermPalette.colorOf(CliStyle.PLAIN), ground);
        }
    }

    private void submit(final String line) {
        this.recalled = -1;
        if (this.keyboard.busy()) {
            if (this.keyboard.asking()) {
                PacketDistributor.sendToServer(new DesktopShellRunPayload(this.host,
                        line.isEmpty() ? TerminalTools.ENTER : line, this.session, this.columns));
            }
            return;
        }
        if (this.busy) {
            PacketDistributor.sendToServer(new DesktopShellRunPayload(this.host,
                    line.isEmpty() ? TerminalTools.ENTER : line, this.session, this.columns));
            return;
        }
        if (line.trim().toLowerCase(Locale.ROOT).equals(EXIT) && this.closing == null) {
            end();
            return;
        }
        this.scrollback.push(new CliLine(this.prompt + line, CliStyle.PROMPT));
        if (!line.isBlank()) {
            this.history.remove(line);
            this.history.add(0, line);
            if (this.history.size() > MOST_HISTORY) {
                this.history.remove(this.history.size() - 1);
            }
        }
        this.started = true;
        this.busy = true;
        this.pending++;
        PacketDistributor.sendToServer(new DesktopShellRunPayload(this.host, line, this.session, this.columns));
    }

    /* Brings back an earlier line, the way the arrow keys do at a prompt that remembers. */
    private void recall(final int by) {
        if (this.history.isEmpty()) {
            return;
        }
        this.recalled = Math.max(-1, Math.min(this.history.size() - 1, this.recalled + by));
        this.typed.setLength(0);
        if (this.recalled >= 0) {
            this.typed.append(this.history.get(this.recalled));
        }
        this.caret = this.typed.length();
    }

    private void openEditor(final String path, final TtyEditor.IKeys keys) {
        this.flavour = keys;
        CodeFileReplies.expectContent(this.opening, path);
        PacketDistributor.sendToServer(new RequestFileContentPayload(this.host, path));
    }

    private void editorDone() {
        this.editor = null;
        if (this.editorWasTheProgram) {
            end();
            return;
        }
        PacketDistributor.sendToServer(new DesktopShellRunPayload(this.host, "", this.session));
    }

    private void end() {
        release();
    }
}
