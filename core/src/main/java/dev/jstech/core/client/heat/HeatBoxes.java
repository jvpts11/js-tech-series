/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.heat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.function.LongPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

/**
 * Boxes of one colour drawn over blocks and things, each face turned outward so the back faces are left out, and a
 * block's faces against another of the same set left out too, so a run of blocks shows as one shape.
 */
final class HeatBoxes {

    private HeatBoxes() {
    }

    /**
     * The box of the block at {@code pos}, a little bigger than it, but for the faces against a block that
     * {@code alsoDrawn} says is drawn too.
     */
    static void block(final VertexConsumer consumer, final PoseStack.Pose pose, final BlockPos pos, final double grow,
                      final int colour, final LongPredicate alsoDrawn) {
        final float x0 = (float) (pos.getX() - grow);
        final float y0 = (float) (pos.getY() - grow);
        final float z0 = (float) (pos.getZ() - grow);
        final float x1 = (float) (pos.getX() + 1 + grow);
        final float y1 = (float) (pos.getY() + 1 + grow);
        final float z1 = (float) (pos.getZ() + 1 + grow);
        for (final Direction face : Direction.values()) {
            if (!alsoDrawn.test(pos.relative(face).asLong())) {
                face(consumer, pose, face, x0, y0, z0, x1, y1, z1, colour);
            }
        }
    }

    /** The whole of {@code box}. */
    static void box(final VertexConsumer consumer, final PoseStack.Pose pose, final AABB box, final int colour) {
        for (final Direction face : Direction.values()) {
            face(consumer, pose, face, (float) box.minX, (float) box.minY, (float) box.minZ, (float) box.maxX,
                    (float) box.maxY, (float) box.maxZ, colour);
        }
    }

    /* One face of the box, its corners counter-clockwise as seen from outside it. */
    private static void face(final VertexConsumer consumer, final PoseStack.Pose pose, final Direction face,
                             final float x0, final float y0, final float z0, final float x1, final float y1,
                             final float z1, final int colour) {
        switch (face) {
            case DOWN -> quad(consumer, pose, colour, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
            case UP -> quad(consumer, pose, colour, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
            case NORTH -> quad(consumer, pose, colour, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0);
            case SOUTH -> quad(consumer, pose, colour, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
            case WEST -> quad(consumer, pose, colour, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
            case EAST -> quad(consumer, pose, colour, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1);
        }
    }

    private static void quad(final VertexConsumer consumer, final PoseStack.Pose pose, final int colour,
                             final float ax, final float ay, final float az, final float bx, final float by,
                             final float bz, final float cx, final float cy, final float cz, final float dx,
                             final float dy, final float dz) {
        consumer.addVertex(pose, ax, ay, az).setColor(colour);
        consumer.addVertex(pose, bx, by, bz).setColor(colour);
        consumer.addVertex(pose, cx, cy, cz).setColor(colour);
        consumer.addVertex(pose, dx, dy, dz).setColor(colour);
    }
}
