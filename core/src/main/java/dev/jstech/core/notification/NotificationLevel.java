/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.notification;

import java.util.Optional;

/**
 * How much a notification matters: information, a warning or an alert. The level gives it its icon, its colour, its
 * sound and how long it stays on the screen, and the player filters the history by it.
 *
 * <p>Pure: no game types.
 */
public enum NotificationLevel {

    /** Something done or worth knowing: a craft finished, a research completed. */
    INFO("info"),
    /** Something that wants looking at soon: machines on their batteries, a disk nearly full. */
    WARNING("warning"),
    /** Something wrong now: a machine burned, a reactor running hot. */
    ALERT("alert");

    private final String serializedName;

    NotificationLevel(final String serializedName) {
        this.serializedName = serializedName;
    }

    /** The name the level is written and sent as. */
    public String serializedName() {
        return this.serializedName;
    }

    /** How long a notification of this level stays on the screen unless its kind says otherwise, in milliseconds. */
    public long defaultMillis() {
        return switch (this) {
            case INFO -> 5_000L;
            case WARNING -> 8_000L;
            case ALERT -> 12_000L;
        };
    }

    /** The level written as {@code name}, or none. */
    public static Optional<NotificationLevel> byName(final String name) {
        for (final NotificationLevel level : values()) {
            if (level.serializedName.equals(name)) {
                return Optional.of(level);
            }
        }
        return Optional.empty();
    }
}
