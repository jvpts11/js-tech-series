/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.component;

import com.mojang.blaze3d.platform.NativeImage;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.Nullable;

/**
 * A map of the land round a place, in the game's own map colours and shading, north up: the places the owner marks
 * with their names, a grid every sixteen pixels, a scale bar, and buttons that bring it closer and take it further.
 * It is drawn from what the player's game has loaded, once a second or when it is moved, never every frame; land not
 * loaded is left dark.
 */
public final class MapView extends UiComponent {

    private final Supplier<BlockPos> centre;
    private final Supplier<List<Marker>> markers;
    private final String northLabel;
    private int blocksPerPixel = 1;
    @Nullable
    private DynamicTexture texture;
    @Nullable
    private ResourceLocation textureId;
    private BlockPos drawnAround = BlockPos.ZERO;
    private int drawnScale;
    private long drawnAt;
    private int drawnWidth;
    private int drawnHeight;

    private static final int LEAST_SCALE = 1;
    private static final int MOST_SCALE = 16;
    private static final long REDRAW_MILLIS = 1000;
    private static final int GRID = 16;
    private static final int BUTTON = 16;
    private static final int MARK = 3;
    private static final float SMALL = 0.75f;
    private static int counter;

    /**
     * @param centre     the place the map is centred on
     * @param markers    the places marked on it
     * @param northLabel the letter north is marked with
     */
    public MapView(final Supplier<BlockPos> centre, final Supplier<List<Marker>> markers, final String northLabel) {
        this.centre = Objects.requireNonNull(centre, "centre");
        this.markers = Objects.requireNonNull(markers, "markers");
        this.northLabel = Objects.requireNonNull(northLabel, "northLabel");
    }

    /** How many blocks each pixel of the map covers. */
    public int blocksPerPixel() {
        return blocksPerPixel;
    }

    @Override
    public void render(final GuiGraphics g, final UiContext ctx) {
        g.fill(x(), y(), right(), bottom(), ctx.skin().edge());
        final int w = width() - 2;
        final int h = height() - 2;
        if (w <= 0 || h <= 0) {
            return;
        }
        refresh(w, h, ctx.skin().fieldBg());
        if (textureId != null) {
            g.blit(textureId, x() + 1, y() + 1, 0, 0, w, h, w, h);
        }
        final int grid = ctx.skin().chartLine();
        for (int gx = x() + 1 + GRID / 2; gx < right() - 1; gx += GRID) {
            g.fill(gx, y() + 1, gx + 1, bottom() - 1, grid);
        }
        for (int gy = y() + 1 + GRID / 2; gy < bottom() - 1; gy += GRID) {
            g.fill(x() + 1, gy, right() - 1, gy + 1, grid);
        }
        final BlockPos here = centre.get();
        for (final Marker marker : markers.get()) {
            final int mx = x() + width() / 2 + (marker.pos().getX() - here.getX()) / blocksPerPixel;
            final int my = y() + height() / 2 + (marker.pos().getZ() - here.getZ()) / blocksPerPixel;
            if (mx < x() + MARK || mx >= right() - MARK || my < y() + MARK || my >= bottom() - MARK) {
                continue;
            }
            g.fill(mx - MARK, my - MARK, mx + MARK, my + MARK, ctx.skin().windowBg());
            g.fill(mx - MARK + 1, my - MARK + 1, mx + MARK - 1, my + MARK - 1, marker.colour());
            final int tw = Math.round(ctx.skin().textWidth(ctx.font(), marker.name()) * SMALL);
            ctx.skin().drawText(g, ctx.font(), marker.name(), mx - tw / 2, my + MARK + 1, ctx.skin().text(), SMALL);
        }
        ctx.skin().drawText(g, ctx.font(), northLabel, x() + width() / 2 - 2, y() + 3, ctx.skin().text(), SMALL);
        final int bx = right() - BUTTON - 3;
        ctx.skin().button(g, ctx.font(), bx, y() + 3, BUTTON, BUTTON, "+", ctx.over(bx, y() + 3, BUTTON, BUTTON),
                false, false);
        ctx.skin().button(g, ctx.font(), bx, y() + 5 + BUTTON, BUTTON, BUTTON, "-",
                ctx.over(bx, y() + 5 + BUTTON, BUTTON, BUTTON), false, false);
        scaleBar(g, ctx);
    }

    @Override
    public boolean mouseClicked(final double mx, final double my, final int button) {
        final int bx = right() - BUTTON - 3;
        if (mx >= bx && mx < bx + BUTTON) {
            if (my >= y() + 3 && my < y() + 3 + BUTTON) {
                blocksPerPixel = Math.max(LEAST_SCALE, blocksPerPixel / 2);
                return true;
            }
            if (my >= y() + 5 + BUTTON && my < y() + 5 + 2 * BUTTON) {
                blocksPerPixel = Math.min(MOST_SCALE, blocksPerPixel * 2);
                return true;
            }
        }
        return false;
    }

    @Override
    public void dispose() {
        if (texture != null) {
            Minecraft.getInstance().getTextureManager().release(textureId);
            texture = null;
            textureId = null;
        }
    }

    /*
     * The picture of the land, drawn again when it moved, changed scale or size, or a second went by; land the game
     * has not loaded is drawn in {@code unloaded}.
     */
    private void refresh(final int w, final int h, final int unloaded) {
        final ClientLevel level = Minecraft.getInstance().level;
        final BlockPos here = centre.get();
        final long now = System.currentTimeMillis();
        final boolean resized = texture == null || w != drawnWidth || h != drawnHeight;
        if (level == null || !resized && here.equals(drawnAround) && blocksPerPixel == drawnScale
                && now - drawnAt < REDRAW_MILLIS) {
            return;
        }
        if (resized) {
            dispose();
            texture = new DynamicTexture(w, h, false);
            textureId = Minecraft.getInstance().getTextureManager().register("jscore_map_" + nextId(), texture);
            drawnWidth = w;
            drawnHeight = h;
        }
        final NativeImage image = texture.getPixels();
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
        texture.upload();
        drawnAround = here;
        drawnScale = blocksPerPixel;
        drawnAt = now;
    }

    private void scaleBar(final GuiGraphics g, final UiContext ctx) {
        final int length = 32;
        final int bx = x() + 6;
        final int by = bottom() - 7;
        g.fill(bx, by, bx + length, by + 2, ctx.skin().text());
        g.fill(bx, by - 3, bx + 1, by + 2, ctx.skin().text());
        g.fill(bx + length - 1, by - 3, bx + length, by + 2, ctx.skin().text());
        ctx.skin().drawText(g, ctx.font(), length * blocksPerPixel + " m", bx + length + 3, by - 4,
                ctx.skin().text(), SMALL);
    }

    private static synchronized int nextId() {
        return counter++;
    }

    /* The game's images keep a colour as alpha, blue, green, red. */
    private static int abgr(final int argb) {
        return argb & (0xFF << 24 | 0xFF << 8) | (argb & 0xFF) << 16 | argb >> 16 & 0xFF;
    }

    /**
     * A place marked on a map.
     *
     * @param pos    where it is
     * @param name   what it is called
     * @param colour its mark's colour
     */
    public record Marker(BlockPos pos, String name, int colour) {
    }
}
