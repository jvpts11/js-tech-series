/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block.part;

import dev.jstech.computers.bus.BusAbilities;
import dev.jstech.computers.bus.BusFeature;
import dev.jstech.computers.bus.BusSettings;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.menu.ExternalStorageBusMenu;
import dev.jstech.computers.operation.ExternalStores;
import dev.jstech.computers.storage.ExternalDataPort;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.multipart.PartType;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.core.uuid.NodeUuid;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * An External Storage Bus: moves nothing itself; the network reaches the inventory its face touches as storage of its
 * own, ten times slower than its servers. The Vintage bus shows the network all of it; from the Legacy, what its filter
 * lets through, read and written or only one way; from the Standard, a priority that decides which storage the network
 * fills first; on the Advanced, tags and the loose match.
 */
public final class ExternalStorageBusPart extends AbstractBusPart {

    private int access = BusSettings.READ_WRITE;
    private long registeredAt = Long.MIN_VALUE;
    private long signatureAt = Long.MIN_VALUE;
    private long signature;

    /** How many times slower the network reaches an external inventory than its own storage. */
    public static final int SLOWER = 10;
    /** How often, in ticks, the inventory is read again for the network's catalog. */
    public static final long SIGNATURE_EVERY = 10L;
    /* How often, in ticks, the bus says again where it is, so the network finds it after a load. */
    private static final long REGISTER_EVERY = 20L;

    public ExternalStorageBusPart(final HardwareEra era) {
        super(era, BusAbilities.external(era));
    }

    @Override
    public PartType<?> type() {
        return ComputingParts.externalBus(era());
    }

    @Override
    public ItemStack partItem() {
        return ComputingParts.externalBusItem(era());
    }

    @Override
    public AbstractContainerMenu createMenu(final int containerId, final Inventory inventory,
                                            final CableBlockEntity cable, final Direction mountedFace) {
        return ExternalStorageBusMenu.create(containerId, inventory, cable, mountedFace);
    }

    @Override
    public void serverTick() {
        final ServerLevel level = serverLevel();
        if (level == null) {
            return;
        }
        final long now = level.getGameTime();
        settleLamps(now);
        if (registeredAt == Long.MIN_VALUE || now - registeredAt >= REGISTER_EVERY) {
            registeredAt = now;
            linked = network() != null;
            ExternalStores.register(level, node(), host.getBlockPos(), face);
        }
    }

    @Override
    public int access() {
        return access;
    }

    /** Which way the network may use the inventory: read and write, read only, or write only. */
    public boolean setAccess(final int value, final String by) {
        if (!can(BusFeature.ACCESS)) {
            return false;
        }
        access = value == BusSettings.READ_ONLY || value == BusSettings.WRITE_ONLY ? value : BusSettings.READ_WRITE;
        signatureAt = Long.MIN_VALUE;
        return changed(BusSettings.ACCESS, by);
    }

    /** The node the network keeps the inventory's contents under: the same for the same cable face, always. */
    public NodeUuid node() {
        final String where = "external_storage_bus:" + (host == null ? 0L : host.getBlockPos().asLong()) + ":"
                + face.get3DDataValue();
        return new NodeUuid(UUID.nameUUIDFromBytes(where.getBytes(StandardCharsets.UTF_8)));
    }

    /** The network the bus's cable carries now, or null. */
    @Nullable
    public NetworkUuid networkNow() {
        return network();
    }

    /** What of the inventory the network sees: what the filter lets through, and nothing when it may only write. */
    public Map<StorageKey, Long> visible() {
        final Map<StorageKey, Long> seen = new LinkedHashMap<>();
        if (access == BusSettings.WRITE_ONLY) {
            return seen;
        }
        final ExternalDataPort port = neighborPort();
        for (final StorageKey key : port.available()) {
            if (admits(key)) {
                seen.merge(key, port.count(key), Math::max);
            }
        }
        return seen;
    }

    /** How many of {@code key} the network sees in the inventory. */
    public long count(final StorageKey key) {
        return access == BusSettings.WRITE_ONLY || !admits(key) ? 0L : neighborPort().count(key);
    }

    /** Takes up to {@code amount} of {@code key} out of the inventory for the network, where it may read. */
    public long take(final StorageKey key, final long amount) {
        final long taken = access == BusSettings.WRITE_ONLY || !admits(key) ? 0L
                : neighborPort().extract(key, amount, false);
        return lit(taken);
    }

    /** Puts up to {@code amount} of {@code key} into the inventory for the network, where it may write. */
    public long give(final StorageKey key, final long amount) {
        return lit(takesIn(key) ? neighborPort().insert(key, amount, false) : 0L);
    }

    /** Whether the network may write {@code key} into the inventory. */
    public boolean takesIn(final StorageKey key) {
        return access != BusSettings.READ_ONLY && admits(key);
    }

    /** How much room the inventory has for the network, by weight, none when it may only read. */
    public long room() {
        return access == BusSettings.READ_ONLY ? 0L : neighborPort().room();
    }

    /** How many slots and tanks the inventory has, and how many hold something: what its window tells. */
    public int places() {
        return neighborPort().places();
    }

    public int placesUsed() {
        return neighborPort().placesUsed();
    }

    /** Which storage the network fills first: this one's priority where its era has one, else the network's own. */
    public int fillPriority() {
        return can(BusFeature.PRIORITY) ? priority : 0;
    }

    /** How many items a tick the network moves through the bus: a tenth of what its cable carries, at least one. */
    public long throughput() {
        return Math.max(1L, cableCarries() / SLOWER);
    }

    /** How long the inventory takes to answer, as a drive's seek: ten times the slowest drive's. */
    public int latencyTicks() {
        return StorageTier.HDD.latencyTicks() * SLOWER;
    }

    /**
     * A number that changes when what the network sees in the inventory changes, so the network's catalog reads the
     * inventory again only then. The catalog asks every tick; the inventory is read for it at most every
     * {@link #SIGNATURE_EVERY} ticks, so a large one costs the server little, and the catalog is at most that late.
     */
    public long signature() {
        final ServerLevel level = serverLevel();
        final long now = level == null ? 0L : level.getGameTime();
        if (signatureAt == Long.MIN_VALUE || now - signatureAt >= SIGNATURE_EVERY || now < signatureAt) {
            signatureAt = now;
            long sum = access;
            for (final Map.Entry<StorageKey, Long> seen : visible().entrySet()) {
                sum += seen.getKey().hashCode() * 31L + seen.getValue();
            }
            signature = sum;
        }
        return signature;
    }

    @Override
    public void save(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.save(tag, registries);
        tag.putInt("Access", access);
    }

    @Override
    public void load(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.load(tag, registries);
        final int saved = tag.getInt("Access");
        access = saved == BusSettings.READ_ONLY || saved == BusSettings.WRITE_ONLY ? saved : BusSettings.READ_WRITE;
    }

    /* What the network moved through the bus lights its lamps; gives the amount back. */
    private long lit(final long moved) {
        final ServerLevel level = serverLevel();
        if (moved > 0L && level != null) {
            worked(level.getGameTime());
        }
        return moved;
    }
}
