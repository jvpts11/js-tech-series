/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.crash;

import dev.jstech.core.id.IStableId;
import dev.jstech.core.id.StableIds;

/**
 * Why a system went down on its own: what its crash page says happened. A system of the Frames family shows the first
 * on its blue page and the second on its red one; a Unix kernel panics over all three in its own words.
 */
public enum CrashCause implements IStableId {

    /** A program asked for memory the machine no longer had to give. */
    MEMORY(0),

    /** A part the system was running on was taken out from under it while it ran. */
    HARDWARE(1),

    /** The system's first process, the one every other descends from, was killed. */
    INIT_KILLED(2);

    private static final StableIds<CrashCause> IDS = StableIds.of(CrashCause.class);

    private final int id;

    CrashCause(final int id) {
        this.id = id;
    }

    /** The cause that declares {@code id}; an id none declares reads as memory. */
    public static CrashCause byId(final int id) {
        return IDS.byId(id, MEMORY);
    }

    @Override
    public int id() {
        return this.id;
    }
}
