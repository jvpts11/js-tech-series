/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.jstech.core.text.Text;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class McpProtocolTest {

    private final AtomicLong now = new AtomicLong(1_000_000);
    private Host host;
    private McpProtocol protocol;

    /* The tools the tests call, declared once for the whole run, as a mod declares its tools once. */
    private static final String ECHO = "test_echo";
    private static final String WRITE = "test_write";

    static {
        McpTools.declare(McpSide.SERVER, "jscore", new McpTool(ECHO, "Says what it is given.",
                McpSchema.object().words("words", "What to say.", true).build(), McpPermission.READ,
                call -> CompletableFuture.completedFuture(McpResult.text(call.text("words").orElse("")))));
        McpTools.declare(McpSide.SERVER, "jscore", new McpTool(WRITE, "Acts.", McpSchema.object().build(),
                McpPermission.ACT, call -> CompletableFuture.completedFuture(McpResult.text("acted"))));
    }

    @BeforeEach
    void setUp() {
        this.host = new Host();
        this.protocol = new McpProtocol(this.host, this.now::get, IMcpLogger.quiet());
    }

    @Test
    void initialize_pairsTheClientTheWayThePlayerAnswered() {
        this.host.answer = Optional.of(McpPermission.READ);
        final McpHttp.Response response = this.post(null, initialize("2025-06-18"));
        assertEquals(200, response.status());
        final String key = response.headers().get(McpProtocol.SESSION);
        assertEquals(64, key.length());
        final JsonObject result = json(response).getAsJsonObject("result");
        assertEquals("2025-06-18", result.get("protocolVersion").getAsString());
        assertEquals("Test client 1.0", this.protocol.clients().all().getFirst().toString());
        assertEquals("Test client", this.host.asked);
    }

    @Test
    void initialize_givesTheNewestVersionToAClientThatAsksForAnother() {
        this.host.answer = Optional.of(McpPermission.READ);
        final JsonObject result = json(this.post(null, initialize("1999-01-01"))).getAsJsonObject("result");
        assertEquals(McpProtocol.VERSIONS.getFirst(), result.get("protocolVersion").getAsString());
    }

    @Test
    void initialize_refusedLetsNoClientInAndRestsAWhile() {
        this.host.answer = Optional.empty();
        final JsonObject refused = json(this.post(null, initialize("2025-06-18")));
        assertEquals(JsonRpc.REFUSED, refused.getAsJsonObject("error").get("code").getAsInt());
        assertTrue(this.protocol.clients().all().isEmpty());
        this.host.answer = Optional.of(McpPermission.ACT);
        this.host.asked = null;
        final JsonObject resting = json(this.post(null, initialize("2025-06-18")));
        assertTrue(resting.getAsJsonObject("error").get("message").getAsString().contains("seconds"), resting
                .toString());
        assertEquals(null, this.host.asked, "a client that rests is not asked about");
        this.now.addAndGet(60_000);
        assertEquals(200, this.post(null, initialize("2025-06-18")).status());
        assertEquals(1, this.protocol.clients().all().size());
    }

    @Test
    void messages_withNoKeyOrAKeyTakenBackAreRefused() {
        assertEquals(400, this.post(null, call("tools/list", new JsonObject())).status());
        assertEquals(404, this.post("0".repeat(64), call("tools/list", new JsonObject())).status());
        final String key = this.paired(McpPermission.READ);
        this.protocol.revokeAll();
        assertEquals(404, this.post(key, call("tools/list", new JsonObject())).status());
    }

    @Test
    void toolsList_listsTheToolsOn() {
        final String key = this.paired(McpPermission.READ);
        final JsonArray tools = json(this.post(key, call("tools/list", new JsonObject()))).getAsJsonObject(
                "result").getAsJsonArray("tools");
        final List<String> names = tools.asList().stream().map(tool -> tool.getAsJsonObject().get("name")
                .getAsString()).toList();
        assertTrue(names.contains(ECHO) && names.contains(WRITE), names.toString());
    }

    @Test
    void toolsCall_runsAToolTheClientMayUse() {
        final String key = this.paired(McpPermission.READ);
        final JsonObject args = new JsonObject();
        args.addProperty("words", "hello");
        final JsonObject result = json(this.post(key, tool(ECHO, args))).getAsJsonObject("result");
        assertEquals("hello", result.getAsJsonArray("content").get(0).getAsJsonObject().get("text").getAsString());
        assertFalse(result.has("isError"));
        assertEquals(1, this.protocol.clients().all().getFirst().calls());
    }

    @Test
    void toolsCall_refusesAToolThatActsToAClientLetInToRead() {
        final String key = this.paired(McpPermission.READ);
        final JsonObject result = json(this.post(key, tool(WRITE, new JsonObject()))).getAsJsonObject("result");
        assertTrue(result.get("isError").getAsBoolean());
        assertTrue(this.host.began.isEmpty(), "a refused call never acts");
        final McpCallLog.Entry entry = this.protocol.log().entries().getFirst();
        assertEquals(McpWords.REFUSED, entry.answer());
    }

    @Test
    void toolsCall_tellsTheGameWhileACallActs() {
        final String key = this.paired(McpPermission.ACT);
        json(this.post(key, tool(WRITE, new JsonObject())));
        assertEquals(List.of(WRITE), this.host.began);
        assertEquals(McpWords.DONE, this.protocol.log().entries().getFirst().answer());
    }

    @Test
    void toolsCall_refusesAToolThereIsNot() {
        final String key = this.paired(McpPermission.ACT);
        final JsonObject answer = json(this.post(key, tool("no_such_tool", new JsonObject())));
        assertEquals(JsonRpc.INVALID_PARAMS, answer.getAsJsonObject("error").get("code").getAsInt());
    }

    @Test
    void delete_endsTheSession() {
        final String key = this.paired(McpPermission.READ);
        final Map<String, String> headers = new HashMap<>();
        headers.put("mcp-session-id", key);
        assertEquals(204, this.protocol.handle(new McpHttp.Request("DELETE", "/mcp", headers, new byte[0]))
                .status());
        assertTrue(this.protocol.clients().all().isEmpty());
    }

    @Test
    void messages_refuseAVersionNotSpoken() {
        final String key = this.paired(McpPermission.READ);
        final Map<String, String> headers = new HashMap<>();
        headers.put("mcp-session-id", key);
        headers.put("mcp-protocol-version", "1999-01-01");
        assertEquals(400, this.protocol.handle(new McpHttp.Request("POST", "/mcp", headers, call("ping",
                new JsonObject()).toString().getBytes(StandardCharsets.UTF_8))).status());
    }

    @Test
    void messages_areRefusedWhileTheGamesToolsAreOff() {
        final String key = this.paired(McpPermission.ACT);
        this.host.closed = true;
        final JsonObject answer = json(this.post(key, tool(ECHO, new JsonObject())));
        assertEquals(JsonRpc.REFUSED, answer.getAsJsonObject("error").get("code").getAsInt());
    }

    private String paired(final McpPermission permission) {
        this.host.answer = Optional.of(permission);
        return this.post(null, initialize("2025-06-18")).headers().get(McpProtocol.SESSION);
    }

    private McpHttp.Response post(final String key, final JsonObject message) {
        final Map<String, String> headers = new HashMap<>();
        if (key != null) {
            headers.put("mcp-session-id", key);
        }
        return this.protocol.handle(new McpHttp.Request("POST", "/mcp", headers, message.toString().getBytes(
                StandardCharsets.UTF_8)));
    }

    private static JsonObject initialize(final String version) {
        final JsonObject params = new JsonObject();
        params.addProperty("protocolVersion", version);
        final JsonObject info = new JsonObject();
        info.addProperty("name", "Test client");
        info.addProperty("version", "1.0");
        params.add("clientInfo", info);
        return call("initialize", params);
    }

    private static JsonObject tool(final String name, final JsonObject args) {
        final JsonObject params = new JsonObject();
        params.addProperty("name", name);
        params.add("arguments", args);
        return call("tools/call", params);
    }

    private static JsonObject call(final String method, final JsonObject params) {
        final JsonObject message = new JsonObject();
        message.addProperty("jsonrpc", "2.0");
        message.addProperty("id", 1);
        message.addProperty("method", method);
        message.add("params", params);
        return message;
    }

    private static JsonObject json(final McpHttp.Response response) {
        return JsonParser.parseString(new String(response.body(), StandardCharsets.UTF_8)).getAsJsonObject();
    }

    /** A game that answers pairings as the test says, and writes down the calls that act. */
    private static final class Host implements IMcpHost {

        private Optional<McpPermission> answer = Optional.empty();
        private String asked;
        private boolean closed;
        private final List<String> began = new ArrayList<>();

        @Override
        public McpSide side() {
            return McpSide.SERVER;
        }

        @Override
        public String version() {
            return "0.0.0";
        }

        @Override
        public Optional<Text> closed() {
            return this.closed ? Optional.of(McpWords.OFF.text()) : Optional.empty();
        }

        @Override
        public CompletableFuture<Pairing> pair(final String name, final String version) {
            this.asked = name;
            return CompletableFuture.completedFuture(this.answer.map(permission -> Pairing.allowed(permission, null))
                    .orElseGet(() -> Pairing.refused(McpWords.PAIR_REFUSED.text())));
        }

        @Override
        public void began(final McpCall call) {
            this.began.add(call.tool().name());
        }

        @Override
        public void ended(final McpCall call) {
            // Nothing to show.
        }
    }
}
