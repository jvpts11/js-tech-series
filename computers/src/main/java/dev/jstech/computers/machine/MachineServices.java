/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * What the programs on a machine reach through it, kept with the machine so a call finds it ready.
 *
 * <p>Each thing is kept with what it was made from and made again only when that has changed, which costs less to
 * check than to make. Nothing has to tell it the machine changed: a machine read out of a save and then placed in a
 * world, or put in another one, is noticed the next time a program asks.
 */
public final class MachineServices {

    private final AbstractComputerBlockEntity machine;

    /**
     * The world the shell and the services over it were made for; null until one is first asked for, and while the
     * machine is in none.
     */
    @Nullable
    private Level shellLevel;

    @Nullable
    private ServerCliComputer shell;

    @Nullable
    private FileService files;

    @Nullable
    private ComputerInfoService computer;

    @Nullable
    private NetworkReadService network;

    @Nullable
    private MainframeStatsService mainframe;

    @Nullable
    private OperationsService operations;

    @Nullable
    private IqlService iql;

    public MachineServices(final AbstractComputerBlockEntity machine) {
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
        this.follow();
        return this.shell;
    }

    /**
     * The machine's drives, reached through the shell's own door.
     *
     * @return null when the machine has no shell
     */
    @Nullable
    public FileService files() {
        this.follow();
        return this.files;
    }

    /**
     * What the machine is and what it holds, as what runs on it reads it.
     *
     * @return null when the machine has no shell
     */
    @Nullable
    public ComputerInfoService computer() {
        this.follow();
        return this.computer;
    }

    /**
     * The data network the machine is on, as what runs on it reads it.
     *
     * @return null when the machine has no shell
     */
    @Nullable
    public NetworkReadService network() {
        this.follow();
        return this.network;
    }

    /**
     * The Mainframe of the machine's network, and what it remembers of the work it has done.
     *
     * @return null when the machine has no shell
     */
    @Nullable
    public MainframeStatsService mainframe() {
        this.follow();
        return this.mainframe;
    }

    /**
     * Asking the machine's network to move and make things.
     *
     * @return null when the machine has no shell
     */
    @Nullable
    public OperationsService operations() {
        this.follow();
        return this.operations;
    }

    /**
     * The network's own language, run on its Mainframe.
     *
     * @return null when the machine has no shell
     */
    @Nullable
    public IqlService iql() {
        this.follow();
        return this.iql;
    }

    /** Makes the shell and what goes through it again when the machine is in another world than before. */
    private void follow() {
        final Level level = this.machine.getLevel();
        if (level == this.shellLevel) {
            return;
        }
        this.shellLevel = level;
        if (this.machine instanceof IComputerTerminalHost terminal && level instanceof ServerLevel server) {
            this.shell = new ServerCliComputer(terminal, server);
            this.files = new FileService(this.shell);
            this.computer = new ComputerInfoService(this.machine, this.shell);
            this.network = new NetworkReadService(terminal, server, this.shell);
            this.mainframe = new MainframeStatsService(this.shell);
            this.operations = new OperationsService(this.shell);
            this.iql = new IqlService(this.shell);
        } else {
            this.shell = null;
            this.files = null;
            this.computer = null;
            this.network = null;
            this.mainframe = null;
            this.operations = null;
            this.iql = null;
        }
    }
}
