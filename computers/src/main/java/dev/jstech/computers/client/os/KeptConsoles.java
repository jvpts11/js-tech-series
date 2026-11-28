/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;

/**
 * The console of each machine whose terminal was closed, kept on this client so opening the terminal again finds
 * what was said at its prompt, as a desktop keeps its programs while its monitor is left ({@link WindowLayouts}). A
 * kept console still listens, so what the machine says while nobody looks is there too. Bounded, the least recently
 * seen let go past {@link #MOST}, and all let go as the player leaves a world; one let go stops listening.
 */
public final class KeptConsoles {

    /** How many machines' consoles are kept at once. */
    private static final int MOST = 16;

    private static final Map<BlockPos, ShellView> KEPT = new LinkedHashMap<>(MOST, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(final Map.Entry<BlockPos, ShellView> eldest) {
            if (size() <= MOST) {
                return false;
            }
            eldest.getValue().release();
            return true;
        }
    };

    private KeptConsoles() {
    }

    /** The console of the machine at {@code host}: the one kept for it, or {@code made} the first time. */
    public static ShellView of(final BlockPos host, final Supplier<ShellView> made) {
        return KEPT.computeIfAbsent(host.immutable(), at -> made.get());
    }

    /** Lets go of every kept console, as the player leaves a world. */
    public static void forgetAll() {
        KEPT.values().forEach(ShellView::release);
        KEPT.clear();
    }
}
