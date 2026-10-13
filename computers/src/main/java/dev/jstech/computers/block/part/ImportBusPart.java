/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block.part;

import com.mojang.logging.LogUtils;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.bus.BusActivity;
import dev.jstech.computers.menu.ImportBusMenu;
import dev.jstech.computers.operation.MoveLabels;
import dev.jstech.computers.operation.NetworkInsertOperation;
import dev.jstech.computers.storage.ExternalDataPort;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.computers.trace.TracePoints;
import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.multipart.PartType;
import dev.jstech.core.persistence.SavedValue;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.core.uuid.NetworkUuid;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * An Import Bus: takes data from the block its face touches (items, fluids or chemicals alike) into the network, as
 * INSERT Operations the Mainframe dispatches, at its era's speed and never faster than its cable carries. It takes what
 * its filter lets through, leaving in the chest what it was set to keep, a move at a time of at most its max; the
 * Vintage bus, with no filter, takes whatever it finds first, one kind at a time. When the network has no room, the
 * buses of a higher priority bring their items in first.
 */
public non-sealed class ImportBusPart extends AbstractBusPart {

    private StorageKey bufferKey;
    private long bufferAmount;
    private NetworkInsertOperation activeOp;
    private StorageKey flushedKey;
    private long flushedAmount;
    private int ticksSinceFlush;
    /* What the bus may still take at its speed: a tick's worth is added each tick, up to a move's worth. */
    private long credit;
    /* When the network last had no room for what this bus brought, which is when buses take turns by priority. */
    private long fullSince = Long.MIN_VALUE;

    private static final Logger LOGGER = LogUtils.getLogger();
    /* A move goes when it is as big as it may be, or after this long with whatever was taken. */
    private static final int FLUSH_TICKS = 20;
    /* How long after the network had no room the buses keep taking turns by priority. */
    private static final long TURNS_FOR = 40L;
    /* What the buses taking turns for the network's room want. */
    private static final String ROOM = "room";

    public ImportBusPart(final HardwareEra era) {
        super(era);
    }

    @Override
    public PartType<?> type() {
        return ComputingParts.importBus(era());
    }

    @Override
    public AbstractContainerMenu createMenu(final int containerId, final Inventory inventory,
                                            final CableBlockEntity cable, final Direction mountedFace) {
        return ImportBusMenu.create(containerId, inventory, cable, mountedFace);
    }

    @Override
    public void serverTick() {
        final ServerLevel level = serverLevel();
        if (level != null) {
            settleLamps(level.getGameTime());
        }
        if (activeOp != null) {
            if (!activeOp.isDone()) {
                return;
            }
            settle(level);
        }
        if (level == null) {
            return;
        }
        final NetworkUuid network = network();
        linked = network != null;
        final MainframeBlockEntity mainframe = network == null ? null : mainframe();
        ticksSinceFlush++;
        final long speed = speed();
        final ExternalDataPort port = neighborPort();
        boolean typeChange = false;
        if (network != null && !port.isEmpty() && speed > 0L && mayMove(level, network) && takesTurn(level, network)) {
            credit = Math.min(credit + speed, batch(bufferKey, speed));
            if (bufferKey == null) {
                final StorageKey pick = pick(level, port);
                if (pick != null) {
                    final long taken = take(port, pick, Math.min(credit, batch(pick, speed)));
                    if (taken > 0L) {
                        bufferKey = pick;
                        bufferAmount = taken;
                    }
                }
            } else if (!admits(bufferKey)) {
                // The filter changed while another kind was taken: send that one on, take no more of it.
                typeChange = true;
            } else {
                final long room = Math.min(credit, batch(bufferKey, speed) - bufferAmount);
                if (room > 0L) {
                    bufferAmount += take(port, bufferKey, room);
                }
                // A kind with nothing more to take above what the chest keeps goes, and the next kind starts.
                typeChange = above(port, bufferKey) <= 0L && pick(level, port) != null;
            }
        }
        final boolean flush = bufferKey != null && (bufferAmount >= batch(bufferKey, speed)
                || ticksSinceFlush >= FLUSH_TICKS || typeChange);
        if (!flush) {
            return;
        }
        if (mainframe == null) {
            ticksSinceFlush = 0; // not networked: hold the buffer, do not spin
            return;
        }
        final StorageKey payloadKey = bufferKey;
        final long payloadAmount = bufferAmount;
        bufferKey = null;
        bufferAmount = 0L;
        ticksSinceFlush = 0;
        /*
         * Push the buffered data into the network as a timed INSERT; whatever does not fit comes back
         * as the Operation's leftover and is re-buffered when it finishes (above).
         */
        activeOp = mainframe.networkOperations().push(payloadKey, payloadAmount, MoveLabels.bus("Import Bus", name()));
        flushedKey = payloadKey;
        flushedAmount = payloadAmount;
        if (activeOp == null) {
            bufferKey = payloadKey; // dispatch failed (not running): keep the data
            bufferAmount = payloadAmount;
            flushedKey = null;
            flushedAmount = 0L;
        } else {
            TracePoints.imported(level, network, name(), payloadKey, payloadAmount);
        }
        host.setChanged();
    }

    @Override
    public ItemStack partItem() {
        return ComputingParts.importBusItem(era());
    }

    @Override
    public void dropContents(final ServerLevel level) {
        /*
         * Drop a buffered item back into the world; a buffered fluid (rare, transient) is discarded.
         * If an INSERT operation is in flight, the payload was already extracted from the source but
         * not yet confirmed by the network, so drop what the network has not taken yet; nothing is silently
         * lost, and the operation is stopped so it cannot also insert the dropped items.
         */
        final StorageKey drop = bufferKey != null ? bufferKey : flushedKey;
        final long dropAmount = bufferKey != null ? bufferAmount : unconfirmedAmount();
        if (activeOp != null) {
            if (!activeOp.isDone()) {
                activeOp.abandon();
            }
            activeOp = null;
        }
        if (drop != null && drop.isItem() && dropAmount > 0L && host != null) {
            Containers.dropItemStack(level,
                    host.getBlockPos().getX(), host.getBlockPos().getY(), host.getBlockPos().getZ(),
                    drop.stack((int) Math.min(dropAmount, Integer.MAX_VALUE)));
        }
        bufferKey = null;
        bufferAmount = 0L;
        flushedKey = null;
        flushedAmount = 0L;
    }

    @Override
    public void save(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.save(tag, registries);
        if (bufferKey != null && bufferAmount > 0L) {
            StorageKey.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), bufferKey)
                    .result().ifPresent(encoded -> tag.put("BufferKey", encoded));
            tag.putLong("BufferAmount", bufferAmount);
        }
        /*
         * Persist the in-flight payload so a save/reload cannot destroy items that were extracted
         * from the source but whose INSERT operation has not yet been confirmed by the network. Only
         * what the network has not taken yet is saved: the rest is already in storage, and saving the
         * full batch would insert it a second time after the reload.
         */
        final long unconfirmed = unconfirmedAmount();
        if (flushedKey != null && unconfirmed > 0L) {
            StorageKey.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), flushedKey)
                    .result().ifPresent(encoded -> tag.put("FlushedKey", encoded));
            tag.putLong("FlushedAmount", unconfirmed);
        }
    }

    @Override
    public void load(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.load(tag, registries);
        bufferKey = null;
        bufferAmount = 0L;
        flushedKey = null;
        flushedAmount = 0L;
        final var ops = registries.createSerializationContext(NbtOps.INSTANCE);
        if (tag.contains("BufferKey")) {
            SavedValue.read(StorageKey.CODEC.parse(ops, tag.get("BufferKey")),
                            LOGGER, "what an import bus was holding")
                    .ifPresent(key -> {
                        bufferKey = key;
                        bufferAmount = tag.getLong("BufferAmount");
                    });
        }
        /*
         * Recover items that were in flight before the reload; the timed operation is gone but the data
         * must not be lost, so move them into the buffer to be re-inserted next tick. Buffer and flushed
         * should be mutually exclusive at a save point, but if both are present, accumulate (same key) or
         * keep the larger batch rather than overwrite, so no items are silently dropped.
         */
        if (tag.contains("FlushedKey")) {
            final long flushedAmt = tag.getLong("FlushedAmount");
            SavedValue.read(StorageKey.CODEC.parse(ops, tag.get("FlushedKey")),
                            LOGGER, "what an import bus had on its way in")
                    .ifPresent(key -> {
                        if (bufferKey == null) {
                            bufferKey = key;
                            bufferAmount = flushedAmt;
                        } else if (bufferKey.equals(key)) {
                            bufferAmount += flushedAmt;
                        } else if (flushedAmt > bufferAmount) {
                            bufferKey = key;
                            bufferAmount = flushedAmt;
                        }
                    });
        }
    }

    /* The part of the in-flight batch the network has not written into storage yet. */
    private long unconfirmedAmount() {
        if (flushedKey == null) {
            return 0L;
        }
        return activeOp == null ? flushedAmount : Math.max(0L, flushedAmount - activeOp.writtenTotal());
    }

    /*
     * A finished move: what went in is in the activity; what the network had no room for comes back to be sent again,
     * and the hold is written down.
     */
    private void settle(@Nullable final ServerLevel level) {
        final long leftover = activeOp.leftover();
        final long now = level == null ? 0L : level.getGameTime();
        if (flushedKey != null) {
            if (flushedAmount - leftover > 0L) {
                activity.moved(now, flushedKey.id(), flushedAmount - leftover, leftover > 0L);
                worked(now);
            }
            if (leftover > 0L) {
                activity.held(now, flushedKey.id(), BusActivity.FULL, 0L);
                fullSince = now;
                if (bufferKey == null) {
                    bufferKey = flushedKey;
                    bufferAmount = leftover;
                }
            }
        }
        activeOp = null;
        flushedKey = null;
        flushedAmount = 0L;
        ticksSinceFlush = 0;
        host.setChanged();
    }

    /* While the network has had no room lately, the buses take turns by priority; otherwise every bus goes. */
    private boolean takesTurn(final ServerLevel level, final NetworkUuid network) {
        final boolean fullLately = fullSince != Long.MIN_VALUE && level.getGameTime() - fullSince <= TURNS_FOR;
        return !fullLately || firstFor(level, network, ROOM);
    }

    /*
     * What to take next: the first kind the faced block holds that the filter lets through and that has more than the
     * chest keeps of it. A kind held back by what the chest keeps is written down.
     */
    @Nullable
    private StorageKey pick(final ServerLevel level, final ExternalDataPort port) {
        StorageKey kept = null;
        for (final StorageKey key : port.available()) {
            if (!admits(key)) {
                continue;
            }
            if (above(port, key) > 0L) {
                return key;
            }
            kept = kept == null ? key : kept;
        }
        if (kept != null) {
            activity.held(level.getGameTime(), kept.id(), BusActivity.KEEPS, keepFor(kept));
        }
        return null;
    }

    /* How much of {@code key} the faced block holds beyond what the chest keeps. */
    private long above(final ExternalDataPort port, final StorageKey key) {
        return port.count(key) - keepFor(key);
    }

    /* Takes up to {@code amount} of {@code key}, never below what the chest keeps, out of the bus's credit. */
    private long take(final ExternalDataPort port, final StorageKey key, final long amount) {
        final long wanted = Math.min(amount, above(port, key));
        if (wanted <= 0L) {
            return 0L;
        }
        final long taken = port.extract(key, wanted, false);
        credit -= taken;
        if (taken > 0L) {
            host.setChanged();
        }
        return taken;
    }

    /* How big a move of {@code key} may be: its max, or a second's worth at the bus's speed. */
    private long batch(@Nullable final StorageKey key, final long speed) {
        final int most = key == null ? max : maxFor(key);
        return Math.max(1L, most > 0 ? most : speed * FLUSH_TICKS);
    }
}
