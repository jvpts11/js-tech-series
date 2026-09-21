/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.computers.menu.DesktopMenu;
import dev.jstech.computers.operation.payload.CommandOutputPayload;
import dev.jstech.computers.operation.payload.DesktopShellOutputPayload;
import dev.jstech.computers.operation.payload.TerminalKeyboard;
import dev.jstech.computers.operation.payload.WireSink;
import dev.jstech.computers.operation.payload.program.TerminalTools;
import dev.jstech.computers.program.ComputerConsoleState;
import dev.jstech.computers.program.TerminalForeground;
import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.tty.ITtyProcess;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Moves the tool in front of a machine's terminal along, once a tick, and sends what it printed to whoever
 * is looking.
 *
 * <p>The tool runs whether or not anybody is there, because a compile does not stop when the player walks
 * away from it. What changes is only that with nobody there it is told there is nowhere to print, and makes
 * none of the lines it would have printed: a machine unpacking an archive in an empty room costs a comparison
 * a tick, not eight hundred lines a second.
 */
final class TerminalFeed {

    private final IFedTerminal terminal;

    /** Whether the question the tool is standing at has been sent, so it goes out once and not every tick. */
    private boolean asked;

    TerminalFeed(final IFedTerminal terminal) {
        this.terminal = terminal;
    }

    /** The feed of a computer, which has the one terminal. */
    static TerminalFeed of(final AbstractComputerBlockEntity machine) {
        return new TerminalFeed(new IFedTerminal() {
            @Override
            public ComputerConsoleState console() {
                return machine.console();
            }

            @Override
            public List<ServerPlayer> watching(final ServerLevel level) {
                return machine.consoleViewers(level);
            }

            @Override
            public CliLine prompt() {
                return machine.shellPromptLine();
            }

            @Override
            public ITtyProcess remake(final ServerLevel level, final String line) {
                return machine instanceof IComputerTerminalHost host ? TerminalTools.remake(host, level, line) : null;
            }

            @Override
            public void changed() {
                machine.setChanged();
            }
        });
    }

    void tick(final ServerLevel level) {
        final ComputerConsoleState console = this.terminal.console();
        if (console == null || !console.foreground().running()) {
            this.asked = false;
            return;
        }
        final TerminalForeground front = console.foreground();
        final long now = level.getGameTime();
        front.findAgain(line -> this.terminal.remake(level, line), now);
        if (front.tool() == null) {
            return;
        }
        final List<ServerPlayer> viewers = this.terminal.watching(level);
        final WireSink out = viewers.isEmpty() ? null : new WireSink();
        final boolean ended = front.advance(now, out);
        if (ended) {
            this.terminal.changed();
        }
        if (out == null) {
            return;
        }
        final TerminalKeyboard held = TerminalTools.keyboardOf(console);
        final boolean asking = held.asking();
        if (out.isEmpty() && !ended && asking == this.asked) {
            return;
        }
        this.asked = asking;
        /* A tool that has just ended gives the prompt back, named a run at a time so it comes back in colour. */
        final CliLine shown = ended ? this.terminal.prompt() : null;
        final String prompt = shown == null ? "" : shown.text();
        final TerminalKeyboard keyboard = shown == null ? held : TerminalKeyboard.atPrompt(shown);
        for (final ServerPlayer viewer : viewers) {
            PacketDistributor.sendToPlayer(viewer, viewer.containerMenu instanceof DesktopMenu
                    ? new DesktopShellOutputPayload(prompt, out.lines(), keyboard)
                    : new CommandOutputPayload(prompt, out.lines(), keyboard));
        }
    }
}
