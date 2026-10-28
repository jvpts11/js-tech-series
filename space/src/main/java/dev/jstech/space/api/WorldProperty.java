/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.api;

import java.util.Objects;

/**
 * Something a team can come to know of a world, as an instrument reveals it: its orbit, its gravity, its atmosphere,
 * what lies on its ground. A world's share known is how much of every property its team knows.
 *
 * @param name      the key of its name in the language files
 * @param essential whether a crew needs it known to land there, since it is what kills: the atmosphere, the
 *                  temperature, the radiation, the gravity and the relief
 */
public record WorldProperty(String name, boolean essential) {

    public WorldProperty {
        Objects.requireNonNull(name, "name");
    }
}
