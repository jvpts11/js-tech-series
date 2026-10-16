/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * A box of a model, in the model's own space and its pixels: its lowest corner and its size, grown by {@code inflate}
 * on every side, turned about {@code pivot}, and the picture each face shows, mirrored left to right when
 * {@code mirror}. A face with no picture is not drawn.
 *
 * @param from     its lowest corner
 * @param size     its size along each axis
 * @param inflate  how far it grows on every side, without moving its picture
 * @param pivot    the point it is turned about
 * @param rotation its turn, in degrees about X, Y and Z, X first
 * @param faces    the picture of each face drawn
 * @param mirror   whether its pictures are mirrored left to right, its east and west sides changing places
 */
public record ModelCube(Vector3fc from, Vector3fc size, float inflate, Vector3fc pivot, Vector3fc rotation,
                        Map<CubeFace, FaceUv> faces, boolean mirror) {

    public ModelCube {
        from = new Vector3f(Objects.requireNonNull(from, "from"));
        size = new Vector3f(Objects.requireNonNull(size, "size"));
        pivot = new Vector3f(Objects.requireNonNull(pivot, "pivot"));
        rotation = new Vector3f(Objects.requireNonNull(rotation, "rotation"));
        final Map<CubeFace, FaceUv> kept = new EnumMap<>(CubeFace.class);
        kept.putAll(faces);
        faces = Map.copyOf(kept);
    }

    /**
     * The pictures of a cube laid out the way box UV lays them, as Blockbench and Bedrock do, from the corner
     * {@code u, v}: the top and the bottom in the first row, the four sides under them, each as wide as the cube's
     * whole pixels.
     */
    public static Map<CubeFace, FaceUv> boxUv(final float u, final float v, final Vector3fc size) {
        final float x = (float) Math.floor(size.x());
        final float y = (float) Math.floor(size.y());
        final float z = (float) Math.floor(size.z());
        final Map<CubeFace, FaceUv> faces = new EnumMap<>(CubeFace.class);
        faces.put(CubeFace.EAST, FaceUv.of(u, v + z, z, y));
        faces.put(CubeFace.NORTH, FaceUv.of(u + z, v + z, x, y));
        faces.put(CubeFace.WEST, FaceUv.of(u + z + x, v + z, z, y));
        faces.put(CubeFace.SOUTH, FaceUv.of(u + z + x + z, v + z, x, y));
        faces.put(CubeFace.UP, FaceUv.of(u + z, v, x, z));
        faces.put(CubeFace.DOWN, FaceUv.of(u + z + x, v + z, x, -z));
        return faces;
    }
}
