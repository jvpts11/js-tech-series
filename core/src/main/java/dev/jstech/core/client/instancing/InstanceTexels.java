/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.instancing;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.shaders.Uniform;
import net.minecraft.client.renderer.ShaderInstance;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;

/**
 * Numbers on the graphics card that a shader reads by index as {@code Instances}, a texture buffer of four numbers to
 * a texel: each frame's are sent over the last frame's, from an {@link InstanceData}. Made the first time it is sent
 * to, on the render thread.
 */
public final class InstanceTexels implements AutoCloseable {

    private int buffer = -1;
    private int texture = -1;
    /* The program last read from, and where its Instances are, so the place is asked once a program. */
    private int program = -1;
    private int location = -1;

    private static final String SAMPLER = "Instances";
    private static final int TEXEL_BYTES = 4 * Float.BYTES;

    /** Sends texels {@code first} to {@code first + count} of {@code data}, over what was sent before. */
    public void send(final InstanceData data, final int first, final int count) {
        if (this.buffer < 0) {
            make();
        }
        GlStateManager._glBindBuffer(GL31.GL_TEXTURE_BUFFER, this.buffer);
        GL15.nglBufferData(GL31.GL_TEXTURE_BUFFER, (long) count * TEXEL_BYTES, data.address(first),
                GL15.GL_STREAM_DRAW);
        GlStateManager._glBindBuffer(GL31.GL_TEXTURE_BUFFER, 0);
        // The texture is laid over the buffer again, for a driver that keeps the size its numbers had before.
        attach();
    }

    /** Lets {@code shader}, applied, read what was sent as its {@code Instances}. */
    public void bind(final ShaderInstance shader) {
        if (shader.getId() != this.program) {
            this.program = shader.getId();
            this.location = Uniform.glGetUniformLocation(this.program, SAMPLER);
        }
        Uniform.uploadInteger(this.location, Instancing.UNIT);
        on(this.texture);
    }

    /** Leaves the texture unit as it was. */
    public void unbind() {
        on(0);
    }

    @Override
    public void close() {
        if (this.texture >= 0) {
            GlStateManager._deleteTexture(this.texture);
            GlStateManager._glDeleteBuffers(this.buffer);
            this.texture = -1;
            this.buffer = -1;
        }
    }

    private void make() {
        this.buffer = GlStateManager._glGenBuffers();
        GlStateManager._glBindBuffer(GL31.GL_TEXTURE_BUFFER, this.buffer);
        GlStateManager._glBufferData(GL31.GL_TEXTURE_BUFFER, TEXEL_BYTES, GL15.GL_STREAM_DRAW);
        GlStateManager._glBindBuffer(GL31.GL_TEXTURE_BUFFER, 0);
        this.texture = GlStateManager._genTexture();
    }

    /* Lays the texture over the buffer, as four numbers to a texel, on the unit the shaders read it from. */
    private void attach() {
        final int active = GlStateManager._getActiveTexture();
        GlStateManager._activeTexture(GL13.GL_TEXTURE0 + Instancing.UNIT);
        GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, this.texture);
        GL31.glTexBuffer(GL31.GL_TEXTURE_BUFFER, GL30.GL_RGBA32F, this.buffer);
        GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, 0);
        GlStateManager._activeTexture(active);
    }

    /* Binds a texture buffer to the unit the shaders read it from, the active unit left as it was. */
    private static void on(final int texture) {
        final int active = GlStateManager._getActiveTexture();
        GlStateManager._activeTexture(GL13.GL_TEXTURE0 + Instancing.UNIT);
        GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, texture);
        GlStateManager._activeTexture(active);
    }
}
