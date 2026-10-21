/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.component;

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
import dev.jstech.core.client.live.RenderTargets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

/**
 * The parts of a screen that never change, drawn once into a picture the size of the game's window and laid down
 * again each frame instead of drawn again: a window's frame, its panels and wells, its slots' frames, its fixed words.
 * The picture is drawn again only when what it shows may have changed (the screen was built or laid out again) or the
 * window changed size. What moves is drawn over it, as before.
 */
public final class StillLayer {

    private @Nullable TextureTarget picture;
    private long drawnFor = Long.MIN_VALUE;
    private int paints;

    /* Whether every still part is drawn each frame as before, for a test to compare the two. */
    private static volatile boolean bypassed;

    /**
     * Lays down what {@code paint} draws, drawn into the picture again only when {@code revision} or the window
     * changed since it last was.
     */
    public void draw(final GuiGraphics g, final long revision, final Runnable paint) {
        // A picture drawn for something in the world has a target of its own, not the window's.
        if (g instanceof LiveGraphics || bypassed) {
            paint.run();
            return;
        }
        final RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
        final long key = revision * 31L * 31L + main.width * 31L + main.height;
        if (picture == null || picture.width != main.width || picture.height != main.height || drawnFor != key) {
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
            drawnFor = key;
            paints++;
        }
        layDown(g, picture);
    }

    /**
     * Draws every still part each frame straight onto the screen, as screens drew before, or goes back to the
     * pictures: for a test to compare what is seen either way.
     */
    public static void bypass(final boolean value) {
        bypassed = value;
    }

    /** How many times the picture was drawn; for a test to see that a still screen is drawn once. */
    public int paints() {
        return paints;
    }

    /** Lets the picture go, when its screen goes away. */
    public void release() {
        if (picture != null) {
            RenderTargets.release(picture);
            picture = null;
        }
        drawnFor = Long.MIN_VALUE;
    }

    /*
     * The picture over the whole window. What was drawn into it carries its colour already weighed by its own alpha,
     * so it is laid down as such: the colour as it is, what is behind kept by what the picture leaves uncovered.
     */
    private static void layDown(final GuiGraphics g, final TextureTarget from) {
        final float width = g.guiWidth();
        final float height = g.guiHeight();
        final Matrix4f pose = g.pose().last().pose();
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, from.getColorTextureId());
        final BufferBuilder quad = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_TEX);
        // A texture's first row is its bottom one, so the top of the window samples its last.
        quad.addVertex(pose, 0.0F, 0.0F, 0.0F).setUv(0.0F, 1.0F);
        quad.addVertex(pose, 0.0F, height, 0.0F).setUv(0.0F, 0.0F);
        quad.addVertex(pose, width, height, 0.0F).setUv(1.0F, 0.0F);
        quad.addVertex(pose, width, 0.0F, 0.0F).setUv(1.0F, 1.0F);
        BufferUploader.drawWithShader(quad.buildOrThrow());
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
    }

    private TextureTarget sized(final int width, final int height) {
        final TextureTarget held = picture;
        if (held != null && held.width == width && held.height == height) {
            return held;
        }
        if (held != null) {
            RenderTargets.release(held);
        }
        final TextureTarget made = RenderTargets.make(width, height);
        made.setFilterMode(GL11.GL_NEAREST);
        picture = made;
        return made;
    }
}
