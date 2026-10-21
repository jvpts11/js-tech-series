/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer.mcp;

import com.google.gson.JsonObject;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/**
 * A tool of the MCP server: its name, what it does in words an agent reads (the Core's own, never another's), the
 * arguments it takes as a JSON schema, what the client needs to use it, and what runs it.
 *
 * @param name        its name: small letters, digits and underscores, as every client takes them
 * @param description what it does and returns
 * @param inputSchema the arguments it takes
 * @param needs       what a client must have been let do to call it
 * @param handler     what runs it
 */
public record McpTool(String name, String description, JsonObject inputSchema, McpPermission needs,
                      IHandler handler) {

    private static final Pattern NAME = Pattern.compile("[a-z][a-z0-9_]{0,63}");

    public McpTool {
        if (!NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("a tool's name is small letters, digits and underscores: " + name);
        }
    }

    /** The tool as {@code tools/list} lists it. */
    public JsonObject toJson() {
        final JsonObject out = new JsonObject();
        out.addProperty("name", this.name);
        out.addProperty("description", this.description);
        out.add("inputSchema", this.inputSchema);
        final JsonObject hints = new JsonObject();
        hints.addProperty("readOnlyHint", this.needs == McpPermission.READ);
        out.add("annotations", hints);
        return out;
    }

    /** What runs a tool: given the call, it answers, at once or later. */
    @FunctionalInterface
    public interface IHandler {

        CompletableFuture<McpResult> run(McpCall call);
    }
}
