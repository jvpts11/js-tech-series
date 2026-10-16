/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

import java.util.HashMap;
import java.util.Map;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;

/**
 * Where every bone of a model is at one moment: its rest pose, as its file has it, moved by the animations playing and
 * by the code that drives the model, and the weight of each of its morph targets. Animations add to a bone's turn in
 * degrees, as Bedrock's do, or take its turn's place, as glTF's and FBX's do; they move it from its rest place and
 * multiply its size. A pose is worked out once a frame for what is in view and drawn from {@link #world}. Pure, and
 * not shared between threads.
 */
public final class ModelPose {

    private final ModelData model;
    private final Map<String, Integer> index = new HashMap<>();
    private final int[] parents;
    private final boolean[] eulerRest;
    private final float[] euler;
    private final Quaternionf[] turns;
    private final boolean[] turned;
    private final float[] moves;
    private final float[] sizes;
    private final boolean[] hidden;
    private final Map<String, Float> weights = new HashMap<>();
    private final Quaternionf scratchTurn = new Quaternionf();
    private final Quaternionf scratchEuler = new Quaternionf();

    private static final float SAME = 1.0e-4F;

    public ModelPose(final ModelData model) {
        this.model = model;
        final int count = model.bones().size();
        this.parents = new int[count];
        this.eulerRest = new boolean[count];
        this.euler = new float[count * 3];
        this.turns = new Quaternionf[count];
        this.turned = new boolean[count];
        this.moves = new float[count * 3];
        this.sizes = new float[count * 3];
        this.hidden = new boolean[count];
        for (int i = 0; i < count; i++) {
            this.index.put(model.bones().get(i).name(), i);
            this.turns[i] = new Quaternionf();
        }
        for (int i = 0; i < count; i++) {
            final ModelBone bone = model.bones().get(i);
            this.parents[i] = bone.parent() == null ? -1 : this.index.getOrDefault(bone.parent(), -1);
            // A bone turned at rest by angles, as Bedrock's are, has angles added to them; any other is turned on.
            this.eulerRest[i] = Rotations.ofEuler(bone.euler()).equals(bone.rotation(), SAME);
        }
        reset();
    }

    /** The model it is a pose of. */
    public ModelData model() {
        return this.model;
    }

    /** The place of the bone {@code name} among the model's, or -1 for none of that name. */
    public int index(final String name) {
        return this.index.getOrDefault(name, -1);
    }

    /** How many bones it has. */
    public int bones() {
        return this.parents.length;
    }

    /** Puts every bone back at rest, its hiding as its file has it, and every morph target at no weight. */
    public void reset() {
        for (int i = 0; i < this.parents.length; i++) {
            this.turned[i] = false;
            this.hidden[i] = this.model.bones().get(i).hidden();
            for (int k = 0; k < 3; k++) {
                this.euler[i * 3 + k] = 0;
                this.moves[i * 3 + k] = 0;
                this.sizes[i * 3 + k] = 1;
            }
        }
        this.weights.clear();
    }

    /** Adds to the turn of bone {@code bone} in degrees about X, Y and Z, X first. */
    public void addEuler(final int bone, final float x, final float y, final float z) {
        this.euler[bone * 3] += x;
        this.euler[bone * 3 + 1] += y;
        this.euler[bone * 3 + 2] += z;
    }

    /** Turns bone {@code bone} to {@code turn} in place of its rest turn. */
    public void setTurn(final int bone, final Quaternionfc turn) {
        this.turns[bone].set(turn);
        this.turned[bone] = true;
    }

    /** Moves bone {@code bone} on from its rest place, in pixels. */
    public void addMove(final int bone, final float x, final float y, final float z) {
        this.moves[bone * 3] += x;
        this.moves[bone * 3 + 1] += y;
        this.moves[bone * 3 + 2] += z;
    }

    /** Multiplies the size of bone {@code bone}. */
    public void multiplySize(final int bone, final float x, final float y, final float z) {
        this.sizes[bone * 3] *= x;
        this.sizes[bone * 3 + 1] *= y;
        this.sizes[bone * 3 + 2] *= z;
    }

    /** Hides or shows bone {@code bone} and everything that hangs from it. */
    public void setHidden(final int bone, final boolean hide) {
        this.hidden[bone] = hide;
    }

    /** Whether bone {@code bone} itself is hidden, whatever the bones it hangs from are. */
    public boolean hides(final int bone) {
        return this.hidden[bone];
    }

    /** Whether bone {@code bone}, or a bone it hangs from, is hidden. */
    public boolean hidden(final int bone) {
        for (int at = bone; at >= 0; at = this.parents[at]) {
            if (this.hidden[at]) {
                return true;
            }
        }
        return false;
    }

    /** Sets the weight of the morph target {@code target}, from 0 to 1. */
    public void setWeight(final String target, final float weight) {
        this.weights.put(target, weight);
    }

    /** The weight of the morph target {@code target}; 0 until one is set. */
    public float weight(final String target) {
        return this.weights.getOrDefault(target, 0.0F);
    }

    /** The bone a bone hangs from, by its place, or -1 for a root. */
    public int parent(final int bone) {
        return this.parents[bone];
    }

    /**
     * Writes bone {@code bone}'s place in its parent's space into {@code out}: moved to its place, turned and sized
     * about its pivot.
     */
    public Matrix4f local(final int bone, final Matrix4f out) {
        final ModelBone rest = this.model.bones().get(bone);
        final int at = bone * 3;
        final Quaternionf turn;
        if (this.turned[bone]) {
            turn = this.scratchTurn.set(this.turns[bone]).mul(Rotations.ofEuler(this.euler[at], this.euler[at + 1],
                    this.euler[at + 2], this.scratchEuler));
        } else if (this.eulerRest[bone]) {
            turn = Rotations.ofEuler(rest.euler().x() + this.euler[at], rest.euler().y() + this.euler[at + 1],
                    rest.euler().z() + this.euler[at + 2], this.scratchTurn);
        } else {
            turn = this.scratchTurn.set(rest.rotation()).mul(Rotations.ofEuler(this.euler[at], this.euler[at + 1],
                    this.euler[at + 2], this.scratchEuler));
        }
        final float px = rest.pivot().x();
        final float py = rest.pivot().y();
        final float pz = rest.pivot().z();
        return out.translation(rest.translation().x() + this.moves[at] + px,
                        rest.translation().y() + this.moves[at + 1] + py,
                        rest.translation().z() + this.moves[at + 2] + pz)
                .rotate(turn)
                .scale(rest.scale().x() * this.sizes[at], rest.scale().y() * this.sizes[at + 1],
                        rest.scale().z() * this.sizes[at + 2])
                .translate(-px, -py, -pz);
    }

    /**
     * Writes every bone's place in the model's own space into {@code out}, one matrix each in the model's order; each
     * is its parent's with its own after it.
     */
    public void world(final Matrix4f[] out) {
        for (int i = 0; i < this.parents.length; i++) {
            local(i, out[i]);
            if (this.parents[i] >= 0) {
                out[i].mulLocal(out[this.parents[i]]);
            }
        }
    }

    /** The point a locator is at in the model's space, its bone placed by {@code world}. */
    public Vector3f locator(final String name, final Matrix4f[] world, final Vector3f out) {
        for (int i = 0; i < this.parents.length; i++) {
            for (final ModelLocator locator : this.model.bones().get(i).locators()) {
                if (locator.name().equals(name)) {
                    return world[i].transformPosition(out.set(locator.offset()));
                }
            }
        }
        return out.zero();
    }
}
