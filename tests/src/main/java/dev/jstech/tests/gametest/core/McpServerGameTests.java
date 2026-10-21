/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.jstech.core.developer.mcp.McpOnServer;
import dev.jstech.core.developer.mcp.McpPermission;
import dev.jstech.core.developer.mcp.McpProtocol;
import dev.jstech.core.developer.mcp.McpServer;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestKitMachine;
import dev.jstech.testkit.gametest.GameTestPlayers;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.ServerOpListEntry;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jetbrains.annotations.Nullable;

/**
 * The MCP server of a dedicated server, as an agent on the server's computer meets it: turned on by an operator in the
 * game, a client asked about to the operators and let in by one, its tools reading the operator's world and running
 * the allowed commands as theirs, and refusing the rest; and never turned on by the console.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class McpServerGameTests {

    private static final String ARENA = "empty";
    private static final String BATCH = "mcp_server";
    private static final BlockPos AT = new BlockPos(2, 2, 2);
    private static final int OPERATOR = 4;
    private static final int WAIT = 200;
    /** The real time a tick waits for the network, in milliseconds. */
    private static final long PAUSE = 10;
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private static final String INITIALIZE = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":"
            + "{\"protocolVersion\":\"2025-06-18\",\"clientInfo\":{\"name\":\"gametest\",\"version\":\"1\"}}}";

    private McpServerGameTests() {
    }

    /* A batch of its own: it waits for the network in real time, which would slow the ticks of tests beside it. */
    @GameTest(template = ARENA, timeoutTicks = 2000, batch = BATCH)
    public static void server_letsAnOperatorsClientInAndRunsItsTools(final GameTestHelper helper) {
        helper.setBlock(AT, TestKitMachine.BLOCK.get());
        final BlockPos pos = helper.absolutePos(AT);
        final ServerPlayer operator = GameTestPlayers.connected(helper, "mcp_operator");
        operator.server.getPlayerList().getOps().add(new ServerOpListEntry(operator.getGameProfile(), OPERATOR,
                false));
        operator.server.getCommands().performPrefixedCommand(operator.createCommandSourceStack(),
                "jstech mcp server on");
        final McpServer server = McpOnServer.running().orElse(null);
        helper.assertTrue(server != null, "an operator in the game turns the server's MCP server on");
        final int port = server.port();
        final CompletableFuture<HttpResponse<String>> pairing = post(port, null, INITIALIZE);
        final AtomicReference<String> key = new AtomicReference<>();
        final AtomicReference<CompletableFuture<HttpResponse<String>>> next = new AtomicReference<>();
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(waited(() -> !McpOnServer.waiting().isEmpty()),
                        "the operators are asked about the client" + (pairing.isDone() ? ", but it was answered "
                                + pairing.handle((answered, failed) -> failed != null ? failed.toString()
                                : answered.statusCode() + " " + answered.body()).join() : "")))
                .thenExecute(() -> McpOnServer.answer(operator, McpOnServer.waiting().iterator().next(),
                        Optional.of(McpPermission.ACT)))
                .thenWaitUntil(() -> helper.assertTrue(waited(pairing::isDone), "the client is answered once let in"))
                .thenExecute(() -> {
                    final HttpResponse<String> answered = pairing.join();
                    helper.assertTrue(answered.statusCode() == 200, "the pairing is answered: " + answered.body());
                    key.set(answered.headers().firstValue(McpProtocol.SESSION).orElse(null));
                    helper.assertTrue(key.get() != null, "the client is given its key");
                    next.set(post(port, key.get(), tool("block_read", "{\"place\":{\"x\":" + pos.getX()
                            + ",\"y\":" + pos.getY() + ",\"z\":" + pos.getZ() + "}}")));
                })
                .thenWaitUntil(() -> helper.assertTrue(waited(next.get()::isDone), "the block is read"))
                .thenExecute(() -> {
                    final JsonObject result = result(next.get().join());
                    helper.assertTrue(!result.has("isError") && result.getAsJsonObject("structuredContent")
                            .get("block").getAsString().equals(TestKitMachine.BLOCK.getId().toString()),
                            "the operator's world is read: " + result);
                    next.set(post(port, key.get(), tool("commands_run", "{\"command\":\"/time set 1000\"}")));
                })
                .thenWaitUntil(() -> helper.assertTrue(waited(next.get()::isDone), "the command is run"))
                .thenExecute(() -> {
                    final JsonObject result = result(next.get().join());
                    helper.assertTrue(!result.has("isError"), "an allowed command runs as the operator's: "
                            + result);
                    next.set(post(port, key.get(), tool("commands_run", "{\"command\":\"op mcp_operator\"}")));
                })
                .thenWaitUntil(() -> helper.assertTrue(waited(next.get()::isDone), "the command is refused"))
                .thenExecute(() -> {
                    final JsonObject result = result(next.get().join());
                    helper.assertTrue(result.has("isError") && result.toString().contains("never"),
                            "giving operator rights is never allowed: " + result);
                    operator.server.getCommands().performPrefixedCommand(operator.createCommandSourceStack(),
                            "jstech mcp server off");
                    helper.assertTrue(McpOnServer.running().isEmpty(), "the operator turns it off again");
                    leave(operator);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void server_isNeverTurnedOnByTheConsole(final GameTestHelper helper) {
        final int done;
        try {
            done = helper.getLevel().getServer().getCommands().getDispatcher().execute("jstech mcp server on",
                    helper.getLevel().getServer().createCommandSourceStack());
        } catch (final CommandSyntaxException wrong) {
            throw new IllegalStateException(wrong);
        }
        helper.assertTrue(done == 0, "the console cannot turn the server's MCP server on");
        helper.succeed();
    }

    /*
     * Whether {@code done} holds, a moment of real time given first when it does not: the test server runs its ticks
     * as fast as it can, and the client's requests take the time a network does.
     */
    private static boolean waited(final BooleanSupplier done) {
        if (done.getAsBoolean()) {
            return true;
        }
        try {
            Thread.sleep(PAUSE);
        } catch (final InterruptedException stopped) {
            Thread.currentThread().interrupt();
        }
        return done.getAsBoolean();
    }

    private static void leave(final ServerPlayer player) {
        player.server.getPlayerList().getOps().remove(player.getGameProfile());
        GameTestPlayers.leave(player);
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
                .timeout(Duration.ofSeconds(WAIT / 20 + 60))
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (key != null) {
            request.header(McpProtocol.SESSION, key);
        }
        return HTTP.sendAsync(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
