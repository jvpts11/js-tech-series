/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import java.util.List;

/**
 * What a command reaches of the Σ# programs a computer runs: which are running, starting one from a file and stopping
 * one.
 *
 * <p>Every member answers as a computer with no Σ# runtime installed would.
 */
public interface ICliProcesses {

    /** The Σ# programs running here, oldest first. */
    default List<ICliComputer.SigmaProcess> sigmaProcesses() {
        return List.of();
    }

    /**
     * Starts a compiled Σ# program from a file on this computer's disk.
     *
     * @param path   the assembly file to run
     * @param heapMb how much room to give it, or 0 for what the computer decides
     */
    default ICliComputer.OpResult startSigma(final String path, final int heapMb) {
        return ICliComputer.OpResult.fail("sigma: not installed");
    }

    /** The same, with what the program is started with, as its {@code Program.Args} will read them. */
    default ICliComputer.OpResult startSigma(final String path, final int heapMb, final List<String> arguments) {
        return this.startSigma(path, heapMb);
    }

    /** Stops one of the Σ# programs running here. */
    default ICliComputer.OpResult stopSigma(final int id) {
        return ICliComputer.OpResult.fail("sigma: not installed");
    }
}
