/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.guide;

import com.mojang.blaze3d.platform.NativeImage;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.guide.GuideBlock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

/**
 * The faces of a block as a manual draws them: the picture on its top, its front and its side, read from the block's
 * model as the game has it, and the same face traced as a draughtsman would, its outline and every edge where one
 * shade meets another. A face is traced once and kept, since a texture does not change while a manual is open.
 */
final class GuideFaces {

    private final Map<ResourceLocation, List<Segment>> traced = new HashMap<>();

    /** How much two neighbouring pixels must differ in brightness, from 0 to 1, for a line to run between them. */
    private static final float EDGE = 0.09F;
    /** Pixels less opaque than this are the background a face's outline stands against. */
    private static final int SEEN = 128;
    private static final int PIXELS = 16;
    private static final int SEED = 42;

    /** The face of a block's item seen from that view, or null when the item is not a block or has no such face. */
    @Nullable
    TextureAtlasSprite face(final ItemStack stack, final GuideBlock.View view) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return null;
        }
        final BlockState state = blockItem.getBlock().defaultBlockState();
        final BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
        final Direction side = switch (view) {
            case TOP -> Direction.UP;
            case FRONT -> Direction.NORTH;
            case SIDE -> Direction.EAST;
        };
        final RandomSource random = RandomSource.create(SEED);
        for (final BakedQuad quad : model.getQuads(state, side, random, ModelData.EMPTY, null)) {
            return quad.getSprite();
        }
        for (final BakedQuad quad : model.getQuads(state, null, random, ModelData.EMPTY, null)) {
            if (quad.getDirection() == side) {
                return quad.getSprite();
            }
        }
        return model.getParticleIcon(ModelData.EMPTY);
    }

    /**
     * Draws a face {@code scale} times its size inside its outline: traced in {@code line} when asked, as it looks
     * otherwise.
     */
    void draw(final GuiGraphics g, final TextureAtlasSprite sprite, final int x, final int y, final int scale,
              final boolean trace, final int line, final int frame) {
        final int size = PIXELS * scale;
        if (trace) {
            for (final Segment segment : this.traced.computeIfAbsent(sprite.contents().name(),
                    name -> trace(sprite.contents()))) {
                final int sx = x + segment.x() * scale;
                final int sy = y + segment.y() * scale;
                if (segment.across()) {
                    g.fill(sx, sy, sx + scale + 1, sy + 1, line);
                } else {
                    g.fill(sx, sy, sx + 1, sy + scale + 1, line);
                }
            }
        } else {
            g.blit(x, y, 0, size, size, sprite);
        }
        Draw.outline(g, x, y, size + 1, size + 1, frame);
    }

    /** The lines of a face: between every two neighbouring pixels whose brightness differs enough. */
    private static List<Segment> trace(final SpriteContents contents) {
        final float[][] light = new float[PIXELS][PIXELS];
        try {
            final NativeImage image = contents.getOriginalImage();
            for (int y = 0; y < PIXELS; y++) {
                for (int x = 0; x < PIXELS; x++) {
                    final int abgr = image.getPixelRGBA(x * contents.width() / PIXELS, y * contents.height() / PIXELS);
                    final int sum = (abgr & 0xFF) + (abgr >> 8 & 0xFF) + (abgr >> 16 & 0xFF);
                    light[y][x] = (abgr >>> 24) < SEEN ? -1.0F : sum / 765.0F;
                }
            }
        } catch (final IllegalStateException freed) {
            // A texture whose pixels the game let go of is drawn as its outline alone.
            return List.of();
        }
        final List<Segment> segments = new ArrayList<>();
        for (int y = 0; y < PIXELS; y++) {
            for (int x = 0; x < PIXELS; x++) {
                if (x < PIXELS - 1 && Math.abs(light[y][x] - light[y][x + 1]) > EDGE) {
                    segments.add(new Segment(x + 1, y, false));
                }
                if (y < PIXELS - 1 && Math.abs(light[y][x] - light[y + 1][x]) > EDGE) {
                    segments.add(new Segment(x, y + 1, true));
                }
            }
        }
        return List.copyOf(segments);
    }

    /**
     * A line one pixel of the face long.
     *
     * @param x      where it starts across, in the face's pixels
     * @param y      where it starts down
     * @param across whether it runs across, under a pixel, or down, beside one
     */
    private record Segment(int x, int y, boolean across) {
    }
}
