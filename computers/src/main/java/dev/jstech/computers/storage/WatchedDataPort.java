/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.storage;

import java.util.List;
import java.util.Objects;

/**
 * A port that says each time something really goes through it: an insert or an extract that moves anything and is
 * not a simulation. A crafting bus's face is watched so its lamps blink while the crafting engine feeds the machine
 * or collects from it; asking what the port holds says nothing.
 */
public final class WatchedDataPort implements IDataPort {

    private final IDataPort delegate;
    private final Runnable onMove;

    /** A port passing everything to {@code delegate}, running {@code onMove} after each real move. */
    public WatchedDataPort(final IDataPort delegate, final Runnable onMove) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.onMove = Objects.requireNonNull(onMove, "onMove");
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public long insert(final StorageKey key, final long amount, final boolean simulate) {
        return watched(delegate.insert(key, amount, simulate), simulate);
    }

    @Override
    public long extract(final StorageKey key, final long amount, final boolean simulate) {
        return watched(delegate.extract(key, amount, simulate), simulate);
    }

    @Override
    public long count(final StorageKey key) {
        return delegate.count(key);
    }

    @Override
    public List<StorageKey> available() {
        return delegate.available();
    }

    private long watched(final long moved, final boolean simulate) {
        if (!simulate && moved > 0L) {
            onMove.run();
        }
        return moved;
    }
}
