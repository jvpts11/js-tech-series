/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.heat.Blackbody;
import dev.jstech.core.heat.HeatClimate;
import dev.jstech.core.heat.ThermalSight;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestHeat;
import java.util.OptionalDouble;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Seeing heat in a world: goggles of the Core's tag let a player see it, worn or held; a block that holds heat is seen
 * at its temperature, lava at its type's and water not at all; a pig at a body's warmth, a zombie not, and a burning
 * pig hotter; and the hot plate glows once it is past the Draper point.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ThermalSightGameTests {

    private static final String ARENA = "empty";
    private static final double CLOSE = 1.0e-6;
    /* Long enough for the hot plate to reach its limit. */
    private static final int HEAT_UP = 60;

    private ThermalSightGameTests() {
    }

    @GameTest(template = ARENA)
    public static void sees_withGogglesWornOrHeldAndNotWithout(final GameTestHelper helper) {
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.assertFalse(ThermalSight.sees(player), "no goggles, no heat seen");
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(TestHeat.GOGGLES.get()));
        helper.assertTrue(ThermalSight.sees(player), "worn on the head");
        player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(TestHeat.GOGGLES.get()));
        helper.assertTrue(ThermalSight.sees(player), "or held");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void of_aHeatHolderIsSeenAtItsTemperature(final GameTestHelper helper) {
        final BlockPos at = new BlockPos(2, 2, 2);
        helper.setBlock(at, TestHeat.IRON.get());
        final TestHeat.HeatBlockEntity iron = helper.getBlockEntity(at);
        iron.heatBody().set(500.0);
        final OptionalDouble seen = ThermalSight.of(helper.getLevel(), helper.absolutePos(at));
        helper.assertTrue(seen.isPresent() && Math.abs(seen.getAsDouble() - 500.0) < CLOSE,
                "the iron is seen as hot as it is: " + seen);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void of_lavaIsSeenAndWaterIsNot(final GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 2, 1), Blocks.LAVA);
        helper.setBlock(new BlockPos(3, 2, 1), Blocks.WATER);
        helper.setBlock(new BlockPos(5, 2, 1), Blocks.STONE);
        final OptionalDouble lava = ThermalSight.of(helper.getLevel(), helper.absolutePos(new BlockPos(1, 2, 1)));
        helper.assertTrue(lava.isPresent() && lava.getAsDouble() > 1_000.0, "lava, hot: " + lava);
        helper.assertTrue(ThermalSight.of(helper.getLevel(), helper.absolutePos(new BlockPos(3, 2, 1))).isEmpty(),
                "water, near the air's temperature, is not seen apart from it");
        helper.assertTrue(ThermalSight.of(helper.getLevel(), helper.absolutePos(new BlockPos(5, 2, 1))).isEmpty(),
                "nor stone, which holds no heat of its own");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void of_livingThingsAtABodysWarmthTheUndeadNot(final GameTestHelper helper) {
        final Pig pig = helper.spawn(EntityType.PIG, new BlockPos(1, 2, 3));
        final Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(4, 2, 3));
        helper.assertTrue(Math.abs(ThermalSight.of(pig).orElse(0.0) - ThermalSight.BODY) < CLOSE, "the pig");
        helper.assertTrue(ThermalSight.of(zombie).isEmpty(), "the zombie is as cold as the air");
        pig.igniteForSeconds(5.0F);
        helper.assertTrue(Math.abs(ThermalSight.of(pig).orElse(0.0) - ThermalSight.BURNING) < CLOSE,
                "a burning pig is hotter");
        pig.discard();
        zombie.discard();
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 120)
    public static void hotPlate_glowsPastTheDraperPoint(final GameTestHelper helper) {
        final BlockPos at = new BlockPos(2, 2, 2);
        helper.setBlock(at, TestHeat.HOT_PLATE.get());
        helper.runAfterDelay(HEAT_UP, () -> {
            final TestHeat.HeatBlockEntity plate = helper.getBlockEntity(at);
            final double kelvin = plate.heatBody().temperature();
            helper.assertTrue(Blackbody.glows(kelvin), "the plate is past the Draper point: " + kelvin + " K");
            helper.assertTrue(kelvin > HeatClimate.ambient(helper.getLevel(), helper.absolutePos(at)),
                    "far above the air");
            helper.assertTrue(Blackbody.glow(kelvin) > 0.5, "glowing brightly: " + Blackbody.glow(kelvin));
            helper.succeed();
        });
    }
}
