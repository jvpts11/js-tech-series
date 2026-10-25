/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.eventlog;

import dev.jstech.core.text.TextTags;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.core.uuid.NodeUuid;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

/**
 * The event logs of a world's data networks, apart from their Operations: what happened to every machine on each,
 * oldest first, saved with the world.
 *
 * <p>Each network keeps its last seven days, at most {@link #MOST} events; an older or further event makes way as a
 * new one comes. Adding one costs the same however long the log is, and a log changes its {@link #revision} with each,
 * so a screen that already shows it asks for nothing new until it does.
 */
public final class NetworkEventLog extends SavedData {

    private final Map<NetworkUuid, Feed> feeds = new HashMap<>();

    /** The most events a network keeps. */
    public static final int MOST = 2_000;
    /** How long a network keeps an event: seven days of the world's clock, in ticks. */
    public static final long KEPT_TICKS = 7L * 24_000L;

    private static final String FILE = "jsc_network_events";
    private static final String NETWORKS = "networks";
    private static final String NETWORK = "network";
    private static final String EVENTS = "events";
    private static final String TIME = "time";
    private static final String DAY_TIME = "dayTime";
    private static final String LEVEL = "level";
    private static final String MACHINE = "machine";
    private static final String MACHINE_NAME = "machineName";
    private static final String MESSAGE = "message";

    /** The event logs of {@code level}'s networks. */
    public static NetworkEventLog of(final ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(factory(), FILE);
    }

    /** How a world makes the logs afresh and reads them back from its save. */
    public static SavedData.Factory<NetworkEventLog> factory() {
        return new SavedData.Factory<>(NetworkEventLog::new, NetworkEventLog::load);
    }

    /** Writes {@code event} into {@code network}'s log, the oldest making way past seven days or {@link #MOST}. */
    public void add(final NetworkUuid network, final NetworkEvent event) {
        final Feed feed = this.feeds.computeIfAbsent(network, any -> new Feed());
        feed.events.addLast(event);
        feed.revision++;
        trim(feed, event.time());
        setDirty();
    }

    /**
     * The events of {@code network}'s log that {@code keep} keeps, newest first, at most {@code most}; the ones past
     * seven days at {@code now} are let go first.
     */
    public List<NetworkEvent> newest(final NetworkUuid network, final long now, final Predicate<NetworkEvent> keep,
                                     final int most) {
        final Feed feed = feed(network, now);
        final List<NetworkEvent> out = new ArrayList<>();
        if (feed == null) {
            return out;
        }
        final Iterator<NetworkEvent> newestFirst = feed.events.descendingIterator();
        while (newestFirst.hasNext() && out.size() < most) {
            final NetworkEvent event = newestFirst.next();
            if (keep.test(event)) {
                out.add(event);
            }
        }
        return out;
    }

    /**
     * The machines {@code network}'s log names, each once by the name it last had, the most recent first: what a
     * filter by machine offers.
     */
    public List<NetworkEvent> machines(final NetworkUuid network, final long now) {
        final Feed feed = feed(network, now);
        final List<NetworkEvent> out = new ArrayList<>();
        if (feed == null) {
            return out;
        }
        final Map<NodeUuid, Boolean> seen = new HashMap<>();
        final Iterator<NetworkEvent> newestFirst = feed.events.descendingIterator();
        while (newestFirst.hasNext()) {
            final NetworkEvent event = newestFirst.next();
            if (event.machine() != null && seen.putIfAbsent(event.machine(), Boolean.TRUE) == null) {
                out.add(event);
            }
        }
        return out;
    }

    /** How many events {@code network}'s log holds now. */
    public int size(final NetworkUuid network) {
        final Feed feed = this.feeds.get(network);
        return feed == null ? 0 : feed.events.size();
    }

    /** A number that changes whenever {@code network}'s log gains an event; zero for a network with none yet. */
    public long revision(final NetworkUuid network) {
        final Feed feed = this.feeds.get(network);
        return feed == null ? 0L : feed.revision;
    }

    @Override
    public CompoundTag save(final CompoundTag tag, final HolderLookup.Provider registries) {
        final ListTag networks = new ListTag();
        for (final Map.Entry<NetworkUuid, Feed> entry : this.feeds.entrySet()) {
            final ListTag events = new ListTag();
            for (final NetworkEvent event : entry.getValue().events) {
                events.add(save(event));
            }
            final CompoundTag network = new CompoundTag();
            network.putString(NETWORK, entry.getKey().asString());
            network.put(EVENTS, events);
            networks.add(network);
        }
        tag.put(NETWORKS, networks);
        return tag;
    }

    /* The feed of {@code network} with what is past seven days at {@code now} let go, or null for a network without. */
    @Nullable
    private Feed feed(final NetworkUuid network, final long now) {
        final Feed feed = this.feeds.get(network);
        if (feed != null && trim(feed, now)) {
            setDirty();
        }
        return feed;
    }

    /* Lets the events past seven days at {@code now}, and those past the most a log keeps, go; whether any went. */
    private static boolean trim(final Feed feed, final long now) {
        boolean trimmed = false;
        while (!feed.events.isEmpty() && (feed.events.size() > MOST
                || now - feed.events.peekFirst().time() > KEPT_TICKS)) {
            feed.events.removeFirst();
            trimmed = true;
        }
        return trimmed;
    }

    private static CompoundTag save(final NetworkEvent event) {
        final CompoundTag tag = new CompoundTag();
        tag.putLong(TIME, event.time());
        tag.putLong(DAY_TIME, event.dayTime());
        tag.putByte(LEVEL, (byte) event.level().id());
        if (event.machine() != null) {
            tag.putUUID(MACHINE, event.machine().value());
        }
        tag.putString(MACHINE_NAME, event.machineName());
        tag.put(MESSAGE, TextTags.write(event.message()));
        return tag;
    }

    private static NetworkEventLog load(final CompoundTag tag, final HolderLookup.Provider registries) {
        final NetworkEventLog log = new NetworkEventLog();
        for (final Tag saved : tag.getList(NETWORKS, Tag.TAG_COMPOUND)) {
            final CompoundTag network = (CompoundTag) saved;
            final Optional<NetworkUuid> id = NetworkUuid.parse(network.getString(NETWORK));
            if (id.isEmpty()) {
                continue;
            }
            final Feed feed = new Feed();
            for (final Tag one : network.getList(EVENTS, Tag.TAG_COMPOUND)) {
                feed.events.addLast(load((CompoundTag) one));
            }
            log.feeds.put(id.get(), feed);
        }
        return log;
    }

    private static NetworkEvent load(final CompoundTag tag) {
        return new NetworkEvent(tag.getLong(TIME), tag.getLong(DAY_TIME), EventLevel.byId(tag.getByte(LEVEL)),
                tag.hasUUID(MACHINE) ? new NodeUuid(tag.getUUID(MACHINE)) : null, tag.getString(MACHINE_NAME),
                TextTags.read(tag.getCompound(MESSAGE)));
    }

    /* One network's events, oldest first, and the number that changes with each one added. */
    private static final class Feed {

        private final ArrayDeque<NetworkEvent> events = new ArrayDeque<>();
        private long revision;
    }
}
