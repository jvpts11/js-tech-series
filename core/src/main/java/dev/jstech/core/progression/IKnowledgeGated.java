/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.progression;

import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * A recipe that asks for a piece of knowledge, whatever works it: the recipe viewers and the manuals show it with a
 * padlock while the player's team does not know what it needs ({@link KnowledgeGate}).
 */
public interface IKnowledgeGated {

    /** The knowledge it asks for, or none. */
    Optional<ResourceLocation> knowledge();
}
