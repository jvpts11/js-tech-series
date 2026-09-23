/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.datagen;

import net.minecraft.resources.ResourceLocation;

/**
 * Reads a file name the way a declaration writes it.
 */
final class ContentFiles {

    private ContentFiles() {
    }

    /** {@code "block/monitor_side"} is the mod's own; {@code "minecraft:block/glass"} names its namespace. */
    static ResourceLocation named(final String modid, final String name) {
        return name.indexOf(':') >= 0 ? ResourceLocation.parse(name) : ResourceLocation.fromNamespaceAndPath(modid, name);
    }
}
