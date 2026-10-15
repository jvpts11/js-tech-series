/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.protection;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * One protection the Core asks, a claim mod's or the game's own: whether an actor may do something to a place, or
 * nothing to say about it. A mod with protections of its own registers one with {@link Protection#register}.
 */
public interface IProtectionAdapter {

    /** What a protection says of an action. */
    enum Verdict {
        /** It lets it through. */
        ALLOW,
        /** It stops it. */
        DENY,
        /** It has nothing to say: the place is not its to protect. */
        ABSTAIN
    }

    /** Whether {@code actor} may do {@code action} to {@code pos} in {@code level}. */
    Verdict ask(Actor actor, ProtectedAction action, ServerLevel level, BlockPos pos);

    /**
     * Who holds the claim {@code pos} is in, as the player or team who answers for it, or none when it is not claimed
     * here: what tells whether the world's own work (a fire, a fluid) stays inside one claim.
     */
    default Optional<UUID> claimOwner(final ServerLevel level, final BlockPos pos) {
        return Optional.empty();
    }
}
