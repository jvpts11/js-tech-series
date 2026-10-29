/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.space;

import dev.jstech.core.dimension.DimensionBorders;
import dev.jstech.core.dimension.RuntimeDimensions;
import dev.jstech.space.worldgen.MarsWorld;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.testkit.client.ClientTestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Mars' ground on a player's game, as its sphere shows it from orbit: looked down on from above, the southern
 * highlands are its red regolith and the north cap its white frost.
 */
public final class MarsClientTests {

    /* Places of Mars' map, by longitude and latitude, on its middle meridian: southern highlands, and the north cap. */
    private static final double[] HIGHLANDS = {0, -30};
    private static final double[] NORTH_CAP = {0, 88};
    /* Noon on the middle meridian, the clock held still there: under the Sun, the ground shows its own colour. */
    private static final long NOON = 6_000L;
    private static final int ABOVE = 18;
    private static final int ARRIVE = 600;
    private static final int SETTLE = 10;
    private static final int PATCH = 30;
    /* How much redder than blue the regolith is, and how much brighter the frost, its three channels summed. */
    private static final int REDDER = 40;
    private static final int BRIGHTER = 150;
    private static final int CHANNEL = 0xFF;
    private static final int SEEING = 20 * 60 * 5;

    private MarsClientTests() {
    }

    @ClientTest(timeoutTicks = 2400)
    public static void ground_isTheHighlandsRedRegolithAndTheCapsWhiteFrost(final ClientTestContext ctx) {
        final int[] highlands = new int[2];
        final boolean[] daylight = new boolean[1];
        ctx.afterTest(() -> ctx.server().execute(() -> {
            ctx.server().getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(daylight[0], ctx.server());
            final ServerPlayer player = ctx.serverPlayer();
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
            player.removeEffect(MobEffects.NIGHT_VISION);
            final BlockPos origin = ctx.origin().above(2);
            player.teleportTo(ctx.server().overworld(), origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5, 0F,
                    0F);
        }));
        ctx.thenServer(0, level -> {
                    final GameRules.BooleanValue rule = level.getServer().getGameRules()
                            .getRule(GameRules.RULE_DAYLIGHT);
                    daylight[0] = rule.get();
                    rule.set(false, level.getServer());
                    lookDown(ctx, HIGHLANDS);
                })
                .thenWaitUntil(() -> ctx.mc().level != null
                        && ctx.mc().level.dimension().equals(MarsWorld.world().dimension()), ARRIVE,
                        "the player's game to reach Mars")
                .thenAwaitLandDrawn(ARRIVE)
                .then(SETTLE, () -> {
                    final int[] colour = mean(ctx);
                    highlands[0] = colour[0] + colour[1] + colour[2];
                    highlands[1] = colour[0] - colour[2];
                    ctx.assertTrue(highlands[1] > REDDER, "the highlands' regolith is red: red over blue by "
                            + highlands[1]);
                })
                .thenScreenshot(0, "southern_highlands_from_above")
                .thenServer(0, level -> lookDown(ctx, NORTH_CAP))
                .thenAwaitLandDrawn(ARRIVE)
                .then(SETTLE, () -> {
                    final int[] colour = mean(ctx);
                    final int cap = colour[0] + colour[1] + colour[2];
                    ctx.assertTrue(cap - highlands[0] > BRIGHTER, "the north cap's frost is white, far brighter than"
                            + " the highlands: " + cap + " over " + highlands[0]);
                })
                .thenScreenshot(0, "north_cap_from_above");
    }

    /* Puts the player over Mars' place {@code at}, by longitude and latitude, seeing in the dark, looking down. */
    private static void lookDown(final ClientTestContext ctx, final double[] at) {
        final ResourceKey<Level> key = MarsWorld.world().dimension();
        final ServerLevel mars = RuntimeDimensions.getOrCreate(ctx.server(), key, key.location());
        final DimensionBorders.Bounds bounds = DimensionBorders.inForce(mars.dimension()).orElseThrow();
        final double perDegree = (bounds.maxX() - bounds.minX()) / 360;
        final int x = (int) Math.round(at[0] * perDegree);
        final int z = (int) Math.round(-at[1] * perDegree);
        mars.setDayTime(NOON);
        mars.getChunk(new BlockPos(x, 0, z));
        final int ground = mars.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        final ServerPlayer player = ctx.serverPlayer();
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, SEEING, 0, false, false));
        player.teleportTo(mars, x + 0.5, ground + ABOVE, z + 0.5, 0F, 90F);
    }

    /* The mean colour of the middle of the screen, red, green and blue. */
    private static int[] mean(final ClientTestContext ctx) {
        final int x = ctx.mc().getWindow().getGuiScaledWidth() / 2;
        final int y = ctx.mc().getWindow().getGuiScaledHeight() / 2;
        final long[] sum = new long[3];
        int count = 0;
        try (ClientTestHelper.FrameSampler frame = ctx.sampleFrame()) {
            for (int dx = -PATCH; dx <= PATCH; dx++) {
                for (int dy = -PATCH; dy <= PATCH; dy++) {
                    final int argb = frame.pixel(x + dx, y + dy);
                    sum[0] += argb >> 16 & CHANNEL;
                    sum[1] += argb >> 8 & CHANNEL;
                    sum[2] += argb & CHANNEL;
                    count++;
                }
            }
        }
        return new int[] {(int) (sum[0] / count), (int) (sum[1] / count), (int) (sum[2] / count)};
    }
}
