/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.light;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The light items give held, read from the files of the data map {@code jscore:held_lights} as the mods' jars carry
 * them, and written again as the files LambDynamicLights reads for the light of an item
 * ({@code assets/<namespace>/dynamiclights/item/<name>.json}): what the Core hands it while it steps aside for it.
 * A file read later wins an item; a file that cannot be read is passed over.
 *
 * <p>Pure: it reads and writes the files' text.
 */
public final class HeldLightFiles {

    private static final String VALUES = "values";
    private static final String VALUE = "value";
    private static final String LEVEL = "level";

    private HeldLightFiles() {
    }

    /** The level of light each item gives held, by id, from the text of each file of the data map. */
    public static Map<String, Integer> levels(final List<String> files) {
        final Map<String, Integer> out = new TreeMap<>();
        for (final String text : files) {
            final JsonObject values;
            try {
                if (!(JsonParser.parseString(text) instanceof JsonObject file)
                        || !(file.get(VALUES) instanceof JsonObject found)) {
                    continue;
                }
                values = found;
            } catch (final JsonParseException unreadable) {
                continue;
            }
            for (final Map.Entry<String, JsonElement> entry : values.entrySet()) {
                JsonElement glow = entry.getValue();
                // An entry may be wrapped with whether it replaces what came before.
                if (glow instanceof JsonObject wrapped && wrapped.get(VALUE) instanceof JsonObject inner) {
                    glow = inner;
                }
                if (glow instanceof JsonObject object && object.get(LEVEL) instanceof JsonPrimitive level
                        && level.isNumber()) {
                    out.put(entry.getKey(), Math.clamp(level.getAsInt(), 0, LightMath.BRIGHTEST));
                }
            }
        }
        return out;
    }

    /** The file LambDynamicLights reads for {@code item}, by id, giving light of {@code level}. */
    public static String lambFile(final String item, final int level) {
        final JsonObject match = new JsonObject();
        match.addProperty("items", item);
        final JsonObject out = new JsonObject();
        out.add("match", match);
        out.addProperty("luminance", Math.clamp(level, 0, LightMath.BRIGHTEST));
        return out.toString();
    }

    /** The name of the file for {@code item}, by id: its namespace and path joined, as a file may be named. */
    public static String fileName(final String item) {
        return item.replace(':', '_').replace('/', '_');
    }
}
