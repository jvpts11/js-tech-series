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
 * answers null, and anybody may do anything with it. A block whose block entity is owned, and an owned entity, are
 * kept from being broken, and from being used, by whoever its ownership does not let.
 */
public interface IOwned {

    /** Whose it is and who else may do what with it, or null when it is nobody's. */
    @Nullable
    Ownership ownership();

    /** Whether {@code player} may use it. */
    default boolean mayUse(final ServerPlayer player) {
        final Ownership ownership = ownership();
        return ownership == null || ownership.mayUse(player);
    }

    /** Whether {@code player} may open its screen. */
    default boolean mayOpen(final ServerPlayer player) {
        final Ownership ownership = ownership();
        return ownership == null || ownership.mayOpen(player);
    }

    /** Whether {@code player} may change how it is set up. */
    default boolean maySetUp(final ServerPlayer player) {
        final Ownership ownership = ownership();
        return ownership == null || ownership.maySetUp(player);
    }

    /** Whether {@code player} may break it. */
    default boolean mayBreak(final ServerPlayer player) {
        final Ownership ownership = ownership();
        return ownership == null || ownership.mayBreak(player);
    }
}
