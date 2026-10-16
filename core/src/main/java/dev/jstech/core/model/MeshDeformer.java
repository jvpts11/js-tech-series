/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Works out, each frame, where the corners of a mesh that moves are: moved toward its morph targets by their weights
 * in the pose, then, for one that bends with a skin, carried by the bones of the skin, each by its share. Keeps its
 * own room, so a frame makes nothing new; one for each mesh drawn, kept by one thread. Pure.
 */
public final class MeshDeformer {

    private final BakedGeometry.Deformed deformed;
    private final float[] moved;
    private final int[] jointBones;
    private final Matrix4f[] joints;
    private final Matrix4f bind = new Matrix4f();
    private final Vector3f corner = new Vector3f();
    private final Vector3f sum = new Vector3f();
    private final Vector3f carried = new Vector3f();

    public MeshDeformer(final BakedGeometry.Deformed deformed, final ModelPose pose) {
        this.deformed = deformed;
        this.moved = new float[deformed.mesh().positions().length];
        final ModelSkin skin = deformed.skin();
        final int count = skin == null ? 0 : skin.joints().size();
        this.jointBones = new int[count];
        this.joints = new Matrix4f[count];
        for (int j = 0; j < count; j++) {
            this.jointBones[j] = pose.index(skin.joints().get(j));
            this.joints[j] = new Matrix4f();
        }
    }

    /** The mesh it works out. */
    public BakedGeometry.Deformed deformed() {
        return this.deformed;
    }

    /**
     * Works out where the corners are in {@code pose}, its bones placed by {@code world}; the corners it hands back
     * are kept for the next frame, so they are read before it is asked again.
     */
    public float[] deform(final ModelPose pose, final Matrix4f[] world) {
        final ModelMesh mesh = this.deformed.mesh();
        System.arraycopy(mesh.positions(), 0, this.moved, 0, this.moved.length);
        for (final ModelMesh.MorphTarget target : mesh.morphs()) {
            final float weight = pose.weight(target.name());
            if (weight == 0) {
                continue;
            }
            final float[] moves = target.moves();
            for (int i = 0; i < this.moved.length && i < moves.length; i++) {
                this.moved[i] += moves[i] * weight;
            }
        }
        final ModelSkin skin = this.deformed.skin();
        if (skin == null) {
            return this.moved;
        }
        final float[] binds = skin.inverseBinds();
        for (int j = 0; j < this.joints.length; j++) {
            final int bone = this.jointBones[j];
            if (bone < 0) {
                this.joints[j].identity();
                continue;
            }
            this.joints[j].set(world[bone]);
            if (binds.length > 0) {
                this.joints[j].mul(this.bind.set(binds, j * 16));
            }
        }
        final int[] jointsOf = mesh.joints();
        final float[] weights = mesh.weights();
        final int corners = mesh.corners();
        final int per = corners == 0 ? 0 : jointsOf.length / corners;
        for (int c = 0; c < corners; c++) {
            this.corner.set(this.moved[c * 3], this.moved[c * 3 + 1], this.moved[c * 3 + 2]);
            this.sum.zero();
            float total = 0;
            for (int k = 0; k < per; k++) {
                final float share = weights[c * per + k];
                final int joint = jointsOf[c * per + k];
                if (share == 0 || joint < 0 || joint >= this.joints.length) {
                    continue;
                }
                this.joints[joint].transformPosition(this.carried.set(this.corner));
                this.sum.fma(share, this.carried);
                total += share;
            }
            if (total > 0) {
                this.sum.div(total);
                this.moved[c * 3] = this.sum.x;
                this.moved[c * 3 + 1] = this.sum.y;
                this.moved[c * 3 + 2] = this.sum.z;
            }
        }
        return this.moved;
    }
}
