/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer.mcp;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import java.nio.charset.StandardCharsets;

/**
 * The messages of JSON-RPC 2.0, which MCP is written in: one read from a request's body, a call with an id that is
 * answered, a notice with none that is not, or the client's answer to the server; and the answers written back, a
 * result or an error with its code.
 */
public final class JsonRpc {

    /** The codes JSON-RPC gives its errors, and the one the server gives what the player refused. */
    public static final int PARSE_ERROR = -32700;
    public static final int INVALID_REQUEST = -32600;
    public static final int METHOD_NOT_FOUND = -32601;
    public static final int INVALID_PARAMS = -32602;
    public static final int INTERNAL_ERROR = -32603;
    public static final int REFUSED = -32000;

    private static final String VERSION = "2.0";

    private JsonRpc() {
    }

    /** The message {@code body} holds. */
    public static Message read(final byte[] body) {
        final JsonElement parsed;
        try {
            parsed = JsonParser.parseString(new String(body, StandardCharsets.UTF_8));
        } catch (final JsonSyntaxException | IllegalStateException unreadable) {
            return new Unreadable(PARSE_ERROR, "the body is not JSON");
        }
        if (!parsed.isJsonObject()) {
            // A batch, which the protocol no longer sends, or anything else that is not one message.
            return new Unreadable(INVALID_REQUEST, "the body is not one JSON-RPC message");
        }
        final JsonObject message = parsed.getAsJsonObject();
        if (!message.has("jsonrpc") || !VERSION.equals(text(message.get("jsonrpc")))) {
            return new Unreadable(INVALID_REQUEST, "the message is not JSON-RPC 2.0");
        }
        final JsonElement id = message.get("id");
        if (id != null && !id.isJsonNull() && !(id.isJsonPrimitive() && (id.getAsJsonPrimitive().isString()
                || id.getAsJsonPrimitive().isNumber()))) {
            return new Unreadable(INVALID_REQUEST, "an id is a string or a number");
        }
        if (message.has("method")) {
            final String method = text(message.get("method"));
            if (method == null) {
                return new Unreadable(INVALID_REQUEST, "a method is a string");
            }
            final JsonElement params = message.get("params");
            if (params != null && !params.isJsonNull() && !params.isJsonObject()) {
                return new Unreadable(INVALID_PARAMS, "the params are an object");
            }
            final JsonObject given = params != null && params.isJsonObject() ? params.getAsJsonObject()
                    : new JsonObject();
            return id == null ? new Notice(method, given) : new Call(id, method, given);
        }
        if (id != null && (message.has("result") || message.has("error"))) {
            return new Reply(id);
        }
        return new Unreadable(INVALID_REQUEST, "the message is neither a call nor an answer");
    }

    /** The answer to the call {@code id}: {@code result}. */
    public static JsonObject result(final JsonElement id, final JsonObject result) {
        final JsonObject out = envelope(id);
        out.add("result", result);
        return out;
    }

    /** The answer to the call {@code id}, or to a message with none: the error {@code code}, saying {@code why}. */
    public static JsonObject error(final JsonElement id, final int code, final String why) {
        final JsonObject out = envelope(id);
        final JsonObject error = new JsonObject();
        error.addProperty("code", code);
        error.addProperty("message", why);
        out.add("error", error);
        return out;
    }

    private static JsonObject envelope(final JsonElement id) {
        final JsonObject out = new JsonObject();
        out.addProperty("jsonrpc", VERSION);
        out.add("id", id == null ? JsonNull.INSTANCE : id);
        return out;
    }

    private static String text(final JsonElement element) {
        return element instanceof JsonPrimitive primitive && primitive.isString() ? primitive.getAsString() : null;
    }

    /** A message read. */
    public sealed interface Message permits Call, Notice, Reply, Unreadable {
    }

    /**
     * A call, which is answered.
     *
     * @param id     its id, given back with the answer
     * @param method what it calls
     * @param params what it hands, an empty object when it hands nothing
     */
    public record Call(JsonElement id, String method, JsonObject params) implements Message {
    }

    /**
     * A notice, which is not answered.
     *
     * @param method what it tells
     * @param params what it hands
     */
    public record Notice(String method, JsonObject params) implements Message {
    }

    /**
     * The client's answer to something the server asked it.
     *
     * @param id the id of what it answers
     */
    public record Reply(JsonElement id) implements Message {
    }

    /**
     * A body that holds no message the server can take.
     *
     * @param code the error's code
     * @param why  why, in the protocol's English
     */
    public record Unreadable(int code, String why) implements Message {
    }
}
