/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.client.os.CodeFileReplies;
import dev.jstech.computers.client.os.ParkedEditors;
import dev.jstech.computers.client.os.TtyEditor;
import dev.jstech.computers.client.os.TtyEditorWire;
import dev.jstech.computers.client.os.TtyEditors;
import dev.jstech.computers.menu.CommandPromptMenu;
import dev.jstech.computers.operation.payload.CommandOutputPayload;
import dev.jstech.computers.operation.payload.ConsoleInitPayload;
import dev.jstech.computers.operation.payload.RequestConsoleInitPayload;
import dev.jstech.computers.operation.payload.RequestFileContentPayload;
import dev.jstech.computers.operation.payload.RunCommandPayload;
import dev.jstech.computers.os.Branding;
import dev.jstech.computers.os.edit.InkPalette;
import dev.jstech.computers.client.term.TermPainter;
import dev.jstech.computers.client.term.TermPalette;
import dev.jstech.computers.client.term.TermSelector;
import dev.jstech.computers.gui.MonitorGlass;
import dev.jstech.computers.gui.term.TermBuffer;
import dev.jstech.computers.gui.term.TermCompletion;
import dev.jstech.computers.gui.term.TermInput;
import dev.jstech.computers.gui.term.TermRow;
import dev.jstech.computers.operation.payload.TerminalKeyboard;
import dev.jstech.computers.operation.payload.WireLine;
import dev.jstech.computers.operation.payload.program.DesktopShellPayloads;
import dev.jstech.computers.operation.payload.program.TerminalTools;
import dev.jstech.computers.os.ConsoleIdentity;
import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.cli.CliStyle;
import dev.jstech.computers.program.cli.ConsoleGreeting;
import dev.jstech.core.client.gui.theme.JsTechTheme;
import dev.jstech.core.gui.LineHistory;
import dev.jstech.core.gui.Phosphor;
import dev.jstech.core.tier.HardwareEra;
import java.util.HashMap;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The Command Prompt: a full CLI over the computer the Monitor is bound to. A typed line is echoed, sent to the server to run through the shell, and the styled result is appended to the scrollback. Up/Down walk the input history; the mouse wheel scrolls back through output. The same OS skin as the rest of the computing GUIs, square corners and all.
 */
public class CommandPromptScreen<M extends CommandPromptMenu> extends AbstractComputerScreen<M>
        implements MachineKeyboard.ITakesKeysFirst {

    private static final int CONSOLE = 0xFF070A0E;

    /** Where a network system stands before it has said so itself: at the machine, since it has no path. */
    private static final String NET_PROMPT = "SYSTEM:>";
    private static final int MAX_SCROLLBACK = 512;

    /**
     * The largest the text is drawn, which is the three quarters a desktop is drawn at unless told otherwise;
     * a glass too narrow for its columns at this size draws it smaller.
     */
    private static final float TEXT_SCALE = 0.75f;

    /** How long the cursor is there for, and then not there for, in milliseconds. */
    private static final long CURSOR_BLINK_MS = 500L;

    /**
     * How much of a paste a terminal takes: enough for a handful of commands or a wrapped path, and no more.
     *
     * <p>A paste is whatever happened to be on the clipboard, which may be a whole file. Running a thousand
     * lines at a machine because somebody meant to paste one is not a thing a terminal should let happen.
     */
    private static final int MOST_PASTED_LINES = 16;
    private static final int MOST_PASTED_LETTERS = 512;
    private static final int LINE_H = 9;

    /**
     * What each machine's prompt has printed, kept while the game runs.
     *
     * <p>A terminal's screen belongs to the machine, not to the window that happens to be showing it:
     * walking away from a TTY and coming back finds what was there, the way a real one does. The lines
     * are kept here by machine, so opening the prompt again picks up where it was.
     */
    private static final Map<BlockPos, Kept> KEPT = new HashMap<>();

    /**
     * What one machine's terminal has printed, and which run of that machine printed it.
     *
     * <p>The lines outlive the window, not the machine: a restart ends the run they belong to. Without the
     * run, a machine came up showing the installation that had just been typed into it, and a disk swapped
     * for a blank one came up showing the session of the disk that had been taken out.
     */
    private record Kept(long session, TermBuffer glass) {
    }

    /** What is on the glass: the lines the machine sent, cut into rows of the columns the glass has. */
    private final TermBuffer scrollback;

    /** Draws the glass a cell at a time, which is what makes a terminal's columns line up. */
    private final TermPainter painter = new TermPainter();

    /** What is picked out on the glass with the pointer, and what copying it puts on the clipboard. */
    private final TermSelector selector = new TermSelector();

    /** How much smaller than the game's own the text is drawn, worked out from the room the glass has. */
    private float textScale = TEXT_SCALE;

    /** Who has the keyboard: the prompt, or a tool the machine is running in front of it. */
    private TerminalKeyboard keyboard = TerminalKeyboard.PROMPT;

    /**
     * Whether the tool in front has printed a line on this glass yet.
     *
     * <p>A bar redraws the line it is on. A monitor opened half way through a fetch has no such line, so the
     * first thing it is sent goes under what is there rather than over it.
     */
    private boolean toolSpoke;
    /** Whether the machine's identity line has been added, so a late init reply adds it only once. */
    private boolean identityShown;

    /** The lines typed at this machine, which the arrow keys walk through. */
    private final LineHistory history = new LineHistory();

    /** What Tab finishes: the commands the machine's shell knows and the drives it has. */
    private final TermCompletion completion = new TermCompletion();
    private final Map<String, String> commandUsage = new LinkedHashMap<>();
    private int scrollOffset;
    private boolean programmaticEdit;

    private EditBox input;

    /** The DOS prompt, synced from the server after each command so it tracks the current directory. */
    private String dosPrompt = "C:\\>";

    /** The same prompt a run at a time, when the machine sent it that way; null for one that came as words. */
    @Nullable
    private CliLine promptLine;

    public CommandPromptScreen(final M menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
        this.imageWidth = 256;
        this.imageHeight = 178;
        this.titleLabelX = -10000;
        this.inventoryLabelY = -10000;
        final Kept had = KEPT.get(menu.hostPos());
        final Kept kept = had != null && had.session() == menu.session()
                ? had : new Kept(menu.session(), new TermBuffer(MAX_SCROLLBACK, TermBuffer.MONITOR_COLUMNS));
        KEPT.put(menu.hostPos(), kept);
        this.scrollback = kept.glass();
        // A screen that still has its lines has already said whose it is.
        this.identityShown = !this.scrollback.isEmpty();
    }

    /** Whether this terminal wears the MC-DOS identity (the dedicated DOS screen overrides). */
    protected boolean dosStyle() {
        return false;
    }

    /**
     * Whether this terminal is a network system's, which greets in its own house's voice and stands at a
     * prompt that names the machine rather than a place on a disk.
     */
    protected boolean netStyle() {
        return false;
    }

    /**
     * A bare terminal draws no window chrome, just the glass and the text, the way a real console
     * fills its display. Every console the mod opens is one: a machine at its prompt is the prompt,
     * and a window around it would be a window drawn by nothing.
     */
    protected boolean bareTerminal() {
        return false;
    }

    @Override
    protected void init() {
        /*
         * A console fills the same glass everything else a machine shows does, the desktop included: a monitor
         * does not change size when the machine leaves one for the other.
         */
        this.imageWidth = MonitorGlass.width(this.width);
        this.imageHeight = MonitorGlass.height(this.height);
        /*
         * The glass always has the same columns, so on a window too small to hold them at the usual size the
         * text is drawn smaller instead of the glass losing columns: what the machine laid out for that many
         * cells stays laid out.
         */
        this.textScale = Math.min(TEXT_SCALE,
                (this.imageWidth - 20) / (float) (TermBuffer.MONITOR_COLUMNS * TermPainter.CELL));
        super.init();
        /*
         * The box holds what is being typed and takes the keys that edit it, and that is all it does. It is
         * never drawn: the line being typed is the last line on the glass, in the same cells at the same size
         * as everything above it, so it is laid out and painted with the rest. It sits well off the window so
         * that a click on the glass cannot land in it and move a cursor nobody can see it moving.
         */
        input = new EditBox(font, -4000, -4000, 40, 11, Component.literal("command"));
        input.setBordered(false);
        input.setMaxLength(RunCommandPayload.MAX_LEN);
        input.setFocused(true);
        // A real edit (typing/backspace) restarts Tab cycling; our own programmatic setValue does not.
        input.setResponder(s -> {
            if (!programmaticEdit) {
                completion.typedByHand();
            }
        });
        setInitialFocus(input);
        addWidget(input);
        if (scrollback.isEmpty()) {
            /*
             * Opening a terminal starts a session, and a session announces itself. Restoring the old
             * scrollback used to drop the player mid-conversation with no sign the program had just
             * been opened; the command history (arrow keys) still persists, which is the part worth
             * keeping; a fresh terminal emulator behaves exactly like this.
             */
            if (menu.posixShell()) {
                // A Unix console: the banner, the login and the welcome, each family's in its own shape.
                ConsoleGreeting.of(menu.console()).forEach(scrollback::push);
            } else if (dosStyle()) {
                /*
                 * MC-DOS wears a period boot banner instead of the generic shell greeting. The lines are
                 * kept short on purpose so they never overflow the narrow 256px window.
                 */
                push(menu.osLabel() + "  Version 1.0  [Network Build]", CliStyle.ACCENT);
                push(Branding.systemCopyright(
                        menu.osLabel(), screenEra()), CliStyle.DIM);
                push("640K base memory", CliStyle.DIM);
                push("", CliStyle.PLAIN);
            } else if (netStyle()) {
                /*
                 * A network system with no space on it is a prompt and nothing else, so the first thing it
                 * says is what it is and how to give it a face again. It is the same relationship a Linux
                 * has with its desktop, and the same answer: the interface is a package, so fetch one.
                 */
                push(menu.osLabel() + " 1.0", CliStyle.ACCENT);
                push(Branding.systemCopyright(menu.osLabel(), screenEra()), CliStyle.DIM);
                push("", CliStyle.PLAIN);
                push("No operating space is installed.", CliStyle.WARN);
                push("netgetter install interactor  puts one on.", CliStyle.DIM);
                push("", CliStyle.PLAIN);
            } else {
                push(Branding.houseOf(menu.osLabel()).name()
                        + " Shell v1.0", CliStyle.ACCENT);
                push("type 'help' for commands, TAB to complete", CliStyle.DIM);
                push("", CliStyle.PLAIN);
            }
        }
        /*
         * An editor somebody looked away from is still open on the machine, so it is what this look finds. The
         * glass is given to it again either way, because a window that changed size has a fresh prompt line to
         * keep out from under it.
         */
        if (this.editor == null) {
            final TtyEditor left = ParkedEditors.take(menu.hostPos(), menu.session());
            if (left != null) {
                left.handTo(this.terminalHost);
                this.editor = left;
            }
        }
        giveTheGlassTo(this.editor);
        // Ask the server for this computer's saved history and the command list (for completion).
        PacketDistributor.sendToServer(new RequestConsoleInitPayload(menu.hostPos()));
    }

    // output

    /** Routes a server output payload to the open Command Prompt, if one is showing. */
    public static void accept(final CommandOutputPayload payload) {
        if (Minecraft.getInstance().screen instanceof CommandPromptScreen screen) {
            screen.apply(payload);
        }
    }

    /** Seeds the open Command Prompt with the computer's saved history and the command list. */
    public static void acceptInit(final ConsoleInitPayload payload) {
        if (Minecraft.getInstance().screen instanceof CommandPromptScreen screen) {
            screen.applyInit(payload);
        }
    }

    private void applyInit(final ConsoleInitPayload payload) {
        /*
         * Only this computer's console seeds this screen: a reply raced from another machine's prompt must
         * never leak its history (Up-arrow on computer B recalling computer A's commands).
         */
        if (!menu.hostPos().equals(payload.hostPos())) {
            return;
        }
        history.replaceWith(payload.history());
        final List<String> names = new ArrayList<>(payload.commands().size());
        commandUsage.clear();
        for (final ConsoleInitPayload.WireCommand command : payload.commands()) {
            names.add(command.name());
            commandUsage.put(command.name(), command.usage());
        }
        completion.know(names, payload.devices());
        /*
         * Say which machine this session is on. It arrives with the init reply (a tick after the
         * window opens) rather than being guessed client-side, and it is what makes a terminal
         * opened over ssh or a KVM channel obviously belong to the machine it is talking to.
         */
        if (!identityShown) {
            identityShown = true;
            final String machine = menu.hostname().isBlank() ? "machine" : menu.hostname();
            final String drives = payload.devices().isEmpty() ? "no drives"
                    : payload.devices().size() + (payload.devices().size() == 1 ? " drive" : " drives");
            push("machine: " + machine + "  ·  " + drives, CliStyle.DIM);
            push("", CliStyle.PLAIN);
        }
    }

    private void apply(final CommandOutputPayload payload) {
        if (payload.clear()) {
            scrollback.clear();
        }
        // A bar growing on one line: what was printed last is drawn over rather than followed.
        boolean over = payload.replaceLast();
        for (final WireLine line : payload.lines()) {
            final CliLine said = line.toLine();
            if (over || (line.over() && toolSpoke)) {
                scrollback.replaceLast(said);
                over = false;
            } else {
                scrollback.push(said);
            }
            toolSpoke = toolSpoke || payload.keyboard().busy();
        }
        keyboard = payload.keyboard();
        if (!keyboard.busy()) {
            toolSpoke = false;
        }
        /*
         * An empty prompt means "unchanged"; otherwise track the new current directory. A prompt that came a
         * run at a time is the one shown, in the shell's own colours; one that came only as words is shown in
         * the terminal's.
         */
        if (keyboard.namesThePrompt()) {
            promptLine = keyboard.standing().toLine();
            dosPrompt = promptLine.text();
        } else if (!payload.prompt().isEmpty()) {
            dosPrompt = payload.prompt();
            promptLine = null;
        }
        scrollOffset = 0;
        /*
         * The machine decided a command gives the terminal away, having checked the editor is there.
         * A terminal that has never heard of the one it named carries on with its prompt.
         */
        if (payload.handsOver()) {
            final var flavourAsked =
                    TtyEditors.flavourOf(payload.editor());
            if (flavourAsked != null) {
                openEditor(payload.editorPath(), flavourAsked);
            }
        }
    }

    /** The console's scrollback, oldest first, what the player can read on the prompt right now. */
    public List<String> scrollbackText() {
        final List<String> lines = new ArrayList<>(scrollback.rows().size());
        for (final TermRow row : scrollback.rows()) {
            lines.add(row.text());
        }
        return lines;
    }

    /** What is on the command line now, typed or put there by Tab or the arrow keys. */
    public String typed() {
        return input == null ? "" : input.getValue();
    }

    /**
     * A line of one colour, which is what this screen writes on its own account: a banner, the echo of what
     * was typed. Wrapping is the glass's business, by columns, so a long line never leaks past it and the
     * wheel scrolls real rows.
     */
    private void push(final String text, final CliStyle style) {
        scrollback.push(new CliLine(text, style));
    }

    private void submit() {
        final String line = input.getValue().trim();
        input.setValue("");
        history.rest();
        if (keyboard.busy()) {
            /*
             * A tool is in front. It is typed at only when it has asked, and what is typed is not echoed
             * here: the machine prints the question with its answer for every terminal looking at it, and
             * the question alone when the answer was not for showing.
             */
            if (keyboard.asking()) {
                scrollOffset = 0;
                PacketDistributor.sendToServer(new RunCommandPayload(menu.monitorPos(), menu.hostPos(),
                        line.isEmpty() ? TerminalTools.ENTER : line));
            }
            return;
        }
        // The line stays on the glass as it looked while it was being typed, the prompt's colours included.
        scrollback.push(CliLine.build().add(before()).add(" " + line, CliStyle.PROMPT).done());
        if (line.isEmpty()) {
            return;
        }
        history.add(line);
        scrollOffset = 0;
        PacketDistributor.sendToServer(new RunCommandPayload(menu.monitorPos(), menu.hostPos(), line));
    }

    // rendering

    @Override
    protected void renderBg(final GuiGraphics g, final float partialTick, final int mouseX, final int mouseY) {
        final int x = leftPos;
        final int y = topPos;
        // The monitor frame wraps the whole software window in the host computer's hardware-era bezel.
        MonitorFrame.renderBody(g, x, y, imageWidth, imageHeight, screenEra(), font);
        if (bareTerminal()) {
            // A raw console: the whole glass is the terminal, no program window around it.
            g.fill(x, y, x + imageWidth, y + imageHeight, bareGlass());
            return;
        }
        JsTechTheme.window(g, x, y, imageWidth, imageHeight);
        JsTechTheme.headerBar(g, x + 6, y + 6, imageWidth - 12);
        // The console panel.
        final int top = y + 26;
        final int bottom = y + imageHeight - 22;
        g.fill(x + 6, top, x + imageWidth - 6, bottom, CONSOLE);
        g.fill(x + 6, top, x + imageWidth - 6, top + 1, JsTechTheme.line());
        // Input strip.
        g.fill(x + 6, y + imageHeight - 20, x + imageWidth - 6, y + imageHeight - 8, JsTechTheme.panel());
        g.fill(x + 6, y + imageHeight - 20, x + imageWidth - 6, y + imageHeight - 19, JsTechTheme.line());
    }

    @Override
    protected void renderLabels(final GuiGraphics g, final int mouseX, final int mouseY) {
        if (!bareTerminal()) {
            JsTechTheme.text(g, font, "COMMAND PROMPT", 12, 11, JsTechTheme.text());
            JsTechTheme.textRight(g, font, "PROGRAM", imageWidth - 10, 11, JsTechTheme.accent());
        }

        // Console scrollback, newest at the bottom, honoring the scroll offset.
        final TermInput.Laid typing = typing();
        final int top = scrollbackTop();
        final int bottom = imageHeight - 23 - Math.round((typing.rows().size() - 1) * rowPitch() * textScale);
        final int visible = visibleRows();
        final List<TermRow> all = scrollback.rows();
        final int end = Math.max(0, all.size() - scrollOffset);
        final int start = firstVisibleRow();
        /*
         * Drawn in the glass's own scale, so a row's pitch and a cell's width are the same whole numbers
         * whatever size the text comes out at, and the whole glass goes to the card in one batch.
         */
        g.pose().pushPose();
        g.pose().translate(10, top, 0);
        g.pose().scale(textScale, textScale, 1.0f);
        TermPainter.highlight(g, all.subList(start, end), 0, 0, rowPitch(), start, selector.selection(),
                TermPalette.selectionOn(glass()));
        painter.draw(g, font, all.subList(start, end), 0, 0, rowPitch(), this::colorOf, glass());
        g.pose().popPose();
        if (scrollOffset > 0) {
            JsTechTheme.textSRight(g, font, "scrolled +" + scrollOffset, imageWidth - 10, bottom - 7, JsTechTheme.dim());
        }

        if (this.editor != null) {
            // An editor has the glass, its own keys listed on it, and nothing of the prompt's is under it.
            return;
        }
        drawTyping(g, typing);

        // Usage hint: once the verb is recognised, show how it is used, dimmed on the right.
        final String typed = input == null ? "" : input.getValue().trim();
        final int space = typed.indexOf(' ');
        final String verb = (space < 0 ? typed : typed.substring(0, space)).toLowerCase(Locale.ROOT);
        final String usage = keyboard.busy() || typing.rows().size() > 1 ? null : commandUsage.get(verb);
        if (usage != null && !usage.isEmpty()) {
            /*
             * The hint shares the input line: it gets the room to the right of what is typed, and is cut
             * short rather than drawn over the prompt when a long usage does not fit.
             */
            final int used = Math.round((typing.rows().get(0).length() + 2) * TermPainter.CELL * textScale);
            final int room = imageWidth - 10 - (10 + used);
            if (room >= 40) {
                String hint = verb + " " + usage;
                if (JsTechTheme.widthS(font, hint) > room) {
                    String cut = hint;
                    while (cut.length() > 3 && JsTechTheme.widthS(font, cut + "..") > room) {
                        cut = cut.substring(0, cut.length() - 1);
                    }
                    hint = cut + "..";
                }
                JsTechTheme.textSRight(g, font, hint, imageWidth - 10, imageHeight - 17, JsTechTheme.dim());
            }
        }
        JsTechTheme.textS(g, font, keyboard.busy()
                        ? "CTRL+C interrupt    wheel scroll    ESC close"
                        : "ENTER run    UP/DOWN history    wheel scroll    ESC close",
                10, imageHeight - 7, JsTechTheme.dim());
    }

    /**
     * The line being typed, laid out on the glass's grid: the prompt or a tool's question, what has been typed
     * after it, and the cell the cursor is in. An answer asked for unseen is typed and never drawn, not even
     * as dots, which is how the tools that ask for one have always taken it.
     */
    private TermInput.Laid typing() {
        final boolean unseen = keyboard.asking() && keyboard.unseen();
        final boolean shown = input != null && !unseen;
        return TermInput.lay(before(), shown ? input.getValue() : "", CliStyle.PROMPT,
                shown ? input.getCursorPosition() : 0, shown ? markOf(input) : 0,
                scrollback.columns());
    }

    /**
     * Where the far end of what Shift and the arrows have picked out sits in the line being typed.
     *
     * <p>The box keeps that end to itself and hands over only the letters between the two, so it is worked out
     * from them: if the letters are the ones after the cursor the far end is to its right, and otherwise to its
     * left. With nothing picked out there are no letters and both ends are the cursor.
     */
    private static int markOf(final EditBox box) {
        final String picked = box.getHighlighted();
        final int cursor = box.getCursorPosition();
        return box.getValue().startsWith(picked, cursor) ? cursor + picked.length() : cursor - picked.length();
    }

    /**
     * Paints the line being typed as the last rows of the glass, in the same cells at the same size as what is
     * above it, with the cursor under the cell the next character goes in.
     */
    private void drawTyping(final GuiGraphics g, final TermInput.Laid typing) {
        if (keyboard.busy() && !keyboard.asking()) {
            // A tool is working and has asked nothing: there is no prompt, as there is none at a real one.
            return;
        }
        final float rowHeight = rowPitch() * textScale;
        final int last = typing.rows().size() - 1;
        for (int i = 0; i <= last; i++) {
            g.pose().pushPose();
            g.pose().translate(10, imageHeight - 17 - (last - i) * rowHeight, 0);
            g.pose().scale(textScale, textScale, 1.0f);
            TermPainter.highlight(g, typing.rows().subList(i, i + 1), 0, 0, rowPitch(), i, typing.selection(),
                    TermPalette.selectionOn(typingGround()));
            painter.drawOnce(g, font, typing.rows().get(i), 0, 0, this::colorOf, typingGround());
            if (i == typing.cursorRow() && (Util.getMillis() / CURSOR_BLINK_MS) % 2 == 0) {
                final int at = typing.cursorColumn() * TermPainter.CELL;
                g.fill(at, LINE_H - 1, at + TermPainter.CELL - 1, LINE_H, colorOf(CliStyle.PROMPT));
            }
            g.pose().popPose();
        }
    }

    /**
     * How far apart the rows are, in the glass's own scaled units.
     *
     * <p>A whole number of them, so every row lands on a whole unit and no row's text is drawn between two
     * pixels of the scaled grid, which is what makes small text look smeared.
     */
    private int rowPitch() {
        return Math.round(LINE_H / textScale);
    }

    /** What the scrollback is written on, which is what the shadow under it is worked out against. */
    private int glass() {
        return bareTerminal() ? bareGlass() : CONSOLE;
    }

    /** What the line being typed is written on: the glass of a raw console, the input strip of a window. */
    private int typingGround() {
        return bareTerminal() ? bareGlass() : JsTechTheme.panel();
    }

    /** What a raw console is written on: the whole glass, with no program window around it. */
    private static int bareGlass() {
        return TermPalette.GLASS.get().ground();
    }

    private int colorOf(final CliStyle style) {
        /*
         * A Vintage machine draws on a green-phosphor tube, which has ONE colour: every style comes out
         * as that green, brighter or dimmer, so an error still reads as an error without being red.
         */
        final int color = TermPalette.colorOf(style);
        return screenEra() == HardwareEra.VINTAGE ? Phosphor.green(color) : color;
    }

    // input

    /** A terminal has a use for every key there is, so none of them is anybody else's while it is open. */
    @Override
    public boolean keyFirst(final int key, final int scanCode, final int modifiers) {
        return keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean keyPressed(final int key, final int scan, final int mods) {
        /*
         * While an editor has the terminal every key is its. Escape is the one it may hand back: an editor
         * with no use for it leaves the player free to look away from the monitor, and what was being edited
         * is kept for when they look again, since walking away from a machine closes nothing on it.
         */
        if (this.editor != null) {
            if (!this.editor.keyPressed(key, mods) && key == 256) {
                ParkedEditors.park(menu.hostPos(), menu.session(), this.editor);
                onClose();
            }
            return true;
        }
        /*
         * Copying and pasting come before everything else a key means here, at a prompt and in front of a tool
         * alike. Ctrl+C with something picked out copies it, which is the one thing it can mean then; with
         * nothing picked out it falls through to interrupting, as it always has.
         */
        if ((mods & GLFW.GLFW_MOD_CONTROL) != 0 && key == InputConstants.KEY_C && selector.copy(scrollback.rows())) {
            return true;
        }
        if ((mods & GLFW.GLFW_MOD_CONTROL) != 0 && key == InputConstants.KEY_V) {
            paste();
            return true;
        }
        if (key == 257 || key == 335) { // Enter / numpad Enter
            selector.clear();
            submit();
            return true;
        }
        if (keyboard.busy()) {
            /*
             * A tool has the terminal, and the one thing the terminal itself still understands is Ctrl+C.
             * History, completion and the rest belong to a prompt that is not there.
             */
            if (key == InputConstants.KEY_C && (mods & GLFW.GLFW_MOD_CONTROL) != 0) {
                PacketDistributor.sendToServer(new RunCommandPayload(menu.monitorPos(), menu.hostPos(),
                        DesktopShellPayloads.INTERRUPT));
                return true;
            }
            if (key != 256) {
                // The line being typed is only there to be edited when the tool has asked for one.
                if (keyboard.asking() && input != null) {
                    input.keyPressed(key, scan, mods);
                }
                return true;
            }
        }
        if (key == 258) { // Tab: complete the command word
            complete();
            return true;
        }
        if (key == 265) { // Up: older history
            recallHistory(-1);
            return true;
        }
        if (key == 264) { // Down: newer history
            recallHistory(1);
            return true;
        }
        if (key == 256) { // Esc closes the prompt
            onClose();
            return true;
        }
        /*
         * Everything else (typing, backspace, arrows within the line) goes to the input box, so the
         * inventory key never reaches the screen and closes it mid-command.
         */
        if (input != null) {
            input.keyPressed(key, scan, mods);
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean charTyped(final char c, final int mods) {
        if (this.editor != null) {
            return this.editor.charTyped(c);
        }
        // A tool that is working and has asked nothing is not reading the keyboard.
        if (keyboard.busy() && !keyboard.asking()) {
            return true;
        }
        return input != null && input.charTyped(c, mods);
    }

    /** Tab: the command being typed, or the drive a command is being pointed at, a candidate a press. */
    private void complete() {
        completion.next(input.getValue()).ifPresent(line -> {
            // Put there by the terminal and not typed, so it does not end the round of presses it is part of.
            programmaticEdit = true;
            input.setValue(line);
            input.moveCursorToEnd(false);
            programmaticEdit = false;
        });
    }

    private void recallHistory(final int direction) {
        history.recall(direction).ifPresent(input::setValue);
    }

    /**
     * Types what is on the clipboard.
     *
     * <p>Several lines are run one after another, which is what a terminal does with a paste and what makes
     * pasting a few commands worth doing; the last one is left on the prompt unrun, since a paste that did not
     * end in a newline is a line somebody is still writing.
     */
    private void paste() {
        if (input == null || (keyboard.busy() && !keyboard.asking())) {
            return;
        }
        final String text = TermSelector.clipboard();
        if (text.isEmpty()) {
            return;
        }
        final String[] lines = text.split("\r?\n", -1);
        for (int i = 0; i < lines.length && i < MOST_PASTED_LINES; i++) {
            final String line = lines[i].length() > MOST_PASTED_LETTERS
                    ? lines[i].substring(0, MOST_PASTED_LETTERS) : lines[i];
            input.insertText(line);
            if (i < lines.length - 1 && i < MOST_PASTED_LINES - 1) {
                submit();
            }
        }
    }

    @Override
    public boolean mouseClicked(final double mx, final double my, final int button) {
        /*
         * A program holding the whole glass is handed the cell that was clicked, the way a terminal hands one
         * to a program that asked for the mouse. Which line of the file that is depends on the program's own
         * view, so only the cell travels.
         */
        if (this.editor != null && button == 0 && overGlass(mx, my)) {
            return this.editor.clicked(TermPainter.rowAt(my - topPos - scrollbackTop(), rowPitch(), textScale),
                    columnUnder(mx));
        }
        if (this.editor == null && button == 0 && overGlass(mx, my)) {
            selector.pressed(scrollback.rows(), rowUnder(my), columnUnder(mx));
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(final double mx, final double my, final int button, final double dx, final double dy) {
        if (this.editor == null && button == 0) {
            selector.draggedTo(rowUnder(my), columnUnder(mx));
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(final double mx, final double my, final int button) {
        selector.released();
        return super.mouseReleased(mx, my, button);
    }

    /** Whether the pointer is over the rows the machine has printed, which is what can be picked out. */
    private boolean overGlass(final double mx, final double my) {
        return mx >= leftPos + 10 && mx < leftPos + imageWidth - 10
                && my >= topPos + scrollbackTop() && my < topPos + imageHeight - 23;
    }

    /** Which row of the buffer the pointer is over, counting from the top of the buffer and not of the glass. */
    private int rowUnder(final double my) {
        return firstVisibleRow() + TermPainter.rowAt(my - topPos - scrollbackTop(), rowPitch(), textScale);
    }

    private int columnUnder(final double mx) {
        return TermPainter.columnAt(mx - leftPos - 10, textScale);
    }

    /** The row of the buffer the top of the glass is showing, which is where what is drawn starts. */
    private int firstVisibleRow() {
        final int end = Math.max(0, scrollback.rows().size() - scrollOffset);
        return Math.max(0, end - visibleRows());
    }

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double dx, final double dy) {
        if (this.editor != null) {
            return this.editor.scrolled(dy);
        }
        final int maxScroll = Math.max(0, scrollback.rows().size() - visibleRows());
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset + (int) Math.signum(dy)));
        return true;
    }

    /**
     * Where the scrollback starts down the glass. A terminal taking the whole monitor has no header above it,
     * so it starts higher and fits more rows than one drawn in the window with a title bar.
     */
    private int scrollbackTop() {
        return bareTerminal() ? 8 : 27;
    }

    /**
     * How many rows of scrollback the glass shows.
     *
     * <p>Read by the drawing and by the wheel, from here, because they used to work it out separately and the
     * wheel used the windowed header height on a bare terminal too. It therefore thought fewer rows fitted
     * than really did, let the scroll go that many rows past the top, and the rows at the bottom emptied out
     * one by one with nothing left to take their place.
     */
    private int visibleRows() {
        // The line being typed is one row as a rule; every row past that comes out of what the scrollback has.
        final int typingRows = typing().rows().size() - 1;
        return Math.max(1, (int) ((imageHeight - 23 - scrollbackTop()) / (rowPitch() * textScale)) - typingRows);
    }


    @Override
    public void removed() {
        /*
         * Nothing to remember: a terminal that is closed is over, and the next one opens fresh with
         * its own banner. The command history lives on the machine and comes back with the session.
         */
        super.removed();
    }

    @Override
    public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        if (this.editor != null) {
            /*
             * The editor has the glass: over everything, because it is what the terminal is showing
             * now, not something drawn on top of a console that is still there.
             */
            /*
             * At the size the terminal's own text is, with its rows the same distance apart, so taking the
             * glass over does not change how big anything on it is.
             */
            this.editor.setRowPitch(rowPitch());
            g.pose().pushPose();
            g.pose().translate(leftPos + 8, topPos + 8, 0);
            g.pose().scale(textScale, textScale, 1.0f);
            this.editor.render(g, font, 0, 0, Math.round((imageWidth - 16) / textScale),
                    Math.round((imageHeight - 16) / textScale), InkPalette.GLASS.get());
            g.pose().popPose();
        }
    }

    /**
     * The editor that has this terminal, or null when the prompt has it.
     *
     * <p>This screen is the terminal of a machine that may have no desktop at all, which is exactly
     * where an editor that needs none earns its place.
     */
    private TtyEditor editor;

    /** Whoever is waiting for the file the machine said to open. */
    private final CodeFileReplies.IReader opening =
            new CodeFileReplies.IReader() {
                @Override
                public void onContent(final String path, final String content, final boolean exists) {
                    giveTheGlassTo(new TtyEditor(path, content, CommandPromptScreen.this.flavour,
                            CommandPromptScreen.this.terminalHost));
                    CommandPromptScreen.this.editor.opened(exists);
                }
            };

    /** How the editor being opened reads a keyboard, set just before the file is asked for. */
    private TtyEditor.IKeys flavour;

    /** What an editor running here can ask this terminal to do for it. */
    private final TtyEditor.IHost terminalHost =
            new TtyEditorWire(() -> this.menu.hostPos(), () -> giveTheGlassTo(null));

    /**
     * Gives the glass to an editor, or back to the prompt when there is none.
     *
     * <p>The prompt's own line goes away with it: a caret blinking under an editor's last row is a second
     * cursor on a screen that has one.
     */
    private void giveTheGlassTo(final TtyEditor to) {
        this.editor = to;
        if (this.input != null) {
            this.input.setVisible(to == null);
            this.input.setFocused(to == null);
        }
    }

    /** Whether an editor has this terminal. */
    public boolean editing() {
        return this.editor != null;
    }

    /** Hands this terminal to an editor on {@code path}, which the machine is asked for. */
    private void openEditor(final String path,
                            final TtyEditor.IKeys keys) {
        this.flavour = keys;
        CodeFileReplies.expectContent(this.opening, path);
        PacketDistributor.sendToServer(
                new RequestFileContentPayload(menu.hostPos(), path));
    }


    /**
     * The POSIX prompt for the moment before the machine has said what it is: the home directory, in the
     * installed shell's style. A live medium's is not guessed at, since each has a prompt of its own in
     * colours of its own and the machine says which a tick after the terminal opens.
     */
    private CliLine initialPosixPrompt() {
        if (menu.console().live()) {
            return new CliLine("", CliStyle.ACCENT);
        }
        return ConsoleIdentity.promptLineOf(menu.platform(), menu.shell(), menu.hostname(), "~");
    }

    /**
     * What stands on the glass before what is typed, a run at a time.
     *
     * <p>A tool in front has the glass. When it has stopped to ask, its question stands where the prompt
     * would; while it is simply working there is nothing there at all, as at a real terminal. Otherwise it is
     * the shell's prompt: in the shell's own colours when the machine sent it that way, and in the
     * terminal's one colour for a prompt when all it sent was the words.
     */
    private CliLine before() {
        if (keyboard.asking()) {
            return keyboard.standing().toLine();
        }
        if (keyboard.busy()) {
            return CliLine.plain("");
        }
        // A console of a machine's own shows that machine's prompt; anything else shows the mod's.
        if (promptLine != null && (menu.posixShell() || dosStyle() || netStyle())) {
            return promptLine;
        }
        /*
         * The server has not said yet what the prompt is, which is true for the tick between the window
         * opening and the machine answering. Each family stands at its own until then rather than at a
         * drive letter no flat disk has.
         */
        final boolean unsaid = dosPrompt.equals("C:\\>");
        final String words;
        if (menu.posixShell() && unsaid) {
            return initialPosixPrompt();
        }
        if (menu.posixShell()) {
            words = dosPrompt;
        } else if (dosStyle()) {
            words = dosPrompt;
        } else if (netStyle()) {
            words = unsaid ? NET_PROMPT : dosPrompt;
        } else {
            words = "jsc>";
        }
        return new CliLine(words, CliStyle.ACCENT);
    }

    @Override
    protected HardwareEra screenEra() {
        /*
         * Resolve the host computer's era from its block entity on the client so the first frame already wears
         * the right era skin; the synced era slot lagged a tick and flashed the default era on open. Fall back
         * to the synced value when the host isn't client-loaded.
         */
        final Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.getBlockEntity(menu.hostPos())
                instanceof AbstractComputerBlockEntity host) {
            return host.displayEra();
        }
        return menu.hardwareEra();
    }
}
