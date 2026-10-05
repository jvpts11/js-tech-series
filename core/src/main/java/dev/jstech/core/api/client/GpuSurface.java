/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.api.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.ApiStatus;

/**
 * A surface on the graphics card: a render target of its own size a renderer draws into with the game's rendering.
 *
 * <p>While its renderer draws a frame the target is bound for writing, so everything it renders lands on the surface
 * and not on the game's window; the host binds the game's own target back afterwards. Its picture is then drawn on
 * the screen like any other. Named after the game's rendering rather than raw OpenGL on purpose: the versions of the
 * game after this one put rendering behind a layer of their own, and a renderer written against the game's targets
 * moves with it.
 */
@ApiStatus.Experimental
public final class GpuSurface implements ISurface {

    private final int width;
    private final int height;
    private RenderTarget target;
    private ResourceLocation location;

    /** How many surfaces have been given a texture, so each gets a name of its own. */
    private static int made;

    /**
     * A surface of that size, at least one pixel each way, black to begin with. Made by the host of a renderer, at the
     * size the renderer asks for; a renderer never makes its own. Its target is only made the first time it is asked
     * for, on the thread that draws.
     */
    public GpuSurface(final int width, final int height) {
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
    }

    @Override
    public int width() {
        return this.width;
    }

    @Override
    public int height() {
        return this.height;
    }

    /** The render target to draw into, made the first time it is asked for. */
    public RenderTarget target() {
        if (this.target == null) {
            this.target = new TextureTarget(this.width, this.height, true, Minecraft.ON_OSX);
            this.target.setClearColor(0, 0, 0, 1);
            this.target.clear(Minecraft.ON_OSX);
        }
        return this.target;
    }

    /** Draws what the target holds into that rectangle of the screen. */
    public void blit(final GuiGraphics g, final int x, final int y, final int w, final int h) {
        final RenderTarget drawn = this.target();
        if (this.location == null) {
            this.location = ResourceLocation.fromNamespaceAndPath("jscore", "dynamic/gpu_surface_" + made++);
            Minecraft.getInstance().getTextureManager().register(this.location, new TargetTexture(drawn));
        }
        // A render target's picture is stored bottom row first, so it is read upside down.
        g.blit(this.location, x, y, w, h, 0, this.height, this.width, -this.height, this.width, this.height);
    }

    /** Lets the target go, for a surface whose window has closed. */
    public void close() {
        if (this.location != null) {
            Minecraft.getInstance().getTextureManager().release(this.location);
            this.location = null;
        }
        if (this.target != null) {
            this.target.destroyBuffers();
            this.target = null;
        }
    }

    /* The target's colour texture under a name the screen can draw by; the target owns it, so it is not freed here. */
    private static final class TargetTexture extends AbstractTexture {

        private final RenderTarget target;

        TargetTexture(final RenderTarget target) {
            this.target = target;
        }

        @Override
        public int getId() {
            return this.target.getColorTextureId();
        }

        @Override
        public void load(final ResourceManager manager) {
            // Nothing to load: the picture is whatever the renderer drew.
        }

        @Override
        public void releaseId() {
            // The target frees its own texture when it is let go.
        }
    }
}
