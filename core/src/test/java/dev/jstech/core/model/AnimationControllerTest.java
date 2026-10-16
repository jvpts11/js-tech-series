/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class AnimationControllerTest {

    private static final double EPSILON = 1.0e-3;

    @Test
    void apply_movesTheBonesAsItsAnimationSays() {
        final ModelPose pose = new ModelPose(lid());
        final AnimationController controller = new AnimationController("lid");
        controller.play(raise(ModelAnimation.Loop.LOOP), 10, 0);
        assertEquals(4, height(pose, controller, 10.5), EPSILON, "halfway through, halfway up");
        assertEquals(4, height(pose, controller, 11.5), EPSILON, "and round again a second later");
    }

    @Test
    void apply_holdsTheLastFrameOrGoesBackAsTheLoopSays() {
        final ModelPose pose = new ModelPose(lid());
        final AnimationController held = new AnimationController("lid");
        held.play(raise(ModelAnimation.Loop.HOLD), 0, 0);
        assertEquals(8, height(pose, held, 5), EPSILON, "held at the top");
        final AnimationController once = new AnimationController("lid");
        once.play(raise(ModelAnimation.Loop.ONCE), 0, 0);
        assertEquals(0, height(pose, once, 5), EPSILON, "back at rest once it is over");
        assertNull(once.playing());
        assertTrue(once.idle(5));
    }

    @Test
    void speed_playsFasterFromWhereItIs() {
        final ModelPose pose = new ModelPose(lid());
        final AnimationController controller = new AnimationController("lid");
        controller.play(raise(ModelAnimation.Loop.HOLD), 0, 0);
        controller.speed(2, 0.25);
        // A quarter second at one, then a quarter at two: three quarters of the way.
        assertEquals(6, height(pose, controller, 0.5), EPSILON);
    }

    @Test
    void play_blendsFromWhereTheBonesWereToTheNewAnimation() {
        final ModelPose pose = new ModelPose(lid());
        final AnimationController controller = new AnimationController("lid");
        controller.play(raise(ModelAnimation.Loop.HOLD), 0, 0);
        assertEquals(8, height(pose, controller, 2), EPSILON);
        controller.play(stay(), 2, 1);
        assertEquals(4, height(pose, controller, 2.5), EPSILON, "halfway from the top down to rest");
        assertEquals(0, height(pose, controller, 3.5), EPSILON, "and there once the blend is over");
    }

    @Test
    void play_letsAnAnimationAlreadyPlayingGoOn() {
        final ModelPose pose = new ModelPose(lid());
        final AnimationController controller = new AnimationController("lid");
        final ModelAnimation raise = raise(ModelAnimation.Loop.HOLD);
        controller.play(raise, 0, 0);
        controller.play(raise, 0.5, 0);
        assertEquals(6, height(pose, controller, 0.75), EPSILON, "not started again");
    }

    @Test
    void apply_tellsOfEveryEventPassedRoundEveryLoop() {
        final ModelPose pose = new ModelPose(lid());
        final AnimationController controller = new AnimationController("lid");
        controller.play(raise(ModelAnimation.Loop.LOOP), 0, 0);
        final List<String> heard = new ArrayList<>();
        final IAnimationListener listener = (name, animation, event) -> heard.add(event.value());
        controller.apply(pose, 0, new ExpressionContext(), listener);
        assertEquals(List.of("start"), heard, "an event at the start is heard as it starts");
        controller.apply(pose, 2.6, new ExpressionContext(), listener);
        assertEquals(List.of("start", "click", "start", "click", "start", "click"), heard,
                "once a round for every round gone by");
    }

    /* A lid that rises eight pixels over a second, with a sound as it starts and a click halfway. */
    private static ModelAnimation raise(final ModelAnimation.Loop loop) {
        final AnimationChannel up = new AnimationChannel(AnimationChannel.Kind.POSITION,
                List.of(Keyframe.of(0, 0, 0, 0), Keyframe.of(1, 0, 8, 0)));
        return new ModelAnimation("raise", 1, loop, Map.of("lid", new ModelAnimation.Tracks(null, up, null)),
                Map.of(), List.of(new ModelAnimation.Event(0, ModelAnimation.EventKind.SOUND, "start", ""),
                new ModelAnimation.Event(0.5, ModelAnimation.EventKind.SOUND, "click", "")));
    }

    private static ModelAnimation stay() {
        return new ModelAnimation("stay", 1, ModelAnimation.Loop.LOOP, Map.of(), Map.of(), List.of());
    }

    private static ModelData lid() {
        return new ModelData("box", 16, 16, List.of(ModelBone.ofEuler("lid", null, new Vector3f(), new Vector3f(),
                List.of(), List.of(), List.of(), false)), Map.of(), List.of(), List.of());
    }

    private static float height(final ModelPose pose, final AnimationController controller, final double now) {
        pose.reset();
        controller.apply(pose, now, new ExpressionContext(), IAnimationListener.NONE);
        return pose.local(0, new Matrix4f()).transformPosition(new Vector3f()).y;
    }
}
