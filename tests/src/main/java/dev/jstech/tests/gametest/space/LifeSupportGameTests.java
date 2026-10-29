/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import dev.jstech.core.dimension.DimensionEffects;
import dev.jstech.core.dimension.DimensionHazardEvent;
import dev.jstech.core.dimension.RuntimeDimensions;
import dev.jstech.core.hazard.DamageKinds;
import dev.jstech.core.hazard.Resistances;
import dev.jstech.core.jstechseries.hazard.CosmicRadiation;
import dev.jstech.space.JsSpace;
import dev.jstech.space.api.SpaceSuit;
import dev.jstech.space.api.SpaceSuits;
import dev.jstech.space.api.WorldDanger;
import dev.jstech.space.api.WorldDangers;
import dev.jstech.space.life.LifeSupport;
import dev.jstech.space.universe.SolarSystemWorlds;
import dev.jstech.space.worldgen.MoonWorld;
import dev.jstech.testkit.gametest.GameTestPlayers;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestSpaceSuits;
import dev.jstech.tests.TestWorldGen;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Life support: a pressure suit worn whole breathes from its tanks where there is no air, a second of air each second,
 * and stands the cold of its range; empty, or not worn whole, its wearer suffocates. Modules fit only the piece they go
 * on, once, and do what they say: more air, the sky's radiation stopped. The worlds say their dangers, and the sky's
 * radiation reaches the Moon's open ground as its dose a day.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class LifeSupportGameTests {

    private static final String ARENA = "empty";
    private static final String BATCH = "jsspace_life_support";
    /* Where the player stands on the test Moon, high in its vacuum. */
    private static final Vec3 HIGH = new Vec3(0.5, 100, 0.5);
    private static final double CLOSE = 1e-6;
    /* The Moon's dose a day of the game, in millisieverts, and the game's days an hour. */
    private static final double MOON_DOSE = 1.4;
    private static final double GAME_DAYS_AN_HOUR = 3;

    private LifeSupportGameTests() {
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void suit_breathesFromItsTanksAndStandsTheColdUntilTheyRunOut(final GameTestHelper helper) {
        final ResourceKey<Level> key = key("life_moon");
        final ServerLevel moon = RuntimeDimensions.getOrCreate(helper.getLevel().getServer(), key,
                TestWorldGen.MOON.id());
        final ServerPlayer player = GameTestPlayers.join(moon, UUID.randomUUID(), "astronaut");
        player.setPos(HIGH);
        final Set<DimensionHazardEvent.Hazard> bare = DimensionEffects.weigh(player);
        suit(player);
        final SpaceSuit suit = SpaceSuits.worn(player).orElse(null);
        final Set<DimensionHazardEvent.Hazard> suited = DimensionEffects.weigh(player);
        final ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        final int left = suit == null ? -1 : LifeSupport.air(moon, chest, suit);
        if (suit != null) {
            LifeSupport.setAir(moon, chest, suit, 0);
        }
        final Set<DimensionHazardEvent.Hazard> empty = DimensionEffects.weigh(player);
        GameTestPlayers.leave(player);
        RuntimeDimensions.remove(helper.getLevel().getServer(), key);
        helper.assertTrue(bare.contains(DimensionHazardEvent.Hazard.AIR), "with no suit, no air; " + bare);
        helper.assertTrue(suit != null, "the four pieces worn are the suit");
        helper.assertTrue(!suited.contains(DimensionHazardEvent.Hazard.AIR)
                && !suited.contains(DimensionHazardEvent.Hazard.COLD), "sealed, it breathes and stands the cold; "
                + suited);
        helper.assertTrue(left == TestSpaceSuits.AIR - 1, "a second of air spent from full tanks: " + left);
        helper.assertTrue(empty.contains(DimensionHazardEvent.Hazard.AIR)
                && !empty.contains(DimensionHazardEvent.Hazard.COLD), "its tanks empty, it suffocates, warm still; "
                + empty);
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void suit_notWornWholeKeepsNoOneAlive(final GameTestHelper helper) {
        final ResourceKey<Level> key = key("life_half");
        final ServerLevel moon = RuntimeDimensions.getOrCreate(helper.getLevel().getServer(), key,
                TestWorldGen.MOON.id());
        final ServerPlayer player = GameTestPlayers.join(moon, UUID.randomUUID(), "half_suited");
        player.setPos(HIGH);
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        final boolean worn = SpaceSuits.worn(player).isPresent();
        final Set<DimensionHazardEvent.Hazard> hazards = DimensionEffects.weigh(player);
        GameTestPlayers.leave(player);
        RuntimeDimensions.remove(helper.getLevel().getServer(), key);
        helper.assertFalse(worn, "half a suit is no suit");
        helper.assertTrue(hazards.contains(DimensionHazardEvent.Hazard.AIR), "and breathes nothing; " + hazards);
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void modules_fitTheirPieceOnceAndDoWhatTheySay(final GameTestHelper helper) {
        final Level level = helper.getLevel();
        final SpaceSuit suit = SpaceSuits.declared().get(TestSpaceSuits.SUIT);
        final ItemStack chest = new ItemStack(Items.IRON_CHESTPLATE);
        final ItemStack helmet = new ItemStack(Items.IRON_HELMET);
        helper.assertTrue(LifeSupport.capacity(level, chest, suit) == TestSpaceSuits.AIR,
                "bare tanks hold the suit's air");
        helper.assertTrue(LifeSupport.fit(level, chest, module("extra_oxygen")), "the extra oxygen fits the chest");
        helper.assertTrue(LifeSupport.capacity(level, chest, suit) == TestSpaceSuits.AIR * 3 / 2,
                "and holds half as much air again: " + LifeSupport.capacity(level, chest, suit));
        helper.assertFalse(LifeSupport.fit(level, chest, module("extra_oxygen")), "a module fits once");
        helper.assertFalse(LifeSupport.fit(level, chest, module("suit_lamps")), "the lamps do not fit the chest");
        helper.assertTrue(LifeSupport.fit(level, helmet, module("suit_lamps")), "they fit the helmet");
        helper.assertFalse(LifeSupport.fit(level, new ItemStack(Items.DIAMOND_CHESTPLATE), module("radiation_vest")),
                "nor does a module fit what is no suit's piece");
        helper.assertTrue(Resistances.of(chest, DamageKinds.RADIATION) == 0, "the bare chest stops no radiation");
        helper.assertTrue(LifeSupport.fit(level, chest, module("radiation_vest")), "the vest fits the chest");
        helper.assertTrue(Math.abs(Resistances.of(chest, DamageKinds.RADIATION) - 0.5) < CLOSE,
                "and stops half the sky's radiation");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void worlds_sayTheirDangersAndTheSkysDoseReachesTheMoon(final GameTestHelper helper) {
        helper.assertTrue(WorldDangers.of(helper.getLevel(), SolarSystemWorlds.MOON).containsAll(Set.of(
                WorldDanger.VACUUM, WorldDanger.COSMIC_RADIATION, WorldDanger.DUST, WorldDanger.GRAVITY)),
                "the Moon's dangers: vacuum, the sky's radiation, its dust, its pull");
        helper.assertTrue(WorldDangers.of(helper.getLevel(), SolarSystemWorlds.MARS).containsAll(Set.of(
                WorldDanger.THIN_AIR, WorldDanger.COLD, WorldDanger.POISONOUS_AIR, WorldDanger.PERCHLORATES)),
                "Mars': thin air, cold, carbon dioxide, perchlorates");
        final ResourceKey<Level> key = MoonWorld.world().dimension();
        final ServerLevel moon = RuntimeDimensions.getOrCreate(helper.getLevel().getServer(), key, key.location());
        final double dose = CosmicRadiation.at(moon, new Vec3(0.5, moon.getMaxBuildHeight() - 1, 0.5));
        helper.assertTrue(Math.abs(dose - MOON_DOSE * GAME_DAYS_AN_HOUR) < CLOSE,
                "the sky gives the Moon's open ground its dose a day: " + dose + " mSv an hour");
        helper.succeed();
    }

    /* Puts the test suit on {@code player}, all four pieces. */
    private static void suit(final ServerPlayer player) {
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
    }

    private static ResourceLocation module(final String id) {
        return ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, id);
    }

    private static ResourceKey<Level> key(final String path) {
        return ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(JsTests.MODID,
                path + "_" + UUID.randomUUID().toString().substring(0, 8)));
    }
}
