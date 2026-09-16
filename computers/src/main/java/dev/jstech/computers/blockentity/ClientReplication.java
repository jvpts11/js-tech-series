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
import dev.jstech.computers.operation.payload.UiWindowPayload;
import dev.jstech.computers.program.cli.CliStyle;
import dev.jstech.core.language.ILanguageProcess;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * What a machine sends to the players watching it: the windows its programs have open, and what the
 * program in front has printed.
 */
final class ClientReplication {

    private final AbstractComputerBlockEntity machine;
    /* Every window of a program that has been sent, by the program and the window, as it was sent. */
    private final Map<Long, UiWindowPayload> sentWindows = new HashMap<>();

    ClientReplication(final AbstractComputerBlockEntity machine) {
        this.machine = machine;
    }

    /**
     * Sends the windows the programs on this machine have open to whoever is at its desktop.
     *
     * <p>A window goes over whole whenever anything in it changes, and once more, empty, when it closes.
     * A machine nobody is looking at sends nothing and forgets what it sent, so whoever opens the desktop
     * next is sent everything as it stands.
     */
    void pushWindows(final ServerLevel level) {
        final List<ServerPlayer> viewers = this.machine.consoleViewers(level);
        if (viewers.isEmpty()) {
            this.sentWindows.clear();
            return;
        }
        final BlockPos pos = this.machine.getBlockPos();
        final Map<Long, UiWindowPayload> open = new HashMap<>();
        this.machine.programs().eachWindow((window, program) -> {
            final var payload = UiWindowPayload.of(pos, program, window);
            if (payload != null) {
                open.put(key(payload.program(), payload.window()), payload);
            }
        });
        final List<UiWindowPayload> send = new ArrayList<>();
        for (final var entry : open.entrySet()) {
            if (!entry.getValue().equals(this.sentWindows.get(entry.getKey()))) {
                send.add(entry.getValue());
            }
        }
        for (final var entry : this.sentWindows.entrySet()) {
            if (!open.containsKey(entry.getKey())) {
                send.add(UiWindowPayload.gone(pos, entry.getValue().program(), entry.getValue().window()));
            }
        }
        this.sentWindows.clear();
        this.sentWindows.putAll(open);
        if (send.isEmpty()) {
            return;
        }
        for (final ServerPlayer viewer : viewers) {
            if (!(viewer.containerMenu instanceof DesktopMenu)) {
                continue;
            }
            for (final var payload : send) {
                PacketDistributor.sendToPlayer(viewer, payload);
            }
        }
    }

    /**
     * Sends what the program in front has printed to whoever is at this machine's terminal.
     *
     * <p>This is what makes a program at a terminal behave like one anywhere else: its lines appear as
     * it prints them rather than all at once when it is over, and the prompt comes back the moment it
     * returns. A program nobody is watching still runs; there is simply nowhere for its lines to go.
     */
    void pushOutput(final ServerLevel level) {
        final MachinePrograms programs = this.machine.programs();
        if (programs.held() == 0) {
            return;
        }
        final var one = programs.byId(programs.held());
        if (one == null) {
            programs.release();
            return;
        }
        final var state = one.process().state();
        final boolean over = !MachinePrograms.running(one.process());
        final List<String> fresh = programs.unseen();
        final String halt = over && state == ILanguageProcess.State.HALTED ? one.process().message() : null;
        if (over) {
            programs.release();
            this.machine.setChanged();
        }
        if (fresh.isEmpty() && halt == null && !over) {
            return;
        }
        final List<ServerPlayer> viewers = this.machine.consoleViewers(level);
        if (viewers.isEmpty()) {
            return;
        }
        final List<DesktopShellOutputPayload.WireLine> wire = new ArrayList<>();
        for (final String line : fresh) {
            wire.add(new DesktopShellOutputPayload.WireLine(line, CliStyle.PLAIN.id()));
        }
        if (halt != null) {
            wire.add(new DesktopShellOutputPayload.WireLine(halt, CliStyle.ERROR.id()));
        }
        final String prompt = over ? this.machine.shellPrompt() : "";
        final var desktop = new DesktopShellOutputPayload(false, !over, prompt, wire);
        /*
         * The same said twice, once in each terminal's own words: a window on a desktop, and the prompt
         * that is the whole glass of a machine that has none. Both are watching this one console.
         */
        final List<CommandOutputPayload.WireLine> promptWire = new ArrayList<>(wire.size());
        for (final var line : wire) {
            promptWire.add(new CommandOutputPayload.WireLine(line.text(), line.style()));
        }
        final var terminal = new CommandOutputPayload(false, prompt, promptWire);
        for (final ServerPlayer viewer : viewers) {
            PacketDistributor.sendToPlayer(viewer,
                    viewer.containerMenu instanceof DesktopMenu ? desktop : terminal);
        }
    }

    private static long key(final int program, final long window) {
        return ((long) program << 32) | (window & 0xFFFFFFFFL);
    }
}
