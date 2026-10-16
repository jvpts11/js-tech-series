/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

/** Hears the events of the animations a model plays as their times pass: sounds, particle effects and words to code. */
@FunctionalInterface
public interface IAnimationListener {

    /** Hears nothing. */
    IAnimationListener NONE = (controller, animation, event) -> {
    };

    /**
     * An event's time has passed.
     *
     * @param controller the controller playing the animation
     * @param animation  the animation
     * @param event      the event
     */
    void event(String controller, ModelAnimation animation, ModelAnimation.Event event);
}
