/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.skin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * Draws the shapes a formed structure of the approved pictures is made of, as they are drawn there: boxes whose faces
 * carry skins laid over them, each once, or as the face of a larger box they are a piece of, so the pieces of one
 * thing share one picture; square steel members from one point to another; and round rods with their ends closed.
 * Everything else is one plain colour. The points are in the drawing's own space, in blocks.
 */
public final class SkinDrawing {

    private final VertexConsumer to;
    private final PoseStack.Pose pose;
    private final SkinAtlas atlas;
    private final ToIntFunction<Vector3f> light;

    /* How far off a face, in blocks, the light it is lit by is read. */
    private static final float LIGHT_OUT = 0.3F;
    private static final float HALF = 0.5F;
    private static final float UPRIGHT = 0.9F;
    private static final int OPAQUE = 0xFF;
    private static final float QUARTER = 90.0F;

    /** Draws into {@code to} at {@code pose} with the skins of {@code atlas}, lit as {@code light} says at a point. */
    public SkinDrawing(final VertexConsumer to, final PoseStack.Pose pose, final SkinAtlas atlas,
                       final ToIntFunction<Vector3f> light) {
        this.to = to;
        this.pose = pose;
        this.atlas = atlas;
        this.light = light;
    }

    /**
     * A drawing in a structure's own space, pushed onto {@code pose}, which stands on the block {@code at}: its origin
     * the middle of the bottom of the block {@code from}, along it toward {@code along}, up, and across it to its
     * right, lit by {@code level}'s light where each face looks out; the caller pops the pose once done.
     */
    public static SkinDrawing in(final PoseStack pose, final MultiBufferSource buffers, final SkinAtlas atlas,
                                 final Level level, final BlockPos at, final BlockPos from, final Direction along) {
        pose.pushPose();
        pose.translate(from.getX() - at.getX() + HALF, from.getY() - at.getY(), from.getZ() - at.getZ() + HALF);
        pose.mulPose(Axis.YP.rotationDegrees(-(along.toYRot() + QUARTER)));
        final Direction across = along.getClockWise();
        return new SkinDrawing(buffers.getBuffer(RenderType.entitySolid(atlas.texture())), pose.last(), atlas,
                point -> LevelRenderer.getLightColor(level, BlockPos.containing(
                        from.getX() + HALF + point.x() * along.getStepX() + point.z() * across.getStepX(),
                        from.getY() + point.y(),
                        from.getZ() + HALF + point.x() * along.getStepZ() + point.z() * across.getStepZ())));
    }

    /**
     * The box from {@code min} to {@code max}, each face painted as {@code paints} says, or not drawn where it says
     * nothing.
     */
    public void box(final float[] min, final float[] max, final Function<Face, @Nullable Paint> paints) {
        for (final Face face : Face.values()) {
            final Paint paint = paints.apply(face);
            if (paint == null) {
                continue;
            }
            final float[] origin = face.origin(min, max);
            final float wide = face.wide(min, max);
            final float high = face.high(min, max);
            float s0 = 0;
            float t0 = 0;
            float s1 = 0;
            float t1 = 0;
            if (paint.skin() != null) {
                final SkinAtlas.Region region = this.atlas.region(paint.skin());
                if (paint.frame() != null) {
                    // Laid as the frame's face: where this face starts on it, sixteen texels a block.
                    final float[] from = face.origin(paint.frame()[0], paint.frame()[1]);
                    s0 = dot(origin, from, face.across) * PaintedSkin.TEXELS;
                    t0 = dot(origin, from, face.down) * PaintedSkin.TEXELS;
                    s1 = s0 + wide * PaintedSkin.TEXELS;
                    t1 = t0 + high * PaintedSkin.TEXELS;
                } else {
                    s1 = region.width();
                    t1 = region.height();
                }
                quad(origin, face.across, face.down, wide, high, face.normal, region, s0, t0, s1, t1, paint.colour());
            } else {
                final SkinAtlas.Region white = this.atlas.region(SkinAtlas.WHITE);
                quad(origin, face.across, face.down, wide, high, face.normal, white, (float) white.middleX(),
                        (float) white.middleY(), (float) white.middleX(), (float) white.middleY(), paint.colour());
            }
        }
    }

    /**
     * A square member from {@code from} to {@code to}, {@code wide} across and {@code high} the other way, in
     * {@code colour}: lying along it, its width level and its height across both.
     */
    public void member(final Vector3f from, final Vector3f to, final float wide, final float high, final int colour) {
        final Vector3f along = new Vector3f(to).sub(from);
        final float length = along.length();
        along.normalize();
        final Vector3f reference = Math.abs(along.y()) < UPRIGHT ? new Vector3f(0, 1, 0) : new Vector3f(1, 0, 0);
        final Vector3f side = new Vector3f(along).cross(reference).normalize();
        final Vector3f normal = new Vector3f(side).cross(along);
        final Vector3f middle = new Vector3f(from).add(to).mul(HALF);
        final Vector3f[] axes = {side, along, normal};
        final float[] halves = {wide * HALF, length * HALF, high * HALF};
        for (int k = 0; k < 3; k++) {
            final Vector3f out = axes[k];
            final Vector3f first = axes[(k + 1) % 3];
            final Vector3f second = axes[(k + 2) % 3];
            final float firstHalf = halves[(k + 1) % 3];
            final float secondHalf = halves[(k + 2) % 3];
            for (final int sign : new int[] {1, -1}) {
                final Vector3f n = new Vector3f(out).mul(sign);
                final Vector3f centre = new Vector3f(middle).add(new Vector3f(out).mul(halves[k] * sign));
                // Corners turning about the outward normal, so the face looks out.
                final Vector3f e1 = new Vector3f(first).mul(firstHalf * sign);
                final Vector3f e2 = new Vector3f(second).mul(secondHalf);
                plain(new Vector3f(centre).sub(e1).sub(e2), new Vector3f(centre).add(e1).sub(e2),
                        new Vector3f(centre).add(e1).add(e2), new Vector3f(centre).sub(e1).add(e2), n, colour);
            }
        }
    }

    /** A round rod from {@code from} to {@code to}, {@code radius} thick, of {@code facets} faces, its ends closed. */
    public void rod(final Vector3f from, final Vector3f to, final float radius, final int facets, final int colour) {
        capped(from, to, radius, facets, colour, colour);
    }

    /** A round rod as {@link #rod}, its ends closed in {@code caps}. */
    public void capped(final Vector3f from, final Vector3f to, final float radius, final int facets, final int colour,
                       final int caps) {
        final Vector3f along = new Vector3f(to).sub(from).normalize();
        final Vector3f reference = Math.abs(along.y()) < UPRIGHT ? new Vector3f(0, 1, 0) : new Vector3f(1, 0, 0);
        final Vector3f e1 = new Vector3f(along).cross(reference).normalize();
        final Vector3f e2 = new Vector3f(along).cross(e1);
        final Vector3f[] round = new Vector3f[facets + 1];
        for (int i = 0; i <= facets; i++) {
            final double turn = 2 * Math.PI * i / facets;
            round[i] = new Vector3f(e1).mul((float) Math.cos(turn) * radius)
                    .add(new Vector3f(e2).mul((float) Math.sin(turn) * radius));
        }
        final Vector3f back = new Vector3f(along).negate();
        for (int i = 0; i < facets; i++) {
            final Vector3f out = new Vector3f(round[i]).add(round[i + 1]).normalize();
            plain(new Vector3f(from).add(round[i]), new Vector3f(from).add(round[i + 1]),
                    new Vector3f(to).add(round[i + 1]), new Vector3f(to).add(round[i]), out, colour);
            plain(new Vector3f(to), new Vector3f(to).add(round[i]), new Vector3f(to).add(round[i + 1]),
                    new Vector3f(to).add(round[i + 1]), along, caps);
            plain(new Vector3f(from), new Vector3f(from).add(round[i + 1]), new Vector3f(from).add(round[i]),
                    new Vector3f(from).add(round[i]), back, caps);
        }
    }

    /**
     * A band round the upright line through {@code centre}, from {@code y0} over it at a radius of {@code r0} up to
     * {@code y1} at {@code r1}, of {@code facets} faces, its skin {@code skin} laid once round it: a ring of a lathed
     * shape, as the approved pictures turn their spheres and domes.
     */
    public void band(final Vector3f centre, final float y0, final float y1, final float r0, final float r1,
                     final int facets, final String skin) {
        final SkinAtlas.Region region = this.atlas.region(skin);
        final float slope = (r0 - r1) / Math.max(1e-4F, y1 - y0);
        for (int i = 0; i < facets; i++) {
            final double t0 = 2 * Math.PI * i / facets;
            final double t1 = 2 * Math.PI * (i + 1) / facets;
            final Vector3f a = round(centre, t0, r0, y0);
            final Vector3f b = round(centre, t1, r0, y0);
            final Vector3f c = round(centre, t1, r1, y1);
            final Vector3f d = round(centre, t0, r1, y1);
            final double mid = (t0 + t1) / 2;
            final Vector3f out = new Vector3f((float) -Math.sin(mid), slope, (float) -Math.cos(mid)).normalize();
            final float s0 = (float) i / facets * region.width();
            final float s1 = (float) (i + 1) / facets * region.width();
            final int lit = this.light.applyAsInt(new Vector3f(a).add(c).mul(HALF).add(new Vector3f(out)
                    .mul(LIGHT_OUT)));
            final int white = OPAQUE << 24 | 0xFFFFFF;
            vertex(a, this.atlas.u(region, s0), this.atlas.v(region, region.height()), out, white, lit);
            vertex(b, this.atlas.u(region, s1), this.atlas.v(region, region.height()), out, white, lit);
            vertex(c, this.atlas.u(region, s1), this.atlas.v(region, 0), out, white, lit);
            vertex(d, this.atlas.u(region, s0), this.atlas.v(region, 0), out, white, lit);
        }
    }

    /* The point {@code radius} out from the upright line through {@code centre} at the turn {@code turn}, so high. */
    private static Vector3f round(final Vector3f centre, final double turn, final float radius, final float high) {
        return new Vector3f(centre.x() - (float) Math.sin(turn) * radius, centre.y() + high,
                centre.z() - (float) Math.cos(turn) * radius);
    }

    /* The distance from {@code from} to {@code at} along {@code way}. */
    private static float dot(final float[] at, final float[] from, final Vector3f way) {
        return (at[0] - from[0]) * way.x() + (at[1] - from[1]) * way.y() + (at[2] - from[2]) * way.z();
    }

    /* A face from {@code origin}, down it first and then across it, its skin's texels from s0, t0 to s1, t1. */
    private void quad(final float[] origin, final Vector3f across, final Vector3f down, final float wide,
                      final float high, final Vector3f normal, final SkinAtlas.Region region, final float s0,
                      final float t0, final float s1, final float t1, final int colour) {
        final Vector3f a = new Vector3f(origin[0], origin[1], origin[2]);
        final Vector3f b = new Vector3f(down).mul(high).add(a);
        final Vector3f c = new Vector3f(across).mul(wide).add(b);
        final Vector3f d = new Vector3f(across).mul(wide).add(a);
        final int lit = this.light.applyAsInt(new Vector3f(a).add(c).mul(HALF).add(new Vector3f(normal)
                .mul(LIGHT_OUT)));
        vertex(a, this.atlas.u(region, s0), this.atlas.v(region, t0), normal, colour, lit);
        vertex(b, this.atlas.u(region, s0), this.atlas.v(region, t1), normal, colour, lit);
        vertex(c, this.atlas.u(region, s1), this.atlas.v(region, t1), normal, colour, lit);
        vertex(d, this.atlas.u(region, s1), this.atlas.v(region, t0), normal, colour, lit);
    }

    /* A face of one colour through its four corners, turning about {@code normal}. */
    private void plain(final Vector3f a, final Vector3f b, final Vector3f c, final Vector3f d, final Vector3f normal,
                       final int colour) {
        final SkinAtlas.Region white = this.atlas.region(SkinAtlas.WHITE);
        final float u = this.atlas.u(white, white.middleX());
        final float v = this.atlas.v(white, white.middleY());
        final int lit = this.light.applyAsInt(new Vector3f(a).add(c).mul(HALF).add(new Vector3f(normal)
                .mul(LIGHT_OUT)));
        vertex(a, u, v, normal, colour, lit);
        vertex(b, u, v, normal, colour, lit);
        vertex(c, u, v, normal, colour, lit);
        vertex(d, u, v, normal, colour, lit);
    }

    private void vertex(final Vector3f at, final float u, final float v, final Vector3f normal, final int colour,
                        final int lit) {
        this.to.addVertex(this.pose, at.x(), at.y(), at.z()).setColor(colour >> 16 & OPAQUE, colour >> 8 & OPAQUE,
                        colour & OPAQUE, OPAQUE).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lit)
                .setNormal(this.pose, normal.x(), normal.y(), normal.z());
    }

    /**
     * How a face is painted: with a skin of the atlas, laid once over it or, with a frame, the box from the frame's
     * first corner to its second, as that box's face; or in one colour.
     */
    public record Paint(@Nullable String skin, float @Nullable [][] frame, int colour) {

        /** The skin {@code skin}, laid once over the face. */
        public static Paint skin(final String skin) {
            return new Paint(skin, null, OPAQUE << 24 | 0xFFFFFF);
        }

        /** The skin {@code skin}, laid as the face of the box from {@code min} to {@code max}. */
        public static Paint framed(final String skin, final float[] min, final float[] max) {
            return new Paint(skin, new float[][] {min, max}, OPAQUE << 24 | 0xFFFFFF);
        }

        /** One plain {@code colour}. */
        public static Paint tile(final int colour) {
            return new Paint(null, null, colour);
        }
    }

    /**
     * A box's faces, each with the way out of it, the corner its skin starts at, and the ways across and down it its
     * skin runs, as the approved pictures lay them.
     */
    public enum Face {
        PLUS_X(new Vector3f(1, 0, 0), new Vector3f(0, 0, -1), new Vector3f(0, -1, 0)),
        MINUS_X(new Vector3f(-1, 0, 0), new Vector3f(0, 0, 1), new Vector3f(0, -1, 0)),
        PLUS_Z(new Vector3f(0, 0, 1), new Vector3f(1, 0, 0), new Vector3f(0, -1, 0)),
        MINUS_Z(new Vector3f(0, 0, -1), new Vector3f(-1, 0, 0), new Vector3f(0, -1, 0)),
        PLUS_Y(new Vector3f(0, 1, 0), new Vector3f(1, 0, 0), new Vector3f(0, 0, 1)),
        MINUS_Y(new Vector3f(0, -1, 0), new Vector3f(1, 0, 0), new Vector3f(0, 0, -1));

        private final Vector3f normal;
        private final Vector3f across;
        private final Vector3f down;

        Face(final Vector3f normal, final Vector3f across, final Vector3f down) {
            this.normal = normal;
            this.across = across;
            this.down = down;
        }

        /* The corner of the box from {@code min} to {@code max} the face's skin starts at. */
        float[] origin(final float[] min, final float[] max) {
            final float[] at = new float[3];
            for (int k = 0; k < 3; k++) {
                final float n = component(this.normal, k);
                final float a = component(this.across, k);
                final float d = component(this.down, k);
                // On the face's own side; where the skin runs toward the low side, from the high one.
                at[k] = n > 0 || a < 0 || d < 0 ? max[k] : min[k];
                if (n < 0) {
                    at[k] = min[k];
                }
            }
            return at;
        }

        /* How far the face's skin runs across it. */
        float wide(final float[] min, final float[] max) {
            return span(this.across, min, max);
        }

        /* How far the face's skin runs down it. */
        float high(final float[] min, final float[] max) {
            return span(this.down, min, max);
        }

        private static float span(final Vector3f way, final float[] min, final float[] max) {
            for (int k = 0; k < 3; k++) {
                if (component(way, k) != 0) {
                    return max[k] - min[k];
                }
            }
            return 0;
        }

        private static float component(final Vector3f way, final int k) {
            return k == 0 ? way.x() : k == 1 ? way.y() : way.z();
        }
    }
}
