/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.operation;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The kinds of Operation the network can be asked to carry out, by id, from every mod of the series.
 *
 * <p>Two of the same id is refused rather than one replacing the other: an Operation's id is written into
 * saved worlds and sent over the wire, so which of two answered for it cannot be allowed to depend on the
 * order the mods loaded in.
 *
 * <p>They are added while the game loads and the registry is closed once it has, so that what a world
 * knows how to do does not change under it while it runs.
 */
public final class OperationTypeRegistry {

    private final Map<String, OperationType<?>> registry = new LinkedHashMap<>();
    private volatile boolean frozen;

    /**
     * Adds one, and refuses an id something already answers for.
     *
     * <p>The two refusals are not the same and are not treated the same. A taken id can only be two mods
     * declaring the same thing, which is a mistake in one of them and is worth stopping the load over while
     * somebody is there to read why. Adding one after the loading is done can happen in a world that is
     * already being played, and no mistake of an addon's is worth ending somebody's game over, so that one
     * is refused by answering nothing.
     *
     * <p>It says so by its answer rather than to a log, because this is plain logic that runs with no game
     * around it and there is no log to say it to.
     *
     * @return the type, so a declaration can keep what it registered, or null when the registry was closed
     */
    public synchronized <T extends IOperationArgs> OperationType<T> register(final OperationType<T> type) {
        if (type == null || type.id() == null) {
            return null;
        }
        if (this.frozen) {
            return null;
        }
        if (this.registry.containsKey(type.id())) {
            throw new IllegalStateException("Operation type already registered: " + type.id());
        }
        this.registry.put(type.id(), type);
        return type;
    }

    /** Closes the registry, as the Core does once every mod has loaded: nothing is added after. */
    public void freeze() {
        this.frozen = true;
    }

    /** Whether the registry has been closed. */
    public boolean isFrozen() {
        return this.frozen;
    }

    public Optional<OperationType<?>> get(final String id) {
        return Optional.ofNullable(this.registry.get(id));
    }

    public boolean contains(final String id) {
        return this.registry.containsKey(id);
    }

    /** Every kind there is, in the order they were added. */
    public Collection<OperationType<?>> all() {
        return Collections.unmodifiableCollection(this.registry.values());
    }

    public int size() {
        return this.registry.size();
    }
}
