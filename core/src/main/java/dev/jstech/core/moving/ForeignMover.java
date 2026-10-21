/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import dev.jstech.core.region.Box;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * A moving structure of another mod as the kit sees it, an obstacle that moves: where its own places stand in the
 * world, the box of its places its blocks are in, what reads its blocks at those places, and how it goes, its pivot's
 * speed and its spin. The kit's structures meet its blocks as they meet each other's, moved as it moves them and never
 * moving it; none of its blocks is broken by a blow.
 *
 * @param name   what a player calls the mod of it
 * @param frame  where its places stand in the world, as a frame turns a structure's yard
 * @param blocks the box of its places its blocks are in
 * @param reads  what reads its blocks at its own places
 * @param loaded the level whose chunks its places are in, read only where they are loaded; null when its places are
 *               no level's, as a contraption's own
 * @param speed  how fast its pivot goes, in metres a second
 * @param spin   how fast it turns about its pivot, in radians a second about each axis of the world
 */
public record ForeignMover(String name, StructureFrame frame, Box blocks, BlockGetter reads, @Nullable Level loaded,
                           Vec3 speed, Vec3 spin) {

    /* Ticks a second, for how fast it goes from where it stood a tick ago. */
    private static final double TICKS = 20;

    /**
     * The structure that stands at {@code frame} this tick and stood at {@code was} the last, both turned about the
     * same pivot: how fast it goes and turns is the step between the two.
     */
    public static ForeignMover moved(final String name, final StructureFrame was, final StructureFrame frame,
                                     final Box blocks, final BlockGetter reads, @Nullable final Level loaded) {
        final StructurePose from = was.pose();
        final StructurePose to = frame.pose();
        final double[] turned = new double[3];
        MovingStructure.spinBetween(from, to, turned);
        return new ForeignMover(name, frame, blocks, reads, loaded,
                new Vec3((to.x() - from.x()) * TICKS, (to.y() - from.y()) * TICKS, (to.z() - from.z()) * TICKS),
                new Vec3(turned[0] * TICKS, turned[1] * TICKS, turned[2] * TICKS));
    }
}
