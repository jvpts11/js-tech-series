/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.mcp;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.InputConstants;
import dev.jstech.core.client.developer.ScreenGestures;
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
import dev.jstech.core.multiblock.shape.FormedPieceAim;
import java.util.ArrayList;
import java.util.Comparator;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * What an agent does as the player, through the input the player uses: the movement keys held, the view turned as
 * the mouse turns it, to a block, a point of it or an entity, the use and attack keys, the hotbar's keys, and a
 * screen's own clicks, drags, turns of the wheel, typing and keys. While the studio records a test, what an agent does
 * on a screen is kept as its steps.
 */
public final class PlayActing {

    private static final double ARRIVED = 0.6;
    /*
     * A walk's ways: how near a place on the way counts as reached, the height a step is walked up without a jump,
     * the ticks between two jumps (more than a double press, which takes a player who may fly into the air), how
     * near a shut door is opened from, and how long the player may come no nearer before the way counts as blocked.
     */
    private static final double ARRIVED_SPOT = 0.35;
    private static final double STEP = 0.6;
    private static final int JUMP_GAP = 10;
    private static final double DOOR_REACH = 2.0;
    private static final double PROGRESS = 0.05;
    private static final int STUCK_TICKS = 40;
    private static final int WALK_TICKS = 600;
    private static final int BREAK_TICKS = 400;
    private static final int OPEN_TICKS = 40;
    private static final int SLOTS = 9;
    /* How long a flight up or down may take, in ticks: four minutes, a climb of well over a thousand blocks. */
    private static final int FLY_TICKS = 4_800;
    /* The heights a flight may be asked to reach. */
    private static final int MIN_HEIGHT = -2_048;
    private static final int MAX_HEIGHT = 30_000;
    /* The most a turn of the wheel may be asked to turn it, either way, in notches. */
    private static final int MOST_NOTCHES = 100;
    /* What the point a tool looks at is, for the tools that look at a block. */
    /* What a look by name asks for, how far round the player it looks, in blocks, and how long it may take. */
    private static final String PIECE = "piece";
    private static final String ENTITY_TYPE = "entity_type";
    private static final int SEARCH = 24;
    private static final int LOOK_TICKS = 40;
    /* What a hold of a key asks for, the most ticks it may last, four minutes, and the ticks it is given to let go. */
    private static final String MAPPING = "mapping";
    private static final String WITH = "with";
    private static final String TICKS = "ticks";
    private static final int MOST_HOLD = 4_800;
    private static final int LET_GO_TICKS = 5;
    private static final String AT_ABOUT = "A point of the block to look at, in sixteenths of it from its lowest "
            + "corner, as a model's coordinates count (0 to 16 each): a port or a button on a face; with a face, the "
            + "point is on that face. Its middle, or the face's, when left out.";

    private PlayActing() {
    }

    /** The tools. */
    public static List<McpTool> all() {
        return List.of(
                new McpTool("player_walk_to", "Walks the player to a place as the movement keys do, along a way a "
                        + "player walks: up steps, slabs and stairs, a block up with a jump, down three at most, "
                        + "through doors it opens; and answers once there, or where it stopped and why. Any key the "
                        + "player presses stops it.",
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
                new McpTool("player_look_at", "Turns the player to look at a block, the middle of it, of one of its "
                        + "faces or a point of it, at an entity, as the mouse turns them; or at the nearest piece of "
                        + "a formed model by its name (a seat, a console, a door, as player_state names a piece), "
                        + "at a point of it the look meets it at, or at the nearest entity of a type.",
                        McpSchema.object()
                        .place(Play.PLACE, "The block.", false).oneOf("face", "Which face of it.", faces(), false)
                        .place(Play.AT, AT_ABOUT, false)
                        .whole("entity", "An entity's id, as player_state gives it.", 0, Integer.MAX_VALUE, false)
                        .words(PIECE, "A piece of a formed model, by the name its model gives it.", false)
                        .words(ENTITY_TYPE, "A type of entity, as minecraft:pig.", false)
                        .build(), McpPermission.ACT, PlayActing::lookAt),
                new McpTool("hotbar_select", "Picks a slot of the hotbar, as its number key does.", McpSchema.object()
                        .whole("slot", "The slot, from 0 at the left.", 0, SLOTS - 1, true).build(), McpPermission.ACT,
                        call -> Play.withPlayer(player -> select(call))),
                new McpTool("player_use", "Uses what the player holds on what they look at, as the use key does; "
                        + "with a place, looks at it first: a point of it, such as a port or a button on a face, "
                        + "uses that part. The answer says what the use landed on; with a place, why it landed on "
                        + "something else (in the way, or behind a point off the block's shape), and why nothing is "
                        + "used when the look lands on nothing (out of reach, off the block's shape). The same holds "
                        + "for player_place and block_open.", McpSchema.object().place(Play.PLACE,
                        "What to look at first.", false)
                        .oneOf("face", "Which face of it.", faces(), false).place(Play.AT, AT_ABOUT, false)
                        .build(), McpPermission.ACT, call -> use(call, false)),
                new McpTool("player_place", "Places the block the player holds against a face of a block, as the use "
                        + "key does.", McpSchema.object().place(Play.PLACE, "The block to place against.", true)
                        .oneOf("face", "Which face of it.", faces(), true).place(Play.AT, AT_ABOUT, false)
                        .build(), McpPermission.ACT, call -> use(call, false)),
                new McpTool("block_open", "Uses a block, as the use key does, and answers with the screen it opens.",
                        McpSchema.object().place(Play.PLACE, "The block.", true).oneOf("face", "Which face of it.",
                        faces(), false).place(Play.AT, AT_ABOUT, false).build(), McpPermission.ACT,
                        call -> use(call, true)),
                new McpTool("player_break", "Breaks a block as the attack key held on it does, and answers once it is "
                        + "gone; or why it cannot: out of reach, a block nothing breaks, or the game mode keeping the "
                        + "player from it.", McpSchema.object().place(Play.PLACE, "The block.", true).build(),
                        McpPermission.ACT, PlayActing::breakBlock),
                new McpTool("screen_click", "Clicks the screen open as the mouse does: a component by its name (as "
                        + "screen_tree names it), or a point of the screen.", McpSchema.object()
                        .words("name", "The component's name.", false)
                        .whole("x", "The point's x, in the screen's pixels.", 0, 10_000, false)
                        .whole("y", "The point's y.", 0, 10_000, false)
                        .whole("button", "0 for the left button, 1 for the right.", 0, 1, false).build(),
                        McpPermission.ACT, call -> onScreen(call, PlayActing::click)),
                new McpTool("screen_drag", "Drags the mouse across the screen open, its button pressed at one point, "
                        + "moved in steps and let go at another: a slider, a slide to unlock, a thing carried.",
                        McpSchema.object()
                        .whole("x", "Where the drag starts, its x in the screen's pixels.", 0, 10_000, true)
                        .whole("y", "Where it starts, its y.", 0, 10_000, true)
                        .whole("to_x", "Where it ends, its x.", 0, 10_000, true)
                        .whole("to_y", "Where it ends, its y.", 0, 10_000, true)
                        .whole("button", "0 for the left button, 1 for the right.", 0, 1, false).build(),
                        McpPermission.ACT, call -> onScreen(call, PlayActing::drag)),
                new McpTool("screen_scroll", "Turns the mouse's wheel over a point of the screen open, as the wheel "
                        + "does: a list or a page scrolled, a map drawn nearer.", McpSchema.object()
                        .whole("x", "The point's x, in the screen's pixels.", 0, 10_000, true)
                        .whole("y", "The point's y.", 0, 10_000, true)
                        .whole("notches", "How far it turns: up when more than 0, down when less.", -MOST_NOTCHES,
                                MOST_NOTCHES, true)
                        .build(), McpPermission.ACT, call -> onScreen(call, PlayActing::scroll)),
                new McpTool("screen_type", "Types words into the screen open, a character at a time, as the keyboard "
                        + "does.", McpSchema.object().words("text", "What to type.", true).build(),
                        McpPermission.ACT, call -> onScreen(call, PlayActing::type)),
                new McpTool("key_press", "Presses a key, by its name (escape, enter, tab, e, f3...): on the screen "
                        + "open, or in the world, where the actions bound to it act.", McpSchema.object()
                        .words("key", "The key's name.", true).build(), McpPermission.ACT, PlayActing::key),
                new McpTool("key_hold", "Holds a key or a key mapping down for a number of ticks, as a finger on it "
                        + "does, then lets go: a key by its name (w, space, left.control...), or a mapping of the game "
                        + "or of a mod by the game's name for it (key.jump, key.sneak), with a key held with it, a "
                        + "modifier; through the game's own input, so a mapping that waits for Control, or a mod "
                        + "reading a key itself, finds it down.", McpSchema.object()
                        .words("key", "The key's name.", false)
                        .words(MAPPING, "A key mapping, by the game's name for it.", false)
                        .words(WITH, "A key held down with it, as left.control.", false)
                        .whole(TICKS, "How long it is held, in ticks.", 1, MOST_HOLD, true).build(),
                        McpPermission.ACT, PlayActing::hold),
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
        // The way found as the walk starts, the place on it walked to next, the tick of the last jump, and how near
        // the place walked to the player came, and since when no nearer.
        final PlayPaths.Way[] way = new PlayPaths.Way[1];
        final int[] next = {0};
        final int[] jumped = {-JUMP_GAP};
        final double[] nearest = {Double.MAX_VALUE};
        final int[] since = {0};
        return PlayTasks.run(call, WALK_TICKS, new PlayTasks.ITask() {
            @Override
            public Optional<McpResult> step(final Minecraft game, final int tick) {
                final LocalPlayer player = game.player;
                if (player == null) {
                    return Optional.of(McpResult.failed(McpTexts.NO_WORLD.text()));
                }
                if (tick == 1) {
                    if (player.getAbilities().flying) {
                        // Walking, a flying player comes down first, as the jump key pressed twice brings them.
                        player.getAbilities().flying = false;
                        player.onUpdateAbilities();
                    }
                    way[0] = PlayPaths.find(player.level(), player, target.get());
                }
                final Options options = game.options;
                final double dx = goal.x - player.getX();
                final double dz = goal.z - player.getZ();
                if (Math.sqrt(dx * dx + dz * dz) < ARRIVED && Math.abs(goal.y - player.getY()) < 1) {
                    final JsonObject out = new JsonObject();
                    out.add("arrived", Play.place(player.blockPosition()));
                    return Optional.of(McpResult.json(out));
                }
                final List<PlayPaths.Spot> spots = way[0].spots();
                if (next[0] >= spots.size()) {
                    return Optional.of(stopped(player, target.get(), way[0].reached() ? "blocked" : "no_way"));
                }
                final PlayPaths.Spot spot = spots.get(next[0]);
                final double toSpot = Math.hypot(spot.at().x - player.getX(), spot.at().z - player.getZ());
                if (spot.door() && player.level().getBlockState(spot.place()).getBlock() instanceof DoorBlock
                        && !player.level().getBlockState(spot.place()).getValue(DoorBlock.OPEN)) {
                    options.keyUp.setDown(false);
                    if (toSpot < DOOR_REACH && tick - jumped[0] > JUMP_GAP) {
                        // A shut door on the way is opened as a player opens it, with the use key, looking at it.
                        Play.lookAt(player, Vec3.atCenterOf(spot.place()));
                        KeyMapping.click(options.keyUse.getKey());
                        jumped[0] = tick;
                        return Optional.empty();
                    }
                }
                if (toSpot < ARRIVED_SPOT && Math.abs(spot.at().y - player.getY()) < STEP) {
                    next[0]++;
                    nearest[0] = Double.MAX_VALUE;
                    since[0] = 0;
                    return Optional.empty();
                }
                if (toSpot < nearest[0] - PROGRESS) {
                    nearest[0] = toSpot;
                    since[0] = 0;
                } else if (++since[0] > STUCK_TICKS) {
                    return Optional.of(stopped(player, spot.place(), "blocked"));
                }
                final float yaw = (float) Math.toDegrees(Math.atan2(-(spot.at().x - player.getX()),
                        spot.at().z - player.getZ()));
                player.setYRot(yaw);
                player.setYHeadRot(yaw);
                options.keyUp.setDown(true);
                options.keySprint.setDown(sprint);
                // Up a whole block the jump key carries the player, as a player's would; a step, a slab or a stair
                // is walked up. Never twice in a row, so a player who may fly never takes to the air.
                final boolean jump = spot.at().y - player.getY() > STEP && player.onGround()
                        && tick - jumped[0] > JUMP_GAP;
                if (jump) {
                    jumped[0] = tick;
                }
                options.keyJump.setDown(jump);
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

    /*
     * A walk that cannot go on, where the player stands: why, as the blocks before them toward {@code toward} show it,
     * a wall two blocks high or more, a step with no room over it for the head, a drop deeper than a player jumps
     * down, a shut door no hand opens; else {@code otherwise}, no way found or the way blocked.
     */
    private static McpResult stopped(final LocalPlayer player, final BlockPos toward, final String otherwise) {
        final Level level = player.level();
        final BlockPos at = player.blockPosition();
        final int dx = toward.getX() - at.getX();
        final int dz = toward.getZ() - at.getZ();
        final Direction side = Math.abs(dx) >= Math.abs(dz) ? dx >= 0 ? Direction.EAST : Direction.WEST
                : dz >= 0 ? Direction.SOUTH : Direction.NORTH;
        final BlockPos front = at.relative(side);
        final String why;
        if (level.getBlockState(front).getBlock() instanceof DoorBlock) {
            why = "shut_door";
        } else if (solid(level, front.above())) {
            why = "wall";
        } else if (solid(level, front) && solid(level, front.above(2))) {
            why = "no_headroom";
        } else if (!solid(level, front) && !solid(level, front.below()) && !solid(level, front.below(2))
                && !solid(level, front.below(3)) && !solid(level, front.below(4))) {
            why = "drop";
        } else {
            why = otherwise;
        }
        final JsonObject out = new JsonObject();
        out.addProperty(PlayAim.FAILED, why);
        out.add("stopped_at", Play.place(at));
        final JsonObject ahead = new JsonObject();
        ahead.addProperty("block", BuiltInRegistries.BLOCK.getKey(level.getBlockState(front).getBlock()).toString());
        ahead.add(Play.PLACE, Play.place(front));
        out.add("ahead", ahead);
        return McpResult.failed(McpTexts.WALK_STOPPED.with(at.getX() + " " + at.getY() + " " + at.getZ(), why))
                .with(out);
    }

    private static boolean solid(final Level level, final BlockPos pos) {
        return !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
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

    /* A look at a piece by its name takes a few ticks, the game picking what is looked at after each turn. */
    private static CompletableFuture<McpResult> lookAt(final McpCall call) {
        final Optional<String> piece = call.text(PIECE);
        if (piece.isPresent()) {
            return lookAtPiece(call, piece.get());
        }
        final Optional<String> type = call.text(ENTITY_TYPE);
        return Play.withPlayer(player -> type.isPresent() ? lookAtType(player, type.get()) : look(player, call));
    }

    /*
     * Looks at the nearest piece called {@code name}: at its middle and then its faces' middles, a point a tick, until
     * the game finds the look on the piece. One past the player's reach is looked at its middle, and said to be so.
     */
    private static CompletableFuture<McpResult> lookAtPiece(final McpCall call, final String name) {
        final List<Vec3> points = new ArrayList<>();
        final FormedPieceAim.Aimed[] target = new FormedPieceAim.Aimed[1];
        return PlayTasks.run(call, LOOK_TICKS, (game, tick) -> {
            final LocalPlayer player = game.player;
            if (player == null) {
                return Optional.of(McpResult.failed(McpTexts.NO_WORLD.text()));
            }
            if (tick == 1) {
                final List<FormedPieceAim.Aimed> near = PlayPieces.near(player.level(), player.getEyePosition(), name,
                        SEARCH);
                if (near.isEmpty()) {
                    final JsonObject out = new JsonObject();
                    out.addProperty(PlayAim.FAILED, "none_near");
                    return Optional.of(McpResult.failed(McpTexts.NONE_NEAR.with(name, SEARCH)).with(out));
                }
                target[0] = near.getFirst();
                points.addAll(PlayPieces.points(target[0].box(), player.getEyePosition()));
            }
            final double distance = Math.sqrt(target[0].box().distanceToSqr(player.getEyePosition()));
            if (distance > player.blockInteractionRange()) {
                Play.lookAt(player, points.getFirst());
                final JsonObject out = seen(target[0]);
                out.addProperty("within_reach", false);
                out.addProperty("distance", Math.round(distance * 10) / 10.0);
                return Optional.of(McpResult.json(out));
            }
            final int point = (tick - 1) / 2;
            if (tick % 2 == 1) {
                if (point >= points.size()) {
                    final JsonObject out = seen(target[0]);
                    out.addProperty(PlayAim.FAILED, "out_of_sight");
                    out.add("in_the_way", PlayAim.landedOn(game, player));
                    return Optional.of(McpResult.failed(McpTexts.OUT_OF_SIGHT.with(name)).with(out));
                }
                Play.lookAt(player, points.get(point));
                return Optional.empty();
            }
            // The view turned last tick: on the piece, the look is done; else the next point is tried.
            final Optional<JsonObject> looked = PlaySeeing.lookedAtBlock(game, player);
            final JsonObject found = looked.map(block -> block.getAsJsonObject("piece")).orElse(null);
            if (found != null && name.equals(found.get("name").getAsString())
                    && target[0].model().toString().equals(found.get("model").getAsString())) {
                final JsonObject out = seen(target[0]);
                out.addProperty("within_reach", true);
                return Optional.of(McpResult.json(out));
            }
            return Optional.empty();
        });
    }

    private static McpResult lookAtType(final LocalPlayer player, final String type) {
        final Optional<EntityType<?>> kind = EntityType.byString(type);
        if (kind.isEmpty()) {
            return McpCall.wrong(ENTITY_TYPE, McpWords.AN_ID.text());
        }
        final Vec3 eye = player.getEyePosition();
        final Optional<Entity> nearest = player.level().getEntities((Entity) player, new AABB(eye, eye).inflate(SEARCH),
                entity -> entity.getType() == kind.get()).stream().min(Comparator.comparingDouble(entity ->
                entity.distanceToSqr(eye)));
        if (nearest.isEmpty()) {
            final JsonObject out = new JsonObject();
            out.addProperty(PlayAim.FAILED, "none_near");
            return McpResult.failed(McpTexts.NONE_NEAR.with(type, SEARCH)).with(out);
        }
        Play.lookAt(player, nearest.get().getEyePosition());
        final JsonObject out = new JsonObject();
        out.addProperty("looking", true);
        out.addProperty("entity", nearest.get().getId());
        return McpResult.json(out);
    }

    /* The piece looked at, as an answer gives it: its model, its name and the box round it in the world. */
    private static JsonObject seen(final FormedPieceAim.Aimed piece) {
        final JsonObject out = new JsonObject();
        out.addProperty("looking", true);
        final JsonObject found = new JsonObject();
        found.addProperty("model", piece.model().toString());
        found.addProperty("name", piece.piece());
        final JsonObject box = new JsonObject();
        box.add("from", PlaySeeing.point(new Vec3(piece.box().minX, piece.box().minY, piece.box().minZ)));
        box.add("to", PlaySeeing.point(new Vec3(piece.box().maxX, piece.box().maxY, piece.box().maxZ)));
        found.add("box", box);
        out.add(PIECE, found);
        return out;
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
        final Optional<Vec3> point = Play.pointOf(call, pos.get(), face(call));
        if (point.isEmpty()) {
            return McpCall.wrong(Play.AT, McpWords.A_NUMBER.with(0, Play.SIXTEENTHS));
        }
        Play.lookAt(player, point.get());
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

    /*
     * Looks at the place given, if any, then presses the use key; opening, it waits for the screen it opens. A look
     * that lands on nothing is not used: the answer says why (the block out of reach, the point off its shape); one
     * that lands on something else is used as the player's would be, and the answer says what took it and why.
     */
    private static CompletableFuture<McpResult> use(final McpCall call, final boolean opening) {
        final Optional<BlockPos> pos = Play.place(call, Play.PLACE);
        if (opening && pos.isEmpty()) {
            return WorldTools.done(McpCall.wrong(Play.PLACE, McpWords.A_PLACE.text()));
        }
        final Optional<Vec3> point = pos.flatMap(at -> Play.pointOf(call, at, face(call)));
        if (pos.isPresent() && point.isEmpty()) {
            return WorldTools.done(McpCall.wrong(Play.AT, McpWords.A_NUMBER.with(0, Play.SIXTEENTHS)));
        }
        // What the use landed on, and what took it in place of the block asked, as the tick of the use read them.
        final JsonElement[] usedOn = {JsonNull.INSTANCE};
        final JsonObject[] instead = {new JsonObject()};
        return PlayTasks.run(call, OPEN_TICKS + 10, (game, tick) -> {
            final LocalPlayer player = game.player;
            if (player == null) {
                return Optional.of(McpResult.failed(McpTexts.NO_WORLD.text()));
            }
            if (tick == 1) {
                point.ifPresent(at -> Play.lookAt(player, at));
                return Optional.empty();
            }
            if (tick == 2) {
                // The view turned last tick, so the game has picked what is looked at since.
                if (pos.isPresent()) {
                    final Optional<McpResult> missed = PlayAim.missed(game, player, pos.get(), point.get());
                    if (missed.isPresent()) {
                        return missed;
                    }
                    instead[0] = PlayAim.instead(game, player, pos.get(), point.get()).orElseGet(JsonObject::new);
                }
                usedOn[0] = PlayAim.landedOn(game, player);
                KeyMapping.click(game.options.keyUse.getKey());
                return Optional.empty();
            }
            if (!opening) {
                return tick >= 4 ? Optional.of(used(usedOn[0], instead[0])) : Optional.empty();
            }
            if (game.screen != null) {
                final JsonObject out = new JsonObject();
                out.addProperty("kind", game.screen.getClass().getSimpleName());
                out.addProperty("title", game.screen.getTitle().getString());
                return Optional.of(McpResult.json(out));
            }
            return tick >= OPEN_TICKS ? Optional.of(McpResult.failed(McpTexts.NOTHING_OPENED.with(
                    BuiltInRegistries.BLOCK.getKey(player.level().getBlockState(pos.get()).getBlock())))
                    .with(instead[0])) : Optional.empty();
        });
    }

    /* A use done: what it landed on, a block, an entity or nothing, and what took it in place of the block asked. */
    private static McpResult used(final JsonElement usedOn, final JsonObject instead) {
        final JsonObject out = new JsonObject();
        out.addProperty("used", true);
        out.add(PlayAim.USED_ON, usedOn);
        instead.entrySet().forEach(entry -> out.add(entry.getKey(), entry.getValue()));
        return McpResult.json(out);
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
                final Optional<McpResult> refused = PlayAim.unbreakable(game, player, pos.get());
                if (refused.isPresent()) {
                    return refused;
                }
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
        ScreenGestures.click(screen, x, y, button);
        final Optional<String> named = name.isPresent() ? name : screen instanceof IDataScreen data
                ? ScreenPoints.nameAt(data, x, y) : Optional.empty();
        TestRecorder.agentDid(named.<RecordedTest.Step>map(RecordedTest.Click::new).orElseGet(() ->
                new RecordedTest.ClickAt((int) Math.round(x - screen.width / 2.0), (int) Math.round(y
                        - screen.height / 2.0))));
        return done("clicked", true);
    }

    private static McpResult drag(final Screen screen, final McpCall call) {
        final Optional<Long> x = call.whole("x");
        final Optional<Long> y = call.whole("y");
        final Optional<Long> toX = call.whole("to_x");
        final Optional<Long> toY = call.whole("to_y");
        if (x.isEmpty() || y.isEmpty() || toX.isEmpty() || toY.isEmpty()) {
            return McpCall.wrong("x", McpWords.A_NUMBER.with(0, screen.width));
        }
        final int button = (int) (long) call.whole("button").orElse(0L);
        ScreenGestures.drag(screen, x.get(), y.get(), toX.get(), toY.get(), button);
        // A recorded test drags with the left button, as the player's own drags are kept.
        if (button == 0) {
            TestRecorder.agentDid(new RecordedTest.Drag((int) Math.round(x.get() - screen.width / 2.0),
                    (int) Math.round(y.get() - screen.height / 2.0), (int) Math.round(toX.get() - screen.width / 2.0),
                    (int) Math.round(toY.get() - screen.height / 2.0)));
        }
        return done("dragged", true);
    }

    private static McpResult scroll(final Screen screen, final McpCall call) {
        final Optional<Long> x = call.whole("x");
        final Optional<Long> y = call.whole("y");
        final Optional<Long> notches = call.whole("notches").filter(turned -> turned != 0);
        if (x.isEmpty() || y.isEmpty()) {
            return McpCall.wrong("x", McpWords.A_NUMBER.with(0, screen.width));
        }
        if (notches.isEmpty()) {
            return McpCall.wrong("notches", McpWords.A_NUMBER.with(-MOST_NOTCHES, MOST_NOTCHES));
        }
        ScreenGestures.scroll(screen, x.get(), y.get(), notches.get());
        TestRecorder.agentDid(new RecordedTest.Scroll((int) Math.round(x.get() - screen.width / 2.0),
                (int) Math.round(y.get() - screen.height / 2.0), (int) (long) notches.get()));
        return done("scrolled", true);
    }

    private static McpResult type(final Screen screen, final McpCall call) {
        final String text = call.text("text").orElse("");
        for (final char c : text.toCharArray()) {
            ScreenGestures.type(screen, c);
        }
        TestRecorder.agentDid(new RecordedTest.Type(text));
        return done("typed", true);
    }

    /*
     * Holds a key, or a key mapping's key, and a key with it, down for the ticks asked, then lets go of them: pressed
     * once as it goes down, kept down every tick, and told down to the game asking the window.
     */
    private static CompletableFuture<McpResult> hold(final McpCall call) {
        final Optional<String> named = call.text(MAPPING);
        final Optional<KeyMapping> mapping = named.flatMap(name -> List.of(Minecraft.getInstance().options.keyMappings)
                .stream().filter(each -> each.getName().equals(name)).findFirst());
        if (named.isPresent() && mapping.isEmpty()) {
            return WorldTools.done(McpCall.wrong(MAPPING, McpWords.SOME_WORDS.text()));
        }
        final Optional<InputConstants.Key> key = mapping.isPresent() ? Optional.of(mapping.get().getKey())
                : call.text("key").flatMap(PlayActing::keyNamed);
        if (key.isEmpty()) {
            return WorldTools.done(McpCall.wrong("key", McpWords.SOME_WORDS.text()));
        }
        final Optional<InputConstants.Key> with = call.text(WITH).flatMap(PlayActing::keyNamed);
        if (call.text(WITH).isPresent() && with.isEmpty()) {
            return WorldTools.done(McpCall.wrong(WITH, McpWords.SOME_WORDS.text()));
        }
        final int ticks = (int) (long) call.whole(TICKS).orElse(1L);
        final List<InputConstants.Key> keys = new ArrayList<>();
        with.ifPresent(keys::add);
        keys.add(key.get());
        return PlayTasks.run(call, ticks + LET_GO_TICKS, new PlayTasks.ITask() {
            private boolean held;

            @Override
            public Optional<McpResult> step(final Minecraft game, final int tick) {
                if (tick > ticks) {
                    final JsonObject out = new JsonObject();
                    out.addProperty("held", ticks);
                    return Optional.of(McpResult.json(out));
                }
                if (tick == 1) {
                    this.held = true;
                    keys.stream().filter(each -> each.getType() == InputConstants.Type.KEYSYM)
                            .forEach(each -> AgentKeys.hold(each.getValue()));
                    if (game.screen == null) {
                        KeyMapping.click(key.get());
                    }
                }
                keys.forEach(each -> KeyMapping.set(each, true));
                mapping.ifPresent(each -> each.setDown(true));
                return Optional.empty();
            }

            @Override
            public void release(final Minecraft game) {
                if (!this.held) {
                    return;
                }
                this.held = false;
                keys.forEach(each -> KeyMapping.set(each, false));
                mapping.ifPresent(each -> each.setDown(false));
                keys.stream().filter(each -> each.getType() == InputConstants.Type.KEYSYM)
                        .forEach(each -> AgentKeys.release(each.getValue()));
            }
        });
    }

    /* The keyboard's key of the name {@code name}, as the game names keys: e, space, left.control. */
    private static Optional<InputConstants.Key> keyNamed(final String name) {
        try {
            final InputConstants.Key key = InputConstants.getKey("key.keyboard." + name.strip().toLowerCase(
                    Locale.ROOT));
            return key == InputConstants.UNKNOWN ? Optional.empty() : Optional.of(key);
        } catch (final IllegalArgumentException unknown) {
            return Optional.empty();
        }
    }

    private static CompletableFuture<McpResult> key(final McpCall call) {
        final Optional<InputConstants.Key> named = call.text("key").flatMap(PlayActing::keyNamed);
        if (named.isEmpty()) {
            return WorldTools.done(McpCall.wrong("key", McpWords.SOME_WORDS.text()));
        }
        final InputConstants.Key key = named.get();
        final Minecraft game = Minecraft.getInstance();
        return game.submit(() -> {
            if (game.screen != null) {
                ScreenGestures.key(game.screen, key.getValue());
                if (game.screen != null) {
                    ScreenGestures.keyUp(game.screen, key.getValue());
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
