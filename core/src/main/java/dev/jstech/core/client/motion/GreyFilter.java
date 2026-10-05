/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.motion;

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
import dev.jstech.core.client.live.LiveGraphics;
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
 * Takes the colour out of a region of the game's window, part of the way or all of it: what a system did to the
 * screen behind the dialog that asked whether to turn the computer off. The region is copied as the screen has drawn
 * it and painted back through a shader, so it greys whatever is in it, whoever drew it.
 *
 * <p>Without the shader (a graphics card that would not build it) nothing is greyed; {@link #ready} says which, so
 * the caller can lay a veil instead.
 */
@EventBusSubscriber(modid = JsCore.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class GreyFilter {

    @Nullable
    private static ShaderInstance shader;
    /* The texture a region of the window is copied into before it is painted back, grown as needed, and its size. */
    private static int scratch = -1;
    private static int scratchWidth;
    private static int scratchHeight;

    private GreyFilter() {
    }

    @SubscribeEvent
    public static void onRegisterShaders(final RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
                ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "grey"), DefaultVertexFormat.POSITION_TEX),
                loaded -> shader = loaded);
    }

    /** Whether the shader has loaded, without which nothing is greyed. */
    public static boolean ready() {
        return shader != null;
    }

    /**
     * Greys that region of the game's window, in the units the screen is drawn in whatever the pose, by
     * {@code amount}: 0 leaves it as it is and 1 leaves nothing but its brightness. Everything drawn so far is laid
     * down first. A picture drawn for the world rather than the window is left as it is.
     */
    public static void filterScreen(final GuiGraphics graphics, final float x, final float y, final float width,
                                    final float height, final float amount) {
        final ShaderInstance grey = shader;
        if (grey == null || amount <= 0.0F || width <= 0 || height <= 0 || graphics instanceof LiveGraphics) {
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
        grey.safeGetUniform("Amount").set(Math.min(1.0F, amount));
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(() -> grey);
        RenderSystem.setShaderTexture(0, scratch);
        RenderSystem.disableBlend();
        // The region is already in the screen's units, so it is drawn with no pose of its own.
        final Matrix4f pose = new Matrix4f();
        final float x0 = (float) (left / scale);
        final float y0 = (float) (top / scale);
        final float x1 = (float) (right / scale);
        final float y1 = (float) (bottom / scale);
        final BufferBuilder quad = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_TEX);
        quad.addVertex(pose, x0, y0, 0.0F).setUv(0.0F, vTop);
        quad.addVertex(pose, x0, y1, 0.0F).setUv(0.0F, 0.0F);
        quad.addVertex(pose, x1, y1, 0.0F).setUv(u1, 0.0F);
        quad.addVertex(pose, x1, y0, 0.0F).setUv(u1, vTop);
        BufferUploader.drawWithShader(quad.buildOrThrow());
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
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
