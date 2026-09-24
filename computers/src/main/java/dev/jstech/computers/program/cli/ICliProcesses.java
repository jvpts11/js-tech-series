/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
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
        return ICliComputer.OpResult.fail(CliTexts.SAID_BY.with("sigma", ProcessWords.NOT_INSTALLED));
    }

    /** The same, with what the program is started with, as its {@code Program.Args} will read them. */
    default ICliComputer.OpResult startSigma(final String path, final int heapMb, final List<String> arguments) {
        return this.startSigma(path, heapMb);
    }

    /** Stops one of the Σ# programs running here. */
    default ICliComputer.OpResult stopSigma(final int id) {
        return ICliComputer.OpResult.fail(CliTexts.SAID_BY.with("sigma", ProcessWords.NOT_INSTALLED));
    }

    /** What a computer with no Σ# runtime answers. */
    @TextHolder
    final class ProcessWords {

        static final TextKey NOT_INSTALLED = TextKey.of("jsc.cli.processes.not_installed", "not installed");

        private ProcessWords() {
        }
    }
}
