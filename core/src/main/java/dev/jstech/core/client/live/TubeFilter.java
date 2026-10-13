/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.live;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.jstech.core.JsCore;
import dev.jstech.core.gui.Tube;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

/**
 * Paints a picture as a monitor's tube shows it, on the graphics card: a monochrome tube lights its one phosphor by
 * how bright each pixel reads, the sixteen-colour tube takes the nearest of its sixteen, and a colour tube shows the
 * picture as it is. The rule is {@link Tube#apply}'s, done for every pixel at once.
 *
 * <p>Used two ways: a screen on the player's monitor has its glass repainted once it is drawn, so everything on it
 * goes through the tube whoever drew it; and a picture drawn into a texture for the world is copied through it.
 */
@EventBusSubscriber(modid = JsCore.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class TubeFilter {

    private static final int MODE_COPY = 0;
    private static final int MODE_PHOSPHOR = 1;
    private static final int MODE_SIXTEEN = 2;

    @Nullable
    private static ShaderInstance shader;
    /* The texture a region of the window is copied into before it is painted back, grown as needed, and its size. */
    private static int scratch = -1;
    private static int scratchWidth;
    private static int scratchHeight;

    private TubeFilter() {
    }

    @SubscribeEvent
    public static void onRegisterShaders(final RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
                ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "tube"), DefaultVertexFormat.POSITION_TEX),
                loaded -> shader = loaded);
    }

    /** Whether the shader has loaded, without which nothing is filtered. */
    public static boolean ready() {
        return shader != null;
    }

    /**
     * Repaints that region of the game's window, in the units the screen is drawn in, as {@code tube} shows it.
     * Everything drawn so far is flushed first, so the region holds what the screen has drawn.
     */
    public static void filterScreen(final GuiGraphics graphics, final int x, final int y, final int width,
                                    final int height, final Tube tube) {
        if (!tube.filters() || shader == null || width <= 0 || height <= 0) {
            return;
        }
        graphics.flush();
        final Window window = Minecraft.getInstance().getWindow();
        final double scale = window.getGuiScale();
        final int left = Math.max(0, (int) Math.round(x * scale));
        final int right = Math.min(window.getWidth(), (int) Math.round((x + width) * scale));
        final int top = Math.max(0, (int) Math.round(y * scale));
        final int bottom = Math.min(window.getHeight(), (int) Math.round((y + height) * scale));
        final int pw = right - left;
        final int ph = bottom - top;
        if (pw <= 0 || ph <= 0) {
            return;
        }
        ensureScratch(pw, ph);
        GlStateManager._bindTexture(scratch);
        // The window's rows count up from its bottom edge.
        GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, left, window.getHeight() - bottom, pw, ph);
        final float u1 = (float) pw / scratchWidth;
        final float vTop = (float) ph / scratchHeight;
        RenderSystem.disableDepthTest();
        draw(scratch, graphics.pose().last().pose(), (float) (left / scale), (float) (top / scale),
                (float) (right / scale), (float) (bottom / scale), 0.0F, 0.0F, vTop, u1, 0.0F, tube);
        RenderSystem.enableDepthTest();
    }

    /**
     * Draws {@code texture} over the rectangle from {@code (x0, y0)} to {@code (x1, y1)} as {@code tube} shows it, with
     * {@code (u0, vTop)} at the rectangle's top left and {@code (u1, vBottom)} at its bottom right. The quad is opaque.
     */
    public static void draw(final int texture, final Matrix4f pose, final float x0, final float y0, final float x1,
                            final float y1, final float z, final float u0, final float vTop, final float u1,
                            final float vBottom, final Tube tube) {
        final ShaderInstance tubeShader = shader;
        if (tubeShader == null) {
            return;
        }
        final int glow = tube.glow();
        tubeShader.safeGetUniform("Phosphor").set(((glow >> 16) & 0xFF) / 255.0F, ((glow >> 8) & 0xFF) / 255.0F,
                (glow & 0xFF) / 255.0F);
        tubeShader.safeGetUniform("Mode").set(tube.monochrome() ? MODE_PHOSPHOR
                : tube == Tube.SIXTEEN ? MODE_SIXTEEN : MODE_COPY);
        RenderSystem.setShader(() -> tubeShader);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.disableBlend();
        final BufferBuilder quad = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_TEX);
        quad.addVertex(pose, x0, y0, z).setUv(u0, vTop);
        quad.addVertex(pose, x0, y1, z).setUv(u0, vBottom);
        quad.addVertex(pose, x1, y1, z).setUv(u1, vBottom);
        quad.addVertex(pose, x1, y0, z).setUv(u1, vTop);
        BufferUploader.drawWithShader(quad.buildOrThrow());
        RenderSystem.enableBlend();
    }

    private static void ensureScratch(final int width, final int height) {
        if (scratch != -1 && scratchWidth >= width && scratchHeight >= height) {
            return;
        }
        if (scratch == -1) {
            scratch = TextureUtil.generateTextureId();
        }
        scratchWidth = Math.max(width, scratchWidth);
        scratchHeight = Math.max(height, scratchHeight);
        TextureUtil.prepareImage(NativeImage.InternalGlFormat.RGBA, scratch, 0, scratchWidth, scratchHeight);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
    }
}
