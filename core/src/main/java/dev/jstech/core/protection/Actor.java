/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.protection;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * Who does something to the world, as protections weigh it: a player in person; a machine, a robot or a moving
 * structure acting for its owner, weighed as that owner whether they are online or not, never as a fake player that
 * every protection lets through or stops; or nobody, the world itself (fire spreading, a fluid flowing, an explosion
 * with no one behind it), weighed by where it comes from.
 */
public sealed interface Actor {

    /** A player in person. */
    static Actor of(final ServerPlayer player) {
        return new Person(player);
    }

    /** A machine, a robot or a structure acting for {@code owner}. */
    static Actor forOwner(final UUID owner) {
        return new ForOwner(owner);
    }

    /** The world itself, coming from {@code origin}, or from nowhere in particular when it is null. */
    static Actor world(@Nullable final BlockPos origin) {
        return new World(origin == null ? null : origin.immutable());
    }

    /** The player who answers for what is done: the player in person, or the owner; none for the world. */
    Optional<UUID> who();

    /**
     * A player in person.
     *
     * @param player the player
     */
    record Person(ServerPlayer player) implements Actor {

        public Person {
            Objects.requireNonNull(player, "player");
        }

        @Override
        public Optional<UUID> who() {
            return Optional.of(player.getUUID());
        }
    }

    /**
     * A machine, a robot or a moving structure acting for its owner.
     *
     * @param owner the owner it acts for
     */
    record ForOwner(UUID owner) implements Actor {

        public ForOwner {
            Objects.requireNonNull(owner, "owner");
        }

        @Override
        public Optional<UUID> who() {
            return Optional.of(owner);
        }
    }

    /**
     * The world itself.
     *
     * @param origin where it comes from, or null when from nowhere in particular
     */
    record World(@Nullable BlockPos origin) implements Actor {

        @Override
        public Optional<UUID> who() {
            return Optional.empty();
        }
    }
}
