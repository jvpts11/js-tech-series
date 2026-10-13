/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.advancement.Acting;
import dev.jstech.computers.block.part.NamedBus;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.engine.EngineVerb;
import dev.jstech.computers.engine.NetworkEngines;
import dev.jstech.computers.operation.INetworkOperation;
import dev.jstech.computers.operation.MoveLabels;
import dev.jstech.computers.operation.NetworkInsertOperation;
import dev.jstech.computers.operation.NetworkStorage;
import dev.jstech.computers.operation.payload.network.NetworkLookup;
import dev.jstech.computers.program.IqlEngine;
import dev.jstech.computers.program.IqlRedstoneSetter;
import dev.jstech.computers.program.cli.CliTexts;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.program.iql.IIqlCondition;
import dev.jstech.computers.program.iql.IIqlView;
import dev.jstech.computers.program.iql.IqlOperation;
import dev.jstech.computers.program.iql.IqlParseResult;
import dev.jstech.computers.program.iql.IqlParser;
import dev.jstech.computers.program.iql.IqlRedstoneStatement;
import dev.jstech.computers.program.iql.IqlTable;
import dev.jstech.computers.program.iql.IqlUpdate;
import dev.jstech.computers.storage.ExternalDataPort;
import dev.jstech.computers.storage.IDataSink;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.computers.storage.StoreSink;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.computers.workshop.UpdateAction;
import dev.jstech.computers.workshop.UpdateDoor;
import dev.jstech.computers.workshop.UpdateRequest;
import dev.jstech.core.network.NetworkSystem;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.util.Loaded;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.core.uuid.NodeUuid;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * The network's own language, as what runs on one of its computers speaks it.
 *
 * <p>A statement goes through the network's door to the engine running on its Mainframe, exactly as it would from
 * the prompt or the management studio, and is answered in that engine's dialect. The core of the language, which
 * every engine accepts, is carried out here as the door's verbs: a SELECT is a pull, an INSERT a push, a MOVE a move
 * and a DELETE an export, so it means the same whichever engine runs. A statement of the network's second layer (a
 * view, a procedure) needs an engine that keeps them.
 */
@TextHolder
public final class IqlService {

    /** How many rows one query may bring back, the same as the studio's default page. */
    private static final int ROW_LIMIT = 4096;

    /** Safety cap on how many item types a single {@code *} statement expands to. */
    private static final int MAX_WILDCARD_TYPES = 256;

    private static final TextKey LIMIT_CAPPED =
            TextKey.of("jsc.service.iql.limit_capped", "%s item types were shown");
    /** A result and a second line under it. */
    private static final TextKey WITH_NOTE = TextKey.of("jsc.service.iql.with_note", "%s\n%s");
    private static final TextKey TYPES_ONE = TextKey.of("jsc.service.iql.types_one", "%s item type");
    private static final TextKey TYPES_MANY = TextKey.of("jsc.service.iql.types_many", "%s item types");
    /** How much of what: a count, or the word for all, and the item's name. */
    private static final TextKey AMOUNT = TextKey.of("jsc.service.iql.amount", "%s %s");
    private static final TextKey NO_SERVER = TextKey.of("jsc.service.iql.no_server", "no server named '%s'");
    private static final TextKey NOTHING_TO_SELECT =
            TextKey.of("jsc.service.iql.nothing_to_select", "nothing to select");
    private static final TextKey SELECT_QUEUED =
            TextKey.of("jsc.service.iql.select_queued", "SELECT queued: %s -> local storage");
    private static final TextKey SELECT_QUEUED_FROM =
            TextKey.of("jsc.service.iql.select_queued_from", "SELECT queued: %s from %s -> local storage");
    private static final TextKey NO_BUS = TextKey.of("jsc.service.iql.no_bus", "no bus named '%s'");
    /** Nothing for a verb to do, the verb written the way the statement wrote it, in small letters. */
    private static final TextKey NOTHING_TO = TextKey.of("jsc.service.iql.nothing_to", "nothing to %s");
    private static final TextKey QUEUED = TextKey.of("jsc.service.iql.queued", "%s queued: %s");
    private static final TextKey NO_HOST = TextKey.of("jsc.service.iql.no_host",
            "the network has no running Mainframe to host the Midsoft IQL Server");
    private static final TextKey INSTALLED = TextKey.of("jsc.service.iql.installed",
            "Midsoft IQL Server installed on the Mainframe");
    private static final TextKey ALREADY_INSTALLED =
            TextKey.of("jsc.service.iql.already_installed", "the Midsoft IQL Server is already installed");
    private static final TextKey STARTED = TextKey.of("jsc.service.iql.started", "Midsoft IQL Server started");
    private static final TextKey ALREADY_RUNNING =
            TextKey.of("jsc.service.iql.already_running", "the Midsoft IQL Server is already running");
    private static final TextKey NOT_INSTALLED =
            TextKey.of("jsc.service.iql.not_installed", "the Midsoft IQL Server is not installed");
    private static final TextKey STOPPED = TextKey.of("jsc.service.iql.stopped", "Midsoft IQL Server stopped");
    private static final TextKey ALREADY_STOPPED =
            TextKey.of("jsc.service.iql.already_stopped", "the Midsoft IQL Server is already stopped");
    private static final TextKey STATUS = TextKey.of("jsc.service.iql.status", "Midsoft IQL Server: %s");
    private static final TextKey STATE_NOT_INSTALLED =
            TextKey.of("jsc.service.iql.state.not_installed", "not installed");
    private static final TextKey STATE_RUNNING = TextKey.of("jsc.service.iql.state.running", "running");
    private static final TextKey STATE_STOPPED = TextKey.of("jsc.service.iql.state.stopped", "stopped");
    private static final TextKey NO_SERVER_OR_BUS =
            TextKey.of("jsc.service.iql.no_server_or_bus", "no server or bus named '%s'");
    private static final TextKey DESTINATION_UNAVAILABLE =
            TextKey.of("jsc.service.iql.destination_unavailable", "the destination server is unavailable");
    private static final TextKey MOVE_FAILED = TextKey.of("jsc.service.iql.move_failed", "could not start the MOVE");
    private static final TextKey MOVE_QUEUED = TextKey.of("jsc.service.iql.move_queued", "MOVE queued: %s %s -> %s");
    private static final TextKey BUS_TOUCHES_NOTHING =
            TextKey.of("jsc.service.iql.bus_touches_nothing", "the bus '%s' touches no inventory");
    private static final TextKey NOTHING_TO_MOVE = TextKey.of("jsc.service.iql.nothing_to_move", "nothing to move");
    private static final TextKey NOTHING_TO_IMPORT =
            TextKey.of("jsc.service.iql.nothing_to_import", "nothing to import");
    private static final TextKey NOTHING_TO_MOVE_TO =
            TextKey.of("jsc.service.iql.nothing_to_move_to", "nothing to move to %s");
    private static final TextKey MOVE_TO_BUS_QUEUED =
            TextKey.of("jsc.service.iql.move_to_bus_queued", "MOVE queued: %s -> %s");
    private static final TextKey NOTHING_TO_IMPORT_FROM =
            TextKey.of("jsc.service.iql.nothing_to_import_from", "nothing to import from %s");
    private static final TextKey IMPORT_QUEUED =
            TextKey.of("jsc.service.iql.import_queued", "MOVE queued: import from %s");
    private static final TextKey READ_IS_NO_OPERATION =
            TextKey.of("jsc.service.iql.read_is_no_operation", "a read does not run as an operation");
    private static final TextKey ONLY_IQL =
            TextKey.of("jsc.service.iql.only_iql", "%s: only .iql files can be run (got .%s)");
    private static final TextKey NO_EXTENSION = TextKey.of("jsc.service.iql.no_extension", "<none>");
    private static final TextKey SYNTAX_ERROR =
            TextKey.of("jsc.service.iql.syntax_error", "%s: syntax error in '%s': %s");
    private static final TextKey NO_READS_IN_RUN = TextKey.of("jsc.service.iql.no_reads_in_run",
            "%s: QUERY/COUNT are not supported by 'run', use 'operation' instead");
    private static final TextKey NOTHING_TO_RUN_IN =
            TextKey.of("jsc.service.iql.nothing_to_run_in", "%s: nothing to run");
    private static final TextKey NOTHING_TO_RUN = TextKey.of("jsc.service.iql.nothing_to_run", "nothing to run");
    private static final TextKey NOTHING_TO_UPDATE =
            TextKey.of("jsc.service.iql.nothing_to_update", "the network holds no %s");
    private static final TextKey UPDATE_FAILED =
            TextKey.of("jsc.service.iql.update_failed", "could not start the UPDATE");
    /** How much of what, and the action the card was set to, as the statement wrote it. */
    private static final TextKey UPDATE_QUEUED =
            TextKey.of("jsc.service.iql.update_queued", "UPDATE queued: %s %s, SET %s");

    private final IComputerTerminalHost terminal;
    private final ServerLevel level;
    /** The machine's drives, for reading a file of statements. */
    private final FileService files;
    /** The work a statement asks of the network, which is the same work the prompt asks for. */
    private final OperationsService operations;
    /** The network a statement reads, for the servers it names and the rows a query brings back. */
    private final NetworkReadService network;

    public IqlService(final IComputerTerminalHost terminal, final ServerLevel level, final FileService files,
                      final OperationsService operations, final NetworkReadService network) {
        this.terminal = terminal;
        this.level = level;
        this.files = files;
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
        final StorageKey key = StorageKey.byName(item);
        return key == null ? List.of() : List.of(key);
    }

    /**
     * The keys an action touches, its {@code WHERE}, {@code ORDER BY} and {@code LIMIT} taken into account. Without
     * them it is {@link #keysFor(String, NodeUuid)}. With them, every variant the network holds of the item (every
     * item, for {@code *}) is a row: the {@code WHERE} keeps the ones it matches (a damaged tool, a name), the
     * {@code ORDER BY} sorts them and the {@code LIMIT} takes the first ones, so the action moves exactly those.
     */
    public List<StorageKey> keysFor(final IqlOperation op, @Nullable final NodeUuid scopeServer) {
        final boolean narrowed = op.where() != null || !op.orderBy().isEmpty() || op.limit() > 0;
        final NetworkUuid net = this.terminal.networkUuid();
        if (!narrowed || net == null) {
            return this.keysFor(op.item(), scopeServer);
        }
        final StorageKey named = op.isAnyItem() ? null : StorageKey.byName(op.item());
        if (!op.isAnyItem() && named == null) {
            return List.of();
        }
        final NetworkStorage storage = scopeServer == null
                ? NetworkStorage.of(this.level, net)
                : NetworkStorage.ofServers(this.level, List.of(scopeServer));
        final String server = scopeServer == null ? "" : NetworkLookup.serverLabel(this.level, scopeServer);
        final List<Map.Entry<StorageKey, Long>> rows = new ArrayList<>();
        for (final Map.Entry<StorageKey, Long> entry : storage.query().entrySet()) {
            final boolean sameItem = named == null || entry.getKey().item() == named.item();
            if (sameItem && (op.where() == null
                    || op.where().matches(NetworkReadService.rowOf(entry.getKey(), entry.getValue(), server)))) {
                rows.add(entry);
            }
        }
        if (!op.orderBy().isEmpty()) {
            sortByField(rows, op, server);
        }
        final int cap = op.limit() > 0 ? Math.min(op.limit(), MAX_WILDCARD_TYPES) : MAX_WILDCARD_TYPES;
        return rows.stream().limit(cap).map(Map.Entry::getKey).toList();
    }

    /**
     * Orders two values of a field the way a person reads them: as numbers when both are numbers, else as words
     * regardless of case. A field the row does not have sorts after every value it does have.
     */
    public static int compareFields(@Nullable final String a, @Nullable final String b) {
        if (a == null || b == null) {
            return a == null ? (b == null ? 0 : 1) : -1;
        }
        try {
            return Double.compare(Double.parseDouble(a), Double.parseDouble(b));
        } catch (final NumberFormatException notNumbers) {
            return a.compareToIgnoreCase(b);
        }
    }

    /**
     * Whether an explicit {@code LIMIT} asked for more item types than one statement handles, and the statement was
     * cut at the cap, so the player must be told that fewer rows than asked for were acted on.
     */
    public static boolean isLimitCapped(final int limit, final int keyCount) {
        return limit > MAX_WILDCARD_TYPES && keyCount >= MAX_WILDCARD_TYPES;
    }

    /** How a statement reads back: "N item types" for a {@code *}, else "qty item". */
    public static Text describe(final IqlOperation op, final List<StorageKey> keys) {
        if (op.isAnyItem()) {
            return (keys.size() == 1 ? TYPES_ONE : TYPES_MANY).with(keys.size());
        }
        return AMOUNT.with(OperationsService.qtyLabel(op.quantity()), keys.get(0).displayName().getString());
    }

    /** The result line of a queued statement, with a second line when its LIMIT was cut at the item type cap. */
    private static Text queuedLine(final Text line, final IqlOperation op, final List<StorageKey> keys) {
        return isLimitCapped(op.limit(), keys.size())
                ? WITH_NOTE.with(line, LIMIT_CAPPED.with(MAX_WILDCARD_TYPES)) : line;
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
            return ICliComputer.OpResult.fail(OperationsService.NO_MAINFRAME);
        }
        NodeUuid from = null;
        if (op.hasFrom()) {
            from = this.network.serverNamed(net, op.from());
            if (from == null) {
                return ICliComputer.OpResult.fail(NO_SERVER.with(op.from()));
            }
        }
        final List<StorageKey> keys = this.keysFor(op, from);
        if (keys.isEmpty()) {
            return op.isAnyItem() ? ICliComputer.OpResult.fail(NOTHING_TO_SELECT)
                    : ICliComputer.OpResult.fail(OperationsService.UNKNOWN_ITEM.with(op.item()));
        }
        int queued = 0;
        for (final StorageKey key : keys) {
            final var operation = prioritize(from == null
                    ? mainframe.networkOperations().pull(key, OperationsService.demand(op.quantity()),
                            this.terminal.localStorage(), this.terminal.originLabel(MoveLabels.IQL))
                    : mainframe.networkOperations().move(key, OperationsService.demand(op.quantity()),
                            this.terminal.localStorage(), this.terminal.originLabel(MoveLabels.IQL),
                            Set.of(from)), op);
            if (operation != null) {
                operation.abortWhen(this.operations.hostGone()); // the pull lands here: stop once this machine is gone
                queued++;
            }
        }
        if (queued == 0) {
            return ICliComputer.OpResult.fail(OperationsService.SELECT_FAILED);
        }
        return ICliComputer.OpResult.ok(queuedLine(from == null ? SELECT_QUEUED.with(describe(op, keys))
                : SELECT_QUEUED_FROM.with(describe(op, keys), op.from()), op, keys));
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
            return ICliComputer.OpResult.fail(OperationsService.NO_MAINFRAME);
        }
        IDataSink target = (k, amount, simulate) -> amount;
        if ("DELETE".equals(verb) && op.to() != null && !op.to().isBlank()) {
            final NamedBus.Located bus = NamedBus.find(this.level, this.terminal.networkUuid(), op.to());
            if (bus == null) {
                return ICliComputer.OpResult.fail(NO_BUS.with(op.to()));
            }
            target = bus.port();
        }
        final List<StorageKey> keys = this.keysFor(op, null);
        if (keys.isEmpty()) {
            return op.isAnyItem()
                    ? ICliComputer.OpResult.ok(NOTHING_TO.with(verb.toLowerCase(Locale.ROOT)))
                    : ICliComputer.OpResult.fail(OperationsService.UNKNOWN_ITEM.with(op.item()));
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
            if (prioritize(mainframe.networkOperations().export(key, OperationsService.demand(op.quantity()), target,
                    this.terminal.originLabel(MoveLabels.IQL)), op) != null) {
                queued++;
            }
        }
        return queued == 0 ? ICliComputer.OpResult.ok(NOTHING_TO.with(verb.toLowerCase(Locale.ROOT)))
                : ICliComputer.OpResult.ok(queuedLine(QUEUED.with(verb, describe(op, keys)), op, keys));
    }

    /** The Mainframe of the machine's network, or null when it is on none, or none is running. */
    @Nullable
    private MainframeBlockEntity mainframe() {
        final NetworkUuid net = this.terminal.networkUuid();
        if (net == null) {
            return null;
        }
        return NetworkSystem.get(this.level).mainframePositionOf(net)
                .map(pos -> Loaded.blockEntity(this.level, BlockPos.of(pos)) instanceof MainframeBlockEntity mf
                        ? mf : null)
                .orElse(null);
    }

    /**
     * Installs the Midsoft IQL Server on the network's Mainframe, starts it, stops it, or says how it stands.
     *
     * <p>The engine is the network's, not this machine's, so it is installed where the network is run from and every
     * computer of the network speaks to that one. Starting it makes it the engine that plans the network's work;
     * stopping it leaves the network without one until it, or another, is started.
     */
    public ICliComputer.OpResult control(final String action) {
        final MainframeBlockEntity mainframe = this.mainframe();
        if (mainframe == null) {
            return ICliComputer.OpResult.fail(NO_HOST);
        }
        final ResourceLocation midsoft = NetworkEngines.MIDSOFT_IQL_SERVER.program();
        final boolean installed = mainframe.installedEngines().containsKey(midsoft);
        final boolean serving = serving(mainframe);
        return switch (action.toLowerCase(Locale.ROOT)) {
            case "install" -> mainframe.installEngine(midsoft)
                    ? ICliComputer.OpResult.ok(INSTALLED)
                    : ICliComputer.OpResult.fail(ALREADY_INSTALLED);
            case "start" -> !installed ? ICliComputer.OpResult.fail(NOT_INSTALLED)
                    : serving ? ICliComputer.OpResult.fail(ALREADY_RUNNING)
                    : (midsoft.equals(mainframe.activeEngine()) ? mainframe.setEngineRunning(true)
                            : mainframe.activateEngine(midsoft))
                            ? ICliComputer.OpResult.ok(STARTED) : ICliComputer.OpResult.fail(ALREADY_RUNNING);
            case "stop" -> !installed ? ICliComputer.OpResult.fail(NOT_INSTALLED)
                    : !serving ? ICliComputer.OpResult.fail(ALREADY_STOPPED)
                    : mainframe.setEngineRunning(false) ? ICliComputer.OpResult.ok(STOPPED)
                            : ICliComputer.OpResult.fail(ALREADY_STOPPED);
            case "status", "" -> ICliComputer.OpResult.ok(STATUS.with(this.stateText()));
            // The verbs are what is typed, so they are written as typed in every language.
            default -> ICliComputer.OpResult.fail(
                    CliTexts.USAGE.with(Text.literal("iqlengine"), Text.literal("install|start|stop|status")));
        };
    }

    /** Whether the network's Mainframe has the Midsoft IQL Server installed. */
    public boolean installed() {
        final MainframeBlockEntity mainframe = this.mainframe();
        return mainframe != null
                && mainframe.installedEngines().containsKey(NetworkEngines.MIDSOFT_IQL_SERVER.program());
    }

    /** How the Midsoft IQL Server stands on the network's Mainframe, in the words every view shows. */
    public Text stateText() {
        final MainframeBlockEntity mainframe = this.mainframe();
        if (mainframe == null
                || !mainframe.installedEngines().containsKey(NetworkEngines.MIDSOFT_IQL_SERVER.program())) {
            return STATE_NOT_INSTALLED.text();
        }
        return (serving(mainframe) ? STATE_RUNNING : STATE_STOPPED).text();
    }

    /** The same in English, for a listing that still carries its states as words. */
    public String state() {
        return this.stateText().english();
    }

    /** Whether the machine is on a network with a Mainframe, which a statement needs to go anywhere. */
    public boolean onNetwork() {
        return this.mainframe() != null;
    }

    /**
     * Runs a statement through the network's door, in the dialect of the engine running on its Mainframe: what it
     * reads comes from this machine's network, and what it asks to be done lands here.
     */
    public IqlEngine.Outcome run(final String statement) {
        return this.run(statement, ROW_LIMIT);
    }

    /** The same, a read bringing back at most {@code rowLimit} rows when the statement sets no limit of its own. */
    public IqlEngine.Outcome run(final String statement, final int rowLimit) {
        final MainframeBlockEntity mainframe = this.mainframe();
        if (mainframe == null) {
            return new IqlEngine.Outcome(false, OperationsService.NO_MAINFRAME.text(), List.of());
        }
        return mainframe.networkOperations().query(this.view(), statement, rowLimit);
    }


    /**
     * Runs an INSERT.
     *
     * <p>An INSERT from a named bus imports through that bus's external inventory; one with no bus source pushes this
     * machine's own storage into the network, and then the source name, if any, only says where it came from.
     */
    public ICliComputer.OpResult insert(final IqlOperation op) {
        final MainframeBlockEntity mainframe = this.mainframe();
        if (op.from() != null && !op.from().isBlank() && mainframe != null) {
            final NamedBus.Located bus = NamedBus.find(this.level, this.terminal.networkUuid(), op.from());
            if (bus != null) {
                return this.moveFromBus(op, mainframe, bus.port());
            }
        }
        return this.operations.insert(op.item(), op.quantity(), op.priority(), MoveLabels.IQL);
    }

    /**
     * Runs a MOVE.
     *
     * <p>A named bus on either side routes through its external inventory: to a bus exports, from a bus imports.
     * Otherwise both sides name servers and it is an internal server-to-server move.
     */
    public ICliComputer.OpResult move(final IqlOperation op) {
        final NetworkUuid net = this.terminal.networkUuid();
        final MainframeBlockEntity mainframe = this.mainframe();
        if (mainframe == null || net == null) {
            return ICliComputer.OpResult.fail(OperationsService.NO_MAINFRAME);
        }
        final NamedBus.Located toBus = NamedBus.find(this.level, net, op.to());
        if (toBus != null) {
            return this.moveToBus(op, mainframe, toBus.port());
        }
        final NamedBus.Located fromBus = NamedBus.find(this.level, net, op.from());
        if (fromBus != null) {
            return this.moveFromBus(op, mainframe, fromBus.port());
        }
        final NodeUuid source = this.network.serverNamed(net, op.from());
        final NodeUuid dest = this.network.serverNamed(net, op.to());
        if (source == null) {
            return ICliComputer.OpResult.fail(NO_SERVER_OR_BUS.with(op.from()));
        }
        if (dest == null) {
            return ICliComputer.OpResult.fail(NO_SERVER_OR_BUS.with(op.to()));
        }
        final IDataSink destSink = this.serverSink(dest);
        if (destSink == null) {
            return ICliComputer.OpResult.fail(DESTINATION_UNAVAILABLE);
        }
        final List<StorageKey> keys = this.keysFor(op, source);
        if (keys.isEmpty()) {
            return op.isAnyItem() ? ICliComputer.OpResult.fail(NOTHING_TO_MOVE)
                    : ICliComputer.OpResult.fail(OperationsService.UNKNOWN_ITEM.with(op.item()));
        }
        int queued = 0;
        for (final StorageKey key : keys) {
            if (prioritize(mainframe.networkOperations().move(key, OperationsService.demand(op.quantity()), destSink,
                    this.terminal.originLabel(MoveLabels.IQL), Set.of(source)), op) != null) {
                queued++;
            }
        }
        return queued == 0 ? ICliComputer.OpResult.fail(MOVE_FAILED)
                : ICliComputer.OpResult.ok(queuedLine(MOVE_QUEUED.with(describe(op, keys), op.from(),
                        NetworkLookup.serverLabel(this.level, dest)), op, keys));
    }

    /** Network to a named bus's external inventory: a timed export, the same path the Export Bus uses. */
    private ICliComputer.OpResult moveToBus(final IqlOperation op, final MainframeBlockEntity mainframe,
                                            final ExternalDataPort port) {
        if (port.isEmpty()) {
            return ICliComputer.OpResult.fail(BUS_TOUCHES_NOTHING.with(op.to()));
        }
        final List<StorageKey> keys = this.keysFor(op, null);
        if (keys.isEmpty()) {
            return op.isAnyItem() ? ICliComputer.OpResult.ok(NOTHING_TO_MOVE)
                    : ICliComputer.OpResult.fail(OperationsService.UNKNOWN_ITEM.with(op.item()));
        }
        final Map<StorageKey, Long> stock = NetworkStorage.of(this.level, this.terminal.networkUuid()).query();
        int queued = 0;
        for (final StorageKey key : keys) {
            if (stock.getOrDefault(key, 0L) <= 0L) {
                continue;
            }
            if (prioritize(mainframe.networkOperations().export(key, OperationsService.demand(op.quantity()), port,
                    this.terminal.originLabel(MoveLabels.IQL)), op) != null) {
                queued++;
            }
        }
        return queued == 0 ? ICliComputer.OpResult.ok(NOTHING_TO_MOVE_TO.with(op.to()))
                : ICliComputer.OpResult.ok(queuedLine(MOVE_TO_BUS_QUEUED.with(describe(op, keys), op.to()), op, keys));
    }

    /**
     * A named bus's external inventory to the network. Pulls from the bus and inserts into the network as a timed
     * operation; anything the network cannot hold is returned to the source, so nothing is lost.
     */
    private ICliComputer.OpResult moveFromBus(final IqlOperation op, final MainframeBlockEntity mainframe,
                                              final ExternalDataPort port) {
        if (port.isEmpty()) {
            return ICliComputer.OpResult.fail(BUS_TOUCHES_NOTHING.with(op.from()));
        }
        final List<StorageKey> keys = op.isAnyItem() ? port.available() : this.keysFor(op.item(), null);
        if (keys.isEmpty()) {
            return op.isAnyItem() ? ICliComputer.OpResult.ok(NOTHING_TO_IMPORT)
                    : ICliComputer.OpResult.fail(OperationsService.UNKNOWN_ITEM.with(op.item()));
        }
        final long perKey = OperationsService.demand(op.quantity());
        int queued = 0;
        for (final StorageKey key : keys) {
            final long avail = port.extract(key, perKey, true);
            if (avail <= 0L) {
                continue;
            }
            final long pulled = port.extract(key, avail, false);
            if (pulled <= 0L) {
                continue;
            }
            final NetworkInsertOperation insert = prioritize(
                    mainframe.networkOperations().push(key, pulled, this.terminal.originLabel(MoveLabels.IQL)), op);
            if (insert != null) {
                insert.onSettle(() -> {
                    final long left = insert.leftover();
                    if (left > 0L) {
                        port.insert(key, left, false); // the network could not hold it all: return to the source
                    }
                });
                queued++;
            } else {
                port.insert(key, pulled, false); // dispatch failed (engine off): put it back, lose nothing
            }
        }
        return queued == 0 ? ICliComputer.OpResult.ok(NOTHING_TO_IMPORT_FROM.with(op.from()))
                : ICliComputer.OpResult.ok(IMPORT_QUEUED.with(op.from()));
    }

    /** Where a server's own storage takes what is moved into it, or null when that server cannot be reached. */
    @Nullable
    private IDataSink serverSink(final NodeUuid node) {
        return NetworkSystem.get(this.level).locationOf(node)
                .map(loc -> Loaded.blockEntity(this.level, BlockPos.of(loc.rackPos()))
                        instanceof ServerRackBlockEntity rack
                        ? (IDataSink) new StoreSink(rack.getServerStorage(loc.slot()))
                        : null)
                .orElse(null);
    }

    /**
     * Carries out a statement that changes something.
     *
     * <p>Every verb is answered here, on the machine's own services: the ones that move and make things go to the
     * work the prompt asks for through the same doors, and a read is refused, because a read is not an operation.
     */
    public ICliComputer.OpResult execute(final IqlOperation op) {
        return switch (op.verb()) {
            case SELECT -> this.select(op);
            case INSERT -> this.insert(op);
            case CRAFT -> this.operations.craft(op.item(), op.quantity(), op.priority(), MoveLabels.IQL);
            case DELETE -> this.destroy(op, "DELETE");
            case DROP -> this.destroy(op, "DROP");
            case MOVE -> this.move(op);
            case LOCK -> this.operations.lock(op.item(), op.quantity());
            case UNLOCK -> this.operations.unlock(op.item());
            case ANALYZE, VACUUM, REINDEX -> this.operations.maintenance(op.verb());
            case UPDATE -> this.update(op);
            case QUERY, COUNT -> ICliComputer.OpResult.fail(READ_IS_NO_OPERATION);
        };
    }

    /**
     * Runs an UPDATE: a personal-use card of this machine changes the item the network holds, the variant the
     * statement's WHERE picks (the most worn first for a repair, else the one the network holds most of). Whoever is
     * acting pays the card's price. ENCHANT with no OFFER only lists the three offers and changes nothing.
     */
    public ICliComputer.OpResult update(final IqlOperation op) {
        final NetworkUuid net = this.terminal.networkUuid();
        final MainframeBlockEntity mainframe = this.mainframe();
        if (mainframe == null || net == null || !op.hasUpdate()) {
            return ICliComputer.OpResult.fail(OperationsService.NO_MAINFRAME);
        }
        final Text unavailable = mainframe.networkOperations().refusal(EngineVerb.UPDATE);
        if (unavailable != null) {
            return ICliComputer.OpResult.fail(unavailable);
        }
        final IqlUpdate set = op.update();
        NodeUuid from = null;
        if (op.hasFrom()) {
            from = this.network.serverNamed(net, op.from());
            if (from == null) {
                return ICliComputer.OpResult.fail(NO_SERVER.with(op.from()));
            }
        }
        if (StorageKey.byName(op.item()) == null) {
            return ICliComputer.OpResult.fail(OperationsService.UNKNOWN_ITEM.with(op.item()));
        }
        final List<StorageKey> keys = this.variants(op, from, set.action());
        if (keys.isEmpty()) {
            return ICliComputer.OpResult.fail(NOTHING_TO_UPDATE.with(op.item()));
        }
        final StorageKey key = keys.get(0);
        final ServerPlayer payer = Acting.current().map(id -> this.level.getServer().getPlayerList().getPlayer(id))
                .orElse(null);
        final Text refused = UpdateDoor.refusal(this.level, this.terminal, key, set.action(), payer);
        if (refused != null) {
            return ICliComputer.OpResult.fail(refused);
        }
        if (set.action() == UpdateAction.ENCHANT && !set.hasOffer()) {
            return ICliComputer.OpResult.ok(UpdateDoor.offersText(payer, key));
        }
        final Map<StorageKey, Long> stock = NetworkStorage.of(this.level, net).query();
        StorageKey with = null;
        if (set.action().takesSecond()) {
            with = set.hasWith() ? mostHeld(stock, StorageKey.byName(set.with()))
                    : UpdateDoor.material(stock, key.stack(1));
            if (with == null) {
                return ICliComputer.OpResult.fail(set.hasWith() ? NOTHING_TO_UPDATE.with(set.with())
                        : UpdateDoor.NO_MATERIAL.with(key.displayName().getString()));
            }
        }
        // ALL means all the named server holds when FROM is given; the network-wide stock only feeds WITH.
        final Map<StorageKey, Long> source = from == null ? stock
                : NetworkStorage.ofServers(this.level, List.of(from)).query();
        final long quantity = op.quantity() == IqlOperation.ALL ? source.getOrDefault(key, 1L)
                : Math.max(1L, op.quantity());
        final UpdateRequest request = new UpdateRequest(((BlockEntity) this.terminal).getBlockPos(), key, quantity,
                set.action(), set.offer() - 1, set.name(), with, from, payer == null ? null : payer.getUUID(),
                this.terminal.originLabel(MoveLabels.IQL));
        if (prioritize(mainframe.networkOperations().update(request), op) == null) {
            return ICliComputer.OpResult.fail(UPDATE_FAILED);
        }
        return ICliComputer.OpResult.ok(UPDATE_QUEUED.with(OperationsService.qtyLabel(quantity),
                key.displayName().getString(), set.action().name()));
    }

    /*
     * Every variant the network holds of the statement's item, its WHERE kept to and its ORDER BY and LIMIT applied;
     * with no ORDER BY, the most worn first for a repair and the one held most of first for anything else.
     */
    private List<StorageKey> variants(final IqlOperation op, @Nullable final NodeUuid scopeServer,
                                      final UpdateAction action) {
        final NetworkUuid net = this.terminal.networkUuid();
        final StorageKey named = StorageKey.byName(op.item());
        if (net == null || named == null) {
            return List.of();
        }
        final NetworkStorage storage = scopeServer == null
                ? NetworkStorage.of(this.level, net)
                : NetworkStorage.ofServers(this.level, List.of(scopeServer));
        final String server = scopeServer == null ? "" : NetworkLookup.serverLabel(this.level, scopeServer);
        final List<Map.Entry<StorageKey, Long>> rows = new ArrayList<>();
        for (final Map.Entry<StorageKey, Long> entry : storage.query().entrySet()) {
            if (entry.getKey().item() == named.item() && entry.getValue() > 0L && (op.where() == null
                    || op.where().matches(NetworkReadService.rowOf(entry.getKey(), entry.getValue(), server)))) {
                rows.add(entry);
            }
        }
        if (!op.orderBy().isEmpty()) {
            sortByField(rows, op, server);
        } else if (action == UpdateAction.REPAIR) {
            rows.sort(Comparator.comparingInt((Map.Entry<StorageKey, Long> entry) -> entry.getKey().stack(1)
                    .getDamageValue()).reversed());
        } else {
            rows.sort(Map.Entry.<StorageKey, Long>comparingByValue().reversed());
        }
        final int cap = op.limit() > 0 ? Math.min(op.limit(), MAX_WILDCARD_TYPES) : MAX_WILDCARD_TYPES;
        return rows.stream().limit(cap).map(Map.Entry::getKey).toList();
    }

    /*
     * Sorts the rows by the statement's ORDER BY field. The field is read once per row, not once per comparison:
     * for the stack-backed fields every read builds an item stack, which a comparator would repeat O(n log n) times.
     */
    private static void sortByField(final List<Map.Entry<StorageKey, Long>> rows, final IqlOperation op,
                                    final String server) {
        final List<SortKeyed> keyed = new ArrayList<>(rows.size());
        for (final Map.Entry<StorageKey, Long> entry : rows) {
            keyed.add(new SortKeyed(entry,
                    NetworkReadService.rowOf(entry.getKey(), entry.getValue(), server).apply(op.orderBy())));
        }
        final Comparator<SortKeyed> order = Comparator.comparing(SortKeyed::value, IqlService::compareFields);
        keyed.sort(op.orderByDescending() ? order.reversed() : order);
        rows.clear();
        for (final SortKeyed row : keyed) {
            rows.add(row.entry());
        }
    }

    /* The variant of {@code named}'s item the network holds most of, or null when it holds none. */
    @Nullable
    private static StorageKey mostHeld(final Map<StorageKey, Long> stock, @Nullable final StorageKey named) {
        if (named == null) {
            return null;
        }
        StorageKey best = null;
        long most = 0L;
        for (final Map.Entry<StorageKey, Long> entry : stock.entrySet()) {
            if (entry.getKey().item() == named.item() && entry.getValue() > most) {
                best = entry.getKey();
                most = entry.getValue();
            }
        }
        return best;
    }

    /** The whole of this machine the engine is handed: what it reads, and what it asks to be done. */
    private IIqlView view() {
        return new IIqlView() {
            @Override
            public List<ICliComputer.StoredItem> queryObject(final String object, final IIqlCondition where,
                                                             final String server, final int limit) {
                return IqlService.this.network.queryObject(object, where, server, limit);
            }

            @Override
            public IqlTable queryTable(final String object, final IIqlCondition where, final String server,
                                       final int limit, final String orderBy, final boolean descending) {
                return IqlService.this.network.queryTable(object, where, server, limit, orderBy, descending);
            }

            @Override
            public ICliComputer.OpResult execute(final IqlOperation operation) {
                return IqlService.this.execute(operation);
            }

            @Override
            public ICliComputer.OpResult setRedstone(final IqlRedstoneStatement statement, final String by) {
                return IqlRedstoneSetter.apply(IqlService.this.level, IqlService.this.terminal, statement, by);
            }
        };
    }

    /** A file of statements, read the way a machine reads any file. */
    public ICliComputer.FsResult read(final String path) {
        return this.files.readFile(path);
    }

    /**
     * Runs a file of statements from the machine's drives.
     *
     * <p>Only a {@code .iql} file is run, and the extension is checked before the file is looked for, so a wrong
     * name is answered as a wrong name rather than as a missing file. What is inside goes through the very path the
     * {@code operation} command uses, so a statement in a file and one typed at the prompt are the same statement.
     */
    public ICliComputer.FsResult runFile(final String path) {
        final String ext = extensionOf(path);
        if (!"iql".equalsIgnoreCase(ext)) {
            return ICliComputer.FsResult.fail(ONLY_IQL.with(path, ext.isEmpty() ? NO_EXTENSION : ext));
        }
        final ICliComputer.FsResult read = this.files.readFile(path);
        if (!read.ok()) {
            return read;
        }
        /*
         * The whole file is read before any of it runs, so a mistake on the third line stops the run before the
         * first two have moved anything.
         */
        final List<IqlParseResult> statements = new ArrayList<>();
        final List<String> texts = new ArrayList<>();
        for (final String statement : statementsOf(read.message().english())) {
            final IqlParseResult parsed = IqlParser.tryParse(statement);
            if (!parsed.ok()) {
                return ICliComputer.FsResult.fail(SYNTAX_ERROR.with(path, statement, parsed.error()));
            }
            /*
             * QUERY/COUNT are read operations that produce rows, not timed operations; they cannot be
             * dispatched via execute(). The caller should use 'operation' for those.
             */
            if (parsed.isRead()) {
                return ICliComputer.FsResult.fail(NO_READS_IN_RUN.with(path));
            }
            statements.add(parsed);
            texts.add(statement);
        }
        if (statements.isEmpty()) {
            return ICliComputer.FsResult.fail(NOTHING_TO_RUN_IN.with(path));
        }
        ICliComputer.OpResult last = null;
        for (int i = 0; i < statements.size(); i++) {
            /*
             * A definition is the engine's to keep, and a setting the engine's to make, so both go through the
             * network's door as the studio sends them; only an action is carried out here.
             */
            if (statements.get(i).operation() == null) {
                final IqlEngine.Outcome defined = this.run(texts.get(i));
                last = defined.ok() ? ICliComputer.OpResult.ok(defined.said())
                        : ICliComputer.OpResult.fail(defined.said());
            } else {
                last = this.execute(statements.get(i).operation());
            }
            if (!last.ok()) {
                break;
            }
        }
        return ICliComputer.FsResult.iqlResult(last);
    }

    /**
     * The statements a file holds, one a line, the way the studio saves them: blank lines and lines starting with
     * {@code --} are left out. A file run at the prompt and one run by a program are read by this one rule.
     */
    private static List<String> statementsOf(final String text) {
        final List<String> statements = new ArrayList<>();
        for (final String line : text.split("\\r?\\n")) {
            final String statement = line.strip();
            if (!statement.isEmpty() && !statement.startsWith("--")) {
                statements.add(statement);
            }
        }
        return statements;
    }

    /** The lowercase extension of a path (after the last dot), or {@code ""} when it has none. */
    private static String extensionOf(final String path) {
        final int dot = path.lastIndexOf('.');
        return dot >= 0 && dot < path.length() - 1 ? path.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
    }

    /**
     * Runs the statements a file holds, one a line, the way the studio saves them: blank lines and lines starting with
     * {@code --} are skipped, the first refusal ends the run, and what the last statement run answered is the answer.
     */
    public IqlEngine.Outcome runEach(final String text) {
        IqlEngine.Outcome last = new IqlEngine.Outcome(true, NOTHING_TO_RUN.text(), List.of());
        for (final String statement : statementsOf(text)) {
            last = this.run(statement);
            if (!last.ok()) {
                break;
            }
        }
        return last;
    }

    /** Whether the Midsoft IQL Server is the engine planning the network's work now. */
    private static boolean serving(final MainframeBlockEntity mainframe) {
        return NetworkEngines.MIDSOFT_IQL_SERVER.program().equals(mainframe.activeEngine())
                && mainframe.engineRunning();
    }

    /* A storage row paired with the ORDER BY value read from it once. */
    private record SortKeyed(Map.Entry<StorageKey, Long> entry, @Nullable String value) {
    }
}
