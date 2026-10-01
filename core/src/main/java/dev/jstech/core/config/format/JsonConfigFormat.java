/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.config.format;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Plain JSON, read and written with Gson, which the game already carries. It reads as Gson does, forgiving the slips
 * a file edited by hand makes (a comment, a key without quotes). JSON has no comments, so a file of it is written
 * with none; a settings file that wants them is written in JSON5 instead.
 */
public final class JsonConfigFormat implements IConfigFormat {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    JsonConfigFormat() {
    }

    @Override
    public String extension() {
        return "json";
    }

    @Override
    public boolean keepsComments() {
        return false;
    }

    @Override
    public Map<String, Object> read(final byte[] file) throws ConfigFormatException {
        final JsonElement parsed;
        try {
            parsed = JsonParser.parseString(new String(file, StandardCharsets.UTF_8));
        } catch (final JsonParseException e) {
            throw new ConfigFormatException("not JSON: " + e.getMessage(), e);
        }
        if (parsed.isJsonNull()) {
            return new LinkedHashMap<>();
        }
        if (!parsed.isJsonObject()) {
            throw new ConfigFormatException("not a JSON object at the top of the file");
        }
        return plain(parsed.getAsJsonObject());
    }

    @Override
    public byte[] write(final Map<String, Object> values, final IConfigComments comments) {
        return (GSON.toJson(json(values)) + "\n").getBytes(StandardCharsets.UTF_8);
    }

    private static Map<String, Object> plain(final JsonObject object) {
        final Map<String, Object> out = new LinkedHashMap<>();
        for (final Map.Entry<String, JsonElement> entry : object.entrySet()) {
            final Object value = plainValue(entry.getValue());
            if (value != null) {
                out.put(entry.getKey(), value);
            }
        }
        return out;
    }

    private static Object plainValue(final JsonElement element) {
        if (element.isJsonObject()) {
            return plain(element.getAsJsonObject());
        }
        if (element.isJsonArray()) {
            final List<Object> out = new ArrayList<>();
            for (final JsonElement item : element.getAsJsonArray()) {
                final Object value = plainValue(item);
                if (value != null) {
                    out.add(value);
                }
            }
            return out;
        }
        if (element.isJsonNull()) {
            return null;
        }
        final JsonPrimitive primitive = element.getAsJsonPrimitive();
        if (primitive.isBoolean()) {
            return primitive.getAsBoolean();
        }
        if (primitive.isNumber()) {
            return number(primitive.getAsString());
        }
        return primitive.getAsString();
    }

    /** A number as the file wrote it: whole when it has no point or exponent, as small a whole as holds it. */
    static Number number(final String written) {
        if (written.indexOf('.') < 0 && written.indexOf('e') < 0 && written.indexOf('E') < 0) {
            try {
                return whole(Long.parseLong(written));
            } catch (final NumberFormatException tooBig) {
                return Double.parseDouble(written);
            }
        }
        return Double.parseDouble(written);
    }

    /** A whole number as an {@link Integer} when one holds it, else a {@link Long}. */
    static Number whole(final long value) {
        // Not a conditional expression: one of an Integer and a Long would unbox both and come back a Long.
        if (value == (int) value) {
            return (int) value;
        }
        return value;
    }

    private static JsonElement json(final Object value) {
        if (value instanceof Map<?, ?> map) {
            final JsonObject object = new JsonObject();
            for (final Map.Entry<?, ?> entry : map.entrySet()) {
                object.add(String.valueOf(entry.getKey()), json(entry.getValue()));
            }
            return object;
        }
        if (value instanceof List<?> list) {
            final JsonArray array = new JsonArray();
            for (final Object element : list) {
                array.add(json(element));
            }
            return array;
        }
        if (value instanceof Boolean flag) {
            return new JsonPrimitive(flag);
        }
        if (value instanceof Number number) {
            return new JsonPrimitive(number);
        }
        return new JsonPrimitive(String.valueOf(value));
    }
}
