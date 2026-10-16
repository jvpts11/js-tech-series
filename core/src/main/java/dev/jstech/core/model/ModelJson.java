/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Arrays;
import org.joml.Vector3f;

/* The small reads every model file of JSON makes: numbers, words, flags and vectors, each with what stands for none. */
final class ModelJson {

    private ModelJson() {
    }

    static Vector3f vector(final JsonObject object, final String key) {
        return object.has(key) ? vector(object.get(key)) : new Vector3f();
    }

    static Vector3f vector(final JsonElement value) {
        final float[] numbers = floats(value, 3);
        return new Vector3f(numbers[0], numbers[1], numbers[2]);
    }

    /* {@code count} numbers from an array, or one number for each. */
    static float[] floats(final JsonElement value, final int count) {
        final float[] out = new float[count];
        if (value.isJsonArray()) {
            final JsonArray array = value.getAsJsonArray();
            for (int i = 0; i < count && i < array.size(); i++) {
                out[i] = array.get(i).getAsFloat();
            }
        } else {
            Arrays.fill(out, value.getAsFloat());
        }
        return out;
    }

    static float number(final JsonObject object, final String key, final float otherwise) {
        return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsFloat() : otherwise;
    }

    static String string(final JsonObject object, final String key, final String otherwise) {
        return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsString() : otherwise;
    }

    static boolean flag(final JsonObject object, final String key, final boolean otherwise) {
        return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsBoolean() : otherwise;
    }
}
