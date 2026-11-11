/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.backup;

import dev.jstech.core.id.IStableId;
import dev.jstech.core.id.IStableName;
import dev.jstech.core.id.StableIds;
import dev.jstech.core.id.StableNames;
import org.jetbrains.annotations.Nullable;

/**
 * How the network lost items that stopped existing, as its book of losses says it: what a backup can bring back.
 * Items that left the network, were exported or were crafted still exist, so they are never a loss
 * (computers/docs/design/programs.md). Each has the id it travels by and the name the book is saved with.
 */
public enum LossCause implements IStableId, IStableName {

    /** Corrupted, and the Defrag could not bring them back. */
    DEFRAG(0, "defrag"),
    /** On a disk that died. */
    DEAD_DISK(1, "dead_disk"),
    /** On a RAID 0 array that lost one of its drives, which takes the whole stripe with it. */
    RAID0(2, "raid0"),
    /** Destroyed by a DROP. */
    DROP(3, "drop");

    private final int id;
    private final String serializedName;

    private static final StableIds<LossCause> IDS = StableIds.of(LossCause.class);
    private static final StableNames<LossCause> NAMES = StableNames.of(LossCause.class);

    LossCause(final int id, final String serializedName) {
        this.id = id;
        this.serializedName = serializedName;
    }

    @Override
    public int id() {
        return id;
    }

    @Override
    public String serializedName() {
        return serializedName;
    }

    /** The cause that declares {@code id}; one no cause declares reads as a DROP's. */
    public static LossCause byId(final int id) {
        return IDS.byId(id, DROP);
    }

    /** The cause saved as {@code name}, or null for a name no cause declares. */
    @Nullable
    public static LossCause byName(final String name) {
        return NAMES.find(name);
    }
}
