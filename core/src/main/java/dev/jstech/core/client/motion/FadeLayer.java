/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.motion;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.jstech.core.client.live.LiveGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

/**
 * Draws a thing less than solid as one picture: first off the screen, into a layer the size of the game's window,
 * then laid over what is behind it at the strength asked for. A window drawn at half strength straight onto the
 * screen would show its own background through its text and its frame through its body; drawn this way it fades
 * the way a window on a real desktop does.
 *
 * <p>Only the game's own window is drawn through the layer. A picture drawn for something in the world, a monitor's
 * face, shows everything solid, as every motion there is shown where it ends.
 */
public final class FadeLayer {

    @Nullable
    private static TextureTarget layer;

    private FadeLayer() {
    }

    /**
     * Draws what {@code paint} draws, at {@code opacity} from 0 (nothing shows) to 1 (drawn as it is). Everything
     * drawn before is laid down first, so the layer goes over it.
     */
    public static void draw(final GuiGraphics g, final float opacity, final Runnable paint) {
        if (opacity >= 1.0F || g instanceof LiveGraphics) {
            paint.run();
            return;
        }
        if (opacity <= 0.0F) {
            return;
        }
        final RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
        g.flush();
        final TextureTarget into = sized(main.width, main.height);
        into.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        into.clear(Minecraft.ON_OSX);
        into.bindWrite(true);
        try {
            paint.run();
            g.flush();
        } finally {
            main.bindWrite(true);
        }
        layDown(g, into, opacity);
    }

    /*
     * The layer over the whole window. What was drawn into it carries its colour already weighed by its own alpha, so
     * it is laid down as such: the colour as it is, what is behind kept by what the layer leaves uncovered.
     */
    private static void layDown(final GuiGraphics g, final TextureTarget from, final float opacity) {
        final float width = g.guiWidth();
        final float height = g.guiHeight();
        final Matrix4f pose = new Matrix4f();
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, from.getColorTextureId());
        RenderSystem.setShaderColor(opacity, opacity, opacity, opacity);
        final BufferBuilder quad = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_TEX);
        // A texture's first row is its bottom one, so the top of the window samples its last.
        quad.addVertex(pose, 0.0F, 0.0F, 0.0F).setUv(0.0F, 1.0F);
        quad.addVertex(pose, 0.0F, height, 0.0F).setUv(0.0F, 0.0F);
        quad.addVertex(pose, width, height, 0.0F).setUv(1.0F, 0.0F);
        quad.addVertex(pose, width, 0.0F, 0.0F).setUv(1.0F, 1.0F);
        BufferUploader.drawWithShader(quad.buildOrThrow());
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
    }

    private static TextureTarget sized(final int width, final int height) {
        final TextureTarget held = layer;
        if (held != null && held.width == width && held.height == height) {
            return held;
        }
        if (held != null) {
            held.destroyBuffers();
        }
        final TextureTarget made = new TextureTarget(width, height, true, Minecraft.ON_OSX);
        made.setFilterMode(GL11.GL_NEAREST);
        layer = made;
        return made;
    }
}
