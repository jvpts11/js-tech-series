/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.computers.os.HostScope;
import dev.jstech.computers.os.Platform;
import dev.jstech.computers.os.ShellFamily;
import dev.jstech.computers.os.UnixTree;
import dev.jstech.computers.program.job.JobWhen;
import dev.jstech.computers.program.job.MachineJobs;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * What a command reaches of the computer it runs on: what the computer is called and built from, whether it runs, what
 * hangs off it, and the restart a command can ask for once it is done.
 *
 * <p>Every member answers as a computer that says nothing about itself would, so a computer made for a test writes only
 * what its commands read.
 */
public interface ICliMachine {

    /** The name the computer is known by, or empty when it has none. */
    default String name() {
        return "";
    }

    /** A human label for the computer kind, e.g. {@code "Personal Computer"} or {@code "Mainframe"}. */
    default String type() {
        return "";
    }

    /** A short, stable identifier for this node (the head of its UUID), for display. */
    default String nodeId() {
        return "";
    }

    /** Whether the computer is switched on. */
    default boolean running() {
        return false;
    }

    /** Orchestration capacity in items per tick. */
    default long cpuCapacity() {
        return 0L;
    }

    /** RAM buffer in items. */
    default long ramBuffer() {
        return 0L;
    }

    /** The block entity behind this shell, for commands that exist on one kind of machine only. */
    default Object hostBlock() {
        return null;
    }

    /** This computer's host name as a POSIX shell shows it (its name, or the OS id when unnamed). */
    default String hostname() {
        return "computer";
    }

    /** The command-syntax family of the installed OS's kernel; DOS when nothing says otherwise. */
    default ShellFamily shellFamily() {
        return ShellFamily.DOS;
    }

    /**
     * The family of the installed system, which is what its tools name the kernel after; Linux when nothing
     * says otherwise, since that is the family most Unix prompts here belong to.
     */
    default Platform platform() {
        return Platform.LINUX;
    }

    /** Where the installed system keeps the home and mounts the other drives, which is its family's habit. */
    default UnixTree tree() {
        return UnixTree.of(platform());
    }

    /**
     * Where the installed system stands in its family's order, counting from one, so a command can belong to one
     * edition onwards; nought when the system puts itself nowhere in an order.
     */
    default int osEdition() {
        return 0;
    }

    /** Whether this computer is of the kind a command asks for (any computer, a Mainframe, a Cluster Manager). */
    default boolean hostIs(final HostScope scope) {
        return scope == HostScope.ANY;
    }

    /** Whether the system keeps files at all, which a network system such as MC-NET does not. */
    default boolean hasFiles() {
        return false;
    }

    /** Whether the computer has ports for peripherals. */
    default boolean hasPorts() {
        return false;
    }

    /**
     * The day and the hour by the clock of the world this machine stands in, as a person reads them.
     *
     * <p>There is one clock here and everything is timed by it: a job on a schedule, a line in a log, the hour
     * on a file. A machine that cannot see it says so rather than making an hour up.
     */
    default String worldTime() {
        return "the clock is not set";
    }

    /** What this machine's memory is spent on: what it has, what it promised, and what is really in it. */
    default ICliComputer.MemoryUse memory() {
        return new ICliComputer.MemoryUse(0, 0, 0L);
    }

    /** The work this machine does with nobody at it: lines left running and lines waiting for an hour. */
    default List<MachineJobs.Job> jobs() {
        return List.of();
    }

    /** Leaves a line for the machine to run, now or at an hour; null on a machine that keeps none. */
    @Nullable
    default MachineJobs.Job addJob(final String line, final JobWhen when) {
        return null;
    }

    /** Takes one off the list; false when there was none of that number. */
    default boolean stopJob(final int id) {
        return false;
    }

    /** How wide the processor's word is, which is what a system names its architecture after. */
    default int processorBits() {
        return 64;
    }

    /** The system information for {@code screenfetch}, or null when no OS is installed. */
    default ICliComputer.SystemInfo systemInfo() {
        return null;
    }

    /** One-line summaries of the peripherals linked to this computer (monitors, drives, ...). */
    default List<String> peripherals() {
        return List.of();
    }

    /**
     * Asks the host to restart into its firmware setup once this command finishes (the {@code reboot --firmware}
     * verb).
     */
    default void requestFirmwareReboot() {
    }

    /** Whether a command asked for a restart into the firmware during this run. */
    default boolean firmwareRebootRequested() {
        return false;
    }

    /** Asks the host to restart once this command finishes: the monitor replays the POST, then boots. */
    default void requestReboot() {
    }

    /** Whether a command asked for a plain restart during this run. */
    default boolean rebootRequested() {
        return false;
    }
}
