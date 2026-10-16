/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.effect;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * Marks on the world that fade: the scorch an explosion leaves, the soot of a chimney on the wall beside it. Laid on
 * every face of a solid block within reach that faces open air, darkest nearest the middle and nothing at the edge,
 * then fading out over their time. On the player's game only.
 */
final class WorldMarks {

    private static final List<Mark> MARKS = new ArrayList<>();
    private static final int MOST_MARKS = 64;
    private static final int MOST_FACES = 512;
    /* How far off a face a mark lies, so it shows over it; and how finely each face is cut so its edge is soft. */
    private static final float OFF = 0.002F;
    private static final int CUTS = 4;
    private static final int CHANNEL = 255;
    private static final int ALPHA_SHIFT = 24;
    private static final double TICKS_A_SECOND = 20.0;

    private WorldMarks() {
    }

    /** Lays a mark of {@code argb} on the faces within {@code reach} of {@code at}, fading over {@code seconds}. */
    static void add(final ClientLevel level, final Vec3 at, final double reach, final int argb,
                    final double seconds) {
        final List<Face> faces = new ArrayList<>();
        final BlockPos centre = BlockPos.containing(at);
        final int r = (int) Math.ceil(reach);
        for (final BlockPos pos : BlockPos.betweenClosed(centre.offset(-r, -r, -r), centre.offset(r, r, r))) {
            if (faces.size() >= MOST_FACES || !level.getBlockState(pos).isCollisionShapeFullBlock(level, pos)) {
                continue;
            }
            for (final Direction side : Direction.values()) {
                final BlockPos open = pos.relative(side);
                if (!level.getBlockState(open).isCollisionShapeFullBlock(level, open)
                        && Vec3.atCenterOf(pos).relative(side, 0.5).distanceTo(at) < reach + 0.5) {
                    faces.add(new Face(pos.immutable(), side));
                }
            }
        }
        if (faces.isEmpty()) {
            return;
        }
        if (MARKS.size() >= MOST_MARKS) {
            MARKS.removeFirst();
        }
        MARKS.add(new Mark(level, at, reach, argb, level.getGameTime(), Math.max(1, seconds * TICKS_A_SECOND),
                List.copyOf(faces)));
    }

    /** Takes every mark away, as the world closes. */
    static void clear() {
        MARKS.clear();
    }

    /** Draws the marks of {@code level}, the camera at {@code eye}. */
    static void render(final ClientLevel level, final Vec3 eye, final MultiBufferSource.BufferSource buffers) {
        if (MARKS.isEmpty()) {
            return;
        }
        final VertexConsumer consumer = buffers.getBuffer(EffectRenderTypes.MARKS);
        final Iterator<Mark> marks = MARKS.iterator();
        while (marks.hasNext()) {
            final Mark mark = marks.next();
            final double left = 1 - (level.getGameTime() - mark.born()) / mark.ticks();
            if (mark.level() != level || left <= 0) {
                marks.remove();
                continue;
            }
            for (final Face face : mark.faces()) {
                draw(consumer, eye, mark, face, left);
            }
        }
        buffers.endBatch(EffectRenderTypes.MARKS);
    }

    /* One face of a mark, cut finely, each corner as dark as it is near the mark's middle. */
    private static void draw(final VertexConsumer consumer, final Vec3 eye, final Mark mark, final Face face,
                             final double left) {
        final Direction side = face.side();
        final Vec3 middle = Vec3.atCenterOf(face.pos()).relative(side, 0.5 + OFF);
        final Direction across = side.getAxis() == Direction.Axis.Y ? Direction.EAST
                : side.getClockWise();
        final Direction up = side.getAxis() == Direction.Axis.Y ? Direction.SOUTH : Direction.UP;
        final Vec3 u = Vec3.atLowerCornerOf(across.getNormal());
        final Vec3 v = Vec3.atLowerCornerOf(up.getNormal());
        for (int i = 0; i < CUTS; i++) {
            for (int j = 0; j < CUTS; j++) {
                final double u0 = (double) i / CUTS - 0.5;
                final double u1 = (double) (i + 1) / CUTS - 0.5;
                final double v0 = (double) j / CUTS - 0.5;
                final double v1 = (double) (j + 1) / CUTS - 0.5;
                corner(consumer, eye, mark, middle, u, v, u0, v0, left);
                corner(consumer, eye, mark, middle, u, v, u0, v1, left);
                corner(consumer, eye, mark, middle, u, v, u1, v1, left);
                corner(consumer, eye, mark, middle, u, v, u1, v0, left);
            }
        }
    }

    private static void corner(final VertexConsumer consumer, final Vec3 eye, final Mark mark, final Vec3 middle,
                               final Vec3 u, final Vec3 v, final double du, final double dv, final double left) {
        final Vec3 at = middle.add(u.scale(du)).add(v.scale(dv));
        final double near = Math.max(0, 1 - at.distanceTo(mark.at()) / mark.reach());
        final int argb = mark.argb();
        final int alpha = (int) Math.round((argb >>> ALPHA_SHIFT) * near * left);
        consumer.addVertex((float) (at.x - eye.x), (float) (at.y - eye.y), (float) (at.z - eye.z))
                .setColor(argb >> 16 & CHANNEL, argb >> 8 & CHANNEL, argb & CHANNEL, alpha);
    }

    /* A mark: where, how far, its colour, when it was laid and how many ticks it fades over, and its faces. */
    private record Mark(ClientLevel level, Vec3 at, double reach, int argb, long born, double ticks,
                        List<Face> faces) {

        Mark {
            faces = List.copyOf(faces);
        }
    }

    private record Face(BlockPos pos, Direction side) {
    }
}
