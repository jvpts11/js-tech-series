/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.camera;

import java.util.Objects;
import net.minecraft.world.phys.Vec3;

/**
 * What a camera in the world sees, for a screen to show: where its lens is, which way it looks and how wide. A mod's
 * camera block says it, a screen that shows the camera draws it ({@code CameraFeeds} on the player's game), and two
 * screens showing the same camera share one view.
 *
 * @param at    where the lens is
 * @param yaw   which way it looks, in the game's degrees (0 south, 90 west)
 * @param pitch how far down it looks, in degrees
 * @param fov   how wide it sees, in degrees, from 10 to 110
 */
public record CameraFeed(Vec3 at, float yaw, float pitch, double fov) {

    /** The narrowest and the widest a camera sees, in degrees. */
    public static final double NARROWEST = 10.0;
    public static final double WIDEST = 110.0;

    public CameraFeed {
        Objects.requireNonNull(at, "at");
        if (!(fov >= NARROWEST && fov <= WIDEST)) {
            throw new IllegalArgumentException("a camera sees " + NARROWEST + " to " + WIDEST + " degrees wide, not "
                    + fov);
        }
    }
}
