/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock.shape;

import dev.jstech.core.model.BakedGeometry;
import dev.jstech.core.model.ModelData;
import dev.jstech.core.model.ModelPose;
import java.util.Arrays;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * The faces of one of the Core's models at rest, as triangles in the model's own space, in blocks: each bone placed
 * where its pose at rest puts it, the bones hidden at rest left out, and the model as big as its declaration draws it.
 * The same on the server, where the data run reads the model's file, and on a player's game, where the model is drawn.
 */
public final class ModelTriangles {

    private static final float PIXEL = 1.0F / 16.0F;
    private static final int CORNERS = 4;
    private static final int TRIANGLE = 9;
    private static final int[][] HALVES = {{0, 1, 2}, {0, 2, 3}};

    private ModelTriangles() {
    }

    /**
     * The triangles of the model {@code data} drawn as {@code geometry}, {@code scale} times as big as its file says.
     *
     * @return nine numbers to a triangle
     */
    public static float[] of(final ModelData data, final BakedGeometry geometry, final double scale) {
        final ModelPose pose = new ModelPose(data);
        final Matrix4f[] world = new Matrix4f[data.bones().size()];
        for (int i = 0; i < world.length; i++) {
            world[i] = new Matrix4f();
        }
        pose.world(world);
        final float size = (float) (PIXEL * scale);
        float[] out = new float[TRIANGLE * CORNERS * 2];
        int count = 0;
        final Vector3f corner = new Vector3f();
        final float[] quad = new float[CORNERS * 3];
        for (int bone = 0; bone < world.length && bone < geometry.bones().size(); bone++) {
            if (pose.hidden(bone)) {
                continue;
            }
            for (final BakedGeometry.Part part : geometry.partsOf(bone)) {
                final float[] vertices = part.vertices();
                for (int face = 0; face < part.faces(); face++) {
                    for (int c = 0; c < CORNERS; c++) {
                        final int from = (face * CORNERS + c) * BakedGeometry.STRIDE;
                        world[bone].transformPosition(corner.set(vertices[from], vertices[from + 1],
                                vertices[from + 2]));
                        quad[c * 3] = corner.x * size;
                        quad[c * 3 + 1] = corner.y * size;
                        quad[c * 3 + 2] = corner.z * size;
                    }
                    for (final int[] half : HALVES) {
                        if (count + TRIANGLE > out.length) {
                            out = Arrays.copyOf(out, out.length * 2);
                        }
                        for (final int c : half) {
                            out[count++] = quad[c * 3];
                            out[count++] = quad[c * 3 + 1];
                            out[count++] = quad[c * 3 + 2];
                        }
                    }
                }
            }
        }
        return Arrays.copyOf(out, count);
    }
}
