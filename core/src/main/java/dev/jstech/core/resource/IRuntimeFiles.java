/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.resource;

import java.util.Map;
import java.util.function.Supplier;

/**
 * The files of a pack a mod makes as the game loads, rather than ships: textures drawn from the game's own, models,
 * tags, loot tables, for things that exist only because of what the installed mods declared. Asked again whenever the
 * game reloads its resources or its data.
 */
@FunctionalInterface
public interface IRuntimeFiles {

    /**
     * Every file it gives now, by its path in the pack ({@code assets/mymod/models/item/tin_dust.json},
     * {@code data/c/tags/item/dusts/tin.json}), each made only when it is first read, and at most once per reload.
     */
    Map<String, Supplier<byte[]>> files();
}
