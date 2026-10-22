/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.modify;

import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;

/**
 * A change to the world generation of the game or of any mod, read from a datapack, made as the server starts and
 * before its worlds are made ({@link WorldgenModifiers}). A mod adds a kind of its own with
 * {@link WorldgenModifiers#declareKind}.
 */
public interface IWorldgenModifier {

    /** The kind it is, as its file names it in {@code "type"}. */
    ResourceLocation kind();

    /** What it changes, by its id, for the log and the dump. */
    ResourceLocation changes();

    /** Makes the change in {@code registries}; true when it did, else what is wrong added to {@code problems}. */
    boolean apply(RegistryAccess registries, List<String> problems);
}
