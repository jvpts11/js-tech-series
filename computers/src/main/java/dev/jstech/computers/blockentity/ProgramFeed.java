/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.computers.machine.MachinePrograms;
import dev.jstech.computers.menu.DesktopMenu;
import dev.jstech.computers.operation.payload.CommandOutputPayload;
import dev.jstech.computers.operation.payload.DesktopShellOutputPayload;
import dev.jstech.computers.operation.payload.TerminalKeyboard;
import dev.jstech.computers.operation.payload.WireLine;
import dev.jstech.computers.operation.payload.program.ProgramKeyboard;
import dev.jstech.computers.program.cli.CliStyle;
import dev.jstech.computers.api.language.ILanguageProcess;
import dev.jstech.core.text.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * Sends what the program in front of a machine's terminal prints to whoever is at that terminal.
 *
 * <p>This is what makes a program at a terminal behave like one anywhere else: its lines appear as it prints them
 * rather than all at once when it is over, and the prompt comes back the moment it returns. A program nobody is
 * watching still runs; there is simply nowhere for its lines to go. A computer has one terminal and so one feed; a
 * rack has one for every server in it.
 */
final class ProgramFeed {

    private final IFedTerminal terminal;
    /**
     * The question the program in front was last seen asking, with how much it had written by then, or null while
     * it asks nothing: a terminal is told only when that changes.
     */
    @Nullable
    private String asked;

    /** What a shell says of the program in front of it that was killed: the signal's own name. */
    private static final String KILLED = "Killed";

    ProgramFeed(final IFedTerminal terminal) {
        this.terminal = terminal;
    }

    /** Sends what the program in front has printed since it was last asked, and the prompt once it has returned. */
    void push(final ServerLevel level) {
        final MachinePrograms programs = this.terminal.programs();
        if (programs == null || programs.held() == 0) {
            this.asked = null;
            return;
        }
        final var one = programs.byId(programs.held());
        if (one == null) {
            programs.release();
            return;
        }
        final var state = one.process().state();
        final boolean over = !MachinePrograms.running(one.process());
        final List<Text> fresh = programs.unseen();
        final Text halt = over && state == ILanguageProcess.State.HALTED ? one.process().message() : null;
        /*
         * A program waiting for a line asks with what it left open. The question is sent when it changes: when the
         * program comes to ask, and again after every line typed, since each one is written down and so moves on.
         */
        final String asking = over || !one.process().waitingForInput() ? null
                : one.process().written() + "\n" + one.process().openLine();
        final boolean askedAnew = !Objects.equals(asking, this.asked);
        this.asked = asking;
        if (over) {
            programs.release();
            this.terminal.changed();
        }
        if (fresh.isEmpty() && halt == null && !over && !askedAnew) {
            return;
        }
        final List<ServerPlayer> viewers = this.terminal.watching(level);
        if (viewers.isEmpty()) {
            return;
        }
        final List<WireLine> wire = new ArrayList<>();
        for (final Text line : fresh) {
            wire.add(new WireLine(line, CliStyle.PLAIN.id()));
        }
        if (halt != null) {
            wire.add(new WireLine(halt, CliStyle.ERROR.id()));
        }
        if (over) {
            say(viewers, wire, this.terminal.prompt().text(), false, TerminalKeyboard.PROMPT, TerminalKeyboard.PROMPT);
        } else {
            say(viewers, wire, "", true, ProgramKeyboard.onDesktop(one.process()),
                    ProgramKeyboard.atTerminal(one.process()));
        }
    }

    /**
     * The program in front of the terminal was ended from outside it: the shell says so in its own word, as a shell
     * says it of a program killed under it, and gives the prompt back.
     */
    void frontKilled(final ServerLevel level) {
        this.asked = null;
        final List<ServerPlayer> viewers = this.terminal.watching(level);
        if (!viewers.isEmpty()) {
            say(viewers, List.of(new WireLine(Text.literal(KILLED), CliStyle.PLAIN.id())),
                    this.terminal.prompt().text(), false, TerminalKeyboard.PROMPT, TerminalKeyboard.PROMPT);
        }
    }

    /**
     * The same said twice, once in each terminal's own words: a window on a desktop, and the prompt that is
     * the whole glass of a machine that has none. Both are watching this one console, so each viewer is sent
     * the one its own screen speaks, with who has its keyboard.
     */
    private static void say(final List<ServerPlayer> viewers, final List<WireLine> wire, final String prompt,
                            final boolean holdsTerminal, final TerminalKeyboard onDesktop,
                            final TerminalKeyboard atTerminal) {
        final var desktop = new DesktopShellOutputPayload(false, holdsTerminal, prompt, wire, "", "", 0, false,
                false, onDesktop);
        final var terminal = new CommandOutputPayload(prompt, wire, atTerminal);
        for (final ServerPlayer viewer : viewers) {
            PacketDistributor.sendToPlayer(viewer,
                    viewer.containerMenu instanceof DesktopMenu ? desktop : terminal);
        }
    }
}
