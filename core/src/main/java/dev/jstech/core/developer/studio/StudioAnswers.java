/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer.studio;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The player's side of the studio's questions: each sent to the server with a number, and its answer handed to whoever
 * asked when it comes back, on the game's own thread.
 */
public final class StudioAnswers {

    private static final AtomicInteger NEXT = new AtomicInteger();
    private static final Map<Integer, Consumer<CompoundTag>> WAITING = new ConcurrentHashMap<>();

    private StudioAnswers() {
    }

    /** Asks the server's service {@code service} {@code args}; {@code answered} is handed its answer. */
    public static void ask(final String service, final CompoundTag args, final Consumer<CompoundTag> answered) {
        final int id = NEXT.incrementAndGet();
        WAITING.put(id, answered);
        PacketDistributor.sendToServer(new StudioAskPayload(id, service, args));
    }

    /** Hands the answer numbered {@code id} to whoever asked; one nobody waits for any more is dropped. */
    static void answered(final int id, final CompoundTag answer) {
        final Consumer<CompoundTag> waiting = WAITING.remove(id);
        if (waiting != null) {
            waiting.accept(answer);
        }
    }
}
