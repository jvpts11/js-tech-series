/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.model.kit;

import net.minecraft.client.renderer.MultiBufferSource;

/**
 * Something a mod lays over a model of the Core's each time it is drawn: a glow, an overlay, an outline, a shield. It
 * draws after the model, in the model's own space (a sixteenth of a block, its origin where the model stands), and
 * redraws the model's faces into what it chooses through {@code faces}, or draws anything else there.
 *
 * <pre>{@code
 * KitLayers.add(rl("reactor"), (instance, faces, buffers, light, overlay) ->
 *         faces.into(texture -> RenderType.energySwirl(SHIELD, 0, 0), LightTexture.FULL_BRIGHT, overlay));
 * }</pre>
 */
@FunctionalInterface
public interface IKitLayer {

    /** Draws over {@code instance}'s model, which was drawn at {@code light}. */
    void draw(KitInstance instance, IKitFaces faces, MultiBufferSource buffers, int light, int overlay);
}
