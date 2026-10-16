/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Works out a model's shape once, as {@link BakedGeometry} keeps it: each cube's six faces with the pictures its file
 * gives them, turned about its pivot, as Bedrock and Blockbench draw them; each mesh's triangles, with normals where
 * its file gives none. A bone glows when its name ends in {@code _glow}, when the definition names it, or when a bone
 * it hangs from glows. Pure.
 */
public final class GeometryBaker {

    private static final float FLAT = 1.0e-6F;

    private GeometryBaker() {
    }

    /**
     * The shape of {@code model}.
     *
     * @param glow the bones that glow besides those named {@code _glow}
     */
    public static BakedGeometry bake(final ModelData model, final Set<String> glow) {
        final List<List<BakedGeometry.Part>> bones = new ArrayList<>();
        final List<BakedGeometry.Deformed> deformed = new ArrayList<>();
        final boolean[] glows = new boolean[model.bones().size()];
        final Map<String, Integer> index = new LinkedHashMap<>();
        for (int i = 0; i < model.bones().size(); i++) {
            final ModelBone bone = model.bones().get(i);
            index.put(bone.name(), i);
            final Integer parent = bone.parent() == null ? null : index.get(bone.parent());
            glows[i] = bone.glows() || glow.contains(bone.name()) || parent != null && glows[parent];
        }
        for (int i = 0; i < model.bones().size(); i++) {
            final ModelBone bone = model.bones().get(i);
            final Map<Integer, Corners> byTexture = new LinkedHashMap<>();
            for (final ModelCube cube : bone.cubes()) {
                cube(model, cube, byTexture);
            }
            for (final ModelMesh mesh : bone.meshes()) {
                final ModelSkin skin = model.skins().get(bone.name());
                if (mesh.skinned() || !mesh.morphs().isEmpty()) {
                    deformed.add(new BakedGeometry.Deformed(i, glows[i], mesh, mesh.skinned() ? skin : null));
                } else {
                    mesh(mesh, mesh.positions(), byTexture.computeIfAbsent(mesh.texture(), key -> new Corners()));
                }
            }
            final List<BakedGeometry.Part> parts = new ArrayList<>();
            final boolean glowing = glows[i];
            byTexture.forEach((texture, corners) -> parts.add(new BakedGeometry.Part(texture, glowing,
                    corners.done())));
            bones.add(parts);
        }
        return new BakedGeometry(bones, deformed);
    }

    /**
     * Lays a mesh's triangles out as four-cornered faces, its corners at {@code positions}, which a mesh moved toward
     * its morph targets or bent by a skin gives in place of its own; writes them into {@code out}.
     */
    public static void mesh(final ModelMesh mesh, final float[] positions, final Corners out) {
        final int[] triangles = mesh.triangles();
        final boolean given = mesh.normals().length == positions.length;
        final Vector3f a = new Vector3f();
        final Vector3f b = new Vector3f();
        final Vector3f c = new Vector3f();
        final Vector3f flat = new Vector3f();
        for (int t = 0; t + 2 < triangles.length; t += 3) {
            a.set(positions[triangles[t] * 3], positions[triangles[t] * 3 + 1], positions[triangles[t] * 3 + 2]);
            b.set(positions[triangles[t + 1] * 3], positions[triangles[t + 1] * 3 + 1],
                    positions[triangles[t + 1] * 3 + 2]);
            c.set(positions[triangles[t + 2] * 3], positions[triangles[t + 2] * 3 + 1],
                    positions[triangles[t + 2] * 3 + 2]);
            // A face is lit by the way it faces: worked out from its corners, counter-clockwise from the front.
            new Vector3f(b).sub(a).cross(new Vector3f(c).sub(a), flat);
            if (flat.lengthSquared() > FLAT) {
                flat.normalize();
            } else {
                flat.set(0, 1, 0);
            }
            for (int k = 0; k < 4; k++) {
                final int corner = triangles[t + Math.min(k, 2)];
                final float[] uvs = mesh.uvs();
                final float u = corner * 2 + 1 < uvs.length ? uvs[corner * 2] : 0;
                final float v = corner * 2 + 1 < uvs.length ? uvs[corner * 2 + 1] : 0;
                if (given) {
                    out.add(positions[corner * 3], positions[corner * 3 + 1], positions[corner * 3 + 2], u, v,
                            mesh.normals()[corner * 3], mesh.normals()[corner * 3 + 1],
                            mesh.normals()[corner * 3 + 2]);
                } else {
                    out.add(positions[corner * 3], positions[corner * 3 + 1], positions[corner * 3 + 2], u, v,
                            flat.x, flat.y, flat.z);
                }
            }
        }
    }

    /*
     * A cube's faces, each as Bedrock lays it: its corners in a fixed order round the face, the picture's right edge
     * at the first corner unless the cube is mirrored, turned by the face's quarter turns.
     */
    private static void cube(final ModelData model, final ModelCube cube, final Map<Integer, Corners> byTexture) {
        final float grow = cube.inflate();
        final float lx = cube.from().x() - grow;
        final float ly = cube.from().y() - grow;
        final float lz = cube.from().z() - grow;
        final float hx = cube.from().x() + cube.size().x() + grow;
        final float hy = cube.from().y() + cube.size().y() + grow;
        final float hz = cube.from().z() + cube.size().z() + grow;
        final Quaternionf turn = Rotations.ofEuler(cube.rotation());
        final boolean turned = cube.rotation().lengthSquared() > 0;
        final boolean boxUv = isBoxUv(cube);
        final Vector3f corner = new Vector3f();
        final Vector3f normal = new Vector3f();
        for (final Map.Entry<CubeFace, FaceUv> entry : cube.faces().entrySet()) {
            final CubeFace face = entry.getKey();
            final FaceUv picture = entry.getValue();
            final float[] corners = corners(face, cube.mirror(), boxUv, lx, ly, lz, hx, hy, hz);
            final float[] uvs = uvs(model, picture, cube.mirror());
            normal.set(face.stepX(), face.stepY(), face.stepZ());
            if (cube.mirror()) {
                normal.x = -normal.x;
            }
            flatten(cube, normal);
            if (turned) {
                turn.transform(normal);
            }
            final Corners out = byTexture.computeIfAbsent(picture.texture(), key -> new Corners());
            for (int k = 0; k < 4; k++) {
                corner.set(corners[k * 3], corners[k * 3 + 1], corners[k * 3 + 2]);
                if (turned) {
                    corner.sub(cube.pivot());
                    turn.transform(corner);
                    corner.add(cube.pivot());
                }
                out.add(corner.x, corner.y, corner.z, uvs[k * 2], uvs[k * 2 + 1], normal.x, normal.y, normal.z);
            }
        }
    }

    /* Whether a cube's pictures are the ones box UV lays out, which a mirrored cube's top and bottom keep. */
    private static boolean isBoxUv(final ModelCube cube) {
        final FaceUv north = cube.faces().get(CubeFace.NORTH);
        final FaceUv up = cube.faces().get(CubeFace.UP);
        if (north == null || up == null || cube.faces().size() != 6) {
            return false;
        }
        final Map<CubeFace, FaceUv> laid = ModelCube.boxUv(north.u() - (float) Math.floor(cube.size().z()),
                north.v() - (float) Math.floor(cube.size().z()), cube.size());
        return laid.equals(cube.faces());
    }

    /* The four corners of a face, three numbers each, in the order Bedrock walks them. */
    private static float[] corners(final CubeFace face, final boolean mirror, final boolean boxUv, final float lx,
                                   final float ly, final float lz, final float hx, final float hy, final float hz) {
        final CubeFace walked = switch (face) {
            case WEST -> mirror ? CubeFace.EAST : CubeFace.WEST;
            case EAST -> mirror ? CubeFace.WEST : CubeFace.EAST;
            case UP -> mirror && !boxUv ? CubeFace.DOWN : CubeFace.UP;
            case DOWN -> mirror && !boxUv ? CubeFace.UP : CubeFace.DOWN;
            default -> face;
        };
        return switch (walked) {
            case WEST -> new float[] {lx, hy, hz, lx, hy, lz, lx, ly, lz, lx, ly, hz};
            case EAST -> new float[] {hx, hy, lz, hx, hy, hz, hx, ly, hz, hx, ly, lz};
            case NORTH -> new float[] {lx, hy, lz, hx, hy, lz, hx, ly, lz, lx, ly, lz};
            case SOUTH -> new float[] {hx, hy, hz, lx, hy, hz, lx, ly, hz, hx, ly, hz};
            case UP -> new float[] {lx, hy, hz, hx, hy, hz, hx, hy, lz, lx, hy, lz};
            case DOWN -> new float[] {lx, ly, lz, hx, ly, lz, hx, ly, hz, lx, ly, hz};
        };
    }

    /*
     * The point of the texture at each of a face's four corners, from 0 to 1. The first corner of a face takes the
     * picture's right edge, as the face is seen from outside, and a mirrored cube's its left.
     */
    private static float[] uvs(final ModelData model, final FaceUv picture, final boolean mirror) {
        final float width = textureWidth(model, picture.texture());
        final float height = textureHeight(model, picture.texture());
        final float left = picture.u() / width;
        final float right = (picture.u() + picture.width()) / width;
        final float first = mirror ? left : right;
        final float other = mirror ? right : left;
        final float top = picture.v() / height;
        final float bottom = (picture.v() + picture.height()) / height;
        return switch (Math.floorMod(picture.rotation(), 360)) {
            case 90 -> new float[] {other, top, other, bottom, first, bottom, first, top};
            case 180 -> new float[] {other, bottom, first, bottom, first, top, other, top};
            case 270 -> new float[] {first, bottom, first, top, other, top, other, bottom};
            default -> new float[] {first, top, other, top, other, bottom, first, bottom};
        };
    }

    /* A flat cube is lit alike from both its sides: its face toward the minus side faces the plus side. */
    private static void flatten(final ModelCube cube, final Vector3f normal) {
        final Vector3f size = new Vector3f(cube.size());
        if (normal.x < 0 && (size.y == 0 || size.z == 0)) {
            normal.x = -normal.x;
        }
        if (normal.y < 0 && (size.x == 0 || size.z == 0)) {
            normal.y = -normal.y;
        }
        if (normal.z < 0 && (size.x == 0 || size.y == 0)) {
            normal.z = -normal.z;
        }
    }

    private static float textureWidth(final ModelData model, final int texture) {
        return texture < model.textures().size() && model.textures().get(texture).width() > 0
                ? model.textures().get(texture).width() : model.textureWidth();
    }

    private static float textureHeight(final ModelData model, final int texture) {
        return texture < model.textures().size() && model.textures().get(texture).height() > 0
                ? model.textures().get(texture).height() : model.textureHeight();
    }

    /** Corners laid out as {@link BakedGeometry} keeps them, growing as they are added. */
    public static final class Corners {

        private float[] data = new float[BakedGeometry.STRIDE * 4 * 8];
        private int size;

        /** Adds one corner. */
        public void add(final float x, final float y, final float z, final float u, final float v, final float nx,
                        final float ny, final float nz) {
            if (this.size + BakedGeometry.STRIDE > this.data.length) {
                this.data = Arrays.copyOf(this.data, this.data.length * 2);
            }
            this.data[this.size++] = x;
            this.data[this.size++] = y;
            this.data[this.size++] = z;
            this.data[this.size++] = u;
            this.data[this.size++] = v;
            this.data[this.size++] = nx;
            this.data[this.size++] = ny;
            this.data[this.size++] = nz;
        }

        /** Forgets the corners, keeping the room they took. */
        public void clear() {
            this.size = 0;
        }

        /** How many numbers it holds. */
        public int size() {
            return this.size;
        }

        /** The numbers it holds; only the first {@link #size()} are its. */
        public float[] data() {
            return this.data;
        }

        /** Its corners, as many numbers as it holds. */
        public float[] done() {
            return Arrays.copyOf(this.data, this.size);
        }
    }
}
