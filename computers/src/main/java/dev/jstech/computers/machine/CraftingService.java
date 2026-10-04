/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.operation.payload.network.NetworkLookup;
import dev.jstech.computers.program.IqlCraftingSetter;
import dev.jstech.computers.program.IqlEngine;
import dev.jstech.computers.program.iql.IqlCraftingStatement;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * The parts of the machine's crafting network as its programs reach them: the Crafting Interfaces and the Crafting
 * Input Routers on the crafting cables of the network's Crafting Computers, found by name and set the way the IQL sets
 * them, through the same door, so a program can set exactly what a statement can, and is refused the same.
 */
public final class CraftingService {

    private final IComputerTerminalHost terminal;
    private final ServerLevel level;

    public CraftingService(final IComputerTerminalHost terminal, final ServerLevel level) {
        this.terminal = terminal;
        this.level = level;
    }

    /** Whether an interface of that name is on the machine's crafting network. */
    public boolean interfaceExists(final String name) {
        return IqlCraftingSetter.findInterface(level, computers(), name) != null;
    }

    /** Whether a router of that name is on the machine's crafting network. */
    public boolean routerExists(final String name) {
        return IqlCraftingSetter.findRouter(level, computers(), name) != null;
    }

    /** Sets {@code change} on the part named {@code name}, marked with {@code by}: what the IQL would answer. */
    public IqlEngine.Outcome set(final IqlCraftingStatement.Part part, final String name,
                                 final IqlCraftingStatement.Change change, final String by) {
        return IqlCraftingSetter.apply(level, computers(), new IqlCraftingStatement(part, name, change), by);
    }

    /* The network's Crafting Computers, through its Mainframe; none without one. */
    private List<BlockPos> computers() {
        final MainframeBlockEntity mainframe = terminal.networkUuid() == null ? null
                : NetworkLookup.resolveMainframe(level, terminal.networkUuid());
        return mainframe == null ? List.of() : mainframe.craftingComputerPositions();
    }
}
