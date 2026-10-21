/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer.mcp;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.jstech.core.text.Text;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * One call of a tool: who made it, with what arguments, and whether the player took the controls back while it ran,
 * which a tool that acts over many ticks looks at between them and stops on.
 */
public final class McpCall {

    private final PairedClients.PairedClient client;
    private final McpTool tool;
    private final JsonObject args;
    private final AtomicBoolean takenBack = new AtomicBoolean();

    public McpCall(final PairedClients.PairedClient client, final McpTool tool, final JsonObject args) {
        this.client = client;
        this.tool = tool;
        this.args = args;
    }

    public PairedClients.PairedClient client() {
        return this.client;
    }

    public McpTool tool() {
        return this.tool;
    }

    public JsonObject args() {
        return this.args;
    }

    /** Whether the player took the controls back since it began. */
    public boolean takenBack() {
        return this.takenBack.get();
    }

    /** The player took the controls back: whatever it does next, it stops. */
    public void takeBack() {
        this.takenBack.set(true);
    }

    /** The argument {@code name} as a string, or none when it is not one. */
    public Optional<String> text(final String name) {
        final JsonElement value = this.args.get(name);
        return value instanceof JsonPrimitive primitive && primitive.isString() ? Optional.of(primitive.getAsString())
                : Optional.empty();
    }

    /** The argument {@code name} as a whole number, or none when it is not one. */
    public Optional<Long> whole(final String name) {
        final JsonElement value = this.args.get(name);
        if (value instanceof JsonPrimitive primitive && primitive.isNumber()) {
            final double number = primitive.getAsDouble();
            if (number == Math.rint(number) && Math.abs(number) < 1e15) {
                return Optional.of((long) number);
            }
        }
        return Optional.empty();
    }

    /** The argument {@code name} as a true or false, {@code otherwise} when it is not one. */
    public boolean flag(final String name, final boolean otherwise) {
        final JsonElement value = this.args.get(name);
        return value instanceof JsonPrimitive primitive && primitive.isBoolean() ? primitive.getAsBoolean() : otherwise;
    }

    /** The argument {@code name} as an object, or none. */
    public Optional<JsonObject> object(final String name) {
        final JsonElement value = this.args.get(name);
        return value != null && value.isJsonObject() ? Optional.of(value.getAsJsonObject()) : Optional.empty();
    }

    /** The argument {@code name} is missing or wrong, where it should be {@code what}: the result that says so. */
    public static McpResult wrong(final String name, final Text what) {
        return McpResult.failed(McpWords.BAD_ARGUMENT.with(Text.literal(name), what));
    }
}
