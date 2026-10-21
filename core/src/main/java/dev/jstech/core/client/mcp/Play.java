/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.mcp;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.jstech.core.developer.mcp.McpCall;
import dev.jstech.core.developer.mcp.McpResult;
import dev.jstech.core.developer.mcp.PlayerText;
import dev.jstech.core.developer.mcp.WorldTools;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** What the tools that play as the player share: the game's own thread, a place given, looking at a point, an item. */
final class Play {

    static final String PLACE = "place";

    private Play() {
    }

    /** {@code read} of the player, on the game's own thread; refused with no world open. */
    static CompletableFuture<McpResult> withPlayer(final Function<LocalPlayer, McpResult> read) {
        final Minecraft game = Minecraft.getInstance();
        return game.submit(() -> game.player == null ? McpResult.failed(McpTexts.NO_WORLD.text())
                : read.apply(game.player));
    }

    /** The place the call names as {@code {x, y, z}} under {@code name}, or none. */
    static Optional<BlockPos> place(final McpCall call, final String name) {
        return call.object(name).flatMap(WorldTools::placeOf);
    }

    /** Turns the player to look at {@code point}, as the mouse turns them. */
    static void lookAt(final LocalPlayer player, final Vec3 point) {
        final Vec3 eye = player.getEyePosition();
        final double dx = point.x - eye.x;
        final double dy = point.y - eye.y;
        final double dz = point.z - eye.z;
        final float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        final float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        player.setYRot(yaw);
        player.setXRot(pitch);
        player.setYHeadRot(yaw);
        player.yRotO = yaw;
        player.xRotO = pitch;
    }

    /** The point of {@code face} of the block at {@code pos} a click lands on: its middle. */
    static Vec3 faceOf(final BlockPos pos, final Direction face) {
        return Vec3.atCenterOf(pos).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
    }

    /** A place as JSON. */
    static JsonObject place(final BlockPos pos) {
        final JsonObject out = new JsonObject();
        out.addProperty("x", pos.getX());
        out.addProperty("y", pos.getY());
        out.addProperty("z", pos.getZ());
        return out;
    }

    /** A stack as JSON: its item, how many, and a name a player gave it marked as theirs. */
    static JsonObject stack(final ItemStack stack) {
        final JsonObject out = new JsonObject();
        out.addProperty("item", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        out.addProperty("count", stack.getCount());
        if (stack.has(DataComponents.CUSTOM_NAME)) {
            out.add("name", PlayerText.mark(stack.getHoverName().getString()));
        }
        return out;
    }

    /** {@code value} as words, as an argument may come. */
    static Optional<String> words(final JsonElement value) {
        return value instanceof JsonPrimitive primitive && primitive.isString() ? Optional.of(primitive.getAsString())
                : Optional.empty();
    }
}
