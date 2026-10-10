/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.registry;

import java.util.function.Consumer;
import org.jetbrains.annotations.Nullable;

/**
 * Where common code finds something only a client can do, such as opening a screen.
 *
 * <p>A payload's handler lives in common code, so it cannot name a screen class. The client sets the real
 * implementation here during its setup, and a dedicated server, where nothing is ever set, does nothing when the
 * handler asks for it.
 *
 * @param <T> what the client brings
 */
public final class ClientHook<T> {

    @Nullable
    private volatile T instance;

    /** Sets what the client does; the last one set is the one used. */
    public void set(final T implementation) {
        instance = implementation;
    }

    /** Hands what the client set to {@code use}, or does nothing when nothing was set. */
    public void ifPresent(final Consumer<? super T> use) {
        final T set = instance;
        if (set != null) {
            use.accept(set);
        }
    }
}
