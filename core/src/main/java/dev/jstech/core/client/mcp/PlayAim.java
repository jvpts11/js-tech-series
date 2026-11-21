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
import dev.jstech.core.developer.mcp.McpResult;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * What a look at a block lands on, read once the view has turned and the game has picked what is looked at, so an
 * acting tool can say why it did not do what it was asked: the block out of the player's reach, the point looked at
 * off the block's shape, something else in the way of it or the look passing through it to what is behind, and a
 * block that cannot be broken. An agent then knows what to change: come nearer, go round, or look at another point.
 */
final class PlayAim {

    /** Why a tool failed, as its answer names it, and what took a use instead of the block asked. */
    static final String FAILED = "failed";
    static final String USED_ON = "used_on";
    static final String INSTEAD = "instead";
    /** The block asked for and the point of it looked at, as a failure gives them. */
    private static final String ASKED = "asked";
    /* Why a use landed on something other than the block asked: nearer than it, or past it. */
    private static final String IN_THE_WAY = "in_the_way";
    private static final String PAST_IT = "passed_through_it";

    private PlayAim() {
    }

    /**
     * Why a look at {@code point} of the block at {@code pos} lands on nothing at all: the block is past the player's
     * reach, or the point is off its shape and the look meets nothing behind it; none when it lands on something.
     */
    static Optional<McpResult> missed(final Minecraft game, final LocalPlayer player, final BlockPos pos,
                                      final Vec3 point) {
        if (game.hitResult != null && game.hitResult.getType() != HitResult.Type.MISS) {
            return Optional.empty();
        }
        final String block = blockOf(player, pos);
        final double distance = player.getEyePosition().distanceTo(point);
        final double reach = player.blockInteractionRange();
        final JsonObject out = asked(pos, block);
        if (distance > reach) {
            out.addProperty(FAILED, "out_of_reach");
            out.addProperty("distance", tenth(distance));
            out.addProperty("reach", tenth(reach));
            return Optional.of(McpResult.failed(McpTexts.OUT_OF_REACH.with(block, where(pos), tenth(distance),
                    tenth(reach))).with(out));
        }
        out.addProperty(FAILED, "off_its_shape");
        return Optional.of(McpResult.failed(McpTexts.OFF_ITS_SHAPE.with(block, where(pos))).with(out));
    }

    /**
     * What a use lands on when it is not the block at {@code pos} asked for: another block or an entity, with whether
     * it is in the way, nearer than {@code point}, or behind it; none when the use lands on the block asked, or on
     * nothing.
     */
    static Optional<JsonObject> instead(final Minecraft game, final LocalPlayer player, final BlockPos pos,
                                        final Vec3 point) {
        final JsonElement landed = landedOn(game, player);
        if (landed.isJsonNull() || game.hitResult instanceof BlockHitResult hit && hit.getBlockPos().equals(pos)) {
            return Optional.empty();
        }
        final double eye = player.getEyePosition().distanceTo(game.hitResult.getLocation());
        final JsonObject out = new JsonObject();
        out.add(USED_ON, landed);
        out.addProperty(INSTEAD, eye < player.getEyePosition().distanceTo(point) ? IN_THE_WAY : PAST_IT);
        out.add(ASKED, asked(pos, blockOf(player, pos)));
        return Optional.of(out);
    }

    /**
     * Why the player cannot break the block at {@code pos}: past their reach, a block no tool breaks, or one their
     * game mode keeps them from breaking; none when they can.
     */
    static Optional<McpResult> unbreakable(final Minecraft game, final LocalPlayer player, final BlockPos pos) {
        final BlockState state = player.level().getBlockState(pos);
        final String block = blockOf(player, pos);
        final JsonObject out = asked(pos, block);
        if (!player.canInteractWithBlock(pos, 1.0)) {
            final double distance = player.getEyePosition().distanceTo(Vec3.atCenterOf(pos));
            out.addProperty(FAILED, "out_of_reach");
            out.addProperty("distance", tenth(distance));
            out.addProperty("reach", tenth(player.blockInteractionRange()));
            return Optional.of(McpResult.failed(McpTexts.OUT_OF_REACH.with(block, where(pos), tenth(distance),
                    tenth(player.blockInteractionRange()))).with(out));
        }
        if (game.gameMode != null && player.blockActionRestricted(player.level(), pos, game.gameMode.getPlayerMode())) {
            out.addProperty(FAILED, "game_mode");
            return Optional.of(McpResult.failed(McpTexts.KEPT_FROM_BREAKING.with(block)).with(out));
        }
        if (state.getDestroySpeed(player.level(), pos) < 0 && !player.getAbilities().instabuild) {
            out.addProperty(FAILED, "unbreakable");
            return Optional.of(McpResult.failed(McpTexts.UNBREAKABLE.with(block)).with(out));
        }
        return Optional.empty();
    }

    /** What the crosshair is on now, as player_state tells it: a block, an entity, or nothing (null). */
    static JsonElement landedOn(final Minecraft game, final LocalPlayer player) {
        final Optional<JsonObject> block = PlaySeeing.lookedAtBlock(game, player);
        if (block.isPresent()) {
            return block.get();
        }
        final Optional<JsonObject> entity = PlaySeeing.lookedAtEntity(game);
        return entity.isPresent() ? entity.get() : JsonNull.INSTANCE;
    }

    private static JsonObject asked(final BlockPos pos, final String block) {
        final JsonObject out = new JsonObject();
        out.addProperty("block", block);
        out.add(Play.PLACE, Play.place(pos));
        return out;
    }

    private static String blockOf(final LocalPlayer player, final BlockPos pos) {
        return BuiltInRegistries.BLOCK.getKey(player.level().getBlockState(pos).getBlock()).toString();
    }

    private static String where(final BlockPos pos) {
        return pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }

    private static double tenth(final double value) {
        return Math.round(value * 10) / 10.0;
    }
}
