/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.block.CraftingComputerBlock;
import dev.jstech.computers.block.MainframeBlock;
import dev.jstech.computers.block.PersonalComputerBlock;
import dev.jstech.computers.block.ServerRackBlock;
import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.crafting.NetworkRecipe;
import dev.jstech.computers.item.DiskItem;
import dev.jstech.computers.operation.NetworkStorage;
import dev.jstech.computers.operation.payload.OperationRecord;
import dev.jstech.computers.operation.payload.crafting.CraftPlaces;
import dev.jstech.computers.operation.payload.network.NetworkLookup;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.program.iql.IIqlCondition;
import dev.jstech.computers.program.iql.IqlTable;
import dev.jstech.computers.storage.DriveVolumes;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.core.network.NetworkSystem;
import dev.jstech.core.network.ServerNode;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.text.TextLists;
import dev.jstech.core.util.Loaded;
import dev.jstech.core.util.ShortId;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.core.uuid.NodeUuid;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

/**
 * The tables of the network's language with every column they have: what the network holds, its servers, their
 * disks, its Operations, its recipes and its computers. A read filters on any column, and on the few fields a table
 * keeps without showing them, sorts by any column and stops at its limit, so a WHERE, an ORDER BY and a LIMIT mean
 * the same thing on every table.
 */
@TextHolder
public final class IqlTables {

    /** What the network holds: the item, how many, where, its first tag and its enchantments. */
    public static final List<String> ITEMS = List.of("item", "qty", "server", "tag", "enchant");
    /** The servers: the name, what they hold, the room they have left and their age. */
    public static final List<String> SERVERS = List.of("name", "used", "free", "era");
    /** The servers' disks: the disk, its server, its kind, how many items it holds and how many it has. */
    public static final List<String> DISKS = List.of("disk", "server", "tier", "size", "used");
    /** The Operations in flight, then the log: the id, the verb, the state, the item and how far it went. */
    public static final List<String> OPERATIONS = List.of("id", "verb", "state", "item", "moved", "requested",
            "priority");
    /** The recipes the network's Crafting Computers hold: what they make, where, how, and the chance. */
    public static final List<String> RECIPES = List.of("recipe", "where", "machine", "chance");
    /** The computers on the network: the name, the kind, the age, whether it runs and its system. */
    public static final List<String> COMPUTERS = List.of("name", "kind", "era", "state", "os");
    /** The tables, in the order a studio lists them. */
    public static final List<String> TABLES = List.of("items", "servers", "disks", "operations", "recipes",
            "computers");

    private static final TextKey ON_SERVERS = TextKey.of("jsc.iql.table.on_servers", "%s servers");
    private static final TextKey MAINFRAME = TextKey.of("jsc.iql.table.mainframe", "Mainframe");
    private static final TextKey SERVER = TextKey.of("jsc.iql.table.server", "Server");
    private static final TextKey PERSONAL_COMPUTER = TextKey.of("jsc.iql.table.personal_computer",
            "Personal Computer");
    private static final TextKey CRAFTING_COMPUTER = TextKey.of("jsc.iql.table.crafting_computer",
            "Crafting Computer");
    private static final TextKey RUNNING = TextKey.of("jsc.iql.table.running", "running");
    private static final TextKey OFF = TextKey.of("jsc.iql.table.off", "off");
    private static final TextKey BENCH = TextKey.of("jsc.iql.table.bench", "bench");
    private static final TextKey MACHINE = TextKey.of("jsc.iql.table.machine", "machine");
    private static final TextKey STAGES = TextKey.of("jsc.iql.table.stages", "stages");
    /** The number a row is limited to when the caller asks for none. */
    private static final int MOST_ROWS = 4096;

    private IqlTables() {
    }

    /**
     * The rows of {@code object} on {@code network} with all its columns, the ones {@code where} keeps, sorted by
     * {@code orderBy} when there is one and at most {@code limit}; {@link IqlTable#NONE} for a name no table has.
     *
     * @param server a server's name to scope what is held to, or empty for the whole network
     */
    public static IqlTable read(final ServerLevel level, @Nullable final NetworkUuid network,
                                final OperationsService operations, final String object,
                                @Nullable final IIqlCondition where, final String server, final int limit,
                                final String orderBy, final boolean descending) {
        final String table = object.toLowerCase(Locale.ROOT);
        final List<String> columns = columnsOf(table);
        if (columns == null) {
            return IqlTable.NONE;
        }
        if (network == null) {
            return new IqlTable(columns, List.of());
        }
        final List<Map<String, Text>> rows = switch (table) {
            case "items", "*" -> items(level, network, server);
            case "servers" -> servers(level, network);
            case "disks" -> disks(level, network);
            case "operations" -> operations(level, network, operations);
            case "recipes" -> recipes(level, network);
            default -> computers(level, network);
        };
        final List<Map<String, Text>> kept = new ArrayList<>();
        for (final Map<String, Text> row : rows) {
            if (where == null || where.matches(field -> fieldOf(row, field))) {
                kept.add(row);
            }
        }
        if (orderBy != null && !orderBy.isBlank()) {
            final String field = orderBy.toLowerCase(Locale.ROOT);
            final Comparator<Map<String, Text>> order = Comparator.comparing(row -> fieldOf(row, field),
                    IqlService::compareFields);
            kept.sort(descending ? order.reversed() : order);
        }
        final int most = limit > 0 ? limit : MOST_ROWS;
        final List<List<Text>> cells = new ArrayList<>(Math.min(most, kept.size()));
        for (final Map<String, Text> row : kept) {
            if (cells.size() >= most) {
                break;
            }
            final List<Text> line = new ArrayList<>(columns.size());
            for (final String column : columns) {
                line.add(row.getOrDefault(column, Text.EMPTY));
            }
            cells.add(line);
        }
        return new IqlTable(columns, cells);
    }

    /**
     * How many rows {@code table} holds on {@code network} now: what a studio's Object Explorer shows beside it. The
     * tables that are long and costly to read row by row are counted where they are kept instead.
     */
    public static int count(final ServerLevel level, final NetworkUuid network, final String table) {
        return switch (table.toLowerCase(Locale.ROOT)) {
            case "items" -> NetworkStorage.of(level, network).query().size();
            case "servers" -> NetworkSystem.get(level).serversOf(network).size();
            case "disks" -> disks(level, network).size();
            case "operations" -> {
                final MainframeBlockEntity mainframe = NetworkLookup.resolveMainframe(level, network);
                yield mainframe == null ? 0 : mainframe.liveOperations().size()
                        + mainframe.recentOperations().size();
            }
            case "recipes" -> recipes(level, network).size();
            case "computers" -> computers(level, network).size();
            default -> 0;
        };
    }

    /** The columns of the table {@code table} names, or null when no table has that name. */
    @Nullable
    public static List<String> columnsOf(final String table) {
        return switch (table.toLowerCase(Locale.ROOT)) {
            case "items", "*" -> ITEMS;
            case "servers" -> SERVERS;
            case "disks" -> DISKS;
            case "operations" -> OPERATIONS;
            case "recipes" -> RECIPES;
            case "computers" -> COMPUTERS;
            default -> null;
        };
    }

    /* A field of a row as a WHERE and an ORDER BY compare it: what it says in English, or null when it has none. */
    @Nullable
    private static String fieldOf(final Map<String, Text> row, final String field) {
        final Text value = row.get(field.toLowerCase(Locale.ROOT));
        return value == null ? null : value.english();
    }

    private static List<Map<String, Text>> items(final ServerLevel level, final NetworkUuid network,
                                                 final String server) {
        NodeUuid scope = null;
        if (!server.isEmpty()) {
            for (final ServerNode node : NetworkSystem.get(level).serversOf(network)) {
                if (NetworkLookup.serverLabel(level, node.nodeUuid()).equalsIgnoreCase(server)) {
                    scope = node.nodeUuid();
                }
            }
            if (scope == null) {
                return List.of();
            }
        }
        final NetworkStorage storage = scope == null ? NetworkStorage.of(level, network)
                : NetworkStorage.ofServers(level, List.of(scope));
        final NetworkStorage whole = NetworkStorage.of(level, network);
        final List<Map<String, Text>> rows = new ArrayList<>();
        for (final Map.Entry<StorageKey, Long> entry : storage.query().entrySet()) {
            final StorageKey key = entry.getKey();
            final ItemStack stack = key.stack(1);
            final Map<String, Text> row = new LinkedHashMap<>();
            row.put("item", Text.literal(key.registryId().getPath()));
            row.put("qty", Text.literal(Long.toString(entry.getValue())));
            row.put("server", scope == null ? location(level, whole, key) : Text.literal(server));
            row.put("tag", Text.literal(firstTag(stack)));
            row.put("enchant", enchantments(stack));
            row.put("name", GameText.of(stack.getHoverName()));
            row.put("count", row.get("qty"));
            row.put("amount", row.get("qty"));
            row.put("damaged", Text.literal(Boolean.toString(stack.isDamaged())));
            row.put("durability", Text.literal(durability(stack)));
            rows.add(row);
        }
        return rows;
    }

    private static List<Map<String, Text>> servers(final ServerLevel level, final NetworkUuid network) {
        final NetworkStorage storage = NetworkStorage.of(level, network);
        final List<Map<String, Text>> rows = new ArrayList<>();
        for (final ServerNode node : NetworkSystem.get(level).serversOf(network)) {
            final long used = storage.usedOf(node.nodeUuid());
            final long capacity = storage.capacityOf(node.nodeUuid());
            final Map<String, Text> row = new LinkedHashMap<>();
            row.put("name", Text.literal(NetworkLookup.serverLabel(level, node.nodeUuid())));
            row.put("used", Text.literal(Long.toString(used)));
            row.put("free", Text.literal(Long.toString(Math.max(0L, capacity - used))));
            final ServerRackBlockEntity rack = rackOf(level, node.nodeUuid());
            row.put("era", rack != null && rack.getBlockState().getBlock() instanceof ServerRackBlock block
                    ? block.era().text() : Text.EMPTY);
            rows.add(row);
        }
        return rows;
    }

    private static List<Map<String, Text>> disks(final ServerLevel level, final NetworkUuid network) {
        final List<Map<String, Text>> rows = new ArrayList<>();
        for (final ServerNode node : NetworkSystem.get(level).serversOf(network)) {
            final ServerRackBlockEntity rack = rackOf(level, node.nodeUuid());
            final int slot = NetworkSystem.get(level).locationOf(node.nodeUuid()).map(at -> at.slot()).orElse(-1);
            if (rack == null || slot < 0) {
                continue;
            }
            final String label = NetworkLookup.serverLabel(level, node.nodeUuid());
            for (final ItemStack disk : rack.claimedDriveStacks(slot)) {
                if (!(disk.getItem() instanceof DiskItem item)) {
                    continue;
                }
                final Map<String, Text> row = new LinkedHashMap<>();
                row.put("disk", GameText.of(disk.getHoverName()));
                row.put("server", Text.literal(label));
                row.put("tier", Text.literal(item.spec().tier().name()));
                row.put("size", Text.literal(Long.toString(item.spec().capacityItems())));
                row.put("used", Text.literal(Long.toString(DriveVolumes.usedWeight(disk)
                        / StorageKey.MB_EQ_PER_ITEM)));
                rows.add(row);
            }
        }
        return rows;
    }

    private static List<Map<String, Text>> operations(final ServerLevel level, final NetworkUuid network,
                                                      final OperationsService operations) {
        final List<Map<String, Text>> rows = new ArrayList<>();
        for (final ICliComputer.ActiveOp op : operations.list()) {
            rows.add(operation(op.id(), op.type(), op.status(), Text.literal(op.item()), op.progress(), op.total(),
                    op.priority()));
        }
        final MainframeBlockEntity mainframe = NetworkLookup.resolveMainframe(level, network);
        if (mainframe != null) {
            for (final OperationRecord record : mainframe.recentOperations()) {
                rows.add(operation(record.hasId() ? ShortId.of(record.id().toString()) : "-",
                        OperationRecord.typeName(record.type()), OperationRecord.statusName(record.status()),
                        GameText.of(record.name()), record.moved(), record.requested(), record.priority().label()));
            }
        }
        return rows;
    }

    private static Map<String, Text> operation(final String id, final String verb, final String state,
                                               final Text item, final long moved, final long requested,
                                               final String priority) {
        final Map<String, Text> row = new LinkedHashMap<>();
        row.put("id", Text.literal(id));
        row.put("verb", Text.literal(verb));
        row.put("state", Text.literal(state.toLowerCase(Locale.ROOT)));
        row.put("item", item);
        row.put("moved", Text.literal(Long.toString(moved)));
        row.put("requested", Text.literal(Long.toString(requested)));
        row.put("priority", Text.literal(priority.toLowerCase(Locale.ROOT)));
        return row;
    }

    private static List<Map<String, Text>> recipes(final ServerLevel level, final NetworkUuid network) {
        final MainframeBlockEntity mainframe = NetworkLookup.resolveMainframe(level, network);
        if (mainframe == null) {
            return List.of();
        }
        final List<Map<String, Text>> rows = new ArrayList<>();
        for (final BlockPos at : mainframe.craftingComputerPositions()) {
            if (!(Loaded.blockEntity(level, at) instanceof CraftingComputerBlockEntity computer)) {
                continue;
            }
            for (final CraftPlaces.Place place : CraftPlaces.of(computer, level).all()) {
                for (final NetworkRecipe recipe : place.recipes()) {
                    final Map<String, Text> row = new LinkedHashMap<>();
                    row.put("recipe", recipe.displayText());
                    row.put("where", Text.literal(computer.hostname()));
                    row.put("machine", (recipe.bench().isPresent() ? BENCH : recipe.multi().isPresent() ? STAGES
                            : MACHINE).text());
                    row.put("chance", Text.literal(Integer.toString(chanceOf(recipe))));
                    rows.add(row);
                }
            }
        }
        return rows;
    }

    private static List<Map<String, Text>> computers(final ServerLevel level, final NetworkUuid network) {
        final NetworkSystem system = NetworkSystem.get(level);
        final List<Map<String, Text>> rows = new ArrayList<>();
        system.mainframePositionOf(network).ifPresent(pos -> {
            if (Loaded.blockEntity(level, BlockPos.of(pos)) instanceof MainframeBlockEntity mainframe) {
                rows.add(computer(mainframe, MAINFRAME));
            }
        });
        for (final ServerNode node : system.serversOf(network)) {
            final ServerRackBlockEntity rack = rackOf(level, node.nodeUuid());
            final int slot = system.locationOf(node.nodeUuid()).map(at -> at.slot()).orElse(-1);
            final Map<String, Text> row = new LinkedHashMap<>();
            row.put("name", Text.literal(NetworkLookup.serverLabel(level, node.nodeUuid())));
            row.put("kind", SERVER.text());
            row.put("era", rack != null && rack.getBlockState().getBlock() instanceof ServerRackBlock block
                    ? block.era().text() : Text.EMPTY);
            row.put("state", (rack != null && slot >= 0 && rack.unitRunning(slot) ? RUNNING : OFF).text());
            final OsDef os = rack == null ? null : rack.installedOs();
            row.put("os", os == null ? Text.EMPTY : Text.literal(os.displayName()));
            rows.add(row);
        }
        for (final NetworkSystem.PersonalComputerNode pc : system.personalComputersOf(network)) {
            if (Loaded.blockEntity(level, BlockPos.of(pc.pos())) instanceof AbstractComputerBlockEntity computer) {
                rows.add(computer(computer, PERSONAL_COMPUTER));
            }
        }
        for (final NetworkSystem.CraftingComputerNode cc : system.craftingComputersOf(network)) {
            if (Loaded.blockEntity(level, BlockPos.of(cc.pos())) instanceof AbstractComputerBlockEntity computer) {
                rows.add(computer(computer, CRAFTING_COMPUTER));
            }
        }
        return rows;
    }

    private static Map<String, Text> computer(final AbstractComputerBlockEntity computer, final TextKey kind) {
        final Map<String, Text> row = new LinkedHashMap<>();
        row.put("name", Text.literal(computer instanceof IComputerTerminalHost host ? host.hostname() : ""));
        row.put("kind", kind.text());
        final Block block = computer.getBlockState().getBlock();
        row.put("era", block instanceof MainframeBlock mainframe ? mainframe.era().text()
                : block instanceof PersonalComputerBlock pc ? pc.era().text()
                : block instanceof CraftingComputerBlock cc ? cc.era().text() : Text.EMPTY);
        row.put("state", (computer.isRunning() ? RUNNING : OFF).text());
        final OsDef os = computer.installedOs();
        row.put("os", os == null ? Text.EMPTY : Text.literal(os.displayName()));
        return row;
    }

    /* Where an item lives: the one server holding it, or how many servers share it. */
    private static Text location(final ServerLevel level, final NetworkStorage storage, final StorageKey key) {
        final Map<NodeUuid, Long> breakdown = storage.breakdown(key);
        if (breakdown.size() == 1) {
            return Text.literal(NetworkLookup.serverLabel(level, breakdown.keySet().iterator().next()));
        }
        return ON_SERVERS.with(breakdown.size());
    }

    /** The item's tag a player most likely means: a common one first, then the rest in order; empty for none. */
    static String firstTag(final ItemStack stack) {
        final List<TagKey<Item>> tags = stack.getTags().sorted(Comparator.comparing(tag -> tag.location()
                .toString())).toList();
        for (final TagKey<Item> tag : tags) {
            if ("c".equals(tag.location().getNamespace())) {
                return tag.location().toString();
            }
        }
        return tags.isEmpty() ? "" : tags.get(0).location().toString();
    }

    /** The item's enchantments with their levels, the stored ones for a book, or empty text for none. */
    static Text enchantments(final ItemStack stack) {
        final ItemEnchantments enchantments = EnchantmentHelper.getEnchantmentsForCrafting(stack);
        if (enchantments.isEmpty()) {
            return Text.EMPTY;
        }
        final List<Text> names = new ArrayList<>();
        for (final Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
            names.add(GameText.of(Enchantment.getFullname(entry.getKey(), entry.getIntValue())));
        }
        return TextLists.join(", ", names);
    }

    private static String durability(final ItemStack stack) {
        if (!stack.isDamageableItem() || stack.getMaxDamage() == 0) {
            return "100";
        }
        return Long.toString(Math.round(100.0 * (stack.getMaxDamage() - stack.getDamageValue())
                / stack.getMaxDamage()));
    }

    /* A recipe's chance of its main output, in percent: always for a bench, the first output's for a machine. */
    private static int chanceOf(final NetworkRecipe recipe) {
        return recipe.proc().map(pattern -> pattern.outputs().isEmpty() ? 100
                : pattern.outputs().get(0).chancePercent()).orElse(100);
    }

    @Nullable
    private static ServerRackBlockEntity rackOf(final ServerLevel level, final NodeUuid node) {
        return NetworkSystem.get(level).locationOf(node)
                .map(at -> Loaded.blockEntity(level, BlockPos.of(at.rackPos())) instanceof ServerRackBlockEntity rack
                        ? rack : null)
                .orElse(null);
    }
}
