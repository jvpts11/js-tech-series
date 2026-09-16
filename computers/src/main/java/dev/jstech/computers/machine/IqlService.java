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
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.core.network.NetworkSystem;
import dev.jstech.core.uuid.NetworkUuid;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
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

    private final IComputerTerminalHost terminal;
    private final ServerLevel level;
    /** The shell, for what the engine is still handed and for reading a file of statements. */
    private final ServerCliComputer shell;

    /** The Mainframe the kept engine runs on, which is what says whether it can be kept. */
    @Nullable
    private MainframeBlockEntity mainframe;

    @Nullable
    private IqlEngine engine;

    public IqlService(final IComputerTerminalHost terminal, final ServerLevel level,
                      final ServerCliComputer shell) {
        this.terminal = terminal;
        this.level = level;
        this.shell = shell;
    }

    /** The Mainframe of the machine's network, or null when it is on none, or none is running. */
    @Nullable
    private MainframeBlockEntity mainframe() {
        final NetworkUuid net = this.terminal.networkUuid();
        if (net == null) {
            return null;
        }
        return NetworkSystem.get(this.level).mainframePositionOf(net)
                .map(pos -> this.level.getBlockEntity(BlockPos.of(pos)) instanceof MainframeBlockEntity mf ? mf : null)
                .orElse(null);
    }

    /**
     * Installs the engine on the network's Mainframe, starts it, stops it, or says how it stands.
     *
     * <p>The engine is the network's, not this machine's, so it is installed where the network is run from and every
     * computer of the network speaks to that one.
     */
    public ICliComputer.OpResult control(final String action) {
        final MainframeBlockEntity mainframe = this.mainframe();
        if (mainframe == null) {
            return ICliComputer.OpResult.fail("the network has no running Mainframe to host the IQL Engine");
        }
        return switch (action.toLowerCase(Locale.ROOT)) {
            case "install" -> mainframe.installIqlEngine()
                    ? ICliComputer.OpResult.ok("IQL Engine installed on the Mainframe and started")
                    : ICliComputer.OpResult.fail("the IQL Engine is already installed");
            case "start" -> mainframe.setIqlEngineRunning(true)
                    ? ICliComputer.OpResult.ok("IQL Engine started")
                    : ICliComputer.OpResult.fail(mainframe.isIqlEngineInstalled()
                            ? "the IQL Engine is already running" : "the IQL Engine is not installed");
            case "stop" -> mainframe.setIqlEngineRunning(false)
                    ? ICliComputer.OpResult.ok("IQL Engine stopped")
                    : ICliComputer.OpResult.fail(mainframe.isIqlEngineInstalled()
                            ? "the IQL Engine is already stopped" : "the IQL Engine is not installed");
            case "status", "" -> ICliComputer.OpResult.ok("IQL Engine: " + this.state());
            default -> ICliComputer.OpResult.fail("usage: iqlengine install|start|stop|status");
        };
    }

    /** Whether the network's Mainframe has the engine installed, which is what gates the Engine's own commands. */
    public boolean installed() {
        final MainframeBlockEntity mainframe = this.mainframe();
        return mainframe != null && mainframe.isIqlEngineInstalled();
    }

    /** How the engine stands on the network's Mainframe, in the words every view shows. */
    public String state() {
        final MainframeBlockEntity mainframe = this.mainframe();
        if (mainframe == null || !mainframe.isIqlEngineInstalled()) {
            return "not installed";
        }
        return mainframe.isIqlEngineRunning() ? "running" : "stopped";
    }

    /**
     * The engine that runs statements on the network's Mainframe, or null when the machine is on no network with one.
     *
     * <p>One engine serves every statement while the network's Mainframe stays the same one, and is made again only
     * when the network has another, so asking is a lookup rather than a new engine each time.
     */
    @Nullable
    public IqlEngine engine() {
        final MainframeBlockEntity current = this.mainframe();
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
