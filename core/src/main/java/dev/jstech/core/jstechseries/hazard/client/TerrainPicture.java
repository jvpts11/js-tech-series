/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.hazard.client;

import com.mojang.blaze3d.platform.NativeImage;
import dev.jstech.core.JsCore;
import java.util.Arrays;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

/**
 * The land round a chunk as the game's maps draw it, a square of chunks wide, for a map of hazards to lay its readings
 * over: each column the colour of what tops it, lit or shaded by how it stands against the one north of it, water
 * darker the deeper it is, and grey where this game has no chunk. Drawn into a texture of its own a few chunks a frame,
 * so a map never stops the game; as its centre moves the picture slides with it and only the chunks it uncovers are
 * drawn again.
 */
public final class TerrainPicture implements AutoCloseable {

    private final int chunks;
    private final int pixelsPerChunk;
    private final int size;
    private final DynamicTexture texture;
    private final ResourceLocation location;
    private final boolean[] drawn;
    private int originX = Integer.MIN_VALUE;
    private int originZ = Integer.MIN_VALUE;
    private int next;

    /** Grey, where this game has no chunk to draw: the colour as the game's textures keep it, its red last. */
    private static final int UNKNOWN = 0xFF << 24 | 0x505050;
    /* How many blocks down a column is looked into for something with a colour, past glass and air. */
    private static final int DEEPEST = 32;
    private static int made;

    /**
     * A picture {@code chunks} wide, each chunk {@code pixelsPerChunk} pixels: 16 for a pixel a block, 8 for a pixel
     * every two.
     */
    public TerrainPicture(final int chunks, final int pixelsPerChunk) {
        this.chunks = chunks;
        this.pixelsPerChunk = pixelsPerChunk;
        this.size = chunks * pixelsPerChunk;
        this.texture = new DynamicTexture(this.size, this.size, true);
        this.drawn = new boolean[chunks * chunks];
        this.location = Minecraft.getInstance().getTextureManager().register(JsCore.MODID + "_terrain_" + made++,
                this.texture);
    }

    /** How many chunks wide it is. */
    public int chunks() {
        return this.chunks;
    }

    /** The chunk at its top left corner. */
    public ChunkPos origin() {
        return new ChunkPos(this.originX, this.originZ);
    }

    /**
     * Centres it on {@code centre} and draws up to {@code budget} chunks it has not drawn yet, then sends what changed
     * to the graphics card. Whether every chunk is drawn.
     */
    public boolean update(final ChunkPos centre, final int budget) {
        final ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return false;
        }
        recentre(centre.x - this.chunks / 2, centre.z - this.chunks / 2);
        int left = budget;
        boolean changed = false;
        for (int tried = 0; tried < this.drawn.length && left > 0; tried++) {
            final int slot = this.next;
            this.next = (this.next + 1) % this.drawn.length;
            if (!this.drawn[slot]) {
                this.drawn[slot] = drawChunk(level, slot % this.chunks, slot / this.chunks);
                changed = true;
                left--;
            }
        }
        if (changed) {
            this.texture.upload();
        }
        for (final boolean done : this.drawn) {
            if (!done) {
                return false;
            }
        }
        return true;
    }

    /**
     * Draws the part of it from chunk {@code (fromX, fromZ)}, {@code across} by {@code down} chunks, with its top left
     * at {@code (x, y)}, {@code cell} pixels a chunk on the screen.
     */
    public void draw(final GuiGraphics g, final int x, final int y, final float cell, final double fromX,
                     final double fromZ, final double across, final double down) {
        final float u = (float) ((fromX - this.originX) * this.pixelsPerChunk);
        final float v = (float) ((fromZ - this.originZ) * this.pixelsPerChunk);
        final int w = (int) Math.round(across * cell);
        final int h = (int) Math.round(down * cell);
        g.blit(this.location, x, y, w, h, u, v, (int) Math.round(across * this.pixelsPerChunk),
                (int) Math.round(down * this.pixelsPerChunk), this.size, this.size);
    }

    @Override
    public void close() {
        Minecraft.getInstance().getTextureManager().release(this.location);
    }

    /* Slides the picture so its top left is chunk (x, z), keeping what it had drawn of the chunks still in it. */
    private void recentre(final int x, final int z) {
        if (x == this.originX && z == this.originZ) {
            return;
        }
        final NativeImage pixels = this.texture.getPixels();
        final boolean first = this.originX == Integer.MIN_VALUE || pixels == null
                || Math.abs(x - this.originX) >= this.chunks || Math.abs(z - this.originZ) >= this.chunks;
        if (first) {
            Arrays.fill(this.drawn, false);
        } else {
            shift(pixels, this.originX - x, this.originZ - z);
        }
        this.originX = x;
        this.originZ = z;
    }

    private void shift(final NativeImage pixels, final int byX, final int byZ) {
        final int[] copy = new int[this.size * this.size];
        for (int py = 0; py < this.size; py++) {
            for (int px = 0; px < this.size; px++) {
                copy[py * this.size + px] = pixels.getPixelRGBA(px, py);
            }
        }
        final boolean[] was = this.drawn.clone();
        Arrays.fill(this.drawn, false);
        for (int cz = 0; cz < this.chunks; cz++) {
            for (int cx = 0; cx < this.chunks; cx++) {
                final int fromX = cx - byX;
                final int fromZ = cz - byZ;
                if (fromX < 0 || fromZ < 0 || fromX >= this.chunks || fromZ >= this.chunks
                        || !was[fromZ * this.chunks + fromX]) {
                    continue;
                }
                for (int py = 0; py < this.pixelsPerChunk; py++) {
                    for (int px = 0; px < this.pixelsPerChunk; px++) {
                        pixels.setPixelRGBA(cx * this.pixelsPerChunk + px, cz * this.pixelsPerChunk + py,
                                copy[(fromZ * this.pixelsPerChunk + py) * this.size + fromX * this.pixelsPerChunk
                                        + px]);
                    }
                }
                this.drawn[cz * this.chunks + cx] = true;
            }
        }
    }

    /* Draws one chunk of the picture; whether the game had it to draw, so a chunk still loading is tried again. */
    private boolean drawChunk(final ClientLevel level, final int cx, final int cz) {
        final NativeImage pixels = this.texture.getPixels();
        if (pixels == null) {
            return false;
        }
        final int chunkX = this.originX + cx;
        final int chunkZ = this.originZ + cz;
        final boolean known = level.getChunkSource().getChunk(chunkX, chunkZ, ChunkStatus.FULL, false) != null;
        final int step = 16 / this.pixelsPerChunk;
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int pz = 0; pz < this.pixelsPerChunk; pz++) {
            for (int px = 0; px < this.pixelsPerChunk; px++) {
                final int colour = known ? column(level, pos, chunkX * 16 + px * step, chunkZ * 16 + pz * step, step)
                        : UNKNOWN;
                pixels.setPixelRGBA(cx * this.pixelsPerChunk + px, cz * this.pixelsPerChunk + pz, colour);
            }
        }
        return known;
    }

    /* The colour of the column at (x, z), as the game's maps draw it, its red last. */
    private static int column(final ClientLevel level, final BlockPos.MutableBlockPos pos, final int x, final int z,
                              final int step) {
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
        BlockState state = level.getBlockState(pos.set(x, y, z));
        for (int down = 0; down < DEEPEST && y > level.getMinBuildHeight()
                && state.getMapColor(level, pos) == MapColor.NONE; down++) {
            state = level.getBlockState(pos.set(x, --y, z));
        }
        final MapColor colour = state.getMapColor(level, pos);
        if (colour == MapColor.WATER) {
            int depth = 0;
            while (depth < DEEPEST && y - depth > level.getMinBuildHeight()
                    && !level.getFluidState(pos.set(x, y - depth - 1, z)).isEmpty()) {
                depth++;
            }
            final MapColor.Brightness deep = depth > 9 ? MapColor.Brightness.LOWEST : depth > 4
                    ? MapColor.Brightness.LOW : MapColor.Brightness.NORMAL;
            return colour.calculateRGBColor(deep);
        }
        final int north = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z - step) - 1;
        final MapColor.Brightness light = y > north ? MapColor.Brightness.HIGH : y < north
                ? MapColor.Brightness.LOW : MapColor.Brightness.NORMAL;
        return colour.calculateRGBColor(light);
    }
}
