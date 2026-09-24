/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio;

/**
 * Where a sound is heard from.
 *
 * <p>A sound of the world comes from a place: it is louder close to it, turns with the listener's head and fades
 * with distance, so its file is mono. A sound of the interface belongs to the player's own screen and sits nowhere
 * in the world, so it neither fades nor turns, and its file may be stereo.
 */
public enum SoundSpace {
    WORLD,
    INTERFACE
}
