/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.computers.program.install.LiveInstallState;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

/**
 * What a command reaches of the programs installed on a computer: which there are, installing one, and the manual
 * install a live medium walks through.
 *
 * <p>Every member answers as a computer with nothing installed and nothing to install from would.
 */
public interface ICliInstallation {

    /** The programs installed on this computer. */
    default List<ICliComputer.ProgramInfo> programs() {
        return List.of();
    }

    /** Installs a program on this computer by id. */
    default ICliComputer.OpResult install(final String programId) {
        return ICliComputer.OpResult.fail("this computer cannot install programs");
    }

    /** Whether the program with the given id is present on this computer (installed, or a service flag). */
    default boolean hasProgram(final ResourceLocation id) {
        return false;
    }

    /** The live installation in progress on this computer, or null when it booted a real OS. */
    default LiveInstallState liveInstall() {
        return null;
    }

    /**
     * Runs one live-installer line against the install state. The message carries the tool's output lines
     * (newline-separated); when the sequence completes, the host installs the system and ends the session.
     */
    default ICliComputer.OpResult liveRun(final String line) {
        return ICliComputer.OpResult.fail("no live medium is booted");
    }
}
