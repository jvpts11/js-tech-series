/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.config;

/**
 * The values of a file's settings as something holds them: the file as read, or a settings screen's changes not kept
 * yet. A rule between settings, and whether a setting is active, are asked of whichever is in hand.
 */
@FunctionalInterface
public interface IConfigValues {

    /** The value {@code key} has here. */
    <T> T get(ConfigKey<T> key);
}
