/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer.mcp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.List;

/** The JSON schema of a tool's arguments, written a property at a time. */
public final class McpSchema {

    private final JsonObject properties = new JsonObject();
    private final JsonArray required = new JsonArray();

    private McpSchema() {
    }

    /** A schema with no arguments yet. */
    public static McpSchema object() {
        return new McpSchema();
    }

    /** A whole number from {@code least} to {@code most}. */
    public McpSchema whole(final String name, final String about, final long least, final long most,
                           final boolean needed) {
        final JsonObject property = property("integer", about);
        property.addProperty("minimum", least);
        property.addProperty("maximum", most);
        return this.add(name, property, needed);
    }

    /** Words. */
    public McpSchema words(final String name, final String about, final boolean needed) {
        return this.add(name, property("string", about), needed);
    }

    /** One of {@code values}. */
    public McpSchema oneOf(final String name, final String about, final List<String> values, final boolean needed) {
        final JsonObject property = property("string", about);
        final JsonArray allowed = new JsonArray();
        values.forEach(allowed::add);
        property.add("enum", allowed);
        return this.add(name, property, needed);
    }

    /** True or false. */
    public McpSchema flag(final String name, final String about) {
        return this.add(name, property("boolean", about), false);
    }

    /** A place in the world: an object of its whole x, y and z. */
    public McpSchema place(final String name, final String about, final boolean needed) {
        final JsonObject property = property("object", about);
        final JsonObject axes = new JsonObject();
        for (final String axis : List.of("x", "y", "z")) {
            final JsonObject one = new JsonObject();
            one.addProperty("type", "integer");
            axes.add(axis, one);
        }
        property.add("properties", axes);
        final JsonArray all = new JsonArray();
        List.of("x", "y", "z").forEach(all::add);
        property.add("required", all);
        return this.add(name, property, needed);
    }

    /** The schema. */
    public JsonObject build() {
        final JsonObject out = new JsonObject();
        out.addProperty("type", "object");
        out.add("properties", this.properties.deepCopy());
        if (!this.required.isEmpty()) {
            out.add("required", this.required.deepCopy());
        }
        out.addProperty("additionalProperties", false);
        return out;
    }

    private McpSchema add(final String name, final JsonObject property, final boolean needed) {
        this.properties.add(name, property);
        if (needed) {
            this.required.add(name);
        }
        return this;
    }

    private static JsonObject property(final String type, final String about) {
        final JsonObject property = new JsonObject();
        property.addProperty("type", type);
        property.addProperty("description", about);
        return property;
    }
}
