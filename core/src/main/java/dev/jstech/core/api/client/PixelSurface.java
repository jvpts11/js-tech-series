/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.api.client;

import com.mojang.blaze3d.platform.NativeImage;
import java.util.Arrays;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

/**
 * A surface of pixels: an array of colours, one opaque {@code 0xAARRGGBB} each, row by row from the top left.
 *
 * <p>A renderer writes what it likes into it and the game uploads it to the graphics card once a frame, only when
 * something was written since; drawing it on the screen scales it by whole pixels where it can, so a picture made
 * pixel by pixel stays sharp. Made by the host of the renderer at the size the renderer asks for.
 */
@ApiStatus.Experimental
public final class PixelSurface implements ISurface {

    private final int width;
    private final int height;
    private final int[] pixels;
    private boolean changed = true;
    private DynamicTexture texture;
    private ResourceLocation location;

    /** How many surfaces have been given a texture, so each gets a name of its own. */
    private static int made;

    /**
     * A surface of that size, at least one pixel each way, every pixel 0 (transparent) to begin with. Made by the host
     * of a renderer, at the size the renderer asks for; a renderer never makes its own.
     */
    public PixelSurface(final int width, final int height) {
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
        this.pixels = new int[this.width * this.height];
    }

    @Override
    public int width() {
        return this.width;
    }

    @Override
    public int height() {
        return this.height;
    }

    /** Sets one pixel; a pixel off the surface is nothing. */
    public void set(final int x, final int y, final int argb) {
        if (x >= 0 && y >= 0 && x < this.width && y < this.height) {
            this.pixels[y * this.width + x] = argb;
            this.changed = true;
        }
    }

    /** What one pixel holds, or 0 off the surface. */
    public int get(final int x, final int y) {
        return x >= 0 && y >= 0 && x < this.width && y < this.height ? this.pixels[y * this.width + x] : 0;
    }

    /** Fills the whole surface with one colour. */
    public void fill(final int argb) {
        Arrays.fill(this.pixels, argb);
        this.changed = true;
    }

    /**
     * The pixels themselves, for a renderer that writes a whole frame at once; call {@link #changed()} once it has, so
     * the frame is uploaded.
     */
    public int[] pixels() {
        return this.pixels;
    }

    /** Says the pixels were written straight into {@link #pixels()}. */
    public void changed() {
        this.changed = true;
    }

    /** Draws the surface into that rectangle of the screen, uploading it first when it changed. */
    public void blit(final GuiGraphics g, final int x, final int y, final int w, final int h) {
        if (this.texture == null) {
            this.texture = new DynamicTexture(this.width, this.height, false);
            this.location = Minecraft.getInstance().getTextureManager()
                    .register("jscore_surface_" + made++, this.texture);
        }
        if (this.changed) {
            final NativeImage image = this.texture.getPixels();
            if (image != null) {
                for (int row = 0; row < this.height; row++) {
                    for (int column = 0; column < this.width; column++) {
                        image.setPixelRGBA(column, row, abgr(this.pixels[row * this.width + column]));
                    }
                }
                this.texture.upload();
            }
            this.changed = false;
        }
        g.blit(this.location, x, y, w, h, 0, 0, this.width, this.height, this.width, this.height);
    }

    /** Lets the texture go, for a surface whose window has closed. */
    public void close() {
        if (this.location != null) {
            Minecraft.getInstance().getTextureManager().release(this.location);
            this.location = null;
            this.texture = null;
        }
    }

    /* The game's images keep their colours as red in the lowest byte, the other way round from how they are written. */
    private static int abgr(final int argb) {
        final int alphaAndGreen = argb & (0xFF << 24 | 0xFF << 8);
        return alphaAndGreen | (argb & 0xFF) << 16 | argb >>> 16 & 0xFF;
    }
}
