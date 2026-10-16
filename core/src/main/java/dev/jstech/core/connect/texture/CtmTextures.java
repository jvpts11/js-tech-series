/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.connect.texture;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Reads the connected textures resource packs write for CTM into {@link CtmTexture}s, as CTM reads them: the
 * {@code ctm} section of a texture's {@code .mcmeta} file, version 1, and a block model's {@code ctm_overrides}, each
 * the faces of one tint index. A type may be named with or without CTM's namespace ({@code ctm:ctm}). Besides, a
 * condition of {@code connect_to} may name a tag of blocks ({@code "tag": "c:glass_blocks"}). Pure.
 */
public final class CtmTextures {

    private static final String SECTION = "ctm";
    private static final String NAMESPACE = "ctm:";
    private static final String OVERRIDES = "ctm_overrides";
    private static final int VERSION = 1;
    private static final int BRIGHTEST = 15;
    private static final Map<String, CtmTexture.Type> TYPES = types();

    private CtmTextures() {
    }

    /**
     * What the {@code .mcmeta} file {@code meta} of a texture says of it, if it has a {@code ctm} section.
     *
     * @throws IllegalArgumentException when its version is not 1, its type is not known, or a value does not read
     */
    public static Optional<CtmTexture> ofMeta(final JsonObject meta) {
        if (!meta.has(SECTION) || !meta.get(SECTION).isJsonObject()) {
            return Optional.empty();
        }
        return Optional.of(of(meta.getAsJsonObject(SECTION)));
    }

    /**
     * The overrides a block model's {@code model} says for the faces of each tint index, if it has any.
     *
     * @throws IllegalArgumentException as {@link #ofMeta}
     */
    public static Map<Integer, CtmTexture> overridesOf(final JsonObject model) {
        if (!model.has(OVERRIDES) || !model.get(OVERRIDES).isJsonObject()) {
            return Map.of();
        }
        final Map<Integer, CtmTexture> out = new LinkedHashMap<>();
        for (final Map.Entry<String, JsonElement> entry : model.getAsJsonObject(OVERRIDES).entrySet()) {
            final int tint;
            try {
                tint = Integer.parseInt(entry.getKey().trim());
            } catch (final NumberFormatException e) {
                throw new IllegalArgumentException("an override is for a tint index, not " + entry.getKey(), e);
            }
            out.put(tint, of(entry.getValue().getAsJsonObject()));
        }
        return Map.copyOf(out);
    }

    /** The texture a {@code ctm} section {@code section} says. */
    public static CtmTexture of(final JsonObject section) {
        if (section.has("ctm_version") && section.get("ctm_version").getAsInt() != VERSION) {
            throw new IllegalArgumentException("only CTM's version 1 is read, not " + section.get("ctm_version"));
        }
        final String word = string(section, "type").orElse("normal").toLowerCase(Locale.ROOT);
        CtmTexture.Type type = TYPES.get(word.startsWith(NAMESPACE) ? word.substring(NAMESPACE.length()) : word);
        Optional<String> logic = Optional.empty();
        if (type == null && word.contains(":")) {
            // A type by id is a logic written as data, which the packs' ctm_logic files or the Core give.
            type = CtmTexture.Type.LOGIC;
            logic = Optional.of(word);
        }
        if (type == null) {
            throw new IllegalArgumentException("a texture is drawn as a type CTM does not know: " + word);
        }
        final List<String> textures = new ArrayList<>();
        if (section.has("textures")) {
            for (final JsonElement texture : section.getAsJsonArray("textures")) {
                textures.add(texture.getAsString());
            }
        }
        if (textures.size() < type.companions()) {
            throw new IllegalArgumentException("a texture of type " + word + " takes " + type.companions()
                    + " companion textures, not " + textures.size());
        }
        final JsonObject extra = section.has("extra") && section.get("extra").isJsonObject()
                ? section.getAsJsonObject("extra") : new JsonObject();
        return new CtmTexture(type, string(section, "layer").map(layer -> layer.toUpperCase(Locale.ROOT)), textures,
                string(section, "proxy"), string(section, "texture"), light(extra), grid(extra),
                extra.has("ignore_states") && extra.get("ignore_states").getAsBoolean(),
                extra.has("connect_inside") ? Optional.of(extra.get("connect_inside").getAsBoolean())
                        : Optional.empty(), connectTo(extra), logic);
    }

    private static Optional<CtmTexture.Light> light(final JsonObject extra) {
        if (!extra.has("light")) {
            return Optional.empty();
        }
        final JsonElement light = extra.get("light");
        if (light.isJsonPrimitive()) {
            final int both = Math.clamp(light.getAsInt(), 0, BRIGHTEST);
            return Optional.of(new CtmTexture.Light(both, both));
        }
        final JsonObject parts = light.getAsJsonObject();
        return Optional.of(new CtmTexture.Light(Math.clamp(integer(parts, "block", 0), 0, BRIGHTEST),
                Math.clamp(integer(parts, "sky", 0), 0, BRIGHTEST)));
    }

    private static CtmTexture.Grid grid(final JsonObject extra) {
        final int size = integer(extra, "size", 2);
        final int width = extra.has("width") && extra.has("height") ? integer(extra, "width", size) : size;
        final int height = extra.has("width") && extra.has("height") ? integer(extra, "height", size) : size;
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("a grid is at least one tile each way, not " + width + " by " + height);
        }
        return new CtmTexture.Grid(width, height, integer(extra, "x_offset", 0), integer(extra, "y_offset", 0));
    }

    private static Optional<CtmTexture.ConnectTo> connectTo(final JsonObject extra) {
        if (!extra.has("connect_to")) {
            return Optional.empty();
        }
        final JsonElement value = extra.get("connect_to");
        if (value.isJsonArray()) {
            return Optional.of(new CtmTexture.ConnectTo(conditions(value), Map.of()));
        }
        // An object: a default for every face, and faces of their own by name.
        final JsonObject object = value.getAsJsonObject();
        final List<CtmTexture.Condition> all = object.has("default") ? conditions(object.get("default")) : List.of();
        final Map<String, CtmTexture.Side> sides = new HashMap<>();
        for (final Map.Entry<String, JsonElement> entry : object.entrySet()) {
            if ("default".equals(entry.getKey())) {
                continue;
            }
            final Optional<String> defer = entry.getValue().isJsonObject()
                    ? string(entry.getValue().getAsJsonObject(), "defer").map(word -> word.toLowerCase(Locale.ROOT))
                    : Optional.empty();
            sides.put(entry.getKey().toLowerCase(Locale.ROOT), new CtmTexture.Side(conditions(entry.getValue()),
                    defer));
        }
        return Optional.of(new CtmTexture.ConnectTo(all, sides));
    }

    /* One condition, or an array of them. */
    private static List<CtmTexture.Condition> conditions(final JsonElement value) {
        final List<CtmTexture.Condition> out = new ArrayList<>();
        final JsonArray array = value.isJsonArray() ? value.getAsJsonArray() : single(value);
        for (final JsonElement element : array) {
            final JsonObject condition = element.getAsJsonObject();
            final boolean tag = condition.has("tag");
            final String block = string(condition, tag ? "tag" : "block").orElseThrow(
                    () -> new IllegalArgumentException("a condition of connect_to names no block: " + condition));
            out.add(new CtmTexture.Condition(block.contains(":") ? block : "minecraft:" + block, tag,
                    predicates(condition)));
        }
        return List.copyOf(out);
    }

    private static List<CtmTexture.Predicate> predicates(final JsonObject condition) {
        if (!condition.has("predicate")) {
            return List.of();
        }
        final List<CtmTexture.Predicate> out = new ArrayList<>();
        final JsonElement value = condition.get("predicate");
        for (final JsonElement element : value.isJsonArray() ? value.getAsJsonArray() : single(value)) {
            final JsonObject predicate = element.getAsJsonObject();
            final String compare = string(predicate, "compare_func").orElse("=");
            for (final Map.Entry<String, JsonElement> entry : predicate.entrySet()) {
                if (!"compare_func".equals(entry.getKey())) {
                    out.add(new CtmTexture.Predicate(entry.getKey(), entry.getValue().getAsString(), compare));
                }
            }
        }
        return List.copyOf(out);
    }

    private static JsonArray single(final JsonElement value) {
        final JsonArray array = new JsonArray();
        array.add(value);
        return array;
    }

    private static Optional<String> string(final JsonObject object, final String key) {
        return object.has(key) && object.get(key).isJsonPrimitive() ? Optional.of(object.get(key).getAsString())
                : Optional.empty();
    }

    private static int integer(final JsonObject object, final String key, final int otherwise) {
        return object.has(key) ? object.get(key).getAsInt() : otherwise;
    }

    private static Map<String, CtmTexture.Type> types() {
        final Map<String, CtmTexture.Type> out = new HashMap<>();
        for (final CtmTexture.Type type : CtmTexture.Type.values()) {
            for (final String word : type.words()) {
                out.put(word, type);
            }
        }
        return Map.copyOf(out);
    }
}
