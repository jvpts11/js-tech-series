/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.series;

import dev.jstech.core.energy.Energies;
import dev.jstech.core.energy.EnergyFaults;
import dev.jstech.core.energy.EnergyTooltips;
import dev.jstech.core.energy.WrongClass;
import dev.jstech.core.jstechseries.energy.JsEnergy;
import dev.jstech.core.jstechseries.energy.JsEnergySettings;
import dev.jstech.core.jstechseries.energy.VoltageClass;
import dev.jstech.tests.JsTests;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * J's Energy, the series' energy made from the Core's kit: joules shown as watts, five voltage classes each with its
 * grid and its rating in a port's tooltip, the server's settings shaping what a port of the wrong class suffers and what
 * a joule is worth in FE, and what a burn and an arc flash do in the world.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class JsEnergyGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos MACHINE = new BlockPos(2, 2, 2);
    private static final BlockPos BESIDE = new BlockPos(3, 2, 2);

    private JsEnergyGameTests() {
    }

    @GameTest(template = ARENA)
    public static void je_isJoulesShownAsWattsInFiveClasses(final GameTestHelper helper) {
        helper.assertTrue(Energies.byId(ResourceLocation.fromNamespaceAndPath("jscore", "je")).orElse(null)
                == JsEnergy.ENERGY, "J's Energy is jscore:je");
        helper.assertTrue(JsEnergy.ENERGY.classes().size() == VoltageClass.values().length,
                "it has the five voltage classes");
        helper.assertTrue(JsEnergy.ENERGY.grid(JsEnergy.HIGH).id()
                .equals(ResourceLocation.fromNamespaceAndPath("jscore", "je_high")), "each with its grid");
        helper.assertTrue(JsEnergy.ENERGY.rate(15L).english().equals("300 W"),
                "fifteen joules a tick are shown as 300 watts");
        helper.assertTrue(EnergyTooltips.port(JsEnergy.ENERGY, JsEnergy.LOW).orElseThrow().english()
                .equals("Port: Low voltage, 400 V"), "a port says its class and its voltage");
        helper.assertTrue(JsEnergy.voltageOf(JsEnergy.classOf(VoltageClass.EXTRA_HIGH)).orElse(null)
                == VoltageClass.EXTRA_HIGH, "a class knows its voltage");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void settings_shapeWhatAPortOfTheWrongClassSuffers(final GameTestHelper helper) {
        try {
            helper.assertTrue(JsEnergy.ENERGY.meeting(JsEnergy.LOW, JsEnergy.MEDIUM) == WrongClass.BURNS
                    && JsEnergy.ENERGY.meeting(JsEnergy.LOW, JsEnergy.HIGH) == WrongClass.ARC_FLASH
                    && JsEnergy.ENERGY.meeting(JsEnergy.HIGH, JsEnergy.LOW) == WrongClass.UNDERVOLTAGE,
                    "by default a class up burns, two up arc, one down is short");
            JsEnergySettings.setOvervoltageDamages(false);
            helper.assertTrue(JsEnergy.ENERGY.meeting(JsEnergy.LOW, JsEnergy.HIGH) == WrongClass.OVERVOLTAGE,
                    "with damage off, a class too high only keeps a block from starting");
            JsEnergySettings.setAcceptAnyVoltage(true);
            helper.assertTrue(JsEnergy.ENERGY.meeting(JsEnergy.LOW, JsEnergy.ULTRA_HIGH) == WrongClass.NONE,
                    "with any voltage accepted, every block takes every class");
        } finally {
            JsEnergySettings.setOvervoltageDamages(true);
            JsEnergySettings.setAcceptAnyVoltage(false);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void settings_setWhatAJouleIsWorthAndWhoSpeaksFe(final GameTestHelper helper) {
        try {
            helper.assertTrue(JsEnergy.ENERGY.toFe(10L) == 10L, "a joule is one FE by default");
            JsEnergySettings.setJoulesPerFe(4);
            helper.assertTrue(JsEnergy.ENERGY.toFe(10L) == 2L && JsEnergy.ENERGY.fromFe(3L) == 12L,
                    "at four joules to the FE, ten joules are two FE, rounded down");
            helper.assertTrue(JsEnergy.speaksFe(), "with J's Energy off, the series speaks FE");
            JsEnergySettings.setEnabled(true);
            helper.assertTrue(!JsEnergy.speaksFe(), "on, it speaks joules");
            JsEnergySettings.setAcceptFe(true);
            helper.assertTrue(JsEnergy.speaksFe(), "unless the server lets its blocks take FE directly");
        } finally {
            JsEnergySettings.setJoulesPerFe(1);
            JsEnergySettings.setEnabled(false);
            JsEnergySettings.setAcceptFe(false);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void arcFlash_destroysTheBlockSetsFireAndBurnsWhoeverIsClose(final GameTestHelper helper) {
        helper.setBlock(MACHINE, Blocks.IRON_BLOCK);
        final ArmorStand close = helper.spawn(EntityType.ARMOR_STAND, BESIDE);
        EnergyFaults.strike(helper.getLevel(), helper.absolutePos(MACHINE), WrongClass.ARC_FLASH);
        helper.assertTrue(!helper.getBlockState(MACHINE).is(Blocks.IRON_BLOCK),
                "the block is destroyed, fire taking its place");
        helper.assertTrue(close.isOnFire(), "whoever is close is set on fire");
        boolean fire = false;
        for (final BlockPos near : BlockPos.betweenClosed(MACHINE.offset(-1, 0, -1), MACHINE.offset(1, 0, 1))) {
            fire |= helper.getBlockState(near).getBlock() instanceof BaseFireBlock;
        }
        helper.assertTrue(fire, "and fire is set round it");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void burn_leavesTheBlockWhereItIs(final GameTestHelper helper) {
        helper.setBlock(MACHINE, Blocks.IRON_BLOCK);
        EnergyFaults.strike(helper.getLevel(), helper.absolutePos(MACHINE), WrongClass.BURNS);
        helper.assertTrue(helper.getBlockState(MACHINE).is(Blocks.IRON_BLOCK), "a burnt block stays, damaged");
        helper.succeed();
    }
}
