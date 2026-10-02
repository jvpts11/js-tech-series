/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block.part;

import dev.jstech.computers.block.DataWires;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.bus.BusAbilities;
import dev.jstech.computers.bus.BusActivity;
import dev.jstech.computers.bus.BusClaims;
import dev.jstech.computers.bus.BusCondition;
import dev.jstech.computers.bus.BusFeature;
import dev.jstech.computers.bus.BusSettings;
import dev.jstech.computers.menu.AbstractBusMenu;
import dev.jstech.computers.operation.NetworkStorage;
import dev.jstech.computers.storage.ExternalDataPort;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.cable.Wire;
import dev.jstech.core.chemical.ChemicalBridges;
import dev.jstech.core.id.StableIds;
import dev.jstech.core.multipart.IFacePart;
import dev.jstech.core.multipart.IPartHost;
import dev.jstech.core.network.DataLink;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.core.util.Utf8Text;
import dev.jstech.core.uuid.NetworkUuid;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * The storage buses' shared base (Import, Export, and the crafting buses built on them): a part on a cable's face that
 * moves between the network and the block it faces. A bus is of an era, which decides what it can be set to do and how
 * fast it moves ({@link BusAbilities}), never faster than its cable carries; and it is set up both ways, in its window
 * and from software, where what a program set is marked with the program's name.
 *
 * <p>Its settings: a name, by which software finds it; on or off; continuous, or on demand (only with a redstone
 * signal); a filter of up to five items, only these or all but these, with tags on the Advanced and a loose match that
 * takes an item whatever its damage and components; what the faced chest keeps and how many a move takes, for the
 * whole bus or, on the Transition, for each listed item; a priority over the network's other buses; and conditions.
 * Each move is an Operation, and what the bus did lately, its holds as well, is its {@link BusActivity}.
 */
public abstract sealed class AbstractBusPart implements IFacePart permits ImportBusPart, ExportBusPart {

    protected CableBlockEntity host;
    protected Direction face = Direction.NORTH;
    private final HardwareEra era;
    private final BusAbilities abilities;
    protected final ItemStackHandler filter = new ItemStackHandler(BusAbilities.FILTER_SLOTS) {
        @Override
        public int getSlotLimit(final int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(final int slot) {
            markHostChanged();
        }
    };
    protected boolean exclude;
    protected int keep;
    protected int max;
    protected final int[] itemKeep = new int[BusAbilities.FILTER_SLOTS];
    protected final int[] itemMax = new int[BusAbilities.FILTER_SLOTS];
    protected int priority;
    protected final List<BusCondition> conditions = new ArrayList<>();
    protected final List<ResourceLocation> tags = new ArrayList<>();
    protected boolean fuzzy;
    protected boolean powered = true;
    protected int mode = MODE_CONTINUOUS;
    protected boolean linked;
    protected String name = "";
    /* Which setting a program set last, by the setting's word, and the program's name; a hand on it clears it. */
    private final Map<String, String> setBy = new LinkedHashMap<>();
    protected final BusActivity activity = new BusActivity();
    private long conditionsCheckedAt = Long.MIN_VALUE;
    private boolean conditionsHeld = true;

    public static final int MODE_CONTINUOUS = 0;
    /** On demand: the bus moves only while its cable block has a redstone signal. */
    public static final int MODE_REDSTONE = 1;
    /** The longest name a bus may carry; keeps the name field and any query reference bounded. */
    public static final int MAX_NAME_LENGTH = 32;
    public static final int MAX_CONDITIONS = 4;
    public static final int MAX_TAGS = 4;
    /** How long, in ticks, a bus must have made no move for another bus waiting on it to go. */
    public static final long FINISHED_AFTER = 40L;
    /* How often the conditions are looked at again: a network's stock is a sum worth not taking every tick. */
    private static final long CONDITIONS_EVERY = 20L;

    /* Each level's wants of its buses, which say which bus goes first. */
    private static final Map<ServerLevel, BusClaims> CLAIMS = new WeakHashMap<>();

    protected AbstractBusPart(final HardwareEra era) {
        this.era = era;
        this.abilities = BusAbilities.of(era);
    }

    @Override
    public void attach(final IPartHost host, final Direction face) {
        if (!(host instanceof CableBlockEntity cable)) {
            throw new IllegalArgumentException("a bus mounts on a cable block, not on " + host);
        }
        this.host = cable;
        this.face = face;
    }

    /*
     * Using the bus opens its window; picking it off the cable is a left-click, handled where the cable block breaks,
     * so it never breaks the cable. The open packet carries the bus's name, so the field shows it, and its era, which
     * decides the window's rows and so where the inventory sits under them.
     */
    @Override
    public boolean use(final ServerPlayer player) {
        if (this.host == null) {
            return false;
        }
        final CableBlockEntity cable = this.host;
        final Direction mounted = this.face;
        player.openMenu(new SimpleMenuProvider((id, inventory, opener) -> createMenu(id, inventory, cable, mounted),
                partItem().getHoverName()), AbstractBusMenu.Opening.of(cable, mounted, this)::write);
        return true;
    }

    /** Builds this bus's configuration menu, each bus its own, so the cable stays generic. */
    public abstract AbstractContainerMenu createMenu(int containerId, Inventory inventory,
                                                     CableBlockEntity cable, Direction mountedFace);

    /** The era the bus belongs to. */
    public HardwareEra era() {
        return era;
    }

    /** What the bus can be set to do and how fast it moves. */
    public BusAbilities abilities() {
        return abilities;
    }

    /** What the bus did lately. */
    public BusActivity activity() {
        return activity;
    }

    public ItemStackHandler getFilterHandler() {
        return filter;
    }

    public boolean exclude() {
        return exclude;
    }

    public int keep() {
        return keep;
    }

    public int max() {
        return max;
    }

    public int itemKeep(final int slot) {
        return itemKeep[slot];
    }

    public int itemMax(final int slot) {
        return itemMax[slot];
    }

    public int priority() {
        return priority;
    }

    public List<BusCondition> conditions() {
        return List.copyOf(conditions);
    }

    public List<ResourceLocation> tags() {
        return List.copyOf(tags);
    }

    public boolean fuzzy() {
        return fuzzy;
    }

    public boolean powered() {
        return powered;
    }

    public int mode() {
        return mode;
    }

    public boolean linked() {
        return linked;
    }

    /** Whether the bus's cable reaches a network now: asked of the cable, for a bus that does not tick, too. */
    public boolean reachesNetwork() {
        return network() != null;
    }

    public String name() {
        return name;
    }

    /** The program that set {@code setting} last, or empty when it was set by hand or never. */
    public String setBy(final String setting) {
        return setBy.getOrDefault(setting, "");
    }

    /** How the bus is set, as plain values its window and software read alike. */
    public BusSettings settings() {
        final List<String> listed = new ArrayList<>();
        for (int slot = 0; slot < filter.getSlots(); slot++) {
            final StorageKey key = keyIn(slot);
            listed.add(key == null ? "" : key.id());
        }
        return new BusSettings(name, era, listed, exclude, keep, max, boxed(itemKeep), boxed(itemMax), priority,
                conditions, tags.stream().map(ResourceLocation::toString).toList(), fuzzy, powered,
                mode == MODE_REDSTONE, setBy);
    }

    /** What each filter slot holds, copies to show. */
    public List<ItemStack> filterStacks() {
        final List<ItemStack> stacks = new ArrayList<>();
        for (int slot = 0; slot < filter.getSlots(); slot++) {
            stacks.add(filter.getStackInSlot(slot).copy());
        }
        return stacks;
    }

    /** What the filter lists, slot by slot, empty slots left out: a crafting bus routes its face by these. */
    public List<StorageKey> filterKeys() {
        final List<StorageKey> keys = new ArrayList<>();
        for (int slot = 0; slot < filter.getSlots(); slot++) {
            final StorageKey key = keyIn(slot);
            if (key != null) {
                keys.add(key);
            }
        }
        return keys;
    }

    /** The first filter slot that lists nothing, or -1 when every one lists something. */
    public int firstEmptyFilterSlot() {
        for (int slot = 0; slot < filter.getSlots(); slot++) {
            if (filter.getStackInSlot(slot).isEmpty()) {
                return slot;
            }
        }
        return -1;
    }

    /** How many items a tick the bus's cable carries, 0 when it is on none. */
    public long cableCarries() {
        if (host == null) {
            return 0L;
        }
        final Wire wire = DataWires.networkWire(host);
        final DataLink link = wire == null ? null : DataWires.linkOf(wire);
        return link == null ? 0L : link.throughput();
    }

    /**
     * The era whose skin the bus's window wears: its own; a crafting bus, which is one design for every era, wears
     * the skin of the Mainframe that commands it, the Standard's when it reaches none.
     */
    public HardwareEra skin() {
        if (!ComputingParts.isCrafting(type())) {
            return era;
        }
        final MainframeBlockEntity mainframe = mainframe();
        return mainframe == null ? HardwareEra.STANDARD : mainframe.mainframeEra();
    }

    /** Sets the bus name, trimmed and cut to fit; an empty name means the bus is unaddressable by query. */
    public void setName(final String newName) {
        this.name = Utf8Text.field(newName, MAX_NAME_LENGTH);
        markHostChanged();
    }

    /** Lists {@code stack} alone in the filter, or empties it; what a crafting bus routes by is the first slot. */
    public void setFilter(final ItemStack stack) {
        for (int slot = 0; slot < filter.getSlots(); slot++) {
            filter.setStackInSlot(slot, ItemStack.EMPTY);
        }
        filter.setStackInSlot(0, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
        markHostChanged();
    }

    /**
     * The settings software and the window change, each only where the era has it: the answer is whether it was
     * taken. {@code by} names the program that set it, or is empty for a hand in the window.
     */
    public boolean setFilterSlot(final int slot, final ItemStack stack, final String by) {
        if (!can(BusFeature.FILTER) && slot > 0 || slot < 0 || slot >= filter.getSlots()) {
            return false;
        }
        filter.setStackInSlot(slot, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
        return changed(BusSettings.FILTER, by);
    }

    public boolean setExclude(final boolean allBut, final String by) {
        if (!can(BusFeature.FILTER)) {
            return false;
        }
        exclude = allBut;
        return changed(BusSettings.FILTER, by);
    }

    public boolean setKeep(final int count, final String by) {
        if (!can(BusFeature.QUANTITIES)) {
            return false;
        }
        keep = Math.max(0, count);
        return changed(BusSettings.KEEP, by);
    }

    public boolean setMax(final int count, final String by) {
        if (!can(BusFeature.QUANTITIES)) {
            return false;
        }
        max = Math.max(0, count);
        return changed(BusSettings.MAX, by);
    }

    /** A keep and a max for the item in filter slot {@code slot}; zero falls back to the bus's own. */
    public boolean setItemQuantities(final int slot, final int keepCount, final int maxCount, final String by) {
        if (!can(BusFeature.ITEM_QUANTITIES) || slot < 0 || slot >= itemKeep.length) {
            return false;
        }
        itemKeep[slot] = Math.max(0, keepCount);
        itemMax[slot] = Math.max(0, maxCount);
        return changed(BusSettings.KEEP, by);
    }

    public boolean setPriority(final int value, final String by) {
        if (!can(BusFeature.PRIORITY)) {
            return false;
        }
        priority = value;
        return changed(BusSettings.PRIORITY, by);
    }

    public boolean addCondition(final BusCondition condition, final String by) {
        if (!can(BusFeature.CONDITIONS) || conditions.size() >= MAX_CONDITIONS) {
            return false;
        }
        conditions.add(condition);
        conditionsCheckedAt = Long.MIN_VALUE;
        return changed(BusSettings.CONDITIONS, by);
    }

    public boolean removeCondition(final int index, final String by) {
        if (index < 0 || index >= conditions.size()) {
            return false;
        }
        conditions.remove(index);
        conditionsCheckedAt = Long.MIN_VALUE;
        return changed(BusSettings.CONDITIONS, by);
    }

    public boolean setTags(final List<ResourceLocation> listed, final String by) {
        if (!can(BusFeature.TAGS)) {
            return false;
        }
        tags.clear();
        tags.addAll(listed.subList(0, Math.min(MAX_TAGS, listed.size())));
        return changed(BusSettings.FILTER, by);
    }

    /** Lists the tag {@code id} as well, while there is room for another and it is not listed yet. */
    public boolean addTag(final ResourceLocation id, final String by) {
        if (!can(BusFeature.TAGS) || tags.size() >= MAX_TAGS || tags.contains(id)) {
            return false;
        }
        tags.add(id);
        return changed(BusSettings.FILTER, by);
    }

    /** Takes the tag listed at {@code index} off. */
    public boolean removeTag(final int index, final String by) {
        if (index < 0 || index >= tags.size()) {
            return false;
        }
        tags.remove(index);
        return changed(BusSettings.FILTER, by);
    }

    public boolean setFuzzy(final boolean loose, final String by) {
        if (!can(BusFeature.FUZZY)) {
            return false;
        }
        fuzzy = loose;
        return changed(BusSettings.MATCH, by);
    }

    public boolean setPowered(final boolean on, final String by) {
        powered = on;
        return changed(BusSettings.POWER, by);
    }

    public boolean setMode(final int newMode, final String by) {
        mode = newMode == MODE_REDSTONE ? MODE_REDSTONE : MODE_CONTINUOUS;
        return changed(BusSettings.MODE, by);
    }

    public void adjustKeep(final int delta) {
        setKeep(keep + delta, "");
    }

    public void adjustMax(final int delta) {
        setMax(max + delta, "");
    }

    public void toggleMode() {
        setMode(mode == MODE_CONTINUOUS ? MODE_REDSTONE : MODE_CONTINUOUS, "");
    }

    public Item filterItem() {
        return filter.getStackInSlot(0).isEmpty() ? Items.AIR : filter.getStackInSlot(0).getItem();
    }

    /**
     * The storage key the first filter slot selects, or {@code null} for an empty slot: what a crafting bus routes its
     * face by. A fluid container in the slot (e.g. a filled bucket) selects its FLUID, and an item carrying a chemical
     * (a filled tank item, a hohlraum) selects that CHEMICAL, so a bus targets any kind of data the same way.
     */
    @Nullable
    public StorageKey filterKey() {
        return keyIn(0);
    }

    /** Whether the bus moves {@code key}: what its filter and its tags, only these or all but these, let through. */
    public boolean admits(final StorageKey key) {
        if (!can(BusFeature.FILTER)) {
            return true;
        }
        boolean listedAny = false;
        boolean match = false;
        for (int slot = 0; slot < filter.getSlots(); slot++) {
            final StorageKey listed = keyIn(slot);
            if (listed != null) {
                listedAny = true;
                match |= same(listed, key);
            }
        }
        if (can(BusFeature.TAGS)) {
            for (final ResourceLocation tag : tags) {
                listedAny = true;
                match |= key.isItem() && key.item().builtInRegistryHolder().is(TagKey.create(Registries.ITEM, tag));
            }
        }
        if (!listedAny) {
            return true;
        }
        return exclude != match;
    }

    /** What the faced chest keeps of {@code key}: its own keep where its item has one, else the bus's. */
    public int keepFor(final StorageKey key) {
        final int slot = listedSlot(key);
        if (slot >= 0 && can(BusFeature.ITEM_QUANTITIES) && itemKeep[slot] > 0) {
            return itemKeep[slot];
        }
        return can(BusFeature.QUANTITIES) ? keep : 0;
    }

    /** How many of {@code key} a move takes at most: its own max where its item has one, else the bus's; 0 for any. */
    public int maxFor(final StorageKey key) {
        final int slot = listedSlot(key);
        if (slot >= 0 && can(BusFeature.ITEM_QUANTITIES) && itemMax[slot] > 0) {
            return itemMax[slot];
        }
        return can(BusFeature.QUANTITIES) ? max : 0;
    }

    /** How many items a tick the bus moves on its cable: its era's speed, never more than the cable carries. */
    public long speed() {
        return abilities.speedOn(cableCarries());
    }

    /** Whether a redstone-mode bus is currently held off because its block has no neighbor signal. */
    protected boolean redstoneBlocked() {
        final ServerLevel level = serverLevel();
        return mode == MODE_REDSTONE && level != null && !level.hasNeighborSignal(host.getBlockPos());
    }

    /**
     * Whether the bus may move now: it is on, its mode lets it, and its conditions hold; a hold on a condition is
     * written to its activity.
     */
    protected boolean mayMove(final ServerLevel level, @Nullable final NetworkUuid network) {
        if (!powered || redstoneBlocked()) {
            return false;
        }
        if (conditions.isEmpty() || !can(BusFeature.CONDITIONS)) {
            return true;
        }
        final long now = level.getGameTime();
        if (conditionsCheckedAt == Long.MIN_VALUE || now - conditionsCheckedAt >= CONDITIONS_EVERY) {
            conditionsCheckedAt = now;
            conditionsHeld = conditionsHold(level, network);
            if (!conditionsHeld) {
                activity.held(now, "", BusActivity.HELD, 0L);
            }
        }
        return conditionsHeld;
    }

    /** Whether this bus, of its priority, goes first for {@code what} on its network; always below the Standard. */
    protected boolean firstFor(final ServerLevel level, final NetworkUuid network, final String what) {
        if (!can(BusFeature.PRIORITY)) {
            return true;
        }
        return CLAIMS.computeIfAbsent(level, l -> new BusClaims()).mayGo(network, what, priority, level.getGameTime());
    }

    /** The server level the bus's cable is in, or null on a player's game or before it is placed. */
    protected @Nullable ServerLevel serverLevel() {
        return host == null ? null : host.partServerLevel();
    }

    /** The network the bus's cable carries, or null when it is on none. */
    protected @Nullable NetworkUuid network() {
        return host == null || serverLevel() == null ? null : DataWires.networkOf(host);
    }

    /** The Mainframe of the network the bus's cable carries, or null when there is none. */
    protected @Nullable MainframeBlockEntity mainframe() {
        final ServerLevel level = serverLevel();
        return level == null ? null : DataWires.mainframeOf(level, network());
    }

    /** Every kind of data the block the bus faces holds: a bus moves whatever is there. */
    protected ExternalDataPort neighborPort() {
        final ServerLevel level = serverLevel();
        if (level == null) {
            return new ExternalDataPort(null, null);
        }
        return ExternalDataPort.at(level, host.getBlockPos().relative(face), face.getOpposite());
    }

    protected void markHostChanged() {
        if (host != null) {
            host.setChanged();
        }
    }

    protected boolean can(final BusFeature feature) {
        return abilities.can(feature);
    }

    @Override
    public void save(final CompoundTag tag, final HolderLookup.Provider registries) {
        tag.put("Filter", filter.serializeNBT(registries));
        tag.putBoolean("Exclude", exclude);
        tag.putInt("Keep", keep);
        tag.putInt("Max", max);
        tag.putIntArray("ItemKeep", itemKeep);
        tag.putIntArray("ItemMax", itemMax);
        tag.putInt("Priority", priority);
        final ListTag conditionList = new ListTag();
        for (final BusCondition condition : conditions) {
            final CompoundTag one = new CompoundTag();
            one.putInt("Kind", condition.kind().id());
            one.putString("Subject", condition.subject());
            one.putLong("Below", condition.below());
            one.putInt("From", condition.fromHour());
            one.putInt("To", condition.toHour());
            conditionList.add(one);
        }
        tag.put("Conditions", conditionList);
        final ListTag tagList = new ListTag();
        tags.forEach(id -> tagList.add(StringTag.valueOf(id.toString())));
        tag.put("Tags", tagList);
        tag.putBoolean("Fuzzy", fuzzy);
        tag.putBoolean("Powered", powered);
        tag.putInt("Mode", mode);
        if (!name.isEmpty()) {
            tag.putString("Name", name);
        }
        final CompoundTag marks = new CompoundTag();
        setBy.forEach(marks::putString);
        tag.put("SetBy", marks);
        final ListTag lines = new ListTag();
        for (final BusActivity.Entry entry : activity.entries().reversed()) {
            final CompoundTag line = new CompoundTag();
            line.putLong("Time", entry.time());
            line.putString("What", entry.what());
            line.putLong("Amount", entry.amount());
            line.putByte("Status", entry.status());
            line.putByte("Reason", entry.reason());
            line.putLong("Detail", entry.detail());
            lines.add(line);
        }
        tag.put("Activity", lines);
    }

    @Override
    public void load(final CompoundTag tag, final HolderLookup.Provider registries) {
        filter.deserializeNBT(registries, tag.getCompound("Filter"));
        exclude = tag.getBoolean("Exclude");
        keep = tag.getInt("Keep");
        max = tag.getInt("Max");
        copyInto(tag.getIntArray("ItemKeep"), itemKeep);
        copyInto(tag.getIntArray("ItemMax"), itemMax);
        priority = tag.getInt("Priority");
        conditions.clear();
        for (final Tag one : tag.getList("Conditions", Tag.TAG_COMPOUND)) {
            final CompoundTag c = (CompoundTag) one;
            final BusCondition.Kind kind = StableIds.of(BusCondition.Kind.class).find(c.getInt("Kind"));
            if (kind != null) {
                conditions.add(new BusCondition(kind, c.getString("Subject"), c.getLong("Below"), c.getInt("From"),
                        c.getInt("To")));
            }
        }
        tags.clear();
        for (final Tag one : tag.getList("Tags", Tag.TAG_STRING)) {
            final ResourceLocation id = ResourceLocation.tryParse(one.getAsString());
            if (id != null) {
                tags.add(id);
            }
        }
        fuzzy = tag.getBoolean("Fuzzy");
        // A bus saved before it could be switched off was on.
        powered = !tag.contains("Powered") || tag.getBoolean("Powered");
        mode = tag.getInt("Mode");
        name = tag.getString("Name");
        setBy.clear();
        final CompoundTag marks = tag.getCompound("SetBy");
        for (final String setting : marks.getAllKeys()) {
            setBy.put(setting, marks.getString(setting));
        }
        final List<BusActivity.Entry> lines = new ArrayList<>();
        for (final Tag one : tag.getList("Activity", Tag.TAG_COMPOUND)) {
            final CompoundTag line = (CompoundTag) one;
            lines.add(new BusActivity.Entry(line.getLong("Time"), line.getString("What"), line.getLong("Amount"),
                    line.getByte("Status"), line.getByte("Reason"), line.getLong("Detail")));
        }
        activity.restore(lines);
    }

    /* Records who set {@code setting}: a program by its name, or a hand, which clears the mark. */
    private boolean changed(final String setting, final String by) {
        if (by == null || by.isEmpty()) {
            setBy.remove(setting);
        } else {
            setBy.put(setting, Utf8Text.field(by, MAX_NAME_LENGTH));
        }
        markHostChanged();
        return true;
    }

    /**
     * The storage key filter slot {@code slot} selects, or null for an empty slot. A fluid container selects its fluid
     * and an item carrying a chemical its chemical.
     */
    @Nullable
    protected StorageKey keyIn(final int slot) {
        final ItemStack stack = filter.getStackInSlot(slot);
        if (stack.isEmpty()) {
            return null;
        }
        return FluidUtil.getFluidContained(stack)
                .filter(f -> !f.isEmpty())
                .map(StorageKey::of)
                .or(() -> ChemicalBridges.chemicalOf(stack)
                        .map(StorageKey::chemical))
                .orElseGet(() -> StorageKey.of(stack));
    }

    /* The filter slot listing {@code key}, or -1. */
    private int listedSlot(final StorageKey key) {
        for (int slot = 0; slot < filter.getSlots(); slot++) {
            final StorageKey listed = keyIn(slot);
            if (listed != null && same(listed, key)) {
                return slot;
            }
        }
        return -1;
    }

    /* Whether two keys are the same thing: exactly, or, matched loosely, the same item whatever its components. */
    private boolean same(final StorageKey listed, final StorageKey key) {
        if (listed.equals(key)) {
            return true;
        }
        return fuzzy && can(BusFeature.FUZZY) && listed.kind() == key.kind()
                && listed.registryId().equals(key.registryId());
    }

    /* Whether every condition holds: the network's stock, the hour, the other bus finished. */
    private boolean conditionsHold(final ServerLevel level, @Nullable final NetworkUuid network) {
        final int hour = (int) ((level.getDayTime() / 1000L + 6L) % 24L);
        for (final BusCondition condition : conditions) {
            final boolean holds = switch (condition.kind()) {
                case HOURS -> condition.holdsAt(hour);
                case STOCK -> network == null || stockOf(level, network, condition) < condition.below();
                case AFTER -> otherFinished(level, network, condition.subject());
            };
            if (!holds) {
                return false;
            }
        }
        return true;
    }

    /* How many of a condition's item, or of its tag's items, the network holds. */
    private static long stockOf(final ServerLevel level, final NetworkUuid network, final BusCondition condition) {
        final NetworkStorage storage = NetworkStorage.of(level, network);
        if (!condition.onTag()) {
            final StorageKey key = StorageKey.byName(condition.subject());
            return key == null ? 0L : storage.count(key);
        }
        final ResourceLocation tagId = ResourceLocation.tryParse(condition.subject().substring(1));
        if (tagId == null) {
            return 0L;
        }
        final TagKey<Item> tag = TagKey.create(Registries.ITEM, tagId);
        long total = 0L;
        for (final Map.Entry<StorageKey, Long> held : storage.query().entrySet()) {
            if (held.getKey().isItem() && held.getKey().item().builtInRegistryHolder().is(tag)) {
                total += held.getValue();
            }
        }
        return total;
    }

    /* Whether the bus named {@code other} has made no move for a while; a bus that is not there holds no one back. */
    private static boolean otherFinished(final ServerLevel level, @Nullable final NetworkUuid network,
                                         final String other) {
        final NamedBus.Located located = NamedBus.find(level, network, other);
        if (located == null || !(located.cable().getPart(located.face()) instanceof AbstractBusPart bus)) {
            return true;
        }
        return bus.activity.idleFor(level.getGameTime(), FINISHED_AFTER);
    }

    private static void copyInto(final int[] from, final int[] to) {
        System.arraycopy(from, 0, to, 0, Math.min(from.length, to.length));
    }

    private static List<Integer> boxed(final int[] values) {
        final List<Integer> list = new ArrayList<>();
        for (final int value : values) {
            list.add(value);
        }
        return list;
    }
}
