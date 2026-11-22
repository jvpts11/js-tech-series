/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.jstech.core.agent.AgentPlace;
import dev.jstech.core.agent.AgentPlaces;
import dev.jstech.core.agent.AgentReadings;
import dev.jstech.core.audio.AudioChannels;
import dev.jstech.core.audio.AudioPrefs;
import dev.jstech.core.audio.SoundMixerTexts;
import dev.jstech.core.client.audio.AudioMixer;
import dev.jstech.core.client.audio.AudioPrefsStore;
import dev.jstech.core.client.audio.mixer.SoundMixerScreen;
import dev.jstech.core.client.gui.screen.IGuiTab;
import dev.jstech.core.client.gui.screen.TabbedScreen;
import dev.jstech.core.client.mcp.McpGame;
import dev.jstech.core.client.mcp.McpPairingScreen;
import dev.jstech.core.client.studio.TestRecorder;
import dev.jstech.core.developer.mcp.McpCall;
import dev.jstech.core.developer.mcp.McpChanges;
import dev.jstech.core.developer.mcp.McpGuard;
import dev.jstech.core.developer.mcp.McpHold;
import dev.jstech.core.developer.mcp.McpPermission;
import dev.jstech.core.developer.mcp.McpProtocol;
import dev.jstech.core.developer.studio.RecordedTest;
import dev.jstech.core.text.GameText;
import dev.jstech.testkit.client.ClientTestFailure;
import dev.jstech.tests.JsTests;
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
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

/**
 * An agent playing the game as the player through the MCP server: it sees where the player is, sets up the test
 * world, opens a machine and reads its screen, walks, picks a slot, places and breaks a block, opens the inventory with
 * its key, takes a picture, waits for what it waits for, runs a GameTest, plays a client test kept as a file, records
 * what it did, and saves and opens the world again; drags a slider, turns the wheel over a list and looks at a point of
 * a face; every call over the wire, each answered with the game's tick and what the call changed.
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
    /* The block between the player standing at STAND and the machine, on the line from the eye to its middle. */
    private static final BlockPos BETWEEN = new BlockPos(0, 2, -2);
    private static final int HOTBAR = 9;
    /* How far along its track the slider is dragged, and how near the volume it sets must come. */
    private static final float QUARTER = 0.25F;
    private static final float CLOSE = 0.03F;
    /* How near, in blocks, the point looked at must be to the one asked for. */
    private static final double AIMED = 0.02;
    /* The line of chat an agent's wait hears, and how long after the call began it comes, in milliseconds. */
    private static final String HEARD = "heard by the agent";
    private static final long HEARD_AFTER = 500;
    /* How long a key is held, in ticks. */
    private static final int HELD_TICKS = 20;
    /*
     * The walk test's course: a platform two blocks high from x 5 to 8, a stair of two steps up to it from the west, a
     * wall across it at x 7 with a shut door in it, the place beyond the door, and the top of a pillar beside it.
     */
    private static final int PLATFORM_FROM = 5;
    private static final int PLATFORM_TO = 8;
    private static final BlockPos DOOR = new BlockPos(7, 4, 0);
    private static final BlockPos BEYOND_THE_DOOR = new BlockPos(8, 4, 0);
    private static final BlockPos PILLAR_TOP = new BlockPos(12, 7, 4);
    private static final String MACHINES = AudioChannels.MACHINES.id().toString();
    /* How long a held world is watched standing still between calls, in the client's ticks. */
    private static final int THINKS = 40;
    /* How many slots of cobblestone the furnace's recipe takes. */
    private static final int FURNACE_SLOTS = 8;
    /* What the test mod tells of its machine, and the mod whose reading fails. */
    private static final String WORKING = "working";
    private static final String BROKEN = "jstests_broken";
    /* The kind of the test mod's place, and how far east of the player's eyes it is, in blocks. */
    private static final String MARK = "test_mark";
    private static final double MARK_EAST = 10;
    /* A screen of the Core's kit made for the test, and its two tabs' titles. */
    private static final String KIT_TITLE = "Kit";
    private static final String[] KIT_TABS = {"Alpha", "Beta"};
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private static final String INITIALIZE = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":"
            + "{\"protocolVersion\":\"2025-11-25\",\"clientInfo\":{\"name\":\"Player test\",\"version\":\"1.0\"}}}";
    /* Whether the test readings were declared, once for the game's run. */
    private static boolean readingsDeclared;

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
        steps.add(new Step("screen_close", "{}", result -> ctx.assertTrue(result.has(McpChanges.TICK)
                && result.has(McpChanges.CHANGED) && result.getAsJsonObject(McpChanges.CHANGED).has("screen")
                && result.getAsJsonObject(McpChanges.CHANGED).getAsJsonObject("screen").get(McpChanges.TO)
                .isJsonNull(), "the answer tells the game's tick and that the call closed the screen: " + result)));
        // Why an action fails, or lands elsewhere: a block forty blocks off is out of reach, one between is in the way.
        final BlockPos far = ctx.abs(FAR);
        final BlockPos between = ctx.abs(BETWEEN);
        steps.add(new Step("world_set_block", "{\"place\":" + place(far) + ",\"block\":\"minecraft:stone\"}",
                result -> { }));
        steps.add(Step.failing("block_open", "{\"place\":" + place(far) + "}", result -> ctx.assertTrue(
                "out_of_reach".equals(text(result, "failed")) && result.get("distance").getAsDouble()
                        > result.get("reach").getAsDouble(), "opening a block out of reach says so: " + result)));
        steps.add(Step.failing("player_break", "{\"place\":" + place(far) + "}", result -> ctx.assertTrue(
                "out_of_reach".equals(text(result, "failed")), "breaking it says so too: " + result)));
        steps.add(new Step("world_set_block", "{\"place\":" + place(far) + ",\"block\":\"minecraft:air\"}",
                result -> { }));
        steps.add(new Step("world_set_block", "{\"place\":" + place(between) + ",\"block\":\"minecraft:stone\"}",
                result -> { }));
        steps.add(new Step("player_use", "{\"place\":" + place(machine) + "}", result -> ctx.assertTrue(
                "in_the_way".equals(text(result, "instead")) && result.getAsJsonObject("used_on").get("block")
                        .getAsString().equals("minecraft:stone"), "a use stopped by a block between says what took "
                        + "it: " + result)));
        // Taken away again, and what the use may have placed on it with it.
        steps.add(new Step("world_set_block", "{\"place\":" + place(between) + ",\"block\":\"minecraft:air\"}",
                result -> { }));
        steps.add(new Step("world_set_block", "{\"place\":" + place(between.above()) + ",\"block\":"
                + "\"minecraft:air\"}", result -> { }));
        steps.add(new Step("player_walk_to", "{\"place\":" + place(walkTo) + "}", result -> ctx.assertTrue(
                result.has("arrived") && result.has("moved") && result.get("moved").getAsDouble() > 0,
                "the player walks there, and the answer says how far: " + result)));
        // A line of chat that comes while the agent waits for it, well after the call began.
        steps.add(new Step("wait_until", () -> {
            CompletableFuture.delayedExecutor(HEARD_AFTER, TimeUnit.MILLISECONDS).execute(() -> ctx.server()
                    .execute(() -> ctx.serverPlayer().sendSystemMessage(Component.literal(HEARD))));
            return "{\"chat\":\"" + HEARD + "\",\"ticks\":200}";
        }, result -> ctx.assertTrue(result.has("chat_new") && result.getAsJsonArray("chat_new").toString()
                .contains(HEARD), "the answer gives the line of chat that came while the call ran: " + result)));
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
        // The server sets the client's clock now and then, which may move it by a tick: only that time went by counts.
        steps.add(new Step("wait_ticks", "{\"ticks\":3}", result -> ctx.assertTrue(
                result.has(McpChanges.TICKS_TAKEN) && result.get(McpChanges.TICKS_TAKEN).getAsLong() >= 1,
                "the answer tells how many of the game's ticks went by while it waited: " + result)));
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
        // What the screen's own events hear of the agent's hand, as a mod listening round a screen hears the player's.
        final int[] pressed = new int[1];
        final Consumer<ScreenEvent.MouseButtonPressed.Pre> hearing = event -> pressed[0]++;
        NeoForge.EVENT_BUS.addListener(ScreenEvent.MouseButtonPressed.Pre.class, hearing);
        ctx.afterTest(() -> {
            NeoForge.EVENT_BUS.unregister(hearing);
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
                .thenAssert(0, () -> pressed[0] == 1, "the drag's press goes through the screen's events once, as "
                        + "the hand's does: heard " + pressed[0])
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
                .thenAssert(0, () -> count(RecordedTest.Drag.class) == 1 && count(RecordedTest.Scroll.class) == 1
                        && count(RecordedTest.ClickAt.class) == 0, "the recording keeps the drag and the turn of the "
                        + "wheel as one step each, never again from the events they went through: "
                        + TestRecorder.steps())
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

    /*
     * A walk as a player walks: up a stair of two steps onto a platform two blocks high, through the shut door in the
     * wall across it, and never in the air though the player may fly; and to the top of a pillar no way leads up, it
     * stops beside it and says a wall is before it.
     */
    @ClientTest(timeoutTicks = 2400)
    public static void walk_climbsAStairGoesThroughADoorAndSaysWhyItStops(final ClientTestContext ctx) {
        final AtomicReference<CompletableFuture<HttpResponse<String>>> sent = new AtomicReference<>();
        final AtomicReference<String> key = new AtomicReference<>();
        final int[] port = new int[1];
        final boolean[] flew = new boolean[1];
        final Consumer<ClientTickEvent.Post> watching = event -> flew[0] |= ctx.player() != null
                && ctx.player().getAbilities().flying;
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, watching);
        ctx.afterTest(() -> {
            NeoForge.EVENT_BUS.unregister(watching);
            McpGame.turn(false);
            ctx.server().execute(() -> course(ctx, ctx.serverPlayer().serverLevel(), true));
        });
        pair(ctx, sent, key, port);
        ctx.thenServer(0, level -> {
            course(ctx, level, false);
            ctx.serverPlayer().getAbilities().flying = false;
            ctx.serverPlayer().onUpdateAbilities();
        }).then(SETTLE, () -> flew[0] = false);
        run(ctx, sent, key, port, new Step("player_walk_to", "{\"place\":" + place(ctx.abs(BEYOND_THE_DOOR)) + "}",
                result -> ctx.assertTrue(result.has("arrived"), "the player walks up the stair, through the door and"
                        + " beyond it: " + result)));
        ctx.thenAssert(0, () -> ctx.player().blockPosition().equals(ctx.abs(BEYOND_THE_DOOR)), "the player stands "
                + "beyond the door: " + ctx.player().blockPosition())
                .thenAssert(0, () -> ctx.mc().level.getBlockState(ctx.abs(DOOR)).getValue(DoorBlock.OPEN),
                        "the door was opened on the way")
                .thenAssert(0, () -> !flew[0], "the player never took to the air on the way");
        run(ctx, sent, key, port, Step.failing("player_walk_to", "{\"place\":" + place(ctx.abs(PILLAR_TOP)) + "}",
                result -> ctx.assertTrue("wall".equals(text(result, "failed")) && result.has("stopped_at"),
                        "a walk to the top of a pillar no way leads up stops beside it, a wall before it: " + result)));
    }

    /* The course the walk test walks, laid or, {@code clear}, taken away again. */
    private static void course(final ClientTestContext ctx, final ServerLevel level, final boolean clear) {
        final BlockState stone = clear ? Blocks.AIR.defaultBlockState() : Blocks.STONE.defaultBlockState();
        for (int x = PLATFORM_FROM; x <= PLATFORM_TO; x++) {
            for (int z = -1; z <= 1; z++) {
                level.setBlockAndUpdate(ctx.abs(new BlockPos(x, 2, z)), stone);
                level.setBlockAndUpdate(ctx.abs(new BlockPos(x, 3, z)), stone);
            }
        }
        level.setBlockAndUpdate(ctx.abs(new BlockPos(PLATFORM_FROM - 1, 2, 0)), stone);
        final BlockState stair = clear ? Blocks.AIR.defaultBlockState() : Blocks.OAK_STAIRS.defaultBlockState()
                .setValue(StairBlock.FACING, Direction.EAST);
        level.setBlockAndUpdate(ctx.abs(new BlockPos(PLATFORM_FROM - 2, 2, 0)), stair);
        level.setBlockAndUpdate(ctx.abs(new BlockPos(PLATFORM_FROM - 1, 3, 0)), stair);
        for (int z = -1; z <= 1; z += 2) {
            level.setBlockAndUpdate(ctx.abs(DOOR.offset(0, 0, z)), stone);
            level.setBlockAndUpdate(ctx.abs(DOOR.offset(0, 1, z)), stone);
        }
        level.setBlockAndUpdate(ctx.abs(DOOR.above(2)), stone);
        if (clear) {
            level.setBlockAndUpdate(ctx.abs(DOOR.above()), Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(ctx.abs(DOOR), Blocks.AIR.defaultBlockState());
        } else {
            final BlockState door = Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.WEST);
            level.setBlock(ctx.abs(DOOR), door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER), Block.UPDATE_ALL);
            level.setBlock(ctx.abs(DOOR.above()), door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER),
                    Block.UPDATE_ALL);
        }
        for (int y = 2; y < PILLAR_TOP.getY(); y++) {
            level.setBlockAndUpdate(ctx.abs(new BlockPos(PILLAR_TOP.getX(), y, PILLAR_TOP.getZ())), stone);
        }
    }

    /*
     * A key held as a finger holds it, through the game's own input: the jump mapping held twenty ticks is down twenty
     * ticks and up after, and Control held with it is down to the game asking the window, as a modifier is asked.
     */
    @ClientTest(timeoutTicks = 1200)
    public static void hold_keepsAMappingAndAModifierDownForTheTicksAsked(final ClientTestContext ctx) {
        final AtomicReference<CompletableFuture<HttpResponse<String>>> sent = new AtomicReference<>();
        final AtomicReference<String> key = new AtomicReference<>();
        final int[] port = new int[1];
        final int[] down = new int[2];
        final Consumer<ClientTickEvent.Post> counting = event -> {
            down[0] += ctx.mc().options.keyJump.isDown() ? 1 : 0;
            down[1] += Screen.hasControlDown() ? 1 : 0;
        };
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, counting);
        ctx.afterTest(() -> {
            NeoForge.EVENT_BUS.unregister(counting);
            McpGame.turn(false);
        });
        pair(ctx, sent, key, port);
        ctx.then(0, () -> {
            down[0] = 0;
            down[1] = 0;
        });
        run(ctx, sent, key, port, new Step("key_hold", "{\"mapping\":\"key.jump\",\"with\":\"left.control\","
                + "\"ticks\":" + HELD_TICKS + "}", result -> ctx.assertTrue(result.get("held").getAsInt()
                == HELD_TICKS, "the hold ends once held: " + result)));
        ctx.thenAssert(SETTLE, () -> down[0] == HELD_TICKS && down[1] == HELD_TICKS, "held " + HELD_TICKS
                + " ticks, the jump mapping and Control with it are down that many ticks: " + down[0] + " and "
                + down[1])
                .thenAssert(0, () -> !ctx.mc().options.keyJump.isDown() && !Screen.hasControlDown(),
                        "then both are let go");
    }

    /*
     * What a screen draws itself is named, and the game's own widgets are found where they nest: a screen of the
     * Core's kit names its tabs, clicked by name and by their words; the sound mixer's tabs, buttons of the game's tab
     * bar held in it, are listed with their words and clicked by them; a name nothing on the screen has is refused.
     */
    @ClientTest(timeoutTicks = 1600)
    public static void screens_nameWhatTheyDrawAndListTheWidgetsTheirBarsHold(final ClientTestContext ctx) {
        final AtomicReference<CompletableFuture<HttpResponse<String>>> sent = new AtomicReference<>();
        final AtomicReference<String> key = new AtomicReference<>();
        final int[] port = new int[1];
        final TabbedScreen kit = new TabbedScreen(Component.literal(KIT_TITLE)) {
            {
                addTab(new KitTab(KIT_TABS[0]));
                addTab(new KitTab(KIT_TABS[1]));
            }
        };
        ctx.afterTest(() -> {
            McpGame.turn(false);
            ctx.mc().setScreen(null);
        });
        pair(ctx, sent, key, port);
        ctx.then(SETTLE, () -> ctx.mc().setScreen(kit));
        run(ctx, sent, key, port, new Step("screen_tree", "{}", result -> ctx.assertTrue(named(result, "tab:0",
                KIT_TABS[0]) && named(result, "tab:1", KIT_TABS[1]), "the kit's screen names its tabs by their "
                + "titles: " + result)));
        run(ctx, sent, key, port, new Step("screen_click", "{\"name\":\"tab:1\"}", result -> { }));
        ctx.thenAssert(SETTLE, () -> kit.activeTabIndex() == 1, "clicked by its name, the second tab is chosen");
        run(ctx, sent, key, port, new Step("screen_click", "{\"words\":\"" + KIT_TABS[0].toLowerCase(Locale.ROOT)
                + "\"}", result -> { }));
        ctx.thenAssert(SETTLE, () -> kit.activeTabIndex() == 0, "clicked by its words, whatever their case, the "
                + "first tab is chosen again");
        run(ctx, sent, key, port, Step.failing("screen_click", "{\"name\":\"tab:9\"}", result -> { }));
        ctx.then(SETTLE, () -> ctx.mc().setScreen(new SoundMixerScreen(null)))
                .thenAwaitScreen(SoundMixerScreen.class, WAIT);
        final String sounds = GameText.resolve(SoundMixerTexts.TAB_SOUNDS);
        run(ctx, sent, key, port, new Step("screen_tree", "{}", result -> ctx.assertTrue(worded(result, sounds),
                "the sound mixer's tab bar holds its tabs' buttons, listed with their words: " + result)));
        run(ctx, sent, key, port, new Step("screen_click", "{\"words\":\"" + sounds + "\"}", result -> { }));
        ctx.thenAssert(SETTLE, () -> ctx.screen(SoundMixerScreen.class).currentTab() != null
                && ctx.screen(SoundMixerScreen.class).currentTab().getTabTitle().getString().equals(sounds),
                "clicked by its words, the sounds' tab shows");
    }

    /*
     * A game held for its agent stands still between its calls and runs only while one runs: the world's clock stays
     * where it is while the agent thinks, goes on about as many ticks as a wait asks, then stands again; let go, it
     * runs on its own, unfrozen as it was before it was held.
     */
    @ClientTest(timeoutTicks = 1600)
    public static void hold_standsTheWorldStillBetweenCallsAndRunsItWhileOneWaits(final ClientTestContext ctx) {
        final AtomicReference<CompletableFuture<HttpResponse<String>>> sent = new AtomicReference<>();
        final AtomicReference<String> key = new AtomicReference<>();
        final int[] port = new int[1];
        final long[] clock = new long[2];
        ctx.afterTest(() -> {
            McpHold.letGo();
            McpGame.turn(false);
        });
        pair(ctx, sent, key, port);
        run(ctx, sent, key, port, new Step("game_hold", "{\"on\":true}", result -> ctx.assertTrue(result.has("held")
                && result.get("held").getAsBoolean(), "the game is held for the agent: " + result)));
        ctx.thenServer(SETTLE, level -> {
            ctx.assertTrue(level.tickRateManager().isFrozen(), "between calls the world stands frozen");
            clock[0] = level.getGameTime();
        })
                .thenServer(THINKS, level -> ctx.assertTrue(level.getGameTime() == clock[0], "while the agent "
                        + "thinks, the world's clock stands: " + clock[0] + " then " + level.getGameTime()));
        run(ctx, sent, key, port, new Step("wait_ticks", "{\"ticks\":" + HELD_TICKS + "}", result -> { }));
        ctx.thenServer(SETTLE, level -> {
            clock[1] = level.getGameTime();
            final long ran = clock[1] - clock[0];
            ctx.assertTrue(ran >= HELD_TICKS - SETTLE && ran <= HELD_TICKS + THINKS, "a wait of " + HELD_TICKS
                    + " ticks runs the world about as many: " + ran);
        })
                .thenServer(THINKS, level -> ctx.assertTrue(level.getGameTime() == clock[1]
                        && level.tickRateManager().isFrozen(), "and once it has answered, the world stands again"));
        run(ctx, sent, key, port, new Step("game_hold", "{\"on\":false}", result -> ctx.assertTrue(
                !result.get("held").getAsBoolean(), "the game is let go: " + result)));
        ctx.thenServer(THINKS, level -> ctx.assertTrue(!level.tickRateManager().isFrozen()
                && level.getGameTime() - clock[1] >= THINKS / 2, "let go, it runs on its own again, as it did"));
    }

    /*
     * What a mod tells an agent of its block is in what the player looks at, under the mod's id; a mod's reading that
     * fails is left out, the rest of the answer and the other mods' readings still given.
     */
    @ClientTest(timeoutTicks = 1600)
    public static void readings_aModsWordsOfItsBlockAreInWhatThePlayerLooksAt(final ClientTestContext ctx) {
        final AtomicReference<CompletableFuture<HttpResponse<String>>> sent = new AtomicReference<>();
        final AtomicReference<String> key = new AtomicReference<>();
        final int[] port = new int[1];
        final BlockPos machine = ctx.abs(MACHINE);
        declareReadings();
        ctx.afterTest(() -> McpGame.turn(false));
        pair(ctx, sent, key, port);
        run(ctx, sent, key, port, new Step("world_set_block", "{\"place\":" + place(machine) + ",\"block\":\""
                + TestKitMachine.BLOCK.getId() + "\"}", result -> { }));
        run(ctx, sent, key, port, new Step("player_look_at", "{\"place\":" + place(machine) + "}", result -> { }));
        run(ctx, sent, key, port, new Step("player_state", "{}", result -> {
            final JsonObject looked = result.getAsJsonObject("looking_at_block");
            final JsonObject readings = looked == null || !looked.has("readings") ? new JsonObject()
                    : looked.getAsJsonObject("readings");
            ctx.assertTrue(readings.has(JsTests.MODID) && WORKING.equals(readings.getAsJsonObject(JsTests.MODID)
                    .getAsJsonObject("machine").get("state").getAsString()), "the test mod's words of its machine "
                    + "are in what the player looks at: " + looked);
            ctx.assertTrue(!readings.has(BROKEN), "a reading that fails is left out: " + readings);
        }));
    }

    /*
     * Where things are: the player's respawn point, from the Core, and a place a mod tells of, ten blocks east of the
     * player facing north, told ten blocks off, level, its bearing east and a quarter turn to the right; asked by
     * kind, only places of that kind.
     */
    @ClientTest(timeoutTicks = 1600)
    public static void places_tellTheRespawnAndAModsPlaceWithTheirWayFromThePlayer(final ClientTestContext ctx) {
        final AtomicReference<CompletableFuture<HttpResponse<String>>> sent = new AtomicReference<>();
        final AtomicReference<String> key = new AtomicReference<>();
        final int[] port = new int[1];
        final BlockPos respawn = ctx.abs(WALK_TO);
        declareReadings();
        ctx.afterTest(() -> McpGame.turn(false));
        pair(ctx, sent, key, port);
        ctx.thenServer(0, level -> ctx.serverPlayer().setRespawnPosition(level.dimension(), respawn, 0, true, false));
        run(ctx, sent, key, port, new Step("places_near", "{}", result -> {
            final List<JsonObject> places = result.getAsJsonArray("places").asList().stream()
                    .map(JsonElement::getAsJsonObject).toList();
            ctx.assertTrue(places.stream().anyMatch(place -> "respawn".equals(text(place, "kind"))
                    && place.get("x").getAsDouble() == respawn.getX() + 0.5 && place.get("z").getAsDouble()
                    == respawn.getZ() + 0.5), "the respawn point is told where it is: " + places);
            final JsonObject mark = places.stream().filter(place -> MARK.equals(text(place, "kind"))).findFirst()
                    .orElseThrow(() -> new ClientTestFailure("the mod's place is told: " + places));
            ctx.assertTrue(Math.abs(mark.get("distance").getAsDouble() - MARK_EAST) < 0.2
                    && Math.abs(mark.get("bearing").getAsDouble() + 90) < 1
                    && Math.abs(mark.get("turn").getAsDouble() - 90) < 1
                    && Math.abs(mark.get("climb").getAsDouble()) < 1, "ten blocks off, east, a quarter turn to the "
                    + "right and level: " + mark);
        }));
        run(ctx, sent, key, port, new Step("places_near", "{\"kind\":\"respawn\"}", result -> ctx.assertTrue(
                result.getAsJsonArray("places").asList().stream().allMatch(place -> "respawn".equals(text(
                        place.getAsJsonObject(), "kind"))), "asked by kind, only places of that kind: " + result)));
    }

    /*
     * What blocks lie round the player: two sponges laid by the player are counted by their id, the first found one of
     * them; asked of a mod alone, only that mod's blocks are counted.
     */
    @ClientTest(timeoutTicks = 1600)
    public static void survey_countsTheBlocksRoundThePlayerByTheirId(final ClientTestContext ctx) {
        final AtomicReference<CompletableFuture<HttpResponse<String>>> sent = new AtomicReference<>();
        final AtomicReference<String> key = new AtomicReference<>();
        final int[] port = new int[1];
        final List<BlockPos> laid = List.of(ctx.abs(WALK_TO), ctx.abs(WALK_TO.north()));
        ctx.afterTest(() -> {
            McpGame.turn(false);
            ctx.server().execute(() -> laid.forEach(at -> ctx.serverLevel().setBlockAndUpdate(at,
                    Blocks.AIR.defaultBlockState())));
        });
        pair(ctx, sent, key, port);
        ctx.thenServer(0, level -> laid.forEach(at -> level.setBlockAndUpdate(at,
                Blocks.SPONGE.defaultBlockState())));
        run(ctx, sent, key, port, new Step("blocks_survey", "{\"radius\":1,\"words\":\"minecraft:sponge\"}",
                result -> {
                    final JsonObject sponge = result.getAsJsonArray("blocks").asList().stream()
                            .map(JsonElement::getAsJsonObject).filter(one -> "minecraft:sponge".equals(text(one, "id")))
                            .findFirst().orElseThrow(() -> new ClientTestFailure("the sponges are found: " + result));
                    final BlockPos first = new BlockPos(sponge.get("x").getAsInt(), sponge.get("y").getAsInt(),
                            sponge.get("z").getAsInt());
                    ctx.assertTrue(sponge.get("count").getAsLong() == laid.size() && laid.contains(first),
                            "both sponges are counted, the first found one of them: " + sponge);
                }));
        run(ctx, sent, key, port, new Step("blocks_survey", "{\"radius\":1,\"mod\":\"jscore\"}",
                result -> ctx.assertTrue(result.getAsJsonArray("blocks").asList().stream().allMatch(one -> text(
                        one.getAsJsonObject(), "id").startsWith("jscore:")), "asked of a mod, only its blocks: "
                        + result)));
    }

    /*
     * The recipes the game holds: the furnace's, a crafting table's, takes eight slots any of cobblestone does; a test
     * machine's pressing takes two iron ingots, coal and water and makes an iron block, charcoal and lava; a refining,
     * making oxygen, takes hydrogen and needs a blaze rod; and a kind is listed with the machine that works it.
     */
    @ClientTest(timeoutTicks = 1600)
    public static void recipes_tellWhatEachTakesAndMakes(final ClientTestContext ctx) {
        final AtomicReference<CompletableFuture<HttpResponse<String>>> sent = new AtomicReference<>();
        final AtomicReference<String> key = new AtomicReference<>();
        final int[] port = new int[1];
        ctx.afterTest(() -> McpGame.turn(false));
        pair(ctx, sent, key, port);
        run(ctx, sent, key, port, new Step("recipes_read", "{\"kind\":\"minecraft:crafting\","
                + "\"makes\":\"minecraft:furnace\"}", result -> {
                    final JsonObject furnace = recipe(result, "minecraft:furnace");
                    final List<JsonElement> takes = furnace.getAsJsonArray("takes").asList();
                    ctx.assertTrue(takes.size() == FURNACE_SLOTS && takes.stream().allMatch(slot -> holds(slot,
                            "items", "minecraft:cobblestone")), "the furnace takes eight of cobblestone: " + furnace);
                }));
        run(ctx, sent, key, port, new Step("recipes_read", "{\"mod\":\"jstests\",\"kind\":\"test_pressing\","
                + "\"makes\":\"minecraft:lava\"}", result -> {
                    final JsonObject pressing = recipe(result, "jstests:test_pressing/iron_block");
                    final List<JsonElement> takes = pressing.getAsJsonArray("takes").asList();
                    ctx.assertTrue(takes.stream().anyMatch(slot -> holds(slot, "items", "minecraft:iron_ingot")
                            && slot.getAsJsonObject().get("count").getAsInt() == 2)
                            && takes.stream().anyMatch(slot -> holds(slot, "items", "minecraft:coal"))
                            && takes.stream().anyMatch(slot -> holds(slot, "fluids", "minecraft:water")),
                            "the pressing takes two iron ingots, coal and water: " + pressing);
                    final List<JsonElement> makes = pressing.getAsJsonArray("makes").asList();
                    ctx.assertTrue(makes.stream().anyMatch(slot -> holds(slot, "items", "minecraft:iron_block"))
                            && makes.stream().anyMatch(slot -> holds(slot, "items", "minecraft:charcoal"))
                            && makes.stream().anyMatch(slot -> holds(slot, "fluids", "minecraft:lava")),
                            "the pressing makes an iron block, charcoal and lava: " + pressing);
                }));
        run(ctx, sent, key, port, new Step("recipes_read", "{\"kind\":\"test_refining\","
                + "\"makes\":\"jscore:oxygen\"}", result -> {
                    ctx.assertTrue(result.getAsJsonArray("kinds").asList().stream().map(JsonElement::getAsJsonObject)
                            .anyMatch(kind -> "jstests:test_refining".equals(text(kind, "id"))
                                    && holds(kind, "stations", "jstests:test_kit_machine")),
                            "the refining kind is listed with the kit machine that works it: " + result.get("kinds"));
                    final JsonObject refining = recipe(result, "jstests:test_refining/nuggets");
                    ctx.assertTrue(refining.getAsJsonArray("takes").asList().stream().anyMatch(slot -> holds(slot,
                            "matter", "jscore:hydrogen")) && refining.getAsJsonArray("needs").asList().stream()
                            .anyMatch(slot -> holds(slot, "items", "minecraft:blaze_rod")),
                            "the refining takes hydrogen and needs a blaze rod it does not use up: " + refining);
                }));
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

    /*
     * The test mod's reading of its machine, one of another mod that always fails, and a place of the test mod ten
     * blocks east of the player's eyes, declared once however many times the tests run.
     */
    private static void declareReadings() {
        if (readingsDeclared) {
            return;
        }
        readingsDeclared = true;
        AgentReadings.block(JsTests.MODID, (level, pos, report) -> {
            if (level.getBlockState(pos).is(TestKitMachine.BLOCK.get())) {
                report.part("machine").words("state", WORKING);
            }
        });
        AgentReadings.block(BROKEN, (level, pos, report) -> {
            throw new IllegalStateException("a reading that fails, for the test");
        });
        AgentPlaces.declare(JsTests.MODID, (level, player, out) -> out.accept(new AgentPlace(MARK, "east",
                player.getEyePosition().add(MARK_EAST, 0, 0))));
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
                    ctx.assertTrue(result != null && result.has("isError") == step.fails(), step.tool() + " answers"
                            + (step.fails() ? ", failing: " : ": ") + answer);
                    step.check().accept(result.has("structuredContent") ? result.getAsJsonObject(
                            "structuredContent") : new JsonObject());
                });
    }

    /* How many steps of {@code kind} the recording keeps. */
    private static long count(final Class<? extends RecordedTest.Step> kind) {
        return TestRecorder.steps().stream().filter(kind::isInstance).count();
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

    /* Whether a screen's tree names a part {@code name} that reads {@code words}. */
    private static boolean named(final JsonObject tree, final String name, final String words) {
        return tree.has("parts") && tree.getAsJsonArray("parts").asList().stream().map(JsonElement::getAsJsonObject)
                .anyMatch(part -> name.equals(text(part, "name")) && words.equals(text(part, "words")));
    }

    /* Whether a screen's tree has a widget that reads {@code words}. */
    private static boolean worded(final JsonObject tree, final String words) {
        return tree.has("widgets") && tree.getAsJsonArray("widgets").asList().stream()
                .map(JsonElement::getAsJsonObject).anyMatch(widget -> words.equals(text(widget, "words")));
    }

    /* The recipe {@code id} among those an answer of recipes_read holds. */
    private static JsonObject recipe(final JsonObject result, final String id) {
        return result.getAsJsonArray("recipes").asList().stream().map(JsonElement::getAsJsonObject)
                .filter(one -> id.equals(text(one, "id"))).findFirst()
                .orElseThrow(() -> new ClientTestFailure("the recipe " + id + " is read: " + result));
    }

    /* Whether a recipe's slot, or a kind, holds {@code id} among its {@code what}: items, fluids, matter, stations. */
    private static boolean holds(final JsonElement slot, final String what, final String id) {
        final JsonObject one = slot.getAsJsonObject();
        return one.has(what) && one.getAsJsonArray(what).asList().stream()
                .anyMatch(each -> id.equals(each.getAsString()));
    }

    /* The words under {@code name} in an answer, or none. */
    private static @Nullable String text(final JsonObject result, final String name) {
        return result.has(name) && result.get(name).isJsonPrimitive() ? result.get(name).getAsString() : null;
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
    private record Step(String tool, Supplier<String> arguments, Consumer<JsonObject> check, boolean fails) {

        Step(final String tool, final Supplier<String> arguments, final Consumer<JsonObject> check) {
            this(tool, arguments, check, false);
        }

        Step(final String tool, final String arguments, final Consumer<JsonObject> check) {
            this(tool, () -> arguments, check, false);
        }

        /* A step whose tool is to fail, and say why. */
        static Step failing(final String tool, final String arguments, final Consumer<JsonObject> check) {
            return new Step(tool, () -> arguments, check, true);
        }
    }

    /* A tab of the kit's screen that draws nothing, known by its title. */
    private record KitTab(String name) implements IGuiTab {

        @Override
        public Component title() {
            return Component.literal(this.name);
        }

        @Override
        public void renderContent(final GuiGraphics graphics, final int contentX, final int contentY,
                                  final int contentWidth, final int contentHeight, final int mouseX, final int mouseY,
                                  final float partialTick) {
            // Nothing is drawn in it: the test reads its tab.
        }
    }
}
