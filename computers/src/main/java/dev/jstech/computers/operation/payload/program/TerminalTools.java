/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.program;

import dev.jstech.computers.gui.term.TermBuffer;
import dev.jstech.computers.operation.payload.TerminalKeyboard;
import dev.jstech.computers.operation.payload.WireLine;
import dev.jstech.computers.operation.payload.WireSink;
import dev.jstech.computers.program.ComputerConsoleState;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.TerminalForeground;
import dev.jstech.computers.program.cli.CliCommands;
import dev.jstech.computers.program.tty.ITtyProcess;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * What a terminal does about a tool running in front of it, whichever terminal it is.
 *
 * <p>A machine has one console and two ways of looking at it, the prompt that is the whole glass and the
 * window on a desktop, and each has a handler of its own for what is typed. What happens to a line typed while
 * a tool is in front is not theirs to decide separately: it goes to the tool if the tool asked, it stops the
 * tool if it is Ctrl+C, and otherwise it goes nowhere, exactly as at a real terminal. So that is decided here,
 * once, and both handlers send what comes of it in their own packet.
 */
public final class TerminalTools {

    /**
     * Enter on an empty line, sent as a character of its own.
     *
     * <p>An empty line already means something else: it is what a terminal sends when it opens, to be told
     * where things stand without having typed anything. At a question, Enter by itself is an answer, the one
     * that takes the default, and it has to be told apart from a window that has just been opened or that
     * window would answer the question for the player.
     */
    public static final String ENTER = String.valueOf((char) 0x0D);

    private TerminalTools() {
    }

    /**
     * What came of one turn at a tool: what it printed, and who has the keyboard now.
     *
     * @param ended whether the tool ended on this turn, which is when the prompt is worth sending again
     */
    public record Turn(List<WireLine> lines, TerminalKeyboard keyboard, boolean ended) {
    }

    /**
     * Hands a typed line to the tool in front of that machine's terminal.
     *
     * @return what came of it, or null when no tool is in front and the line is the shell's
     */
    @Nullable
    public static Turn typed(final IComputerTerminalHost host, final ServerLevel level, final String line) {
        final ComputerConsoleState console = host.console();
        if (console == null || !console.foreground().running()) {
            return null;
        }
        final TerminalForeground front = console.foreground();
        final long now = level.getGameTime();
        front.findAgain(typed -> remake(host, level, typed), now);
        if (front.tool() == null) {
            return null;
        }
        final WireSink out = new WireSink();
        boolean ended = false;
        if (DesktopShellPayloads.INTERRUPT.equals(line)) {
            front.interrupt(now, out);
            ended = true;
        } else if (front.tool().asking() != null && !line.isEmpty()) {
            ended = front.answer(ENTER.equals(line) ? "" : line, front.tool().asking().masked(), now, out);
        }
        changed(host);
        return new Turn(out.lines(), keyboardOf(console), ended);
    }

    /**
     * Puts the tool a command left running in front of that machine's terminal, and plays its opening.
     *
     * @param line what was typed to start it, which is how it is found again after a load
     */
    public static Turn started(final IComputerTerminalHost host, final ServerLevel level, final String line,
                               final ITtyProcess tool) {
        final ComputerConsoleState console = host.console();
        if (console == null) {
            return new Turn(List.of(), TerminalKeyboard.PROMPT, true);
        }
        final long now = level.getGameTime();
        final WireSink out = new WireSink();
        console.foreground().begin(tool, line, now);
        final boolean ended = console.foreground().advance(now, out);
        changed(host);
        return new Turn(out.lines(), keyboardOf(console), ended);
    }

    /** Who has the keyboard at that console right now. */
    public static TerminalKeyboard keyboardOf(@Nullable final ComputerConsoleState console) {
        final ITtyProcess tool = console == null ? null : console.foreground().tool();
        return tool == null ? TerminalKeyboard.PROMPT : TerminalKeyboard.heldBy(tool.asking());
    }

    /**
     * Makes again the tool a line started, by typing the line again where nobody can see.
     *
     * <p>Safe because a tool does what it was run for only at its end: the machine is as it was when the line
     * was first typed, so the same line makes the same tool.
     */
    @Nullable
    public static ITtyProcess remake(final IComputerTerminalHost host, final ServerLevel level, final String line) {
        final ServerCliComputer computer = new ServerCliComputer(host, level);
        return CliCommands.shellFor(computer, TermBuffer.MONITOR_COLUMNS).run(line, computer).started();
    }

    private static void changed(final IComputerTerminalHost host) {
        if (host instanceof BlockEntity block) {
            block.setChanged();
        }
    }
}
