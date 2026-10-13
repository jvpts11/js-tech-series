/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.config;

/** Whose settings a file holds, which decides where it lives and when it is read. */
public enum ConfigSide {

    /** One player's own: read on their game only, from the game's config folder, and never sent anywhere. */
    CLIENT,
    /** The same on every game, server and player alike: read on each from its own config folder. */
    COMMON,
    /** A world's: read on the server when the world starts, from beside that world, and reset when it stops. */
    SERVER
}
