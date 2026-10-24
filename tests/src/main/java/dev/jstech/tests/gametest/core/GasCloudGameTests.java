/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.dimension.DimensionHazards;
import dev.jstech.core.explosion.Explosions;
import dev.jstech.core.hazard.GasClouds;
import dev.jstech.core.hazard.GasLimits;
import dev.jstech.core.hazard.Hazards;
import dev.jstech.core.jstechseries.Element;
import dev.jstech.core.jstechseries.Elements;
import dev.jstech.core.material.Material;
import dev.jstech.tests.JsTests;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Gas let out into the open: a cloud that spreads and thins; a toxic gas poisoning whoever breathes it by its share of
 * what is dangerous; an asphyxiant leaving too little oxygen; a flammable gas between its explosive limits exploding
 * from a flame; and the real limits each gas carries.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class GasCloudGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos MIDDLE = new BlockPos(3, 3, 3);
    private static final double NEAR = 16;

    private GasCloudGameTests() {
    }

    @GameTest(template = ARENA)
    public static void limits_areTheRealOnesOfEachGas(final GameTestHelper helper) {
        final Material hydrogen = Elements.of(Element.HYDROGEN);
        helper.assertTrue(GasLimits.lowerExplosive(hydrogen).orElse(0) == 0.04
                && GasLimits.upperExplosive(hydrogen).orElse(0) == 0.75, "hydrogen burns from 4% to 75% of the air");
        helper.assertTrue(GasLimits.idlh(Elements.of(Element.CHLORINE)) == 10,
                "chlorine is immediately dangerous from 10 ppm");
        helper.assertTrue(GasLimits.idlh(Elements.of(Element.ARGON)) == GasLimits.UNKNOWN_IDLH,
                "a gas with no data of its own is taken at the common poisons' mark");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void cloud_spreadsAndThins(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final Material argon = Elements.of(Element.ARGON);
        final BlockPos at = helper.absolutePos(MIDDLE);
        GasClouds.release(level, at, argon, 100);
        final double first = GasClouds.shareAt(level, at, argon);
        helper.assertTrue(first > 0.5, "let out, the gas is most of the air where it came out: " + first);
        helper.runAfterDelay(2 * GasClouds.EVERY + 1, () -> {
            final double later = GasClouds.shareAt(level, at, argon);
            helper.assertTrue(later > 0 && later < first, "it spreads and thins: " + first + " then " + later);
            clear(level, at);
            helper.succeed();
        });
    }

    @GameTest(template = ARENA)
    public static void cloud_ofAToxicGasPoisonsWhoeverBreathesIt(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos at = helper.absolutePos(MIDDLE);
        final Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, MIDDLE.below());
        GasClouds.release(level, BlockPos.containing(pig.getEyePosition()), Elements.of(Element.CHLORINE), 1);
        Hazards.second(pig, level);
        helper.assertTrue(Hazards.rate(pig, GasClouds.POISONING) > 30,
                "a mole of chlorine, thousands of parts per million, poisons within the second: "
                        + Hazards.rate(pig, GasClouds.POISONING));
        clear(level, at);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void cloud_ofAnAsphyxiantLeavesTooLittleOxygen(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos at = helper.absolutePos(MIDDLE);
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.moveTo(Vec3.atBottomCenterOf(at.below()));
        GasClouds.release(level, BlockPos.containing(player.getEyePosition()), Elements.of(Element.ARGON), 150);
        Hazards.second(player, level);
        helper.assertTrue(Hazards.rate(player, DimensionHazards.AIR) >= 1,
                "in a cloud of argon most of the air, there is too little oxygen to breathe");
        clear(level, at);
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void cloud_ofAFlammableGasExplodesFromAFlame(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos at = helper.absolutePos(MIDDLE);
        final Material hydrogen = Elements.of(Element.HYDROGEN);
        // Ten moles in the cubic metres of a cloud a block wide: about 6% of the air, between its limits.
        GasClouds.release(level, at, hydrogen, 10);
        helper.setBlock(MIDDLE.east(), Blocks.CAMPFIRE);
        helper.succeedWhen(() -> {
            helper.assertTrue(GasClouds.of(level).stream().noneMatch(cloud -> cloud.gas().equals(hydrogen.id())
                    && cloud.centre().distanceTo(Vec3.atCenterOf(at)) <= NEAR), "a flame sets the cloud off");
            helper.assertTrue(Explosions.running(level).stream().anyMatch(blast -> blast.explosive()
                    .equals(hydrogen.id())), "and it explodes");
        });
    }

    /* Takes the clouds near {@code at} away, so no other test breathes them. */
    private static void clear(final ServerLevel level, final BlockPos at) {
        final Vec3 centre = Vec3.atCenterOf(at);
        GasClouds.STATE.set(level, new GasClouds.Book(GasClouds.of(level).stream()
                .filter(cloud -> cloud.centre().distanceTo(centre) > NEAR).toList()));
    }
}
