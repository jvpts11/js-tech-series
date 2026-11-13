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
    /** The point of a block a tool looks at, in sixteenths of it, and how far each of its numbers goes. */
    static final String AT = "at";
    static final int SIXTEENTHS = 16;

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

    /**
     * The point of the block at {@code pos} the call asks to look at: its {@link #AT} point, in sixteenths of the
     * block, put on {@code face} when one is named; else the face's middle, or the block's. None when the point asked
     * is outside the block.
     */
    static Optional<Vec3> pointOf(final McpCall call, final BlockPos pos, final Optional<Direction> face) {
        final Optional<BlockPos> at = place(call, AT);
        if (at.isEmpty()) {
            return Optional.of(face.map(side -> faceOf(pos, side)).orElse(Vec3.atCenterOf(pos)));
        }
        final BlockPos in = at.get();
        if (Math.min(in.getX(), Math.min(in.getY(), in.getZ())) < 0
                || Math.max(in.getX(), Math.max(in.getY(), in.getZ())) > SIXTEENTHS) {
            return Optional.empty();
        }
        double x = in.getX() / (double) SIXTEENTHS;
        double y = in.getY() / (double) SIXTEENTHS;
        double z = in.getZ() / (double) SIXTEENTHS;
        if (face.isPresent()) {
            // On the face named: its own axis at the face's plane, the other two where the point says.
            final Direction side = face.get();
            final double plane = side.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1.0 : 0.0;
            switch (side.getAxis()) {
                case X -> x = plane;
                case Y -> y = plane;
                case Z -> z = plane;
            }
        }
        return Optional.of(new Vec3(pos.getX() + x, pos.getY() + y, pos.getZ() + z));
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
