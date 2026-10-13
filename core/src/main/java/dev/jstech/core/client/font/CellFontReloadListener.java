/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.font;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.jstech.core.JsCore;
import dev.jstech.core.content.ModContent;
import dev.jstech.core.font.CellFont;
import java.io.IOException;
import java.io.Reader;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.GsonHelper;

/**
 * Reads, each time the packs load, which characters every declared font draws: the cells of its pictures and the
 * characters it gives a width with nothing to draw.
 *
 * <p>Every pack's file for a font counts, as the game puts all of them together into the one font. A file that
 * cannot be read is said so in the log and counts for nothing, which leaves its characters to the game's font.
 */
public final class CellFontReloadListener implements ResourceManagerReloadListener {

    @Override
    public void onResourceManagerReload(final ResourceManager manager) {
        final Map<ResourceLocation, BitSet> covered = new HashMap<>();
        for (final ModContent content : ModContent.all()) {
            for (final CellFont font : content.declaredFonts()) {
                final BitSet characters = new BitSet();
                for (final Resource file : manager.getResourceStack(font.definition())) {
                    try (Reader reader = file.openAsReader()) {
                        final BitSet listed = new BitSet();
                        read(GsonHelper.parse(reader), listed);
                        characters.or(listed);
                    } catch (final IOException | RuntimeException unreadable) {
                        JsCore.LOGGER.warn("The font {} could not be read from {}, so the game's font draws what it "
                                + "lists: {}", font.id(), file.sourcePackId(), unreadable.getMessage());
                    }
                }
                covered.put(font.id(), characters);
            }
        }
        CellFonts.load(covered);
    }

    private static void read(final JsonObject file, final BitSet characters) {
        for (final JsonElement element : GsonHelper.getAsJsonArray(file, "providers")) {
            final JsonObject provider = element.getAsJsonObject();
            switch (GsonHelper.getAsString(provider, "type", "")) {
                case "bitmap" -> GsonHelper.getAsJsonArray(provider, "chars").forEach(row ->
                        row.getAsString().codePoints().filter(codePoint -> codePoint != 0).forEach(characters::set));
                case "space" -> GsonHelper.getAsJsonObject(provider, "advances").keySet().forEach(key ->
                        key.codePoints().forEach(characters::set));
                default -> {
                    // A reference to another font, or a kind of picture the grid does not lay out, adds nothing.
                }
            }
        }
    }
}
