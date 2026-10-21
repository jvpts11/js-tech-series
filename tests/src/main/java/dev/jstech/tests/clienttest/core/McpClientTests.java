/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.jstech.core.client.mcp.McpGame;
import dev.jstech.core.client.mcp.McpPairingScreen;
import dev.jstech.core.client.studio.StudioClient;
import dev.jstech.core.client.studio.StudioScreen;
import dev.jstech.core.client.studio.StudioTool;
import dev.jstech.core.developer.mcp.McpPermission;
import dev.jstech.core.developer.mcp.McpProtocol;
import dev.jstech.tests.TestKitMachine;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

/**
 * The studio's MCP server on the player's game, as an agent on this computer meets it: turned on from the game, a
 * client asked about on the screen and let in with a click, its tools reading the world over the wire, the mark on
 * the screen while it is in, its call in the Agents tab, and every key taken back at once.
 */
public final class McpClientTests {

    private static final int WAIT = 200;
    private static final int SETTLE = 3;
    /* The studio reads every thing as it opens, and the ticks it held up then run with no frame between. */
    private static final int SHOT = 20;
    private static final BlockPos STAND = new BlockPos(0, 2, 0);
    private static final BlockPos AT = new BlockPos(0, 2, -3);
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private static final String INITIALIZE = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":"
            + "{\"protocolVersion\":\"2025-06-18\",\"clientInfo\":{\"name\":\"Client test\",\"version\":\"1.0\"}}}";

    private McpClientTests() {
    }

    @ClientTest(timeoutTicks = 900)
    public static void mcp_letsAClientInWithAClickAndAnswersItsTools(final ClientTestContext ctx) {
        final AtomicReference<CompletableFuture<HttpResponse<String>>> sent = new AtomicReference<>();
        final AtomicReference<String> key = new AtomicReference<>();
        final int[] port = new int[1];
        ctx.afterTest(() -> {
            McpGame.turn(false);
            ctx.mc().setScreen(null);
        });
        ctx.thenTeleport(0, STAND, Direction.NORTH)
                .thenServer(SETTLE, level -> level.setBlockAndUpdate(ctx.abs(AT), TestKitMachine.BLOCK.get()
                        .defaultBlockState()))
                .then(SETTLE, () -> {
                    McpGame.turn(true);
                    port[0] = McpGame.running().orElseThrow().port();
                    sent.set(post(port[0], null, INITIALIZE));
                })
                .thenAwaitScreen(McpPairingScreen.class, WAIT)
                .then(0, () -> ctx.assertTrue(ctx.screen(McpPairingScreen.class).asker().equals("Client test 1.0"),
                        "the question names who asks"))
                .thenScreenshot(SHOT, "mcp-pairing")
                .then(0, () -> ctx.screen(McpPairingScreen.class).answer(Optional.of(McpPermission.ACT)))
                .thenWaitUntil(() -> sent.get().isDone(), WAIT, "the client is answered once let in")
                .then(0, () -> {
                    final HttpResponse<String> answered = sent.get().join();
                    ctx.assertTrue(answered.statusCode() == 200, "it is let in: " + answered.body());
                    key.set(answered.headers().firstValue(McpProtocol.SESSION).orElseThrow());
                    final BlockPos pos = ctx.abs(AT);
                    sent.set(post(port[0], key.get(), tool("block_read", "{\"place\":{\"x\":" + pos.getX()
                            + ",\"y\":" + pos.getY() + ",\"z\":" + pos.getZ() + "}}")));
                })
                .thenWaitUntil(() -> sent.get().isDone(), WAIT, "the block is read over the wire")
                .then(0, () -> {
                    final JsonObject result = result(sent.get().join());
                    ctx.assertTrue(!result.has("isError") && result.getAsJsonObject("structuredContent")
                            .get("block").getAsString().equals(TestKitMachine.BLOCK.getId().toString()),
                            "the machine is read: " + result);
                    sent.set(post(port[0], key.get(), tool("thing_sheet", "{\"id\":\""
                            + TestKitMachine.BLOCK.getId() + "\"}")));
                })
                .thenWaitUntil(() -> sent.get().isDone(), WAIT, "the thing's sheet is read")
                .then(0, () -> {
                    final JsonObject result = result(sent.get().join());
                    ctx.assertTrue(result.getAsJsonObject("structuredContent").getAsJsonArray("parts").size() > 0,
                            "what the machine is made of is listed: " + result);
                    sent.set(post(port[0], key.get(), tool("files_read",
                            "{\"path\":\"mod:jscore/../../outside.json\"}")));
                })
                .thenWaitUntil(() -> sent.get().isDone(), WAIT, "a file outside is refused")
                .then(0, () -> {
                    final JsonObject result = result(sent.get().join());
                    ctx.assertTrue(result.has("isError"), "nothing outside the mods' folders is read: " + result);
                    sent.set(post(port[0], key.get(), tool("commands_run", "{\"command\":\"time set 1000\"}")));
                })
                .thenWaitUntil(() -> sent.get().isDone(), WAIT, "an allowed command runs in the test world")
                .then(0, () -> ctx.assertTrue(!result(sent.get().join()).has("isError"),
                        "a test world runs an allowed command: " + sent.get().join().body()))
                .thenScreenshot(SHOT, "mcp-mark")
                .then(0, () -> ctx.mc().setScreen(StudioClient.openOn(StudioTool.AGENTS)))
                .thenAwaitScreen(StudioScreen.class, WAIT)
                .then(SETTLE, () -> ctx.assertTrue(ctx.screen(StudioScreen.class).lines().contains(
                        "Client test 1.0"), "the Agents tab lists the client: "
                        + ctx.screen(StudioScreen.class).lines()))
                .thenScreenshot(SHOT, "mcp-agents")
                .then(0, () -> {
                    ctx.assertTrue(McpGame.cutAll() == 1, "every key is taken back at once");
                    sent.set(post(port[0], key.get(), "{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"ping\"}"));
                })
                .thenWaitUntil(() -> sent.get().isDone(), WAIT, "a key taken back is answered")
                .then(0, () -> ctx.assertTrue(sent.get().join().statusCode() == 404,
                        "a key taken back no longer lets the client in"));
    }

    @ClientTest(timeoutTicks = 400)
    public static void mcp_refusesAClientThePlayerRefuses(final ClientTestContext ctx) {
        final AtomicReference<CompletableFuture<HttpResponse<String>>> sent = new AtomicReference<>();
        ctx.afterTest(() -> {
            McpGame.turn(false);
            ctx.mc().setScreen(null);
        });
        ctx.then(0, () -> {
            McpGame.turn(true);
            sent.set(post(McpGame.running().orElseThrow().port(), null, INITIALIZE));
        })
                .thenAwaitScreen(McpPairingScreen.class, WAIT)
                .then(SETTLE, () -> ctx.screen(McpPairingScreen.class).answer(Optional.empty()))
                .thenWaitUntil(() -> sent.get().isDone(), WAIT, "the client is answered once refused")
                .then(0, () -> {
                    final String body = sent.get().join().body();
                    ctx.assertTrue(body.contains("\"error\"") && sent.get().join().headers().firstValue(
                            McpProtocol.SESSION).isEmpty(), "a refused client gets no key: " + body);
                    ctx.assertTrue(McpGame.running().orElseThrow().protocol().clients().all().isEmpty(),
                            "nobody is let in");
                });
    }

    private static String tool(final String name, final String arguments) {
        return "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/call\",\"params\":{\"name\":\"" + name
                + "\",\"arguments\":" + arguments + "}}";
    }

    private static JsonObject result(final HttpResponse<String> response) {
        return JsonParser.parseString(response.body()).getAsJsonObject().getAsJsonObject("result");
    }

    /* A message sent as an MCP client on this computer sends it, from a thread of its own. */
    private static CompletableFuture<HttpResponse<String>> post(final int port, @Nullable final String key,
                                                                final String body) {
        final HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/mcp"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/event-stream")
                .timeout(Duration.ofSeconds(90))
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (key != null) {
            request.header(McpProtocol.SESSION, key);
        }
        return HTTP.sendAsync(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
