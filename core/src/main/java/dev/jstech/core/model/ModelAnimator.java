/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Everything that moves one drawn model: its controllers, each playing an animation of its own, in the order they
 * were made, a later one's turns added to an earlier one's and its turns in place of them; then the code that moves
 * bones itself; and the expressions' queries and variables, which live as long as the model is in view. One for each
 * block, entity or item drawn with an animated model, kept on the client.
 */
public final class ModelAnimator {

    private final ModelPose pose;
    private final Map<String, ModelAnimation> animations = new LinkedHashMap<>();
    private final Map<String, AnimationController> controllers = new LinkedHashMap<>();
    private final List<IBoneMover> movers = new ArrayList<>();
    private final ExpressionContext context = new ExpressionContext();
    private double bornAt = Double.NaN;

    /** The default blend from one animation to the next, in seconds. */
    public static final double DEFAULT_TRANSITION = 0.2;

    /**
     * @param model      the model it moves
     * @param animations the animations it can play, its model's own and those of animation files beside it
     */
    public ModelAnimator(final ModelData model, final Collection<ModelAnimation> animations) {
        this.pose = new ModelPose(model);
        for (final ModelAnimation animation : model.animations()) {
            this.animations.put(animation.name(), animation);
        }
        for (final ModelAnimation animation : animations) {
            this.animations.put(animation.name(), animation);
        }
    }

    /** The pose it leaves the model in. */
    public ModelPose pose() {
        return this.pose;
    }

    /** The queries and variables its expressions read. */
    public ExpressionContext context() {
        return this.context;
    }

    /**
     * The animation named {@code name}: by its whole name, or by its last part, as {@code open} for
     * {@code animation.door.open}.
     */
    public Optional<ModelAnimation> animation(final String name) {
        final ModelAnimation exact = this.animations.get(name);
        if (exact != null) {
            return Optional.of(exact);
        }
        final String suffix = "." + name;
        for (final ModelAnimation animation : this.animations.values()) {
            if (animation.name().endsWith(suffix)) {
                return Optional.of(animation);
            }
        }
        return Optional.empty();
    }

    /** The names of the animations it can play. */
    public Collection<String> animationNames() {
        return this.animations.keySet();
    }

    /** The controller named {@code name}, made the first time it is asked for, after those there are. */
    public AnimationController controller(final String name) {
        return this.controllers.computeIfAbsent(name, AnimationController::new);
    }

    /** Its controllers, in their order. */
    public Collection<AnimationController> controllers() {
        return this.controllers.values();
    }

    /**
     * Plays the animation named {@code animation} on the controller named {@code controller}, blending to it.
     *
     * @return whether it has an animation of that name
     */
    public boolean play(final String controller, final String animation, final double now) {
        final Optional<ModelAnimation> found = animation(animation);
        found.ifPresent(chosen -> controller(controller).play(chosen, now, DEFAULT_TRANSITION));
        return found.isPresent();
    }

    /** Adds code that moves bones after the animations, every frame. */
    public ModelAnimator mover(final IBoneMover mover) {
        this.movers.add(mover);
        return this;
    }

    /**
     * Works out the pose at {@code now}, the model's clock in seconds: from rest, each controller's animation, then
     * the code's moves; telling {@code listener} of the events that have gone by.
     */
    public ModelPose animate(final double now, final IAnimationListener listener) {
        if (Double.isNaN(this.bornAt)) {
            this.bornAt = now;
        }
        this.context.lifeTime(now - this.bornAt);
        this.pose.reset();
        for (final AnimationController controller : this.controllers.values()) {
            controller.apply(this.pose, now, this.context, listener);
        }
        for (final IBoneMover mover : this.movers) {
            mover.move(this.pose, now);
        }
        return this.pose;
    }
}
