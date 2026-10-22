/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.model.kit;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.BufferUploader;
import dev.jstech.core.model.BakedGeometry;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.system.MemoryUtil;

/**
 * A model's faces on the graphics card, to draw it many at once: one group of triangles for each of its pictures,
 * every corner with its place in its bone, its picture's place, its normal, its bone and whether it glows. Made from
 * the model's baked shape the first time it is drawn so, and freed when the models are read again. The middle of each
 * glowing part, for the light it casts, is worked out apart, with nothing sent to the card.
 */
final class KitMesh implements AutoCloseable {

    private final List<Group> groups;

    /* Floats a corner: place, picture, normal, bone and glow. */
    private static final int FLOATS = 10;
    private static final int STRIDE = FLOATS * Float.BYTES;
    /* The two triangles of a face of four corners. */
    private static final int[] TRIANGLES = {0, 1, 2, 0, 2, 3};

    private KitMesh(final List<Group> groups) {
        this.groups = groups;
    }

    /** Sends {@code model}'s faces to the graphics card. On the render thread. */
    static KitMesh of(final KitModel model) {
        final Map<Integer, List<float[]>> byPicture = new LinkedHashMap<>();
        final List<List<BakedGeometry.Part>> bones = model.geometry().bones();
        for (int bone = 0; bone < bones.size(); bone++) {
            for (final BakedGeometry.Part part : bones.get(bone)) {
                byPicture.computeIfAbsent(part.texture(), key -> new ArrayList<>()).add(corners(part, bone));
            }
        }
        final List<Group> groups = new ArrayList<>();
        byPicture.forEach((picture, parts) -> groups.add(Group.of(picture, parts)));
        return new KitMesh(List.copyOf(groups));
    }

    /** The middle of each of {@code model}'s glowing parts, in its bone. */
    static List<Glow> glowsOf(final KitModel model) {
        final List<Glow> glows = new ArrayList<>();
        final List<List<BakedGeometry.Part>> bones = model.geometry().bones();
        for (int bone = 0; bone < bones.size(); bone++) {
            for (final BakedGeometry.Part part : bones.get(bone)) {
                if (part.glow()) {
                    glows.add(Glow.of(bone, part.vertices()));
                }
            }
        }
        return List.copyOf(glows);
    }

    /** Its groups, one for each picture. */
    List<Group> groups() {
        return this.groups;
    }

    @Override
    public void close() {
        for (final Group group : this.groups) {
            GlStateManager._glDeleteVertexArrays(group.vertexArray());
            GlStateManager._glDeleteBuffers(group.buffer());
        }
    }

    /* A part's faces as triangles, each corner with its bone and whether it glows. */
    private static float[] corners(final BakedGeometry.Part part, final int bone) {
        final float[] vertices = part.vertices();
        final int faces = vertices.length / (BakedGeometry.STRIDE * 4);
        final float[] out = new float[faces * TRIANGLES.length * FLOATS];
        int at = 0;
        for (int face = 0; face < faces; face++) {
            for (final int corner : TRIANGLES) {
                final int from = (face * 4 + corner) * BakedGeometry.STRIDE;
                System.arraycopy(vertices, from, out, at, BakedGeometry.STRIDE);
                out[at + BakedGeometry.STRIDE] = bone;
                out[at + BakedGeometry.STRIDE + 1] = part.glow() ? 1.0F : 0.0F;
                at += FLOATS;
            }
        }
        return out;
    }

    /**
     * The triangles painted with one of the model's pictures, on the graphics card.
     *
     * @param picture     the picture's place in the model
     * @param vertexArray how the shader reads them
     * @param buffer      where they are
     * @param corners     how many corners they have
     */
    record Group(int picture, int vertexArray, int buffer, int corners) {

        static Group of(final int picture, final List<float[]> parts) {
            int floats = 0;
            for (final float[] part : parts) {
                floats += part.length;
            }
            final FloatBuffer numbers = MemoryUtil.memAllocFloat(floats);
            try {
                for (final float[] part : parts) {
                    numbers.put(part);
                }
                numbers.flip();
                final int vertexArray = GlStateManager._glGenVertexArrays();
                GlStateManager._glBindVertexArray(vertexArray);
                final int buffer = GlStateManager._glGenBuffers();
                GlStateManager._glBindBuffer(GL15.GL_ARRAY_BUFFER, buffer);
                GL15.glBufferData(GL15.GL_ARRAY_BUFFER, numbers, GL15.GL_STATIC_DRAW);
                attribute(0, 3, 0);
                attribute(1, 2, 3);
                attribute(2, 3, 5);
                attribute(3, 2, 8);
                GlStateManager._glBindVertexArray(0);
                GlStateManager._glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
                // The game remembers which of its own it bound last; it binds its own again.
                BufferUploader.invalidate();
                return new Group(picture, vertexArray, buffer, floats / FLOATS);
            } finally {
                MemoryUtil.memFree(numbers);
            }
        }

        private static void attribute(final int place, final int size, final int offset) {
            GlStateManager._enableVertexAttribArray(place);
            GlStateManager._vertexAttribPointer(place, size, GL11.GL_FLOAT, false, STRIDE,
                    (long) offset * Float.BYTES);
        }
    }

    /**
     * The middle of a glowing part in its bone.
     *
     * @param bone the bone
     * @param x    across, in the model's pixels
     * @param y    up
     * @param z    along
     */
    record Glow(int bone, float x, float y, float z) {

        static Glow of(final int bone, final float[] vertices) {
            float x = 0;
            float y = 0;
            float z = 0;
            final int corners = vertices.length / BakedGeometry.STRIDE;
            for (int at = 0; at < vertices.length; at += BakedGeometry.STRIDE) {
                x += vertices[at];
                y += vertices[at + 1];
                z += vertices[at + 2];
            }
            return new Glow(bone, x / corners, y / corners, z / corners);
        }
    }
}
