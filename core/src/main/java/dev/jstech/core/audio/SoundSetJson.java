/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * A cue's rules as its file keeps them, the form a resource pack writes to bind the cue to other sounds:
 *
 * <pre>{@code
 * { "sounds": [
 *     { "when": { "device": "jsc:pc_speaker" }, "sound": "jsc:computer/boot_beep" },
 *     { "sound": "jsc:computer/boot" } ] }
 * }</pre>
 *
 * <p>Reading is strict, so a pack author learns what is wrong with a file rather than hearing the wrong sound.
 */
public final class SoundSetJson {

    private static final String SOUNDS = "sounds";
    private static final String WHEN = "when";
    private static final String SOUND = "sound";

    private SoundSetJson() {
    }

    /** The rules as a file keeps them. */
    public static JsonObject write(final SoundSet set) {
        final JsonArray sounds = new JsonArray();
        for (final SoundSet.Rule rule : set.rules()) {
            final JsonObject one = new JsonObject();
            if (!rule.when().isEmpty()) {
                final JsonObject when = new JsonObject();
                new TreeMap<>(rule.when()).forEach(when::addProperty);
                one.add(WHEN, when);
            }
            one.addProperty(SOUND, rule.sound());
            sounds.add(one);
        }
        final JsonObject root = new JsonObject();
        root.add(SOUNDS, sounds);
        return root;
    }

    /**
     * The rules a file keeps.
     *
     * @throws IllegalArgumentException when the file is not in the form above, saying where
     */
    public static SoundSet read(final JsonElement file) {
        if (!file.isJsonObject() || !file.getAsJsonObject().has(SOUNDS)
                || !file.getAsJsonObject().get(SOUNDS).isJsonArray()) {
            throw new IllegalArgumentException("a cue's file is an object with a \"sounds\" list");
        }
        final List<SoundSet.Rule> rules = new ArrayList<>();
        int index = 0;
        for (final JsonElement element : file.getAsJsonObject().getAsJsonArray(SOUNDS)) {
            rules.add(rule(element, index++));
        }
        return new SoundSet(rules);
    }

    private static SoundSet.Rule rule(final JsonElement element, final int index) {
        if (!element.isJsonObject()) {
            throw new IllegalArgumentException("sound " + index + " is not an object");
        }
        final JsonObject one = element.getAsJsonObject();
        final JsonElement sound = one.get(SOUND);
        if (sound == null || !sound.isJsonPrimitive() || !sound.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("sound " + index + " names no \"sound\"");
        }
        final Map<String, String> when = new TreeMap<>();
        final JsonElement conditions = one.get(WHEN);
        if (conditions != null) {
            if (!conditions.isJsonObject()) {
                throw new IllegalArgumentException("sound " + index + " has a \"when\" that is not an object");
            }
            for (final Map.Entry<String, JsonElement> condition : conditions.getAsJsonObject().entrySet()) {
                if (!condition.getValue().isJsonPrimitive()) {
                    throw new IllegalArgumentException("sound " + index + " wants " + condition.getKey()
                            + " to be something that is not a value");
                }
                when.put(condition.getKey(), condition.getValue().getAsString());
            }
        }
        return new SoundSet.Rule(when, sound.getAsString());
    }
}
