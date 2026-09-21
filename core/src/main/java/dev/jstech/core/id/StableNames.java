/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.id;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The constants of one {@link IStableName} enum, found by their names. Built once per enum or per codec: a duplicate
 * or malformed name fails right there, when the enum or the codec is first used.
 *
 * <p>Pure Java with no Minecraft types, so enums used by unit-tested logic can hold one.
 *
 * @param <E> the enum
 */
public final class StableNames<E extends Enum<E> & IStableName> {

    private final String typeName;
    private final Map<String, E> byName;

    private StableNames(final String typeName, final Map<String, E> byName) {
        this.typeName = typeName;
        this.byName = byName;
    }

    /**
     * Indexes every constant of {@code type} by its name.
     *
     * @throws IllegalStateException if a constant declares a name that is empty, holds anything but lowercase
     *                               letters, digits and underscores, or is one another constant already declares
     */
    public static <E extends Enum<E> & IStableName> StableNames<E> of(final Class<E> type) {
        final Map<String, E> byName = new LinkedHashMap<>();
        for (final E constant : type.getEnumConstants()) {
            final String name = constant.serializedName();
            if (!wellFormed(name)) {
                throw new IllegalStateException(type.getSimpleName() + "." + constant.name() + " declares the name '"
                        + name + "', but a name is lowercase letters, digits and underscores");
            }
            final E taken = byName.putIfAbsent(name, constant);
            if (taken != null) {
                throw new IllegalStateException(type.getSimpleName() + "." + constant.name() + " declares the name '"
                        + name + "', which " + taken.name() + " already declares");
            }
        }
        return new StableNames<>(type.getSimpleName(), Collections.unmodifiableMap(byName));
    }

    /** The constant that declares {@code name}, or null when none does. */
    @Nullable
    public E find(@Nullable final String name) {
        return name == null ? null : byName.get(name);
    }

    /** The constant that declares {@code name}, or {@code fallback} when none does. */
    public E byName(@Nullable final String name, final E fallback) {
        final E found = find(name);
        return found != null ? found : fallback;
    }

    /** Every name, in declaration order, for a message that says what would have been accepted. */
    public Collection<String> names() {
        return byName.keySet();
    }

    /** The enum's simple name, for the same messages. */
    public String typeName() {
        return typeName;
    }

    private static boolean wellFormed(@Nullable final String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        for (int i = 0; i < name.length(); i++) {
            final char c = name.charAt(i);
            if (!(c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '_')) {
                return false;
            }
        }
        return true;
    }
}
