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
import dev.jstech.computers.cannon.CannonCosts;
import dev.jstech.computers.cannon.machine.HostFiles;
import dev.jstech.computers.cannon.machine.HostIql;
import dev.jstech.computers.cannon.machine.HostNetwork;
import dev.jstech.computers.cannon.machine.HostOperations;
import dev.jstech.computers.cannon.machine.HostProgram;
import dev.jstech.computers.cannon.machine.HostRemote;
import dev.jstech.computers.cannon.machine.MachineHost;
import dev.jstech.computers.cannon.machine.MachinePrograms;
import dev.jstech.computers.cannon.run.Halt;
import dev.jstech.computers.cannon.run.IHost;
import dev.jstech.computers.operation.INetworkOperation;
import dev.jstech.computers.operation.MoveLabels;
import dev.jstech.computers.operation.NetworkInsertOperation;
import dev.jstech.computers.operation.NetworkSelectOperation;
import dev.jstech.computers.operation.NetworkStorage;
import dev.jstech.computers.operation.payload.ComputingPayloads;
import dev.jstech.computers.operation.payload.OperationRecord;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.core.operation.OperationPriority;
import dev.jstech.core.peripheral.IPeripheralOwner;
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
 */
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
            throw new GatewayRefusedException("the Gateway is not in a world");
        }
        final IPeripheralOwner owner = gateway.owner();
        if (!(owner instanceof IComputerTerminalHost terminal) || !(owner instanceof BlockEntity)) {
            throw new GatewayRefusedException("the Gateway is not linked to a computer");
        }
        return new GatewayService(gateway, level, terminal);
    }

    // Names

    /** The key a name on the other side stands for. */
    public static StorageKey resolve(final String name) throws GatewayRefusedException {
        if (name == null || name.isBlank()) {
            throw new GatewayRefusedException("which item?");
        }
        final String given = name.trim().toLowerCase(Locale.ROOT);
        if (given.startsWith(FLUID_PREFIX)) {
            final ResourceLocation id = ResourceLocation.tryParse(given.substring(FLUID_PREFIX.length()));
            final Fluid fluid = id == null ? null : BuiltInRegistries.FLUID.getOptional(id).orElse(null);
            if (fluid == null) {
                throw new GatewayRefusedException("unknown fluid: " + name);
            }
            return StorageKey.of(new FluidStack(fluid, 1));
        }
        if (given.startsWith(CHEMICAL_PREFIX)) {
            final ResourceLocation id = ResourceLocation.tryParse(given.substring(CHEMICAL_PREFIX.length()));
            if (id == null) {
                throw new GatewayRefusedException("unknown chemical: " + name);
            }
            return StorageKey.chemical(id);
        }
        final ResourceLocation id = ResourceLocation.tryParse(given.contains(":") ? given : "minecraft:" + given);
        final Item item = id == null ? null : BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        if (item == null) {
            throw new GatewayRefusedException("unknown item: " + name);
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
        charge(CannonCosts.GLANCE_NETWORK);
        return value;
    }

    public long used(final Caller caller) throws GatewayRefusedException {
        read(caller, "used");
        final long value = shell.networkUse().stored();
        charge(CannonCosts.GLANCE_NETWORK);
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
        charge(HostNetwork.priceOf(out.size()));
        return out;
    }

    public long total(final Caller caller, final String name) throws GatewayRefusedException {
        read(caller, "total " + name);
        final long value = storage().count(resolve(name));
        charge(CannonCosts.READ);
        return value;
    }

    /** Which servers hold {@code name}, and how much each. */
    public List<Map<String, Object>> find(final Caller caller, final String name) throws GatewayRefusedException {
        read(caller, "find " + name);
        final List<Map<String, Object>> rows = new ArrayList<>();
        for (final Map.Entry<NodeUuid, Long> entry : storage().breakdown(resolve(name)).entrySet()) {
            if (entry.getValue() > 0L) {
                rows.add(row("server", ComputingPayloads.serverLabel(level, entry.getKey()), "quantity", entry.getValue()));
            }
        }
        charge(HostNetwork.priceOf(rows.size()));
        return rows;
    }

    public List<Map<String, Object>> servers(final Caller caller) throws GatewayRefusedException {
        read(caller, "servers");
        final List<Map<String, Object>> rows = new ArrayList<>();
        for (final ICliComputer.ServerUse server : shell.servers()) {
            rows.add(row("name", server.name(), "used", server.stored(), "capacity", server.capacity(), "online", true));
        }
        charge(HostNetwork.priceOf(rows.size()));
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
            rows.add(row("name", host.hostname(), "label", host.name(), "os", host.os(), "type", host.type(),
                    "online", host.running(), "shares", sharesByHost.getOrDefault(host.hostname(), List.of())));
        }
        charge(HostNetwork.priceOf(rows.size()));
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
            throw denied(caller, what, "could not start the SELECT");
        }
        op.setPriority(priorityOf(priority));
        op.abortWhen(gateway::isRemoved);
        op.onSettle(() -> settled(caller, op));
        return started(caller, what, op, CannonCosts.SUBMIT);
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
            throw denied(caller, what, "the buffer holds no " + name);
        }
        final NetworkInsertOperation op = mainframe().submitNetworkInsert(key, taken, label(caller));
        if (op == null) {
            returnToBuffer(key, taken);
            throw denied(caller, what, "could not start the INSERT");
        }
        op.setPriority(priorityOf(priority));
        op.onSettle(() -> {
            returnToBuffer(key, op.leftover());
            settled(caller, op);
        });
        return started(caller, what, op, CannonCosts.SUBMIT);
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
            throw denied(caller, what, "no pattern crafts " + name);
        }
        made[0] = op;
        op.setPriority(priorityOf(priority));
        return started(caller, what, op, CannonCosts.SUBMIT);
    }

    /** One operation by id (or an unambiguous prefix), in flight or settled this past hour; null when unknown. */
    @Nullable
    public Map<String, Object> operation(final Caller caller, final String id) throws GatewayRefusedException {
        read(caller, "operation " + id);
        charge(CannonCosts.READ);
        final MainframeBlockEntity mainframe = mainframe();
        for (final INetworkOperation live : mainframe.liveOperations()) {
            if (matches(live.operationId(), id)) {
                return row(live.liveRecord());
            }
        }
        for (final OperationRecord record : mainframe.recentOperations()) {
            if (matches(record.id(), id)) {
                return row(record);
            }
        }
        return null;
    }

    /** Every operation in flight on the network. */
    public List<Map<String, Object>> operations(final Caller caller) throws GatewayRefusedException {
        read(caller, "operations");
        final List<Map<String, Object>> rows = new ArrayList<>();
        for (final OperationRecord record : mainframe().activeOperationRecords()) {
            rows.add(row(record));
        }
        charge(HostNetwork.priceOf(rows.size()));
        return rows;
    }

    /** Stops an operation in flight; whether one by that id was still running. */
    public boolean cancel(final Caller caller, final String id) throws GatewayRefusedException {
        final String what = "cancel " + id;
        operations(caller, what);
        charge(CannonCosts.SUBMIT);
        final MainframeBlockEntity mainframe = mainframe();
        for (final INetworkOperation live : mainframe.liveOperations()) {
            if (matches(live.operationId(), id)) {
                final boolean stopped = mainframe.cancelOperation(live.operationId());
                gateway.logged(caller.label(), what, stopped ? "stopped" : "would not stop",
                        stopped ? GatewayLog.Tone.OK : GatewayLog.Tone.BUSY);
                return stopped;
            }
        }
        gateway.logged(caller.label(), what, "not running", GatewayLog.Tone.BUSY);
        return false;
    }

    /** Starts a compiled program on another computer of the network, as its prompt would; the process id. */
    public int run(final Caller caller, final String computer, final String program, final List<String> args,
                   @Nullable final String priority) throws GatewayRefusedException {
        final String what = "run " + program + " on " + computer;
        operations(caller, what);
        final ServerCliComputer remote = shell.remoteShell(computer);
        if (remote == null) {
            throw denied(caller, what, computer + ": no such computer on this network");
        }
        if (!remote.running()) {
            throw denied(caller, what, computer + " is powered off");
        }
        if (!remote.remoteAllowed()) {
            throw denied(caller, what, computer + " does not take programs from other computers");
        }
        if (!(remote.machine() instanceof AbstractComputerBlockEntity machine)) {
            throw denied(caller, what, computer + " cannot run programs");
        }
        final int dot = program.lastIndexOf('.');
        final String extension = dot < 0 ? "" : program.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (dev.jstech.core.JsCore.languages().runnerOf(extension) == null) {
            throw denied(caller, what, program + ": nothing installed runs a program of this kind");
        }
        final ICliComputer.FsResult read = remote.readFile(program);
        if (!read.ok()) {
            throw denied(caller, what, computer + ": " + read.message());
        }
        final int room = MachinePrograms.DEFAULT_HEAP_MB;
        if (!machine.ramLedger().fits(room)) {
            throw denied(caller, what, computer + ": " + room + " MB will not fit in " + machine.ramLedger().freeMb()
                    + " MB of free memory");
        }
        final int slash = Math.max(program.lastIndexOf('\\'), program.lastIndexOf('/'));
        final String name = slash < 0 ? program : program.substring(slash + 1);
        final String level = priority == null || priority.isBlank() ? MachinePrograms.DEFAULT_PRIORITY
                : priority.toLowerCase(Locale.ROOT);
        final MachinePrograms.Started started = machine.cannon().start(name, read.message(), room, machine,
                new ArrayList<>(args), 0, level);
        if (!started.ok()) {
            throw denied(caller, what, computer + ": " + started.message());
        }
        machine.setChanged();
        gateway.stats().count(GatewayStats.Kind.OPERATION, now());
        gateway.logged(caller.label(), what, "process " + started.id(), GatewayLog.Tone.OK);
        charge(CannonCosts.SUBMIT);
        return started.id();
    }

    // Watches and the log

    /** Asks to be told when the total of {@code name} changes; the total now. */
    public long watch(final Caller caller, final String name) throws GatewayRefusedException {
        read(caller, "watch " + name);
        final StorageKey key = resolve(name);
        final long total = storage().count(key);
        gateway.watch(caller.id(), nameOf(key), total);
        gateway.logged(caller.label(), "watch " + nameOf(key), "ok", GatewayLog.Tone.OK);
        charge(CannonCosts.READ);
        return total;
    }

    /** Stops watching {@code name}; whether it was being watched. */
    public boolean unwatch(final Caller caller, final String name) throws GatewayRefusedException {
        admit(caller, "unwatch " + name);
        final boolean was = gateway.unwatch(caller.id(), nameOf(resolve(name)));
        charge(CannonCosts.GLANCE_NETWORK);
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
        charge(CannonCosts.GLANCE_NETWORK);
    }

    // The gate every request goes through

    /** Counts the call against the cap; refused once the tick's calls are spent. */
    private void admit(final Caller caller, final String what) throws GatewayRefusedException {
        if (!gateway.admit()) {
            throw denied(caller, what, "busy: " + gateway.permissions().callCap() + " calls a tick");
        }
    }

    private void read(final Caller caller, final String what) throws GatewayRefusedException {
        admit(caller, what);
        if (!gateway.permissions().read()) {
            throw denied(caller, what, "denied: reading the network is off");
        }
    }

    private void operations(final Caller caller, final String what) throws GatewayRefusedException {
        admit(caller, what);
        if (!gateway.permissions().operations()) {
            throw denied(caller, what, "denied: operations are off");
        }
    }

    private GatewayRefusedException denied(final Caller caller, final String what, final String why) {
        gateway.logged(caller.label(), what, why, GatewayLog.Tone.DENIED);
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
            throw new GatewayRefusedException("the computer is not on a network");
        }
        return NetworkStorage.of(level, net);
    }

    private MainframeBlockEntity mainframe() throws GatewayRefusedException {
        final MainframeBlockEntity mainframe = shell.mainframe();
        if (mainframe == null || !mainframe.isRunning()) {
            throw new GatewayRefusedException("the network has no running Mainframe");
        }
        return mainframe;
    }

    /** Signs an operation the way the network's log wants it: the host, then who asked through it. */
    private String label(final Caller caller) {
        return MoveLabels.via(shell.hostname(), caller.label());
    }

    private OperationPriority priorityOf(@Nullable final String name) {
        OperationPriority asked = OperationPriority.DEFAULT;
        if (name != null && !name.isBlank()) {
            try {
                asked = OperationPriority.valueOf(name.trim().toUpperCase(Locale.ROOT));
            } catch (final IllegalArgumentException notAPriority) {
                asked = OperationPriority.DEFAULT;
            }
        }
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

    private static Map<String, Object> row(final OperationRecord record) {
        return row("id", record.id().toString(), "type", typeName(record.type()), "status", status(record.status()),
                "item", record.key() == null ? "" : nameOf(record.key()), "requested", record.requested(),
                "moved", record.moved(), "priority", record.priority().name().toLowerCase(Locale.ROOT));
    }

    private static Map<String, Object> row(final Object... pairs) {
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
