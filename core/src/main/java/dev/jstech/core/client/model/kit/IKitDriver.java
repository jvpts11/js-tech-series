/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.model.kit;

import dev.jstech.core.model.ModelAnimation;
import dev.jstech.core.model.ModelAnimator;
import dev.jstech.core.model.ModelPose;

/**
 * The code that drives the model of one kind of thing on the player's game, every frame it is drawn: which animation
 * each controller plays, from what the thing is doing, and how its bones move besides; and what it does with the
 * words its animations hand to code. A driver that moves bones says so, so the thing is never taken to be at rest.
 *
 * <pre>{@code
 * (machine, animator, seconds) -> animator.play("main", machine.running() ? "working" : "idle", seconds)
 * }</pre>
 *
 * @param <T> what is drawn
 */
@FunctionalInterface
public interface IKitDriver<T> {

    /** Drives nothing: the model plays what the server tells it to, and nothing else. */
    static <T> IKitDriver<T> none() {
        return (thing, animator, seconds) -> {
        };
    }

    /**
     * Chooses what plays, and moves bones, before the frame is worked out.
     *
     * @param thing    what is drawn
     * @param animator what moves its model
     * @param seconds  the model's clock, in seconds
     */
    void drive(T thing, ModelAnimator animator, double seconds);

    /**
     * Moves {@code thing}'s bones after its animations, every frame its pose is worked out: a lamp shown while the
     * machine runs, a part hidden while its slot is empty, a fan turned as fast as the machine goes. A driver that
     * moves bones here says so with {@link #movesBones()}.
     *
     * @param thing   what is drawn
     * @param pose    its pose, as the animations and the animator's movers left it
     * @param seconds the model's clock, in seconds
     */
    default void move(final T thing, final ModelPose pose, final double seconds) {
    }

    /** Whether it moves bones itself every frame, here or through movers it added to the animator. */
    default boolean movesBones() {
        return false;
    }

    /** A word an animation handed to code at its time, as {@code v.opened = 1;} or a name. */
    default void instruction(final T thing, final ModelAnimation animation, final String word) {
    }
}
