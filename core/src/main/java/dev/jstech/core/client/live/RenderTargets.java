/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.live;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/**
 * Making a render target, clearing one or letting one go in the middle of a frame, without leaving the frame drawn
 * into the wrong place.
 *
 * <p>The game's targets bind the window's own framebuffer when they are made, cleared or let go, and do not put back
 * the one that was bound. A picture in the world forgotten at the start of a frame, or a surface made while a screen
 * drew, left the rest of that frame drawn into the window rather than into the game's target, which the game then
 * laid over it: the whole picture went wrong for one frame, the world seeming to flash. Each of these puts back the
 * framebuffer and the viewport that were bound before.
 */
public final class RenderTargets {

    private RenderTargets() {
    }

    /** A target of that size, with a depth buffer, as the game makes its own. */
    public static TextureTarget make(final int width, final int height) {
        final int[] kept = kept();
        try {
            return new TextureTarget(width, height, true, Minecraft.ON_OSX);
        } finally {
            restore(kept);
        }
    }

    /** Clears a target to its clear colour, and its depth. */
    public static void clear(final RenderTarget target) {
        final int[] kept = kept();
        try {
            target.clear(Minecraft.ON_OSX);
        } finally {
            restore(kept);
        }
    }

    /** Lets a target go, its framebuffer and its textures. */
    public static void release(final RenderTarget target) {
        final int[] kept = kept();
        try {
            target.destroyBuffers();
        } finally {
            restore(kept);
        }
    }

    /* The framebuffer bound, then the viewport's corner and size. */
    private static int[] kept() {
        final int[] viewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        return new int[] {GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING), viewport[0], viewport[1], viewport[2],
            viewport[3]};
    }

    private static void restore(final int[] kept) {
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, kept[0]);
        GlStateManager._viewport(kept[1], kept[2], kept[3], kept[4]);
    }
}
