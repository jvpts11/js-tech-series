/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.datagen;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.jstech.core.content.ModContent;
import dev.jstech.core.font.BdfReader;
import dev.jstech.core.font.BitmapFont;
import dev.jstech.core.font.CellFont;
import dev.jstech.core.font.FontSheet;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.Util;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * Turns every font a mod declares from its source into the two files the game's fonts are made of: the picture of
 * its glyphs and the font file that says which character each cell of the picture holds.
 *
 * <p>The font file lists, in the order the game tries them: the characters with nothing to draw and how wide each
 * is; the picture, drawn at the cell's own size and set so the font's baseline lies where the game's font has its
 * own; and the game's font for every character this one lacks, so a text drawn in it never shows a missing glyph.
 * The cell and baseline the declaration gives are checked against the source, and a mismatch stops the generation.
 */
public final class FontSheetProvider implements DataProvider {

    private final PackOutput output;
    private final ModContent content;
    private final ExistingFileHelper files;

    /** How many cells across the picture is; the rows follow from how many glyphs the font has. */
    private static final int COLUMNS = 32;

    public FontSheetProvider(final PackOutput output, final ModContent content, final ExistingFileHelper files) {
        this.output = output;
        this.content = content;
        this.files = files;
    }

    @Override
    public String getName() {
        return content.modid() + ":fonts";
    }

    @Override
    public CompletableFuture<?> run(final CachedOutput cache) {
        final List<CompletableFuture<?>> written = new ArrayList<>();
        for (final CellFont font : content.declaredFonts()) {
            final FontSheet sheet = FontSheet.of(read(font), COLUMNS);
            final Path assets = output.getOutputFolder(PackOutput.Target.RESOURCE_PACK);
            written.add(writePicture(cache, sheet.png(), assets.resolve(font.id().getNamespace())
                    .resolve("textures").resolve(font.sheet().getPath())));
            written.add(DataProvider.saveStable(cache, definition(font, sheet), assets
                    .resolve(font.id().getNamespace()).resolve(font.definition().getPath())));
        }
        return CompletableFuture.allOf(written.toArray(CompletableFuture[]::new));
    }

    /** The font's source, read and checked against what its declaration says of it. */
    private BitmapFont read(final CellFont font) {
        final BitmapFont source;
        try (Reader reader = files.getResource(font.source(), PackType.CLIENT_RESOURCES).openAsReader()) {
            source = BdfReader.read(reader);
        } catch (final IOException e) {
            throw new UncheckedIOException("the font " + font.id() + " could not read its source " + font.source(), e);
        }
        if (source.cellWidth() != font.cellWidth() || source.cellHeight() != font.cellHeight()
                || source.baseline() != font.baseline()) {
            throw new IllegalStateException("the font " + font.id() + " is declared with a cell " + font.cellWidth()
                    + " by " + font.cellHeight() + " and a baseline " + font.baseline() + " rows down, but its source "
                    + "has a cell " + source.cellWidth() + " by " + source.cellHeight() + " and a baseline "
                    + source.baseline() + " rows down");
        }
        return source;
    }

    private static JsonObject definition(final CellFont font, final FontSheet sheet) {
        final JsonArray providers = new JsonArray();
        if (!sheet.spaces().isEmpty()) {
            final JsonObject advances = new JsonObject();
            for (final Map.Entry<Integer, Integer> space : sheet.spaces().entrySet()) {
                advances.addProperty(new String(Character.toChars(space.getKey())), space.getValue());
            }
            final JsonObject spaces = new JsonObject();
            spaces.addProperty("type", "space");
            spaces.add("advances", advances);
            providers.add(spaces);
        }
        final JsonObject bitmap = new JsonObject();
        bitmap.addProperty("type", "bitmap");
        bitmap.addProperty("file", font.sheet().toString());
        bitmap.addProperty("height", font.cellHeight());
        /*
         * The game's letters have seven rows above their baseline, and it draws a glyph's top seven rows less its
         * ascent below the pen. An ascent of this font's own rows above its baseline therefore puts both baselines
         * on one line, so a word in this font sits level with the game's letters around it.
         */
        bitmap.addProperty("ascent", font.baseline());
        final JsonArray chars = new JsonArray();
        sheet.rows().forEach(chars::add);
        bitmap.add("chars", chars);
        providers.add(bitmap);
        final JsonObject fallback = new JsonObject();
        fallback.addProperty("type", "reference");
        fallback.addProperty("id", ResourceLocation.withDefaultNamespace("default").toString());
        providers.add(fallback);
        final JsonObject file = new JsonObject();
        file.add("providers", providers);
        return file;
    }

    /*
     * SHA-1 is the digest the game's own data cache keys files by; it is a cache key, not a security primitive, so
     * Guava's deprecation of it does not apply here.
     */
    @SuppressWarnings("deprecation")
    private static CompletableFuture<?> writePicture(final CachedOutput cache, final byte[] png, final Path path) {
        return CompletableFuture.runAsync(() -> {
            try {
                final HashCode hash = Hashing.sha1().hashBytes(png);
                cache.writeIfNeeded(path, png, hash);
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        }, Util.backgroundExecutor());
    }
}
