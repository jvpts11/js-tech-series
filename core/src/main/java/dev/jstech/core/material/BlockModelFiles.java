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
import java.util.List;
import java.util.Optional;

/**
 * Reads, from a block's own files, the texture it is drawn with: its block state's first model, and that model's
 * texture of its whole cube, its side, or the one its particles take, following a reference to another of its
 * textures; or, with none, the model it is built on, to look in next. An ore in another mod's rock is drawn on that.
 *
 * <p>Pure: it reads the files' text.
 */
public final class BlockModelFiles {

    /** The textures a cube's look is read from, most telling first. */
    private static final List<String> FACES = List.of("all", "side", "particle", "texture", "end", "top");
    private static final char REFERENCE = '#';
    /** How many references within a model are followed before giving up, so a loop of them ends. */
    private static final int MOST_REFERENCES = 8;

    private BlockModelFiles() {
    }

    /** The first model a block state's file names, as {@code othermod:block/limestone}. */
    public static Optional<String> modelOf(final String blockState) {
        final JsonObject file = objectOf(blockState);
        if (file == null) {
            return Optional.empty();
        }
        if (file.get("variants") instanceof JsonObject variants) {
            for (final var variant : variants.entrySet()) {
                final Optional<String> model = modelIn(variant.getValue());
                if (model.isPresent()) {
                    return model;
                }
            }
        }
        if (file.get("multipart") instanceof JsonArray parts) {
            for (final JsonElement part : parts) {
                if (part instanceof JsonObject object) {
                    final Optional<String> model = modelIn(object.get("apply"));
                    if (model.isPresent()) {
                        return model;
                    }
                }
            }
        }
        return Optional.empty();
    }

    /** The texture a model's file draws its cube with, as {@code othermod:block/limestone}; none when it names none. */
    public static Optional<String> textureOf(final String model) {
        final JsonObject file = objectOf(model);
        if (file == null || !(file.get("textures") instanceof JsonObject textures)) {
            return Optional.empty();
        }
        for (final String face : FACES) {
            final Optional<String> found = resolve(textures, face);
            if (found.isPresent()) {
                return found;
            }
        }
        for (final var texture : textures.entrySet()) {
            final Optional<String> found = resolve(textures, texture.getKey());
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    /** The model a model's file is built on, as {@code minecraft:block/cube_all}; none when it is built on none. */
    public static Optional<String> parentOf(final String model) {
        final JsonObject file = objectOf(model);
        return file != null && file.get("parent") != null && file.get("parent").isJsonPrimitive()
                ? Optional.of(file.get("parent").getAsString()) : Optional.empty();
    }

    private static Optional<String> resolve(final JsonObject textures, final String name) {
        String current = name;
        for (int step = 0; step < MOST_REFERENCES; step++) {
            final JsonElement value = textures.get(current);
            if (value == null || !value.isJsonPrimitive()) {
                return Optional.empty();
            }
            final String written = value.getAsString();
            if (written.isEmpty() || written.charAt(0) != REFERENCE) {
                return Optional.of(written);
            }
            current = written.substring(1);
        }
        return Optional.empty();
    }

    private static Optional<String> modelIn(final JsonElement variant) {
        if (variant instanceof JsonArray choices && !choices.isEmpty()) {
            return modelIn(choices.get(0));
        }
        if (variant instanceof JsonObject object && object.get("model") != null && object.get("model")
                .isJsonPrimitive()) {
            return Optional.of(object.get("model").getAsString());
        }
        return Optional.empty();
    }

    private static JsonObject objectOf(final String text) {
        try {
            return JsonParser.parseString(text) instanceof JsonObject object ? object : null;
        } catch (final JsonParseException unreadable) {
            return null;
        }
    }
}
