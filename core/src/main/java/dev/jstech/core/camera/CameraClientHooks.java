/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.camera;

import java.util.Optional;

/**
 * What a player's game does when the server starts or ends a view from elsewhere: only a player's game has a camera,
 * so it hands its answer in here as it starts, and a dedicated server keeps this one, which does nothing.
 */
public final class CameraClientHooks {

    /** What a game with no camera answers. */
    private static final IHooks NONE = new IHooks() {
    };
    private static volatile IHooks hooks = NONE;

    private CameraClientHooks() {
    }

    /** The answers of this game. */
    public static IHooks get() {
        return hooks;
    }

    /** Hands in a player's game's answers, once, as it starts. */
    public static void set(final IHooks from) {
        hooks = from;
    }

    /** What a player's game answers. */
    public interface IHooks {

        /** The server started {@code view}, or ended the one showing when it is empty. */
        default void view(final Optional<CameraView> view) {
        }
    }
}
