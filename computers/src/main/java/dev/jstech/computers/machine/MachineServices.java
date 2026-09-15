/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * What the programs on a machine reach through it, kept with the machine so a call finds it ready.
 *
 * <p>Each thing is kept with what it was made from and made again only when that has changed, which costs less to
 * check than to make. Nothing has to tell it the machine changed: a machine read out of a save and then placed in a
 * world, or put in another one, is noticed the next time a program asks.
 */
public final class MachineServices {

    private final BlockEntity machine;

    /** The world the shell was made for; null until a program first asks, and while the machine is in none. */
    @Nullable
    private Level shellLevel;

    @Nullable
    private ServerCliComputer shell;

    public MachineServices(final BlockEntity machine) {
        this.machine = machine;
    }

    /**
     * The machine as its shell sees it, which is how its programs reach its drives and its network.
     *
     * <p>The shell holds nothing of the world but the world itself, so one serves every call while the machine stays
     * there. What a command sets on a shell, a reboot asked for or the terminal window it speaks for, is never set on
     * this one: programs run no commands on their own machine, and the prompt makes a shell of its own.
     *
     * @return null when the machine is not one a person could sit at, or is in no world yet
     */
    @Nullable
    public ServerCliComputer shell() {
        final Level level = this.machine.getLevel();
        if (level != this.shellLevel) {
            this.shellLevel = level;
            this.shell = this.machine instanceof IComputerTerminalHost terminal
                    && level instanceof ServerLevel server ? new ServerCliComputer(terminal, server) : null;
        }
        return this.shell;
    }
}
