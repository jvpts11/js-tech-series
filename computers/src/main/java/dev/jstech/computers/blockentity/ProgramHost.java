/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.computers.gateway.GatewayManager;
import dev.jstech.computers.hardware.ComputerBuild;
import dev.jstech.computers.hardware.CpuSpec;
import dev.jstech.computers.machine.MachinePrograms;
import dev.jstech.computers.machine.MachineServices;
import dev.jstech.computers.machine.NetworkReadService;
import dev.jstech.computers.machine.ServerTickDeadline;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.cli.SshTerminal;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.computers.vm.program.IProgramParent;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.function.Predicate;
import java.util.function.ToLongFunction;

/**
 * The programs a machine is running and what they reach through it.
 *
 * <p>They live with the machine rather than with its system disk: they are what it is doing, not what it
 * has installed.
 */
final class ProgramHost {

    private final AbstractComputerBlockEntity machine;
    private final MachinePrograms programs;
    /** What the programs on this machine reach through it, kept here so a call finds it ready. */
    private final MachineServices services;
    /** What the network holds of an item, as the programs' tick asks it; made once, not on every tick. */
    private final ToLongFunction<String> stockLookup = this::networkStock;
    /** Whether a program on another machine still waits, as the programs' tick asks it; made once, not per tick. */
    private final Predicate<IProgramParent.Remote> parentWaiting = this::remoteParentWaiting;
    /*
     * Whether a Gateway of this machine may have something waiting. It starts true, so a machine that loads after
     * a Gateway was spoken to still looks once; a Gateway sets it again whenever it is spoken to.
     */
    private boolean gatewayMail = true;

    ProgramHost(final AbstractComputerBlockEntity machine) {
        this.machine = machine;
        this.programs = new MachinePrograms(this::tellRemoteParent);
        this.services = new MachineServices(machine);
    }

    MachinePrograms programs() {
        return this.programs;
    }

    MachineServices services() {
        return this.services;
    }

    /**
     * How much of that the network holds, for the programs watching it.
     *
     * <p>Off a network, everything reads as none: a watch on a machine with no cable simply never goes
     * off, which is the truthful answer and not an error.
     */
    long networkStock(final String item) {
        final NetworkReadService network = this.services.network();
        return network == null ? 0L : network.stock(item);
    }

    /** The prompt this machine's shell would show, for giving it back when a program lets go. */
    String shellPrompt() {
        return this.shellPromptLine().text();
    }

    /** The same prompt a run at a time, in the colours the machine's shell gives it. */
    CliLine shellPromptLine() {
        if (this.machine instanceof IComputerTerminalHost host
                && this.machine.getLevel() instanceof ServerLevel server) {
            final ServerCliComputer shell = new ServerCliComputer(host, server);
            return SshTerminal.promptLine(shell, shell);
        }
        return CliLine.plain("");
    }

    /**
     * How many instructions this machine's processors are worth in one tick.
     *
     * <p>A machine with no build is worth nothing, which is the honest answer for one whose parts have
     * been taken out from under a running program.
     */
    int credits() {
        final ComputerBuild build = this.machine.currentBuild();
        if (build == null) {
            return 0;
        }
        long coreMegahertz = 0;
        for (final CpuSpec cpu : build.cpus()) {
            coreMegahertz += (long) cpu.cores() * cpu.freqMhz();
        }
        return MachinePrograms.creditsFor(coreMegahertz);
    }

    /**
     * Runs whatever programs the machine has, or stops them all if it is no longer up, and says whether they
     * ran at all: a machine that ran nothing has nothing new to tell anyone watching it.
     *
     * <p>A computer that has been switched off is not running programs, so they are told so and given their
     * chance to say goodbye rather than being left frozen for whenever it comes back on.
     */
    boolean tick() {
        // A machine running nothing still pays down what a Gateway spent on its behalf, tick by tick.
        if (this.programs.isEmpty() && this.programs.owed() == 0) {
            return false;
        }
        if (!this.machine.isRunning()) {
            this.programs.stopAll();
            this.machine.setChanged();
            return false;
        }
        /*
         * The server's clock, not this machine's worth, is what bounds the tick: a machine the server has
         * no time for this tick runs nothing and is first next tick.
         */
        final Level level = this.machine.getLevel();
        final long deadline = level instanceof ServerLevel server
                ? ServerTickDeadline.shared().claim(server, this.machine.getBlockPos())
                : Long.MAX_VALUE;
        this.programs.tick(credits(), deadline, this.stockLookup, this.parentWaiting);
        return true;
    }

    /**
     * Hands the programs on this machine whatever the ComputerCraft computers said through its Gateways.
     *
     * <p>A message waits on the Gateway until this tick and no longer: whoever is listening hears it now,
     * and a machine where no program listens simply lets it go.
     */
    void hearGateways() {
        // Nothing was said to a Gateway of this machine since the last look, so there is nothing to find.
        if (!this.gatewayMail) {
            return;
        }
        this.gatewayMail = false;
        if (!(this.machine.getLevel() instanceof ServerLevel server)) {
            return;
        }
        for (final NetworkGatewayBlockEntity gateway : GatewayManager.gatewaysOf(server, this.machine)) {
            for (final NetworkGatewayBlockEntity.Message said : gateway.takeMessages()) {
                this.programs.deliverGatewayMessage(said.from(), said.text(), said.tick());
            }
        }
    }

    /** Says a Gateway linked to this machine has something waiting for its programs. */
    void gatewayMailWaits() {
        this.gatewayMail = true;
    }

    void save(final CompoundTag tag) {
        if (this.programs.isEmpty()) {
            return;
        }
        final CompoundTag sigma = new CompoundTag();
        this.programs.save(sigma);
        tag.put("Σ#", sigma);
    }

    void load(final CompoundTag tag) {
        if (tag.contains("Σ#")) {
            this.programs.load(tag.getCompound("Σ#"), this.machine);
        }
    }

    /**
     * Whether the program on another machine that started one of this machine's programs is still there
     * to read what it left.
     *
     * <p>It is only asked about a program that has finished and was started from elsewhere, so an ordinary
     * tick never looks. A machine whose chunk is not loaded is not known to be gone: its programs come back
     * with it, so what was started for them is kept until it can be asked.
     */
    private boolean remoteParentWaiting(final IProgramParent.Remote parent) {
        if (!(this.machine.getLevel() instanceof ServerLevel server)) {
            return false;
        }
        final BlockPos where = BlockPos.of(parent.machine());
        if (!server.isLoaded(where)) {
            return true;
        }
        return server.getBlockEntity(where) instanceof AbstractComputerBlockEntity machine
                && machine.nodeUuid() != null && parent.node().equals(machine.nodeUuid().value())
                && machine.programs().byId(parent.program()) != null;
    }

    /**
     * Tells the program on another machine that started one of this machine's programs that it has ended, so a
     * wait on it runs again at once. A machine that is not loaded is not told: its programs look again when they
     * come back.
     */
    private void tellRemoteParent(final IProgramParent.Remote parent, final int child) {
        if (!(this.machine.getLevel() instanceof ServerLevel server)) {
            return;
        }
        final BlockPos where = BlockPos.of(parent.machine());
        if (server.isLoaded(where) && server.getBlockEntity(where) instanceof AbstractComputerBlockEntity machine
                && machine.nodeUuid() != null && parent.node().equals(machine.nodeUuid().value())) {
            machine.programs().tellEnded(parent.program(), child);
        }
    }
}
