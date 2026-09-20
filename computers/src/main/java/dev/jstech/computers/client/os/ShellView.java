/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.client.term.TermPainter;
import dev.jstech.computers.client.term.TermPalette;
import dev.jstech.computers.client.term.TermSelector;
import dev.jstech.computers.gui.term.TermBuffer;
import dev.jstech.computers.gui.term.TermRow;
import dev.jstech.computers.operation.payload.DesktopShellOutputPayload;
import dev.jstech.computers.operation.payload.DesktopShellRunPayload;
import dev.jstech.computers.operation.payload.RequestFileContentPayload;
import dev.jstech.computers.operation.payload.TerminalKeyboard;
import dev.jstech.computers.operation.payload.WireLine;
import dev.jstech.computers.operation.payload.program.DesktopShellPayloads;
import dev.jstech.computers.operation.payload.program.TerminalTools;
import dev.jstech.computers.os.Branding;
import dev.jstech.computers.os.edit.InkPalette;
import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.cli.CliStyle;
import dev.jstech.core.client.gui.component.CommandLine;
import dev.jstech.core.client.gui.component.Label;
import dev.jstech.core.client.gui.component.ListView;
import dev.jstech.core.client.gui.component.Panel;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.tier.HardwareEra;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * A view of a computer's console: the scrollback above, the command line below, and the machine's own
 * shell behind both.
 *
 * <p>A computer has one console, not one per window. This is a window onto it, so the terminal window
 * and the terminal panel inside an editor are two of these and both see what the machine said. What
 * one of them types the other watches happen, which is what having a single console means.
 */
public final class ShellView extends Panel {

    private static final int LINE_H = 9;
    private static final int PAD = 3;
    private static final int MAX_SCROLLBACK = 256;
    private static final int TAG_COLOR = 0xFF5A6678;

    private final BlockPos host;

    /**
     * This window's shell session on the machine.
     *
     * <p>Two terminals on one desktop are two shells: each keeps its own directory on the machine and
     * only hears the replies to what was typed in it. What the machine prints on its own reaches both.
     */
    private final int session = ShellViews.newSession();
    /*
     * The console's ground follows the system it runs on, and which system that is is a question only
     * this mod's skin answers; the toolkit's own context knows the shared look and not the form.
     */
    private OsSkin osSkin = OsSkin.fallback();
    /**
     * What is on the glass, wrapped to the columns the window has now.
     *
     * <p>The window is freely resized, and the glass wraps everything again when its width changes and at no
     * other time, so a console nobody types into costs nothing a frame.
     */
    private final TermBuffer scrollback = new TermBuffer(MAX_SCROLLBACK, TermBuffer.MONITOR_COLUMNS);

    /** Draws the glass a cell at a time, which is what makes a terminal's columns line up. */
    private final TermPainter painter = new TermPainter();

    /** What is picked out on the glass with the pointer, the same as at a machine's own prompt. */
    private final TermSelector selector = new TermSelector();
    private final ListView<TermRow> output;
    private final Label scrolledTag;
    private final CommandLine console;

    /** Whether this view is a terminal window of its own rather than a panel inside another program. */
    private boolean ownWindow;
    /** How many lines up from the bottom the output is scrolled. */
    private int scrollOffset;
    /** The prompt, synced from the server after each command so it tracks the current directory. */
    private String prompt;
    /**
     * Whether a program has the terminal.
     *
     * <p>While it does there is no prompt and nothing can be typed: the keyboard belongs to the program,
     * which listens for one thing, the ask to stop.
     */
    private boolean busy;

    /**
     * Who has the keyboard when it is not the prompt or a program: a tool the machine is running in front.
     *
     * <p>A tool is not typed at unless it has stopped to ask. When it has, its question stands where the
     * prompt would and the answer goes to it; asked unseen, what is typed is never drawn.
     */
    private TerminalKeyboard keyboard = TerminalKeyboard.PROMPT;

    /**
     * Whether the tool in front has printed a line on this glass yet.
     *
     * <p>A bar redraws the line it is on. A window opened half way through a fetch has no such line, so the
     * first thing it is sent goes under what is there rather than over it.
     */
    private boolean toolSpoke;

    /**
     * A view of the console of the computer at {@code host}.
     *
     * @param posix  whether the machine speaks bash rather than the DOS prompt
     * @param banner whether to greet the player, which the terminal window does and a panel inside
     *               another program does not
     */
    public ShellView(final BlockPos host, final boolean posix, final String systemName) {
        this.host = host;
        if (posix) {
            /*
             * A real Linux terminal opens on a bare prompt; the empty round trip below replaces this
             * placeholder with the server's user@host one.
             */
            this.prompt = "$";
        } else {
            this.prompt = "C:\\>";
            /*
             * A shell of this family opens by saying which system it belongs to, the way one does. The system is
             * the Midsoft house's, not the board maker's and certainly not the mod's, which is what this used to
             * print. A shell that cannot tell which system it is on says nothing at all.
             */
            if (systemName != null && !systemName.isEmpty()) {
                push(systemName, CliStyle.ACCENT);
                push(Branding.systemCopyright(systemName, era()), CliStyle.DIM);
                /*
                 * The way this family told a player where to start, in its own words. A line nobody ever printed
                 * would teach the same thing and sound like nothing; this one does both.
                 */
                push("Type HELP for a list of commands", CliStyle.DIM);
            }
        }
        this.output = add(new ListView<TermRow>(this.scrollback::rows, LINE_H, this::renderLine));
        this.scrolledTag = add(new Label(() -> this.scrollOffset > 0 ? "scrolled +" + this.scrollOffset : "")
                .setColor(TAG_COLOR).setAlign(Label.Align.RIGHT));
        /*
         * No prompt is drawn while a program is running, because on a real terminal there is none: the
         * program has the screen until it returns.
         */
        this.console = add(new CommandLine(DesktopShellRunPayload.MAX_LEN - 1, this::submit)
                .setPrompt(this::promptNow)
                .setUnseen(() -> this.keyboard.asking() && this.keyboard.unseen())
                .setTakesNothing(() -> this.keyboard.asking()));
        focus(this.console);
        ShellViews.register(this);
        // Sync the real prompt (and any pending build notices) before the player types anything.
        PacketDistributor.sendToServer(new DesktopShellRunPayload(host, "", this.session));
    }

    /** The shell session this window is on the machine. */
    public int session() {
        return this.session;
    }

    /** Asks the program in front to stop, the way Ctrl+C at the terminal does. */
    public void interrupt() {
        if (this.busy) {
            PacketDistributor.sendToServer(new DesktopShellRunPayload(this.host, DesktopShellPayloads.INTERRUPT,
                    this.session));
        }
    }

    /** Stops the machine's console being drawn into a view nobody is looking at. */
    public void release() {
        ShellViews.forget(this);
        CodeFileReplies.forget(this.opening);
    }

    /**
     * The editor that has taken this terminal, or null when the prompt has it.
     *
     * <p>A terminal editor is not a window: it is handed the glass the terminal was using, which is the
     * whole reason a machine with no desktop can still be programmed.
     */
    private TtyEditor editor;

    /** Whoever is waiting for the file an editor was asked to open. */
    private final CodeFileReplies.IReader opening = new CodeFileReplies.IReader() {
        @Override
        public void onContent(final String path, final String content, final boolean exists) {
            ShellView.this.editor = new TtyEditor(path, content,
                    ShellView.this.flavour, ShellView.this.terminalHost);
            ShellView.this.editor.opened(exists);
        }
    };

    /** How the editor being opened reads a keyboard, set just before the file is asked for. */
    private TtyEditor.IKeys flavour;

    /** What an editor running here can ask the terminal to do for it. */
    private final TtyEditor.IHost terminalHost = new TtyEditorWire(this::machine, this::editorDone);

    private BlockPos machine() {
        return this.host;
    }

    private void editorDone() {
        this.editor = null;
        /*
         * The prompt comes back where it was, so the machine is asked for it rather than guessed:
         * a program may have left the terminal somewhere else while the editor had it.
         */
        PacketDistributor.sendToServer(new DesktopShellRunPayload(this.host, ""));
    }

    /** Hands the terminal to an editor on {@code path}, which the machine is asked for. */
    public void openEditor(final String path, final TtyEditor.IKeys keys) {
        this.flavour = keys;
        CodeFileReplies.expectContent(this.opening, path);
        PacketDistributor.sendToServer(
                new RequestFileContentPayload(this.host, path));
    }

    /** Whether an editor has this terminal. */
    public boolean editing() {
        return this.editor != null;
    }

    /** The prompt as it stands, which says where the terminal is. */
    public String prompt() {
        return this.prompt == null ? "" : this.prompt;
    }

    /** The text the editor holding this terminal has, or empty when none has it. */
    public String editorText() {
        return this.editor == null ? "" : this.editor.document().text();
    }

    /** What the editor's second buffer shows, one line after another, or empty when there is none. */
    public String editorLowerText() {
        return this.editor == null ? "" : this.editor.lowerText();
    }

    /** Told each time a command finishes and the prompt is back, so a window can run lines in turn. */
    private Runnable onIdle;

    public ShellView setOnIdle(final Runnable action) {
        this.onIdle = action;
        return this;
    }

    /** Everything the console has printed here, as one piece of text. */
    public String scrollbackText() {
        final StringBuilder text = new StringBuilder();
        for (final CliLine line : this.scrollback.lines()) {
            text.append(line.text()).append('\n');
        }
        return text.toString();
    }

    /** The system this view is running under, which decides the console's ground. */
    public ShellView setSkin(final OsSkin value) {
        this.osSkin = value == null ? OsSkin.fallback() : value;
        return this;
    }

    /** Whether a program currently has the terminal. */
    public boolean busy() {
        return this.busy;
    }

    /** Puts a line in this view's scrollback without asking the machine anything. */
    public void say(final String text, final CliStyle style) {
        push(text, style);
    }

    /** Runs a line as though the player had typed it. */
    public void run(final String line) {
        submit(line);
    }

    /** Takes what the machine's console said. */
    void accept(final DesktopShellOutputPayload payload) {
        if (payload.clear()) {
            this.scrollback.clear();
        }
        // A bar growing on one line: what was printed last is drawn over rather than followed.
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
        // Lines the machine printed on its own say nothing about who has the prompt.
        if (payload.informational()) {
            return;
        }
        // An empty prompt means "unchanged"; otherwise track the new current directory.
        if (!payload.prompt().isEmpty()) {
            this.prompt = payload.prompt();
        }
        this.keyboard = payload.keyboard();
        if (!this.keyboard.busy()) {
            this.toolSpoke = false;
        }
        this.busy = payload.busy() || this.keyboard.busy();
        if (!this.busy && this.onIdle != null) {
            // A command finished: whoever queued the next line behind it may send it now.
            this.onIdle.run();
        }
        /*
         * The machine decided a command gives the terminal away, having checked that the editor is
         * installed. A terminal that has never heard of the one it named carries on with its prompt.
         */
        if (payload.handsOver()) {
            final TtyEditor.IKeys flavourAsked = TtyEditors.flavourOf(payload.editor());
            if (flavourAsked != null) {
                openEditor(payload.editorPath(), flavourAsked);
            }
        }
    }

    /** A line of one colour, which is what this view writes on its own account. */
    private void push(final String text, final CliStyle style) {
        this.scrollback.push(new CliLine(text, style));
    }

    /**
     * Says that this view is a terminal window of its own and not a panel inside another program, so it wears
     * the ground the desktop's own terminal had. Only CDE's differs: its terminal window was paper, written on
     * in dark inks, where a panel inside an editor stays the dark glass the editor is built around.
     */
    public ShellView asOwnWindow() {
        this.ownWindow = true;
        return this;
    }

    /** The ground this view is drawn on. */
    private int ground() {
        return this.ownWindow && this.osSkin.form() == OsSkin.Form.MOTIF ? TermPalette.PAPER : groundOf(this.osSkin);
    }

    /** The console ground, kept dark like a real terminal, tinted to the system it runs on. */
    public static int groundOf(final OsSkin skin) {
        return switch (skin.form()) {
            case BEVEL -> 0xFF000000;
            case LUNA -> 0xFF0A1A30;
            case FLAT -> 0xFF1E1F23;
            /*
             * The period Unix terminals were not pure black: xterm-era consoles carried a slight cast
             * from the desktop they ran on.
             */
            case KDE2 -> 0xFF0C1420;
            case GNOME1 -> 0xFF1A141E;
            // A slate with the cast of CDE's own backdrop, for a panel inside another program.
            case MOTIF -> 0xFF16202A;
        };
    }

    @Override
    public void render(final GuiGraphics g, final UiContext ctx) {
        final int ground = ground();
        if (this.editor != null) {
            // The editor has the glass: no scrollback, no prompt, exactly as at a real terminal.
            this.editor.render(g, ctx.font(), x(), y(), width(), height(),
                    InkPalette.forGround(!TermPalette.lightGround(ground)));
            return;
        }
        g.fill(x(), y(), right(), bottom(), ground);
        // Lines wrap to the columns the view has now, so nothing leaks past the frame however it is resized.
        this.scrollback.setColumns(TermPainter.columnsIn(Math.max(40, width() - PAD * 2 - 2), 1.0f));
        final List<TermRow> all = this.scrollback.rows();
        final int inputY = bottom() - LINE_H;
        final int visible = Math.max(1, (height() - PAD - LINE_H - 2) / LINE_H);
        final int maxScroll = Math.max(0, all.size() - visible);
        this.scrollOffset = Math.min(this.scrollOffset, maxScroll);
        // The output is anchored to its bottom: the scroll offset counts lines up from the newest.
        this.output.setBounds(x() + PAD, y() + PAD, width() - PAD * 2, visible * LINE_H);
        this.output.setScroll(maxScroll - this.scrollOffset);
        this.scrolledTag.setBounds(x() + PAD, inputY, width() - PAD * 2 - 1, 8);
        this.console.setBounds(x(), inputY - 2, width(), LINE_H + 2);
        // What is typed is written in the ink the echoed command will have, whichever kind of ground this is.
        this.console.setStyle(ground, TermPalette.inksFor(ground).applyAsInt(CliStyle.PROMPT));
        super.render(g, ctx);
    }

    private void renderLine(final GuiGraphics g, final UiContext ctx, final TermRow row, final int index,
                            final int x, final int y, final int w, final int h,
                            final boolean hovered, final boolean selected) {
        final int ground = ground();
        TermPainter.highlight(g, List.of(row), x, y, LINE_H, index, this.selector.selection(),
                TermPalette.selectionOn(ground));
        this.painter.drawRow(g, ctx.font(), row, x, y, TermPalette.inksFor(ground), ground);
    }

    @Override
    public boolean mouseClicked(final double mx, final double my, final int button) {
        super.mouseClicked(mx, my, button);
        final int row = this.output.rowAt(mx, my);
        if (button == 0 && row >= 0) {
            this.selector.pressed(this.scrollback.rows(), row, columnUnder(mx));
        } else if (button == 0) {
            this.selector.clear();
        }
        // Typing always goes to the command line: a click on the output must not take the keyboard away.
        focus(this.console);
        return true;
    }

    @Override
    public boolean mouseDragged(final double mx, final double my, final int button) {
        final int row = this.output.rowAt(mx, my);
        if (button == 0 && row >= 0) {
            this.selector.draggedTo(row, columnUnder(mx));
            return true;
        }
        return super.mouseDragged(mx, my, button);
    }

    @Override
    public boolean mouseReleased(final double mx, final double my, final int button) {
        this.selector.released();
        return super.mouseReleased(mx, my, button);
    }

    /** Which cell across a row the pointer is over, counted from the left of the output. */
    private int columnUnder(final double mx) {
        return TermPainter.columnAt(mx - this.output.x(), 1.0f);
    }

    @Override
    public boolean charTyped(final char c) {
        if (this.editor != null) {
            return this.editor.charTyped(c);
        }
        // While a program has the terminal, what is typed is the program's to read when it asks.
        return super.charTyped(c);
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        if (this.editor != null) {
            /*
             * Every key is the editor's, and one it has no use for goes nowhere. On a desktop that matters for
             * Escape, which would otherwise close the desktop under an editor holding text nobody has written:
             * a window is left with the mouse, so there is always another way out of this one.
             */
            this.editor.keyPressed(key, modifiers);
            return true;
        }
        if (key == GLFW.GLFW_KEY_C && ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0 || Screen.hasControlDown())
                && this.selector.copy(this.scrollback.rows())) {
            // Something is picked out, so this is a copy; with nothing picked out it is the interrupt below.
            return true;
        }
        if (this.busy && key == GLFW.GLFW_KEY_C && ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0
                || Screen.hasControlDown())) {
            interrupt();
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(final double mx, final double my, final double delta) {
        if (this.editor != null) {
            return this.editor.scrolled(delta);
        }
        this.scrollOffset = Math.max(0, this.scrollOffset + (delta > 0 ? 1 : -1));
        return true;
    }

    /** What stands in front of what is typed: the prompt, a tool's question, or nothing while something runs. */
    private String promptNow() {
        if (this.keyboard.asking()) {
            return this.keyboard.standing().text().stripTrailing();
        }
        return this.busy ? "" : this.prompt;
    }

    private void submit(final String line) {
        this.scrollOffset = 0;
        if (this.keyboard.busy()) {
            /*
             * A tool is in front. It is typed at only when it has asked, and what is typed is not echoed
             * here: the machine prints the question with its answer for every window looking at it, and
             * prints the question alone when the answer was not for showing.
             */
            if (this.keyboard.asking()) {
                PacketDistributor.sendToServer(new DesktopShellRunPayload(this.host,
                        line.isEmpty() ? TerminalTools.ENTER : line, this.session));
            }
            return;
        }
        if (this.busy) {
            /*
             * A line for the program in front: it shows as typed, with no prompt, and goes to the machine
             * for the program to read. None of the terminal's own words mean anything here.
             */
            push(line, CliStyle.PROMPT);
            PacketDistributor.sendToServer(new DesktopShellRunPayload(this.host, line, this.session));
            return;
        }
        push(this.prompt + " " + line, CliStyle.PROMPT);
        final String[] parts = line.split("\\s+", 2);
        final String verb = parts[0].toLowerCase(Locale.ROOT);
        // "run/start/open <program>" launches a desktop window client-side (the server shell has no windows).
        if (verb.equals("run") || verb.equals("start") || verb.equals("open")) {
            handleRun(parts.length > 1 ? parts[1].trim() : "");
            return;
        }
        PacketDistributor.sendToServer(new DesktopShellRunPayload(this.host, line, this.session));
    }

    /** Opens an installed program's window by name, or lists what can be opened. */
    private void handleRun(final String name) {
        final List<String> labels = DesktopScreen.openableLabels();
        if (name.isEmpty()) {
            push("Programs: " + String.join(", ", labels), CliStyle.PLAIN);
            push("Usage: run <program>", CliStyle.DIM);
            return;
        }
        final String norm = name.toLowerCase(Locale.ROOT).replace(" ", "");
        for (final String label : labels) {
            if (label.equalsIgnoreCase(name) || label.toLowerCase(Locale.ROOT).replace(" ", "").equals(norm)) {
                DesktopScreen.requestOpen(label);
                push("Opening " + label + "...", CliStyle.OK);
                return;
            }
        }
        push("No such program: " + name + " (type 'run' to list them)", CliStyle.ERROR);
    }

    /**
     * The host computer's generation, for the year under the system's name.
     *
     * <p>Read off the machine on the client, the way the other screens here do it, and the middle generation when
     * the computer is not loaded, which only decides a year for a system nobody has heard of.
     */
    private HardwareEra era() {
        final Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.getBlockEntity(this.host)
                instanceof AbstractComputerBlockEntity computer
                && computer.displayEra() != null) {
            return computer.displayEra();
        }
        return HardwareEra.STANDARD;
    }
}
