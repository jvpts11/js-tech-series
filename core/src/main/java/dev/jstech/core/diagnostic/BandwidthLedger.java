/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.diagnostic;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.LongSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * What the server sent over a minute, to one player or to everyone: the bytes of each second, for a graph; the bytes of
 * each kind of packet, a mod's payloads by their id and the game's packets by theirs; and the block entities whose
 * updates took the most.
 *
 * <p>The total is exact to the second. Each kind and each sender is kept as two minutes, this one and the one before,
 * and its rate over the last sixty seconds is read as all of this minute and the part of the last one still inside
 * them, so a ledger holds two numbers for each, however long the server runs.
 */
public final class BandwidthLedger {

    private final LongSupplier clock;
    private final long[] seconds = new long[WINDOW_SECONDS];
    private final Map<String, long[]> kinds = new HashMap<>();
    private final Map<Sender, long[]> senders = new HashMap<>();
    private long lastSecond = Long.MIN_VALUE;
    private long minuteStart = Long.MIN_VALUE;

    /** How many seconds the ledger reads over. */
    public static final int WINDOW_SECONDS = 60;

    private static final int THIS_MINUTE = 0;
    private static final int LAST_MINUTE = 1;

    /** A ledger whose time, in whole seconds, is read from {@code clock}. */
    public BandwidthLedger(final LongSupplier clock) {
        this.clock = clock;
    }

    /** Counts {@code bytes} sent of {@code kind}, as an update of {@code sender} when one sent it. */
    public synchronized void count(final String kind, final @Nullable Sender sender, final long bytes) {
        final long now = roll();
        this.seconds[Math.floorMod(now, WINDOW_SECONDS)] += bytes;
        this.kinds.computeIfAbsent(kind, k -> new long[2])[THIS_MINUTE] += bytes;
        if (sender != null) {
            this.senders.computeIfAbsent(sender, s -> new long[2])[THIS_MINUTE] += bytes;
        }
    }

    /** The bytes a second sent over the last minute. */
    public synchronized long bytesPerSecond() {
        roll();
        long sum = 0L;
        for (final long second : this.seconds) {
            sum += second;
        }
        return sum / WINDOW_SECONDS;
    }

    /** The bytes sent in each of the last sixty seconds, the oldest first. */
    public synchronized long[] lastMinute() {
        final long now = roll();
        final long[] out = new long[WINDOW_SECONDS];
        for (int i = 0; i < WINDOW_SECONDS; i++) {
            out[i] = this.seconds[Math.floorMod(now - (WINDOW_SECONDS - 1) + i, WINDOW_SECONDS)];
        }
        return out;
    }

    /** What each kind of packet took over the last minute, the most first. */
    public synchronized List<Rate> kinds() {
        final long now = roll();
        final List<Rate> out = new ArrayList<>();
        this.kinds.forEach((kind, minutes) -> out.add(new Rate(kind, rate(minutes, now))));
        out.sort(Comparator.comparingLong(Rate::bytesPerSecond).reversed().thenComparing(Rate::name));
        return out;
    }

    /** What each mod's packets took over the last minute, the game's own under {@code minecraft}, the most first. */
    public List<Rate> mods() {
        final Map<String, Long> byMod = new HashMap<>();
        for (final Rate kind : kinds()) {
            final int colon = kind.name().indexOf(':');
            final String mod = colon < 0 ? ResourceLocation.DEFAULT_NAMESPACE : kind.name().substring(0, colon);
            byMod.merge(mod, kind.bytesPerSecond(), Long::sum);
        }
        final List<Rate> out = new ArrayList<>();
        byMod.forEach((mod, rate) -> out.add(new Rate(mod, rate)));
        out.sort(Comparator.comparingLong(Rate::bytesPerSecond).reversed().thenComparing(Rate::name));
        return out;
    }

    /** The {@code count} block entities whose updates took the most over the last minute, the most first. */
    public synchronized List<SenderRate> senders(final int count) {
        final long now = roll();
        final List<SenderRate> out = new ArrayList<>();
        this.senders.forEach((sender, minutes) -> out.add(new SenderRate(sender, rate(minutes, now))));
        out.sort(Comparator.comparingLong(SenderRate::bytesPerSecond).reversed());
        return List.copyOf(out.subList(0, Math.min(count, out.size())));
    }

    /**
     * A block entity whose updates are sent.
     *
     * @param dimension the dimension it is in
     * @param pos       where it is
     * @param type      its block entity type
     */
    public record Sender(ResourceKey<Level> dimension, BlockPos pos, ResourceLocation type) {
    }

    /**
     * What one kind or one mod took.
     *
     * @param name           the kind's id, or the mod's
     * @param bytesPerSecond its bytes a second over the last minute
     */
    public record Rate(String name, long bytesPerSecond) {
    }

    /**
     * What one block entity's updates took.
     *
     * @param sender         the block entity
     * @param bytesPerSecond its bytes a second over the last minute
     */
    public record SenderRate(Sender sender, long bytesPerSecond) {
    }

    /*
     * Brings the ledger to the second it is now: the seconds since the last count are emptied, and once a minute has
     * passed this minute becomes the last, or both are emptied when more than one has.
     */
    private long roll() {
        final long now = this.clock.getAsLong();
        if (this.lastSecond == Long.MIN_VALUE) {
            this.lastSecond = now;
            this.minuteStart = now;
            return now;
        }
        for (long second = this.lastSecond + 1; second <= now && second <= this.lastSecond + WINDOW_SECONDS;
                second++) {
            this.seconds[Math.floorMod(second, WINDOW_SECONDS)] = 0L;
        }
        this.lastSecond = Math.max(this.lastSecond, now);
        final long minutes = (now - this.minuteStart) / WINDOW_SECONDS;
        if (minutes > 0) {
            this.minuteStart += minutes * WINDOW_SECONDS;
            rollMinutes(this.kinds, minutes);
            rollMinutes(this.senders, minutes);
        }
        return now;
    }

    /* The bytes a second over the last sixty seconds: this minute's, and the share of the last still inside them. */
    private long rate(final long[] minutes, final long now) {
        final long into = Math.min(WINDOW_SECONDS, Math.max(0L, now - this.minuteStart));
        return (minutes[LAST_MINUTE] * (WINDOW_SECONDS - into) / WINDOW_SECONDS + minutes[THIS_MINUTE])
                / WINDOW_SECONDS;
    }

    private static <K> void rollMinutes(final Map<K, long[]> counts, final long minutes) {
        final Iterator<long[]> it = counts.values().iterator();
        while (it.hasNext()) {
            final long[] counted = it.next();
            counted[LAST_MINUTE] = minutes == 1 ? counted[THIS_MINUTE] : 0L;
            counted[THIS_MINUTE] = 0L;
            if (counted[LAST_MINUTE] == 0L) {
                it.remove();
            }
        }
    }
}
