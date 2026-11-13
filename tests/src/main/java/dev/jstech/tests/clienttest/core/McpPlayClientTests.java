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
import dev.jstech.core.audio.AudioChannels;
import dev.jstech.core.audio.AudioPrefs;
import dev.jstech.core.client.audio.AudioMixer;
import dev.jstech.core.client.audio.AudioPrefsStore;
import dev.jstech.core.client.audio.mixer.SoundMixerScreen;
import dev.jstech.core.client.mcp.McpGame;
import dev.jstech.core.client.mcp.McpPairingScreen;
import dev.jstech.core.client.studio.TestRecorder;
import dev.jstech.core.developer.mcp.McpCall;
import dev.jstech.core.developer.mcp.McpGuard;
import dev.jstech.core.developer.mcp.McpPermission;
import dev.jstech.core.developer.mcp.McpProtocol;
import dev.jstech.core.developer.studio.RecordedTest;
import dev.jstech.testkit.client.ClientTestFailure;
import dev.jstech.tests.TestKitMachine;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * An agent playing the game as the player through the MCP server: it sees where the player is, sets up the test
 * world, opens a machine and reads its screen, walks, picks a slot, places and breaks a block, opens the inventory with
 * its key, takes a picture, waits for what it waits for, runs a GameTest, plays a client test kept as a file, records
 * what it did, and saves and opens the world again; drags a slider, turns the wheel over a list and looks at a point of
 * a face; every call over the wire.
 */
public final class McpPlayClientTests {

    private static final int WAIT = 600;
    private static final int SETTLE = 3;
    private static final BlockPos STAND = new BlockPos(0, 2, 0);
    private static final BlockPos MACHINE = new BlockPos(0, 2, -3);
    private static final BlockPos WALK_TO = new BlockPos(3, 2, 0);
    /* Far enough that the walk there is still under way when the server is turned off. */
    private static final BlockPos FAR = new BlockPos(0, 2, 40);
    /* A block a little right of the machine, whose south face looks at the player standing at STAND. */
    private static final BlockPos WALL = new BlockPos(1, 2, -3);
    private static final int HOTBAR = 9;
    /* How far along its track the slider is dragged, and how near the volume it sets must come. */
    private static final float QUARTER = 0.25F;
    private static final float CLOSE = 0.03F;
    /* How near, in blocks, the point looked at must be to the one asked for. */
    private static final double AIMED = 0.02;
    private static final String MACHINES = AudioChannels.MACHINES.id().toString();
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
        pair(ctx, sent, key, port);
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
            run(ctx, sent, key, port, step);
        }
    }

    /**
     * The gestures an agent makes as the player's hand does: a drag that moves a slider, a turn of the wheel that
     * scrolls a list, and a look at a point of a face, not its middle; the drag and the turn kept as steps by a
     * recording, as the player's own are.
     */
    @ClientTest(timeoutTicks = 3000)
    public static void gestures_dragTurnTheWheelAndLookAtAPointOfAFace(final ClientTestContext ctx) {
        final AtomicReference<CompletableFuture<HttpResponse<String>>> sent = new AtomicReference<>();
        final AtomicReference<String> key = new AtomicReference<>();
        final int[] port = new int[1];
        final AudioPrefs prefs = AudioPrefsStore.prefs();
        final Map<String, Float> volumes = prefs.volumes();
        final BlockPos wall = ctx.abs(WALL);
        ctx.afterTest(() -> {
            McpGame.turn(false);
            TestRecorder.stop();
            prefs.setVolumes(volumes);
            AudioPrefsStore.save();
            AudioMixer.refreshVolumes();
            ctx.mc().setScreen(null);
            ctx.server().execute(() -> ctx.serverPlayer().serverLevel().setBlockAndUpdate(wall,
                    Blocks.AIR.defaultBlockState()));
        });
        pair(ctx, sent, key, port);
        // The machines' slider: where it starts, a quarter of the way along its track, and its height's middle.
        final int[] slider = new int[3];
        final int[] list = new int[2];
        ctx.then(0, TestRecorder::start)
                .then(SETTLE, () -> ctx.mc().setScreen(new SoundMixerScreen(null)))
                .thenAwaitScreen(SoundMixerScreen.class, WAIT)
                .then(SETTLE, () -> {
                    final AbstractSliderButton first = first(ctx.screen(SoundMixerScreen.class),
                            AbstractSliderButton.class);
                    slider[0] = first.getX() + 4;
                    slider[1] = first.getX() + 4 + Math.round((first.getWidth() - 8) * QUARTER);
                    slider[2] = first.getY() + first.getHeight() / 2;
                });
        run(ctx, sent, key, port, new Step("screen_drag", () -> "{\"x\":" + slider[0] + ",\"y\":" + slider[2]
                + ",\"to_x\":" + slider[1] + ",\"to_y\":" + slider[2] + "}", result -> ctx.assertTrue(
                result.has("dragged"), "the drag is made: " + result)));
        ctx.thenAssert(1, () -> Math.abs(prefs.volume(MACHINES) - QUARTER) < CLOSE, "dragged a quarter of the way "
                + "along its track, the machines' slider sets them to a quarter")
                .then(0, () -> ctx.screen(SoundMixerScreen.class).selectTab(1))
                .then(SETTLE, () -> {
                    final AbstractSelectionList<?> sounds = first(ctx.screen(SoundMixerScreen.class),
                            AbstractSelectionList.class);
                    list[0] = sounds.getX() + sounds.getWidth() / 2;
                    list[1] = sounds.getY() + sounds.getHeight() / 2;
                });
        run(ctx, sent, key, port, new Step("screen_scroll", () -> "{\"x\":" + list[0] + ",\"y\":" + list[1]
                + ",\"notches\":-3}", result -> ctx.assertTrue(result.has("scrolled"), "the wheel turns: "
                + result)));
        ctx.thenAssert(1, () -> first(ctx.screen(SoundMixerScreen.class), AbstractSelectionList.class)
                .getScrollAmount() > 0, "turned down over the list of sounds, the wheel scrolls it")
                .then(0, TestRecorder::stop)
                .thenAssert(0, () -> TestRecorder.steps().stream().anyMatch(RecordedTest.Drag.class::isInstance)
                        && TestRecorder.steps().stream().anyMatch(RecordedTest.Scroll.class::isInstance),
                        "the recording keeps the drag and the turn of the wheel as steps")
                .then(0, () -> ctx.mc().setScreen(null));
        run(ctx, sent, key, port, new Step("world_set_block", "{\"place\":" + place(wall)
                + ",\"block\":\"minecraft:stone\"}", result -> { }));
        // A point near the top left of the face looking at the player: two sixteenths across, fourteen up.
        run(ctx, sent, key, port, new Step("player_look_at", "{\"place\":" + place(wall) + ",\"face\":\"south\","
                + "\"at\":{\"x\":2,\"y\":14,\"z\":0}}", result -> { }));
        final Vec3 aimed = new Vec3(wall.getX() + 2 / 16.0, wall.getY() + 14 / 16.0, wall.getZ() + 1.0);
        ctx.thenAssert(SETTLE, () -> ctx.mc().hitResult instanceof BlockHitResult hit
                && hit.getBlockPos().equals(wall) && hit.getLocation().distanceTo(aimed) < AIMED,
                "the player looks at that point of the face, not at its middle");
    }

    /* Turning the server off takes the keys back from a call still acting, as cutting every client off does. */
    @ClientTest(timeoutTicks = 1600)
    public static void off_takesTheKeysBackFromACallStillActing(final ClientTestContext ctx) {
        final AtomicReference<CompletableFuture<HttpResponse<String>>> sent = new AtomicReference<>();
        final List<McpCall> walking = new ArrayList<>();
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
                .then(0, () -> sent.set(post(port[0], sent.get().join().headers().firstValue(McpProtocol.SESSION)
                        .orElseThrow(), tool("player_walk_to", "{\"place\":" + place(ctx.abs(FAR)) + "}"))))
                .thenWaitUntil(() -> !McpGame.acting().isEmpty() || sent.get().isDone(), WAIT,
                        "the long walk to start")
                .then(0, () -> {
                    ctx.assertTrue(!McpGame.acting().isEmpty(), "the long walk is under way, not answered: "
                            + answer(sent.get()));
                    walking.addAll(McpGame.acting());
                    McpGame.turn(false);
                })
                .thenAssert(0, () -> !walking.isEmpty() && walking.stream().allMatch(McpCall::takenBack),
                        "turning the server off took the keys back from the walk under way");
    }

    /* Turns the server on and lets a client in to read and act, keeping its port and its key. */
    private static void pair(final ClientTestContext ctx,
                             final AtomicReference<CompletableFuture<HttpResponse<String>>> sent,
                             final AtomicReference<String> key, final int[] port) {
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
    }

    /* Calls the step's tool over the wire and checks its answer. */
    private static void run(final ClientTestContext ctx,
                            final AtomicReference<CompletableFuture<HttpResponse<String>>> sent,
                            final AtomicReference<String> key, final int[] port, final Step step) {
        ctx.then(SETTLE, () -> sent.set(post(port[0], key.get(), tool(step.tool(), step.arguments().get()))))
                .thenWaitUntil(() -> sent.get().isDone(), WAIT, "the tool " + step.tool() + " answers")
                .then(0, () -> {
                    final JsonObject answer = JsonParser.parseString(sent.get().join().body()).getAsJsonObject();
                    final JsonObject result = answer.getAsJsonObject("result");
                    ctx.assertTrue(result != null && !result.has("isError"), step.tool() + " answers: " + answer);
                    step.check().accept(result.has("structuredContent") ? result.getAsJsonObject(
                            "structuredContent") : new JsonObject());
                });
    }

    /* The first of the screen's parts of {@code type}. */
    private static <T> T first(final Screen screen, final Class<T> type) {
        for (final GuiEventListener child : screen.children()) {
            if (type.isInstance(child)) {
                return type.cast(child);
            }
        }
        throw new ClientTestFailure("the screen has no " + type.getSimpleName());
    }

    private static String place(final BlockPos pos) {
        return "{\"x\":" + pos.getX() + ",\"y\":" + pos.getY() + ",\"z\":" + pos.getZ() + "}";
    }

    /* What a message sent came back with: the server's answer, or why none came. */
    private static String answer(final CompletableFuture<HttpResponse<String>> sent) {
        try {
            return sent.isDone() ? sent.join().body() : "nothing yet";
        } catch (final RuntimeException failed) {
            return String.valueOf(failed.getCause() == null ? failed : failed.getCause());
        }
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
