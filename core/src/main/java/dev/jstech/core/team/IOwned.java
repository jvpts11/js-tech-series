/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.team;

import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * A thing somebody owns, as another mod asks about it: a machine's block entity, an entity. A thing that is nobody's
 * answers null, and anybody may use it.
 */
public interface IOwned {

    /** Whose it is and who else may use it, or null when it is nobody's. */
    @Nullable
    Ownership ownership();

    /** Whether {@code player} may use it. */
    default boolean mayUse(final ServerPlayer player) {
        final Ownership ownership = ownership();
        return ownership == null || ownership.mayUse(player);
    }
}
