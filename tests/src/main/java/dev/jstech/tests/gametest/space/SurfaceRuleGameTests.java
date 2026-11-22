/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import dev.jstech.core.dimension.RuntimeDimensions;
import dev.jstech.space.api.SpaceWorlds;
import dev.jstech.space.universe.SolarBody;
import dev.jstech.space.universe.SolarSystemWorlds;
import dev.jstech.tests.JsTests;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The surfaces of the worlds of space as the game reads them, block by block down every column: a biome's or a noise's
 * test, far costlier than a depth's, is made only as deep under the floor as the rules under it lay anything. Tested
 * down the whole column, it made the Moon and Mars several times slower to generate than the Overworld.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class SurfaceRuleGameTests {

    private static final String ARENA = "empty";
    /* The conditions costly to test, and the test of how deep under the floor a block lies. */
    private static final List<String> COSTLY = List.of("minecraft:biome", "minecraft:noise_threshold");
    private static final String DEPTH = "minecraft:stone_depth";

    private SurfaceRuleGameTests() {
    }

    @GameTest(template = ARENA)
    public static void surface_ofTheMoonAndMarsTestsItsBiomesOnlyNearTheTop(final GameTestHelper helper) {
        for (final SolarBody body : List.of(SolarBody.MOON, SolarBody.MARS)) {
            final ServerLevel level = levelOf(helper.getLevel(), body);
            final SurfaceRules.RuleSource rule = ((NoiseBasedChunkGenerator) level.getChunkSource().getGenerator())
                    .generatorSettings().value().surfaceRule();
            final JsonElement read = SurfaceRules.RuleSource.CODEC.encodeStart(JsonOps.INSTANCE, rule).getOrThrow();
            final List<String> deep = new ArrayList<>();
            costlyDown(read, false, deep);
            helper.assertTrue(deep.isEmpty(), body + "'s surface tests these down the whole column, not only as deep "
                    + "as its rules lay anything: " + deep);
        }
        helper.succeed();
    }

    /* Into {@code out}, the costly conditions of {@code rule} tested at any depth; {@code near}, under a depth's. */
    private static void costlyDown(final JsonElement rule, final boolean near, final List<String> out) {
        if (!rule.isJsonObject()) {
            return;
        }
        final JsonObject node = rule.getAsJsonObject();
        if (node.has("sequence")) {
            node.getAsJsonArray("sequence").forEach(each -> costlyDown(each, near, out));
        }
        if (node.has("if_true")) {
            final JsonObject condition = node.getAsJsonObject("if_true");
            final String type = condition.get("type").getAsString();
            if (!near && COSTLY.contains(type)) {
                out.add(condition.toString());
            }
            costlyDown(node.get("then_run"), near || DEPTH.equals(type), out);
        }
    }

    private static ServerLevel levelOf(final ServerLevel any, final SolarBody world) {
        final ResourceKey<Level> key = SpaceWorlds.get(any, SolarSystemWorlds.worldOf(world)).orElseThrow()
                .dimension();
        return RuntimeDimensions.getOrCreate(any.getServer(), key, key.location());
    }
}
