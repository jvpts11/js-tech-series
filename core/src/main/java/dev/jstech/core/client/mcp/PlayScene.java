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
import dev.jstech.core.developer.mcp.McpCall;
import dev.jstech.core.developer.mcp.McpPermission;
import dev.jstech.core.developer.mcp.McpResult;
import dev.jstech.core.developer.mcp.McpSchema;
import dev.jstech.core.developer.mcp.McpTool;
import dev.jstech.core.developer.mcp.PlayerText;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The scene the player sees, for an agent that cannot look at a picture or wants it in numbers: where the player
 * stands and the way they face, the sky, the weather and the light; the kinds of block across the view, most first,
 * each with the nearest and where it lies; and the creatures, players and items in sight with how far and where.
 * The view is read as the eyes would: lines of sight across the field of view, each ending at the first block it meets.
 */
final class PlayScene {

    private static final int LEAST_DISTANCE = 4;
    private static final int MOST_DISTANCE = 48;
    private static final long DISTANCE = 24;
    /* The lines of sight across the view, and up it. */
    private static final int ACROSS = 32;
    private static final int UP = 18;
    private static final int MOST_KINDS = 24;
    private static final int MOST_THINGS = 40;
    private static final double TENTHS = 10.0;

    private PlayScene() {
    }

    /** The tools. */
    static List<McpTool> all() {
        return List.of(new McpTool("scene_describe", "The scene the player sees, in numbers: where they stand and "
                + "the way they face, the time, weather, light, dimension and biome, what the crosshair is on, the "
                + "kinds of block across the view (most of the view first, each with the nearest, how far and where "
                + "it lies), how much of the view is open, and the creatures, players and items in sight with how "
                + "far and where.", McpSchema.object().whole("distance", "How far the player looks, in blocks.",
                LEAST_DISTANCE, MOST_DISTANCE, false).build(), McpPermission.READ,
                call -> Play.withPlayer(player -> describe(player, call))));
    }

    private static McpResult describe(final LocalPlayer player, final McpCall call) {
        final Minecraft game = Minecraft.getInstance();
        final Level level = player.level();
        final double distance = Math.max(LEAST_DISTANCE, Math.min(MOST_DISTANCE, call.whole("distance")
                .orElse(DISTANCE)));
        final JsonObject out = new JsonObject();
        out.add("standing", standing(player));
        out.add("world", world(player));
        if (game.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
            final JsonObject looked = Play.place(hit.getBlockPos());
            looked.addProperty("block", BuiltInRegistries.BLOCK.getKey(level.getBlockState(hit.getBlockPos())
                    .getBlock()).toString());
            looked.addProperty("face", hit.getDirection().getSerializedName());
            out.add("crosshair_block", looked);
        } else if (game.hitResult instanceof EntityHitResult hit) {
            out.addProperty("crosshair_entity", BuiltInRegistries.ENTITY_TYPE.getKey(hit.getEntity().getType())
                    .toString());
        }
        final double verticalView = game.options.fov().get();
        final double horizontalView = Math.toDegrees(2 * Math.atan(Math.tan(Math.toRadians(verticalView) / 2)
                * game.getWindow().getWidth() / Math.max(1, game.getWindow().getHeight())));
        final SceneTally tally = look(player, distance, horizontalView, verticalView);
        final JsonArray blocks = new JsonArray();
        for (final SceneTally.Seen seen : tally.most(MOST_KINDS)) {
            final JsonObject one = new JsonObject();
            one.addProperty("block", seen.id());
            one.addProperty("share", Math.round(100.0 * seen.lines() / (ACROSS * UP)) / 100.0);
            one.addProperty("nearest", Math.round(seen.nearest() * TENTHS) / TENTHS);
            one.addProperty("bearing", seen.bearing());
            one.addProperty("height", seen.height());
            blocks.add(one);
        }
        out.add("blocks_in_view", blocks);
        out.addProperty("kinds_in_view", tally.kinds());
        out.addProperty("open_share", Math.round(tally.openShare() * 100) / 100.0);
        out.add("in_sight", inSight(player, distance, Math.max(horizontalView, verticalView)));
        if (game.screen != null) {
            out.addProperty("screen", game.screen.getTitle().getString());
        }
        return McpResult.json(out);
    }

    private static JsonObject standing(final LocalPlayer player) {
        final JsonObject out = Play.place(player.blockPosition());
        out.addProperty("facing", SceneTally.facing(player.getYRot()));
        out.addProperty("yaw", Math.round(player.getYRot() * TENTHS) / TENTHS);
        out.addProperty("pitch", Math.round(player.getXRot() * TENTHS) / TENTHS);
        out.addProperty("on", BuiltInRegistries.BLOCK.getKey(player.level().getBlockState(player.blockPosition()
                .below()).getBlock()).toString());
        out.addProperty("in_water", player.isInWater());
        out.addProperty("in_lava", player.isInLava());
        out.addProperty("flying", player.getAbilities().flying);
        return out;
    }

    private static JsonObject world(final LocalPlayer player) {
        final Level level = player.level();
        final BlockPos at = BlockPos.containing(player.getEyePosition());
        final JsonObject out = new JsonObject();
        out.addProperty("dimension", level.dimension().location().toString());
        level.getBiome(at).unwrapKey().ifPresent(biome -> out.addProperty("biome", biome.location().toString()));
        out.addProperty("time_of_day", Math.floorMod(level.getDayTime(), 24_000L));
        out.addProperty("part_of_day", SceneTally.partOfDay(level.getDayTime()));
        out.addProperty("weather", level.isThundering() ? "thunder" : level.isRaining() ? "rain" : "clear");
        out.addProperty("sky_light", level.getBrightness(LightLayer.SKY, at));
        out.addProperty("block_light", level.getBrightness(LightLayer.BLOCK, at));
        out.addProperty("sees_sky", level.canSeeSky(at));
        return out;
    }

    /* Lines of sight across the view, each ending at the first block it meets or as far as the player looks. */
    private static SceneTally look(final LocalPlayer player, final double distance, final double across,
                                   final double up) {
        final SceneTally tally = new SceneTally();
        final Vec3 eye = player.getEyePosition();
        final Level level = player.level();
        for (int row = 0; row < UP; row++) {
            final float pitch = (float) (player.getXRot() + up * ((row + 0.5) / UP - 0.5));
            for (int column = 0; column < ACROSS; column++) {
                final float yaw = (float) (player.getYRot() + across * ((column + 0.5) / ACROSS - 0.5));
                final Vec3 end = eye.add(Vec3.directionFromRotation(pitch, yaw).scale(distance));
                final BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE,
                        ClipContext.Fluid.ANY, player));
                if (hit.getType() != HitResult.Type.BLOCK) {
                    tally.nothing();
                    continue;
                }
                final Vec3 point = hit.getLocation();
                final double dx = point.x - eye.x;
                final double dz = point.z - eye.z;
                tally.block(BuiltInRegistries.BLOCK.getKey(level.getBlockState(hit.getBlockPos()).getBlock())
                        .toString(), point.distanceTo(eye), SceneTally.bearing(player.getYRot(), dx, dz),
                        SceneTally.height(point.y - eye.y, Math.sqrt(dx * dx + dz * dz)));
            }
        }
        return tally;
    }

    /* The creatures, players and items within {@code distance}, in the view and in sight, the nearest first. */
    private static JsonArray inSight(final LocalPlayer player, final double distance, final double view) {
        final Vec3 eye = player.getEyePosition();
        final Vec3 look = player.getViewVector(1.0F);
        final double widest = Math.cos(Math.toRadians(view / 2));
        final List<Entity> seen = player.level().getEntities(player, player.getBoundingBox().inflate(distance),
                entity -> !entity.isSpectator() && entity.position().distanceTo(eye) <= distance).stream()
                .filter(entity -> entity.getEyePosition().subtract(eye).normalize().dot(look) >= widest
                        && player.hasLineOfSight(entity))
                .sorted(Comparator.comparingDouble(entity -> entity.distanceToSqr(player)))
                .limit(MOST_THINGS).toList();
        final JsonArray out = new JsonArray();
        for (final Entity entity : seen) {
            final JsonObject one = new JsonObject();
            one.addProperty("type", BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
            if (entity instanceof Player other) {
                one.add("name", PlayerText.mark(other.getGameProfile().getName()));
            } else if (entity.hasCustomName() && entity.getCustomName() != null) {
                one.add("name", PlayerText.mark(entity.getCustomName().getString()));
            }
            if (entity instanceof ItemEntity item) {
                one.add("item", Play.stack(item.getItem()));
            }
            if (entity instanceof LivingEntity living) {
                one.addProperty("health", Math.round(living.getHealth() * TENTHS) / TENTHS);
            }
            final Vec3 to = entity.position().subtract(eye);
            one.addProperty("distance", Math.round(entity.position().distanceTo(eye) * TENTHS) / TENTHS);
            one.addProperty("bearing", SceneTally.bearing(player.getYRot(), to.x, to.z));
            one.addProperty("height", SceneTally.height(entity.getEyePosition().y - eye.y,
                    Math.sqrt(to.x * to.x + to.z * to.z)));
            out.add(one);
        }
        return out;
    }
}
