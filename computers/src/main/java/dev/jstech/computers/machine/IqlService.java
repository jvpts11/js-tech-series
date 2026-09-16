/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.block.part.NamedBus;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.operation.INetworkOperation;
import dev.jstech.computers.operation.MoveLabels;
import dev.jstech.computers.operation.NetworkStorage;
import dev.jstech.computers.program.IqlEngine;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.program.iql.IqlOperation;
import dev.jstech.computers.storage.IDataSink;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.core.network.NetworkSystem;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.core.uuid.NodeUuid;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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

    /** Safety cap on how many item types a single {@code *} statement expands to. */
    private static final int MAX_WILDCARD_TYPES = 256;

    private final IComputerTerminalHost terminal;
    private final ServerLevel level;
    /** The shell, for what the engine is still handed and for reading a file of statements. */
    private final ServerCliComputer shell;
    /** The work a statement asks of the network, which is the same work the prompt asks for. */
    private final OperationsService operations;
    /** The network a statement reads, for the servers it names. */
    private final NetworkReadService network;

    /** The Mainframe the kept engine runs on, which is what says whether it can be kept. */
    @Nullable
    private MainframeBlockEntity mainframe;

    @Nullable
    private IqlEngine engine;

    public IqlService(final IComputerTerminalHost terminal, final ServerLevel level, final ServerCliComputer shell,
                      final OperationsService operations, final NetworkReadService network) {
        this.terminal = terminal;
        this.level = level;
        this.shell = shell;
        this.operations = operations;
        this.network = network;
    }

    /**
     * The keys a statement targets: a single resolved item, or every item type in scope (the whole network, or one
     * server) when the item is the {@code *} wildcard, capped at {@link #MAX_WILDCARD_TYPES}.
     */
    public List<StorageKey> keysFor(final String item, @Nullable final NodeUuid scopeServer) {
        if (IqlOperation.ANY_ITEM.equals(item)) {
            final NetworkUuid net = this.terminal.networkUuid();
            if (net == null) {
                return List.of();
            }
            final NetworkStorage storage = scopeServer == null
                    ? NetworkStorage.of(this.level, net)
                    : NetworkStorage.ofServers(this.level, List.of(scopeServer));
            return storage.query().keySet().stream().limit(MAX_WILDCARD_TYPES).toList();
        }
        final StorageKey key = ServerCliComputer.itemKey(item);
        return key == null ? List.of() : List.of(key);
    }

    /** How a statement reads back: "N item types" for a {@code *}, else "qty item". */
    public static String describe(final IqlOperation op, final List<StorageKey> keys) {
        if (op.isAnyItem()) {
            return keys.size() + (keys.size() == 1 ? " item type" : " item types");
        }
        return OperationsService.qtyLabel(op.quantity()) + " " + keys.get(0).displayName().getString();
    }

    /** Applies the statement's {@code PRIORITY} to a freshly submitted Operation; a null submission passes through. */
    @Nullable
    public static <T extends INetworkOperation> T prioritize(@Nullable final T operation,
                                                             final IqlOperation statement) {
        if (operation != null) {
            operation.setPriority(statement.priority());
        }
        return operation;
    }

    /**
     * Runs a SELECT: it pulls from the whole network, and {@code SELECT ... FROM <server>} is a move scoped to that
     * server, both landing in this machine's own storage.
     */
    public ICliComputer.OpResult select(final IqlOperation op) {
        final NetworkUuid net = this.terminal.networkUuid();
        final MainframeBlockEntity mainframe = this.mainframe();
        if (mainframe == null || net == null) {
            return ICliComputer.OpResult.fail("the network has no running Mainframe");
        }
        NodeUuid from = null;
        if (op.hasFrom()) {
            from = this.network.serverNamed(net, op.from());
            if (from == null) {
                return ICliComputer.OpResult.fail("no server named '" + op.from() + "'");
            }
        }
        final List<StorageKey> keys = this.keysFor(op.item(), from);
        if (keys.isEmpty()) {
            return op.isAnyItem() ? ICliComputer.OpResult.fail("nothing to select")
                    : ICliComputer.OpResult.fail("unknown item: " + op.item());
        }
        int queued = 0;
        for (final StorageKey key : keys) {
            final var operation = prioritize(from == null
                    ? mainframe.submitNetworkSelect(key, OperationsService.demand(op.quantity()),
                            this.terminal.localStorage(), this.terminal.originLabel(MoveLabels.IQL))
                    : mainframe.submitNetworkMove(key, OperationsService.demand(op.quantity()),
                            this.terminal.localStorage(), this.terminal.originLabel(MoveLabels.IQL),
                            Set.of(from)), op);
            if (operation != null) {
                operation.abortWhen(this.operations.hostGone()); // the pull lands here: stop once this machine is gone
                queued++;
            }
        }
        if (queued == 0) {
            return ICliComputer.OpResult.fail("could not start the SELECT");
        }
        return ICliComputer.OpResult.ok("SELECT queued: " + describe(op, keys)
                + (from == null ? "" : " from " + op.from()) + " -> local storage");
    }

    /**
     * Runs a DELETE or a DROP.
     *
     * <p>A DELETE that names a bus exports to that bus's external inventory, which is the "leaves the network" sense;
     * a DROP, or a DELETE with no target, trashes through a sink that accepts everything and keeps nothing.
     */
    public ICliComputer.OpResult destroy(final IqlOperation op, final String verb) {
        final MainframeBlockEntity mainframe = this.mainframe();
        if (mainframe == null) {
            return ICliComputer.OpResult.fail("the network has no running Mainframe");
        }
        IDataSink target = (k, amount, simulate) -> amount;
        if ("DELETE".equals(verb) && op.to() != null && !op.to().isBlank()) {
            final NamedBus.Located bus = NamedBus.find(this.level, this.terminal.networkUuid(), op.to());
            if (bus == null) {
                return ICliComputer.OpResult.fail("no bus named '" + op.to() + "'");
            }
            target = bus.port();
        }
        final List<StorageKey> keys = this.keysFor(op.item(), null);
        if (keys.isEmpty()) {
            return op.isAnyItem()
                    ? ICliComputer.OpResult.ok("nothing to " + verb.toLowerCase(Locale.ROOT))
                    : ICliComputer.OpResult.fail("unknown item: " + op.item());
        }
        /*
         * Only act on items the network actually holds, so a repeating job's DROP/DELETE becomes a quiet
         * no-op once the stock runs out, instead of a stream of failed operations polluting the log.
         */
        final Map<StorageKey, Long> stock = NetworkStorage.of(this.level, this.terminal.networkUuid()).query();
        int queued = 0;
        for (final StorageKey key : keys) {
            if (stock.getOrDefault(key, 0L) <= 0L) {
                continue;
            }
            if (prioritize(mainframe.submitNetworkDelete(key, OperationsService.demand(op.quantity()), target,
                    this.terminal.originLabel(MoveLabels.IQL)), op) != null) {
                queued++;
            }
        }
        return queued == 0 ? ICliComputer.OpResult.ok("nothing to " + verb.toLowerCase(Locale.ROOT))
                : ICliComputer.OpResult.ok(verb + " queued: " + describe(op, keys));
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
