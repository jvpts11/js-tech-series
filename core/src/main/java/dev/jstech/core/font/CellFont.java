/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.font;

import net.minecraft.resources.ResourceLocation;

/**
 * A font a mod declares, drawn on a grid of equal cells: where its source is, the cell every glyph sits in, where the
 * baseline runs through that cell, and the licence and the credit the font comes under, which stay with it wherever
 * it is used.
 *
 * <p>Declared with {@code ModContent.font}, it is made by the data generator from its source into the two files the
 * game's fonts are: a picture of every glyph, {@code assets/<mod>/textures/font/<path>.png}, and the font itself,
 * {@code assets/<mod>/font/<path>.json}, which any text can be drawn in by its {@link #id()}. A character the font
 * lacks is drawn in the game's own font.
 */
public final class CellFont {

    private final ResourceLocation id;
    private final String source;
    private final int cellWidth;
    private final int cellHeight;
    private final int baseline;
    private final String licence;
    private final String credit;

    /**
     * @param source   the font's source file under the mod's {@code assets} folder ({@code font/source/x.bdf})
     * @param baseline how many rows of the cell are above the baseline
     * @param licence  the licence the font is under, as its makers name it
     * @param credit   who made the font and where it comes from, as the mod credits it
     */
    public CellFont(final ResourceLocation id, final String source, final int cellWidth, final int cellHeight,
                    final int baseline, final String licence, final String credit) {
        if (cellWidth <= 0 || cellHeight <= 0) {
            throw new IllegalArgumentException("the font " + id + " cannot have a cell " + cellWidth + " by "
                    + cellHeight);
        }
        if (baseline <= 0 || baseline > cellHeight) {
            throw new IllegalArgumentException("the font " + id + " puts its baseline " + baseline
                    + " rows down a cell " + cellHeight + " tall");
        }
        if (licence.isBlank() || credit.isBlank()) {
            throw new IllegalArgumentException("the font " + id + " does not say its licence and who made it");
        }
        this.id = id;
        this.source = source;
        this.cellWidth = cellWidth;
        this.cellHeight = cellHeight;
        this.baseline = baseline;
        this.licence = licence;
        this.credit = credit;
    }

    /** The font's name, which a text's style draws it by. */
    public ResourceLocation id() {
        return id;
    }

    /** The font's source file, a resource of the mod's assets. */
    public ResourceLocation source() {
        return id.withPath(source);
    }

    /** The font file the game reads, under {@code font/}. */
    public ResourceLocation definition() {
        return id.withPath(path -> "font/" + path + ".json");
    }

    /** The picture of its glyphs, as the font file names it: under {@code textures/}, without that folder. */
    public ResourceLocation sheet() {
        return id.withPath(path -> "font/" + path + ".png");
    }

    public int cellWidth() {
        return cellWidth;
    }

    public int cellHeight() {
        return cellHeight;
    }

    /** How many rows of the cell are above the baseline. */
    public int baseline() {
        return baseline;
    }

    public String licence() {
        return licence;
    }

    public String credit() {
        return credit;
    }

    @Override
    public String toString() {
        return id.toString();
    }
}
