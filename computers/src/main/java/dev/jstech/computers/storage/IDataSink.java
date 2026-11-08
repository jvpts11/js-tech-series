/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.storage;

/**
 * A destination that accepts a quantity of one data type (an item OR a fluid) without caring which.
 */
public interface IDataSink {

    long insert(StorageKey key, long amount, boolean simulate);

    /**
     * Drops what it has taken in and holds back until its Operation settles, for a machine that went dark with it on
     * the way, and answers how much that was. A sink that hands everything on at once holds nothing.
     */
    default long dropHeld() {
        return 0L;
    }
}
