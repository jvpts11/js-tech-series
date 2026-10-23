/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.model.kit;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;

/**
 * What a block entity's renderer draws besides its model, every frame it is drawn: the parts a machine holds, each a
 * model of its own on the seat its case marks for it; the disc on a drive's tray, at the bone that rides out with the
 * tray. It draws after the model, where the model stands: in blocks, turned as the block faces, so
 * {@link KitRenderer#toBone} brings it onto any of the model's bones as the frame has them.
 *
 * <pre>{@code
 * .extras((drive, instance, poses, buffers, light, overlay, partialTick) -> {
 *     poses.pushPose();
 *     if (KitRenderer.toBone(instance, "medium", poses)) {
 *         Minecraft.getInstance().getItemRenderer().renderStatic(drive.disc(), ItemDisplayContext.NONE, light,
 *                 overlay, poses, buffers, drive.getLevel(), 0);
 *     }
 *     poses.popPose();
 * })
 * }</pre>
 *
 * @param <T> what is drawn
 */
@FunctionalInterface
public interface IKitExtras<T> {

    /**
     * Draws more of {@code thing}, whose model was just drawn as {@code instance} has it this frame.
     *
     * @param light the light the model was drawn at
     */
    void draw(T thing, KitInstance instance, PoseStack poses, MultiBufferSource buffers, int light, int overlay,
              float partialTick);
}
