/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.config;

/** When a change to a setting takes hold, which a settings screen says beside it. */
public enum ConfigApply {

    /** At once. */
    NOW,
    /** When the world's data is read again, by {@code /reload}. */
    RELOAD,
    /** When the game starts again. */
    RESTART
}
