/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.id;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;

/**
 * The constants of one {@link IStableId} enum, found by their ids in constant time. Built once per enum, usually in a
 * static field of the enum itself: a duplicate or out-of-range id then fails the enum's class initialisation the first
 * time anything touches it, instead of surfacing later as the wrong constant read back from a save.
 *
 * <p>Pure Java with no Minecraft types, so enums used by unit-tested logic can hold one.
 *
 * @param <E> the enum
 */
public final class StableIds<E extends Enum<E> & IStableId> {

    /** The largest id a constant may declare: ids travel as one signed byte, so every id reads back as written. */
    public static final int MAX_ID = Byte.MAX_VALUE;

    private final E[] byId;

    private StableIds(final E[] byId) {
        this.byId = byId;
    }

    /**
     * Indexes every constant of {@code type} by its id.
     *
     * @throws IllegalStateException if a constant declares an id below 0 or above {@link #MAX_ID}, or one another
     *                               constant already declares
     */
    public static <E extends Enum<E> & IStableId> StableIds<E> of(final Class<E> type) {
        final E[] constants = type.getEnumConstants();
        int highest = -1;
        for (final E constant : constants) {
            final int id = constant.id();
            if (id < 0 || id > MAX_ID) {
                throw new IllegalStateException(type.getSimpleName() + "." + constant.name() + " declares id " + id
                        + ", outside 0 to " + MAX_ID);
            }
            highest = Math.max(highest, id);
        }
        @SuppressWarnings("unchecked")
        final E[] byId = (E[]) Array.newInstance(type, highest + 1);
        for (final E constant : constants) {
            final E taken = byId[constant.id()];
            if (taken != null) {
                throw new IllegalStateException(type.getSimpleName() + "." + constant.name() + " declares id "
                        + constant.id() + ", which " + taken.name() + " already declares");
            }
            byId[constant.id()] = constant;
        }
        return new StableIds<>(byId);
    }

    /** The constant that declares {@code id}, or null when none does. */
    @Nullable
    public E find(final int id) {
        return id >= 0 && id < byId.length ? byId[id] : null;
    }

    /** The constant that declares {@code id}, or {@code fallback} when none does, so a stale number never throws. */
    public E byId(final int id, final E fallback) {
        final E found = find(id);
        return found != null ? found : fallback;
    }
}
