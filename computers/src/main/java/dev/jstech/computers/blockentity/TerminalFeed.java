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

    private final AbstractComputerBlockEntity machine;

    /** Whether the question the tool is standing at has been sent, so it goes out once and not every tick. */
    private boolean asked;

    TerminalFeed(final AbstractComputerBlockEntity machine) {
        this.machine = machine;
    }

    void tick(final ServerLevel level) {
        final ComputerConsoleState console = this.machine.console();
        if (console == null || !console.foreground().running()) {
            this.asked = false;
            return;
        }
        final TerminalForeground front = console.foreground();
        final long now = level.getGameTime();
        front.findAgain(line -> this.machine instanceof IComputerTerminalHost host
                ? TerminalTools.remake(host, level, line) : null, now);
        if (front.tool() == null) {
            return;
        }
        final List<ServerPlayer> viewers = this.machine.consoleViewers(level);
        final WireSink out = viewers.isEmpty() ? null : new WireSink();
        final boolean ended = front.advance(now, out);
        if (ended) {
            this.machine.setChanged();
        }
        if (out == null) {
            return;
        }
        final TerminalKeyboard keyboard = TerminalTools.keyboardOf(console);
        final boolean asking = keyboard.asking();
        if (out.isEmpty() && !ended && asking == this.asked) {
            return;
        }
        this.asked = asking;
        final String prompt = ended ? this.machine.shellPrompt() : "";
        for (final ServerPlayer viewer : viewers) {
            PacketDistributor.sendToPlayer(viewer, viewer.containerMenu instanceof DesktopMenu
                    ? new DesktopShellOutputPayload(prompt, out.lines(), keyboard)
                    : new CommandOutputPayload(prompt, out.lines(), keyboard));
        }
    }
}
