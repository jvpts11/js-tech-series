/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.content;

import dev.jstech.core.font.CellFont;
import dev.jstech.core.util.DeclarationError;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Everything about one font, said once: its source, its cell, its baseline, and the licence and credit it comes
 * under. The data generator reads the source and writes the font the game draws from; the cell and the baseline are
 * checked against the source then, so a declaration that has drifted from its font stops the generation instead of
 * drawing every character a pixel out.
 *
 * <pre>{@code
 * public static final CellFont TERMINAL = CONTENT.font("terminal")
 *         .bdf("font/source/terminal.bdf")
 *         .cell(6, 10)
 *         .baseline(8)
 *         .licence("Public domain")
 *         .credit("Who drew it, and where it comes from")
 *         .register();
 * }</pre>
 */
public final class FontBuilder {

    private final ModContent content;
    private final String path;
    private @Nullable String source;
    private int cellWidth;
    private int cellHeight;
    private int baseline;
    private String licence = "";
    private String credit = "";
    private int pixels;

    FontBuilder(final ModContent content, final String path) {
        this.content = content;
        this.path = path;
    }

    /** Reads the font from a BDF file, a path under the mod's {@code assets} folder. */
    public FontBuilder bdf(final String file) {
        this.source = file;
        this.pixels = 0;
        return this;
    }

    /**
     * Reads the font from a TrueType or OpenType file ({@code .ttf}, {@code .otf}), a path under the mod's
     * {@code assets} folder, drawn {@code size} pixels tall with no smoothing, a pixel lit or not as the game's own
     * letters are. The cell and the baseline are the font's own at that size, and are checked as a BDF's are.
     */
    public FontBuilder outline(final String file, final int size) {
        this.source = file;
        this.pixels = size;
        return this;
    }

    /** The cell every glyph sits in, in pixels. */
    public FontBuilder cell(final int width, final int height) {
        this.cellWidth = width;
        this.cellHeight = height;
        return this;
    }

    /** How many rows of the cell are above the baseline; the rest are for descenders. */
    public FontBuilder baseline(final int rows) {
        this.baseline = rows;
        return this;
    }

    /** The licence the font is under, as its makers name it. */
    public FontBuilder licence(final String name) {
        this.licence = name;
        return this;
    }

    /** Who made the font and where it comes from. */
    public FontBuilder credit(final String text) {
        this.credit = text;
        return this;
    }

    /**
     * Declares the font.
     *
     * @throws DeclarationError when the declaration does not say where the font comes from
     */
    public CellFont register() {
        if (source == null) {
            throw DeclarationError.of(content.modid(), "the font " + content.modid() + ":" + path,
                    "does not say where it comes from", "name its file with bdf(...) or outline(...) before "
                            + "register()");
        }
        if (source.endsWith(".bdf") == pixels > 0) {
            throw DeclarationError.of(content.modid(), "the font " + content.modid() + ":" + path,
                    "names " + source + " as the wrong kind of font", "name a .bdf with bdf(...), and a .ttf or an "
                            + ".otf with outline(file, size)");
        }
        final CellFont font = new CellFont(ResourceLocation.fromNamespaceAndPath(content.modid(), path), source,
                cellWidth, cellHeight, baseline, licence, credit, pixels);
        content.declare(font);
        return font;
    }
}
