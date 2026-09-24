/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.util.Map;

/**
 * The player's sound preferences as their file keeps them: readable by hand, and forgiving when read back, so a file
 * edited wrongly or cut short gives the defaults for what it spoiled rather than a broken game.
 */
public final class AudioPrefsJson {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private AudioPrefsJson() {
    }

    /** The preferences as the file keeps them. */
    public static String write(final AudioPrefs prefs) {
        final JsonObject root = new JsonObject();
        final JsonObject levels = new JsonObject();
        prefs.volumes().forEach(levels::addProperty);
        root.add("volumes", levels);
        final JsonArray off = new JsonArray();
        prefs.mutedSounds().forEach(off::add);
        root.add("muted", off);
        root.addProperty("visual_cues", prefs.visualCues());
        root.addProperty("occlusion", prefs.occlusion());
        return GSON.toJson(root);
    }

    /** Preferences read from what a file kept; what is missing or cannot be read is left at its default. */
    public static AudioPrefs read(final String json) {
        final AudioPrefs prefs = new AudioPrefs();
        final JsonObject root;
        try {
            final JsonElement parsed = GSON.fromJson(json, JsonElement.class);
            if (parsed == null || !parsed.isJsonObject()) {
                return prefs;
            }
            root = parsed.getAsJsonObject();
        } catch (final JsonParseException unreadable) {
            return prefs;
        }
        if (root.has("volumes") && root.get("volumes").isJsonObject()) {
            for (final Map.Entry<String, JsonElement> level : root.getAsJsonObject("volumes").entrySet()) {
                if (level.getValue().isJsonPrimitive() && level.getValue().getAsJsonPrimitive().isNumber()) {
                    prefs.setVolume(level.getKey(), level.getValue().getAsFloat());
                }
            }
        }
        if (root.has("muted") && root.get("muted").isJsonArray()) {
            for (final JsonElement sound : root.getAsJsonArray("muted")) {
                if (sound.isJsonPrimitive()) {
                    prefs.setMuted(sound.getAsString(), true);
                }
            }
        }
        prefs.setVisualCues(flag(root, "visual_cues", false));
        prefs.setOcclusion(flag(root, "occlusion", true));
        return prefs;
    }

    private static boolean flag(final JsonObject root, final String name, final boolean fallback) {
        final JsonElement value = root.get(name);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean()
                ? value.getAsBoolean() : fallback;
    }
}
