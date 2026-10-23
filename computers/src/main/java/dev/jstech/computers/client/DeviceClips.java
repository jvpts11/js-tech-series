/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.core.model.AnimationController;
import dev.jstech.core.model.ModelAnimation;
import dev.jstech.core.model.ModelAnimator;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;

/**
 * The clips of a device's model as its renderer chooses them every frame, from what the device is doing: each starts
 * at once, with no blend, as its file was cut to play, and a clip already playing goes on, so a fan that turns keeps
 * turning and a tray held open stays held. Each clip ends as its file says: round again, held on its last frame or
 * once.
 */
final class DeviceClips {

    private DeviceClips() {
    }

    /** Plays the animation {@code animation} on the controller {@code controller}; none stops it. */
    static void play(final ModelAnimator animator, final String controller, @Nullable final String animation,
                     final double seconds) {
        final AnimationController playing = animator.controller(controller);
        if (animation == null) {
            if (playing.playing() != null) {
                playing.stop(seconds, 0);
            }
            return;
        }
        final Optional<ModelAnimation> found = animator.animation(animation);
        if (found.isPresent()) {
            playing.play(found.get(), seconds, 0);
        }
    }
}
