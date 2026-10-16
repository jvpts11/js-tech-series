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
 * Something a team knows: a piece of research, a world discovered, a milestone passed. Any mod declares what a piece
 * of knowledge is ({@link Knowledges#declare}) and gives it to a team ({@link TeamProgress#grant}); a recipe may ask
 * for one before a machine runs it. Knowledge comes whole, or, for one that is {@code partial}, by shares from none to
 * all, and is known once all of it is.
 *
 * @param id      its id, saved in worlds and named by recipes: {@code myaddon:fusion}
 * @param name    its name as a player reads it, on a locked recipe among other places
 * @param partial whether a team comes to know it a share at a time, as a comprehension from 0 to 100%
 */
public record Knowledge(ResourceLocation id, TextKey name, boolean partial) {

    public Knowledge {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
    }

    /** A piece of knowledge that comes whole. */
    public static Knowledge of(final ResourceLocation id, final TextKey name) {
        return new Knowledge(id, name, false);
    }
}
