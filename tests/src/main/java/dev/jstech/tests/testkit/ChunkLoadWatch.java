/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.testkit;

import dev.jstech.tests.JsTests;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ChunkEvent;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Watches for a chunk the series' own code loaded by reading it: a block entity or a block asked for at a place
 * whose chunk was not loaded, which the game answers by loading the chunk then and there, on the server's thread.
 * That costs a disk read or a generation in the middle of a tick, and a chunk loaded that way next to one being
 * unloaded can hold the unload up for good.
 *
 * <p>A chunk loads with the reader's call still on the thread's stack, so the first frame of the series' main code
 * on it names the line that read it. Each line is reported once, with how many chunks it has loaded so far.
 */
public final class ChunkLoadWatch {

    /** Where the series' main code lives; the tests' own frames are not what this watches. */
    private static final String[] WATCHED = {"dev.jstech.core.", "dev.jstech.computers.", "dev.jstech.industrial."};
    private static final StackWalker WALKER = StackWalker.getInstance();
    private static final Map<String, AtomicInteger> BY_LINE = new ConcurrentHashMap<>();

    private ChunkLoadWatch() {
    }

    /** Starts watching, on the game's bus. */
    public static void register() {
        NeoForge.EVENT_BUS.addListener(ChunkLoadWatch::onLoad);
    }

    private static void onLoad(final ChunkEvent.Load event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        final Optional<String> reader = WALKER.walk(frames -> frames
                .map(frame -> frame.getClassName() + "." + frame.getMethodName() + ":" + frame.getLineNumber())
                .filter(ChunkLoadWatch::watched)
                .findFirst());
        reader.ifPresent(line -> {
            final int count = BY_LINE.computeIfAbsent(line, l -> new AtomicInteger()).incrementAndGet();
            if (count == 1 || Integer.bitCount(count) == 1) {
                final ChunkPos pos = event.getChunk().getPos();
                JsTests.LOGGER.warn("[JSC-CHUNKLOAD] chunk {} loaded by reading it at {} ({} so far)", pos, line,
                        count);
            }
        });
    }

    private static boolean watched(final String frame) {
        for (final String prefix : WATCHED) {
            if (frame.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
