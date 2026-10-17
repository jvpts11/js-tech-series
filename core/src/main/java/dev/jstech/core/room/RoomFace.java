/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.room;

import dev.jstech.core.air.AirMix;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * A face of a room: one of its blocks of air on one side, and on the other what bounds it, with what lies beyond, as
 * the room's shape was last worked out.
 *
 * @param cell        the room's block of air on this side
 * @param side        the way from it to what bounds it
 * @param block       what bounds it: a wall, a door, or the open block of a hole
 * @param kind        what kind of bound it is
 * @param area        the share of the face gas crosses, 0 to 1
 * @param rating      the difference in pressure it stands before it bursts, in kilopascals; 0 for a hole
 * @param beyond      what lies past it: the block beyond a wall or door, the open block itself for a hole
 * @param outside     the pressure beyond it when no room is there, in kilopascals: the place's, or the water's
 * @param outsideAir  the air beyond it when no room is there
 * @param water       whether water lies beyond it, pressing in rather than air
 */
public record RoomFace(BlockPos cell, Direction side, BlockPos block, Kind kind, double area, double rating,
                       BlockPos beyond, double outside, AirMix outsideAir, boolean water) {

    /** Whether gas crosses it. */
    public boolean leaks() {
        return this.area > 0;
    }

    /** What bounds a room on a face. */
    public enum Kind {
        /** A wall that holds the air. */
        WALL,
        /** A wall whose cracks let some air through. */
        CRACK,
        /** A door, shut or open. */
        DOOR,
        /** No wall at all: the open way out of a room whose walls were opened. */
        HOLE
    }
}
