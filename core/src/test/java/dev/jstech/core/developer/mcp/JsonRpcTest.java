/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class JsonRpcTest {

    @Test
    void read_tellsACallFromANoticeAndAnAnswer() {
        final JsonRpc.Call call = assertInstanceOf(JsonRpc.Call.class, read(
                "{\"jsonrpc\":\"2.0\",\"id\":7,\"method\":\"tools/list\"}"));
        assertEquals("tools/list", call.method());
        assertEquals(new JsonPrimitive(7), call.id());
        assertInstanceOf(JsonRpc.Notice.class, read("{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}"));
        assertInstanceOf(JsonRpc.Reply.class, read("{\"jsonrpc\":\"2.0\",\"id\":\"a\",\"result\":{}}"));
    }

    @Test
    void read_refusesWhatIsNotOneMessage() {
        assertEquals(JsonRpc.PARSE_ERROR, unreadable("{not json").code());
        assertEquals(JsonRpc.INVALID_REQUEST, unreadable("[{\"jsonrpc\":\"2.0\",\"method\":\"a\"}]").code());
        assertEquals(JsonRpc.INVALID_REQUEST, unreadable("{\"method\":\"a\"}").code());
        assertEquals(JsonRpc.INVALID_REQUEST, unreadable("{\"jsonrpc\":\"2.0\",\"id\":{},\"method\":\"a\"}")
                .code());
        assertEquals(JsonRpc.INVALID_PARAMS, unreadable("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"a\","
                + "\"params\":[1]}").code());
    }

    @Test
    void result_andError_carryTheCallsId() {
        final JsonObject result = JsonRpc.result(new JsonPrimitive(3), new JsonObject());
        assertEquals("2.0", result.get("jsonrpc").getAsString());
        assertEquals(3, result.get("id").getAsInt());
        final JsonObject error = JsonRpc.error(null, JsonRpc.METHOD_NOT_FOUND, "no");
        assertEquals(JsonRpc.METHOD_NOT_FOUND, error.getAsJsonObject("error").get("code").getAsInt());
        assertEquals(true, error.get("id").isJsonNull());
    }

    private static JsonRpc.Message read(final String json) {
        return JsonRpc.read(json.getBytes(StandardCharsets.UTF_8));
    }

    private static JsonRpc.Unreadable unreadable(final String json) {
        return assertInstanceOf(JsonRpc.Unreadable.class, read(json));
    }
}
