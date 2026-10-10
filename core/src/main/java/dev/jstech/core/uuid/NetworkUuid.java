/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.uuid;

import java.util.UUID;
import java.util.Objects;
import java.util.Optional;

/**
 * Strongly-typed wrapper for the UUID of an entire network.
 */
public record NetworkUuid(UUID value) {

    public NetworkUuid {
        Objects.requireNonNull(value, "value must not be null");
    }

    public static NetworkUuid random() {
        return new NetworkUuid(UUID.randomUUID());
    }

    public static NetworkUuid fromString(String s) {
        Objects.requireNonNull(s, "s must not be null");
        return new NetworkUuid(UUID.fromString(s));
    }

    /** The id a string spells, or nothing when it is not a UUID: for text read from a save, which may be damaged. */
    public static Optional<NetworkUuid> parse(final String s) {
        Objects.requireNonNull(s, "s must not be null");
        try {
            return Optional.of(new NetworkUuid(UUID.fromString(s)));
        } catch (final IllegalArgumentException notAUuid) {
            return Optional.empty();
        }
    }

    public String asString() {
        return value.toString();
    }
}
