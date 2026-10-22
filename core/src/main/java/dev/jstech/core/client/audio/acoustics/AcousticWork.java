/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.audio.acoustics;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The acoustics kept off the game's thread: what the walls between two places let through, kept by the two blocks a
 * couple of seconds so a sound heard again from the same spot is weighed at once, and the weighing again of the
 * sounds playing, done on a thread of its own. The world is only read there, as the game's own sound thread reads it;
 * one round of weighing at a time, a round asked for while one runs left for the next.
 */
final class AcousticWork {

    /* How long what the walls let through is kept, in ticks, and how many pairs of places at most. */
    private static final long KEPT_TICKS = 40;
    private static final int MOST_KEPT = 4096;
    private static final Map<Pair, Kept> KEPT = new ConcurrentHashMap<>();
    private static final AtomicBoolean BUSY = new AtomicBoolean();
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(work -> {
        final Thread thread = new Thread(work, "jscore-acoustics");
        thread.setDaemon(true);
        return thread;
    });

    private AcousticWork() {
    }

    /**
     * What the walls between {@code listener} and {@code source} let through, at {@code now} in ticks: as kept for
     * the two blocks while it is fresh, else read from the world now and kept.
     */
    static float walls(final Level level, final Vec3 listener, final Vec3 source, final long now) {
        final Pair pair = new Pair(BlockPos.containing(listener).asLong(), BlockPos.containing(source).asLong());
        final Kept kept = KEPT.get(pair);
        if (kept != null && now - kept.at() < KEPT_TICKS) {
            return kept.through();
        }
        return wallsNow(level, listener, source, now);
    }

    /** What the walls let through read from the world now, and kept for the two blocks. */
    static float wallsNow(final Level level, final Vec3 listener, final Vec3 source, final long now) {
        final float through = Walls.through(level, listener, source);
        if (KEPT.size() >= MOST_KEPT) {
            KEPT.clear();
        }
        KEPT.put(new Pair(BlockPos.containing(listener).asLong(), BlockPos.containing(source).asLong()),
                new Kept(through, now));
        return through;
    }

    /** Runs {@code work} on the acoustics' own thread, unless a round runs there still. */
    static void inBackground(final Runnable work) {
        if (!BUSY.compareAndSet(false, true)) {
            return;
        }
        WORKER.execute(() -> {
            try {
                work.run();
            } finally {
                BUSY.set(false);
            }
        });
    }

    /** Forgets what was kept, as the player leaves the world. */
    static void forget() {
        KEPT.clear();
    }

    /* Two places by their blocks: where the listener is, and where the sound is. */
    private record Pair(long from, long to) {
    }

    /* What the walls let through, and when it was read. */
    private record Kept(float through, long at) {
    }
}
