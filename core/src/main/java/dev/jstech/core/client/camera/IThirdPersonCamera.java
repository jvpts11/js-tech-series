/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.camera;

import dev.jstech.core.camera.ThirdPersonView;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

/** A mod's camera in the third person: where it stands each frame, or none when it is not the one for this view. */
@FunctionalInterface
public interface IThirdPersonCamera {

    /**
     * Where the camera stands this frame, following {@code viewer}, from in front of them when {@code front}, or
     * none to leave the view to the next camera, and in the end to the game.
     */
    @Nullable
    ThirdPersonView view(Entity viewer, boolean front, float partialTick);
}
