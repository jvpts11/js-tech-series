/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.core.fluid.CoreFluidTags;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Fluids declared through the Core: a liquid with its type, its block and its bucket, marked corrosive; a gas lighter
 * than air, with no block and no bucket, marked a gas for the Core and for other mods; both kept in tanks.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class FluidGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos POOL = new BlockPos(2, 2, 2);

    private FluidGameTests() {
    }

    @GameTest(template = ARENA)
    public static void liquid_hasItsTypeBlockBucketAndMarks(final GameTestHelper helper) {
        final FluidType type = TestFluids.ACID.type();

        helper.assertTrue(type.getTemperature() == TestFluids.ACID_TEMPERATURE, "the acid is as hot as declared");
        helper.assertTrue(type.getDensity() > 0, "and heavier than air");
        helper.assertTrue(TestFluids.ACID.source().defaultFluidState().is(CoreFluidTags.CORROSIVE),
                "it is marked corrosive");
        helper.assertTrue(!TestFluids.ACID.source().defaultFluidState().is(CoreFluidTags.GASES), "and not a gas");
        helper.assertTrue(TestFluids.ACID.block().isPresent() && TestFluids.ACID.bucket().isPresent(),
                "a liquid has a block and a bucket");

        helper.setBlock(POOL, TestFluids.ACID.block().orElseThrow().get());
        final FluidState poured = helper.getLevel().getFluidState(helper.absolutePos(POOL));
        helper.assertTrue(poured.isSource() && poured.getType() == TestFluids.ACID.source(),
                "its block is a pool of it");

        final ItemStack bucket = new ItemStack(TestFluids.ACID.bucket().orElseThrow().get());
        final IFluidHandlerItem held = bucket.getCapability(Capabilities.FluidHandler.ITEM);
        helper.assertTrue(held != null && held.getFluidInTank(0).is(TestFluids.ACID.source())
                && held.getFluidInTank(0).getAmount() == 1_000, "its bucket holds a bucket of it");
        helper.assertTrue(held.drain(1_000, IFluidHandler.FluidAction.EXECUTE).getAmount() == 1_000
                && held.getContainer().is(Items.BUCKET), "and pours it out, leaving an empty bucket");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void gas_isLighterThanAirWithNoBlockNorBucket(final GameTestHelper helper) {
        final FluidType type = TestFluids.GAS.type();

        helper.assertTrue(type.getDensity() < 0, "a gas is lighter than air");
        helper.assertTrue(type.getTemperature() == TestFluids.GAS_TEMPERATURE, "and as hot as declared");
        helper.assertTrue(TestFluids.GAS.block().isEmpty() && TestFluids.GAS.bucket().isEmpty(),
                "it has no block and no bucket");
        helper.assertTrue(TestFluids.GAS.source().getBucket() == Items.AIR, "the game finds no bucket for it");
        helper.assertTrue(TestFluids.GAS.source().defaultFluidState().is(CoreFluidTags.GASES)
                && TestFluids.GAS.source().defaultFluidState().is(Tags.Fluids.GASEOUS),
                "it is marked a gas, for the Core and for other mods");

        final FluidTank tank = new FluidTank(8_000);
        helper.assertTrue(tank.fill(TestFluids.GAS.stack(5_000), IFluidHandler.FluidAction.EXECUTE) == 5_000,
                "a tank holds it");
        helper.assertTrue(tank.getFluid().is(TestFluids.GAS.source()), "as itself");
        helper.succeed();
    }
}
