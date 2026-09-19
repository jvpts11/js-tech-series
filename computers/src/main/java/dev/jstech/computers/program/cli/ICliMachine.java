/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.computers.os.Platform;
import dev.jstech.computers.os.ShellFamily;
import java.util.List;

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
