/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.core.dimension.DimensionEffects;
import dev.jstech.core.dimension.DimensionHazardEvent;
import dev.jstech.core.dimension.DimensionRules;
import dev.jstech.core.dimension.DimensionRulesData;
import dev.jstech.core.dimension.RuntimeDimensions;
import dev.jstech.core.gametest.GameTestPlayers;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestSuits;
import dev.jstech.tests.TestWorldGen;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Core's dimensions, through the test mod's small moon: declared and generated with the world, its rules read
 * from its file and felt (its pull on whatever stands there, the air and cold a player suffers unless a suit spares
 * them), and copies of it made while the game runs and taken away again.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class DimensionGameTests {

    private static final String ARENA = "empty";
    private static final ResourceKey<Level> MOON = ResourceKey.create(Registries.DIMENSION, TestWorldGen.MOON.id());
    /*
     * The moon the tests stand on. A test server's flat world leaves the datapacks' dimensions out, so the declared
     * moon itself is not there; a copy of it made while the game runs is, which is also how a mod's copies are made.
     */
    private static final ResourceKey<Level> HERE = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "test_moon_here"));
    private static final double OVERWORLD_PULL = 0.08;
    private static final double MOON_SHARE = 0.16;

    private DimensionGameTests() {
    }

    @GameTest(template = ARENA)
    public static void declaredDimension_isMadeWithItsGround(final GameTestHelper helper) {
        helper.assertTrue(RuntimeDimensions.stemOf(helper.getLevel().getServer(), TestWorldGen.MOON.id()).isPresent(),
                "the declared moon can be made");
        final ServerLevel moon = moon(helper);
        helper.assertTrue(moon.getBlockState(new BlockPos(0, 0, 0)).is(Blocks.BEDROCK)
                        && moon.getBlockState(new BlockPos(0, 4, 0)).is(Blocks.STONE)
                        && moon.getBlockState(new BlockPos(0, 7, 0)).is(Blocks.LIGHT_GRAY_CONCRETE)
                        && moon.getBlockState(new BlockPos(0, 8, 0)).isAir(),
                "its ground is the layers it declares, bottom first");
        helper.assertTrue(moon.dimensionType().fixedTime().orElse(-1) == 18_000, "and it stands at midnight");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void rules_areReadFromTheDimensionsFile(final GameTestHelper helper) {
        final DimensionRules rules = DimensionRulesData.of(MOON);
        helper.assertTrue(Math.abs(rules.gravity() - MOON_SHARE) < 1e-9 && !rules.breathable() && rules.freezing()
                && rules.weather() == DimensionRules.Weather.CLEAR, "the moon's rules are its file's; got " + rules);
        helper.assertTrue(DimensionRulesData.of(Level.OVERWORLD).equals(DimensionRules.OVERWORLD),
                "and a dimension without a file keeps the overworld's");
        final ServerLevel moon = moon(helper);
        helper.assertTrue(DimensionRulesData.of(moon).equals(rules), "a copy without a file keeps its template's");
        helper.assertFalse(moon.getLevelData().isRaining(), "the moon's weather is held clear");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void gravity_pullsWhateverStandsInADimensionAsItsRulesSay(final GameTestHelper helper) {
        final ServerLevel moon = moon(helper);
        final ArmorStand onMoon = stand(moon, new BlockPos(2, 9, 2));
        final ArmorStand atHome = stand(helper.getLevel(), helper.absolutePos(new BlockPos(1, 2, 1)));
        try {
            helper.assertTrue(Math.abs(onMoon.getAttributeValue(Attributes.GRAVITY) - OVERWORLD_PULL * MOON_SHARE)
                    < 1e-9, "the moon pulls a sixth as hard; got " + onMoon.getAttributeValue(Attributes.GRAVITY));
            helper.assertTrue(Math.abs(atHome.getAttributeValue(Attributes.GRAVITY) - OVERWORLD_PULL) < 1e-9,
                    "while the overworld pulls as it always has");
        } finally {
            onMoon.discard();
            atHome.discard();
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void hazards_chokeAndFreezeAPlayerUnlessASuitSparesThem(final GameTestHelper helper) {
        final ServerLevel moon = moon(helper);
        final ServerPlayer bare = GameTestPlayers.join(moon, UUID.randomUUID(), "jstests-bare");
        final ServerPlayer suited = GameTestPlayers.join(moon, UUID.randomUUID(), TestSuits.SUITED);
        try {
            final Set<DimensionHazardEvent.Hazard> bareHazards = DimensionEffects.weigh(bare);
            helper.assertTrue(bareHazards.contains(DimensionHazardEvent.Hazard.AIR)
                            && bareHazards.contains(DimensionHazardEvent.Hazard.COLD)
                            && !bareHazards.contains(DimensionHazardEvent.Hazard.HEAT),
                    "a player with no suit chokes and freezes on the moon; got " + bareHazards);
            final Set<DimensionHazardEvent.Hazard> suitedHazards = DimensionEffects.weigh(suited);
            helper.assertTrue(!suitedHazards.contains(DimensionHazardEvent.Hazard.AIR)
                            && suitedHazards.contains(DimensionHazardEvent.Hazard.COLD),
                    "a suit that cancels the air spares the breath only; got " + suitedHazards);
            final int air = bare.getAirSupply();
            DimensionEffects.suffer(bare, bareHazards);
            helper.assertTrue(bare.getAirSupply() < air, "and the air runs down");
            helper.assertTrue(bare.getTicksFrozen() > 0, "and the cold sets in");
        } finally {
            GameTestPlayers.leave(bare);
            GameTestPlayers.leave(suited);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void runtimeDimension_isMadeAndTakenAway(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final ResourceKey<Level> copy = ResourceKey.create(Registries.DIMENSION,
                ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "moon_" + UUID.randomUUID().toString()
                        .substring(0, 8)));
        final ServerLevel made = RuntimeDimensions.getOrCreate(server, copy, TestWorldGen.MOON.id());
        try {
            helper.assertTrue(server.getLevel(copy) == made, "the copy is a dimension of the server at once");
            helper.assertTrue(RuntimeDimensions.made(server).containsKey(copy.location()),
                    "and is kept to be made again when the server starts");
            helper.assertTrue(made.getBlockState(new BlockPos(0, 7, 0)).is(Blocks.LIGHT_GRAY_CONCRETE),
                    "its ground is its template's");
            helper.assertTrue(Math.abs(DimensionRulesData.of(copy).gravity() - MOON_SHARE) < 1e-9,
                    "and so are its rules, having none of its own");
            helper.assertTrue(RuntimeDimensions.getOrCreate(server, copy, TestWorldGen.MOON.id()) == made,
                    "asking again hands back the same dimension");
        } finally {
            helper.assertTrue(RuntimeDimensions.remove(server, copy), "the copy is taken away");
        }
        helper.assertTrue(server.getLevel(copy) == null
                && !RuntimeDimensions.made(server).containsKey(copy.location()), "and is gone, not to be made again");
        helper.assertFalse(RuntimeDimensions.remove(server, MOON), "a dimension the Core did not make is left alone");
        helper.succeed();
    }

    private static ServerLevel moon(final GameTestHelper helper) {
        return RuntimeDimensions.getOrCreate(helper.getLevel().getServer(), HERE, TestWorldGen.MOON.id());
    }

    private static ArmorStand stand(final ServerLevel level, final BlockPos at) {
        final ArmorStand stand = EntityType.ARMOR_STAND.create(level);
        stand.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        level.addFreshEntity(stand);
        return stand;
    }
}
