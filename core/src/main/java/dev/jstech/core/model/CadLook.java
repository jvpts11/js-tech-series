/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

import dev.jstech.core.font.PngWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * What a model from a CAD program needs to look as the game does, as its files carry no pictures, only colours: a
 * texture of sixteen pixels a side for each colour, the colour with the light and dark pixels a block of the game has,
 * laid on the faces from the sides as a block's are, a pixel a sixteenth of a block; and the turn from the upright Z of
 * the CAD programs to the upright Y of the game. Pure.
 */
public final class CadLook {

    /** The colour of a part whose file gives it none: a bright steel. */
    public static final int STEEL = 0xFFB4B9C0;
    /** How many pixels a side its textures are, as a block's. */
    public static final int SIZE = 16;
    /** The game's pixels in a metre, a block. */
    public static final float PIXELS_A_METRE = 16.0F;
    /* The alpha of an opaque colour, which a colour read from a file is given. */
    static final int OPAQUE = 0xFF000000;
    /* What a colour's texture is called before its colour. */
    private static final String COLOUR = "colour_";
    /* How much lighter or darker a pixel is, at most, and the seed that mixes a pixel's place into it. */
    private static final double SHADE = 0.09;
    private static final long MIX = 0x9E3779B97F4A7C15L;
    private static final int CHANNEL = 0xFF;
    private static final int SHADES = 5;

    private CadLook() {
    }

    /** A texture of the colour {@code argb}, as a block of that colour looks, called by its colour. */
    public static ModelTexture colour(final int argb) {
        final String name = String.format(Locale.ROOT, COLOUR + "%08x", argb);
        final byte[] png = PngWriter.rgba(SIZE, SIZE, (x, y) -> shaded(argb, x, y));
        return new ModelTexture(name, "", png, SIZE, SIZE);
    }

    /** Whether {@code texture} is one made for a colour, as {@link #colour} makes it. */
    public static boolean isColour(final ModelTexture texture) {
        return texture.name().startsWith(COLOUR) && texture.file().isEmpty();
    }

    /** The turn from a CAD program's space, Z up, to the game's, Y up: X stays, Y comes from Z, Z from -Y. */
    public static Matrix4f upright() {
        return new Matrix4f().rotateX((float) (-Math.PI / 2));
    }

    /** {@code point} turned upright, in place. */
    public static Vector3f upright(final Vector3f point) {
        final float y = point.y;
        point.y = point.z;
        point.z = -y;
        return point;
    }

    /**
     * {@code data} moved to stand on its origin, as a block's model stands on the middle of its floor: the middle of
     * its box across and along, and its lowest point, at 0. A CAD program puts a part wherever its drawing had it.
     */
    public static ModelData standing(final ModelData data) {
        final ModelPose pose = new ModelPose(data);
        final Matrix4f[] world = new Matrix4f[data.bones().size()];
        for (int i = 0; i < world.length; i++) {
            world[i] = new Matrix4f();
        }
        pose.world(world);
        final Vector3f low = new Vector3f(Float.MAX_VALUE);
        final Vector3f high = new Vector3f(-Float.MAX_VALUE);
        final Vector3f corner = new Vector3f();
        for (int bone = 0; bone < world.length; bone++) {
            for (final ModelMesh mesh : data.bones().get(bone).meshes()) {
                final float[] positions = mesh.positions();
                for (int at = 0; at + 2 < positions.length; at += 3) {
                    world[bone].transformPosition(corner.set(positions[at], positions[at + 1], positions[at + 2]));
                    low.min(corner);
                    high.max(corner);
                }
            }
        }
        if (low.x > high.x) {
            return data;
        }
        final Vector3f move = new Vector3f(-(low.x + high.x) / 2, -low.y, -(low.z + high.z) / 2);
        final List<ModelBone> bones = new ArrayList<>(data.bones().size());
        for (final ModelBone bone : data.bones()) {
            bones.add(bone.parent() != null ? bone : new ModelBone(bone.name(), null, bone.pivot(), bone.rotation(),
                    bone.euler(), new Vector3f(bone.translation()).add(move), bone.scale(), bone.cubes(),
                    bone.meshes(), bone.locators(), bone.hidden()));
        }
        return new ModelData(data.name(), data.textureWidth(), data.textureHeight(), bones, data.skins(),
                data.textures(), data.animations());
    }

    /*
     * A pixel of a colour's texture: lighter or darker by one of a few steps, as the pixels of the game's blocks
     * are, the same for a colour every time.
     */
    private static int shaded(final int argb, final int x, final int y) {
        long hash = (argb * MIX) ^ (x * 31L + y) * MIX;
        hash ^= hash >>> 29;
        hash *= MIX;
        hash ^= hash >>> 32;
        final int step = (int) Math.floorMod(hash, SHADES) - SHADES / 2;
        final double times = 1 + SHADE * step / (SHADES / 2);
        final int red = channel(argb >> 16, times);
        final int green = channel(argb >> 8, times);
        final int blue = channel(argb, times);
        return argb & OPAQUE | red << 16 | green << 8 | blue;
    }

    private static int channel(final int value, final double times) {
        return (int) Math.clamp(Math.round((value & CHANNEL) * times), 0, CHANNEL);
    }
}
