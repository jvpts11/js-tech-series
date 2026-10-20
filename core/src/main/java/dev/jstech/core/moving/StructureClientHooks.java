/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/**
 * What a player's game does as a moving structure's entity comes, changes and goes: the kit's entity is the same on
 * both sides, but only a player's game draws structures, so it hands its own answers in here as it starts, and a
 * dedicated server keeps these, which do nothing.
 */
public final class StructureClientHooks {

    /** What a game that draws nothing answers. */
    private static final IHooks NONE = new IHooks() {
    };
    private static volatile IHooks hooks = NONE;

    private StructureClientHooks() {
    }

    /** The answers of this game. */
    public static IHooks get() {
        return hooks;
    }

    /** Hands in a player's game's answers, once, as it starts. */
    public static void set(final IHooks from) {
        hooks = from;
    }

    /** What a player's game answers. */
    public interface IHooks {

        /** A structure's entity came into the player's level. */
        default void added(final MovingStructureEntity entity) {
        }

        /** A structure's entity left the player's level. */
        default void removed(final MovingStructureEntity entity) {
        }

        /** The server changed what it tells of a structure: its plot, its pivot or the box of its blocks. */
        default void changed(final MovingStructureEntity entity) {
        }

        /** The box of the world a structure fills on the player's game, or null when the game does not know it yet. */
        default @Nullable AABB box(final MovingStructureEntity entity) {
            return null;
        }

        /** The server told where a structure stood. */
        default void pose(final StructurePosePayload payload) {
        }

        /** The server told that a structure's blocks are about to change hands with the world's. */
        default void handOff(final StructureHandOffPayload payload) {
        }

        /** The server told what a structure shows of itself going: its engines, its glowing faces, its trails. */
        default void effects(final StructureEffectsPayload payload) {
        }
    }
}
