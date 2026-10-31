/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import dev.jstech.core.region.Box;
import dev.jstech.core.uuid.StructureUuid;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * A moving structure as both sides of the game know it: where its blocks are in the yard, how they stand in the world
 * now and stood a tick ago. The server's is the structure itself; a player's game keeps its own, moved smoothly from
 * what the server tells it. Colliding, aiming and carrying what stands on it read only this, so they work the same on
 * both sides.
 */
public interface IStructureBody {

    StructureUuid id();

    /** The plot of the yard its blocks are kept in. */
    int plot();

    /** The level it is in; its blocks are in the same level's yard. */
    Level level();

    /** How its blocks stand in the world this tick. */
    StructureFrame frame();

    /** How its blocks stood in the world the tick before, so what stood on it is carried by how it moved. */
    StructureFrame previousFrame();

    /**
     * How its blocks stand for a ray of the world to meet them ({@link StructureRays}): as they stand this tick; on a
     * player's game, while the player's aim is found for a frame, as that frame draws them, so the block under the
     * crosshair, and outlined, is the block drawn there, frame after frame, however fast it goes.
     */
    default StructureFrame aimFrame() {
        return frame();
    }

    /** The box of the yard its blocks lie in, both corners included. */
    Box bounds();

    /** The box of the world its blocks fill this tick, however it is turned. */
    AABB worldBox();

    /** Whether its blocks are loaded and it can be collided with, aimed at and stood on. */
    boolean ready();
}
