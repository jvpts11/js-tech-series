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
import dev.jstech.core.text.Text;
import java.util.Base64;
import org.jetbrains.annotations.Nullable;

/**
 * What a tool answers: its content, as MCP gives a tool's result (text, and pictures), what it found as one JSON
 * object a client can read without parsing text, and whether it is an error, which the agent reads as the tool
 * failing rather than the call.
 *
 * @param content    the content blocks
 * @param structured what was found, as an object, or none
 * @param error      whether the tool failed
 * @param why        why it failed, for the call's log, or none
 */
public record McpResult(JsonArray content, @Nullable JsonObject structured, boolean error, @Nullable Text why) {

    /** What was found: the value as JSON text, and as structured content when it is an object. */
    public static McpResult json(final JsonElement value) {
        final JsonArray content = new JsonArray();
        content.add(block("text", value.toString()));
        return new McpResult(content, value.isJsonObject() ? value.getAsJsonObject() : null, false, null);
    }

    /** Words. */
    public static McpResult text(final String words) {
        final JsonArray content = new JsonArray();
        content.add(block("text", words));
        return new McpResult(content, null, false, null);
    }

    /** A picture in PNG, with what it shows. */
    public static McpResult picture(final byte[] png, final JsonObject about) {
        final JsonArray content = new JsonArray();
        final JsonObject image = new JsonObject();
        image.addProperty("type", "image");
        image.addProperty("data", Base64.getEncoder().encodeToString(png));
        image.addProperty("mimeType", "image/png");
        content.add(image);
        content.add(block("text", about.toString()));
        return new McpResult(content, about, false, null);
    }

    /** The tool failed, for the reason {@code why}, which the agent reads in English. */
    public static McpResult failed(final Text why) {
        final JsonArray content = new JsonArray();
        content.add(block("text", why.english()));
        return new McpResult(content, null, true, why);
    }

    /** The result as MCP writes it. */
    public JsonObject toJson() {
        final JsonObject out = new JsonObject();
        out.add("content", this.content);
        if (this.structured != null) {
            out.add("structuredContent", this.structured);
        }
        if (this.error) {
            out.addProperty("isError", true);
        }
        return out;
    }

    private static JsonObject block(final String type, final String text) {
        final JsonObject block = new JsonObject();
        block.addProperty("type", type);
        block.addProperty("text", text);
        return block;
    }
}
