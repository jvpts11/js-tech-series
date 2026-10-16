/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.connect;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

/**
 * A face of a block model as its texture lies on it: where across and down its texture each corner is, from 0 to 1,
 * and which ways of the world the texture's right and down run, so the neighbours a texture joins are counted the way
 * the texture is drawn, whatever the model turned. A face is drawn again from another texture, or from a cell of one,
 * whole or a quarter at a time, its corners where they were. Faces whose texture does not lie flat on them are none.
 */
final class QuadPieces {

    private final BakedQuad quad;
    private final float[] across = new float[CORNERS];
    private final float[] down = new float[CORNERS];
    private final Direction right;
    private final Direction under;

    /* The block format's ints per corner, and where in them a corner keeps what. */
    private static final int CORNERS = 4;
    private static final int STRIDE = 8;
    private static final int X = 0;
    private static final int Y = 1;
    private static final int Z = 2;
    private static final int U = 4;
    private static final int V = 5;
    private static final int LIGHT = 6;
    private static final float HALF = 0.5F;
    private static final float TINY = 1.0e-4F;

    private QuadPieces(final BakedQuad quad, final Direction right, final Direction under) {
        this.quad = quad;
        this.right = right;
        this.under = under;
        final TextureAtlasSprite sprite = quad.getSprite();
        final int[] data = quad.getVertices();
        for (int i = 0; i < CORNERS; i++) {
            this.across[i] = unit(Float.intBitsToFloat(data[i * STRIDE + U]), sprite.getU0(), sprite.getU1());
            this.down[i] = unit(Float.intBitsToFloat(data[i * STRIDE + V]), sprite.getV0(), sprite.getV1());
        }
    }

    /** The face {@code quad} as its texture lies on it, or none where the texture does not lie flat on it. */
    static @Nullable QuadPieces of(final BakedQuad quad) {
        final int[] data = quad.getVertices();
        if (data.length < CORNERS * STRIDE) {
            return null;
        }
        final TextureAtlasSprite sprite = quad.getSprite();
        final float[][] at = new float[CORNERS][];
        final float[] s = new float[CORNERS];
        final float[] t = new float[CORNERS];
        for (int i = 0; i < CORNERS; i++) {
            at[i] = new float[] {Float.intBitsToFloat(data[i * STRIDE + X]), Float.intBitsToFloat(data[i * STRIDE
                    + Y]), Float.intBitsToFloat(data[i * STRIDE + Z])};
            s[i] = unit(Float.intBitsToFloat(data[i * STRIDE + U]), sprite.getU0(), sprite.getU1());
            t[i] = unit(Float.intBitsToFloat(data[i * STRIDE + V]), sprite.getV0(), sprite.getV1());
        }
        // Where the texture's across and down run in the world: the edges from the first corner, solved for both.
        final float ds1 = s[1] - s[0];
        final float dt1 = t[1] - t[0];
        final float ds2 = s[3] - s[0];
        final float dt2 = t[3] - t[0];
        final float det = ds1 * dt2 - ds2 * dt1;
        if (Math.abs(det) < TINY) {
            return null;
        }
        final float[] alongS = new float[3];
        final float[] alongT = new float[3];
        for (int axis = 0; axis < 3; axis++) {
            final float e1 = at[1][axis] - at[0][axis];
            final float e2 = at[3][axis] - at[0][axis];
            alongS[axis] = (e1 * dt2 - e2 * dt1) / det;
            alongT[axis] = (e2 * ds1 - e1 * ds2) / det;
        }
        final Direction right = nearest(alongS);
        final Direction under = nearest(alongT);
        if (right == null || under == null || right.getAxis() == under.getAxis()) {
            return null;
        }
        return new QuadPieces(quad, right, under);
    }

    /** The face it is. */
    BakedQuad quad() {
        return this.quad;
    }

    /** Which way of the world its texture's right runs. */
    Direction right() {
        return this.right;
    }

    /** Which way its texture's up runs. */
    Direction up() {
        return this.under.getOpposite();
    }

    /** The face drawn whole from {@code cell} of {@code sprite}, turned {@code turns} quarters clockwise. */
    BakedQuad whole(final TextureAtlasSprite sprite, final Cell cell, final int turns, final int light) {
        final BakedQuad piece = piece(0, 0, 1, 1, sprite, cell, turns, light, true, 0, 0);
        return piece == null ? this.quad : piece;
    }

    /**
     * The face's quarter {@code quarter} (up-left, up-right, down-right, down-left of its texture) drawn from
     * {@code cell} of {@code sprite}; none where the face covers none of that quarter.
     */
    @Nullable BakedQuad quarter(final int quarter, final TextureAtlasSprite sprite, final Cell cell, final int light) {
        final float s0 = quarter == 1 || quarter == 2 ? HALF : 0;
        final float t0 = quarter >= 2 ? HALF : 0;
        return piece(s0, t0, s0 + HALF, t0 + HALF, sprite, cell, 0, light, false, 0, 0);
    }

    /**
     * The part of the face within the share of its texture from {@code s0}, {@code t0} to {@code s1}, {@code t1}
     * drawn from {@code cell} of {@code sprite}; none where the face covers none of it.
     */
    @Nullable BakedQuad region(final float s0, final float t0, final float s1, final float t1,
                               final TextureAtlasSprite sprite, final Cell cell, final int light) {
        return piece(s0, t0, s1, t1, sprite, cell, 0, light, false, 0, 0);
    }

    /**
     * The face's quarter {@code quarter} drawn from the same quarter of {@code sprite}, the point of the texture at
     * the face's middle moved by {@code du} and {@code dv}, shares of the texture, so the four quarters warp it.
     */
    @Nullable BakedQuad warpedQuarter(final int quarter, final TextureAtlasSprite sprite, final float du,
                                      final float dv, final int light) {
        final float s0 = quarter == 1 || quarter == 2 ? HALF : 0;
        final float t0 = quarter >= 2 ? HALF : 0;
        return piece(s0, t0, s0 + HALF, t0 + HALF, sprite, new Cell(s0, t0, HALF, HALF), 0, light, false, du, dv);
    }

    /*
     * The part of the face within its texture's square from s0, t0 to s1, t1, drawn from the cell; the point of the
     * texture at the face's middle moved by du, dv.
     */
    private @Nullable BakedQuad piece(final float s0, final float t0, final float s1, final float t1,
                                      final TextureAtlasSprite sprite, final Cell cell, final int turns,
                                      final int light, final boolean whole, final float du, final float dv) {
        float lowS = 1;
        float highS = 0;
        float lowT = 1;
        float highT = 0;
        for (int i = 0; i < CORNERS; i++) {
            lowS = Math.min(lowS, this.across[i]);
            highS = Math.max(highS, this.across[i]);
            lowT = Math.min(lowT, this.down[i]);
            highT = Math.max(highT, this.down[i]);
        }
        final float fromS = Math.max(lowS, s0);
        final float toS = Math.min(highS, s1);
        final float fromT = Math.max(lowT, t0);
        final float toT = Math.min(highT, t1);
        if (toS - fromS < TINY || toT - fromT < TINY) {
            return null;
        }
        final int[] data = this.quad.getVertices().clone();
        final int[] source = this.quad.getVertices();
        for (int i = 0; i < CORNERS; i++) {
            // Each corner keeps its side of the face: moved in to the piece's edge where the piece is smaller.
            final float s = this.across[i] <= lowS + TINY ? fromS : toS;
            final float t = this.down[i] <= lowT + TINY ? fromT : toT;
            final float[] place = place(source, s, t, lowS, highS, lowT, highT);
            data[i * STRIDE + X] = Float.floatToRawIntBits(place[0]);
            data[i * STRIDE + Y] = Float.floatToRawIntBits(place[1]);
            data[i * STRIDE + Z] = Float.floatToRawIntBits(place[2]);
            // Within its cell: the whole face over the whole cell, a quarter over a quarter's cell.
            float cs = whole ? s : (s - s0) / (s1 - s0);
            float ct = whole ? t : (t - t0) / (t1 - t0);
            for (int turn = 0; turn < Math.floorMod(turns, CORNERS); turn++) {
                final float was = cs;
                cs = 1 - ct;
                ct = was;
            }
            final boolean middle = Math.abs(s - HALF) < TINY && Math.abs(t - HALF) < TINY;
            data[i * STRIDE + U] = Float.floatToRawIntBits(sprite.getU0() + (sprite.getU1() - sprite.getU0())
                    * (cell.u0() + cs * cell.width() + (middle ? du : 0)));
            data[i * STRIDE + V] = Float.floatToRawIntBits(sprite.getV0() + (sprite.getV1() - sprite.getV0())
                    * (cell.v0() + ct * cell.height() + (middle ? dv : 0)));
            if (light >= 0) {
                final int had = data[i * STRIDE + LIGHT];
                data[i * STRIDE + LIGHT] = LightTexture.pack(Math.max(LightTexture.block(had),
                        LightTexture.block(light)), Math.max(LightTexture.sky(had), LightTexture.sky(light)));
            }
        }
        return new BakedQuad(data, this.quad.getTintIndex(), this.quad.getDirection(), sprite, this.quad.isShade(),
                this.quad.hasAmbientOcclusion());
    }

    /*
     * Where on the face the point of its texture s, t lies: the face's corners are blended across its texture's
     * square, which the corners lie at the edges of.
     */
    private float[] place(final int[] data, final float s, final float t, final float lowS, final float highS,
                          final float lowT, final float highT) {
        final float a = highS - lowS < TINY ? 0 : (s - lowS) / (highS - lowS);
        final float b = highT - lowT < TINY ? 0 : (t - lowT) / (highT - lowT);
        final float[] out = new float[3];
        float weightSum = 0;
        for (int i = 0; i < CORNERS; i++) {
            final float ca = this.across[i] <= lowS + TINY ? 1 - a : a;
            final float cb = this.down[i] <= lowT + TINY ? 1 - b : b;
            final float weight = ca * cb;
            weightSum += weight;
            out[0] += weight * Float.intBitsToFloat(data[i * STRIDE + X]);
            out[1] += weight * Float.intBitsToFloat(data[i * STRIDE + Y]);
            out[2] += weight * Float.intBitsToFloat(data[i * STRIDE + Z]);
        }
        if (weightSum > 0) {
            out[0] /= weightSum;
            out[1] /= weightSum;
            out[2] /= weightSum;
        }
        return out;
    }

    private static float unit(final float value, final float from, final float to) {
        return to == from ? 0 : (value - from) / (to - from);
    }

    /* The way of the world nearest a direction, or none for a direction of no length. */
    private static @Nullable Direction nearest(final float[] along) {
        final float length = (float) Math.sqrt(along[0] * along[0] + along[1] * along[1] + along[2] * along[2]);
        if (length < TINY) {
            return null;
        }
        return Direction.getNearest(along[0] / length, along[1] / length, along[2] / length);
    }

    /**
     * A cell of a texture, a share of it each way.
     *
     * @param u0     where it starts across, from 0 to 1
     * @param v0     where it starts down
     * @param width  how wide it is
     * @param height how tall
     */
    record Cell(float u0, float v0, float width, float height) {

        /** The whole texture. */
        static final Cell WHOLE = new Cell(0, 0, 1, 1);
    }

    /** The least light a face glows with, {@code block} and {@code sky} from 0 to 15, as the format packs it. */
    static int light(final int block, final int sky) {
        return LightTexture.pack(block, sky);
    }

    /** The quads of {@code pieces} that are some. */
    static List<BakedQuad> some(final BakedQuad... pieces) {
        final List<BakedQuad> out = new ArrayList<>(pieces.length);
        for (final BakedQuad piece : pieces) {
            if (piece != null) {
                out.add(piece);
            }
        }
        return out;
    }
}
