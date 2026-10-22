/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.camera;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** What the camera's keys are called in the game's controls. */
@TextHolder
public final class CameraTexts {

    public static final TextKey FREE_LOOK_KEY = TextKey.of("key.jscore.free_look", "Free Look");
    public static final TextKey KEEP_KEYFRAME_KEY = TextKey.of("key.jscore.keep_keyframe", "Keep a camera keyframe");

    private CameraTexts() {
    }
}
