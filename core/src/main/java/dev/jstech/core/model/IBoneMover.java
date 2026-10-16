/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

/**
 * Code that moves a model's bones after its animations, every frame it is drawn: a fan turned as fast as its machine
 * runs, a gauge's needle at its reading, a head turned to look at the player. It works on the pose the animations
 * left, adding to it or setting it.
 */
@FunctionalInterface
public interface IBoneMover {

    /**
     * Moves the bones of {@code pose}.
     *
     * @param pose    the pose, as the animations left it
     * @param seconds the model's clock, in seconds
     */
    void move(ModelPose pose, double seconds);
}
