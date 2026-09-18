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
import dev.jstech.computers.gui.term.TermBuffer;
import dev.jstech.computers.gui.term.TermRow;
import dev.jstech.computers.operation.payload.TerminalKeyboard;
import dev.jstech.computers.operation.payload.WireLine;
import dev.jstech.computers.operation.payload.program.DesktopShellPayloads;
import dev.jstech.computers.operation.payload.program.TerminalTools;
import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.cli.CliStyle;
import dev.jstech.core.client.gui.theme.JsTechTheme;
import dev.jstech.core.gui.Phosphor;
import dev.jstech.core.tier.HardwareEra;
import java.util.HashMap;
import java.util.Set;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
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
public class CommandPromptScreen<M extends CommandPromptMenu> extends AbstractComputerScreen<M> {

    private static final int CONSOLE = 0xFF070A0E;
    private static final int MAX_SCROLLBACK = 512;

    /** The largest the text is drawn; a glass too narrow for its columns at this size draws it smaller. */
    private static final float TEXT_SCALE = 0.85f;
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
    private final List<String> history = new ArrayList<>();
    private final List<String> commandNames = new ArrayList<>();
    private final Map<String, String> commandUsage = new LinkedHashMap<>();
    private final List<String> deviceNames = new ArrayList<>();

    /** The verbs whose first argument is a device, so Tab completes {@code /dev/sdX} for them. */
    private static final Set<String> DEVICE_VERBS =
            Set.of("mkfs.ext4", "mkfs", "mount", "grub-install");
    private int historyIndex = -1;
    private int scrollOffset;
    private int completionCycle;
    private boolean programmaticEdit;

    private EditBox input;

    /** The DOS prompt, synced from the server after each command so it tracks the current directory. */
    private String dosPrompt = "C:\\>";

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
     * A bare terminal draws no window chrome, just the glass and the text, the way a real console
     * fills its display. The MC-DOS terminal and the Linux TTY override this; the MC-NET Command
     * Prompt keeps its program window.
     */
    protected boolean bareTerminal() {
        return false;
    }

    @Override
    protected void init() {
        /*
         * A console fills the same glass the firmware, the self-test and the installers do, rather than the
         * larger one the graphical desktops use. It is the machine talking, not a desktop, and it is the whole
         * of what a terminal-only system ever shows: on a machine of the earliest age, whose monitor has the
         * thickest shell of any of them, the larger glass left too little of the window beside it for the
         * recipe viewer to put its list in, so the list went away on exactly the machines that show a console.
         */
        this.imageWidth = Math.min(this.width - 44, 340);
        this.imageHeight = Math.min(this.height - 60, 214);
        /*
         * The glass always has the same columns, so on a window too small to hold them at the usual size the
         * text is drawn smaller instead of the glass losing columns: what the machine laid out for that many
         * cells stays laid out.
         */
        this.textScale = Math.min(TEXT_SCALE,
                (this.imageWidth - 20) / (float) (TermBuffer.MONITOR_COLUMNS * TermPainter.CELL));
        super.init();
        // Start the input box just past the "jsc> " prompt so the caret never sits on top of it.
        final int promptW = font.width(prompt() + " ");
        input = new EditBox(font, leftPos + 10 + promptW, topPos + imageHeight - 18,
                imageWidth - 18 - promptW, 11, Component.literal("command"));
        input.setBordered(false);
        input.setMaxLength(RunCommandPayload.MAX_LEN);
        /*
         * Use the era's primary text color: dark for light-panel eras (Legacy), light for dark-panel
         * eras (Standard, Vintage). The input strip adopts the era's panel background, so the text
         * must track the era, since a fixed light color disappears on Legacy's cream panel.
         * A bare console is always dark glass, so its input is always light; the windowed prompt tracks
         * the era theme (dark text on Legacy's cream panel, light on the dark eras).
         */
        input.setTextColor(colorOf(CliStyle.PROMPT));
        input.setFocused(true);
        // A real edit (typing/backspace) restarts Tab cycling; our own programmatic setValue does not.
        input.setResponder(s -> {
            if (!programmaticEdit) {
                completionCycle = 0;
            }
        });
        setInitialFocus(input);
        addRenderableWidget(input);
        if (scrollback.isEmpty()) {
            /*
             * Opening a terminal starts a session, and a session announces itself. Restoring the old
             * scrollback used to drop the player mid-conversation with no sign the program had just
             * been opened; the command history (arrow keys) still persists, which is the part worth
             * keeping; a fresh terminal emulator behaves exactly like this.
             */
            if (menu.shellId().equals("live")) {
                // A booted installer medium: the live ISO's banner, already logged in as root.
                push(menu.osLabel() + " installation medium (tty1)", CliStyle.ACCENT);
                push("", CliStyle.PLAIN);
                push(menu.hostname() + " login: root (automatic login)", CliStyle.PLAIN);
                push("Type 'help' for the installation walkthrough.", CliStyle.DIM);
                push("", CliStyle.PLAIN);
            } else if (menu.posixShell()) {
                // A Linux TTY: the getty banner and an automatic login, then the shell greeting.
                push(menu.osLabel() + " " + menu.hostname() + " tty1", CliStyle.ACCENT);
                push("", CliStyle.PLAIN);
                push(menu.hostname() + " login: player", CliStyle.PLAIN);
                push("Password:", CliStyle.PLAIN);
                push("Welcome to " + menu.osLabel() + " (Linux 6.8-jsc x86_64)", CliStyle.DIM);
                push("", CliStyle.PLAIN);
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
            } else {
                push(Branding.houseOf(menu.osLabel()).name()
                        + " Shell v1.0", CliStyle.ACCENT);
                push("type 'help' for commands, TAB to complete", CliStyle.DIM);
                push("", CliStyle.PLAIN);
            }
        }
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
        history.clear();
        history.addAll(payload.history());
        historyIndex = -1;
        commandNames.clear();
        commandUsage.clear();
        for (final ConsoleInitPayload.WireCommand command : payload.commands()) {
            commandNames.add(command.name());
            commandUsage.put(command.name(), command.usage());
        }
        deviceNames.clear();
        deviceNames.addAll(payload.devices());
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
        if (input != null) {
            /*
             * An answer asked for unseen is typed and never drawn, not even as dots, which is how the tools
             * that ask for one have always taken it.
             */
            input.setFormatter(keyboard.asking() && keyboard.unseen()
                    ? (text, at) -> FormattedCharSequence.EMPTY
                    : (text, at) -> FormattedCharSequence.forward(text, Style.EMPTY));
            reflowInput();
        }
        // An empty prompt means "unchanged"; otherwise track the new current directory.
        if (!payload.prompt().isEmpty()) {
            dosPrompt = payload.prompt();
            reflowInput();
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

    /** Repositions the input box after the DOS prompt width changes (e.g. after a {@code cd}). */
    private void reflowInput() {
        if (input == null) {
            return;
        }
        final int promptW = font.width(prompt() + " ");
        input.setX(leftPos + 10 + promptW);
        input.setWidth(Math.max(20, imageWidth - 18 - promptW));
    }

    /** The console's scrollback, oldest first, what the player can read on the prompt right now. */
    public List<String> scrollbackText() {
        final List<String> lines = new ArrayList<>(scrollback.rows().size());
        for (final TermRow row : scrollback.rows()) {
            lines.add(row.text());
        }
        return lines;
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
        historyIndex = -1;
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
        push(prompt() + " " + line, CliStyle.PROMPT);
        if (line.isEmpty()) {
            return;
        }
        if (history.isEmpty() || !history.get(history.size() - 1).equals(line)) {
            history.add(line);
        }
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
            g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF000000);
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
        final int top = scrollbackTop();
        final int bottom = imageHeight - 23;
        final int visible = visibleRows();
        final List<TermRow> all = scrollback.rows();
        final int end = Math.max(0, all.size() - scrollOffset);
        final int start = Math.max(0, end - visible);
        /*
         * Drawn in the glass's own scale, so a row's pitch and a cell's width are the same whole numbers
         * whatever size the text comes out at, and the whole glass goes to the card in one batch.
         */
        g.pose().pushPose();
        g.pose().translate(10, top, 0);
        g.pose().scale(textScale, textScale, 1.0f);
        painter.draw(g, font, all.subList(start, end), 0, 0, rowPitch(), this::colorOf);
        g.pose().popPose();
        if (scrollOffset > 0) {
            JsTechTheme.textSRight(g, font, "scrolled +" + scrollOffset, imageWidth - 10, bottom - 7, JsTechTheme.dim());
        }

        // Prompt glyph before the input box.
        JsTechTheme.text(g, font, prompt(), 10, imageHeight - 18, JsTechTheme.accent());

        // Usage hint: once the verb is recognised, show how it is used, dimmed on the right.
        final String typed = input == null ? "" : input.getValue().trim();
        final int space = typed.indexOf(' ');
        final String verb = (space < 0 ? typed : typed.substring(0, space)).toLowerCase(Locale.ROOT);
        final String usage = commandUsage.get(verb);
        if (usage != null && !usage.isEmpty()) {
            /*
             * The hint shares the input line: it gets the room to the right of what is typed, and is cut
             * short rather than drawn over the prompt when a long usage does not fit.
             */
            final int promptW = font.width(prompt() + " ");
            final int typedW = font.width(input == null ? "" : input.getValue());
            final int room = imageWidth - 10 - (10 + promptW + typedW + 12);
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

        if (this.editor != null) {
            // An editor lists its own keys on its own glass, and none of the prompt's mean anything to it.
            return;
        }
        JsTechTheme.textS(g, font, keyboard.busy()
                        ? "CTRL+C interrupt    wheel scroll    ESC close"
                        : "ENTER run    UP/DOWN history    wheel scroll    ESC close",
                10, imageHeight - 7, JsTechTheme.dim());
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

    private int colorOf(final CliStyle style) {
        /*
         * A Vintage machine draws on a green-phosphor tube, which has ONE colour: every style comes out
         * as that green, brighter or dimmer, so an error still reads as an error without being red.
         */
        final int color = TermPalette.colorOf(style);
        return screenEra() == HardwareEra.VINTAGE ? Phosphor.green(color) : color;
    }

    // input

    @Override
    public boolean keyPressed(final int key, final int scan, final int mods) {
        // While an editor has the terminal every key is its, including the ones that would leave.
        if (this.editor != null) {
            this.editor.keyPressed(key, mods);
            return true;
        }
        if (key == 257 || key == 335) { // Enter / numpad Enter
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

    /**
     * Completes the command word the player is typing against the known command names, cycling
     * through the matches on repeated Tab. Only the first word (the verb) is completed for now.
     */
    private void complete() {
        final String text = input.getValue();
        if (text.isEmpty()) {
            return;
        }
        if (text.contains(" ")) {
            completeDevice(text);
            return; // other arguments are not completed yet; only the leading command word
        }
        final String prefix = text.toLowerCase(Locale.ROOT);
        final List<String> matches = new ArrayList<>();
        for (final String name : commandNames) {
            if (name.startsWith(prefix)) {
                matches.add(name);
            }
        }
        if (matches.isEmpty()) {
            return;
        }
        final String pick = matches.get(completionCycle % matches.size());
        completionCycle++;
        programmaticEdit = true;
        input.setValue(matches.size() == 1 ? pick + " " : pick);
        input.moveCursorToEnd(false);
        programmaticEdit = false;
    }

    /**
     * Completes a {@code /dev/<device>} first argument for the device verbs (mkfs.ext4, mount,
     * grub-install), cycling through the drives the server reported, so the Arch/Gentoo install never
     * needs the device names typed out by hand.
     */
    private void completeDevice(final String text) {
        final int space = text.indexOf(' ');
        final String verb = text.substring(0, space).toLowerCase(Locale.ROOT);
        final String arg = text.substring(space + 1);
        if (!DEVICE_VERBS.contains(verb) || deviceNames.isEmpty() || arg.contains(" ")) {
            return; // only the first argument of a device verb is completed
        }
        final String argPrefix = arg.toLowerCase(Locale.ROOT);
        final List<String> matches = new ArrayList<>();
        for (final String device : deviceNames) {
            final String full = "/dev/" + device;
            if (full.startsWith(argPrefix) || device.startsWith(argPrefix)) {
                matches.add(full);
            }
        }
        if (matches.isEmpty()) {
            return;
        }
        final String pick = matches.get(completionCycle % matches.size());
        completionCycle++;
        programmaticEdit = true;
        input.setValue(text.substring(0, space + 1) + pick + (matches.size() == 1 ? " " : ""));
        input.moveCursorToEnd(false);
        programmaticEdit = false;
    }

    private void recallHistory(final int direction) {
        if (history.isEmpty()) {
            return;
        }
        if (historyIndex == -1) {
            historyIndex = history.size();
        }
        historyIndex = Math.max(0, Math.min(history.size(), historyIndex + direction));
        if (historyIndex >= history.size()) {
            historyIndex = -1;
            input.setValue("");
        } else {
            input.setValue(history.get(historyIndex));
        }
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
        return (int) ((imageHeight - 23 - scrollbackTop()) / (rowPitch() * textScale));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        /*
         * Keep the input color in sync if a board swap changes the era while the screen is open. This
         * must repeat the bare-console rule, not just take the era's text colour: a bare terminal is
         * dark glass, and on Legacy the era text is dark for its cream panel, and writing that here left
         * the player typing near-black on black.
         */
        if (input != null) {
            input.setTextColor(colorOf(CliStyle.PROMPT));
        }
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
            this.editor.render(g, font, leftPos + 8, topPos + 8, imageWidth - 16, imageHeight - 16,
                    InkPalette.DARK);
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


    /** The initial POSIX prompt (home directory) in the installed shell's style, before the server syncs one. */
    private String initialPosixPrompt() {
        if (menu.shellId().equals("live")) {
            return "root@" + menu.hostname() + " ~ #";
        }
        return menu.shellId().equals("zsh")
                ? "player@" + menu.hostname() + " ~ %"
                : "player@" + menu.hostname() + ":~$";
    }

    /** The shell prompt: the server-synced prompt for MC-DOS and Linux (drive/directory aware), the jsc prompt otherwise. */
    private String prompt() {
        /*
         * A tool in front has the glass. When it has stopped to ask, its question stands where the prompt
         * would; while it is simply working there is nothing there at all, as at a real terminal.
         */
        if (keyboard.asking()) {
            return keyboard.question().text().stripTrailing();
        }
        if (keyboard.busy()) {
            return "";
        }
        if (menu.posixShell()) {
            return dosPrompt.equals("C:\\>") ? initialPosixPrompt() : dosPrompt;
        }
        return dosStyle() ? dosPrompt : "jsc>";
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
