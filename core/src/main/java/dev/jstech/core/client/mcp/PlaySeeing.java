/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.mcp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import dev.jstech.core.client.gui.component.UiComponent;
import dev.jstech.core.client.gui.data.IDataScreen;
import dev.jstech.core.developer.mcp.McpCall;
import dev.jstech.core.developer.mcp.McpPermission;
import dev.jstech.core.developer.mcp.McpResult;
import dev.jstech.core.developer.mcp.McpSchema;
import dev.jstech.core.developer.mcp.McpTool;
import dev.jstech.core.developer.mcp.McpWords;
import dev.jstech.core.developer.mcp.PlayerText;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

/**
 * What an agent sees of the game, as the player does: where the player is and looks, what the crosshair is on, the
 * inventory, the screen open and its parts by their names, a picture of the window, the blocks round the player, the
 * chat and the game's log.
 */
public final class PlaySeeing {

    private static final int MOST_RADIUS = 8;
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
                        + "and the time of day.", McpSchema.object().build(), McpPermission.READ,
                        call -> Play.withPlayer(PlaySeeing::state)),
                new McpTool("inventory_read", "Every slot of the player's inventory that holds something: its item, "
                        + "how many, and a name a player gave it.", McpSchema.object().build(), McpPermission.READ,
                        call -> Play.withPlayer(PlaySeeing::inventory)),
                new McpTool("screen_tree", "The screen open and its parts: on a screen declared in a file, every "
                        + "named component with its place, size and words, which screen_click clicks by name; on "
                        + "another, its buttons and fields; on a container's, its slots and what they hold.",
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
        if (game.hitResult instanceof BlockHitResult hit && hit.getType() == BlockHitResult.Type.BLOCK) {
            final JsonObject looked = new JsonObject();
            looked.add(Play.PLACE, Play.place(hit.getBlockPos()));
            looked.addProperty("block", BuiltInRegistries.BLOCK.getKey(player.level().getBlockState(
                    hit.getBlockPos()).getBlock()).toString());
            looked.addProperty("face", hit.getDirection().getSerializedName());
            out.add("looking_at_block", looked);
        } else if (game.hitResult instanceof EntityHitResult hit) {
            out.add("looking_at_entity", entity(hit.getEntity()));
        }
        if (game.screen != null) {
            final JsonObject screen = new JsonObject();
            screen.addProperty("kind", game.screen.getClass().getSimpleName());
            screen.addProperty("title", game.screen.getTitle().getString());
            out.add("screen", screen);
        }
        out.addProperty("time_of_day", player.level().getDayTime() % 24_000);
        return McpResult.json(out);
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
        final JsonArray widgets = new JsonArray();
        for (final Object child : screen.children()) {
            if (child instanceof AbstractWidget widget) {
                final JsonObject one = new JsonObject();
                one.addProperty("kind", widget.getClass().getSimpleName());
                one.addProperty("x", widget.getX());
                one.addProperty("y", widget.getY());
                one.addProperty("width", widget.getWidth());
                one.addProperty("height", widget.getHeight());
                one.addProperty("words", widget.getMessage().getString());
                one.addProperty("usable", widget.active);
                widgets.add(one);
            }
        }
        out.add("widgets", widgets);
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
            if (line.byPlayer()) {
                one.add("text", PlayerText.mark(line.text()));
            } else {
                one.addProperty("text", line.text());
            }
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
