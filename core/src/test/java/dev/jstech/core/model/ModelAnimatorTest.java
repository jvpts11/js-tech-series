/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class ModelAnimatorTest {

    private static final double EPSILON = 1.0e-3;

    @Test
    void animation_isFoundByItsWholeNameOrItsLastPart() {
        final ModelAnimator animator = new ModelAnimator(fan(), List.of(spin()));
        assertTrue(animator.animation("animation.fan.spin").isPresent());
        assertTrue(animator.animation("spin").isPresent());
        assertFalse(animator.play("main", "nothing", 0), "an animation it has none of plays nothing");
    }

    @Test
    void animate_addsEveryControllersTurnsThenTheCodes() {
        final ModelAnimator animator = new ModelAnimator(fan(), List.of(spin()));
        animator.controller("main").play(spin(), 0, 0);
        animator.controller("wobble").play(spin(), 0, 0);
        animator.mover((pose, seconds) -> pose.addEuler(pose.index("blades"), 0, 10, 0));
        final ModelPose pose = animator.animate(0.25, IAnimationListener.NONE);
        final Vector3f tip = pose.local(pose.index("blades"), new Matrix4f()).transformPosition(new Vector3f(0, 0,
                1));
        // Two controllers turn it 90 each at a quarter second, and the code ten more: 190 about Y in all.
        final double turned = Math.toDegrees(Math.atan2(tip.x, tip.z));
        assertEquals(-170, turned, EPSILON);
    }

    @Test
    void animate_countsTheModelsLifeFromItsFirstFrame() {
        final ModelAnimator animator = new ModelAnimator(fan(), List.of());
        animator.animate(100, IAnimationListener.NONE);
        animator.animate(102.5, IAnimationListener.NONE);
        assertEquals(2.5, animator.context().lifeTime(), EPSILON);
    }

    private static ModelData fan() {
        return new ModelData("fan", 16, 16, List.of(ModelBone.ofEuler("blades", null, new Vector3f(),
                new Vector3f(), List.of(), List.of(), List.of(), false)), Map.of(), List.of(), List.of());
    }

    /* A spin about Y, a whole turn a second. */
    private static ModelAnimation spin() {
        final AnimationChannel turn = new AnimationChannel(AnimationChannel.Kind.EULER,
                List.of(Keyframe.of(0, 0, 0, 0), Keyframe.of(1, 0, 360, 0)));
        return new ModelAnimation("animation.fan.spin", 1, ModelAnimation.Loop.LOOP,
                Map.of("blades", new ModelAnimation.Tracks(turn, null, null)), Map.of(), List.of());
    }
}
