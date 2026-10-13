/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.iql;

import dev.jstech.computers.audio.SystemSound;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.client.os.IsmsApp;
import dev.jstech.computers.client.os.IsmsProfilerApp;
import dev.jstech.computers.crafting.CraftPlanner;
import dev.jstech.computers.engine.ICraftPlanning;
import dev.jstech.computers.engine.NetworkEngines;
import dev.jstech.computers.engine.NetworkOperationsService;
import dev.jstech.computers.machine.IqlTables;
import dev.jstech.computers.operation.index.IndexHealth;
import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.operation.payload.IqlResultPayload;
import dev.jstech.computers.operation.payload.IsmsActionPayload;
import dev.jstech.computers.operation.payload.IsmsPlanPayload;
import dev.jstech.computers.operation.payload.IsmsSchemaPayload;
import dev.jstech.computers.operation.payload.IsmsTracePayload;
import dev.jstech.computers.operation.payload.RequestIsmsSchemaPayload;
import dev.jstech.computers.operation.payload.RunIqlPayload;
import dev.jstech.computers.operation.payload.operations.OperationsPayloads;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.program.IqlEngine;
import dev.jstech.computers.program.Programs;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.program.iql.IqlCatalog;
import dev.jstech.computers.program.iql.IqlDefinition;
import dev.jstech.computers.program.iql.IqlSavedObject;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.computers.trace.IsmsTraces;
import dev.jstech.core.network.NetworkSystem;
import dev.jstech.core.network.ServerNode;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.util.ShortId;
import dev.jstech.core.uuid.NetworkUuid;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jetbrains.annotations.Nullable;

import static dev.jstech.computers.operation.payload.network.NetworkLookup.networkLabel;
import static dev.jstech.computers.operation.payload.network.NetworkLookup.resolveMainframe;
import static dev.jstech.computers.operation.payload.network.NetworkLookup.serverLabel;

/**
 * The payloads of the IQL Server Management Studio: a statement of a query tab run against the network, the network
 * as its Object Explorer shows it, what it asks of the network beside statements (the engine, the jobs, the locks,
 * an Operation stopped, a craft's estimated plan, the Activity Monitor) and the traces its Profiler records.
 *
 * <p>Every answer carries the number of the studio window that asked, so two studios open at once each hear their
 * own. The scripts, traces and results a studio keeps are files on the computer it runs on, read and written the way
 * every program reads and writes them.
 */
@TextHolder
public final class IqlPayloads {

    /** The columns of what a statement brought back when it read no table: a name, how many, and where. */
    private static final List<String> ROW_COLUMNS = List.of("name", "qty", "detail");
    /** The network a computer on no network would be part of, by the name every network starts with. */
    private static final Text DEFAULT_NETWORK = Text.literal("jsc-net");

    // An estimated plan, a line for each thing it does, top down as a plan reads.
    private static final TextKey PLAN_ROOT = TextKey.of("jsc.isms.plan.root", "CRAFT %s %s: %s");
    private static final TextKey PLAN_FEASIBLE = TextKey.of("jsc.isms.plan.feasible", "can be made now");
    private static final TextKey PLAN_SHORT = TextKey.of("jsc.isms.plan.short", "short of what it needs");
    private static final TextKey PLAN_BENCH = TextKey.of("jsc.isms.plan.bench", "Bench Craft: %s x%s, %s runs");
    private static final TextKey PLAN_MACHINE = TextKey.of("jsc.isms.plan.machine",
            "Machine Process: %s x%s, %s runs");
    private static final TextKey PLAN_SEEK = TextKey.of("jsc.isms.plan.seek", "Index Seek: %s x%s of %s held");
    private static final TextKey PLAN_MISSING = TextKey.of("jsc.isms.plan.missing", "Missing: %s x%s");
    private static final TextKey PLAN_NO_RECIPE = TextKey.of("jsc.isms.plan.no_recipe",
            "no recipe on the network makes %s");
    private static final TextKey PLAN_UNKNOWN = TextKey.of("jsc.isms.plan.unknown", "unknown item: %s");
    private static final TextKey JOB_STARTED = TextKey.of("jsc.isms.job.started", "job %s started");
    private static final TextKey JOB_PAUSED = TextKey.of("jsc.isms.job.paused", "job %s paused");
    private static final TextKey JOB_DELETED = TextKey.of("jsc.isms.job.deleted", "job %s deleted");
    private static final TextKey JOB_NONE = TextKey.of("jsc.isms.job.none", "no job named %s");
    private static final TextKey NO_MAINFRAME = TextKey.of("jsc.isms.action.no_mainframe",
            "the network has no Mainframe");

    private IqlPayloads() {
    }

    /** Registers the payloads this class handles. */
    public static void register(final PayloadRegistrar registrar) {
        ComputerAccess.accept(registrar, RunIqlPayload.TYPE, RunIqlPayload.STREAM_CODEC,
                ComputerAccess.machine(RunIqlPayload::hostPos), IqlPayloads::handleRunIql);
        registrar.playToClient(IqlResultPayload.TYPE, IqlResultPayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread(IqlPayloads::handleIqlResult));
        ComputerAccess.accept(registrar, RequestIsmsSchemaPayload.TYPE, RequestIsmsSchemaPayload.STREAM_CODEC,
                ComputerAccess.machine(RequestIsmsSchemaPayload::hostPos), IqlPayloads::handleRequestSchema);
        registrar.playToClient(IsmsSchemaPayload.TYPE, IsmsSchemaPayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread(IqlPayloads::handleSchema));
        ComputerAccess.accept(registrar, IsmsActionPayload.TYPE, IsmsActionPayload.STREAM_CODEC,
                ComputerAccess.machine(IsmsActionPayload::hostPos), IqlPayloads::handleAction);
        registrar.playToClient(IsmsPlanPayload.TYPE, IsmsPlanPayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread(IqlPayloads::handlePlan));
        registrar.playToClient(IsmsTracePayload.TYPE, IsmsTracePayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread(IqlPayloads::handleTrace));
    }

    /**
     * The network as a studio on {@code host} shows it, for its window {@code window}: the engine with its version,
     * how many rows each table holds, the saved objects, the servers, the index and the items held by hand.
     */
    public static IsmsSchemaPayload ismsSchema(final ServerLevel level, final IComputerTerminalHost host,
                                               final int window) {
        final NetworkUuid net = host.networkUuid();
        if (net == null) {
            return new IsmsSchemaPayload(window, IsmsSchemaPayload.OFFLINE.with(DEFAULT_NETWORK), host.hostname(),
                    IsmsSchemaPayload.Engine.offline(), List.of(), List.of(), List.of(), List.of(), List.of(),
                    null, List.of());
        }
        final MainframeBlockEntity mainframe = resolveMainframe(level, net);
        final List<Integer> tableRows = new ArrayList<>();
        for (final String table : IqlTables.TABLES) {
            tableRows.add(IqlTables.count(level, net, table));
        }
        final List<String> servers = new ArrayList<>();
        for (final ServerNode server : NetworkSystem.get(level).serversOf(net)) {
            servers.add(serverLabel(level, server.nodeUuid()));
        }
        final List<IsmsSchemaPayload.Saved> views = new ArrayList<>();
        final List<IsmsSchemaPayload.Saved> procedures = new ArrayList<>();
        final List<IsmsSchemaPayload.Job> jobs = new ArrayList<>();
        final List<IsmsSchemaPayload.Lock> locks = new ArrayList<>();
        // Without a Mainframe there is no index to report, which is not the same as a healthy one.
        IsmsSchemaPayload.Index index = null;
        if (mainframe != null) {
            final IqlCatalog catalog = mainframe.iqlCatalog();
            catalog.ofType(IqlDefinition.ObjectType.VIEW)
                    .forEach(view -> views.add(new IsmsSchemaPayload.Saved(view.name(), view.body())));
            catalog.ofType(IqlDefinition.ObjectType.PROCEDURE)
                    .forEach(procedure -> procedures.add(new IsmsSchemaPayload.Saved(procedure.name(),
                            procedure.body())));
            for (final IqlSavedObject job : catalog.ofType(IqlDefinition.ObjectType.JOB)) {
                jobs.add(new IsmsSchemaPayload.Job(job.name(), mainframe.isJobPaused(job.name()), trigger(job),
                        job.body()));
            }
            index = new IsmsSchemaPayload.Index(IndexHealth.State.byId(mainframe.indexHealthState()),
                    mainframe.indexedTypes(), mainframe.indexedServers());
            for (final Map.Entry<StorageKey, Long> lock : mainframe.lockedTypes().entrySet()) {
                locks.add(new IsmsSchemaPayload.Lock(lock.getKey().registryId().toString(),
                        GameText.of(lock.getKey().displayName()), lock.getValue()));
            }
        }
        return new IsmsSchemaPayload(window, Text.literal(networkLabel(net)), host.hostname(), engine(mainframe),
                tableRows, views, procedures, jobs, servers, index, locks);
    }

    /**
     * Runs one statement of a studio's query tab for the computer {@code host}, on behalf of {@code requester}, and
     * answers it for the window, the tab and the place in the script that sent it.
     */
    public static IqlResultPayload runStatement(final ServerLevel level, final IComputerTerminalHost host,
                                                final String requester, final RunIqlPayload payload) {
        final MainframeBlockEntity mainframe = resolveMainframe(level, host.networkUuid());
        if (mainframe == null) {
            return IqlResultPayload.said(payload.window(), payload.tab(), payload.seq(), false,
                    IqlResultPayload.NO_MAINFRAME.text());
        }
        final ServerCliComputer computer = new ServerCliComputer(host, level);
        // A trace says who ran the statement and on which computer, as the studio knows them.
        final IsmsTraces.Origin origin = new IsmsTraces.Origin(requester, host.hostname());
        final MainframeBlockEntity.Started<IqlEngine.Outcome> run = IsmsTraces.as(origin,
                () -> mainframe.capturingStarted(() -> mainframe.networkOperations().query(
                        IqlEngine.viewOf(computer), payload.statement(), IqlResultPayload.MAX_ROWS)));
        final List<String> started = new ArrayList<>();
        for (final UUID id : run.operations()) {
            started.add(ShortId.of(id.toString()));
        }
        return result(payload, run.answer(), started);
    }

    /** Does what a studio asked of the engine, a job, a lock or an Operation, and says how it went. */
    public static ICliComputer.OpResult act(final IsmsActionPayload payload,
                                            @Nullable final MainframeBlockEntity mainframe,
                                            final ServerCliComputer computer) {
        final String target = payload.target();
        return switch (payload.action()) {
            case IsmsActionPayload.ENGINE_START -> computer.engineControl("start");
            case IsmsActionPayload.ENGINE_STOP -> computer.engineControl("stop");
            case IsmsActionPayload.ENGINE_RESTART -> {
                computer.engineControl("stop");
                yield computer.engineControl("start");
            }
            case IsmsActionPayload.UNLOCK -> computer.unlock(target);
            case IsmsActionPayload.CANCEL -> computer.cancelOperation(target);
            case IsmsActionPayload.JOB_START, IsmsActionPayload.JOB_PAUSE, IsmsActionPayload.JOB_DELETE ->
                    job(payload.action(), mainframe, target);
            default -> ICliComputer.OpResult.fail(Text.EMPTY);
        };
    }

    /**
     * The estimated plan of {@code quantity} of {@code item}: what the engine would make it from now, root first as a
     * plan reads, then each step it would take, then what it would read from the index and what it lacks. Nothing is
     * made and nothing is held.
     */
    public static IsmsPlanPayload plan(@Nullable final MainframeBlockEntity mainframe, final int window,
                                       final String item, final long quantity) {
        final ICraftPlanning planner = mainframe == null ? null : mainframe.networkOperations().planner();
        if (planner == null) {
            return refused(window, NetworkOperationsService.UNAVAILABLE.text());
        }
        final StorageKey key = StorageKey.byName(item);
        if (key == null) {
            return refused(window, PLAN_UNKNOWN.with(item));
        }
        final Map<StorageKey, Long> stock = mainframe.networkIndex().snapshot();
        final CraftPlanner.Plan plan = planner.plan(key, quantity, mainframe.networkPatterns(),
                mainframe.networkProcessingPatterns(), stock);
        if (plan.steps().isEmpty()) {
            return refused(window, PLAN_NO_RECIPE.with(GameText.of(key.displayName())));
        }
        final List<Text> lines = new ArrayList<>();
        final List<Integer> depth = new ArrayList<>();
        lines.add(PLAN_ROOT.with(quantity, GameText.of(key.displayName()),
                (plan.feasible() ? PLAN_FEASIBLE : PLAN_SHORT).text()));
        depth.add(0);
        for (int i = plan.steps().size() - 1; i >= 0; i--) {
            final CraftPlanner.Step step = plan.steps().get(i);
            lines.add((step.isMachine() ? PLAN_MACHINE : PLAN_BENCH).with(step.resultText(), step.produced(),
                    step.runs()));
            depth.add(1);
        }
        for (final Map.Entry<StorageKey, Long> read : plan.rawConsumption().entrySet()) {
            lines.add(PLAN_SEEK.with(GameText.of(read.getKey().displayName()), read.getValue(),
                    stock.getOrDefault(read.getKey(), 0L)));
            depth.add(2);
        }
        for (final Map.Entry<StorageKey, Long> missing : plan.missing().entrySet()) {
            lines.add(PLAN_MISSING.with(GameText.of(missing.getKey().displayName()), missing.getValue()));
            depth.add(2);
        }
        return new IsmsPlanPayload(window, true, lines, depth);
    }

    private static void handleRunIql(final RunIqlPayload payload, final ServerPlayer player, final ServerLevel level) {
        if (!(level.getBlockEntity(payload.hostPos()) instanceof IComputerTerminalHost host)) {
            return;
        }
        final IqlResultPayload answer = runStatement(level, host, player.getGameProfile().getName(), payload);
        PacketDistributor.sendToPlayer(player, answer);
        // A query that fails is an error of the program's, which the machine sounds as its system sounds one.
        if (!answer.ok() && host instanceof IOsHost machine) {
            machine.systemSound(level, SystemSound.ERROR);
        }
    }

    /*
     * What a statement came to as its tab shows it: the table it read with that table's own columns, or the rows a
     * statement that reads no table brought back, each a name, how many and where.
     */
    private static IqlResultPayload result(final RunIqlPayload asked, final IqlEngine.Outcome outcome,
                                           final List<String> started) {
        if (!outcome.table().isNone()) {
            return new IqlResultPayload(asked.window(), asked.tab(), asked.seq(), outcome.ok(), outcome.said(),
                    outcome.table().columns(), outcome.table().rows(), started);
        }
        if (outcome.rows().isEmpty()) {
            return new IqlResultPayload(asked.window(), asked.tab(), asked.seq(), outcome.ok(), outcome.said(),
                    List.of(), List.of(), started);
        }
        final List<List<Text>> rows = new ArrayList<>(outcome.rows().size());
        for (final ICliComputer.StoredItem item : outcome.rows()) {
            rows.add(List.of(item.name(), Text.literal(Long.toString(item.quantity())), item.detail()));
        }
        return new IqlResultPayload(asked.window(), asked.tab(), asked.seq(), outcome.ok(), outcome.said(),
                ROW_COLUMNS, rows, started);
    }

    private static void handleIqlResult(final IqlResultPayload payload, final Player player) {
        IsmsApp.accept(payload);
    }

    private static void handleRequestSchema(final RequestIsmsSchemaPayload payload, final ServerPlayer player,
                                            final ServerLevel level) {
        if (level.getBlockEntity(payload.hostPos()) instanceof IComputerTerminalHost host) {
            PacketDistributor.sendToPlayer(player, ismsSchema(level, host, payload.window()));
        }
    }

    private static void handleSchema(final IsmsSchemaPayload payload, final Player player) {
        IsmsApp.acceptSchema(payload);
        IsmsProfilerApp.acceptSchema(payload);
    }

    private static void handleAction(final IsmsActionPayload payload, final ServerPlayer player,
                                     final ServerLevel level) {
        if (!(level.getBlockEntity(payload.hostPos()) instanceof IComputerTerminalHost host)) {
            return;
        }
        final NetworkUuid net = host.networkUuid();
        final MainframeBlockEntity mainframe = net == null ? null : resolveMainframe(level, net);
        final int window = payload.window();
        switch (payload.action()) {
            case IsmsActionPayload.ACTIVITY -> {
                if (net != null) {
                    OperationsPayloads.dispatchActiveOperations(player, net, level);
                }
                return;
            }
            case IsmsActionPayload.TRACE_START -> {
                if (net != null) {
                    IsmsTraces.start(player, net, window, payload.arg());
                }
                return;
            }
            case IsmsActionPayload.TRACE_STOP -> {
                IsmsTraces.stop(player, window);
                return;
            }
            case IsmsActionPayload.PLAN -> {
                PacketDistributor.sendToPlayer(player, plan(mainframe, window, payload.target(),
                        Math.max(1, payload.arg())));
                return;
            }
            case IsmsActionPayload.REFRESH -> {
                // Nothing to do but answer with the network as it stands.
            }
            default -> {
                final ICliComputer.OpResult done = act(payload, mainframe, new ServerCliComputer(host, level));
                PacketDistributor.sendToPlayer(player, IqlResultPayload.said(window, IsmsActionPayload.NO_TAB, 0,
                        done.ok(), done.message()));
            }
        }
        // Whatever was asked, the explorer is shown the network as it now stands.
        PacketDistributor.sendToPlayer(player, ismsSchema(level, host, window));
    }

    /* Starts, pauses or deletes the saved job {@code name}. */
    private static ICliComputer.OpResult job(final int action, @Nullable final MainframeBlockEntity mainframe,
                                             final String name) {
        if (mainframe == null) {
            return ICliComputer.OpResult.fail(NO_MAINFRAME.text());
        }
        if (!mainframe.iqlCatalog().contains(IqlDefinition.ObjectType.JOB, name)) {
            return ICliComputer.OpResult.fail(JOB_NONE.with(name));
        }
        return switch (action) {
            case IsmsActionPayload.JOB_START -> {
                mainframe.restartJob(name);
                yield ICliComputer.OpResult.ok(JOB_STARTED.with(name));
            }
            case IsmsActionPayload.JOB_PAUSE -> {
                mainframe.pauseJob(name);
                yield ICliComputer.OpResult.ok(JOB_PAUSED.with(name));
            }
            default -> {
                mainframe.deleteJob(name);
                yield ICliComputer.OpResult.ok(JOB_DELETED.with(name));
            }
        };
    }

    private static IsmsPlanPayload refused(final int window, final Text why) {
        return new IsmsPlanPayload(window, false, List.of(why), List.of(0));
    }

    private static void handlePlan(final IsmsPlanPayload payload, final Player player) {
        IsmsApp.acceptPlan(payload);
    }

    private static void handleTrace(final IsmsTracePayload payload, final Player player) {
        IsmsProfilerApp.accept(payload);
    }

    /*
     * The Midsoft IQL Server on the network's Mainframe: its version, whether it serves, and whether it is the engine
     * the network runs, which is what the studio needs; when another runs, which.
     */
    private static IsmsSchemaPayload.Engine engine(@Nullable final MainframeBlockEntity mainframe) {
        final ResourceLocation midsoft = NetworkEngines.MIDSOFT_IQL_SERVER.program();
        final String name = programName(midsoft);
        if (mainframe == null) {
            return new IsmsSchemaPayload.Engine(IsmsSchemaPayload.EngineState.NOT_INSTALLED, name, "", false, "");
        }
        final ResourceLocation active = mainframe.activeEngine();
        final boolean other = active != null && !active.equals(midsoft);
        final String running = other ? (programName(active) + " "
                + mainframe.installedEngines().getOrDefault(active, "")).strip() : "";
        final String version = mainframe.installedEngines().get(midsoft);
        if (version == null) {
            return new IsmsSchemaPayload.Engine(IsmsSchemaPayload.EngineState.NOT_INSTALLED, name, "", false,
                    running);
        }
        final boolean serving = midsoft.equals(active) && mainframe.engineRunning();
        return new IsmsSchemaPayload.Engine(serving ? IsmsSchemaPayload.EngineState.RUNNING
                : IsmsSchemaPayload.EngineState.STOPPED, name, version, !other, running);
    }

    /* A program's name as its package calls it, which is a product's name and reads the same in every language. */
    private static String programName(final ResourceLocation program) {
        final ProgramSpec spec = Programs.get(program);
        return spec == null ? program.getPath() : Text.of(spec.name()).english();
    }

    /* What fires a job, as its statement wrote it: EVERY or WHEN and what follows; nothing for one never fired. */
    private static String trigger(final IqlSavedObject job) {
        return switch (job.triggerKind()) {
            case EVERY -> "EVERY " + job.triggerSpec();
            case WHEN -> "WHEN " + job.triggerSpec();
            case NONE -> "";
        };
    }
}
