/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer.mcp;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Map;
import java.util.Set;

/**
 * What players wrote, as it goes back to an agent: a sign's lines, a book's pages, a chat line, a name given at an
 * anvil, each put under {@value #KEY}, so the agent reads it as words a player wrote and never as instructions, as the
 * server's instructions say.
 */
public final class PlayerText {

    /** The key what a player wrote is put under. */
    public static final String KEY = "written_by_a_player";

    /** The keys a block entity or an item saves what a player wrote under. */
    private static final Set<String> WRITTEN = Set.of("messages", "filtered_messages", "CustomName", "custom_name",
            "pages", "title", "author", "Text1", "Text2", "Text3", "Text4", "Book", "raw", "filtered",
            "minecraft:custom_name", "minecraft:written_book_content", "minecraft:writable_book_content");

    private PlayerText() {
    }

    /** {@code written} marked as written by a player. */
    public static JsonObject mark(final String written) {
        final JsonObject out = new JsonObject();
        out.addProperty(KEY, written);
        return out;
    }

    /** Whether a value saved at {@code path} ({@code front_text.messages.0}) holds what a player wrote. */
    public static boolean writtenAt(final String path) {
        for (final String part : path.split("[.\\[\\]]")) {
            if (WRITTEN.contains(part)) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@code saved}, what a block entity or an item saved, with every piece of words a player could have written
     * marked: the values of the keys signs, books and names keep them under, all the way down.
     */
    public static JsonElement markIn(final JsonElement saved) {
        return walk(saved, false);
    }

    private static JsonElement walk(final JsonElement element, final boolean written) {
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            return written ? mark(element.getAsString()) : element;
        }
        if (element.isJsonArray()) {
            final JsonArray out = new JsonArray();
            element.getAsJsonArray().forEach(item -> out.add(walk(item, written)));
            return out;
        }
        if (element.isJsonObject()) {
            final JsonObject out = new JsonObject();
            for (final Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                out.add(entry.getKey(), walk(entry.getValue(), written || WRITTEN.contains(entry.getKey())));
            }
            return out;
        }
        return element;
    }
}
