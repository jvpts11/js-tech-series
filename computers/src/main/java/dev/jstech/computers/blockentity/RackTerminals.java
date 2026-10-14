/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.computers.machine.MachinePrograms;
import dev.jstech.computers.operation.payload.program.TerminalTools;
import dev.jstech.computers.program.ComputerConsoleState;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.tty.ITtyProcess;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * The terminals of the servers mounted in one rack: who is looking at the rack's glass, and a feed for every
 * server that keeps the tool in front of its terminal running.
 *
 * <p>A rack shows one server at a time, the one its switch is turned to, so that is the only one anybody
 * watching can be watching. The others run what they were left running all the same: a server told to build
 * something goes on building it after the switch is turned to its neighbour.
 */
final class RackTerminals {

    private final ServerRackBlockEntity rack;
    private final Viewers viewers;
    private final Map<Integer, TerminalFeed> feeds = new HashMap<>();
    /** What each server's programs print in front of its terminal, sent to whoever is watching that server. */
    private final Map<Integer, ProgramFeed> programFeeds = new HashMap<>();

    RackTerminals(final ServerRackBlockEntity rack) {
        this.rack = rack;
        this.viewers = new Viewers(rack.getBlockPos());
    }

    /** One tick of the terminal of the server mounted at that row. */
    void tick(final ServerLevel level, final int row) {
        this.feeds.computeIfAbsent(row, at -> new TerminalFeed(new Unit(at))).tick(level);
    }

    /** The programs of the server at that row ran a tick: what the one in front of its terminal printed is sent. */
    void programsRan(final ServerLevel level, final int row) {
        this.programFeed(row).push(level);
    }

    /** The program in front of the terminal of the server at that row was ended from outside it, and is said so. */
    void frontKilled(final ServerLevel level, final int row) {
        this.programFeed(row).frontKilled(level);
    }

    void opened(final ServerPlayer viewer) {
        this.viewers.opened(viewer);
    }

    void closed(final ServerPlayer viewer) {
        this.viewers.closed(viewer);
    }

    /** Who is at the rack's glass looking at the server at that row: nobody while its switch is turned elsewhere. */
    List<ServerPlayer> watching(final ServerLevel level, final int row) {
        return this.rack.shownRow() == row ? this.viewers.at(level) : List.of();
    }

    private ProgramFeed programFeed(final int row) {
        return this.programFeeds.computeIfAbsent(row, at -> new ProgramFeed(new Unit(at)));
    }

    /** The terminal of the server at one row, as its feed sees it. */
    private final class Unit implements IFedTerminal {

        private final int row;

        Unit(final int row) {
            this.row = row;
        }

        @Override
        public ComputerConsoleState console() {
            return RackTerminals.this.rack.consoleOf(this.row);
        }

        @Override
        public MachinePrograms programs() {
            final ServerRackBlockEntity rack = RackTerminals.this.rack;
            return rack.asUnit(this.row, rack::programs);
        }

        @Override
        public List<ServerPlayer> watching(final ServerLevel level) {
            // Whoever is at the rack is looking at the server its switch is turned to, and at no other.
            return RackTerminals.this.watching(level, this.row);
        }

        @Override
        public CliLine prompt() {
            final ServerRackBlockEntity rack = RackTerminals.this.rack;
            return rack.getLevel() instanceof ServerLevel level
                    ? rack.asUnit(this.row, () -> new ServerCliComputer(rack, level).promptLine())
                    : CliLine.plain("");
        }

        @Override
        public ITtyProcess remake(final ServerLevel level, final String line) {
            final ServerRackBlockEntity rack = RackTerminals.this.rack;
            return rack.asUnit(this.row, () -> TerminalTools.remake(rack, level, line));
        }

        @Override
        public void changed() {
            RackTerminals.this.rack.setChanged();
        }
    }
}
