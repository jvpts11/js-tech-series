/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.mcp;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.blaze3d.platform.NativeImage;
import dev.jstech.core.agent.AgentReadings;
import dev.jstech.core.client.gui.component.UiComponent;
import dev.jstech.core.client.gui.data.IDataScreen;
import dev.jstech.core.client.gui.screen.ShownPart;
import dev.jstech.core.developer.mcp.McpCall;
import dev.jstech.core.developer.mcp.McpPermission;
import dev.jstech.core.developer.mcp.McpResult;
import dev.jstech.core.developer.mcp.McpSchema;
import dev.jstech.core.developer.mcp.McpTool;
import dev.jstech.core.developer.mcp.McpWords;
import dev.jstech.core.developer.mcp.PlayerText;
import dev.jstech.core.moving.IStructureBody;
import dev.jstech.core.moving.LevelMovingStructures;
import dev.jstech.core.moving.StructureFrame;
import dev.jstech.core.multiblock.shape.FormedPieceAim;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * What an agent sees of the game, as the player does: where the player is and looks, what the crosshair is on, the
 * inventory, the screen open and its parts by their names, a picture of the window, the blocks round the player, the
 * chat and the game's log.
 */
public final class PlaySeeing {

    /* The names of what the crosshair is on and of the screen open, as player_state and a call's glimpse give them. */
    static final String LOOKING_AT_BLOCK = "looking_at_block";
    static final String LOOKING_AT_ENTITY = "looking_at_entity";
    static final String SCREEN = "screen";
    private static final int MOST_RADIUS = 8;
    /* How far inside the face met a hit aboard a moving structure is read, for the world's block it stands in. */
    private static final double INSIDE = 0.01;
    /* Within this of its block's middle, squared, the point a hit met is in that block's own space. */
    private static final double NEAR_ITS_PLACE = 4.0;
    private static final int MOST_BLOCKS = 4_000;
    private static final int MOST_LINES = 300;
    private static final int PICTURE_WIDTH = 960;
    /** How much of the end of the log is read for its last lines. */
    private static final int LOG_TAIL = 256 * 1024;
    private static final String CHAT_MARK = "[CHAT]";

    private PlaySeeing() {
    }

    /** The tools. */
    public static List<McpTool> all() {
        return List.of(
                new McpTool("player_state", "Where the player is and looks, their health, food, game mode and the "
                        + "slot held, what the crosshair is on (a block and its face, or an entity), the screen open, "
                        + "what they ride (the entity, a moving structure's seat and whether it steers, the "
                        + "structure) or the structure they stand on, how it stands (heading, pitch down, roll its "
                        + "right side down, in degrees), its speed and velocity in blocks a second and where the "
                        + "player looks in its frame, ahead being nought; and the time of day.",
                        McpSchema.object().build(), McpPermission.READ,
                        call -> Play.withPlayer(PlaySeeing::state)),
                new McpTool("inventory_read", "Every slot of the player's inventory that holds something: its item, "
                        + "how many, and a name a player gave it.", McpSchema.object().build(), McpPermission.READ,
                        call -> Play.withPlayer(PlaySeeing::inventory)),
                new McpTool("screen_tree", "The screen open and its parts: on a screen declared in a file, every "
                        + "named component with its place, size and words, which screen_click clicks by name; its "
                        + "widgets, those in its lists and bars too, with their words; the parts a screen drawn in "
                        + "code names, its tabs, cards and the like, with their words and where a click lands, "
                        + "clicked and dragged by name; on a container's, its slots and what they hold.",
                        McpSchema.object().build(), McpPermission.READ, call -> Play.withPlayer(player -> tree())),
                new McpTool("screen_picture", "A picture of the game's window as it was last drawn, at most 960 "
                        + "pixels wide.", McpSchema.object().build(), McpPermission.READ,
                        call -> Minecraft.getInstance().submit(PlaySeeing::picture)),
                new McpTool("blocks_around", "The blocks round the player other than air, each with its place, its "
                        + "id and whether it has a block entity.", McpSchema.object().whole("radius",
                        "How far round the player, in blocks.", 1, MOST_RADIUS, false).build(), McpPermission.READ,
                        call -> Play.withPlayer(player -> around(player, call))),
                new McpTool("chat_read", "The last lines of chat, a line a player wrote marked as theirs.",
                        McpSchema.object().whole("lines", "How many of the last lines.", 1, MOST_LINES, false)
                                .build(), McpPermission.READ, call -> Minecraft.getInstance().submit(() -> chat(call))),
                new McpTool("game_log_read", "The last lines of the game's log, a line of chat marked as written by "
                        + "a player.", McpSchema.object().whole("lines", "How many of the last lines.", 1,
                        MOST_LINES, false).build(), McpPermission.READ,
                        call -> Minecraft.getInstance().submit(() -> log(call))));
    }

    private static McpResult state(final LocalPlayer player) {
        final Minecraft game = Minecraft.getInstance();
        final JsonObject out = new JsonObject();
        out.addProperty("x", Math.round(player.getX() * 100) / 100.0);
        out.addProperty("y", Math.round(player.getY() * 100) / 100.0);
        out.addProperty("z", Math.round(player.getZ() * 100) / 100.0);
        out.addProperty("yaw", Math.round(player.getYRot() * 10) / 10.0);
        out.addProperty("pitch", Math.round(player.getXRot() * 10) / 10.0);
        out.addProperty("dimension", player.level().dimension().location().toString());
        out.addProperty("health", player.getHealth());
        out.addProperty("food", player.getFoodData().getFoodLevel());
        if (game.gameMode != null) {
            out.addProperty("game_mode", game.gameMode.getPlayerMode().getName());
        }
        out.addProperty("slot", player.getInventory().selected);
        if (!player.getMainHandItem().isEmpty()) {
            out.add("held", Play.stack(player.getMainHandItem()));
        }
        lookedAtBlock(game, player).ifPresent(looked -> out.add(LOOKING_AT_BLOCK, looked));
        lookedAtEntity(game).ifPresent(looked -> out.add(LOOKING_AT_ENTITY, looked));
        screen(game.screen).ifPresent(screen -> out.add(SCREEN, screen));
        PlayRiding.riding(player).ifPresent(riding -> out.add(PlayRiding.RIDING, riding));
        PlayRiding.aboard(player).ifPresent(aboard -> out.add(PlayRiding.ABOARD, aboard));
        out.addProperty("time_of_day", player.level().getDayTime() % 24_000);
        return McpResult.json(out);
    }

    /**
     * The block the crosshair is on: its place, its id and the face; the piece of a formed model drawn there, its
     * model, its name and the box round it in the world; and for a block aboard a moving structure, its place in the
     * world, the point aimed at, and the structure and its place aboard. None when it is on no block. Read on the
     * game's own thread; public for the tests that check what an agent is told.
     */
    public static Optional<JsonObject> lookedAtBlock(final Minecraft game, final LocalPlayer player) {
        if (!(game.hitResult instanceof BlockHitResult hit) || hit.getType() != BlockHitResult.Type.BLOCK) {
            return Optional.empty();
        }
        final Level level = player.level();
        final BlockPos pos = hit.getBlockPos();
        final IStructureBody body = LevelMovingStructures.anyLoaded() ? LevelMovingStructures.of(level)
                .atYard(pos.getX(), pos.getZ()) : null;
        final StructureFrame frame = body == null ? null : body.aimFrame();
        // Aboard a moving structure the hit is the block's place in its yard, and the point met in the yard's space
        // or the world's, as the aim found it: the one near the place is the yard's.
        final Vec3 met = hit.getLocation();
        final boolean metInYard = frame != null && met.distanceToSqr(Vec3.atCenterOf(pos)) < NEAR_ITS_PLACE;
        final Vec3 yardAt = frame == null || metInYard ? met : new Vec3(frame.yardX(met.x, met.y, met.z),
                frame.yardY(met.x, met.y, met.z), frame.yardZ(met.x, met.y, met.z));
        final JsonObject looked = new JsonObject();
        if (frame == null) {
            looked.add(Play.PLACE, Play.place(pos));
        } else {
            final Vec3 at = metInYard ? new Vec3(frame.worldX(met.x, met.y, met.z), frame.worldY(met.x, met.y, met.z),
                    frame.worldZ(met.x, met.y, met.z)) : met;
            // A little inside the face met is the world's block it stands in now.
            final Vec3 in = yardAt.subtract(Vec3.atLowerCornerOf(hit.getDirection().getNormal()).scale(INSIDE));
            looked.add(Play.PLACE, Play.place(BlockPos.containing(frame.worldX(in.x, in.y, in.z), frame.worldY(in.x,
                    in.y, in.z), frame.worldZ(in.x, in.y, in.z))));
            looked.add("at", point(at));
            final JsonObject aboard = new JsonObject();
            aboard.addProperty("structure", body.id().value().toString());
            aboard.add(Play.PLACE, Play.place(pos));
            looked.add("aboard", aboard);
        }
        looked.addProperty("block", BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString());
        looked.addProperty("face", hit.getDirection().getSerializedName());
        FormedPieceAim.at(level, pos, yardAt).ifPresent(aimed -> {
            final JsonObject piece = new JsonObject();
            piece.addProperty("model", aimed.model().toString());
            piece.addProperty("name", aimed.piece());
            final AABB box = aimed.box();
            final double[] world = frame == null ? new double[] {box.minX, box.minY, box.minZ, box.maxX, box.maxY,
                box.maxZ} : frame.worldBox(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
            final JsonObject drawn = new JsonObject();
            drawn.add("from", point(new Vec3(world[0], world[1], world[2])));
            drawn.add("to", point(new Vec3(world[3], world[4], world[5])));
            piece.add("box", drawn);
            looked.add("piece", piece);
        });
        // What the mods tell of it, a machine at work or a computer and its job, read where its block is kept.
        PlayReadings.add(looked, AgentReadings.ofBlock(level, pos));
        return Optional.of(looked);
    }

    /* A point as JSON, to a hundredth of a block. */
    static JsonObject point(final Vec3 at) {
        final JsonObject out = new JsonObject();
        out.addProperty("x", Math.round(at.x * 100) / 100.0);
        out.addProperty("y", Math.round(at.y * 100) / 100.0);
        out.addProperty("z", Math.round(at.z * 100) / 100.0);
        return out;
    }

    /** The entity the crosshair is on, or none. */
    static Optional<JsonObject> lookedAtEntity(final Minecraft game) {
        if (!(game.hitResult instanceof EntityHitResult hit)) {
            return Optional.empty();
        }
        final JsonObject looked = entity(hit.getEntity());
        PlayReadings.add(looked, AgentReadings.ofEntity(hit.getEntity()));
        return Optional.of(looked);
    }

    /** The screen open, its kind and its title, or none. */
    static Optional<JsonObject> screen(@Nullable final Screen open) {
        if (open == null) {
            return Optional.empty();
        }
        final JsonObject screen = new JsonObject();
        screen.addProperty("kind", open.getClass().getSimpleName());
        screen.addProperty("title", open.getTitle().getString());
        return Optional.of(screen);
    }

    /** A line of chat as the tools give it: its words, marked as a player's when a player wrote them. */
    static JsonElement chatText(final ChatLines.Line line) {
        return line.byPlayer() ? PlayerText.mark(line.text()) : new JsonPrimitive(line.text());
    }

    private static JsonObject entity(final Entity entity) {
        final JsonObject out = new JsonObject();
        out.addProperty("type", BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
        out.addProperty("id", entity.getId());
        if (entity.hasCustomName() && entity.getCustomName() != null) {
            out.add("name", PlayerText.mark(entity.getCustomName().getString()));
        }
        out.add(Play.PLACE, Play.place(entity.blockPosition()));
        return out;
    }

    private static McpResult inventory(final LocalPlayer player) {
        final JsonArray slots = new JsonArray();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            final ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                final JsonObject one = Play.stack(stack);
                one.addProperty("slot", i);
                slots.add(one);
            }
        }
        final JsonObject out = new JsonObject();
        out.add("slots", slots);
        return McpResult.json(out);
    }

    private static McpResult tree() {
        final Screen screen = Minecraft.getInstance().screen;
        final JsonObject out = new JsonObject();
        if (screen == null) {
            out.addProperty("screen", "none");
            return McpResult.json(out);
        }
        out.addProperty("kind", screen.getClass().getSimpleName());
        out.addProperty("title", screen.getTitle().getString());
        out.addProperty("width", screen.width);
        out.addProperty("height", screen.height);
        if (screen instanceof IDataScreen data) {
            final JsonArray named = new JsonArray();
            out.addProperty("file", data.file().toString());
            data.view().tree().ifPresent(root -> root.forEach(node -> {
                final UiComponent component = node.component();
                if (node.id().isEmpty() || component == null) {
                    return;
                }
                final JsonObject one = new JsonObject();
                one.addProperty("name", node.id().get());
                one.addProperty("x", component.x());
                one.addProperty("y", component.y());
                one.addProperty("width", component.width());
                one.addProperty("height", component.height());
                one.addProperty("shown", node.visible().getAsBoolean());
                one.addProperty("usable", node.enabled().getAsBoolean());
                one.addProperty("words", component.narration().getString());
                named.add(one);
            }));
            out.add("components", named);
        }
        if (screen instanceof AbstractContainerScreen<?> container) {
            final JsonArray slots = new JsonArray();
            for (final Slot slot : container.getMenu().slots) {
                final JsonObject one = slot.getItem().isEmpty() ? new JsonObject() : Play.stack(slot.getItem());
                one.addProperty("slot", slot.index);
                one.addProperty("x", container.getGuiLeft() + slot.x + 8);
                one.addProperty("y", container.getGuiTop() + slot.y + 8);
                slots.add(one);
            }
            out.add("slots", slots);
        }
        final PlayScreens.Seen seen = PlayScreens.seen(screen);
        final JsonArray widgets = new JsonArray();
        for (final AbstractWidget widget : seen.widgets()) {
            final JsonObject one = new JsonObject();
            one.addProperty("kind", widget.getClass().getSimpleName());
            one.addProperty("x", widget.getX());
            one.addProperty("y", widget.getY());
            one.addProperty("width", widget.getWidth());
            one.addProperty("height", widget.getHeight());
            one.addProperty("words", widget.getMessage().getString());
            one.addProperty("shown", widget.visible);
            one.addProperty("usable", widget.active);
            widgets.add(one);
        }
        out.add("widgets", widgets);
        // What the screen draws itself and names, as its tabs, its cards and the places a thing held goes.
        final JsonArray parts = new JsonArray();
        for (final ShownPart part : seen.parts()) {
            final JsonObject one = new JsonObject();
            one.addProperty("name", part.name());
            one.addProperty("words", part.words());
            one.addProperty("x", part.x());
            one.addProperty("y", part.y());
            one.addProperty("width", part.width());
            one.addProperty("height", part.height());
            one.addProperty("click_x", part.pointX());
            one.addProperty("click_y", part.pointY());
            one.addProperty("usable", part.usable());
            parts.add(one);
        }
        out.add("parts", parts);
        return McpResult.json(out);
    }

    private static McpResult picture() {
        final Minecraft game = Minecraft.getInstance();
        try (NativeImage frame = Screenshot.takeScreenshot(game.getMainRenderTarget())) {
            final int width = Math.min(PICTURE_WIDTH, frame.getWidth());
            final int height = Math.max(1, frame.getHeight() * width / frame.getWidth());
            final byte[] png;
            if (width == frame.getWidth()) {
                png = frame.asByteArray();
            } else {
                try (NativeImage small = new NativeImage(width, height, false)) {
                    frame.resizeSubRectTo(0, 0, frame.getWidth(), frame.getHeight(), small);
                    png = small.asByteArray();
                }
            }
            final JsonObject about = new JsonObject();
            about.addProperty("width", width);
            about.addProperty("height", height);
            about.addProperty("gui_width", game.getWindow().getGuiScaledWidth());
            about.addProperty("gui_height", game.getWindow().getGuiScaledHeight());
            return McpResult.picture(png, about);
        } catch (final IOException unwritten) {
            return McpResult.failed(McpWords.WENT_WRONG.with(String.valueOf(unwritten.getMessage())));
        }
    }

    private static McpResult around(final LocalPlayer player, final McpCall call) {
        final int radius = (int) Math.max(1, Math.min(MOST_RADIUS, call.whole("radius").orElse(4L)));
        final BlockPos at = player.blockPosition();
        final JsonArray blocks = new JsonArray();
        for (final BlockPos pos : BlockPos.betweenClosed(at.offset(-radius, -radius, -radius), at.offset(radius,
                radius, radius))) {
            if (blocks.size() >= MOST_BLOCKS) {
                break;
            }
            final BlockState state = player.level().getBlockState(pos);
            if (state.isAir()) {
                continue;
            }
            final JsonObject one = Play.place(pos.immutable());
            one.addProperty("block", BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
            if (state.hasBlockEntity()) {
                one.addProperty("block_entity", true);
            }
            blocks.add(one);
        }
        final JsonObject out = new JsonObject();
        out.add("blocks", blocks);
        return McpResult.json(out);
    }

    private static McpResult chat(final McpCall call) {
        final List<ChatLines.Line> lines = ChatLines.lines();
        final int shown = (int) Math.max(1, Math.min(MOST_LINES, call.whole("lines").orElse(50L)));
        final JsonArray out = new JsonArray();
        for (final ChatLines.Line line : lines.subList(Math.max(0, lines.size() - shown), lines.size())) {
            final JsonObject one = new JsonObject();
            one.addProperty("millis", line.millis());
            one.add("text", chatText(line));
            out.add(one);
        }
        final JsonObject result = new JsonObject();
        result.add("lines", out);
        return McpResult.json(result);
    }

    private static McpResult log(final McpCall call) {
        final Path file = Minecraft.getInstance().gameDirectory.toPath().resolve("logs").resolve("latest.log");
        final int shown = (int) Math.max(1, Math.min(MOST_LINES, call.whole("lines").orElse(50L)));
        if (!Files.isRegularFile(file)) {
            return McpResult.failed(McpWords.FILE_MISSING.with(file.getFileName().toString()));
        }
        try (RandomAccessFile read = new RandomAccessFile(file.toFile(), "r")) {
            final long start = Math.max(0, read.length() - LOG_TAIL);
            read.seek(start);
            final byte[] tail = new byte[(int) (read.length() - start)];
            read.readFully(tail);
            final List<String> lines = List.of(new String(tail, StandardCharsets.UTF_8).split("\\R"));
            final JsonArray out = new JsonArray();
            for (final String line : lines.subList(Math.max(0, lines.size() - shown), lines.size())) {
                if (line.contains(CHAT_MARK)) {
                    out.add(PlayerText.mark(line));
                } else {
                    out.add(line);
                }
            }
            final JsonObject result = new JsonObject();
            result.add("lines", out);
            return McpResult.json(result);
        } catch (final IOException unread) {
            return McpResult.failed(McpWords.WENT_WRONG.with(String.valueOf(unread.getMessage())));
        }
    }
}
