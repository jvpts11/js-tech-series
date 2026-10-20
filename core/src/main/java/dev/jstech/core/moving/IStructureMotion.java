/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * A rule that moves a structure, exactly and cheaply, a tick at a time, as a lift goes up, a drawbridge swings, a dome
 * turns or a train runs on its rails: each tick it gives the next pose and the rule that goes on from there, so a rule
 * in the middle of its way is saved as it is and goes on after a restart. A mod adds a kind of its own with
 * {@link StructureMotions#register}.
 *
 * <pre>{@code
 * MovingStructures.move(structure, new TowardMotion(StructurePose.at(x, y + 10, z), 2, 0));   // up 10, 2 blocks/s
 * }</pre>
 */
public interface IStructureMotion {

    /** One tick of this rule from where the structure stands now. */
    Step step(StructurePose now, Context context);

    /** The id of this rule's kind, by which it is written and read back. */
    ResourceLocation type();

    /**
     * Whether this rule reads the world, as one riding rails does: such a rule moves on the server's own thread; one
     * that does not may be worked out beside it, with the others.
     */
    default boolean readsWorld() {
        return false;
    }

    /**
     * One tick of a rule: the pose the structure takes, and the rule that goes on from there, null when the rule is
     * done and the structure comes to rest.
     *
     * @param pose the pose after the tick
     * @param next the rule for the next tick, or null when done
     */
    record Step(StructurePose pose, @Nullable IStructureMotion next) {
    }

    /**
     * What a rule may know of the structure it moves: its frame now, and the level, which only a rule that
     * {@link #readsWorld() reads the world} is given.
     *
     * @param frame  how its blocks stand in the world now
     * @param world  the level, for a rule that reads it; null for one worked out beside the server's thread
     */
    record Context(StructureFrame frame, @Nullable Level world) {
    }
}
