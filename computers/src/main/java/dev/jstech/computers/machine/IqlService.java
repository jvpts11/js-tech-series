/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.program.IqlEngine;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.ICliComputer;
import org.jetbrains.annotations.Nullable;

/**
 * The network's own language, as what runs on one of its computers speaks it.
 *
 * <p>A statement goes to the engine on the network's Mainframe exactly as it would from the prompt or the management
 * studio. A statement of the network's second layer (a view, a procedure, a job) needs the engine installed there; a
 * plain one needs only a Mainframe.
 */
public final class IqlService {

    /** How many rows one query may bring back, the same as the studio's default page. */
    private static final int ROW_LIMIT = 4096;

    private final ServerCliComputer shell;

    /** The Mainframe the kept engine runs on, which is what says whether it can be kept. */
    @Nullable
    private MainframeBlockEntity mainframe;

    @Nullable
    private IqlEngine engine;

    IqlService(final ServerCliComputer shell) {
        this.shell = shell;
    }

    /**
     * The engine that runs statements on the network's Mainframe, or null when the machine is on no network with one.
     *
     * <p>One engine serves every statement while the network's Mainframe stays the same one, and is made again only
     * when the network has another, so asking is a lookup rather than a new engine each time.
     */
    @Nullable
    public IqlEngine engine() {
        final MainframeBlockEntity current = this.shell.mainframe();
        if (current == null) {
            this.mainframe = null;
            this.engine = null;
        } else if (current != this.mainframe) {
            this.mainframe = current;
            this.engine = new IqlEngine(current, this.shell, ROW_LIMIT);
        }
        return this.engine;
    }

    /** A file of statements, read the way the shell reads any file. */
    public ICliComputer.FsResult read(final String path) {
        return this.shell.readFile(path);
    }

    /**
     * Runs the statements a file holds, one a line, the way the studio saves them: blank lines and lines starting with
     * {@code --} are skipped, the first refusal ends the run, and what the last statement run answered is the answer.
     */
    public static IqlEngine.Outcome runEach(final IqlEngine engine, final String text) {
        IqlEngine.Outcome last = new IqlEngine.Outcome(true, "nothing to run", java.util.List.of());
        for (final String each : text.split("\\r?\\n")) {
            if (each.isBlank() || each.strip().startsWith("--")) {
                continue;
            }
            last = engine.run(each.strip());
            if (!last.ok()) {
                break;
            }
        }
        return last;
    }
}
