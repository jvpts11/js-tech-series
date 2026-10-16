/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.progression;

import dev.jstech.core.text.TextKey;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/**
 * A first a team reaches once, of an ordered set a mod declares ({@link Milestones}): a suborbital flight, an orbit, a
 * landing on the Moon. Reaching one also gives the team the knowledge of the same id, so a recipe can ask for it.
 *
 * @param id   its id, saved in worlds: {@code jsspace:orbit}
 * @param name its name as a player reads it
 */
public record Milestone(ResourceLocation id, TextKey name) {

    public Milestone {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
    }

    /** The knowledge a team gains as it reaches it. */
    public Knowledge knowledge() {
        return Knowledge.of(this.id, this.name);
    }
}
