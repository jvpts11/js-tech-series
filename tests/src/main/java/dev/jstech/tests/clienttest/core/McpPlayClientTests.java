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
import dev.jstech.core.developer.mcp.McpGuard;
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
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

/**
 * An agent playing the game as the player through the MCP server: it sees where the player is, sets up the test
 * world, opens a machine and reads its screen, walks, picks a slot, places and breaks a block, opens the inventory with
 * its key, takes a picture, waits for what it waits for, runs a GameTest, plays a client test kept as a file, records
 * what it did, and saves and opens the world again; every call over the wire.
 */
public final class McpPlayClientTests {

    private static final int WAIT = 600;
    private static final int SETTLE = 3;
    private static final BlockPos STAND = new BlockPos(0, 2, 0);
    private static final BlockPos MACHINE = new BlockPos(0, 2, -3);
    private static final BlockPos WALK_TO = new BlockPos(3, 2, 0);
    private static final int HOTBAR = 9;
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private static final String INITIALIZE = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":"
            + "{\"protocolVersion\":\"2025-11-25\",\"clientInfo\":{\"name\":\"Player test\",\"version\":\"1.0\"}}}";

    private McpPlayClientTests() {
    }

    @ClientTest(timeoutTicks = 4000)
    public static void play_seesActsAndChecksThroughTheMcpServer(final ClientTestContext ctx) {
        final AtomicReference<CompletableFuture<HttpResponse<String>>> sent = new AtomicReference<>();
        final AtomicReference<String> key = new AtomicReference<>();
        final int[] port = new int[1];
        ctx.afterTest(() -> {
            McpGame.turn(false);
            ctx.mc().setScreen(null);
        });
        ctx.thenTeleport(0, STAND, Direction.NORTH)
                .then(SETTLE, () -> {
                    McpGame.turn(true);
                    port[0] = McpGame.running().orElseThrow().port();
                    sent.set(post(port[0], null, INITIALIZE));
                })
                .thenAwaitScreen(McpPairingScreen.class, WAIT)
                .then(SETTLE, () -> ctx.screen(McpPairingScreen.class).answer(Optional.of(McpPermission.ACT)))
                .thenWaitUntil(() -> sent.get().isDone(), WAIT, "the client is let in")
                .then(0, () -> key.set(sent.get().join().headers().firstValue(McpProtocol.SESSION).orElseThrow()));
        final Deque<Step> steps = new ArrayDeque<>();
        final BlockPos machine = ctx.abs(MACHINE);
        final BlockPos walkTo = ctx.abs(WALK_TO);
        final BlockPos below = ctx.abs(new BlockPos(0, 1, 2));
        final BlockPos placed = below.above();
        final AtomicReference<CompletableFuture<HttpResponse<String>>> page = new AtomicReference<>();
        ctx.then(0, () -> page.set(HTTP.sendAsync(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port[0]
                        + McpGuard.TOOLS)).timeout(Duration.ofSeconds(30)).GET().build(),
                        HttpResponse.BodyHandlers.ofString())))
                .thenWaitUntil(() -> page.get().isDone(), WAIT, "the page of the tools comes back")
                .then(0, () -> ctx.assertTrue(page.get().join().statusCode() == 200 && page.get().join().body()
                        .contains("id=\"game-scene_describe\""), "a browser here reads the page of the game's "
                        + "tools, the scene's among them"));
        steps.add(new Step("player_state", "{}", result -> ctx.assertTrue(result.has("x") && result.has("slot"),
                "it sees where the player is: " + result)));
        steps.add(new Step("scene_describe", "{\"distance\":16}", result -> ctx.assertTrue(
                result.getAsJsonArray("blocks_in_view").size() > 0 && result.getAsJsonObject("world")
                        .has("dimension") && result.getAsJsonObject("standing").has("facing"),
                "it reads the scene the player sees: " + result)));
        steps.add(new Step("world_set_block", "{\"place\":" + place(machine) + ",\"block\":\""
                + TestKitMachine.BLOCK.getId() + "\"}", result -> { }));
        steps.add(new Step("block_open", "{\"place\":" + place(machine) + "}", result -> ctx.assertTrue(
                result.has("title"), "the machine's screen opens: " + result)));
        steps.add(new Step("screen_tree", "{}", result -> ctx.assertTrue(result.has("kind")
                && !result.get("kind").getAsString().equals("none"), "the screen's parts are read: " + result)));
        steps.add(new Step("screen_close", "{}", result -> { }));
        steps.add(new Step("player_walk_to", "{\"place\":" + place(walkTo) + "}", result -> ctx.assertTrue(
                result.has("arrived"), "the player walks there: " + result)));
        steps.add(new Step("world_give", "{\"item\":\"minecraft:stone\",\"count\":4}", result -> { }));
        final int[] stone = {-1};
        steps.add(new Step("inventory_read", "{}", result -> {
            result.getAsJsonArray("slots").forEach(slot -> {
                final JsonObject one = slot.getAsJsonObject();
                if (stone[0] < 0 && one.get("item").getAsString().equals("minecraft:stone")
                        && one.get("slot").getAsInt() < HOTBAR) {
                    stone[0] = one.get("slot").getAsInt();
                }
            });
            ctx.assertTrue(stone[0] >= 0, "the stone given is in the hotbar: " + result);
        }));
        steps.add(new Step("hotbar_select", () -> "{\"slot\":" + stone[0] + "}", result -> { }));
        steps.add(new Step("wait_ticks", "{\"ticks\":3}", result -> { }));
        steps.add(new Step("player_state", "{}", result -> ctx.assertTrue(result.get("slot").getAsInt() == stone[0],
                "the hotbar's key picked the slot: " + result)));
        steps.add(new Step("player_place", "{\"place\":" + place(below) + ",\"face\":\"up\"}", result -> { }));
        steps.add(new Step("wait_until", "{\"block_place\":" + place(placed) + ",\"block\":\"minecraft:stone\","
                + "\"ticks\":100}", result -> ctx.assertTrue(result.has("waited"), "the stone is placed: "
                + result)));
        steps.add(new Step("player_break", "{\"place\":" + place(placed) + "}", result -> ctx.assertTrue(
                result.has("broken"), "the stone is broken: " + result)));
        steps.add(new Step("key_press", "{\"key\":\"e\"}", result -> { }));
        steps.add(new Step("wait_until", "{\"screen\":\"inventory\",\"ticks\":100}", result -> ctx.assertTrue(
                result.has("waited"), "the inventory's key opens it: " + result)));
        steps.add(new Step("screen_close", "{}", result -> { }));
        steps.add(new Step("screen_picture", "{}", result -> ctx.assertTrue(result.has("width"),
                "a picture of the window comes back: " + result)));
        steps.add(new Step("gametests_run", "{\"words\":\"inspect_readsTheBlockItsFields\"}", result ->
                ctx.assertTrue(result.get("ran").getAsInt() >= 1, "a GameTest runs from the world: " + result)));
        steps.add(new Step("recording_start", "{}", result -> { }));
        steps.add(new Step("block_open", "{\"place\":" + place(machine) + "}", result -> { }));
        steps.add(new Step("screen_close", "{}", result -> { }));
        steps.add(new Step("recording_stop", "{}", result -> ctx.assertTrue(result.get("steps").getAsInt() >= 2,
                "what the agent did is recorded: " + result)));
        steps.add(new Step("client_test_run", "{\"mod\":\"jstests\",\"name\":\"kit_machine_opens\"}", result ->
                ctx.assertTrue(result.get("passed").getAsBoolean(), "a client test kept as a file plays: "
                        + result)));
        steps.add(new Step("world_save_and_reload", "{}", result -> ctx.assertTrue(result.has("reloaded"),
                "the world is saved and opened again: " + result)));
        steps.add(new Step("player_state", "{}", result -> ctx.assertTrue(result.has("x"),
                "the player is back in the world")));
        for (final Step step : steps) {
            ctx.then(SETTLE, () -> sent.set(post(port[0], key.get(), tool(step.tool(), step.arguments().get()))))
                    .thenWaitUntil(() -> sent.get().isDone(), WAIT, "the tool " + step.tool() + " answers")
                    .then(0, () -> {
                        final JsonObject answer = JsonParser.parseString(sent.get().join().body()).getAsJsonObject();
                        final JsonObject result = answer.getAsJsonObject("result");
                        ctx.assertTrue(result != null && !result.has("isError"), step.tool() + " answers: "
                                + answer);
                        step.check().accept(result.has("structuredContent") ? result.getAsJsonObject(
                                "structuredContent") : new JsonObject());
                    });
        }
    }

    private static String place(final BlockPos pos) {
        return "{\"x\":" + pos.getX() + ",\"y\":" + pos.getY() + ",\"z\":" + pos.getZ() + "}";
    }

    private static String tool(final String name, final String arguments) {
        return "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/call\",\"params\":{\"name\":\"" + name
                + "\",\"arguments\":" + arguments + "}}";
    }

    /* A message sent as an MCP client on this computer sends it, from a thread of its own. */
    private static CompletableFuture<HttpResponse<String>> post(final int port, @Nullable final String key,
                                                                final String body) {
        final HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/mcp"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/event-stream")
                .timeout(Duration.ofSeconds(120))
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (key != null) {
            request.header(McpProtocol.SESSION, key);
        }
        return HTTP.sendAsync(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    /**
     * A tool called and what is checked of its answer.
     *
     * @param tool      its name
     * @param arguments its arguments, as JSON, written as the call is made
     * @param check     what is checked of what it found
     */
    private record Step(String tool, Supplier<String> arguments, Consumer<JsonObject> check) {

        Step(final String tool, final String arguments, final Consumer<JsonObject> check) {
            this(tool, () -> arguments, check);
        }
    }
}
