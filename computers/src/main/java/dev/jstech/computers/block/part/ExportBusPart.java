/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block.part;

import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.bus.BusActivity;
import dev.jstech.computers.bus.BusFeature;
import dev.jstech.computers.menu.ExportBusMenu;
import dev.jstech.computers.operation.MoveLabels;
import dev.jstech.computers.operation.NetworkSelectOperation;
import dev.jstech.computers.operation.NetworkStorage;
import dev.jstech.computers.storage.ExternalDataPort;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.multipart.PartType;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.core.uuid.NetworkUuid;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/**
 * An Export Bus: takes data out of the network into the block its face touches, as DELETE Operations the Mainframe
 * dispatches ("DELETE" = leaves the network for an external inventory, not destruction), at its era's speed and never
 * faster than its cable carries. It sends what its filter lists, filling the chest up to what it was set to keep, a
 * move at a time of at most its max; the Vintage bus sends the one kind it is set to. When several buses want the same
 * item, the ones of a higher priority take it first.
 */
public non-sealed class ExportBusPart extends AbstractBusPart {

    private NetworkSelectOperation activeOp;
    private StorageKey sentKey;
    private long sentAmount;
    private int ticksSinceExport;
    /* What the bus may still send at its speed: a tick's worth is added each tick, up to a second's worth. */
    private long credit;
    /* What it sends when it sends all but some, or by tag: the network's kinds it lets through, looked at again now
     * and then. */
    private List<StorageKey> candidates = List.of();
    private long candidatesAt = Long.MIN_VALUE;
    /* Where in its list the next send starts, so every kind gets its turn. */
    private int next;

    protected static final int EXPORT_INTERVAL = 2;
    private static final int SECOND = 20;
    private static final long CANDIDATES_EVERY = 20L;

    public ExportBusPart(final HardwareEra era) {
        super(era);
    }

    @Override
    public PartType<?> type() {
        return ComputingParts.exportBus(era());
    }

    @Override
    public AbstractContainerMenu createMenu(final int containerId, final Inventory inventory,
                                            final CableBlockEntity cable, final Direction mountedFace) {
        return ExportBusMenu.create(containerId, inventory, cable, mountedFace);
    }

    @Override
    public void serverTick() {
        final ServerLevel level = serverLevel();
        if (level == null) {
            return;
        }
        settleLamps(level.getGameTime());
        final NetworkUuid network = network();
        linked = network != null;
        // Wait for the in-flight DELETE to finish before starting another.
        if (activeOp != null) {
            if (!activeOp.isDone()) {
                return;
            }
            settle(level);
        }
        if (++ticksSinceExport < EXPORT_INTERVAL) {
            return;
        }
        ticksSinceExport = 0;
        final long speed = speed();
        if (network == null || speed <= 0L) {
            return;
        }
        credit = Math.min(credit + speed * EXPORT_INTERVAL, speed * SECOND);
        if (!mayMove(level, network)) {
            return;
        }
        final ExternalDataPort dest = neighborPort();
        final MainframeBlockEntity mainframe = mainframe();
        if (dest.isEmpty() || mainframe == null) {
            return;
        }
        final NetworkStorage storage = NetworkStorage.of(level, network);
        final List<StorageKey> keys = candidates(level, storage);
        for (int i = 0; i < keys.size(); i++) {
            final StorageKey key = keys.get((next + i) % keys.size());
            /*
             * A bus that wants a kind says so with its priority even while the network has none, so a bus of a higher
             * priority keeps its turn while it waits; then failed DELETEs are not spun on a kind the network lacks.
             */
            final long want = want(level, dest, key);
            if (want <= 0L || !firstFor(level, network, key.id()) || storage.count(key) <= 0L) {
                continue;
            }
            activeOp = mainframe.networkOperations().export(key, want, dest, MoveLabels.bus("Export Bus", name()));
            if (activeOp != null) {
                sentKey = key;
                sentAmount = want;
                credit -= want;
                next = (next + i + 1) % keys.size();
            }
            return;
        }
    }

    @Override
    public ItemStack partItem() {
        return ComputingParts.exportBusItem(era());
    }

    /* A finished send, written to the activity as the move it made. */
    private void settle(final ServerLevel level) {
        final long moved = activeOp.moved();
        if (sentKey != null && moved > 0L) {
            activity.moved(level.getGameTime(), sentKey.id(), moved, moved < sentAmount);
            worked(level.getGameTime());
        }
        activeOp = null;
        sentKey = null;
        sentAmount = 0L;
        markHostChanged();
    }

    /*
     * The kinds the bus sends: the Vintage bus its one kind; a filter of only these, its items; all but some, or tags,
     * the network's kinds that the filter lets through.
     */
    private List<StorageKey> candidates(final ServerLevel level, final NetworkStorage storage) {
        if (!can(BusFeature.FILTER)) {
            final StorageKey one = filterKey();
            return one == null ? List.of() : List.of(one);
        }
        if (!exclude && tags.isEmpty()) {
            final List<StorageKey> listed = new ArrayList<>();
            for (int slot = 0; slot < filter.getSlots(); slot++) {
                final StorageKey key = keyIn(slot);
                if (key != null) {
                    listed.add(key);
                }
            }
            return listed;
        }
        final long now = level.getGameTime();
        if (candidatesAt == Long.MIN_VALUE || now - candidatesAt >= CANDIDATES_EVERY) {
            candidatesAt = now;
            final List<StorageKey> through = new ArrayList<>();
            for (final StorageKey key : storage.query().keySet()) {
                if (admits(key)) {
                    through.add(key);
                }
            }
            candidates = through;
        }
        return candidates;
    }

    /*
     * How many of {@code key} to send now: as many as the credit and the max allow, up to what the chest is set to
     * keep and what it has room for. A chest that has its keep, or no room, is a hold written to the activity.
     */
    private long want(final ServerLevel level, final ExternalDataPort dest, final StorageKey key) {
        final long now = level.getGameTime();
        final int keepTarget = keepFor(key);
        final long held = dest.count(key);
        if (keepTarget > 0 && held >= keepTarget) {
            activity.held(now, key.id(), BusActivity.KEEPS, keepTarget);
            return 0L;
        }
        final int most = maxFor(key);
        long want = Math.min(credit, most > 0 ? most : Long.MAX_VALUE);
        if (keepTarget > 0) {
            want = Math.min(want, keepTarget - held);
        }
        if (want <= 0L) {
            return 0L;
        }
        final long room = dest.insert(key, want, true);
        if (room <= 0L) {
            activity.held(now, key.id(), BusActivity.FULL, 0L);
            return 0L;
        }
        return Math.min(want, room);
    }
}
