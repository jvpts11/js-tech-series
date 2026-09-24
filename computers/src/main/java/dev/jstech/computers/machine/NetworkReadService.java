/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.operation.NetworkStorage;
import dev.jstech.computers.operation.payload.OperationRecord;
import dev.jstech.computers.operation.payload.network.NetworkLookup;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.program.cli.ICliRemote;
import dev.jstech.computers.program.iql.IIqlCondition;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.core.network.NetworkSystem;
import dev.jstech.core.network.ServerNode;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.util.ShortId;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.core.uuid.NodeUuid;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The data network a machine is on, as what runs on the machine reads it.
 *
 * <p>Every read comes from the index the network already keeps, so none of it submits an Operation or waits on
 * anything: whoever asks what the network holds is told in the same tick. A machine with no cable is not on a
 * network, and says so rather than pretending.
 */
@TextHolder
public final class NetworkReadService {

    /**
     * How many rows one read may gather. It is not a cap on the answer, which is why it is set far past any real
     * network: whoever asks for every kind of thing there is is handed all of them, and pays for all of them.
     */
    private static final int EVERYTHING = 1_000_000;

    /** How many ways of making a thing, and things it goes into, one answer names before it stops. */
    private static final int MOST_RECIPE_LINES = 12;

    /* The rows a query of the network's machines and Operations reads back; names and keywords are data. */
    private static final TextKey ON_SERVERS = TextKey.of("jsc.network.read.on_servers", "%s servers");
    private static final TextKey MAINFRAME = TextKey.of("jsc.network.read.mainframe", "Mainframe");
    private static final TextKey A_SERVER = TextKey.of("jsc.network.read.server", "%s (server)");
    private static final TextKey A_PC = TextKey.of("jsc.network.read.pc", "%s (pc)");
    private static final TextKey A_CRAFTING_COMPUTER = TextKey.of("jsc.network.read.crafting", "%s (crafting)");
    /* An Operation: its verb, what it moves and how it stands. */
    private static final TextKey OPERATION = TextKey.of("jsc.network.read.operation", "%s %s [%s]");

    private final IComputerTerminalHost terminal;
    private final ServerLevel level;
    /** The other machines of the network as the machine's shell reaches them. */
    private final ICliRemote remote;
    /** The network's work, for the rows a query asks of it: they belong to whoever owns the Operations. */
    private final OperationsService operations;

    public NetworkReadService(final IComputerTerminalHost terminal, final ServerLevel level,
                              final ICliRemote remote, final OperationsService operations) {
        this.terminal = terminal;
        this.level = level;
        this.remote = remote;
        this.operations = operations;
    }

    /** Whether the machine is on a network at all. */
    public boolean online() {
        return this.terminal.networkUuid() != null;
    }

    /** The short id of the network the machine is on, or {@code ""} when it is on none. */
    public String id() {
        final NetworkUuid net = this.terminal.networkUuid();
        return net == null ? "" : ShortId.of(net.asString());
    }

    /** The network the machine is on, by its short id, or null when it is on none. */
    @Nullable
    public String current() {
        final NetworkUuid net = this.terminal.networkUuid();
        return net == null ? null : ShortId.of(net.asString());
    }

    /** Whether the machine is itself the network's Mainframe, which is what gates the maintenance commands. */
    public boolean isMainframe() {
        return this.terminal.isMainframeHost();
    }

    /** The counts that describe the network at a glance: its servers, computers, kinds of thing and Mainframe. */
    public ICliComputer.NetSummary summary() {
        final NetworkUuid net = this.terminal.networkUuid();
        if (net == null) {
            return new ICliComputer.NetSummary(false, 0, 0, 0, 0, false);
        }
        final NetworkSystem system = NetworkSystem.get(this.level);
        final MainframeBlockEntity mainframe = this.mainframe(net);
        final int types = mainframe == null ? 0 : mainframe.networkIndex().catalogSize();
        return new ICliComputer.NetSummary(true, system.serversOf(net).size(),
                system.personalComputersOf(net).size(), system.subframesOf(net).size(), types, mainframe != null);
    }

    /** What the whole network holds and could hold, under the network's own short id. */
    public ICliComputer.ServerUse use() {
        final NetworkUuid net = this.terminal.networkUuid();
        if (net == null) {
            return new ICliComputer.ServerUse("", 0L, 0L);
        }
        final NetworkStorage storage = NetworkStorage.of(this.level, net);
        return new ICliComputer.ServerUse(this.id(), storage.used(), storage.capacity());
    }

    /** How much the network's servers can hold in all. */
    public long capacity() {
        return this.use().capacity();
    }

    /** How much the network's servers hold. */
    public long used() {
        return this.use().stored();
    }

    /** How much of an item the whole network holds, counting every server that has any. */
    public long total(final String item) {
        long sum = 0;
        for (final ICliComputer.Holding holding : this.find(item)) {
            sum += holding.quantity();
        }
        return sum;
    }

    /** Every kind of thing the network holds, by its English name, which is what a program is handed. */
    public List<String> types() {
        final List<String> names = new ArrayList<>();
        for (final ICliComputer.StoredItem item : this.query(null, "", EVERYTHING)) {
            names.add(item.name().english());
        }
        return names;
    }

    /**
     * What the network holds, item by item, filtered by a condition and scoped to one server by name.
     *
     * @param where  the condition an item has to meet, or null for every item
     * @param server a server name to scope the read to, or {@code ""} for the whole network
     * @param limit  the largest number of rows to hand back
     */
    public List<ICliComputer.StoredItem> query(@Nullable final IIqlCondition where, final String server,
                                               final int limit) {
        final NetworkUuid net = this.terminal.networkUuid();
        if (net == null) {
            return List.of();
        }
        /*
         * WHERE server=X scopes the read to that server; every other field is evaluated per item, so the
         * full condition (qty < 100, name contains "ore", damaged = true, ...) really filters now.
         */
        final String serverName = (server == null || server.isBlank())
                ? IIqlCondition.firstValue(where, "server")
                : server;
        final NetworkStorage storage;
        final String scopedServer;
        if (serverName == null || serverName.isBlank()) {
            storage = NetworkStorage.of(this.level, net);
            scopedServer = "";
        } else {
            final NodeUuid scoped = this.serverNamed(net, serverName);
            if (scoped == null) {
                return List.of(); // a WHERE server that names no server yields nothing
            }
            storage = NetworkStorage.ofServers(this.level, List.of(scoped));
            scopedServer = serverName;
        }
        /*
         * Filter by the condition, then sort by quantity and take the top rows: the limit applies after the
         * sort so the result is the largest holdings, not an arbitrary slice.
         */
        return storage.query().entrySet().stream()
                .filter(entry -> where == null
                        || where.matches(rowOf(entry.getKey(), entry.getValue(), scopedServer)))
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(Math.max(limit, 0))
                .map(entry -> new ICliComputer.StoredItem(GameText.of(entry.getKey().displayName()), entry.getValue(),
                        this.location(net, entry.getKey(), scopedServer)))
                .toList();
    }

    /** Where an item lives: the scoped server, the single server holding it, or "N servers" across the net. */
    private Text location(final NetworkUuid net, final StorageKey key, final String scopedServer) {
        if (!scopedServer.isEmpty()) {
            return Text.literal(scopedServer);
        }
        final Map<NodeUuid, Long> breakdown = NetworkStorage.of(this.level, net).breakdown(key);
        if (breakdown.size() == 1) {
            return Text.literal(NetworkLookup.serverLabel(this.level, breakdown.keySet().iterator().next()));
        }
        return ON_SERVERS.with(breakdown.size());
    }

    /** The server of this network that a name picks out, or null when no server carries that label. */
    @Nullable
    public NodeUuid serverNamed(final NetworkUuid net, final String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        for (final ServerNode server : NetworkSystem.get(this.level).serversOf(net)) {
            if (NetworkLookup.serverLabel(this.level, server.nodeUuid()).equalsIgnoreCase(name)) {
                return server.nodeUuid();
            }
        }
        return null;
    }

    /** The fields a WHERE can test on an item row: item id, name, qty, server (scoped), damaged, durability. */
    private static Function<String, String> rowOf(final StorageKey key, final long qty, final String scopedServer) {
        return field -> switch (field.toLowerCase(Locale.ROOT)) {
            case "item" -> itemPath(key);
            case "name" -> key.displayName().getString();
            case "qty", "count", "amount" -> Long.toString(qty);
            case "server" -> scopedServer;
            case "damaged" -> Boolean.toString(key.stack(1).isDamaged());
            case "durability" -> durabilityPercent(key);
            default -> null; // an unknown field makes its comparison false, so the row is excluded
        };
    }

    private static String itemPath(final StorageKey key) {
        return BuiltInRegistries.ITEM.getKey(key.item()).getPath();
    }

    private static String durabilityPercent(final StorageKey key) {
        final ItemStack stack = key.stack(1);
        if (!stack.isDamageableItem() || stack.getMaxDamage() == 0) {
            return "100";
        }
        return Long.toString(Math.round(
                100.0 * (stack.getMaxDamage() - stack.getDamageValue()) / stack.getMaxDamage()));
    }

    /**
     * The rows a {@code QUERY <object>} asks of the network: {@code items} (what it holds), {@code servers},
     * {@code operations}, {@code computers} and {@code recipes}. An object nothing answers yet reads as no rows.
     *
     * @param object the name of what is being asked about
     * @param where  the condition a row has to meet, or null for every row
     * @param server a server name to scope the read to, or {@code ""} for the whole network
     * @param limit  the largest number of rows to hand back
     */
    public List<ICliComputer.StoredItem> queryObject(final String object, @Nullable final IIqlCondition where,
                                                     final String server, final int limit) {
        return switch (object.toLowerCase(Locale.ROOT)) {
            case "items", "*" -> this.query(where, server, limit); // '*' means every item, like SELECT *
            case "servers" -> this.queryServers(limit);
            case "operations" -> this.queryOperations(limit);
            case "computers" -> this.queryComputers(limit);
            case "recipes" -> this.queryRecipes(limit);
            // disks: the schema object exists, the per-disk live data is not wired yet.
            default -> List.of();
        };
    }

    /** One row per network node: the Mainframe, then servers, personal computers, and crafting computers. */
    private List<ICliComputer.StoredItem> queryComputers(final int limit) {
        final NetworkUuid net = this.terminal.networkUuid();
        if (net == null) {
            return List.of();
        }
        final NetworkSystem system = NetworkSystem.get(this.level);
        final List<ICliComputer.StoredItem> out = new ArrayList<>();
        if (this.mainframe(net) != null) {
            out.add(new ICliComputer.StoredItem(MAINFRAME.text(), 1L));
        }
        for (final ServerNode server : system.serversOf(net)) {
            if (out.size() >= limit) {
                break;
            }
            out.add(new ICliComputer.StoredItem(
                    A_SERVER.with(NetworkLookup.serverLabel(this.level, server.nodeUuid())), 1L));
        }
        for (final var pc : system.personalComputersOf(net)) {
            if (out.size() >= limit) {
                break;
            }
            out.add(new ICliComputer.StoredItem(A_PC.with("PC-" + ShortId.of(pc.nodeUuid().asString())), 1L));
        }
        for (final var cc : system.craftingComputersOf(net)) {
            if (out.size() >= limit) {
                break;
            }
            out.add(new ICliComputer.StoredItem(
                    A_CRAFTING_COMPUTER.with("CC-" + ShortId.of(cc.nodeUuid().asString())), 1L));
        }
        return out;
    }

    /** One row per craftable recipe known to the network: the result item and its output count. */
    private List<ICliComputer.StoredItem> queryRecipes(final int limit) {
        final MainframeBlockEntity mainframe = this.mainframe(this.terminal.networkUuid());
        if (mainframe == null) {
            return List.of();
        }
        final List<ICliComputer.StoredItem> out = new ArrayList<>();
        for (final var pattern : mainframe.networkPatterns()) {
            if (out.size() >= limit) {
                break;
            }
            final ItemStack result = pattern.result();
            out.add(new ICliComputer.StoredItem(GameText.of(result.getHoverName()), result.getCount()));
        }
        return out;
    }

    /** One row per server: its label and the total item count it stores. */
    private List<ICliComputer.StoredItem> queryServers(final int limit) {
        final NetworkUuid net = this.terminal.networkUuid();
        if (net == null) {
            return List.of();
        }
        final List<ICliComputer.StoredItem> out = new ArrayList<>();
        for (final ServerNode srv : NetworkSystem.get(this.level).serversOf(net)) {
            final long used = NetworkStorage.ofServers(this.level, List.of(srv.nodeUuid()))
                    .query().values().stream().mapToLong(Long::longValue).sum();
            out.add(new ICliComputer.StoredItem(NetworkLookup.serverLabel(this.level, srv.nodeUuid()), used));
            if (out.size() >= limit) {
                break;
            }
        }
        return out;
    }

    /**
     * One row per operation: the in-flight ones first ("VERB item [STATUS]" and how much moved so far), then
     * the settled ones from the Mainframe's log, newest first, so a craft that finished a moment ago is still
     * there to be read. The ones in flight are the Operations service's to hand over, because they are its own.
     */
    private List<ICliComputer.StoredItem> queryOperations(final int limit) {
        final List<ICliComputer.StoredItem> out = new ArrayList<>();
        for (final ICliComputer.ActiveOp op : this.operations.list()) {
            out.add(new ICliComputer.StoredItem(OPERATION.with(op.type(), op.item(), op.status()), op.progress()));
            if (out.size() >= limit) {
                return out;
            }
        }
        final MainframeBlockEntity mainframe = this.mainframe(this.terminal.networkUuid());
        if (mainframe != null) {
            for (final OperationRecord record : mainframe.recentOperations()) {
                out.add(new ICliComputer.StoredItem(OPERATION.with(OperationRecord.typeName(record.type()),
                        GameText.of(record.name()), OperationRecord.statusName(record.status())), record.moved()));
                if (out.size() >= limit) {
                    break;
                }
            }
        }
        return out;
    }

    /** Which servers hold an item, and how much each holds; a server holding none of it is left out. */
    public List<ICliComputer.Holding> find(final String item) {
        final NetworkUuid net = this.terminal.networkUuid();
        final StorageKey key = StorageKey.byName(item);
        if (net == null || key == null) {
            return List.of();
        }
        final Map<NodeUuid, Long> perServer = NetworkStorage.of(this.level, net).breakdown(key);
        final List<ICliComputer.Holding> rows = new ArrayList<>();
        for (final Map.Entry<NodeUuid, Long> entry : perServer.entrySet()) {
            if (entry.getValue() > 0L) {
                rows.add(new ICliComputer.Holding(NetworkLookup.serverLabel(this.level, entry.getKey()),
                        entry.getValue()));
            }
        }
        return rows;
    }

    /**
     * What a name a player typed could stand for, best first.
     *
     * <p>What the network is holding is what the name is looked for in, so the count comes back with the name
     * and a listing of what fits reads as a listing of the network.
     */
    public List<ICliComputer.ItemMatch> matching(final String text) {
        final NetworkUuid net = this.terminal.networkUuid();
        final Map<StorageKey, Long> stored = net == null
                ? Map.of() : NetworkStorage.of(this.level, net).query();
        final List<ICliComputer.ItemMatch> out = new ArrayList<>();
        for (final StorageKey key : ItemNames.matching(text, stored)) {
            out.add(new ICliComputer.ItemMatch(key.registryId().toString(),
                    key.displayName().getString(), stored.getOrDefault(key, 0L)));
        }
        return out;
    }

    /** Everything known about one thing the network holds: where it is, what makes it, what it goes into. */
    public ICliComputer.ItemDetail itemDetail(final String id) {
        final NetworkUuid net = this.terminal.networkUuid();
        final StorageKey key = StorageKey.byName(id);
        if (net == null || key == null) {
            return new ICliComputer.ItemDetail("", id, 0L, List.of(), List.of(), List.of());
        }
        final NetworkStorage storage = NetworkStorage.of(this.level, net);
        final MainframeBlockEntity mainframe = this.mainframe(net);
        return new ICliComputer.ItemDetail(key.displayName().getString(), key.registryId().toString(),
                storage.count(key), this.find(id),
                ItemRecipes.madeBy(mainframe, key, MOST_RECIPE_LINES),
                ItemRecipes.usedIn(mainframe, key, MOST_RECIPE_LINES));
    }

    /** The network's servers, with what each holds and can hold. */
    public List<ICliComputer.ServerUse> servers() {
        final NetworkUuid net = this.terminal.networkUuid();
        if (net == null) {
            return List.of();
        }
        final NetworkStorage storage = NetworkStorage.of(this.level, net);
        final List<ICliComputer.ServerUse> rows = new ArrayList<>();
        for (final ServerNode server : NetworkSystem.get(this.level).serversOf(net)) {
            rows.add(new ICliComputer.ServerUse(NetworkLookup.serverLabel(this.level, server.nodeUuid()),
                    storage.usedOf(server.nodeUuid()), storage.capacityOf(server.nodeUuid())));
        }
        return rows;
    }

    /** The other computers the machine can reach on its network. */
    public List<ICliComputer.RemoteHost> computers() {
        return this.remote.reachableHosts();
    }

    /** The computer that name picks out, by its host name or by the name its owner gave it, or null. */
    @Nullable
    public ICliComputer.RemoteHost computer(final String name) {
        for (final ICliComputer.RemoteHost host : this.remote.reachableHosts()) {
            if (host.hostname().equalsIgnoreCase(name) || host.name().equalsIgnoreCase(name)) {
                return host;
            }
        }
        return null;
    }

    /**
     * How much of an item the whole network holds, for the programs watching it: one count across the network's
     * stores, since nothing needs building for a number that is only added up. Off a network everything reads as
     * none, so a watch on a machine with no cable simply never goes off.
     */
    public long stock(final String item) {
        final NetworkUuid net = this.terminal.networkUuid();
        final StorageKey key = StorageKey.byName(item);
        return net == null || key == null ? 0L : NetworkStorage.of(this.level, net).count(key);
    }

    /** The Mainframe of the network, or null when it has none running. */
    @Nullable
    private MainframeBlockEntity mainframe(@Nullable final NetworkUuid net) {
        if (net == null) {
            return null;
        }
        return NetworkSystem.get(this.level).mainframePositionOf(net)
                .map(pos -> this.level.getBlockEntity(BlockPos.of(pos)) instanceof MainframeBlockEntity mf ? mf : null)
                .orElse(null);
    }
}
