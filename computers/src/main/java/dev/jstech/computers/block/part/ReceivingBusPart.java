/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block.part;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.crafting.CraftingLog;
import dev.jstech.computers.menu.ReceivingBusMenu;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.multipart.PartType;
import dev.jstech.core.persistence.SavedValue;
import dev.jstech.core.tier.HardwareEra;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/**
 * A Crafting Receiving Bus: on the crafting cable, against a machine's output. It ties itself to the Crafting
 * Interface that feeds that machine, against it or through the routers of its own cable, or it is tied by hand to one
 * interface or several (for an output that lands in a chest beside the machine, say). While a job of its interfaces
 * runs, what comes out is credited to that job, as much as it fed for; what was there before the job began stays
 * where it is, since it belongs to the machine, and an item no pattern listed goes to the network as unexpected (a
 * fluid or chemical no pattern listed is the machine's own buffer, and stays). It moves nothing on its own: the engine pulls through it, measured and accounted for.
 */
public final class ReceivingBusPart extends ImportBusPart {

    private final List<UUID> tiedByHand = new ArrayList<>();
    /* What it saw when its interfaces went to work: that belongs to the machine, and is never pulled. */
    private final Map<StorageKey, Long> baseline = new LinkedHashMap<>();
    private boolean watching;
    private final CraftingLog log = new CraftingLog();

    /** The most interfaces one bus is tied to by hand. */
    public static final int MOST_TIES = 8;

    /* One design for every era: it only marks a face, and keeps the filter that routes it. */
    public ReceivingBusPart() {
        super(HardwareEra.STANDARD);
    }

    @Override
    public PartType<?> type() {
        return ComputingParts.RECEIVING.get();
    }

    @Override
    public void serverTick() {
        /*
         * Passive: pulling on its own would take a craft's outputs while the engine is crediting them. Only its lamps
         * go out after the engine's last pull.
         */
        final ServerLevel level = serverLevel();
        if (level != null) {
            settleLamps(level.getGameTime());
        }
    }

    /** The interfaces it was tied to by hand, by their ids; empty when it ties itself. */
    public List<UUID> tiedByHand() {
        return List.copyOf(tiedByHand);
    }

    /** Ties it by hand to {@code ids}, at most {@link #MOST_TIES}; none lets it tie itself again. */
    public void tieByHand(final List<UUID> ids) {
        tiedByHand.clear();
        for (final UUID id : ids) {
            if (tiedByHand.size() < MOST_TIES && !tiedByHand.contains(id)) {
                tiedByHand.add(id);
            }
        }
        markHostChanged();
    }

    /** What it saw when its interfaces went to work, which stays where it is. */
    public Map<StorageKey, Long> baseline() {
        return baseline;
    }

    /** Whether its interfaces are at work, with what it saw then kept. */
    public boolean watching() {
        return watching;
    }

    /** Its interfaces went to work: what it sees now belongs to the machine and stays. */
    public void startWatching(final Map<StorageKey, Long> seen) {
        baseline.clear();
        baseline.putAll(seen);
        watching = true;
        markHostChanged();
    }

    /** Its interfaces are idle and owe nothing: it forgets what it saw. */
    public void stopWatching() {
        if (watching) {
            baseline.clear();
            watching = false;
            markHostChanged();
        }
    }

    /** Each arrival it credited and where it went. */
    public CraftingLog log() {
        return log;
    }

    /** The engine pulled something through it at {@code now}: its lamps blink. */
    public void pulled(final long now) {
        worked(now);
    }

    @Override
    public AbstractContainerMenu createMenu(final int containerId, final Inventory inventory,
                                            final CableBlockEntity cable, final Direction mountedFace) {
        return ReceivingBusMenu.create(containerId, inventory, cable, mountedFace);
    }

    @Override
    public ItemStack partItem() {
        return new ItemStack(ComputingModule.RECEIVING_BUS_ITEM.get());
    }

    @Override
    public void save(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.save(tag, registries);
        final ListTag ties = new ListTag();
        tiedByHand.forEach(id -> ties.add(NbtUtils.createUUID(id)));
        tag.put("TiedByHand", ties);
        tag.putBoolean("Watching", watching);
        final RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, registries);
        final ListTag seen = new ListTag();
        baseline.forEach((key, amount) -> StorageKey.CODEC.encodeStart(ops, key).result().ifPresent(keyTag -> {
            final CompoundTag row = new CompoundTag();
            row.put("Key", keyTag);
            row.putLong("Amount", amount);
            seen.add(row);
        }));
        tag.put("Baseline", seen);
        final ListTag lines = new ListTag();
        for (final CraftingLog.Entry entry : log.entries().reversed()) {
            lines.add(CraftingInterfacePart.saveEntry(entry));
        }
        tag.put("Credits", lines);
    }

    @Override
    public void load(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.load(tag, registries);
        tiedByHand.clear();
        for (final Tag one : tag.getList("TiedByHand", Tag.TAG_INT_ARRAY)) {
            tiedByHand.add(NbtUtils.loadUUID(one));
        }
        watching = tag.getBoolean("Watching");
        baseline.clear();
        final RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, registries);
        for (final Tag one : tag.getList("Baseline", Tag.TAG_COMPOUND)) {
            final CompoundTag row = (CompoundTag) one;
            final StorageKey key = SavedValue.readOr(StorageKey.CODEC.parse(ops, row.get("Key")), JsComputers.LOGGER,
                    "what a Receiving Bus saw when its interfaces went to work", null);
            if (key != null) {
                baseline.put(key, row.getLong("Amount"));
            }
        }
        final List<CraftingLog.Entry> lines = new ArrayList<>();
        for (final Tag one : tag.getList("Credits", Tag.TAG_COMPOUND)) {
            lines.add(CraftingInterfacePart.loadEntry((CompoundTag) one));
        }
        log.restore(lines);
    }
}
