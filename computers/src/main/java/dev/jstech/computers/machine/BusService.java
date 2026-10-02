/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.block.part.AbstractBusPart;
import dev.jstech.computers.block.part.NamedBus;
import dev.jstech.computers.program.IqlBusSetter;
import dev.jstech.computers.program.IqlEngine;
import dev.jstech.computers.program.iql.IqlBusStatement;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

import java.util.List;

/**
 * The buses of the machine's network as its programs reach them: found by name, and set the way the IQL sets them,
 * through the same door, so a program can set on a bus exactly what a statement can, and is refused the same.
 */
public final class BusService {

    private final IComputerTerminalHost terminal;
    private final ServerLevel level;

    public BusService(final IComputerTerminalHost terminal, final ServerLevel level) {
        this.terminal = terminal;
        this.level = level;
    }

    /** Whether a bus of that name is on the machine's network. */
    public boolean exists(final String name) {
        return NamedBus.find(level, terminal.networkUuid(), name) != null;
    }

    /** Sets {@code change} on the bus named {@code name}, marked with {@code by}: what the IQL would answer. */
    public IqlEngine.Outcome set(final String name, final IqlBusStatement.Change change, final String by) {
        return IqlBusSetter.apply(level, terminal.networkUuid(), new IqlBusStatement(name, change), by);
    }

    /** The tags the bus named {@code name} lists now, none when there is no such bus. */
    public List<String> tags(final String name) {
        final NamedBus.Located at = NamedBus.find(level, terminal.networkUuid(), name);
        if (at == null || !(at.cable().getPart(at.face()) instanceof AbstractBusPart bus)) {
            return List.of();
        }
        return bus.tags().stream().map(ResourceLocation::toString).toList();
    }
}
