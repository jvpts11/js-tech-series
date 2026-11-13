/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import dev.jstech.core.client.effect.EffectEngine;
import dev.jstech.core.client.sky.SkySight;
import dev.jstech.core.client.weather.WeatherClient;
import dev.jstech.core.dimension.RuntimeDimensions;
import dev.jstech.core.sky.Skies;
import dev.jstech.core.world.event.WorldEvents;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestSkies;
import dev.jstech.tests.TestWorldGen;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * The sky kit on a player's game, to look at: the test sky land in the morning (its two suns, its ringed planet, its
 * clouds and haze), at noon under its moon's eclipse (the day darkened), and at night (the stars, the galaxy, the
 * nebulae and the aurora over the north); then under the green acid rain, its fog closing in; and under a storm of
 * lightning, which strikes near the player. A weather also falls again over a player gone to another land under it.
 */
public final class SkyClientTests {

    private static final ResourceKey<Level> LAND = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "sky_view"));
    /* A second land like it, under the same weather, to go to. */
    private static final ResourceKey<Level> OTHER_LAND = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "sky_view_other"));
    private static final int ARRIVE = 600;
    private static final int WAIT = 200;
    private static final int SHOOT = 10;
    private static final double HIGH = 20;
    private static final long MORNING = 2_000L;
    private static final long NOON = 6_000L;
    private static final long NIGHT = 18_000L;
    private static final float EAST = -90F;
    private static final float WEST = 90F;
    private static final float NORTH = 180F;

    private SkyClientTests() {
    }

    @ClientTest(timeoutTicks = 3600)
    public static void sky_showsItsSunsBodiesStarsAndWeathers(final ClientTestContext ctx) {
        final boolean[] daylight = new boolean[1];
        ctx.afterTest(() -> ctx.server().execute(() -> {
            ctx.server().getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(daylight[0], ctx.server());
            final ServerPlayer player = ctx.serverPlayer();
            final BlockPos home = ctx.origin().above(2);
            player.teleportTo(ctx.server().overworld(), home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 0F, 0F);
            RuntimeDimensions.remove(ctx.server(), LAND);
        }));
        ctx.thenServer(0, level -> {
            final GameRules.BooleanValue rule = level.getServer().getGameRules().getRule(GameRules.RULE_DAYLIGHT);
            daylight[0] = rule.get();
            rule.set(false, level.getServer());
            final ServerLevel land = RuntimeDimensions.getOrCreate(level.getServer(), LAND,
                    TestWorldGen.SKY_LAND.id());
            land.setDayTime(MORNING);
            look(ctx, land, EAST, -25F);
        })
                .thenWaitUntil(() -> ctx.mc().level != null && ctx.mc().level.dimension().equals(LAND), ARRIVE,
                        "the player's game to reach the sky land")
                .thenAwaitLandDrawn(ARRIVE)
                .thenScreenshot(SHOOT, "morning_east")
                .thenServer(0, level -> look(ctx, land(ctx), WEST, -25F))
                .thenScreenshot(SHOOT, "morning_west")
                .thenServer(0, level -> {
                    land(ctx).setDayTime(NOON);
                    look(ctx, land(ctx), EAST, -89F);
                })
                .thenWaitUntil(() -> ctx.mc().level.getDayTime() == NOON, WAIT, "the player's clock to show noon")
                .then(1, () -> ctx.assertTrue(SkySight.eclipseLight(ctx.mc().level, Skies.of(ctx.mc().level), 0F)
                        < 0.2, "the moon hides the sun at noon, and the day darkens"))
                .thenScreenshot(SHOOT, "eclipse_at_noon")
                .thenServer(0, level -> {
                    land(ctx).setDayTime(NIGHT);
                    look(ctx, land(ctx), NORTH, -20F);
                })
                .thenWaitUntil(() -> ctx.mc().level.getDayTime() == NIGHT, WAIT, "the player's clock to show night")
                .thenScreenshot(SHOOT, "night_north")
                .thenServer(0, level -> look(ctx, land(ctx), WEST, -50F))
                .thenScreenshot(SHOOT, "night_west")
                .thenServer(0, level -> {
                    final ServerLevel land = land(ctx);
                    land.setDayTime(MORNING);
                    look(ctx, land, EAST, -10F);
                    WorldEvents.start(land, TestSkies.ACID_RAIN, 2_000, Optional.of(ctx.serverPlayer()
                            .blockPosition()));
                })
                .thenWaitUntil(() -> !WeatherClient.around().isEmpty(), WAIT, "the acid rain to reach the player")
                .thenScreenshot(40, "acid_rain")
                .thenServer(0, level -> {
                    final ServerLevel land = land(ctx);
                    WorldEvents.stop(land, TestSkies.ACID_RAIN);
                    WorldEvents.start(land, TestSkies.SPARK_STORM, 2_000, Optional.of(ctx.serverPlayer()
                            .blockPosition()));
                })
                .thenWaitUntilServer(level -> !land(ctx).getEntitiesOfClass(LightningBolt.class,
                                new AABB(ctx.serverPlayer().blockPosition()).inflate(32)).isEmpty(), WAIT,
                        "the storm's lightning to strike near the player", level -> "no strike yet")
                .thenServer(0, level -> WorldEvents.stop(land(ctx), TestSkies.SPARK_STORM));
    }

    /**
     * A weather falls again over a player who goes to another world under the same weather: the new world's game has
     * none of the old one's effects, and the rain is held there anew.
     */
    @ClientTest(timeoutTicks = 3000)
    public static void weather_fallsAgainInAnotherWorldOfTheSameWeather(final ClientTestContext ctx) {
        ctx.afterTest(() -> ctx.server().execute(() -> {
            final ServerPlayer player = ctx.serverPlayer();
            final BlockPos home = ctx.origin().above(2);
            player.teleportTo(ctx.server().overworld(), home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 0F, 0F);
            RuntimeDimensions.remove(ctx.server(), LAND);
            RuntimeDimensions.remove(ctx.server(), OTHER_LAND);
        }));
        ctx.thenServer(0, level -> {
            for (final ResourceKey<Level> each : List.of(LAND, OTHER_LAND)) {
                final ServerLevel land = RuntimeDimensions.getOrCreate(level.getServer(), each,
                        TestWorldGen.SKY_LAND.id());
                WorldEvents.start(land, TestSkies.ACID_RAIN, 2_000, Optional.of(new BlockPos(0, (int) HIGH, 0)));
            }
            look(ctx, land(ctx), EAST, -10F);
        })
                .thenWaitUntil(() -> ctx.mc().level != null && ctx.mc().level.dimension().equals(LAND), ARRIVE,
                        "the player's game to reach the sky land")
                .thenWaitUntil(() -> !WeatherClient.around().isEmpty() && EffectEngine.playing() > 0, WAIT,
                        "the acid rain to fall over the player")
                .thenServer(0, level -> look(ctx, ctx.server().getLevel(OTHER_LAND), EAST, -10F))
                .thenWaitUntil(() -> ctx.mc().level != null && ctx.mc().level.dimension().equals(OTHER_LAND), ARRIVE,
                        "the player's game to reach the other land")
                .thenWaitUntil(() -> !WeatherClient.around().isEmpty() && EffectEngine.playing() > 0, WAIT,
                        "the acid rain to fall again over the player in the other land")
                .thenServer(0, level -> {
                    WorldEvents.stop(land(ctx), TestSkies.ACID_RAIN);
                    WorldEvents.stop(ctx.server().getLevel(OTHER_LAND), TestSkies.ACID_RAIN);
                });
    }

    /* Puts the player high over the land, looking {@code yaw} and {@code pitch}. */
    private static void look(final ClientTestContext ctx, final ServerLevel land, final float yaw,
                             final float pitch) {
        final ServerPlayer player = ctx.serverPlayer();
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
        player.teleportTo(land, 0.5, HIGH, 0.5, yaw, pitch);
    }

    private static ServerLevel land(final ClientTestContext ctx) {
        final ServerLevel land = ctx.server().getLevel(LAND);
        if (land == null) {
            throw new IllegalStateException("the sky land is gone");
        }
        return land;
    }
}
