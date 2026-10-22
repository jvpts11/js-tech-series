/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.hazard.BlockTemperatures;
import dev.jstech.core.hazard.BodyHeat;
import dev.jstech.core.hazard.BodyTemperature;
import dev.jstech.core.hazard.Clothing;
import dev.jstech.core.hazard.HazardBand;
import dev.jstech.core.hazard.Hazards;
import dev.jstech.core.heat.HeatMath;
import dev.jstech.core.worldgen.biome.BiomeClimates;
import dev.jstech.tests.JsTests;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A player's warmth: leather worn holds heat in and keeps the wind out; a lit campfire near warms what a body sees and
 * an unlit one does not; a core below 35 °C stands in mild hypothermia, slowed, and one above 40 °C in heatstroke,
 * confused; a body used to the cold has its cold bands lower; each second moves a survival player's core and keeps a
 * creative one's normal; and the air a body feels in the game's own dimensions is its biome's climate.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class BodyTemperatureGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos STAND = new BlockPos(4, 2, 4);
    private static final BlockPos FIRE = new BlockPos(4, 2, 2);
    private static final double AIR = 290;
    private static final double CLOSE = 0.05;

    private BodyTemperatureGameTests() {
    }

    @GameTest(template = ARENA)
    public static void clothing_leatherHoldsHeatInAndKeepsTheWindOut(final GameTestHelper helper) {
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.assertTrue(Clothing.worn(player).resistance() == 0, "a bare body holds nothing in");
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
        player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.LEATHER_LEGGINGS));
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.LEATHER_BOOTS));
        final BodyHeat.Clothes worn = Clothing.worn(player);
        helper.assertTrue(Math.abs(worn.resistance() - 0.97 / 6) < 1.0e-6,
                "leather over all but the hands, letting six watts out a square metre a kelvin: " + worn);
        helper.assertTrue(Math.abs(worn.windproof() - 0.97) < 1.0e-6 && worn.waterproof() == 0,
                "keeping the wind out, not the water");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void blocks_aLitCampfireNearWarmsWhatABodySees(final GameTestHelper helper) {
        final Player player = standing(helper, GameType.SURVIVAL);
        helper.setBlock(FIRE, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        final double lit = BlockTemperatures.radiantKelvin(helper.getLevel(), player, AIR);
        helper.assertTrue(lit > AIR + 10, "a lit campfire two blocks off warms what the body sees: " + lit);
        helper.setBlock(FIRE, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
        final double out = BlockTemperatures.radiantKelvin(helper.getLevel(), player, AIR);
        helper.assertTrue(Math.abs(out - AIR) < 1.0e-6, "and once it is out, nothing: " + out);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void core_below35BringsMildHypothermiaAndSlows(final GameTestHelper helper) {
        final Player player = standing(helper, GameType.SURVIVAL);
        BodyTemperature.setCore(player, celsius(34.5));
        Hazards.second(player, helper.getLevel());
        final double level = Hazards.level(player, BodyTemperature.HYPOTHERMIA);
        helper.assertTrue(Math.abs(level - 2.5) < CLOSE, "two and a half degrees below normal: " + level);
        helper.assertTrue(band(player, true).map(HazardBand::name).orElse(null) == BodyTemperature.MILD_HYPOTHERMIA,
                "mild hypothermia");
        helper.assertTrue(player.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "which slows the body");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void core_above40BringsHeatstroke(final GameTestHelper helper) {
        final Player player = standing(helper, GameType.SURVIVAL);
        BodyTemperature.setCore(player, celsius(40.5));
        Hazards.second(player, helper.getLevel());
        helper.assertTrue(band(player, false).map(HazardBand::name).orElse(null) == BodyTemperature.HEATSTROKE,
                "past 40 °C, heatstroke: " + Hazards.level(player, BodyTemperature.OVERHEATING));
        helper.assertTrue(player.hasEffect(MobEffects.CONFUSION), "which confuses the body");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void acclimation_lowersTheColdBandsOfABodyUsedToTheCold(final GameTestHelper helper) {
        final Player player = standing(helper, GameType.SURVIVAL);
        BodyTemperature.setCore(player, celsius(34.85));
        helper.assertTrue(BodyTemperature.coldLevel(player, helper.getLevel()) >= BodyTemperature.MILD,
                "at 34.85 °C a body used to nothing is in mild hypothermia");
        BodyTemperature.setAcclimation(player, -0.5);
        helper.assertTrue(BodyTemperature.coldLevel(player, helper.getLevel()) < BodyTemperature.MILD,
                "one used to the cold, its bands half a degree lower, is not yet");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void second_movesASurvivorsCoreAndKeepsACreativeOnesNormal(final GameTestHelper helper) {
        final Player survivor = standing(helper, GameType.SURVIVAL);
        Hazards.second(survivor, helper.getLevel());
        helper.assertTrue(BodyTemperature.warmth(survivor).coreKelvin() != BodyHeat.NORMAL,
                "a second in the place moves a survivor's core");
        final Player creative = standing(helper, GameType.CREATIVE);
        BodyTemperature.setCore(creative, celsius(30));
        Hazards.second(creative, helper.getLevel());
        helper.assertTrue(BodyTemperature.warmth(creative).coreKelvin() == BodyHeat.NORMAL,
                "a creative player's core is kept normal");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void air_inTheGamesOwnDimensionsIsTheBiomesClimate(final GameTestHelper helper) {
        final BlockPos at = helper.absolutePos(STAND);
        final double air = BodyTemperature.airKelvin(helper.getLevel(), at);
        final double biome = HeatMath.kelvin(BiomeClimates.celsius(helper.getLevel(), at));
        helper.assertTrue(Math.abs(air - biome) < 1.0e-9, "the air a body feels is its biome's: " + air + " K");
        helper.succeed();
    }

    /* A mock player of {@code mode} standing in the arena's middle, facing north. */
    private static Player standing(final GameTestHelper helper, final GameType mode) {
        final Player player = helper.makeMockPlayer(mode);
        final Vec3 at = helper.absoluteVec(Vec3.atBottomCenterOf(STAND));
        player.moveTo(at.x, at.y, at.z, 180.0F, 0.0F);
        return player;
    }

    private static Optional<HazardBand> band(final Player player, final boolean cold) {
        return Hazards.band(player, cold ? BodyTemperature.HYPOTHERMIA : BodyTemperature.OVERHEATING);
    }

    private static double celsius(final double degrees) {
        return degrees + BodyHeat.FREEZING;
    }
}
