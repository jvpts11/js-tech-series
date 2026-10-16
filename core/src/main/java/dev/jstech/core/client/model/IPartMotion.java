/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;

/**
 * What draws the moving pieces of a kind of part each frame on the player's game, a fan's blades or a gauge's needle,
 * for a kind marked as moving; the rest of the part is its model, in the world's mesh. Handed to {@link PartMotions}.
 */
@FunctionalInterface
public interface IPartMotion {

    /**
     * Draws the part's moving pieces.
     *
     * @param pose    at the block's least corner, turned about its middle so the part faces north, as its model does
     * @param buffers what to draw into
     * @param busy    whether the part is at work
     * @param time    the game's time, in ticks and the part of a tick this frame is at
     * @param light   the light at the block
     */
    void draw(PoseStack pose, MultiBufferSource buffers, boolean busy, float time, int light);
}
