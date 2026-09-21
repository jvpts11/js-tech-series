/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.operation;

import dev.jstech.core.id.IStableId;
import dev.jstech.core.id.StableIds;
import java.util.Optional;

/**
 * Where an Operation has got to: the eight states one can be in, and no others.
 *
 * <p>Each carries a number of its own, because a state is written into the log a world saves and sent to
 * whoever is watching it. Numbering starts at one, so that zero is free to mean that nothing is known about
 * an Operation rather than meaning the first of the eight, which is what an unknown one used to look like.
 */
public enum OperationStatus implements IStableId {

    PENDING(1),

    PROCESSING(2),

    WAITING(3),

    COMPLETED(4),

    COMPLETED_PARTIAL(5),

    FAILED(6),

    RESOURCE_LOCKED(7),

    DISCARDED(8);

    /** Nothing is known about that Operation: it is not one of the eight, and never travels as one. */
    public static final byte UNKNOWN = 0;

    private static final StableIds<OperationStatus> IDS = StableIds.of(OperationStatus.class);

    private final int id;

    OperationStatus(final int id) {
        this.id = id;
    }

    /** The one that number stands for, or nothing at all, which is what an Operation nobody knows about is. */
    public static Optional<OperationStatus> of(final int id) {
        return Optional.ofNullable(IDS.find(id));
    }

    @Override
    public int id() {
        return this.id;
    }

    public boolean isTerminal() {
        return this == COMPLETED
                || this == COMPLETED_PARTIAL
                || this == FAILED
                || this == RESOURCE_LOCKED
                || this == DISCARDED;
    }

    public boolean isActive() {
        return this == PENDING || this == PROCESSING || this == WAITING;
    }
}
