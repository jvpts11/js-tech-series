/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.client.guide;

import dev.jstech.core.api.client.IGuideBlockRenderer;
import dev.jstech.core.text.GameText;
import dev.jstech.industrial.guide.PlantDrawingTexts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

/**
 * A geological section, the ground as if cut through with a knife: its columns of layers from the surface down, the
 * veins of ore in them, the faults where the ground broke and slid, and the boreholes that prove it. Each rock is drawn
 * in the hatching geologists give its kind or, in colours, in the texture of its own block; or, for a legend, the rocks
 * a row each with their blocks' names.
 */
public final class GeologyRenderer implements IGuideBlockRenderer {

    /** How large a block's texture is drawn when a rock is shown in its colours. */
    private static final int TILE = 8;
    /** A legend's swatch of a rock, and the row it stands in. */
    private static final int SWATCH_WIDTH = 18;
    private static final int SWATCH_HEIGHT = 10;
    private static final int SWATCH_GAP = 3;
    private static final int ROCK_ROW = 15;
    /** Where a rock's name starts in the legend, past its two swatches. */
    private static final int NAME_X = 48;
    /** The radius of a borehole's head, and how far over it its tag stands. */
    private static final int HEAD = 2;
    private static final int TAG_OVER = 15;

    @Override
    public void draw(final GuiGraphics graphics, final Font font, final int x, final int y, final int width,
                     final int height, final CompoundTag data, final int mouseX, final int mouseY) {
        final DrawingPen pen = new DrawingPen(graphics, font, x, y);
        final boolean colours = data.getBoolean("colours");
        if (data.contains("rocks")) {
            legend(pen, data.getList("rocks", Tag.TAG_COMPOUND));
            return;
        }
        int left = Integer.MAX_VALUE;
        int right = Integer.MIN_VALUE;
        int top = Integer.MAX_VALUE;
        int bottom = Integer.MIN_VALUE;
        for (final Tag each : data.getList("columns", Tag.TAG_COMPOUND)) {
            final CompoundTag column = (CompoundTag) each;
            final int x0 = column.getInt("x0");
            final int x1 = column.getInt("x1");
            int layerY = column.getInt("top");
            top = Math.min(top, layerY);
            left = Math.min(left, x0);
            right = Math.max(right, x1);
            for (final Tag eachLayer : column.getList("layers", Tag.TAG_COMPOUND)) {
                final CompoundTag layer = (CompoundTag) eachLayer;
                final int depth = layer.getInt("depth");
                rock(pen, layer.getString("block"), layer.getString("pattern"), x0, layerY, x1, layerY + depth,
                        colours);
                layerY += depth;
                if (!colours) {
                    pen.line(x0, layerY, x1, layerY, false, pen.ink());
                }
            }
            bottom = Math.max(bottom, layerY);
        }
        for (final Tag each : data.getList("veins", Tag.TAG_COMPOUND)) {
            final CompoundTag vein = (CompoundTag) each;
            rock(pen, vein.getString("block"), "ore", vein.getInt("x0"), vein.getInt("y0"), vein.getInt("x1"),
                    vein.getInt("y1"), colours);
        }
        for (final Tag each : data.getList("faults", Tag.TAG_COMPOUND)) {
            final CompoundTag fault = (CompoundTag) each;
            pen.line(fault.getInt("x0"), fault.getInt("y0"), fault.getInt("x1"), fault.getInt("y1"), true,
                    colours ? pen.paper() : pen.ink());
        }
        if (left <= right) {
            pen.frame(left, top, right, bottom, pen.ink());
        }
        for (final Tag each : data.getList("boreholes", Tag.TAG_COMPOUND)) {
            final CompoundTag hole = (CompoundTag) each;
            final int hx = hole.getInt("x");
            final int ht = hole.getInt("y");
            pen.line(hx, ht - HEAD - 2, hx, ht + hole.getInt("depth"), false, colours ? pen.paper() : pen.ink());
            pen.circle(hx, ht - HEAD - 4, HEAD, pen.ink(), false);
            pen.smallCentred(DrawingPen.words(hole, "tag"), hx, ht - TAG_OVER, pen.faint());
        }
    }

    /* A rock filling its place: its hatching, or its block's texture in its colours. */
    private static void rock(final DrawingPen pen, final String block, final String pattern, final int x0,
                             final int y0, final int x1, final int y1, final boolean colours) {
        if (colours) {
            textured(pen, block, x0, y0, x1, y1);
        } else {
            hatched(pen, pattern, x0, y0, x1, y1);
        }
    }

    /* The block's own texture tiled over the place, cut to it. */
    private static void textured(final DrawingPen pen, final String block, final int x0, final int y0, final int x1,
                                 final int y1) {
        final ResourceLocation id = ResourceLocation.tryParse(block);
        if (id == null || x1 <= x0 || y1 <= y0) {
            return;
        }
        final Block found = BuiltInRegistries.BLOCK.get(id);
        final TextureAtlasSprite sprite = Minecraft.getInstance().getBlockRenderer().getBlockModelShaper()
                .getParticleIcon(found.defaultBlockState());
        final GuiGraphics graphics = pen.graphics();
        final int ox = pen.originX();
        final int oy = pen.originY();
        pen.clip(x0, y0, x1, y1);
        for (int ty = y0; ty < y1; ty += TILE) {
            for (int tx = x0; tx < x1; tx += TILE) {
                graphics.blit(ox + tx, oy + ty, 0, TILE, TILE, sprite);
            }
        }
        pen.unclip();
    }

    /* The place in the hatching geologists draw its kind of rock with. */
    private static void hatched(final DrawingPen pen, final String pattern, final int x0, final int y0, final int x1,
                                final int y1) {
        for (int y = y0; y < y1; y++) {
            for (int x = x0; x < x1; x++) {
                final boolean lit = switch (pattern) {
                    case "soil" -> (x * 7 + y * 3) % 11 == 0;
                    case "sandstone" -> (x + 2 * y) % 5 == 0 && y % 2 == 0;
                    case "stone" -> y % 4 == 0 && (x + y) % 6 < 3;
                    case "granite" -> ((x + y) % 6 == 0 || (x - y) % 6 == 0) && (x / 3 + y / 3) % 2 == 0;
                    case "deepslate" -> (y + (x / 3) % 2) % 4 == 0;
                    case "ore" -> (x + y) % 3 != 0;
                    default -> false;
                };
                if (lit) {
                    pen.dot(x, y, "ore".equals(pattern) ? pen.accent() : pen.ink());
                }
            }
        }
    }

    /* Each rock in its hatching and in its colours side by side, with its block's name; then the fault. */
    private static void legend(final DrawingPen pen, final ListTag rocks) {
        int rowY = 0;
        for (final Tag each : rocks) {
            final CompoundTag rock = (CompoundTag) each;
            final String block = rock.getString("block");
            for (final boolean colours : new boolean[] {false, true}) {
                final int x0 = colours ? 2 + SWATCH_WIDTH + SWATCH_GAP : 2;
                rock(pen, block, rock.getString("pattern"), x0, rowY, x0 + SWATCH_WIDTH, rowY + SWATCH_HEIGHT,
                        colours);
                pen.frame(x0, rowY, x0 + SWATCH_WIDTH, rowY + SWATCH_HEIGHT, pen.ink());
            }
            final ResourceLocation id = ResourceLocation.tryParse(block);
            if (id != null) {
                pen.text(BuiltInRegistries.BLOCK.get(id).getName().getString(), NAME_X, rowY + 1, pen.ink());
            }
            rowY += ROCK_ROW;
        }
        pen.line(2, rowY + 6, 2 + SWATCH_WIDTH, rowY + 1, true, pen.ink());
        pen.text(GameText.resolve(PlantDrawingTexts.FAULT), NAME_X, rowY, pen.ink());
    }
}
