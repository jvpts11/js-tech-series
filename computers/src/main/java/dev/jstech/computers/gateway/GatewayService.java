/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gateway;

import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.NetworkGatewayBlockEntity;
import dev.jstech.computers.machine.ProgramLauncher;
import dev.jstech.computers.operation.INetworkOperation;
import dev.jstech.computers.operation.MoveLabels;
import dev.jstech.computers.operation.NetworkInsertOperation;
import dev.jstech.computers.operation.NetworkSelectOperation;
import dev.jstech.computers.operation.NetworkStorage;
import dev.jstech.computers.operation.payload.OperationRecord;
import dev.jstech.computers.operation.payload.network.NetworkLookup;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.CliTexts;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.computers.vm.program.IProgramParent;
import dev.jstech.computers.vm.program.ProgramPriority;
import dev.jstech.computers.vm.system.CallCost;
import dev.jstech.computers.vm.system.SigmaCosts;
import dev.jstech.core.operation.OperationPriority;
import dev.jstech.core.peripheral.IPeripheralOwner;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.core.uuid.NodeUuid;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * What the other side may ask of the network through a Gateway, answered the way the host computer's own
 * programs are answered: through the host's shell, out of the host's budget, under the Gateway's
 * permissions and call cap, with every request signed by who asked. Free of ComputerCraft types, so it is
 * tested without a ComputerCraft computer and can serve other callers.
 *
 * <p>Names follow ComputerCraft's habit: an item is its registry id ({@code minecraft:iron_ingot}, the
 * namespace optional for vanilla), a fluid is {@code fluid/} and its id, a chemical {@code chemical/} and
 * its id. An operation is named by its id; a prefix of the id is enough where it is unambiguous.
 *
 * <p>Why a request was refused is declared here and handed on in English, the machine's language: the other side
 * is another machine that reads it as data, and the Gateway's log keeps it as it was written.
 */
@TextHolder
public final class GatewayService {

    /** Queued on a computer that watches a name when its total changes: name, total, previous. */
    public static final String EVENT_STOCK = "jsc_stock";
    /** Queued on the computer that started an operation when it settles: id, status. */
    public static final String EVENT_OPERATION = "jsc_operation";

    private static final String FLUID_PREFIX = "fluid/";
    private static final String CHEMICAL_PREFIX = "chemical/";
    private static final int WHAT_LENGTH = 96;

    /** The file calls that change what is on a disk rather than only reading it. */
    private static final List<String> WRITES_FILES =
            List.of("Write", "Append", "Delete", "MkDir", "Put", "MakeDir", "Remove");

    /** A list the other side asks for, priced as a program's read is, by how many rows it brings back. */
    private static final CallCost ROWS = CallCost.perRow(SigmaCosts.READ);

    private static final TextKey COULD_NOT_START =
            TextKey.of("jsc.service.gateway.could_not_start", "could not start the %s");
    private static final TextKey BUFFER_HOLDS_NONE =
            TextKey.of("jsc.service.gateway.buffer_holds_none", "the buffer holds no %s");
    private static final TextKey NO_PATTERN = TextKey.of("jsc.service.gateway.no_pattern", "no pattern crafts %s");
    private static final TextKey WOULD_NOT_STOP = TextKey.of("jsc.service.gateway.would_not_stop", "would not stop");
    private static final TextKey NOT_RUNNING = TextKey.of("jsc.service.gateway.not_running", "not running");
    private static final TextKey NO_SUCH_COMPUTER =
            TextKey.of("jsc.service.gateway.no_such_computer", "%s: no such computer on this network");
    private static final TextKey POWERED_OFF = TextKey.of("jsc.service.gateway.powered_off", "%s is powered off");
    private static final TextKey NO_REMOTE_PROGRAMS = TextKey.of("jsc.service.gateway.no_remote_programs",
            "%s does not take programs from other computers");
    private static final TextKey CANNOT_RUN =
            TextKey.of("jsc.service.gateway.cannot_run", "%s cannot run programs");
    private static final TextKey NO_RUNNER =
            TextKey.of("jsc.service.gateway.no_runner", "%s: nothing installed runs a program of this kind");
    private static final TextKey NO_MEMORY =
            TextKey.of("jsc.service.gateway.no_memory", "%s: %s MB will not fit in %s MB of free memory");
    private static final TextKey BUSY = TextKey.of("jsc.service.gateway.busy", "busy: %s calls a tick");
    private static final TextKey READING_OFF =
            TextKey.of("jsc.service.gateway.reading_off", "denied: reading the network is off");
    private static final TextKey OPERATIONS_OFF =
            TextKey.of("jsc.service.gateway.operations_off", "denied: operations are off");
    private static final TextKey NOT_IN_WORLD =
            TextKey.of("jsc.service.gateway.not_in_world", "the Gateway is not in a world");
    private static final TextKey NOT_LINKED =
            TextKey.of("jsc.service.gateway.not_linked", "the Gateway is not linked to a computer");
    private static final TextKey WHICH_ITEM = TextKey.of("jsc.service.gateway.which_item", "which item?");
    private static final TextKey UNKNOWN_FLUID = TextKey.of("jsc.service.gateway.unknown_fluid", "unknown fluid: %s");
    private static final TextKey UNKNOWN_CHEMICAL =
            TextKey.of("jsc.service.gateway.unknown_chemical", "unknown chemical: %s");
    private static final TextKey UNKNOWN_ITEM = TextKey.of("jsc.service.gateway.unknown_item", "unknown item: %s");
    private static final TextKey NOT_ON_NETWORK =
            TextKey.of("jsc.service.gateway.not_on_network", "the computer is not on a network");
    private static final TextKey NO_MAINFRAME =
            TextKey.of("jsc.service.gateway.no_running_mainframe", "the network has no running Mainframe");

    /** Who is asking, from the other side: a ComputerCraft computer by its id. */
    public record Caller(int id) {

        public String label() {
            return "CC #" + id;
        }
    }

    private final NetworkGatewayBlockEntity gateway;
    private final ServerLevel level;
    private final IComputerTerminalHost terminal;
    private final ServerCliComputer shell;

    private GatewayService(final NetworkGatewayBlockEntity gateway, final ServerLevel level,
                           final IComputerTerminalHost terminal) {
        this.gateway = gateway;
        this.level = level;
        this.terminal = terminal;
        this.shell = new ServerCliComputer(terminal, level);
    }

    /** The service for a linked Gateway; refused while the Gateway has no computer to answer for it. */
    public static GatewayService of(final NetworkGatewayBlockEntity gateway) throws GatewayRefusedException {
        if (!(gateway.getLevel() instanceof ServerLevel level)) {
            throw new GatewayRefusedException(NOT_IN_WORLD.text());
        }
        final IPeripheralOwner owner = gateway.owner();
        if (!(owner instanceof IComputerTerminalHost terminal) || !(owner instanceof BlockEntity)) {
            throw new GatewayRefusedException(NOT_LINKED.text());
        }
        return new GatewayService(gateway, level, terminal);
    }

    // Names

    /** The key a name on the other side stands for. */
    public static StorageKey resolve(final String name) throws GatewayRefusedException {
        if (name == null || name.isBlank()) {
            throw new GatewayRefusedException(WHICH_ITEM.text());
        }
        final String given = name.trim().toLowerCase(Locale.ROOT);
        if (given.startsWith(FLUID_PREFIX)) {
            final ResourceLocation id = ResourceLocation.tryParse(given.substring(FLUID_PREFIX.length()));
            final Fluid fluid = id == null ? null : BuiltInRegistries.FLUID.getOptional(id).orElse(null);
            if (fluid == null) {
                throw new GatewayRefusedException(UNKNOWN_FLUID.with(name));
            }
            return StorageKey.of(new FluidStack(fluid, 1));
        }
        if (given.startsWith(CHEMICAL_PREFIX)) {
            final ResourceLocation id = ResourceLocation.tryParse(given.substring(CHEMICAL_PREFIX.length()));
            if (id == null) {
                throw new GatewayRefusedException(UNKNOWN_CHEMICAL.with(name));
            }
            return StorageKey.chemical(id);
        }
        final ResourceLocation id = ResourceLocation.tryParse(given.contains(":") ? given : "minecraft:" + given);
        final Item item = id == null ? null : BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        if (item == null) {
            throw new GatewayRefusedException(UNKNOWN_ITEM.with(name));
        }
        return StorageKey.of(item);
    }

    /** The name the other side knows a key by. */
    public static String nameOf(final StorageKey key) {
        return switch (key.kind()) {
            case FLUID -> FLUID_PREFIX + key.registryId();
            case CHEMICAL -> CHEMICAL_PREFIX + key.registryId();
            default -> key.registryId().toString();
        };
    }

    /** What the network holds of {@code name}, for the watches; zero for a name it does not know. */
    public static long stockOf(final NetworkGatewayBlockEntity gateway, final String name) {
        try {
            final GatewayService service = of(gateway);
            return service.storage().count(resolve(name));
        } catch (final GatewayRefusedException unknown) {
            return 0L;
        }
    }

    // Reads

    public long capacity(final Caller caller) throws GatewayRefusedException {
        read(caller, "capacity");
        final long value = shell.networkUse().capacity();
        charge(SigmaCosts.GLANCE_NETWORK);
        return value;
    }

    public long used(final Caller caller) throws GatewayRefusedException {
        read(caller, "used");
        final long value = shell.networkUse().stored();
        charge(SigmaCosts.GLANCE_NETWORK);
        return value;
    }

    /** Every kind of thing the network holds, by name, with its total. */
    public Map<String, Long> types(final Caller caller) throws GatewayRefusedException {
        read(caller, "types");
        final Map<String, Long> out = new LinkedHashMap<>();
        for (final Map.Entry<StorageKey, Long> entry : storage().query().entrySet()) {
            if (entry.getValue() > 0L) {
                out.put(nameOf(entry.getKey()), entry.getValue());
            }
        }
        charge(ROWS.at(out.size(), 0));
        return out;
    }

    public long total(final Caller caller, final String name) throws GatewayRefusedException {
        read(caller, "total " + name);
        final long value = storage().count(resolve(name));
        charge(SigmaCosts.READ);
        return value;
    }

    /** Which servers hold {@code name}, and how much each. */
    public List<Map<String, Object>> find(final Caller caller, final String name) throws GatewayRefusedException {
        read(caller, "find " + name);
        final List<Map<String, Object>> rows = new ArrayList<>();
        for (final Map.Entry<NodeUuid, Long> entry : storage().breakdown(resolve(name)).entrySet()) {
            if (entry.getValue() > 0L) {
                rows.add(luaTable("server", NetworkLookup.serverLabel(level, entry.getKey()),
                        "quantity", entry.getValue()));
            }
        }
        charge(ROWS.at(rows.size(), 0));
        return rows;
    }

    public List<Map<String, Object>> servers(final Caller caller) throws GatewayRefusedException {
        read(caller, "servers");
        final List<Map<String, Object>> rows = new ArrayList<>();
        for (final ICliComputer.ServerUse server : shell.servers()) {
            rows.add(luaTable("name", server.name(), "used", server.stored(), "capacity", server.capacity(),
                    "online", true));
        }
        charge(ROWS.at(rows.size(), 0));
        return rows;
    }

    /** The computers of this network, with the folders each shares. */
    public List<Map<String, Object>> computers(final Caller caller) throws GatewayRefusedException {
        read(caller, "computers");
        final Map<String, List<String>> sharesByHost = new LinkedHashMap<>();
        for (final ICliComputer.NetworkShare share : shell.networkShares()) {
            sharesByHost.computeIfAbsent(share.hostname(), k -> new ArrayList<>()).add(share.share().name());
        }
        final List<Map<String, Object>> rows = new ArrayList<>();
        for (final ICliComputer.RemoteHost host : shell.reachableHosts()) {
            rows.add(luaTable("name", host.hostname(), "label", host.name(), "os", host.os(), "type", host.type(),
                    "online", host.running(), "shares", sharesByHost.getOrDefault(host.hostname(), List.of())));
        }
        charge(ROWS.at(rows.size(), 0));
        return rows;
    }

    // Operations

    /** Pulls {@code quantity} of {@code name} from the network into the buffer; the operation's id. */
    public String pull(final Caller caller, final String name, final long quantity, @Nullable final String priority)
            throws GatewayRefusedException {
        final String what = "pull " + count(quantity) + " " + name;
        operations(caller, what);
        final StorageKey key = resolve(name);
        final NetworkSelectOperation op = mainframe().submitNetworkSelect(key, demand(quantity),
                new BufferSink(gateway.buffer()), label(caller));
        if (op == null) {
            throw denied(caller, what, COULD_NOT_START.with("SELECT"));
        }
        op.setPriority(priorityOf(priority));
        op.abortWhen(gateway::isRemoved);
        op.onSettle(() -> settled(caller, op));
        return started(caller, what, op, SigmaCosts.SUBMIT);
    }

    /** Pushes up to {@code quantity} of {@code name} from the buffer into the network; the operation's id. */
    public String push(final Caller caller, final String name, final long quantity, @Nullable final String priority)
            throws GatewayRefusedException {
        final String what = "push " + count(quantity) + " " + name;
        operations(caller, what);
        final StorageKey key = resolve(name);
        final ItemStackHandler buffer = gateway.buffer();
        final long wanted = demand(quantity);
        long taken = 0L;
        for (int slot = 0; slot < buffer.getSlots() && taken < wanted; slot++) {
            final ItemStack held = buffer.getStackInSlot(slot);
            if (!held.isEmpty() && StorageKey.of(held).equals(key)) {
                final int want = (int) Math.min(held.getCount(), wanted - taken);
                taken += buffer.extractItem(slot, want, false).getCount();
            }
        }
        if (taken == 0L) {
            throw denied(caller, what, BUFFER_HOLDS_NONE.with(name));
        }
        final NetworkInsertOperation op = mainframe().submitNetworkInsert(key, taken, label(caller));
        if (op == null) {
            returnToBuffer(key, taken);
            throw denied(caller, what, COULD_NOT_START.with("INSERT"));
        }
        op.setPriority(priorityOf(priority));
        op.onSettle(() -> {
            returnToBuffer(key, op.leftover());
            settled(caller, op);
        });
        return started(caller, what, op, SigmaCosts.SUBMIT);
    }

    /** Asks the network to craft {@code quantity} of {@code name}; the operation's id. */
    public String craft(final Caller caller, final String name, final long quantity, @Nullable final String priority)
            throws GatewayRefusedException {
        final String what = "craft " + count(quantity) + " " + name;
        operations(caller, what);
        final StorageKey key = resolve(name);
        final INetworkOperation[] made = new INetworkOperation[1];
        final INetworkOperation op = mainframe().submitCraftRequest(key, demand(quantity), true, label(caller), () -> {
            if (made[0] != null) {
                settled(caller, made[0]);
            }
        });
        if (op == null) {
            throw denied(caller, what, NO_PATTERN.with(name));
        }
        made[0] = op;
        op.setPriority(priorityOf(priority));
        return started(caller, what, op, SigmaCosts.SUBMIT);
    }

    /** One operation by id (or an unambiguous prefix), in flight or settled this past hour; null when unknown. */
    @Nullable
    public Map<String, Object> operation(final Caller caller, final String id) throws GatewayRefusedException {
        read(caller, "operation " + id);
        charge(SigmaCosts.READ);
        final MainframeBlockEntity mainframe = mainframe();
        for (final INetworkOperation live : mainframe.liveOperations()) {
            if (matches(live.operationId(), id)) {
                return luaTable(live.liveRecord());
            }
        }
        for (final OperationRecord record : mainframe.recentOperations()) {
            if (matches(record.id(), id)) {
                return luaTable(record);
            }
        }
        return null;
    }

    /** Every operation in flight on the network. */
    public List<Map<String, Object>> operations(final Caller caller) throws GatewayRefusedException {
        read(caller, "operations");
        final List<Map<String, Object>> rows = new ArrayList<>();
        for (final OperationRecord record : mainframe().activeOperationRecords()) {
            rows.add(luaTable(record));
        }
        charge(ROWS.at(rows.size(), 0));
        return rows;
    }

    /** Stops an operation in flight; whether one by that id was still running. */
    public boolean cancel(final Caller caller, final String id) throws GatewayRefusedException {
        final String what = "cancel " + id;
        operations(caller, what);
        charge(SigmaCosts.SUBMIT);
        final MainframeBlockEntity mainframe = mainframe();
        for (final INetworkOperation live : mainframe.liveOperations()) {
            if (matches(live.operationId(), id)) {
                final boolean stopped = mainframe.cancelOperation(live.operationId());
                gateway.logged(caller.label(), what, stopped ? "stopped" : WOULD_NOT_STOP.text().english(),
                        stopped ? GatewayLog.Tone.OK : GatewayLog.Tone.BUSY);
                return stopped;
            }
        }
        gateway.logged(caller.label(), what, NOT_RUNNING.text().english(), GatewayLog.Tone.BUSY);
        return false;
    }

    /** Starts a compiled program on another computer of the network, as its prompt would; the process id. */
    public int run(final Caller caller, final String computer, final String program, final List<String> args,
                   @Nullable final String priority) throws GatewayRefusedException {
        final String what = "run " + program + " on " + computer;
        operations(caller, what);
        final ServerCliComputer remote = shell.remoteShell(computer);
        if (remote == null) {
            throw denied(caller, what, NO_SUCH_COMPUTER.with(computer));
        }
        if (!remote.running()) {
            throw denied(caller, what, POWERED_OFF.with(computer));
        }
        if (!remote.remoteAllowed()) {
            throw denied(caller, what, NO_REMOTE_PROGRAMS.with(computer));
        }
        if (!(remote.machine() instanceof AbstractComputerBlockEntity machine)) {
            throw denied(caller, what, CANNOT_RUN.with(computer));
        }
        final ProgramLauncher.Launch launch = ProgramLauncher.launch(machine, program, remote::readFile,
                new ArrayList<>(args), IProgramParent.NONE, ProgramPriority.named(priority), 0);
        if (!launch.ok()) {
            throw denied(caller, what, switch (launch.refusal()) {
                case NO_RUNNER -> NO_RUNNER.with(program);
                case NO_MEMORY -> NO_MEMORY.with(computer, launch.roomMb(), launch.freeMb());
                case UNREADABLE, NOT_STARTED -> CliTexts.SAID_BY.with(computer, launch.message());
            });
        }
        gateway.stats().count(GatewayStats.Kind.OPERATION, now());
        gateway.logged(caller.label(), what, "process " + launch.id(), GatewayLog.Tone.OK);
        charge(SigmaCosts.SUBMIT);
        return launch.id();
    }

    // Watches and the log

    /** Asks to be told when the total of {@code name} changes; the total now. */
    public long watch(final Caller caller, final String name) throws GatewayRefusedException {
        read(caller, "watch " + name);
        final StorageKey key = resolve(name);
        final long total = storage().count(key);
        gateway.watch(caller.id(), nameOf(key), total);
        gateway.logged(caller.label(), "watch " + nameOf(key), "ok", GatewayLog.Tone.OK);
        charge(SigmaCosts.READ);
        return total;
    }

    /** Stops watching {@code name}; whether it was being watched. */
    public boolean unwatch(final Caller caller, final String name) throws GatewayRefusedException {
        admit(caller, "unwatch " + name);
        final boolean was = gateway.unwatch(caller.id(), nameOf(resolve(name)));
        charge(SigmaCosts.GLANCE_NETWORK);
        return was;
    }

    /** Writes a line into the Gateway's log, signed by the caller: {@code error}, {@code warn} or anything else. */
    public void log(final Caller caller, final String levelName, final String text) throws GatewayRefusedException {
        admit(caller, "log");
        final String kind = levelName == null ? "" : levelName.toLowerCase(Locale.ROOT);
        final GatewayLog.Tone tone = kind.startsWith("err") ? GatewayLog.Tone.DENIED
                : kind.startsWith("warn") ? GatewayLog.Tone.BUSY : GatewayLog.Tone.OK;
        final String line = text == null ? "" : text.length() > WHAT_LENGTH ? text.substring(0, WHAT_LENGTH) : text;
        gateway.logged(caller.label(), line, kind.isEmpty() ? "info" : kind, tone);
        charge(SigmaCosts.GLANCE_NETWORK);
    }

    // The gate every request goes through

    /** Counts the call against the cap; refused once the tick's calls are spent. */
    private void admit(final Caller caller, final String what) throws GatewayRefusedException {
        if (!gateway.admit()) {
            throw denied(caller, what, BUSY.with(gateway.permissions().callCap()));
        }
    }

    private void read(final Caller caller, final String what) throws GatewayRefusedException {
        admit(caller, what);
        if (!gateway.permissions().read()) {
            throw denied(caller, what, READING_OFF.text());
        }
    }

    private void operations(final Caller caller, final String what) throws GatewayRefusedException {
        admit(caller, what);
        if (!gateway.permissions().operations()) {
            throw denied(caller, what, OPERATIONS_OFF.text());
        }
    }

    private GatewayRefusedException denied(final Caller caller, final String what, final Text why) {
        gateway.logged(caller.label(), what, why.english(), GatewayLog.Tone.DENIED);
        return new GatewayRefusedException(why);
    }

    private void charge(final int credits) {
        gateway.charge(credits);
    }

    private long now() {
        return level.getGameTime();
    }

    private NetworkStorage storage() throws GatewayRefusedException {
        final NetworkUuid net = terminal.networkUuid();
        if (net == null) {
            throw new GatewayRefusedException(NOT_ON_NETWORK.text());
        }
        return NetworkStorage.of(level, net);
    }

    private MainframeBlockEntity mainframe() throws GatewayRefusedException {
        final MainframeBlockEntity mainframe = shell.mainframe();
        if (mainframe == null || !mainframe.isRunning()) {
            throw new GatewayRefusedException(NO_MAINFRAME.text());
        }
        return mainframe;
    }

    /** Signs an operation the way the network's log wants it: the host, then who asked through it. */
    private String label(final Caller caller) {
        return MoveLabels.via(shell.hostname(), caller.label());
    }

    private OperationPriority priorityOf(@Nullable final String name) {
        final OperationPriority asked = name == null ? OperationPriority.DEFAULT
                : OperationPriority.fromKeyword(name).orElse(OperationPriority.DEFAULT);
        return gateway.permissions().cap(asked);
    }

    private String started(final Caller caller, final String what, final INetworkOperation op, final int cost) {
        gateway.stats().count(GatewayStats.Kind.OPERATION, now());
        gateway.logged(caller.label(), what, "started", GatewayLog.Tone.BUSY);
        charge(cost);
        return op.operationId().toString();
    }

    /** An operation the caller started has settled: the log and the computer both hear how it went. */
    private void settled(final Caller caller, final INetworkOperation op) {
        final OperationRecord record = op.toRecord();
        final String status = status(record.status());
        gateway.logged(caller.label(), typeName(record.type()) + " " + record.moved() + " " + nameOf(record.key()),
                status, record.status() == OperationRecord.STATUS_COMPLETED ? GatewayLog.Tone.OK
                        : record.status() == OperationRecord.STATUS_PARTIAL ? GatewayLog.Tone.BUSY : GatewayLog.Tone.DENIED);
        gateway.eventTo(caller.id(), EVENT_OPERATION, op.operationId().toString(), status);
    }

    private void returnToBuffer(final StorageKey key, final long amount) {
        long left = amount;
        final ItemStackHandler buffer = gateway.buffer();
        for (int slot = 0; slot < buffer.getSlots() && left > 0L; slot++) {
            final int batch = (int) Math.min(left, key.prototype().getMaxStackSize());
            left -= batch - buffer.insertItem(slot, key.stack(batch), false).getCount();
        }
    }

    private static boolean matches(final UUID id, final String asked) {
        final String prefix = asked == null ? "" : asked.trim().toLowerCase(Locale.ROOT);
        return !prefix.isEmpty() && id.toString().startsWith(prefix);
    }

    private static long demand(final long quantity) {
        return quantity <= 0L ? Long.MAX_VALUE : quantity;
    }

    private static String count(final long quantity) {
        return quantity <= 0L ? "all" : Long.toString(quantity);
    }

    private static Map<String, Object> luaTable(final OperationRecord record) {
        return luaTable("id", record.id().toString(), "type", typeName(record.type()),
                "status", status(record.status()), "item", record.key() == null ? "" : nameOf(record.key()),
                "requested", record.requested(), "moved", record.moved(),
                "priority", record.priority().serializedName());
    }

    /** A table for the other side, its field names first and their values after each: names a program reads by. */
    private static Map<String, Object> luaTable(final Object... pairs) {
        final Map<String, Object> made = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            made.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return made;
    }

    /** The one word the other side gets for an operation's state, the same the prompt prints. */
    public static String status(final byte status) {
        return switch (status) {
            case OperationRecord.STATUS_COMPLETED -> "done";
            case OperationRecord.STATUS_PARTIAL -> "partial";
            case OperationRecord.STATUS_FAILED -> "failed";
            case OperationRecord.STATUS_PROCESSING -> "running";
            case OperationRecord.STATUS_WAITING -> "waiting";
            case OperationRecord.STATUS_RESOURCE_LOCKED -> "locked";
            case OperationRecord.STATUS_PENDING -> "pending";
            case OperationRecord.STATUS_DISCARDED -> "discarded";
            default -> "unknown";
        };
    }

    private static String typeName(final byte type) {
        return switch (type) {
            case OperationRecord.TYPE_SELECT -> "pull";
            case OperationRecord.TYPE_INSERT -> "push";
            case OperationRecord.TYPE_DELETE -> "delete";
            case OperationRecord.TYPE_MOVE -> "move";
            case OperationRecord.TYPE_CRAFT -> "craft";
            case OperationRecord.TYPE_ANALYZE -> "analyze";
            case OperationRecord.TYPE_REINDEX -> "reindex";
            case OperationRecord.TYPE_VACUUM -> "vacuum";
            case OperationRecord.TYPE_DROP -> "drop";
            default -> "operation";
        };
    }
}
