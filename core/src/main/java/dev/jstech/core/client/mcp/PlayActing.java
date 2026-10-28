/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.mcp;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.InputConstants;
import dev.jstech.core.client.developer.ScreenPoints;
import dev.jstech.core.client.gui.data.BuiltNode;
import dev.jstech.core.client.gui.data.IDataScreen;
import dev.jstech.core.client.studio.TestRecorder;
import dev.jstech.core.developer.mcp.McpCall;
import dev.jstech.core.developer.mcp.McpPermission;
import dev.jstech.core.developer.mcp.McpResult;
import dev.jstech.core.developer.mcp.McpSchema;
import dev.jstech.core.developer.mcp.McpTool;
import dev.jstech.core.developer.mcp.McpWords;
import dev.jstech.core.developer.mcp.WorldTools;
import dev.jstech.core.developer.studio.RecordedTest;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * What an agent does as the player, through the input the player uses: the movement keys held, the view turned as
 * the mouse turns it, the use and attack keys, the hotbar's keys, and a screen's own clicks, typing and keys. While
 * the studio records a test, what an agent does on a screen is kept as its steps.
 */
public final class PlayActing {

    private static final double ARRIVED = 0.6;
    private static final int WALK_TICKS = 600;
    private static final int BREAK_TICKS = 400;
    private static final int OPEN_TICKS = 40;
    private static final int SLOTS = 9;
    /* How long a flight up or down may take, in ticks: four minutes, a climb of well over a thousand blocks. */
    private static final int FLY_TICKS = 4_800;
    /* The heights a flight may be asked to reach. */
    private static final int MIN_HEIGHT = -2_048;
    private static final int MAX_HEIGHT = 30_000;

    private PlayActing() {
    }

    /** The tools. */
    public static List<McpTool> all() {
        return List.of(
                new McpTool("player_walk_to", "Walks the player to a place as the movement keys do, jumping over a "
                        + "block in the way, and answers once there; any key the player presses stops it.",
                        McpSchema.object().place(Play.PLACE, "Where to walk.", true).flag("sprint",
                                "Whether to sprint.").build(), McpPermission.ACT, PlayActing::walk),
                new McpTool("player_jump", "Jumps, as the jump key does.", McpSchema.object().build(),
                        McpPermission.ACT, PlayActing::jump),
                new McpTool("player_fly", "Flies the player straight up or down to a height, as the jump and sneak "
                        + "keys held in flight do, taking to the air first where the player may fly, and answers "
                        + "once there, or once the flight has taken them into another dimension; any key the player "
                        + "presses stops it.", McpSchema.object()
                        .whole("height", "The height to fly to.", MIN_HEIGHT, MAX_HEIGHT, true).build(),
                        McpPermission.ACT, PlayActing::fly),
                new McpTool("player_sneak", "Holds the sneak key down, or lets it go.", McpSchema.object().flag("down",
                        "Whether to hold it down.").build(), McpPermission.ACT, call -> Play.withPlayer(player -> {
                            Minecraft.getInstance().options.keyShift.setDown(call.flag("down", true));
                            return done("sneaking", call.flag("down", true));
                        })),
                new McpTool("player_look_at", "Turns the player to look at the middle of a block, of one of its "
                        + "faces, or at an entity, as the mouse turns them.", McpSchema.object()
                        .place(Play.PLACE, "The block.", false).oneOf("face", "Which face of it.", faces(), false)
                        .whole("entity", "An entity's id, as player_state gives it.", 0, Integer.MAX_VALUE, false)
                        .build(), McpPermission.ACT, call -> Play.withPlayer(player -> look(player, call))),
                new McpTool("hotbar_select", "Picks a slot of the hotbar, as its number key does.", McpSchema.object()
                        .whole("slot", "The slot, from 0 at the left.", 0, SLOTS - 1, true).build(), McpPermission.ACT,
                        call -> Play.withPlayer(player -> select(call))),
                new McpTool("player_use", "Uses what the player holds on what they look at, as the use key does; "
                        + "with a place, looks at it first.", McpSchema.object().place(Play.PLACE,
                        "What to look at first.", false).oneOf("face", "Which face of it.", faces(), false).build(),
                        McpPermission.ACT, call -> use(call, false)),
                new McpTool("player_place", "Places the block the player holds against a face of a block, as the use "
                        + "key does.", McpSchema.object().place(Play.PLACE, "The block to place against.", true)
                        .oneOf("face", "Which face of it.", faces(), true).build(), McpPermission.ACT,
                        call -> use(call, false)),
                new McpTool("block_open", "Uses a block, as the use key does, and answers with the screen it opens.",
                        McpSchema.object().place(Play.PLACE, "The block.", true).build(), McpPermission.ACT,
                        call -> use(call, true)),
                new McpTool("player_break", "Breaks a block as the attack key held on it does, and answers once it is "
                        + "gone.", McpSchema.object().place(Play.PLACE, "The block.", true).build(),
                        McpPermission.ACT, PlayActing::breakBlock),
                new McpTool("screen_click", "Clicks the screen open as the mouse does: a component by its name (as "
                        + "screen_tree names it), or a point of the screen.", McpSchema.object()
                        .words("name", "The component's name.", false)
                        .whole("x", "The point's x, in the screen's pixels.", 0, 10_000, false)
                        .whole("y", "The point's y.", 0, 10_000, false)
                        .whole("button", "0 for the left button, 1 for the right.", 0, 1, false).build(),
                        McpPermission.ACT, call -> onScreen(call, PlayActing::click)),
                new McpTool("screen_type", "Types words into the screen open, a character at a time, as the keyboard "
                        + "does.", McpSchema.object().words("text", "What to type.", true).build(),
                        McpPermission.ACT, call -> onScreen(call, PlayActing::type)),
                new McpTool("key_press", "Presses a key, by its name (escape, enter, tab, e, f3...): on the screen "
                        + "open, or in the world, where the actions bound to it act.", McpSchema.object()
                        .words("key", "The key's name.", true).build(), McpPermission.ACT, PlayActing::key),
                new McpTool("screen_close", "Closes the screen open, as Escape does.", McpSchema.object().build(),
                        McpPermission.ACT, call -> onScreen(call, (screen, asked) -> {
                            screen.onClose();
                            TestRecorder.agentDid(new RecordedTest.Close());
                            return done("closed", true);
                        })));
    }

    private static List<String> faces() {
        return List.of("down", "up", "north", "south", "west", "east");
    }

    private static Optional<Direction> face(final McpCall call) {
        return call.text("face").map(Direction::byName);
    }

    private static McpResult done(final String what, final boolean value) {
        final JsonObject out = new JsonObject();
        out.addProperty(what, value);
        return McpResult.json(out);
    }

    private static CompletableFuture<McpResult> walk(final McpCall call) {
        final Optional<BlockPos> target = Play.place(call, Play.PLACE);
        if (target.isEmpty()) {
            return WorldTools.done(McpCall.wrong(Play.PLACE, McpWords.A_PLACE.text()));
        }
        final boolean sprint = call.flag("sprint", false);
        final Vec3 goal = Vec3.atBottomCenterOf(target.get());
        return PlayTasks.run(call, WALK_TICKS, new PlayTasks.ITask() {
            @Override
            public Optional<McpResult> step(final Minecraft game, final int tick) {
                final LocalPlayer player = game.player;
                if (player == null) {
                    return Optional.of(McpResult.failed(McpTexts.NO_WORLD.text()));
                }
                final double dx = goal.x - player.getX();
                final double dz = goal.z - player.getZ();
                if (Math.sqrt(dx * dx + dz * dz) < ARRIVED) {
                    final JsonObject out = new JsonObject();
                    out.add("arrived", Play.place(player.blockPosition()));
                    return Optional.of(McpResult.json(out));
                }
                final float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
                player.setYRot(yaw);
                player.setYHeadRot(yaw);
                final Options options = game.options;
                options.keyUp.setDown(true);
                options.keySprint.setDown(sprint);
                // Into a block one high, the jump key carries the player over it, as a player would.
                options.keyJump.setDown(player.horizontalCollision && player.onGround());
                return Optional.empty();
            }

            @Override
            public void release(final Minecraft game) {
                game.options.keyUp.setDown(false);
                game.options.keySprint.setDown(false);
                game.options.keyJump.setDown(false);
            }
        });
    }

    private static CompletableFuture<McpResult> fly(final McpCall call) {
        final double height = call.whole("height").orElse(0L);
        final Object[] from = new Object[1];
        return PlayTasks.run(call, FLY_TICKS, new PlayTasks.ITask() {
            @Override
            public Optional<McpResult> step(final Minecraft game, final int tick) {
                final LocalPlayer player = game.player;
                if (player == null || game.level == null) {
                    return Optional.of(McpResult.failed(McpTexts.NO_WORLD.text()));
                }
                if (!player.getAbilities().mayfly) {
                    return Optional.of(done("flying", false));
                }
                from[0] = from[0] == null ? game.level.dimension() : from[0];
                if (!game.level.dimension().equals(from[0])) {
                    final JsonObject out = new JsonObject();
                    out.addProperty("dimension", game.level.dimension().location().toString());
                    out.addProperty("height", player.getY());
                    return Optional.of(McpResult.json(out));
                }
                if (!player.getAbilities().flying) {
                    // Taking to the air, as the jump key pressed twice does.
                    player.getAbilities().flying = true;
                    player.onUpdateAbilities();
                }
                final double off = height - player.getY();
                if (Math.abs(off) < ARRIVED) {
                    final JsonObject out = new JsonObject();
                    out.addProperty("height", player.getY());
                    return Optional.of(McpResult.json(out));
                }
                game.options.keyJump.setDown(off > 0);
                game.options.keyShift.setDown(off < 0);
                return Optional.empty();
            }

            @Override
            public void release(final Minecraft game) {
                game.options.keyJump.setDown(false);
                game.options.keyShift.setDown(false);
            }
        });
    }

    private static CompletableFuture<McpResult> jump(final McpCall call) {
        return PlayTasks.run(call, 40, new PlayTasks.ITask() {
            @Override
            public Optional<McpResult> step(final Minecraft game, final int tick) {
                game.options.keyJump.setDown(tick <= 2);
                return tick >= 10 ? Optional.of(done("jumped", true)) : Optional.empty();
            }

            @Override
            public void release(final Minecraft game) {
                game.options.keyJump.setDown(false);
            }
        });
    }

    private static McpResult look(final LocalPlayer player, final McpCall call) {
        final Optional<Long> entityId = call.whole("entity");
        if (entityId.isPresent()) {
            final Entity entity = player.level().getEntity(entityId.get().intValue());
            if (entity == null) {
                return McpCall.wrong("entity", McpWords.AN_ID.text());
            }
            Play.lookAt(player, entity.getEyePosition());
            return done("looking", true);
        }
        final Optional<BlockPos> pos = Play.place(call, Play.PLACE);
        if (pos.isEmpty()) {
            return McpCall.wrong(Play.PLACE, McpWords.A_PLACE.text());
        }
        Play.lookAt(player, face(call).map(face -> Play.faceOf(pos.get(), face)).orElse(Vec3.atCenterOf(
                pos.get())));
        return done("looking", true);
    }

    private static McpResult select(final McpCall call) {
        final Optional<Long> slot = call.whole("slot").filter(s -> s >= 0 && s < SLOTS);
        if (slot.isEmpty()) {
            return McpCall.wrong("slot", McpWords.A_NUMBER.with(0, SLOTS - 1));
        }
        KeyMapping.click(Minecraft.getInstance().options.keyHotbarSlots[slot.get().intValue()].getKey());
        return done("selected", true);
    }

    /* Looks at the place given, if any, then presses the use key; opening, it waits for the screen it opens. */
    private static CompletableFuture<McpResult> use(final McpCall call, final boolean opening) {
        final Optional<BlockPos> pos = Play.place(call, Play.PLACE);
        if (opening && pos.isEmpty()) {
            return WorldTools.done(McpCall.wrong(Play.PLACE, McpWords.A_PLACE.text()));
        }
        final Optional<Direction> face = face(call);
        return PlayTasks.run(call, OPEN_TICKS + 10, (game, tick) -> {
            final LocalPlayer player = game.player;
            if (player == null) {
                return Optional.of(McpResult.failed(McpTexts.NO_WORLD.text()));
            }
            if (tick == 1) {
                pos.ifPresent(at -> Play.lookAt(player, face.map(side -> Play.faceOf(at, side)).orElse(
                        Vec3.atCenterOf(at))));
                return Optional.empty();
            }
            if (tick == 2) {
                // The view turned last tick, so the game has picked what is looked at since.
                KeyMapping.click(game.options.keyUse.getKey());
                return Optional.empty();
            }
            if (!opening) {
                return tick >= 4 ? Optional.of(done("used", true)) : Optional.empty();
            }
            if (game.screen != null) {
                final JsonObject out = new JsonObject();
                out.addProperty("kind", game.screen.getClass().getSimpleName());
                out.addProperty("title", game.screen.getTitle().getString());
                return Optional.of(McpResult.json(out));
            }
            return tick >= OPEN_TICKS ? Optional.of(McpResult.failed(McpTexts.NOTHING_OPENED.with(
                    BuiltInRegistries.BLOCK.getKey(player.level().getBlockState(pos.get()).getBlock())))) : Optional
                    .empty();
        });
    }

    /* Breaks the block as holding the attack key on it does: the game mode's own digging, tick by tick. */
    private static CompletableFuture<McpResult> breakBlock(final McpCall call) {
        final Optional<BlockPos> pos = Play.place(call, Play.PLACE);
        if (pos.isEmpty()) {
            return WorldTools.done(McpCall.wrong(Play.PLACE, McpWords.A_PLACE.text()));
        }
        return PlayTasks.run(call, BREAK_TICKS, (game, tick) -> {
            final LocalPlayer player = game.player;
            if (player == null || game.gameMode == null) {
                return Optional.of(McpResult.failed(McpTexts.NO_WORLD.text()));
            }
            if (player.level().getBlockState(pos.get()).isAir()) {
                return Optional.of(done("broken", true));
            }
            Play.lookAt(player, Vec3.atCenterOf(pos.get()));
            final Direction side = Direction.getNearest(player.getEyePosition().subtract(Vec3.atCenterOf(
                    pos.get())));
            if (tick == 1) {
                game.gameMode.startDestroyBlock(pos.get(), side);
            } else {
                game.gameMode.continueDestroyBlock(pos.get(), side);
            }
            player.swing(InteractionHand.MAIN_HAND);
            return Optional.empty();
        });
    }

    private static CompletableFuture<McpResult> onScreen(final McpCall call, final IOnScreen act) {
        final Minecraft game = Minecraft.getInstance();
        return game.submit(() -> game.screen == null ? McpResult.failed(McpTexts.NO_SCREEN.text())
                : act.act(game.screen, call));
    }

    private static McpResult click(final Screen screen, final McpCall call) {
        final int button = (int) (long) call.whole("button").orElse(0L);
        final Optional<String> name = call.text("name");
        final double x;
        final double y;
        if (name.isPresent()) {
            if (!(screen instanceof IDataScreen data)) {
                return McpCall.wrong("name", McpWords.SOME_WORDS.text());
            }
            final Optional<BuiltNode> node = data.find(name.get());
            if (node.isEmpty() || node.get().component() == null) {
                return McpCall.wrong("name", McpWords.SOME_WORDS.text());
            }
            final int[] center = node.get().component().center();
            x = center[0];
            y = center[1];
        } else {
            final Optional<Long> atX = call.whole("x");
            final Optional<Long> atY = call.whole("y");
            if (atX.isEmpty() || atY.isEmpty()) {
                return McpCall.wrong("x", McpWords.A_NUMBER.with(0, screen.width));
            }
            x = atX.get();
            y = atY.get();
        }
        screen.mouseClicked(x, y, button);
        screen.mouseReleased(x, y, button);
        final Optional<String> named = name.isPresent() ? name : screen instanceof IDataScreen data
                ? ScreenPoints.nameAt(data, x, y) : Optional.empty();
        TestRecorder.agentDid(named.<RecordedTest.Step>map(RecordedTest.Click::new).orElseGet(() ->
                new RecordedTest.ClickAt((int) Math.round(x - screen.width / 2.0), (int) Math.round(y
                        - screen.height / 2.0))));
        return done("clicked", true);
    }

    private static McpResult type(final Screen screen, final McpCall call) {
        final String text = call.text("text").orElse("");
        for (final char c : text.toCharArray()) {
            screen.charTyped(c, 0);
        }
        TestRecorder.agentDid(new RecordedTest.Type(text));
        return done("typed", true);
    }

    private static CompletableFuture<McpResult> key(final McpCall call) {
        final Optional<String> name = call.text("key");
        final InputConstants.Key key;
        try {
            key = InputConstants.getKey("key.keyboard." + name.orElse("").strip().toLowerCase(Locale.ROOT));
        } catch (final IllegalArgumentException unknown) {
            return WorldTools.done(McpCall.wrong("key", McpWords.SOME_WORDS.text()));
        }
        if (key == InputConstants.UNKNOWN) {
            return WorldTools.done(McpCall.wrong("key", McpWords.SOME_WORDS.text()));
        }
        final Minecraft game = Minecraft.getInstance();
        return game.submit(() -> {
            if (game.screen != null) {
                game.screen.keyPressed(key.getValue(), 0, 0);
                if (game.screen != null) {
                    game.screen.keyReleased(key.getValue(), 0, 0);
                }
            } else {
                KeyMapping.click(key);
            }
            TestRecorder.agentDid(new RecordedTest.Key(key.getValue()));
            return done("pressed", true);
        });
    }

    /** What acts on the screen open. */
    @FunctionalInterface
    private interface IOnScreen {

        McpResult act(Screen screen, McpCall call);
    }
}
