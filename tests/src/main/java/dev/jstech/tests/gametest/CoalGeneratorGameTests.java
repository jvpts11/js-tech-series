/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.industrial.IndustrialModule;
import dev.jstech.industrial.blockentity.CoalGeneratorBlockEntity;
import dev.jstech.tests.JsTests;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The Coal Generator burning fuel, and what a fuel leaves behind. */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class CoalGeneratorGameTests {

    private static final BlockPos GENERATOR = new BlockPos(2, 2, 2);

    private CoalGeneratorGameTests() {
    }

    @GameTest(template = "empty")
    public static void tick_burningALavaBucketGivesTheEmptyBucketBack(final GameTestHelper helper) {
        helper.setBlock(GENERATOR, IndustrialModule.COAL_GENERATOR.get());
        if (!(helper.getBlockEntity(GENERATOR) instanceof CoalGeneratorBlockEntity generator)) {
            helper.fail("no Coal Generator at " + GENERATOR);
            return;
        }
        generator.getInventory().setStackInSlot(CoalGeneratorBlockEntity.FUEL_SLOT, new ItemStack(Items.LAVA_BUCKET));
        helper.succeedWhen(() -> {
            helper.assertTrue(generator.isBurning(), "the lava burns");
            helper.assertTrue(generator.getInventory().getStackInSlot(CoalGeneratorBlockEntity.FUEL_SLOT)
                    .is(Items.BUCKET), "the empty bucket is back in the fuel slot");
        });
    }

    @GameTest(template = "empty")
    public static void tick_burningCoalLeavesNothingBehind(final GameTestHelper helper) {
        helper.setBlock(GENERATOR, IndustrialModule.COAL_GENERATOR.get());
        if (!(helper.getBlockEntity(GENERATOR) instanceof CoalGeneratorBlockEntity generator)) {
            helper.fail("no Coal Generator at " + GENERATOR);
            return;
        }
        generator.getInventory().setStackInSlot(CoalGeneratorBlockEntity.FUEL_SLOT, new ItemStack(Items.COAL));
        helper.succeedWhen(() -> {
            helper.assertTrue(generator.isBurning(), "the coal burns");
            helper.assertTrue(generator.getInventory().getStackInSlot(CoalGeneratorBlockEntity.FUEL_SLOT).isEmpty(),
                    "nothing is left in the fuel slot");
        });
    }
}
