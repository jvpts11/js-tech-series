/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.physics;

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
import dev.jstech.core.physics.Laws;
import dev.jstech.core.physics.Physics;
import dev.jstech.core.physics.PhysicsArea;
import dev.jstech.core.physics.Pull;
import java.io.IOException;
import java.io.UncheckedIOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;

/**
 * What the laws of light do to what the player sees: the colours of a place with its own light tinted and shifted
 * towards red or blue, its view shimmering, darkened or lit with nothing to make it so; and the view bent round a mass
 * the player looks at, its middle a black horizon. Drawn over the world by a shader before the HUD; on the player's
 * game only.
 */
public final class LightLawsClient {

    private static final ResourceLocation SHADER = ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "light_law");
    /* How far a mass bends the view of a player looking at it, in blocks. */
    private static final double LENS_REACH = 96.0;
    private static final float TIME_SCALE = 1.0F / 20.0F;
    private static final float CHANNEL = 255F;
    private static @Nullable ShaderInstance shader;
    private static int scratch = -1;
    private static int scratchWidth;
    private static int scratchHeight;
    private static Matrix4f view = new Matrix4f();
    private static Matrix4f projection = new Matrix4f();
    private static Vec3 eye = Vec3.ZERO;

    private LightLawsClient() {
    }

    /** Hands the buses what this answers, once, from the Core's client. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterShadersEvent.class, event -> {
            try {
                event.registerShader(new ShaderInstance(event.getResourceProvider(), SHADER,
                        DefaultVertexFormat.POSITION_TEX), loaded -> shader = loaded);
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        NeoForge.EVENT_BUS.addListener(RenderLevelStageEvent.class, event -> {
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_WEATHER) {
                view = new Matrix4f(event.getModelViewMatrix());
                projection = new Matrix4f(event.getProjectionMatrix());
                eye = event.getCamera().getPosition();
            }
        });
        NeoForge.EVENT_BUS.addListener(RenderGuiEvent.Pre.class, event -> draw(event.getGuiGraphics()));
    }

    /** The lens of the mass the player looks at now: x, y on the screen, its radius, its strength; none for none. */
    public static float @Nullable [] lens() {
        final ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return null;
        }
        float[] best = null;
        double nearest = LENS_REACH;
        for (final PhysicsArea area : Physics.areas(level)) {
            final double lens = area.laws().light().map(Laws.Light::lens).orElse(0.0);
            if (lens <= 0 || !(area.laws().gravity().orElse(null) instanceof Pull.Centre centre)) {
                continue;
            }
            final Vec3 at = new Vec3(centre.x(), centre.y(), centre.z());
            final double away = at.distanceTo(eye);
            final float[] screen = onScreen(at);
            if (screen != null && away < nearest && away > 0) {
                nearest = away;
                final float radius = (float) (centre.radius() * projection.m11() / away / 2);
                best = new float[] {screen[0], screen[1], radius, (float) lens};
            }
        }
        return best;
    }

    private static void draw(final GuiGraphics graphics) {
        final ClientLevel level = Minecraft.getInstance().level;
        final ShaderInstance law = shader;
        if (level == null || law == null) {
            return;
        }
        final Laws.Light light = Physics.lawsAt(level, eye).light().orElse(null);
        final float[] lens = lens();
        if (light == null && lens == null) {
            return;
        }
        graphics.flush();
        final Window window = Minecraft.getInstance().getWindow();
        final int width = window.getWidth();
        final int height = window.getHeight();
        ensureScratch(width, height);
        GlStateManager._bindTexture(scratch);
        GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, width, height);
        final float u1 = (float) width / scratchWidth;
        final float v1 = (float) height / scratchHeight;
        law.safeGetUniform("Time").set(level.getGameTime() * TIME_SCALE);
        law.safeGetUniform("Aspect").set((float) width / height);
        law.safeGetUniform("Extent").set(u1, v1);
        final int tint = light == null ? 0xFFFFFF : light.tint();
        law.safeGetUniform("Tint").set((tint >> 16 & 0xFF) / CHANNEL, (tint >> 8 & 0xFF) / CHANNEL,
                (tint & 0xFF) / CHANNEL);
        law.safeGetUniform("Shift").set(light == null ? 0F : (float) light.shift());
        law.safeGetUniform("Mirage").set(light == null ? 0F : (float) light.mirage());
        law.safeGetUniform("Dark").set(light == null ? 0F : (float) light.dark());
        law.safeGetUniform("Glow").set(light == null ? 0F : (float) light.glow());
        final float[] bent = lens == null ? new float[4] : lens;
        law.safeGetUniform("Lens").set(bent[0], bent[1], bent[2], bent[3]);
        RenderSystem.disableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.setShader(() -> law);
        RenderSystem.setShaderTexture(0, scratch);
        final float w = graphics.guiWidth();
        final float h = graphics.guiHeight();
        final Matrix4f pose = new Matrix4f();
        final BufferBuilder quad = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_TEX);
        // The window's rows count up from its bottom edge, the screen's down from its top.
        quad.addVertex(pose, 0, 0, 0).setUv(0, v1);
        quad.addVertex(pose, 0, h, 0).setUv(0, 0);
        quad.addVertex(pose, w, h, 0).setUv(u1, 0);
        quad.addVertex(pose, w, 0, 0).setUv(u1, v1);
        BufferUploader.drawWithShader(quad.buildOrThrow());
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
    }

    /* Where {@code at} was on the screen in the last frame, from 0 to 1 across and up; none behind the eye. */
    private static float @Nullable [] onScreen(final Vec3 at) {
        final Vector4f clip = new Vector4f((float) (at.x - eye.x), (float) (at.y - eye.y), (float) (at.z - eye.z),
                1.0F);
        view.transform(clip);
        projection.transform(clip);
        if (clip.w <= 0) {
            return null;
        }
        return new float[] {clip.x / clip.w * 0.5F + 0.5F, clip.y / clip.w * 0.5F + 0.5F};
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
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
    }
}
