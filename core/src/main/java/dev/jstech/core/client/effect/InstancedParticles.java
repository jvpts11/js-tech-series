/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.effect;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.jstech.core.client.instancing.InstanceData;
import dev.jstech.core.client.instancing.InstanceTexels;
import dev.jstech.core.client.instancing.Instancing;
import java.nio.FloatBuffer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL31;
import org.lwjgl.system.MemoryUtil;

/**
 * One layer of the effects' particles drawn many at once: one square on the graphics card, drawn once for each
 * particle written this frame, in the order written, so the see-through ones still lie back to front. Each particle
 * is four texels: where it is from the camera and half its size, its picture's corners on the sheet, its colour, and
 * its angle with its light. On the render thread.
 */
final class InstancedParticles {

    private final InstanceData data = new InstanceData();
    private int count;

    private static final InstanceTexels TEXELS = new InstanceTexels();
    private static final int TEXELS_EACH = 4;
    private static final float CHANNEL = 255.0F;
    private static final int LOW = 0xFFFF;
    private static final int HIGH = 16;
    /* The square's two triangles, corner by corner. */
    private static final float[] SQUARE = {-1, -1, -1, 1, 1, 1, -1, -1, 1, 1, 1, -1};
    private static final int CORNERS = SQUARE.length / 2;

    private static int square = -1;

    /** Writes a particle at {@code x, y, z} from the camera, {@code argb} its colour, {@code light} packed. */
    void add(final float x, final float y, final float z, final float half, final float u0, final float v0,
             final float u1, final float v1, final int argb, final float angle, final int light) {
        this.data.put(x, y, z, half);
        this.data.put(u0, v0, u1, v1);
        this.data.put((argb >> 16 & 0xFF) / CHANNEL, (argb >> 8 & 0xFF) / CHANNEL, (argb & 0xFF) / CHANNEL,
                (argb >>> 24) / CHANNEL);
        this.data.put(angle, light & LOW, light >>> HIGH, 0);
        this.count++;
    }

    /** Draws what was written, in the render states of {@code type}, and empties it; how many were drawn. */
    int draw(final RenderType type, final Camera camera) {
        final ShaderInstance shader = Instancing.particles();
        final int drawn = this.count;
        if (drawn > 0 && shader != null) {
            if (square < 0) {
                square = makeSquare();
            }
            type.setupRenderState();
            shader.setSampler("Sampler0", RenderSystem.getShaderTexture(0));
            shader.setSampler("Sampler2", RenderSystem.getShaderTexture(2));
            shader.setDefaultUniforms(VertexFormat.Mode.TRIANGLES, RenderSystem.getModelViewMatrix(),
                    RenderSystem.getProjectionMatrix(), Minecraft.getInstance().getWindow());
            shader.safeGetUniform("KitLeft").set(camera.getLeftVector());
            shader.safeGetUniform("KitUp").set(camera.getUpVector());
            shader.apply();
            final int most = Math.max(1, Instancing.mostTexels() / TEXELS_EACH);
            for (int first = 0; first < drawn; first += most) {
                final int some = Math.min(most, drawn - first);
                TEXELS.send(this.data, first * TEXELS_EACH, some * TEXELS_EACH);
                TEXELS.bind(shader);
                GlStateManager._glBindVertexArray(square);
                GL31.glDrawArraysInstanced(GL11.GL_TRIANGLES, 0, CORNERS, some);
            }
            GlStateManager._glBindVertexArray(0);
            // The game remembers which of its own it bound last; it binds its own again.
            BufferUploader.invalidate();
            TEXELS.unbind();
            shader.clear();
            type.clearRenderState();
        }
        this.data.clear();
        this.count = 0;
        return drawn;
    }

    /* The square every particle is drawn from, on the graphics card. */
    private static int makeSquare() {
        final int array = GlStateManager._glGenVertexArrays();
        GlStateManager._glBindVertexArray(array);
        final int buffer = GlStateManager._glGenBuffers();
        GlStateManager._glBindBuffer(GL15.GL_ARRAY_BUFFER, buffer);
        final FloatBuffer corners = MemoryUtil.memAllocFloat(SQUARE.length);
        try {
            corners.put(SQUARE).flip();
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, corners, GL15.GL_STATIC_DRAW);
        } finally {
            MemoryUtil.memFree(corners);
        }
        GlStateManager._enableVertexAttribArray(0);
        GlStateManager._vertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 2 * Float.BYTES, 0);
        GlStateManager._glBindVertexArray(0);
        GlStateManager._glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        BufferUploader.invalidate();
        return array;
    }
}
