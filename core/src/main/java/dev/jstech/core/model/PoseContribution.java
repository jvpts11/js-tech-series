/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.joml.Quaternionf;

/**
 * What one controller's animation does to a pose at one moment, bone by bone: the angles it adds, the turn it puts in
 * place of the rest one, the move and the size, and the weights of morph targets. Kept apart from the pose so a
 * change of animation can blend from the last of the old to the new.
 */
final class PoseContribution {

    private final float[] euler;
    private final boolean[] hasEuler;
    private final Quaternionf[] turns;
    private final boolean[] hasTurn;
    private final float[] moves;
    private final boolean[] hasMove;
    private final float[] sizes;
    private final boolean[] hasSize;
    private final Map<String, Float> weights = new HashMap<>();
    private final Quaternionf scratch = new Quaternionf();
    private final Quaternionf scratchEnd = new Quaternionf();

    PoseContribution(final int bones) {
        this.euler = new float[bones * 3];
        this.hasEuler = new boolean[bones];
        this.turns = new Quaternionf[bones];
        this.hasTurn = new boolean[bones];
        this.moves = new float[bones * 3];
        this.hasMove = new boolean[bones];
        this.sizes = new float[bones * 3];
        this.hasSize = new boolean[bones];
        for (int i = 0; i < bones; i++) {
            this.turns[i] = new Quaternionf();
        }
    }

    void clear() {
        Arrays.fill(this.hasEuler, false);
        Arrays.fill(this.hasTurn, false);
        Arrays.fill(this.hasMove, false);
        Arrays.fill(this.hasSize, false);
        this.weights.clear();
    }

    void euler(final int bone, final float[] value) {
        System.arraycopy(value, 0, this.euler, bone * 3, 3);
        this.hasEuler[bone] = true;
    }

    void turn(final int bone, final float[] value) {
        this.turns[bone].set(value[0], value[1], value[2], value[3]).normalize();
        this.hasTurn[bone] = true;
    }

    void move(final int bone, final float[] value) {
        System.arraycopy(value, 0, this.moves, bone * 3, 3);
        this.hasMove[bone] = true;
    }

    void size(final int bone, final float[] value) {
        System.arraycopy(value, 0, this.sizes, bone * 3, 3);
        this.hasSize[bone] = true;
    }

    void weight(final String target, final float value) {
        this.weights.put(target, value);
    }

    void copyFrom(final PoseContribution other) {
        System.arraycopy(other.euler, 0, this.euler, 0, this.euler.length);
        System.arraycopy(other.hasEuler, 0, this.hasEuler, 0, this.hasEuler.length);
        System.arraycopy(other.moves, 0, this.moves, 0, this.moves.length);
        System.arraycopy(other.hasMove, 0, this.hasMove, 0, this.hasMove.length);
        System.arraycopy(other.sizes, 0, this.sizes, 0, this.sizes.length);
        System.arraycopy(other.hasSize, 0, this.hasSize, 0, this.hasSize.length);
        System.arraycopy(other.hasTurn, 0, this.hasTurn, 0, this.hasTurn.length);
        for (int i = 0; i < this.turns.length; i++) {
            this.turns[i].set(other.turns[i]);
        }
        this.weights.clear();
        this.weights.putAll(other.weights);
    }

    /*
     * Writes the blend {@code t} of the way from {@code from} to {@code to} into this one: what only one of them moves
     * blends from or to where it is at rest.
     */
    void blend(final PoseContribution from, final PoseContribution to, final float t, final ModelData model) {
        for (int i = 0; i < this.hasEuler.length; i++) {
            this.hasEuler[i] = from.hasEuler[i] || to.hasEuler[i];
            this.hasMove[i] = from.hasMove[i] || to.hasMove[i];
            this.hasSize[i] = from.hasSize[i] || to.hasSize[i];
            this.hasTurn[i] = from.hasTurn[i] || to.hasTurn[i];
            for (int k = 0; k < 3; k++) {
                final int at = i * 3 + k;
                this.euler[at] = mix(from.hasEuler[i] ? from.euler[at] : 0, to.hasEuler[i] ? to.euler[at] : 0, t);
                this.moves[at] = mix(from.hasMove[i] ? from.moves[at] : 0, to.hasMove[i] ? to.moves[at] : 0, t);
                this.sizes[at] = mix(from.hasSize[i] ? from.sizes[at] : 1, to.hasSize[i] ? to.sizes[at] : 1, t);
            }
            if (this.hasTurn[i]) {
                final Quaternionf start = from.hasTurn[i] ? from.turns[i] : this.scratch.set(model.bones().get(i)
                        .rotation());
                final Quaternionf end = to.hasTurn[i] ? to.turns[i] : this.scratchEnd.set(model.bones().get(i)
                        .rotation());
                this.turns[i].set(start).slerp(end, t);
            }
        }
        final Set<String> targets = new HashSet<>(from.weights.keySet());
        targets.addAll(to.weights.keySet());
        this.weights.clear();
        for (final String target : targets) {
            this.weights.put(target, mix(from.weights.getOrDefault(target, 0.0F), to.weights.getOrDefault(target,
                    0.0F), t));
        }
    }

    void applyTo(final ModelPose pose) {
        for (int i = 0; i < this.hasEuler.length; i++) {
            if (this.hasEuler[i]) {
                pose.addEuler(i, this.euler[i * 3], this.euler[i * 3 + 1], this.euler[i * 3 + 2]);
            }
            if (this.hasTurn[i]) {
                pose.setTurn(i, this.turns[i]);
            }
            if (this.hasMove[i]) {
                pose.addMove(i, this.moves[i * 3], this.moves[i * 3 + 1], this.moves[i * 3 + 2]);
            }
            if (this.hasSize[i]) {
                pose.multiplySize(i, this.sizes[i * 3], this.sizes[i * 3 + 1], this.sizes[i * 3 + 2]);
            }
        }
        this.weights.forEach(pose::setWeight);
    }

    private static float mix(final float from, final float to, final float t) {
        return from + (to - from) * t;
    }
}
