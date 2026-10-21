/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.material;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

/**
 * What a tag holds, read from the tag's files as the mods' own jars carry them, before the game has read any tag: a
 * tag's every file is joined, a tag named inside one is followed, and an entry an absent mod would fill is kept like
 * any other, for whoever asks to check it against what is registered. A file that cannot be read is passed over.
 *
 * <p>Pure: it is handed each tag's files as text.
 */
public final class TagFiles {

    private static final String VALUES = "values";
    private static final String ID = "id";
    private static final char TAG = '#';

    private TagFiles() {
    }

    /**
     * Every id {@code tag} holds, written as {@code namespace:path}, its own and those of the tags it names, with the
     * text of each file of a tag given by {@code files}.
     */
    public static Set<String> members(final String tag, final Function<String, List<String>> files) {
        final Set<String> out = new TreeSet<>();
        collect(tag, files, out, new HashSet<>());
        return out;
    }

    private static void collect(final String tag, final Function<String, List<String>> files, final Set<String> out,
                                final Set<String> followed) {
        if (!followed.add(tag)) {
            return;
        }
        for (final String text : files.apply(tag)) {
            final JsonArray values;
            try {
                final JsonElement file = JsonParser.parseString(text);
                if (!(file instanceof JsonObject object) || !(object.get(VALUES) instanceof JsonArray array)) {
                    continue;
                }
                values = array;
            } catch (final JsonParseException unreadable) {
                continue;
            }
            for (final JsonElement value : values) {
                final String entry = entryOf(value);
                if (entry == null || entry.isEmpty()) {
                    continue;
                }
                if (entry.charAt(0) == TAG) {
                    collect(entry.substring(1), files, out, followed);
                } else {
                    out.add(entry);
                }
            }
        }
    }

    /* An entry as a tag's file writes it: an id, a tag's id after a hash, or an object holding either. */
    private static String entryOf(final JsonElement value) {
        if (value instanceof JsonObject object && object.get(ID) != null && object.get(ID).isJsonPrimitive()) {
            return object.get(ID).getAsString();
        }
        return value.isJsonPrimitive() ? value.getAsString() : null;
    }
}
