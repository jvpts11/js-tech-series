/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide.markdown;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.jstech.core.guide.GuideBlock;
import dev.jstech.core.guide.GuideCodecs;
import java.util.Optional;
import java.util.Set;

/**
 * Reads a block a manual's Markdown file writes as JSON, as a manual's JSON file writes it ({@link GuideCodecs}): the
 * fence's name is its type, so the JSON need not say it again.
 */
public final class CodecJsonBlocks implements IJsonBlocks {

    /** The one there is. */
    public static final CodecJsonBlocks INSTANCE = new CodecJsonBlocks();

    private static final String TYPE = "type";

    private CodecJsonBlocks() {
    }

    @Override
    public Set<String> kinds() {
        return GuideCodecs.BLOCK_TYPES;
    }

    @Override
    public Optional<GuideBlock> read(final String kind, final String json) {
        try {
            final JsonElement read = JsonParser.parseString(json);
            if (!read.isJsonObject()) {
                return Optional.empty();
            }
            if (!read.getAsJsonObject().has(TYPE)) {
                read.getAsJsonObject().addProperty(TYPE, kind);
            }
            return GuideCodecs.BLOCK.parse(JsonOps.INSTANCE, read).result();
        } catch (final RuntimeException unread) {
            return Optional.empty();
        }
    }
}
