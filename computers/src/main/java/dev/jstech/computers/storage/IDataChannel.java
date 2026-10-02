/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.storage;

import java.util.List;

/**
 * One kind of data behind a block face (items, fluids, chemicals) and the four things the network ever
 * does with it: put some in, take some out, count it, list it. Every {@link StorageKey.Kind} has exactly one
 * channel type; {@link ExternalDataPort} is nothing but the channels a face offers, keyed by kind, so a new
 * kind of data cannot be wired into one path and forgotten in another.
 */
public interface IDataChannel {

    /** The kind of key this channel moves; keys of any other kind are refused. */
    StorageKey.Kind kind();

    /** Pushes up to {@code amount} of {@code key} into the block; returns how much it accepted. */
    long insert(StorageKey key, long amount, boolean simulate);

    /** Pulls up to {@code amount} of {@code key} out of the block; returns how much came out. */
    long extract(StorageKey key, long amount, boolean simulate);

    /** How much of {@code key} the block currently holds. */
    long count(StorageKey key);

    /** Every key of this kind the block currently holds, without duplicates. */
    List<StorageKey> available();

    /** How many places the block keeps this kind in: its slots or its tanks; none when it cannot tell. */
    default int places() {
        return 0;
    }

    /** How many of those places hold something. */
    default int placesUsed() {
        return 0;
    }

    /**
     * How much more of this kind the block has room for, by weight ({@link StorageKey#weight}): an item a thousand,
     * a millibucket one. What fits depends on what is put in, so it is told as if each empty place took a stack.
     */
    default long room() {
        return 0L;
    }
}
