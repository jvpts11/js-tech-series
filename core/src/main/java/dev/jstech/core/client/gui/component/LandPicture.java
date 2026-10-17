/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.component;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.Nullable;

/**
 * A picture of the land round a place from above, in the game's own map colours and shading, north up, for any map a
 * screen shows: drawn from what the player's game has loaded into a texture once a second, or when it moves, changes
 * scale or size, never every frame; land not loaded is left in the colour given. Disposed when the screen closes.
 */
public final class LandPicture {

    private @Nullable DynamicTexture texture;
    private @Nullable ResourceLocation textureId;
    private BlockPos drawnAround = BlockPos.ZERO;
    private int drawnScale;
    private long drawnAt;
    private int drawnWidth;
    private int drawnHeight;

    private static final long REDRAW_MILLIS = 1000;
    private static int counter;

    /**
     * The picture {@code w} by {@code h} pixels round {@code centre}, each pixel {@code blocksPerPixel} blocks a side,
     * drawn again when needed; none until the player's game has a level.
     */
    public @Nullable ResourceLocation around(final BlockPos centre, final int blocksPerPixel, final int w,
                                             final int h, final int unloaded) {
        refresh(centre, Math.max(1, blocksPerPixel), w, h, unloaded);
        return this.textureId;
    }

    /** Lets go of its texture. */
    public void dispose() {
        if (this.texture != null) {
            Minecraft.getInstance().getTextureManager().release(this.textureId);
            this.texture = null;
            this.textureId = null;
        }
    }

    private void refresh(final BlockPos here, final int blocksPerPixel, final int w, final int h,
                         final int unloaded) {
        final ClientLevel level = Minecraft.getInstance().level;
        final long now = System.currentTimeMillis();
        final boolean resized = this.texture == null || w != this.drawnWidth || h != this.drawnHeight;
        if (level == null || w <= 0 || h <= 0 || !resized && here.equals(this.drawnAround)
                && blocksPerPixel == this.drawnScale && now - this.drawnAt < REDRAW_MILLIS) {
            return;
        }
        if (resized) {
            dispose();
            this.texture = new DynamicTexture(w, h, false);
            this.textureId = Minecraft.getInstance().getTextureManager().register("jscore_land_" + nextId(),
                    this.texture);
            this.drawnWidth = w;
            this.drawnHeight = h;
        }
        final NativeImage image = this.texture.getPixels();
        if (image == null) {
            return;
        }
        final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int px = 0; px < w; px++) {
            final int bx = here.getX() + (px - w / 2) * blocksPerPixel;
            int northHeight = Integer.MIN_VALUE;
            for (int py = -1; py < h; py++) {
                final int bz = here.getZ() + (py - h / 2) * blocksPerPixel;
                if (!level.hasChunk(bx >> 4, bz >> 4)) {
                    if (py >= 0) {
                        image.setPixelRGBA(px, py, abgr(unloaded));
                    }
                    northHeight = Integer.MIN_VALUE;
                    continue;
                }
                final int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, bx, bz) - 1;
                if (py >= 0) {
                    at.set(bx, top, bz);
                    final BlockState state = level.getBlockState(at);
                    final MapColor colour = state.getMapColor(level, at);
                    final MapColor.Brightness shade = northHeight == Integer.MIN_VALUE || top == northHeight
                            ? MapColor.Brightness.NORMAL : top > northHeight ? MapColor.Brightness.HIGH
                            : MapColor.Brightness.LOW;
                    // The game's map colours come already in the order its images keep them.
                    image.setPixelRGBA(px, py, colour == MapColor.NONE ? abgr(unloaded)
                            : colour.calculateRGBColor(shade));
                }
                northHeight = top;
            }
        }
        this.texture.upload();
        this.drawnAround = here;
        this.drawnScale = blocksPerPixel;
        this.drawnAt = now;
    }

    private static synchronized int nextId() {
        return counter++;
    }

    /* The game's images keep a colour as alpha, blue, green, red. */
    private static int abgr(final int argb) {
        return argb & (0xFF << 24 | 0xFF << 8) | (argb & 0xFF) << 16 | argb >> 16 & 0xFF;
    }
}
