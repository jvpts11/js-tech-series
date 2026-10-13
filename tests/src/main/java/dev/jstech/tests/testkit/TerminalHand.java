/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.testkit;

import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.os.install.SetupRunner;
import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.tty.ITtySink;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import net.minecraft.server.level.ServerLevel;

/**
 * A player at a machine's terminal who waits for each line to finish: whatever a line leaves running in front of the
 * terminal is moved along on the machine's clock, a tick at a time and all within one tick of the world, until the
 * prompt comes back or the tool stops to ask something. A package manager that hands its work to the machine's setup
 * is moved along the same way. What would take a world minutes of real ticks takes a test one, so a test can type a
 * whole installation or a whole tree of builds in many configurations.
 */
public final class TerminalHand {

    private final AbstractComputerBlockEntity machine;
    private final ServerLevel level;
    private final TerminalAt term;
    /** Every line typed and what it said, for a test to show where a session went wrong. */
    private final StringBuilder transcript = new StringBuilder();
    private long clock;

    /** How many seconds of a tool's work are moved along before the hand gives up on it. */
    private static final int MOST_SECONDS = 20_000;
    private static final int SECOND = 20;

    public <T extends AbstractComputerBlockEntity & IComputerTerminalHost> TerminalHand(final T machine,
            final ServerLevel level) {
        this.machine = machine;
        this.level = level;
        this.term = new TerminalAt(machine, level);
        this.clock = level.getGameTime();
    }

    /** The machine whose terminal this is. */
    public AbstractComputerBlockEntity machine() {
        return this.machine;
    }

    /** Types the line, and gives back what it said with whatever the tool it left running said after. */
    public String type(final String line) {
        final StringBuilder said = new StringBuilder(this.term.type(line));
        this.clock = Math.max(this.clock, this.level.getGameTime());
        final ITtySink sink = new ITtySink() {
            @Override
            public void line(final CliLine printed) {
                said.append(printed.text()).append('\n');
            }

            @Override
            public void redraw(final CliLine printed) {
                said.append(printed.text()).append('\n');
            }
        };
        // A tick at a time, as the machine's own tick moves it, so no line a tool draws on the way is skipped.
        for (int i = 0; i < MOST_SECONDS * SECOND && this.term.busy() && this.term.asking().isEmpty(); i++) {
            this.clock++;
            this.machine.console().foreground().advance(this.clock, sink);
        }
        /*
         * A package manager that hands the work to the machine's own setup, rather than keeping the terminal, is
         * moved along the same way: its ticks run one after another until the setup is done.
         */
        for (int i = 0; i < MOST_SECONDS * SECOND && this.machine.console().setup() != null; i++) {
            SetupRunner.tick(this.machine, this.level, this.machine.getBlockPos());
        }
        this.transcript.append("> ").append(line).append('\n').append(said);
        return said.toString();
    }

    /** The last of what was typed at this terminal and what it said, as much as the given number of characters. */
    public String transcript(final int most) {
        return this.transcript.length() <= most ? this.transcript.toString()
                : this.transcript.substring(this.transcript.length() - most);
    }

    /** Types the line and gives back only what it said at once, leaving whatever it started where it is. */
    public String glance(final String line) {
        final String said = this.term.type(line);
        this.transcript.append("> ").append(line).append('\n').append(said);
        return said;
    }

    /** Types a line whose tool asks before it does anything, and says yes to it; gives back all it said. */
    public String yes(final String line) {
        final String said = type(line);
        return this.term.asking().isEmpty() ? said : said + type("y");
    }

    /** Whether a tool still has the terminal. */
    public boolean busy() {
        return this.term.busy();
    }

    /** What the tool in front has stopped to ask, or empty when nothing is asking. */
    public String asking() {
        return this.term.asking();
    }

    /** The shell's own prompt, which says where the session is standing. */
    public String prompt() {
        return this.term.prompt();
    }
}
