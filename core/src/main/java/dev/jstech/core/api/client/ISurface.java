/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.api.client;

import org.jetbrains.annotations.ApiStatus;

/**
 * What a {@link SurfaceRenderer} draws its frames into: a rectangle of its own size, which the screen showing it
 * scales into whatever room it has.
 *
 * <p>There are two: a {@link PixelSurface}, an array of colours the renderer writes and the game uploads, which is
 * all a software renderer such as a game of the nineties needs; and a {@link GpuSurface}, a target on the graphics
 * card the renderer draws into with the game's own rendering. Neither is ever drawn by a server, and neither outlives
 * the window it is in.
 */
@ApiStatus.Experimental
public sealed interface ISurface permits PixelSurface, GpuSurface {

    /** How many pixels wide the surface is. */
    int width();

    /** How many pixels tall the surface is. */
    int height();
}
