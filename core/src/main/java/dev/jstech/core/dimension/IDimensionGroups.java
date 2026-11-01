/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.dimension;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Which group a dimension is in, for the passages between two dimensions of one group: whatever makes one, a command
 * included, the player sees no loading screen, only the last picture before it held still until the land where they
 * arrive is there ({@link DimensionTravel#STILL}). A mod declares its groups once, as the game loads:
 *
 * <pre>{@code
 * DimensionTravel.group((server, dimension) -> MY_WORLDS.contains(dimension) ? MY_GROUP : null);
 * }</pre>
 */
@FunctionalInterface
public interface IDimensionGroups {

    /** The group {@code dimension} is in on {@code server}, or null when it is in none of these. */
    @Nullable ResourceLocation groupOf(MinecraftServer server, ResourceKey<Level> dimension);
}
