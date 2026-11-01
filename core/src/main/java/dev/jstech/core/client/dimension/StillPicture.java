/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.dimension;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/**
 * The last picture the player's game drew before a passage between dimensions, kept to be shown still while the land
 * where they arrive loads. It is a copy of the whole frame as it stands when the passage opens, before anything of the
 * new level is drawn into it.
 */
final class StillPicture {

    private static @Nullable TextureTarget kept;

    private StillPicture() {
    }

    /** Keeps a copy of the frame the game drew last, in place of any kept before. */
    static void take() {
        final RenderTarget frame = Minecraft.getInstance().getMainRenderTarget();
        if (kept == null || kept.width != frame.width || kept.height != frame.height) {
            release();
            kept = new TextureTarget(frame.width, frame.height, false, Minecraft.ON_OSX);
        }
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, frame.frameBufferId);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, kept.frameBufferId);
        GlStateManager._glBlitFrameBuffer(0, 0, frame.width, frame.height, 0, 0, kept.width, kept.height,
                GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }

    /** Draws the picture kept over the whole of the frame just drawn; nothing when none is kept. */
    static void draw() {
        if (kept != null) {
            final RenderTarget frame = Minecraft.getInstance().getMainRenderTarget();
            frame.bindWrite(true);
            kept.blitToScreen(frame.width, frame.height);
        }
    }

    /** Lets the picture kept go. */
    static void release() {
        if (kept != null) {
            kept.destroyBuffers();
            kept = null;
        }
    }
}
