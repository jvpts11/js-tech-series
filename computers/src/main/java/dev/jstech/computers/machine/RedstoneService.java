/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.blockentity.RedstoneInterfaceBlockEntity;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/**
 * The Redstone Interfaces linked to the machine, as what runs on it reaches them: one found by the name it answers to,
 * whatever the case of its letters. A program reaches only the interfaces of the machine it runs on, as a name is
 * unique among one computer's interfaces and not across a network.
 */
public final class RedstoneService {

    private final AbstractComputerBlockEntity machine;
    private final ServerLevel level;

    public RedstoneService(final AbstractComputerBlockEntity machine, final ServerLevel level) {
        this.machine = machine;
        this.level = level;
    }

    /** The interface linked to the machine that answers to {@code name}, or null when none does. */
    @Nullable
    public RedstoneInterfaceBlockEntity named(final String name) {
        return RedstoneInterfaceBlockEntity.linkedTo(this.level, this.machine, name);
    }
}
