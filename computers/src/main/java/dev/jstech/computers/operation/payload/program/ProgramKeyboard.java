/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.program;

import dev.jstech.computers.machine.IMachineRuntime;
import dev.jstech.computers.machine.MachinePrograms;
import dev.jstech.computers.operation.payload.TerminalKeyboard;
import dev.jstech.computers.operation.payload.WireLine;
import dev.jstech.computers.program.cli.CliStyle;
import org.jetbrains.annotations.Nullable;

/**
 * Who has the keyboard while a program holds a terminal, and what stands before what is typed.
 *
 * <p>A program waiting for a line asks with whatever it left open, the question it printed without ending the line,
 * and the answer is typed after it on the same line; the machine writes the two down together as the line is typed,
 * so a terminal draws nothing of its own. A program that is simply working has no question: a desktop's terminal
 * still takes what is typed ahead for it, and a machine's own prompt, which has nothing else to show, shows nothing.
 */
public final class ProgramKeyboard {

    private ProgramKeyboard() {
    }

    /** The keyboard of a desktop's terminal window: the question the program is asking, or the program's to type at. */
    public static TerminalKeyboard onDesktop(final IMachineRuntime held) {
        final TerminalKeyboard asking = asking(held);
        return asking == null ? TerminalKeyboard.PROMPT : asking;
    }

    /** The keyboard of a machine's own prompt: the question the program is asking, or nothing to type at but Ctrl+C. */
    public static TerminalKeyboard atTerminal(final IMachineRuntime held) {
        final TerminalKeyboard asking = asking(held);
        return asking == null ? TerminalKeyboard.heldBy(null) : asking;
    }

    /** The program holding that machine's terminal, or null when the prompt has it. */
    @Nullable
    public static IMachineRuntime heldBy(@Nullable final MachinePrograms programs) {
        if (programs == null || programs.held() == 0) {
            return null;
        }
        final var one = programs.byId(programs.held());
        return one == null ? null : one.process();
    }

    @Nullable
    private static TerminalKeyboard asking(final IMachineRuntime held) {
        if (!held.waitingForInput()) {
            return null;
        }
        return new TerminalKeyboard(true, new WireLine(held.openLine(), CliStyle.PLAIN.id()), false);
    }
}
