/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.engine.prophet;

import dev.jstech.computers.operation.INetworkOperation;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextTags;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

/**
 * What Prophet YourIQL keeps on one Mainframe: the states and watches declared, how often it looks and how much it
 * asks for at once, whether it reacts at all, and what it did lately. The states and watches, the settings and the
 * reactions go in the Mainframe's save; what is on its way and the levels seen are held while the Mainframe runs.
 */
final class ProphetMind {

    private final ProphetStates states = new ProphetStates();
    private final Deque<Reacted> reactions = new ArrayDeque<>();
    /** The last thing done for each item, and the Operations set going for it lately. */
    private final Map<String, Text> lastReaction = new HashMap<>();
    private final Map<String, Deque<INetworkOperation>> operations = new HashMap<>();
    private final Map<String, StorageKey> keys = new HashMap<>();
    private int interval = DEFAULT_INTERVAL;
    private long maxBatch = DEFAULT_BATCH;
    private boolean reacting = true;
    private long nextLook;
    private long lastRetry;

    /** How often it looks, in ticks, unless the player sets otherwise: once a second. */
    static final int DEFAULT_INTERVAL = 20;
    /** The most it asks for in one craft, unless the player sets otherwise. */
    static final long DEFAULT_BATCH = 1024L;
    /** The intervals a player may choose from, in ticks. */
    static final List<Integer> INTERVALS = List.of(20, 40, 100, 200);
    /** The batches a player may choose from. */
    static final List<Long> BATCHES = List.of(64L, 256L, 1024L, 4096L);
    /** How many reactions are kept. */
    static final int REACTIONS = 32;
    /** How many Operations are kept for each item, for the console to show. */
    static final int OPERATIONS = 4;

    ProphetStates states() {
        return states;
    }

    int interval() {
        return interval;
    }

    void interval(final int ticks) {
        this.interval = INTERVALS.contains(ticks) ? ticks : DEFAULT_INTERVAL;
    }

    long maxBatch() {
        return maxBatch;
    }

    void maxBatch(final long batch) {
        this.maxBatch = BATCHES.contains(batch) ? batch : DEFAULT_BATCH;
    }

    boolean reacting() {
        return reacting;
    }

    void reacting(final boolean value) {
        this.reacting = value;
    }

    /** Whether it is time to look again at {@code now}; when it is, the next look is set. */
    boolean due(final long now) {
        if (now < nextLook) {
            return false;
        }
        nextLook = now + interval;
        return true;
    }

    /** Whether the states that could not hold should try again at {@code now}; when so, the clock is set. */
    boolean retryDue(final long now, final long every) {
        if (now - lastRetry < every) {
            return false;
        }
        lastRetry = now;
        return true;
    }

    /** The key for an item id, read once. */
    @Nullable
    StorageKey key(final String item) {
        return keys.computeIfAbsent(item, StorageKey::byId);
    }

    /** Notes something it did, at {@code at}, for {@code item} (or empty for none). */
    void reacted(final long at, final String item, final Text what) {
        reactions.addFirst(new Reacted(at, what));
        while (reactions.size() > REACTIONS) {
            reactions.removeLast();
        }
        if (!item.isEmpty()) {
            lastReaction.put(item, what);
        }
    }

    /** Notes an Operation set going for {@code item}. */
    void started(final String item, final INetworkOperation operation) {
        final Deque<INetworkOperation> kept = operations.computeIfAbsent(item, k -> new ArrayDeque<>());
        kept.addFirst(operation);
        while (kept.size() > OPERATIONS) {
            kept.removeLast();
        }
    }

    /** The last thing done for {@code item}, or null. */
    @Nullable
    Text lastReaction(final String item) {
        return lastReaction.get(item);
    }

    /** The Operations set going for {@code item} lately, the newest first. */
    List<INetworkOperation> operations(final String item) {
        return List.copyOf(operations.getOrDefault(item, new ArrayDeque<>()));
    }

    /** What it did lately, the newest first. */
    List<Reacted> reactions() {
        return List.copyOf(reactions);
    }

    void save(final CompoundTag tag) {
        final ListTag keeps = new ListTag();
        for (final ProphetStates.KeepState keep : states.keeps()) {
            final CompoundTag row = new CompoundTag();
            row.putString("Item", keep.item());
            row.putLong("Lower", keep.lower());
            row.putLong("Upper", keep.upper());
            keeps.add(row);
        }
        tag.put("Keeps", keeps);
        final ListTag watches = new ListTag();
        for (final ProphetStates.WatchState watch : states.watches()) {
            final CompoundTag row = new CompoundTag();
            row.putInt("Number", watch.number());
            row.putString("Item", watch.item());
            row.putString("Comparison", watch.comparison().symbol());
            row.putLong("Threshold", watch.threshold());
            row.putString("Action", watch.action());
            row.putBoolean("Armed", watch.armed());
            watches.add(row);
        }
        tag.put("Watches", watches);
        tag.putInt("NextWatch", states.nextWatchNumber());
        tag.putInt("Interval", interval);
        tag.putLong("MaxBatch", maxBatch);
        tag.putBoolean("Reacting", reacting);
        final ListTag done = new ListTag();
        for (final Reacted reacted : reactions) {
            final CompoundTag row = new CompoundTag();
            row.putLong("At", reacted.at());
            row.put("What", TextTags.write(reacted.what()));
            done.add(row);
        }
        tag.put("Reactions", done);
    }

    static ProphetMind load(final CompoundTag tag) {
        final ProphetMind mind = new ProphetMind();
        final ListTag keeps = tag.getList("Keeps", Tag.TAG_COMPOUND);
        for (int i = 0; i < keeps.size(); i++) {
            final CompoundTag row = keeps.getCompound(i);
            mind.states.keep(row.getString("Item"), row.getLong("Lower"), row.getLong("Upper"));
        }
        final ListTag watches = tag.getList("Watches", Tag.TAG_COMPOUND);
        int highest = 0;
        for (int i = 0; i < watches.size(); i++) {
            final CompoundTag row = watches.getCompound(i);
            final ProphetStatement.Comparison comparison =
                    ProphetStatement.Comparison.of(row.getString("Comparison"));
            if (comparison == null) {
                continue;
            }
            mind.states.nextWatch(row.getInt("Number"));
            final ProphetStates.WatchState watch = mind.states.watch(row.getString("Item"), comparison,
                    row.getLong("Threshold"), row.getString("Action"));
            watch.armed(row.getBoolean("Armed"));
            highest = Math.max(highest, watch.number());
        }
        mind.states.nextWatch(Math.max(highest + 1, tag.getInt("NextWatch")));
        if (tag.contains("Interval")) {
            mind.interval(tag.getInt("Interval"));
            mind.maxBatch(tag.getLong("MaxBatch"));
            mind.reacting = tag.getBoolean("Reacting");
        }
        final ListTag done = tag.getList("Reactions", Tag.TAG_COMPOUND);
        final List<Reacted> read = new ArrayList<>();
        for (int i = 0; i < done.size() && i < REACTIONS; i++) {
            final CompoundTag row = done.getCompound(i);
            read.add(new Reacted(row.getLong("At"), TextTags.read(row.getCompound("What"))));
        }
        mind.reactions.addAll(read);
        return mind;
    }

    /** Something it did, and when. */
    record Reacted(long at, Text what) {
    }
}
